package network.xyo.sdkobjectmodelkotlin.objects.sets

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectExceotion
import network.xyo.sdkobjectmodelkotlin.objects.XyoObjectCreator
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.ByteBuffer

/**
 * An object fir creating sets (arrays).
 */
object XyoObjectSetCreator {

    /**
     * Creates an untyped array. (An array that can contain different types of objects)
     */
    fun createUntypedIterableObject (schema: XyoObjectSchema, values: Array<ByteArray>) : ByteArray {
        if (schema.isTyped) {
            throw XyoObjectExceotion("Can not create untyped object from typed schema!")
        }

        var totalSize = 0

        for (item in values) {
            totalSize += item.size
        }

        val buffer = ByteBuffer.allocate(totalSize)

        for (item in values) {
            buffer.put(item)
        }

        return XyoObjectCreator.createObject(schema, buffer.array())
    }

    fun convertObjectsToType (array : Array<ByteArray>, type: XyoObjectSchema) : Array<ByteArray> {
        val newValues = ArrayList<ByteArray>()

        for (value in array) {
            if (value[1] != type.id) {
                throw XyoObjectExceotion("Can not convert types! ${value[1]}, ${type.id}")
            }

            newValues.add(XyoObjectCreator.createObject(type, XyoObjectCreator.getObjectValue(value)))
        }

        return newValues.toTypedArray()
    }

    /**
     * Creates an typed array. (An array that can only contain a single type of object).
     */
    fun createTypedIterableObject (schema: XyoObjectSchema, values: Array<ByteArray>) : ByteArray {
        if (!schema.isTyped) {
            throw XyoObjectExceotion("Can not create typed object from untyped schema!")
        }

        var totalSize = 2

        if (values.isEmpty()) {
            totalSize = 0
        }

        for (item in values) {
            totalSize += item.size - 2
        }

        val buffer = ByteBuffer.allocate(totalSize)
        if (values.isNotEmpty()) {
            buffer.put(values[0].copyOfRange(0, 2))

            for (item in values) {
                buffer.put(item.copyOfRange(2, item.size))
            }
        }


        return XyoObjectCreator.createObject(schema, buffer.array())
    }

    /**
     * Adds to a set
     */
    fun addToIterableObject (item : ByteArray, set : ByteArray) : ByteArray {
        val setSchema = XyoObjectSchema.createFromHeader(set.copyOfRange(0, 2))
        val setValue = XyoObjectCreator.getObjectValue(set)

        if (!setSchema.isIterable) throw XyoObjectExceotion("Can no add to non-iterable object.")

        if (setSchema.isTyped) {
            val schemaOfType = set.copyOfRange(2 + setSchema.sizeIdentifier, 4 + setSchema.sizeIdentifier)

            if (!schemaOfType.contentEquals(item.copyOfRange(0, 2))) throw XyoObjectExceotion("Can not add different " +
                    "type to typed array!")

            val buffer = ByteBuffer.allocate(setValue.size + item.size - 2)
            buffer.put(setValue)
            buffer.put(item.copyOfRange(2, item.size))
            return XyoObjectCreator.createObject(setSchema, buffer.array())
        }

        val buffer = ByteBuffer.allocate(setValue.size + item.size)
        buffer.put(setValue)
        buffer.put(item)
        return XyoObjectCreator.createObject(setSchema, buffer.array())
    }
}