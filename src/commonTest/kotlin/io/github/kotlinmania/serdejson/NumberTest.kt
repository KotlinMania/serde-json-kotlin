// port-lint: tests serde_json/src/number.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumberTest {
    @Test
    fun testIntNumbers() {
        val n1 = JsonNumber.from(42)
        assertTrue(n1.isI64())
        assertTrue(n1.isU64())
        assertFalse(n1.isF64())
        assertEquals(42L, n1.asI64())
        assertEquals(42UL, n1.asU64())
        assertEquals(42.0, n1.asF64())

        val nNeg = JsonNumber.from(-100L)
        assertTrue(nNeg.isI64())
        assertFalse(nNeg.isU64())
        assertEquals(-100L, nNeg.asI64())
        assertNull(nNeg.asU64())
    }

    @Test
    fun testFloatNumbers() {
        val nf = JsonNumber.from(3.14)!!
        assertTrue(nf.isF64())
        assertFalse(nf.isI64())
        assertFalse(nf.isU64())
        assertEquals(3.14, nf.asF64())

        assertNull(JsonNumber.from(Double.NaN))
        assertNull(JsonNumber.from(Double.POSITIVE_INFINITY))
        assertNull(JsonNumber.from(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun testNumberEquality() {
        val n1 = JsonNumber.from(100)
        val n2 = JsonNumber.from(100L)
        val n3 = JsonNumber.from(100.0)!!
        assertEquals(n1, n2)
        assertEquals(n1, n3)
    }
}
