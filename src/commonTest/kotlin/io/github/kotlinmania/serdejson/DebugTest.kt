// port-lint: tests serde_json/src/debug.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DebugTest {
    @Test
    fun testNumber() {
        assertEquals("1", JsonNumber.from(1).toString())
        assertEquals("-1", JsonNumber.from(-1).toString())
        assertEquals("1.0", JsonNumber.from(1.0)!!.toString())
    }

    @Test
    fun testValueNull() {
        assertEquals("null", Value.Null.toString())
    }

    @Test
    fun testValueBool() {
        assertEquals("true", Value.Bool(true).toString())
        assertEquals("false", Value.Bool(false).toString())
    }

    @Test
    fun testValueNumber() {
        assertEquals("1", Value.Number(JsonNumber.from(1)).toString())
        assertEquals("-1", Value.Number(JsonNumber.from(-1)).toString())
        assertEquals("1.0", Value.Number(JsonNumber.from(1.0)!!).toString())
    }

    @Test
    fun testValueString() {
        assertEquals("\"s\"", Value.Str("s").toString())
    }

    @Test
    fun testValueArray() {
        assertEquals("[]", Value.Array(emptyList()).toString())
    }

    @Test
    fun testValueObject() {
        assertEquals("{}", Value.Object(JsonMap()).toString())
    }

    @Test
    fun testParseError() {
        val err = fromStr("{0}")
        assertEquals(true, err.isFailure)
    }

    @Test
    fun testIndented() {
        val map = JsonMap()
        map.insert("Array", Value.Array(listOf(Value.Bool(true))))
        map.insert("Bool", Value.Bool(true))
        map.insert("EmptyArray", Value.Array(emptyList()))
        map.insert("EmptyObject", Value.Object(JsonMap()))
        map.insert("Null", Value.Null)
        map.insert("Number", Value.Number(JsonNumber.from(1)))
        map.insert("String", Value.Str("..."))
        val j = Value.Object(map)
        val pretty = j.toPrettyString()
        assertNotNull(pretty)
    }
}
