package network.xyo.sdkobjectmodelkotlin.objects

import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.ByteBuffer


/**
 * An object for creating Xyo Objects.
 */
@ExperimentalUnsignedTypes
object XyoObjectCreator {

    /**
     * Creates a Xyo Object with a schema and a value.
     *
     * @param schema The Schema to make the object with
     * @param value The value to create the object with
     */
    fun createObject (schema : XyoObjectSchema, value : ByteArray) : ByteArray {
        val buffer = ByteBuffer.allocate(value.size + schema.sizeIdentifier + 2)
        buffer.put(schema.header)
        buffer.put(createSize(value.size.toUInt(), schema))
        buffer.put(value)
        return buffer.array()
    }

    /**
     * Gets the best size of size to use.
     *
     * @param sizeOfItem The expected size of the item.
     */
    fun getSmartSize (sizeOfItem : UInt) : Int {
        if (sizeOfItem + 1.toUInt() <= UByte.MAX_VALUE) {
            return 1
        }

        if (sizeOfItem + 2.toUInt() <= UShort.MAX_VALUE) {
            return 2
        }

        if (sizeOfItem + 4.toUInt() <= UInt.MAX_VALUE) {
            return 4
        }

        return 8
    }

    /**
     * Gets the encoded size for an item.
     *
     * @param sizeOfItem The size of the value of the item.
     * @param schema The schema of the item.
     */
    private fun createSize (sizeOfItem : UInt, schema: XyoObjectSchema) : ByteArray {
        when (schema.sizeIdentifier) {
            1 -> return ByteBuffer.allocate(1).put((sizeOfItem + 1.toUInt()).toByte()).array()
            2 -> return ByteBuffer.allocate(2).putShort((sizeOfItem + 2.toUShort()).toShort()).array()
            4 -> return ByteBuffer.allocate(4).putInt((sizeOfItem + 4.toUInt()).toInt()).array()
        }

        throw Exception("Stub for Long Size.")
    }

    fun getObjectValue (item : ByteArray) : ByteArray {
        val objectSchema = XyoObjectSchema.createFromHeader(item.copyOfRange(0, 2))
        return item.copyOfRange(2 + objectSchema.sizeIdentifier, item.size)
    }

}