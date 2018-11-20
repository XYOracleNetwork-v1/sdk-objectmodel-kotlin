package network.xyo.sdkobjectmodelkotlin.schema

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoSchemaException
import org.json.JSONObject
import java.math.BigInteger

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
    @ExperimentalUnsignedTypes
    private val sizeIdentifierByte : UByte
        get() {
            when (sizeIdentifier) {
                1 -> return (0x00.toUByte())
                2 -> return (0x40.toUByte())
                4 -> return (0x80.toUByte())
                8 -> return (0xC0.toUByte())
            }
            throw XyoSchemaException("Invalid Size $sizeIdentifier")
        }

    /**
     * The 3rd most significant bit that represent sif the object is iterable. This value is obtained from
     * isIterable.
     */
    @ExperimentalUnsignedTypes
    private val iterableByte : UByte
        get() {
            if (isIterable) {
                return 0x20.toUByte()
            }

            return 0x00.toUByte()
        }

    /**
     * The 4th most significant bit that represents if the following object is typed.
     */
    @ExperimentalUnsignedTypes
    private val typedByte : UByte
        get() {
            if (isTyped) {
                return 0x00.toUByte() or (0x10.toUByte())
            }

            return 0x00.toUByte()
        }

    /**
     * The first byte of the object. This value contains the sizeIdentifierByte, the iterableByte, the typedByte, and
     * four reserved bits (4 least significant bits).
     */
    @ExperimentalUnsignedTypes
    val encodingCatalogue : UByte
        get() {
            return sizeIdentifierByte or iterableByte or typedByte
        }

    /**
     * The header of the schema or object, with the first byte being the encodingCatalogue, and the second catalogue ]
     * being the ID of the object.
     */
    @ExperimentalUnsignedTypes
    val header : ByteArray
        get() {
            return byteArrayOf(encodingCatalogue.toByte(), id)
        }

    companion object {

        /**
         * This method creates a schema object with given header.
         *
         * @throws XyoSchemaException when uByteArray.size != 2
         * @param uByteArray The byte array of the header. Obtained from a schema object.header
         * @return A schema describing the object.
         */
        @ExperimentalUnsignedTypes
        fun createFromHeader (byteArray: ByteArray) : XyoObjectSchema {
            return object : XyoObjectSchema() {
                override val id: Byte
                    get() = byteArray[1]

                override val isIterable: Boolean
                    get() = readIsIterable(byteArray[0].toUByte())

                override val isTyped: Boolean
                    get() = readIsTyped(byteArray[0].toUByte())

                override val meta: XyoObjectSchemaMeta? = null

                override val sizeIdentifier: Int
                    get() = readSizeIdentifierFromEncodingCatalogue(byteArray[0].toUByte())
            }
        }


        /**
         * Checks if the encodingCatalogue is typed. The 3rd most significant bit.
         *
         * @param encodingCatalogue The encodingCatalogue
         * @return A bool if the object is typed.
         */
        @ExperimentalUnsignedTypes
        private fun readIsTyped (encodingCatalogue: UByte) : Boolean {
            return (encodingCatalogue and 0x10.toUByte()).toInt() != 0
        }


        /**
         * Checks if the object is iterable. The 4th most significant bit.
         *
         * @param encodingCatalogue The encodingCatalogue
         * @return A bool if the object is iterable.
         */
        @ExperimentalUnsignedTypes
        private fun readIsIterable (encodingCatalogue: UByte) : Boolean {
           return (encodingCatalogue and 0x20.toUByte()).toInt() != 0
        }


        /**
         * Checks the size identifier from the encodingCatalogue. The 2 most significant bits.
         *
         * @param encodingCatalogue The encodingCatalogue.
         * @return Either 1, 2, 4, or 8.
         * @throws XyoSchemaException
         */
        @ExperimentalUnsignedTypes
        private fun readSizeIdentifierFromEncodingCatalogue (encodingCatalogue: UByte) : Int {

            // masking the first two bits to get the result
            // 0xC0 == 11000000
            if (encodingCatalogue and 0xC0.toUByte() == 0x00.toUByte()) {
                return 1
            }

            if (encodingCatalogue and 0xC0.toUByte() == 0x40.toUByte()) {
                return 2
            }

            if (encodingCatalogue and 0xC0.toUByte() == 0x80.toUByte()) {
                return 4
            }

            if (encodingCatalogue and 0xC0.toUByte() == 0xC0.toUByte()) {
                return 8
            }

            throw XyoSchemaException("Invalid Size: ${encodingCatalogue.toString(2)}")
        }

        /**
         * Creates a schema from a json schema.
         *
         * @param string The json object in string form.
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
         *
         * @param string The encoded byte. For example, 0x04
         * @return UByte 0x04 -> 4(UByte)
         */
        private fun stringToByte(string: String) : Byte {
            return BigInteger(string.removeRange(0, 2), 16).toByteArray()[0]
        }

        /**
         * Gets a schema meta object from a json object.
         *
         * @param jsonObject The json object to read the meta from
         * @return XyoObjectSchemaMeta The meta schema
         */
        private fun getMetaFromJsonObject (jsonObject: JSONObject) : XyoObjectSchemaMeta? {
            return object : XyoObjectSchemaMeta() {
                override val desc: String? = jsonObject["desc"] as String?
                override val name: String? = jsonObject["name"] as String?
            }
        }
    }
}