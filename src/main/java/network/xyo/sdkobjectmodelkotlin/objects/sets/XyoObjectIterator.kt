package network.xyo.sdkobjectmodelkotlin.objects.sets

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectIteratorException
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.lang.reflect.Array
import java.nio.Buffer
import java.nio.ByteBuffer
import java.util.ArrayList

@ExperimentalUnsignedTypes
/**
 * An Iterator for iterating over sets created with XyoObjectSetCreator.
 *
 * @property item The set to iterate over.
 */
class XyoObjectIterator (private val item : ByteArray) : Iterator<ByteArray> {
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


    /**
     * Reads the size of the object at the current offset.
     *
     * @param sizeToReadForSize The number of bytes to read for the size.
     */
    private fun readSizeOfObject (sizeToReadForSize : Int) : Int {
        when (sizeToReadForSize) {
            1 -> {
                currentOffset++
                return item[currentOffset - 1].toInt()
            }

            2 -> {
                currentOffset += 2
                return ByteBuffer.allocate(2).put(item.copyOfRange(currentOffset - 2, currentOffset)).getShort(0).toInt()
            }

            4 -> {
                currentOffset += 4
                return ByteBuffer.allocate(4).put(item.copyOfRange(currentOffset - 4, currentOffset)).getInt(0)
            }

            else -> {
                throw Exception("Stub for long size")
            }
        }
    }

    /**
     * Gets the next object schema at the current offset.
     */
    private fun getNextHeader () : XyoObjectSchema {
        if ((currentOffset + 2) > item.size) {
            throw XyoObjectIteratorException("Out of size, trying to read header at offset: $currentOffset. " +
                    "Max: ${item.size}")
        }

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

    val size : Int
        get() {
            var i = 0

            while (hasNext()) {
                next()
                i++
            }

            currentOffset = 0
            readOwnHeader()
            return i
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