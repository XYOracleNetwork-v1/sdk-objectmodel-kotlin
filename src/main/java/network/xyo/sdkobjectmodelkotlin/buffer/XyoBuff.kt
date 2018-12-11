package network.xyo.sdkobjectmodelkotlin.buffer

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectException
import network.xyo.sdkobjectmodelkotlin.objects.XyoNumberEncoder
import network.xyo.sdkobjectmodelkotlin.objects.toHexString
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.*

/**
 * A base class for <i>XyoObjects</i>. This is used for obtaining the schema, value, and size of the item.
 */
abstract class XyoBuff {
    /**
     * The primary data input for the XyoBuff. This buffer will not be read before the allowedOffset.
     */
    protected abstract var item : ByteArray

    /**
     * The sizes of the headers to read. This should align with XyoObjectSchema. This value should be set to 0 when
     * dealing with typed elements in a typed array.
     */
    protected open val headerSize : Int = 2

    /**
     * The starting offset of where to read. This buffer will not be read past this buffer.
     */
    abstract val allowedOffset : Int

    /**
     * The XyoObjectSchema of the XyoBuff
     */
    open val schema : XyoObjectSchema
        get() {
            return XyoObjectSchema.createFromHeader(item.copyOfRange(allowedOffset, allowedOffset + headerSize))
        }

    /**
     * The size of the object, in bytes.
     *
     * NOTE: This does not include the first two header bytes.
     */
    open val sizeBytes : Int
        get() {
            return readSizeOfObject(schema.sizeIdentifier, allowedOffset + headerSize)
        }

    /**
     * The value of the object. The value of the object is the object without the size, or the 2 byte header.
     */
    open val valueCopy : ByteArray
        get() {
            return item.copyOfRange(
                    headerSize + schema.sizeIdentifier + allowedOffset,
                    headerSize + allowedOffset + sizeBytes
            )
        }

    /**
     * All of the bytes for the object including the header and size.
     */
    open val bytesCopy : ByteArray
        get() {
            return item.copyOfRange(allowedOffset, allowedOffset + sizeBytes + headerSize)
        }


    /**
     * Reads the size of the object at a current index.
     *
     * @param sizeToReadForSize The number of bytes to read for the size.
     * @param offset The offset at which to read the size.
     * @throws XyoObjectException Ig the sizeToReadForSize is not [1, 2, 4]
     */
    protected fun readSizeOfObject (sizeToReadForSize : Int, offset: Int) : Int {
        val buffer = ByteBuffer.allocate(sizeToReadForSize)
        buffer.put(item.copyOfRange(offset, offset + sizeToReadForSize))

        when (sizeToReadForSize) {
            1 -> return buffer[0].toInt() and 0xFF
            2 -> return buffer.getShort(0).toInt() and 0xFFFF
            4 -> return buffer.getInt(0)
        }

        throw XyoObjectException("Stub for long count. Value: ${item.toHexString()}")
    }

    override fun equals(other: Any?): Boolean {
        if (other is XyoBuff) {
            return other.bytesCopy.contentEquals(bytesCopy)
        }

        return false
    }

    override fun hashCode(): Int {
        return bytesCopy.contentHashCode()
    }

    companion object {
        /**
         * Creates a XyoBuff with a schema and a value.
         *
         * @param schema The schema to create the object with.
         * @param value The value of the object to encode. This does NOT include size.
         */
        fun newInstance (schema : XyoObjectSchema, value : ByteArray) : XyoBuff {
            return object : XyoBuff() {
                override var item: ByteArray = getObjectEncoded(schema, value)
                override val valueCopy: ByteArray = value
                override val allowedOffset: Int = 0
            }
        }

        /**
         * Wraps a given XyoBuff in byte form and creates a XyoBuff.
         *
         * @param buff The encoded XyoBuffer, this value can be obtained from myBuff.bytesCopy
         * @return The represented XyoBuff.
         */
        fun wrap (buff : ByteArray) : XyoBuff {
            return object : XyoBuff() {
                override val allowedOffset: Int = 0
                override var item: ByteArray = buff
            }
        }

        /**
         * Encodes a XyoBuff given a value and schema.
         *
         * @param schema The schema of the XyoBuff to create.
         * @param value The value of the XyoBuff to create. This does NOT include size.
         */
        fun getObjectEncoded (schema: XyoObjectSchema, value: ByteArray) : ByteArray {
            val buffer = ByteBuffer.allocate(value.size + schema.sizeIdentifier + 2)
            buffer.put(schema.header)
            buffer.put(XyoNumberEncoder.createSize(value.size, schema.sizeIdentifier))
            buffer.put(value)
            return buffer.array()
        }
    }
}