package network.xyo.sdkobjectmodelkotlin.schema

import network.xyo.sdkobjectmodelkotlin.exceptions.XyoSchemaException

@ExperimentalUnsignedTypes
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
    abstract val id : UByte

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
    private val sizeIdentifierByte : UByte
        get() {
            when (sizeIdentifier) {
                1 -> return 0x00.toUByte() or (0x00.toUByte())
                2 -> return 0x00.toUByte() or (0x40.toUByte())
                4 -> return 0x00.toUByte() or (0x80.toUByte())
                8 -> return 0x00.toUByte() or (0xC0.toUByte())
            }
            throw XyoSchemaException("Invalid Size $sizeIdentifier")
        }

    /**
     * The 3rd most significant bit that represent sif the object is iterable. This value is obtained from
     * isIterable.
     */
    private val iterableByte : UByte
        get() {
            if (isIterable) {
                return 0x00.toUByte() or (0x20.toUByte())
            }

            return 0x00.toUByte()
        }

    /**
     * The 4th most significant bit that represents if the following object is typed.
     */
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
    val encodingCatalogue : UByte
        get() {
            return sizeIdentifierByte or iterableByte or typedByte
        }

    /**
     * The header of the schema or object, with the first byte being the encodingCatalogue, and the second catalogue ]
     * being the ID of the object.
     */
    val header : UByteArray
        get() {
            return ubyteArrayOf(encodingCatalogue, id.toUByte())
        }

    companion object {

        /**
         * This method creates a schema object with given header.
         *
         * @throws XyoSchemaException when uByteArray.size != 2
         * @param uByteArray The byte array of the header. Obtained from a schema object.header
         * @return A schema describing the object.
         */
        fun createFromHeader (uByteArray: UByteArray) : XyoObjectSchema {
            return object : XyoObjectSchema() {
                override val id: UByte
                    get() = uByteArray[1]

                override val isIterable: Boolean
                    get() = readIsIterable(uByteArray[0])

                override val isTyped: Boolean
                    get() = readIsTyped(uByteArray[0])

                override val meta: XyoObjectSchemaMeta? = null

                override val sizeIdentifier: Int
                    get() = readSizeIdentifierFromEncodingCatalogue(uByteArray[0])
            }
        }


        /**
         * Checks if the encodingCatalogue is typed. The 3rd most significant bit.
         *
         * @param encodingCatalogue The encodingCatalogue
         * @return A bool if the object is typed.
         */
        private fun readIsTyped (encodingCatalogue: UByte) : Boolean {
            return (encodingCatalogue and 0x10.toUByte()).toInt() != 0
        }


        /**
         * Checks if the object is iterable. The 4th most significant bit.
         *
         * @param encodingCatalogue The encodingCatalogue
         * @return A bool if the object is iterable.
         */
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
        private fun readSizeIdentifierFromEncodingCatalogue (encodingCatalogue: UByte) : Int {

            if ((encodingCatalogue and 0x80.toUByte()).toInt() != 0) {
                return 4
            }

            if ((encodingCatalogue and 0x40.toUByte()).toInt() != 0) {
                return 2
            }

            if ((encodingCatalogue and 0xC0.toUByte()).toInt() == 0) {
                return 1
            }

            if ((encodingCatalogue and 0xC0.toUByte()).toInt() != 0) {
                return 8
            }

            throw XyoSchemaException("Invalid Size: ${encodingCatalogue.toString(2)}")
        }
    }
}