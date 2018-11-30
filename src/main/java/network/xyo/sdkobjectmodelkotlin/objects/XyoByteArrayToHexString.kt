package network.xyo.sdkobjectmodelkotlin.objects

import java.lang.StringBuilder

fun ByteArray.toHexString(): String {
    val builder = StringBuilder()
    val it = this.iterator()
    builder.append("0x")
    while (it.hasNext()) {
        builder.append(String.format("%02X", it.next()))
    }

    return builder.toString()
}