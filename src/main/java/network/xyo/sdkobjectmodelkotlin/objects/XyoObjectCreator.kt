package network.xyo.sdkobjectmodelkotlin.objects

import network.xyo.sdkobjectmodelkotlin.objects.sets.XyoIterableObject
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import org.json.JSONArray
import org.json.JSONObject
import java.lang.StringBuilder
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
        if (sizeOfItem + 1 <= 255) {
            return 1
        }

        if (sizeOfItem + 2 <= 65535) {
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

    fun itemToJSON (item : ByteArray) : JSONArray {
        val itemHeader = XyoObjectSchema.createFromHeader(item.copyOfRange(0, 2))
        val rootJsonObject = JSONArray()

        if (itemHeader.isIterable) {
            for (subItem in XyoIterableObject(item).iterator) {
                rootJsonObject.put(itemToJSON(subItem))
            }
        } else {
            rootJsonObject.put(item.toHexString())
        }

        return rootJsonObject
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
}