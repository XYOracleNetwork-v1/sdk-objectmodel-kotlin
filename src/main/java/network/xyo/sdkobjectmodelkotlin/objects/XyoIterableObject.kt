package network.xyo.sdkobjectmodelkotlin.objects

import network.xyo.sdkobjectmodelkotlin.buffer.XyoBuff
import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectException
import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectIteratorException
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import org.json.JSONArray
import java.nio.ByteBuffer

/**
 * An Iterator for iterating over sets.
 */
abstract class XyoIterableObject : XyoBuff() {
    /**
     * The global schema of of the iterator. This value is null when iterating over an untyped set, and is not null
     * when iterating over a typed set.
     */
    private var globalSchema : XyoObjectSchema? = null

    /**
     * The largest offset known currently. This is used so the array is not iterated over many times.
     */
    private var biggestOffset : Int = 0

    /**
     * An array of the current offsets where items lie (a table of contents from index to offset for the item at that
     * index.)
     */
    private val offsets = ArrayList<Int>()

    /**
     * Gets an instance of a new iterator to illiterate over the set.
     *
     * @throws XyoObjectIteratorException If the bytes are malformed.
     */
    open val iterator : Iterator<XyoBuff>
        get() {
            readHeaderIfNeeded()
            return XyoObjectIterator(readOwnHeader())
        }

    /**
     * Gets the number of elements in the array.
     *
     * @throws XyoObjectIteratorException If the bytes are malformed.
     */
    open val count : Int
        get() {
            readHeaderIfNeeded()
            if (biggestOffset == item.size) {
                return offsets.size
            }

            val sizeIt = XyoObjectIterator(biggestOffset)
            while (sizeIt.hasNext()) {sizeIt.next()}
            return offsets.size
        }


    /**
     * Reads the current header of the array if the global offset is 0.
     *
     * @throws XyoObjectIteratorException If the bytes are malformed.
     */
    private fun readHeaderIfNeeded () {
        if (biggestOffset == 0) {
            biggestOffset = readOwnHeader()
        }
    }

    /**
     * Reads the current item at an offset.
     *
     * @param startingOffset The offset at which to read an item from.
     * @return The XyoBuff at that offset.
     * @throws XyoObjectIteratorException If the bytes are malformed.
     */
    private fun readItemAtOffset (startingOffset : Int) : XyoBuff {
        if (globalSchema == null) {
            return readItemUntyped(startingOffset)
        }
        return readItemTyped(startingOffset)
    }

    /**
     * Reads an item from an untyped array the startingOffset.
     *
     * @param startingOffset Where to read the item from.
     * @throws XyoObjectIteratorException If the bytes are malformed.
     */
    private fun readItemUntyped (startingOffset: Int) : XyoBuff {
        val schemaOfItem = getNextHeader(startingOffset)
        checkIndex(startingOffset + 2 + schemaOfItem.sizeIdentifier)
        val sizeOfObject = readSizeOfObject(schemaOfItem.sizeIdentifier, startingOffset + 2)

        if (sizeOfObject == 0) {
            throw XyoObjectIteratorException("Size can not be 0. Value: ${item.toHexString()}")
        }

        if (biggestOffset <= startingOffset) {
            offsets.add(startingOffset)
        }

        biggestOffset = startingOffset + sizeOfObject + 2
        checkIndex(startingOffset + sizeOfObject + 2)

        if (schemaOfItem.isIterable) {
            return object : XyoIterableObject() {
                override val allowedOffset: Int = startingOffset
                override var item: ByteArray = this@XyoIterableObject.item
            }
        }

        return object : XyoBuff() {
            override val allowedOffset: Int = startingOffset
            override var item: ByteArray = this@XyoIterableObject.item
        }
    }

    /**
     * Reads an item from an typed array the startingOffset.
     *
     * @param startingOffset Where to read the item from.
     * @throws XyoObjectIteratorException If the bytes are malformed.
     */
    private fun readItemTyped (startingOffset: Int) : XyoBuff {
        val schemaOfItem =  globalSchema ?: throw XyoObjectIteratorException("Global schema is null!")
        val sizeOfObject = readSizeOfObject(schemaOfItem.sizeIdentifier, startingOffset)

        if (sizeOfObject == 0) {
            throw XyoObjectIteratorException("Size can not be 0. Value: ${item.toHexString()}")
        }

        if (biggestOffset <= startingOffset) {
            offsets.add(startingOffset)
        }

        biggestOffset = startingOffset + sizeOfObject

        val buffer = ByteBuffer.allocate(sizeOfObject + 2)
        checkIndex(startingOffset + sizeOfObject)
        buffer.put(schemaOfItem.header)
        buffer.put(item.copyOfRange(startingOffset, startingOffset + sizeOfObject))

        if (schemaOfItem.isIterable) {
            return object : XyoIterableObject() {
                override val headerSize: Int = 0
                override val allowedOffset: Int = startingOffset
                override var item: ByteArray = this@XyoIterableObject.item
                override val schema: XyoObjectSchema = schemaOfItem
                override val bytesCopy: ByteArray = buffer.array()
            }
        }

        return object : XyoBuff() {
            override val headerSize: Int = 0
            override val allowedOffset: Int = startingOffset
            override var item: ByteArray = this@XyoIterableObject.item
            override val schema: XyoObjectSchema = schemaOfItem
            override val bytesCopy: ByteArray = buffer.array()
        }
    }

    /**
     * Gets an element at a certain index.
     *
     * @param index The index to get the item from.
     * @return The item at that index.
     * @throws XyoObjectIteratorException if the bytes are malformed or if the index is out of range.
     */
    open operator fun get(index: Int): XyoBuff {
        readHeaderIfNeeded()
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


    /**
     * Gets all of the elements in an array that are of a certain type.
     *
     * @param type The type of the elements to look for.
     * @return An array of possible items that have that ID.
     * @throws XyoObjectIteratorException if the bytes are malformed.
     */
    open operator fun get(type: Byte): Array<XyoBuff> {
        readHeaderIfNeeded()
        val it = XyoObjectIterator(readOwnHeader())
        val itemsThatFollowTheType = ArrayList<XyoBuff>()

        while (it.hasNext()) {
            val next = it.next()

            if (next.schema.id == type) {
                itemsThatFollowTheType.add(next)
            }
        }

        return itemsThatFollowTheType.toTypedArray()
    }

    /**
     * Gets the next object schema at the current offset.
     *
     * @param offset The offset at which to read the header from.
     */
    private fun getNextHeader (offset : Int) : XyoObjectSchema {
        return  XyoObjectSchema.createFromHeader(item.copyOfRange(offset, offset + 2))
    }

    /**
     * Gets the index of the current buffer to see if there is space.
     *
     * @throws XyoObjectIteratorException If there is not enough space to read from.
     */
    private fun checkIndex (index: Int) {
        if (index > item.size) {
            throw XyoObjectIteratorException("Out of count. Value: ${item.toHexString()}, Offset: $index")
        }
    }

    /**
     * An iterator class to help iterate over the set.
     *
     * @param currentOffset Where to start the iterator. (The offset of the first element)
     */
    inner class XyoObjectIterator (private var currentOffset: Int) : Iterator<XyoBuff> {

        /**
         * Checks if there is another item in the set.
         *
         * @return True if there is another element in the set.
         */
        override fun hasNext(): Boolean {
            return allowedOffset + sizeBytes + 2  > currentOffset
        }

        /**
         * Gets the next item in the set.
         *
         * @throws XyoObjectIteratorException If the bytes are malformed or if the index is out of range.
         */
        override fun next(): XyoBuff {
            val nextItem = readItemAtOffset(currentOffset)

            if (globalSchema == null) {
                currentOffset += nextItem.sizeBytes + 2
            } else {
                currentOffset += nextItem.sizeBytes
            }

            return nextItem
        }
    }

    /**
     * Reads the header of the iterable object. Reads the first two bytes and size.
     *
     * @return Where the first elements offset is.
     * @throws XyoObjectIteratorException If the bytes are malformed.
     */
    private fun readOwnHeader () : Int {
        val setHeader = getNextHeader(allowedOffset)
        val totalSize = readSizeOfObject(setHeader.sizeIdentifier, allowedOffset + 2)

        if (!setHeader.isIterable) {
            throw XyoObjectIteratorException("Can not iterate on object that is not iterable. Header " +
                    "${setHeader.header[allowedOffset]}, ${setHeader.header[allowedOffset + 1]}. Value: ${item.toHexString()}")
        }

        if (setHeader.isTyped && totalSize != setHeader.sizeIdentifier) {
            globalSchema = getNextHeader(setHeader.sizeIdentifier + 2 + allowedOffset)
            return 4 + setHeader.sizeIdentifier + allowedOffset
        }

        return 2 + setHeader.sizeIdentifier + allowedOffset
    }

    /**
     * Converts the current iterator to a JSON string.
     *
     * @return A JSON encoded string.
     */
    override fun toString(): String {
        val rootJsonObject = JSONArray()

        if (schema.isIterable) {
            for (subItem in iterator) {
                rootJsonObject.put(JSONArray(object : XyoIterableObject() {
                    override val allowedOffset: Int
                        get() = 0

                    override var item: ByteArray = subItem.bytesCopy
                }.toString()))
            }
        } else {
            rootJsonObject.put(item.toHexString())
        }

        return rootJsonObject.toString()
    }

    companion object {

        /**
         * Converts an array of objects to a single type.
         *
         * @param array The array of buffs to convert.
         * @param type The type to convert it two.
         * @throws XyoObjectException if the type can not be converted (wrong IDs).
         */
        fun convertObjectsToType (array : Array<XyoBuff>, type: XyoObjectSchema) : Array<XyoBuff> {
            val newValues = ArrayList<XyoBuff>()

            for (value in array) {
                if (value.schema.id != type.id) {
                    throw XyoObjectException("Can not convert types! ${value.schema.id}, ${type.id}")
                }

                newValues.add(XyoBuff.newInstance(type, value.valueCopy))
            }

            return newValues.toTypedArray()
        }

        /**
         * Creates an untyped array. (An array that can contain different types of objects)
         *
         * @param schema The schema of the array to encode.
         * @param values The values to encode into the typed set.
         * @throws XyoObjectException If the bytes are malformed.
         * @return The iterable object.
         */
        fun createUntypedIterableObject (schema: XyoObjectSchema, values: Array<XyoBuff>) : XyoIterableObject {
            if (schema.isTyped) {
                throw XyoObjectException("Can not create untyped object from typed schema!")
            }

            var totalSize = 0

            for (item in values) {
                totalSize += item.sizeBytes + 2
            }

            val buffer = ByteBuffer.allocate(totalSize)

            for (item in values) {
                buffer.put(item.bytesCopy)
            }

            return object : XyoIterableObject() {
                override val allowedOffset: Int
                    get() = 0

                override var item: ByteArray = XyoBuff.newInstance(schema, buffer.array()).bytesCopy
            }
        }

        /**
         * Creates an typed array. (An array that can only contain a single type of object).
         *
         * @param schema The schema of the array to encode.
         * @param values The values to encode into the typed set.
         * @throws XyoObjectException If the bytes are malformed.
         * @return The iterable object.
         */
        fun createTypedIterableObject (schema: XyoObjectSchema, values: Array<XyoBuff>) : XyoIterableObject {
            if (!schema.isTyped) {
                throw XyoObjectException("Can not create typed object from untyped schema!")
            }

            var totalSize = 2

            if (values.isEmpty()) {
                totalSize = 0
            }

            for (item in values) {
                totalSize += item.sizeBytes
            }

            val buffer = ByteBuffer.allocate(totalSize)
            if (values.isNotEmpty()) {
                buffer.put(values[0].bytesCopy.copyOfRange(0, 2))

                for (item in values) {
                    buffer.put(item.bytesCopy.copyOfRange(2, item.sizeBytes + 2))
                }
            }

            return object : XyoIterableObject() {
                override val allowedOffset: Int = 0
                override var item: ByteArray = XyoBuff.getObjectEncoded(schema, buffer.array())
                override val schema: XyoObjectSchema = schema
                override val count: Int = values.count()

                override val iterator: Iterator<XyoBuff>
                    get() = values.iterator()

                override fun get(index: Int): XyoBuff {
                    return values[index]
                }
            }
        }
    }
}