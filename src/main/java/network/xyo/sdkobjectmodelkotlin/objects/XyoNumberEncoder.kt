package network.xyo.sdkobjectmodelkotlin.objects

import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import java.nio.ByteBuffer

object XyoNumberEncoder {
    fun createSize (sizeOfItem : Int, sizeOfSize : Int) : ByteArray {
        val basBuffer = ByteBuffer.allocate(sizeOfSize)
        when (sizeOfSize) {
            1 -> basBuffer.put((sizeOfItem + 1).toByte())
            2 -> basBuffer.putShort((sizeOfItem + 2).toShort())
            4 -> basBuffer.putInt((sizeOfItem + 4))
            else -> throw Exception("Not a supported size: $sizeOfItem.")
        }

        return basBuffer.array()
    }
}