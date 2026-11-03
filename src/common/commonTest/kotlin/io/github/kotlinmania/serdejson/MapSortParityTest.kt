package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.text.CharacterCodingException

class MapSortParityTest {
    private val supplementary = "\uD800\uDC00"
    private val bmp = "\uE000"
    private val maximum = "\uDBFF\uDFFF"
    private val bmpMaximum = "\uFFFF"

    private fun map(keys: List<String>): JsonMap {
        val result = JsonMap()
        for (key in keys) result.insert(key, Value.Str(key))
        return result
    }

    private fun keys(map: JsonMap): List<String> = map.keys().asSequence().toList()

    private fun check(input: List<String>, expected: List<String>) {
        val value = map(input)
        value.sortKeys()
        assertEquals(expected, keys(value))
        for (key in input) assertEquals(key, value.get(key)?.asStr())
    }

    @Test
    fun asciiAndPrefixOrder() {
        check(listOf("b", "a", "aa", ""), listOf("", "a", "aa", "b"))
    }

    @Test
    fun bmpPrecedesSupplementary() {
        check(listOf(supplementary, bmp), listOf(bmp, supplementary))
    }

    @Test
    fun maximumScalarFollowsBmp() {
        check(listOf(maximum, bmpMaximum), listOf(bmpMaximum, maximum))
    }

    @Test
    fun noUnicodeNormalization() {
        check(listOf("é", "e\u0301", "e"), listOf("e", "e\u0301", "é"))
    }

    @Test
    fun supplementaryPrefixOrder() {
        check(listOf(supplementary + "a", supplementary, bmp + "a", bmp), listOf(bmp, bmp + "a", supplementary, supplementary + "a"))
    }

    @Test
    fun recursiveObjectsInsideArrays() {
        val inner = map(listOf(supplementary, bmp))
        val nested = map(listOf(maximum, bmpMaximum))
        inner.insert("array", Value.Array(listOf(Value.Object(nested))))
        Value.Object(inner).sortAllObjects()
        assertEquals(listOf("array", bmp, supplementary), keys(inner))
        assertEquals(listOf(bmpMaximum, maximum), keys(nested))
    }

    @Test
    fun valuesAndRepeatSortArePreserved() {
        val value = map(listOf(supplementary, bmp))
        value.insert(supplementary, Value.Str("replaced"))
        value.sortKeys()
        value.sortKeys()
        assertEquals(listOf(bmp, supplementary), keys(value))
        assertEquals("replaced", value.get(supplementary)?.asStr())
        assertEquals(bmp, value.get(bmp)?.asStr())
    }

    @Test
    fun malformedUtf16IsRejectedBeforeReordering() {
        val value = map(listOf("z", "\uD800", "a"))
        val before = keys(value)
        assertFailsWith<CharacterCodingException> { value.sortKeys() }
        assertEquals(before, keys(value))
    }
}
