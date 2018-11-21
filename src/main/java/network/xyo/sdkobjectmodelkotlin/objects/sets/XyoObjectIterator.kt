package network.xyo.sdkobjectmodelkotlin.objects.sets

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectIteratorException
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * An Iterator for iterating over sets created with XyoObjectSetCreator.
 */
open class XyoObjectIterator (private val item : ByteArray) : Iterator<ByteArray> {
    private var globalSchema : XyoObjectSchema? = null
    private var currentOffset = 0

    /**
     * Checks if there is another item in the set.
     */
    override fun hasNext(): Boolean {
        return item.size > currentOffset
    }

    /**
     * Gets the next item in the set.
     */
    override fun next(): ByteArray {

        val startingIndex = currentOffset
        val schemaOfItem =  globalSchema ?: getNextHeader()
        val sizeOfObject = readSizeOfObject(schemaOfItem.sizeIdentifier)
        checkBounds(sizeOfObject - schemaOfItem.sizeIdentifier)

        if (globalSchema == null) {
            currentOffset = startingIndex + sizeOfObject + 2
            return item.copyOfRange(startingIndex, startingIndex + sizeOfObject + 2)
        }

        val buffer = ByteBuffer.allocate(sizeOfObject + 2)
        buffer.put(schemaOfItem.header)
        buffer.put(item.copyOfRange(startingIndex, startingIndex + sizeOfObject))
        currentOffset = startingIndex + sizeOfObject
        return buffer.array()
    }

    private fun checkBounds (size : Int) {
        if (size + currentOffset > item.size) {
            throw XyoObjectIteratorException("Out of size. Length: ${item.size}, To Read: ${size + currentOffset}")
        }
    }

    /**
     * Reads the size of the object at the current offset.
     */
    private fun readSizeOfObject (sizeToReadForSize : Int) : Int {
        val buffer = ByteBuffer.allocate(sizeToReadForSize)
        checkBounds(sizeToReadForSize)
        currentOffset += sizeToReadForSize
        buffer.put(item.copyOfRange(currentOffset - sizeToReadForSize, currentOffset))

        when (sizeToReadForSize) {
            1 -> return (buffer[0] and 0xFF.toByte()).toInt()
            2 -> return (buffer.getShort(0).toInt() and 0xFFFF)
            4 -> return buffer.getInt(0)
        }

        throw Exception("Stub for long size")
    }

    /**
     * Gets the next object schema at the current offset.
     */
    private fun getNextHeader () : XyoObjectSchema {
        checkBounds(2)
        currentOffset += 2
        return  XyoObjectSchema.createFromHeader(item.copyOfRange(currentOffset - 2, currentOffset))
    }

    operator fun get(index: Int): ByteArray {
        var i = 0

        while (index != i) {
            next()
            i++
        }

        val next = next()
        currentOffset = 0
        readOwnHeader()
        return next
    }

    operator fun get(type: Byte): Array<ByteArray> {
        val itemsThatFollowTheType = ArrayList<ByteArray>()

        while (hasNext()) {
            val next = next()
            val nextHeader = XyoObjectSchema.createFromHeader(next.copyOfRange(0, 2))

            if (nextHeader.id == type) {
                itemsThatFollowTheType.add(next)
            }
        }

        reset()
        return itemsThatFollowTheType.toTypedArray()
    }

    val size : Int
        get() {
            var i = 0

            while (hasNext()) {
                next()
                i++
            }

            reset()
            return i
        }

    private fun reset() {
        currentOffset = 0
        readOwnHeader()
    }

    private fun readOwnHeader () {
        val setHeader = getNextHeader()
        val totalSize = readSizeOfObject(setHeader.sizeIdentifier)

        if ((totalSize + 2) != item.size) {
            throw XyoObjectIteratorException("Array size does not equal header size.")
        }

        if (!setHeader.isIterable) {
            throw XyoObjectIteratorException("Can not iterate on object that is not iterable.")
        }

        if (setHeader.isTyped) {
            globalSchema = getNextHeader()
        }
    }

    init {
        readOwnHeader()
    }
}