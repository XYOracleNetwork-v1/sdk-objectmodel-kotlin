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

    @Test
    fun testCreateFromSchema () {
        val schema = XyoObjectSchema.fromJson("{\n" +
                "\t\"id\": \"0x1b\",\n" +
                "\t\"sizeIdentifier\": 1,\n" +
                "\t\"isIterable\": false,\n" +
                "\t\"isTyped\": false,\n" +
                "\t\"meta\": {\n" +
                "\t\t\"name\": \"Stub Signature.\",\n" +
                "\t\t\"desc\": \"Stub Signature for testing.\",\n" +
                "\t\t\"childs\": [\n" +
                "\t\t\t\n" +
                "\t\t]\n" +
                "\t}\n" +
                "}")


        Assert.assertEquals(0x1b.toUByte(), schema.id)
        Assert.assertEquals(1, schema.sizeIdentifier)
        Assert.assertFalse(schema.isIterable)
        Assert.assertFalse(schema.isTyped)
        Assert.assertEquals("Stub Signature.", schema.meta?.name)
        Assert.assertEquals("Stub Signature for testing.", schema.meta?.desc)


    }
}