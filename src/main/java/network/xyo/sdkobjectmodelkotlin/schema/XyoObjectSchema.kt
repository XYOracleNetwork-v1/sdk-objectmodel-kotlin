package network.xyo.sdkobjectmodelkotlin.schema

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoSchemaException
import org.json.JSONObject
import java.math.BigInteger
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * A information class used to represent all identifying factors in the XyoObjectModel. This is typically represented
 * as the first two bytes in an object (The encoding catalogue and the ID). You can create a XyoObjectSchema from an
 * objects's header (first 2 bytes), or from a JSON schema.
 *
 */
abstract class XyoObjectSchema {
    /**
     * The ID id the the object
     */
    abstract val id : Byte

    /**
     * The size of the size indicator. This value can be 1, 2, 4, 8, or null. If the value is null, this value will be
     * chosen to optimised the size of the object. If the value is not 1, 2, 4, 8, or null, will through a
     * XyoSchemaException.
     */
    abstract val sizeIdentifier : Int

    /**
     * If the bytes in the object are iterable.
     */
    abstract val isIterable : Boolean

    /**
     * If the bytes in the typed object are iterable and unique.
     */
    abstract val isTyped : Boolean

    /**
     * A meta class to store information about the schema.
     */
    abstract val meta : XyoObjectSchemaMeta?

    /**
     * A meta class to store information about the schema.
     */
    abstract class XyoObjectSchemaMeta {
        /**
         * The name of the schema.
         */
        abstract val name : String?

        /**
         * The description of the schema.
         */
        abstract val desc : String?
    }

    /**
     * The 2 most significant bits (big endian) that represent 1, 2, 4, or 8. This value is obtained from the
     * sizeIdentifier. If the sizeIdentifier does not conform to these values, a XyoSchemaException will be
     * thrown.
     *
     * @throws XyoSchemaException when the sizeIdentifier is not 1, 2, 4, 8, or null
     */
    private val sizeIdentifierByte : Byte
        get() {
            when (sizeIdentifier) {
                1 -> return (0x00)
                2 -> return (0x40)
                4 -> return (0x80.toByte())
                8 -> return (0xC0.toByte())
            }
            throw XyoSchemaException("Invalid Size $sizeIdentifier")
        }

    /**
     * The 3rd most significant bit that represent sif the object is iterable. This value is obtained from
     * isIterable.
     */
    private val iterableByte : Byte
        get() {
            if (isIterable) {
                return 0x20
            }

            return 0x00
        }

    /**
     * The 4th most significant bit that represents if the following object is typed.
     */
    private val typedByte : Byte
        get() {
            if (isTyped) {
                return (0x00.toByte() or 0x10.toByte())
            }

            return 0x00.toByte()
        }

    /**
     * The first byte of the object. This value contains the sizeIdentifierByte, the iterableByte, the typedByte, and
     * four reserved bits (4 least significant bits).
     */
    val encodingCatalogue : Byte
        get() {
            return sizeIdentifierByte or iterableByte or typedByte
        }

    /**
     * The header of the schema or object, with the first byte being the encodingCatalogue, and the second catalogue ]
     * being the ID of the object.
     */
    val header : ByteArray
        get() {
            return byteArrayOf(encodingCatalogue, id)
        }

    companion object {

        //  This method creates a schema object with given header.
        fun createFromHeader (byteArray: ByteArray) : XyoObjectSchema {
            if (byteArray.size != 2) {
                throw XyoSchemaException("Expected header size to be 2, saw: ${byteArray.size}")
            }

            return object : XyoObjectSchema() {
                override val id: Byte
                    get() = byteArray[1]

                override val isIterable: Boolean
                    get() = readIsIterable(byteArray[0])

                override val isTyped: Boolean
                    get() = readIsTyped(byteArray[0])

                override val meta: XyoObjectSchemaMeta? = null

                override val sizeIdentifier: Int
                    get() = readSizeIdentifierFromEncodingCatalogue(byteArray[0])
            }
        }


        /**
         * Checks if the encodingCatalogue is typed. The 3rd most significant bit.
         */
        private fun readIsTyped (encodingCatalogue: Byte) : Boolean {
            return (encodingCatalogue and 0x10).toInt() != 0
        }

        /**
         * Checks if the object is iterable. The 4th most significant bit.
         */
        private fun readIsIterable (encodingCatalogue: Byte) : Boolean {
           return (encodingCatalogue and 0x20).toInt() != 0
        }

        /**
         * Checks the size identifier from the encodingCatalogue. The 2 most significant bits.
         */
        private fun readSizeIdentifierFromEncodingCatalogue (encodingCatalogue: Byte) : Int {

            // masking the first two bits to get the result
            // 0xC0 == 11000000
            if (encodingCatalogue and 0xC0.toByte() == 0x00.toByte()) {
                return 1
            }

            if (encodingCatalogue and 0xC0 .toByte() == 0x40.toByte()) {
                return 2
            }

            if (encodingCatalogue and 0xC0.toByte() == 0x80.toByte()) {
                return 4
            }

            if (encodingCatalogue and 0xC0.toByte() == 0xC0.toByte()) {
                return 8
            }

            throw XyoSchemaException("Invalid Size: ${encodingCatalogue.toString(2)}")
        }

        /**
         * Creates a schema from a json schema.
         */
        fun fromJson(string: String) : XyoObjectSchema {
            val jsonObject = JSONObject(string)
            val id = jsonObject["id"] as String
            val sizeIdentifier = jsonObject["sizeIdentifier"] as Int
            val isIterator = jsonObject["isIterable"] as Boolean
            val isTyped = jsonObject["isTyped"] as Boolean
            val meta = jsonObject["meta"] as JSONObject

            return object : XyoObjectSchema() {
                override val id: Byte = stringToByte(id)
                override val isIterable: Boolean = isIterator
                override val isTyped: Boolean = isTyped
                override val meta: XyoObjectSchemaMeta? = getMetaFromJsonObject(meta)
                override val sizeIdentifier: Int = sizeIdentifier
            }
        }

        /**
         * Gets the string encoded byte to a UByte
         */
        private fun stringToByte(string: String) : Byte {
            return BigInteger(string.removeRange(0, 2), 16).toByteArray()[0]
        }

        /**
         * Gets a schema meta object from a json object.
         */
        private fun getMetaFromJsonObject (jsonObject: JSONObject) : XyoObjectSchemaMeta? {
            return object : XyoObjectSchemaMeta() {
                override val desc: String? = jsonObject["desc"] as String?
                override val name: String? = jsonObject["name"] as String?
            }
        }
    }
}