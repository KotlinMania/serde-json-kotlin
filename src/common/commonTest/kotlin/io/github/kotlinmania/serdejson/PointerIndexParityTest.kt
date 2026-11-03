package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PointerIndexParityTest {
    private fun array(): Value = Value.Array(listOf(Value.Str("zero"), Value.Str("one")))

    @Test
    fun negativeZeroIsRejected() {
        val value = array()
        for (token in listOf("-0", "-00", "-000000")) assertNull(value.pointer("/$token"), token)
    }

    @Test
    fun arabicAndDevanagariDigitsAreRejected() {
        val value = array()
        for (token in listOf("٠", "١", "०", "१")) assertNull(value.pointer("/$token"), token)
    }

    @Test
    fun fullwidthDigitsAreRejected() {
        val value = array()
        for (token in listOf("０", "１")) assertNull(value.pointer("/$token"), token)
    }

    @Test
    fun canonicalArrayIndicesWork() {
        val value = array()
        assertEquals("zero", value.pointer("/0")?.asStr())
        assertEquals("one", value.pointer("/1")?.asStr())
        assertSame(value, value.pointer(""))
    }

    @Test
    fun objectKeysAreNotArrayIndices() {
        val map = JsonMap()
        val keys = listOf("-0", "٠", "१", "０", "01", "+0")
        for (key in keys) map.insert(key, Value.Str(key))
        val value = Value.Object(map)
        for (key in keys) assertEquals(key, value.pointer("/$key")?.asStr())
    }

    @Test
    fun pointerEscapesAndEmptyKeysStillWork() {
        val map = JsonMap()
        for ((key, value) in listOf("a/b" to "slash", "~1" to "tilde", "" to "empty")) map.insert(key, Value.Str(value))
        val value = Value.Object(map)
        for ((pointer, expected) in listOf("/a~1b" to "slash", "/~01" to "tilde", "/" to "empty")) {
            assertEquals(expected, value.pointer(pointer)?.asStr())
        }
        assertNull(value.pointer("a/b"))
    }

    @Test
    fun leadingZeroOverflowAndMalformedIndicesAreRejected() {
        val value = array()
        val tokens = listOf("00", "01", "+0", "", "-1", "-", "2", "2147483648", "18446744073709551615", "18446744073709551616", "999999999999999999999999")
        for (token in tokens) assertNull(value.pointer("/$token"), token)
    }
}
