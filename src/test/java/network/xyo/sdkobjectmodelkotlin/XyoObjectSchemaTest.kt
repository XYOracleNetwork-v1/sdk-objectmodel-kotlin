package network.xyo.sdkobjectmodelkotlin

import network.xyo.sdkobjectmodelkotlin.schema.XyoObjectSchema
import org.junit.Assert
import org.junit.Test

@ExperimentalUnsignedTypes
class XyoObjectSchemaTest {

    @Test
    fun testEncodingCatalogue () {
        val testSchema = object : XyoObjectSchema() {
            override val id: UByte = 0x13.toUByte()
            override val isIterable: Boolean = true
            override val isTyped: Boolean = false
            override val sizeIdentifier: Int = 2
            override val meta: XyoObjectSchemaMeta? = null
        }

        val encodingCatalogue = testSchema.encodingCatalogue

        Assert.assertEquals(0x60.toUByte() /* 01100000 */, encodingCatalogue.toUByte())
    }

    @Test
    fun testHeader () {
        val testSchema = object : XyoObjectSchema() {
            override val id: UByte = 0x11.toUByte()
            override val isIterable: Boolean = true
            override val isTyped: Boolean = false
            override val sizeIdentifier: Int = 2
            override val meta: XyoObjectSchemaMeta? = null
        }

        val encodingCatalogue = testSchema.header

        Assert.assertArrayEquals(ubyteArrayOf(0x60.toUByte() /* 01100000 */, 0x11.toUByte()).toByteArray(), encodingCatalogue.toByteArray())
    }

    @Test
    fun testCreateSchemaFromHeader () {
        val testSchema = object : XyoObjectSchema() {
            override val id: UByte = 0x12.toUByte()
            override val isIterable: Boolean = true
            override val isTyped: Boolean = false
            override val sizeIdentifier: Int = 2
            override val meta: XyoObjectSchemaMeta? = null
        }

        val recreatedTestSchema = XyoObjectSchema.createFromHeader(testSchema.header)

        Assert.assertArrayEquals(testSchema.header.toByteArray(), recreatedTestSchema.header.toByteArray())
    }
}