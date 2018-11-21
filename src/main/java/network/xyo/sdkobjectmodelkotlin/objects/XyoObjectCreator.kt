package network.xyo.sdkobjectmodelkotlin.objects

import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.ByteBuffer


/**
 * An object for creating Xyo Objects.
 */
object XyoObjectCreator {

    /**
     * Creates a Xyo Object with a schema and a value.
     */
    fun createObject (schema : XyoObjectSchema, value : ByteArray) : ByteArray {
        val buffer = ByteBuffer.allocate(value.size + schema.sizeIdentifier + 2)
        buffer.put(schema.header)
        buffer.put(XyoNumberEncoder.createSize(value.size, schema.sizeIdentifier))
        buffer.put(value)
        return buffer.array()
    }

    /**
     * Gets the best size of size to use.
     */
    fun getSmartSize (sizeOfItem : Int) : Int {
        if (sizeOfItem + 1 <= Byte.MAX_VALUE) {
            return 1
        }

        if (sizeOfItem + 2 <= Short.MAX_VALUE) {
            return 2
        }

        if (sizeOfItem + 4 <= Int.MAX_VALUE) {
            return 4
        }

        return 8
    }


    fun getObjectValue (item : ByteArray) : ByteArray {
        val objectSchema = XyoObjectSchema.createFromHeader(item.copyOfRange(0, 2))
        return item.copyOfRange(2 + objectSchema.sizeIdentifier, item.size)
    }

}