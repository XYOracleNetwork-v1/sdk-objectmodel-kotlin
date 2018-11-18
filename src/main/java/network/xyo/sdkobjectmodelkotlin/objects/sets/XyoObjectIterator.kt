package network.xyo.sdkobjectmodelkotlin.objects.sets

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectIteratorException
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.Buffer
import java.nio.ByteBuffer

@ExperimentalUnsignedTypes
/**
 * An Iterator for iterating over sets created with XyoObjectSetCreator.
 *
 * @property item The set to iterate over.
 */
class XyoObjectIterator (private val item : UByteArray) : Iterator<UByteArray> {
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
    override fun next(): UByteArray {
        val startingIndex = currentOffset
        val schemaOfItem =  globalSchema ?: getNextHeader()
        val sizeOfObject = readSizeOfObject(schemaOfItem.sizeIdentifier)
        currentOffset++

        if (globalSchema == null) {
            return item.copyOfRange(startingIndex, startingIndex + sizeOfObject.toInt() + 2)
        }

        val buffer = ByteBuffer.allocate(sizeOfObject.toInt() + 2)
        buffer.put(schemaOfItem.header.toByteArray())
        buffer.put(item.copyOfRange(startingIndex, startingIndex + sizeOfObject.toInt()).toByteArray())
        return buffer.array().toUByteArray()
    }


    /**
     * Reads the size of the object at the current offset.
     *
     * @param sizeToReadForSize The number of bytes to read for the size.
     */
    private fun readSizeOfObject (sizeToReadForSize : Int) : UInt {
        when (sizeToReadForSize) {
            1 -> {
                currentOffset++
                return item[currentOffset - 1].toUInt()
            }

            2 -> {
                currentOffset += 2
                return ByteBuffer.allocate(2).put(item.copyOfRange(currentOffset - 2, currentOffset).toByteArray())
                        .int
                        .toUInt()
            }

            4 -> {
                currentOffset += 4
                return ByteBuffer.allocate(4).put(item.copyOfRange(currentOffset - 4, currentOffset).toByteArray())
                        .int
                        .toUInt()
            }

            else -> {
                throw Exception("Stub!")
            }
        }
    }

    /**
     * Gets the next object schema at the current offset.
     */
    private fun getNextHeader () : XyoObjectSchema {
        if ((currentOffset + 2) > item.size - 1) {
            throw XyoObjectIteratorException("Out of size, trying to read header at offset: $currentOffset. " +
                    "Max: ${item.size}")
        }

        currentOffset += 2
        return  XyoObjectSchema.createFromHeader(item.copyOfRange(currentOffset - 2, currentOffset))
    }

    init {
        val setHeader = getNextHeader()
        val totalSize = readSizeOfObject(setHeader.sizeIdentifier)

        if ((totalSize + 2.toUInt()) != item.size.toUInt()) {
            throw XyoObjectIteratorException("Array size does not equal header size..")
        }

        if (!setHeader.isIterable) {
            throw XyoObjectIteratorException("Can not iterate on object that is not iterable.")
        }

        if (setHeader.isTyped) {
            globalSchema = getNextHeader()
        }
    }
}