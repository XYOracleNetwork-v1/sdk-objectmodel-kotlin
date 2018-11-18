package network.xyo.sdkobjectmodelkotlin.objects.sets

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoObjectExceotion
import network.xyo.sdkobjectmodelkotlin.objects.XyoObjectCreator
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.ByteBuffer

@ExperimentalUnsignedTypes
/**
 * An object fir creating sets (arrays).
 */
object XyoObjectSetCreator {

    /**
     * Creates an untyped array. (An array that can contain different types of objects)
     *
     * @param schema The schema for the set.
     * @param values All of the objects for the set.
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

    /**
     * Creates an typed array. (An array that can only contain a single type of object)
     *
     * @param schema The schema for the set.
     * @param values All of the objects for the set.
     */
    fun createTypedIterableObject (schema: XyoObjectSchema, values: Array<ByteArray>) : ByteArray {
        if (!schema.isTyped) {
            throw XyoObjectExceotion("Can not create typed object from untyped schema!")
        }

        var totalSize = 2

        for (item in values) {
            totalSize += item.size - 2
        }

        val buffer = ByteBuffer.allocate(totalSize)
        buffer.put(values[0].copyOfRange(0, 2))

        for (item in values) {
            buffer.put(item.copyOfRange(2, item.size))
        }

        return XyoObjectCreator.createObject(schema, buffer.array())
    }

}