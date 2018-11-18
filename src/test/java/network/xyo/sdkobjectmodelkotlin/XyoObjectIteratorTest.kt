package network.xyo.sdkobjectmodelkotlin

import network.xyo.sdkobjectmodelkotlin.objects.sets.XyoObjectIterator
import org.junit.Assert
import org.junit.Test

@ExperimentalUnsignedTypes
class XyoObjectIteratorTest  {

    @Test
    fun testObjectIteratorUntyped () {
        val iterator = XyoObjectIterator(byteArrayOf(0x20, 0x41, 0x09, 0x00, 0x44, 0x02, 0x13, 0x00, 0x42, 0x02, 0x37).toUByteArray())
        var index = 0

        while (iterator.hasNext()) {

            if (index == 0) {
                Assert.assertArrayEquals(iterator.next().toByteArray(), byteArrayOf(0x00, 0x44, 0x02, 0x13))
            }

            if (index == 1) {
                Assert.assertArrayEquals(iterator.next().toByteArray(), byteArrayOf(0x00, 0x42, 0x02, 0x37))
            }

            index++
        }
    }

    @Test
    fun testObjectIteratorTyped () {
        val iterator = XyoObjectIterator(byteArrayOf(0x30, 0x41, 0x07, 0x00, 0x44, 0x02, 0x13, 0x02, 0x37).toUByteArray())
        var index = 0

        while (iterator.hasNext()) {

            if (index == 0) {
                Assert.assertArrayEquals(iterator.next().toByteArray(), byteArrayOf(0x00, 0x44, 0x02, 0x13))
            }

            if (index == 1) {
                Assert.assertArrayEquals(iterator.next().toByteArray(), byteArrayOf(0x00, 0x44, 0x02, 0x37))
            }

            index++
        }
    }
}