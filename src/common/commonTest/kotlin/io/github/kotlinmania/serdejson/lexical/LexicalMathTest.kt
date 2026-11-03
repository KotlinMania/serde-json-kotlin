// port-lint: tests lexical/math.rs
package io.github.kotlinmania.serdejson.lexical

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LexicalMathTest {
    private fun fromU32(x: IntArray): MutableList<ULong> {
        val v = ArrayList<ULong>()
        var i = 0
        while (i < x.size) {
            val low = x[i].toUInt().toULong()
            val high = if (i + 1 < x.size) x[i + 1].toUInt().toULong() else 0UL
            if (i + 1 < x.size) {
                v.add((high shl 32) or low)
                i += 2
            } else {
                v.add(low)
                i += 1
            }
        }
        return v
    }

    private fun fromU32Unsigned(x: LongArray): MutableList<ULong> {
        val v = ArrayList<ULong>()
        var i = 0
        while (i < x.size) {
            val low = x[i].toULong()
            val high = if (i + 1 < x.size) x[i + 1].toULong() else 0UL
            if (i + 1 < x.size) {
                v.add((high shl 32) or low)
                i += 2
            } else {
                v.add(low)
                i += 1
            }
        }
        return v
    }

    @Test
    fun testCompare() {
        val x = Bigint(fromU32(intArrayOf(1)))
        val y = Bigint(fromU32(intArrayOf(2)))
        assertTrue(x.compare(y) < 0)
        assertEquals(0, x.compare(x))
        assertTrue(y.compare(x) > 0)

        val x2 = Bigint(fromU32(intArrayOf(5, 1)))
        val y2 = Bigint(fromU32(intArrayOf(2)))
        assertTrue(x2.compare(y2) > 0)
        assertEquals(0, x2.compare(x2))
        assertTrue(y2.compare(x2) < 0)

        val x3 = Bigint(fromU32(intArrayOf(5, 1, 9)))
        val y3 = Bigint(fromU32(intArrayOf(6, 2, 8)))
        assertTrue(x3.compare(y3) > 0)
        assertEquals(0, x3.compare(x3))
        assertTrue(y3.compare(x3) < 0)
    }

    @Test
    fun testBitLength() {
        val x = Bigint(fromU32(intArrayOf(0, 0, 0, 1)))
        assertEquals(97, x.bitLength())

        val x2 = Bigint(fromU32(intArrayOf(0, 0, 0, 3)))
        assertEquals(98, x2.bitLength())

        val x3 = Bigint(fromU32(intArrayOf(1 shl 31)))
        assertEquals(32, x3.bitLength())
    }

    @Test
    fun testIaddSmall() {
        val x = Bigint(fromU32Unsigned(longArrayOf(4294967295L)))
        x.iaddSmall(5UL)
        assertEquals(fromU32(intArrayOf(4, 1)), x.data)

        val x2 = Bigint(fromU32(intArrayOf(5)))
        x2.iaddSmall(7UL)
        assertEquals(fromU32(intArrayOf(12)), x2.data)
    }

    @Test
    fun testImulSmall() {
        val x = Bigint(fromU32(intArrayOf(5)))
        x.imulSmall(7UL)
        assertEquals(fromU32(intArrayOf(35)), x.data)
    }
}
