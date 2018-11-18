package network.xyo.sdkobjectmodelkotlin.exceptions

import java.lang.Exception

/**
 * A base exception for all all XyoObjectIterator related items.
 *
 * @property message The message of the Exception.
 */
open class XyoObjectExceotion (override val message: String?) : Exception()