package network.xyo.sdkobjectmodelkotlin.objects.sets

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectExceotion
import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectIteratorException
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.lang.StringBuilder
import java.nio.ByteBuffer
import kotlin.experimental.and

/**
 * An Iterator for iterating over sets created with XyoObjectSetCreator.
 */

class XyoIterableObject (private val item : ByteArray)  {
    private val startingOffset = readOwnHeader()
    private var globalSchema : XyoObjectSchema? = null
    private var biggestOffset = startingOffset
    private val offsets = ArrayList<Int>()

    val iterator : XyoObjectIterator
        get() = XyoObjectIterator(startingOffset)

    val size : Int
        get() {
            if (biggestOffset == item.size) {
                return offsets.size
            }

            val sizeIt = XyoObjectIterator(biggestOffset)
            while (sizeIt.hasNext()) {sizeIt.next()}
            return offsets.size
        }

    private fun readItemAtOffset (startingOffset : Int) : ByteArray {
        if (globalSchema == null) {
            return readItemUntyped(startingOffset)
        }
        return readItemTyped(startingOffset)
    }

    private fun readItemUntyped (startingOffset: Int) : ByteArray {
        val schemaOfItem = getNextHeader(startingOffset)
        val sizeOfObject = readSizeOfObject(schemaOfItem.sizeIdentifier, startingOffset + 2)

        if (sizeOfObject == 0) {
            throw XyoObjectIteratorException("Size can not be 0. Value: ${item.toHexString()}")
        }

        if (biggestOffset <= startingOffset) {
            offsets.add(startingOffset)
        }

        biggestOffset = startingOffset + sizeOfObject + 2
        checkIndex(startingOffset + sizeOfObject + 2)
        return item.copyOfRange(startingOffset, startingOffset + sizeOfObject + 2)
    }

    private fun readItemTyped (startingOffset: Int) : ByteArray {
        val schemaOfItem =  globalSchema ?: throw XyoObjectIteratorException("Global schema is null!")
        val sizeOfObject = readSizeOfObject(schemaOfItem.sizeIdentifier, startingOffset)

        if (sizeOfObject == 0) {
            throw XyoObjectIteratorException("Size can not be 0. Value: ${item.toHexString()}")
        }

        if (biggestOffset <= startingOffset) {
            offsets.add(startingOffset)
        }

        val buffer = ByteBuffer.allocate(sizeOfObject + 2)
        checkIndex(startingOffset + sizeOfObject)
        buffer.put(schemaOfItem.header)
        buffer.put(item.copyOfRange(startingOffset, startingOffset + sizeOfObject))

        biggestOffset = startingOffset + sizeOfObject
        return buffer.array()
    }

    operator fun get(index: Int): ByteArray {
        if (index < offsets.size) {
            return readItemAtOffset(offsets[index])
        }

        val it = XyoObjectIterator(biggestOffset)
        var i = offsets.size

        while (it.hasNext()) {
            val item = it.next()

            if (i == index) {
                return item
            }

            i++
        }

        throw XyoObjectIteratorException("Index out of range! Size $i, Index: $index. Value: ${item.toHexString()}")
    }

    private fun ByteArray.toHexString(): String {
        val builder = StringBuilder()
        val it = this.iterator()
        builder.append("0x")
        while (it.hasNext()) {
            builder.append(String.format("%02X", it.next()))
        }

        return builder.toString()
    }

    operator fun get(type: Byte): Array<ByteArray> {
        val it = XyoObjectIterator(startingOffset)
        val itemsThatFollowTheType = ArrayList<ByteArray>()

        while (it.hasNext()) {
            val next = it.next()
            val nextHeader = XyoObjectSchema.createFromHeader(next.copyOfRange(0, 2))

            if (nextHeader.id == type) {
                itemsThatFollowTheType.add(next)
            }
        }

        return itemsThatFollowTheType.toTypedArray()
    }

    /**
     * Reads the size of the object at the current offset.
     */
    private fun readSizeOfObject (sizeToReadForSize : Int, offset: Int) : Int {
        val buffer = ByteBuffer.allocate(sizeToReadForSize)
        checkIndex(offset + sizeToReadForSize)
        buffer.put(item.copyOfRange(offset, offset + sizeToReadForSize))

        when (sizeToReadForSize) {
            1 -> return (buffer[0].toInt() and 0xFF)
            2 -> return (buffer.getShort(0).toInt() and 0xFFFF)
            4 -> return buffer.getInt(0)
        }

        throw Exception("Stub for long size. Value: ${item.toHexString()}")
    }

    /**
     * Gets the next object schema at the current offset.
     */
    private fun getNextHeader (offset : Int) : XyoObjectSchema {
        return  XyoObjectSchema.createFromHeader(item.copyOfRange(offset, offset + 2))
    }

    private fun checkIndex (index: Int) {
        if (index > item.size) {
            throw XyoObjectIteratorException("Out of size. Value: ${item.toHexString()}")
        }
    }

    inner class XyoObjectIterator (private var currentOffset: Int) : Iterator<ByteArray> {

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
            val nextItem = readItemAtOffset(currentOffset)

            if (globalSchema == null) {
                currentOffset += nextItem.size
            } else {
                currentOffset += nextItem.size - 2
            }

            return nextItem
        }
    }

    private fun readOwnHeader () : Int {
        val setHeader = getNextHeader(0)
        val totalSize = readSizeOfObject(setHeader.sizeIdentifier, 2)

        if ((totalSize + 2) != item.size) {
            throw XyoObjectIteratorException("Array size does not equal header size. Header size: " +
                    "$totalSize, Expected: ${item.size - 2}. Value: ${item.toHexString()}")
        }

        if (!setHeader.isIterable) {
            throw XyoObjectIteratorException("Can not iterate on object that is not iterable. Header " +
                    "${setHeader.header[0]}, ${setHeader.header[1]}. Value: ${item.toHexString()}")
        }

        if (setHeader.isTyped && totalSize != setHeader.sizeIdentifier) {
            globalSchema = getNextHeader(setHeader.sizeIdentifier + 2)
            return 4 + setHeader.sizeIdentifier
        }

        return 2 + setHeader.sizeIdentifier
    }

}