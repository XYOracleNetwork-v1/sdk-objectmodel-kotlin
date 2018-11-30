package network.xyo.sdkobjectmodelkotlin

import network.xyo.sdkobjectmodelkotlin.objects.XyoObjectCreator
import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import org.junit.Assert
import org.junit.Test
import java.math.BigInteger

class XyoObjectCreatorTest {

//    @Test
//    fun test () {
//        val bytes = BigInteger("A0020000004AA00100000011A0010000000B800E0000000500B0CC0000001EB00700000018A0010000000E8003000000080000000000000004A00100000011A0010000000B800B0000000500", 16).toByteArray()
//        val correctBytes = bytes.copyOfRange(1, bytes.size)
//        println(XyoObjectCreator.itemToByteString(correctBytes,0, true))
//    }

    @Test
    fun testSmartSizeForByte () {
        val sizeOfImageryObject = 254
        val bestWayToEncodeSize = 1
        Assert.assertEquals(bestWayToEncodeSize, XyoObjectCreator.getSmartSize(sizeOfImageryObject))
    }

    @Test
    fun testSmartSizeForShort () {
        val sizeOfImageryObject = 64_000
        val bestWayToEncodeSize = 2
        Assert.assertEquals(bestWayToEncodeSize, XyoObjectCreator.getSmartSize(sizeOfImageryObject))
    }

    @Test
    fun testSmartSizeForInt () {
        val sizeOfImageryObject = 66_000
        val bestWayToEncodeSize = 4
        Assert.assertEquals(bestWayToEncodeSize, XyoObjectCreator.getSmartSize(sizeOfImageryObject))
    }

    @Test
    fun testCreateObject() {
        val schema = object : XyoObjectSchema() {
            override val id: Byte = 0x44
            override val isIterable: Boolean = false
            override val isTyped: Boolean = false
            override val meta: XyoObjectSchemaMeta? = null
            override val sizeIdentifier: Int = 1
        }

        val value = byteArrayOf(0x13)
        val expectedObject = byteArrayOf(0x00, 0x44, 0x02, 0x13)
        val createdObject = XyoObjectCreator.createObject(schema, value)

        Assert.assertArrayEquals(expectedObject, createdObject)
    }

    @Test
    fun getObjectValueTest () {
        val testObject = byteArrayOf(0x00, 0x44, 0x02, 0x13)
        Assert.assertArrayEquals(byteArrayOf(0x13), XyoObjectCreator.getObjectValue(testObject))
    }
}