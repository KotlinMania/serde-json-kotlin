// port-lint: tests test.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValueTest {
    @Test
    fun testValueTypePredicates() {
        val nullVal = Value.Null
        assertTrue(nullVal.isNull())
        assertFalse(nullVal.isBoolean())
        assertNull(nullVal.asBool())

        val boolVal = Value.Bool(true)
        assertTrue(boolVal.isBoolean())
        assertEquals(true, boolVal.asBool())
        assertFalse(boolVal.isNumber())

        val numVal = Value.Number(JsonNumber.from(42))
        assertTrue(numVal.isNumber())
        assertTrue(numVal.isI64())
        assertEquals(42L, numVal.asI64())
        assertEquals(42.0, numVal.asF64())

        val strVal = Value.Str("hello")
        assertTrue(strVal.isString())
        assertEquals("hello", strVal.asStr())

        val arrVal = Value.Array(listOf(Value.Number(JsonNumber.from(1)), Value.Number(JsonNumber.from(2))))
        assertTrue(arrVal.isArray())
        assertEquals(2, arrVal.asArray()?.size)
        assertEquals(Value.Number(JsonNumber.from(1)), arrVal.get(0))
        assertEquals(Value.Number(JsonNumber.from(2)), arrVal.get(1))
        assertNull(arrVal.get(2))

        val map = JsonMap()
        map.insert("key", Value.Str("val"))
        val objVal = Value.Object(map)
        assertTrue(objVal.isObject())
        assertEquals(Value.Str("val"), objVal.get("key"))
        assertNull(objVal.get("missing"))
    }

    @Test
    fun testValueEquality() {
        assertEquals(Value.Null, Value.Null)
        assertEquals(Value.Bool(true), Value.Bool(true))
        assertEquals(Value.Str("abc"), Value.Str("abc"))
        assertEquals(Value.Number(JsonNumber.from(10)), Value.Number(JsonNumber.from(10)))

        val map1 = JsonMap()
        map1.insert("a", Value.Number(JsonNumber.from(1)))
        val map2 = JsonMap()
        map2.insert("a", Value.Number(JsonNumber.from(1)))
        assertEquals(Value.Object(map1), Value.Object(map2))
    }

    @Test
    fun testPointer() {
        val parsed = fromStr("""{"a":{"b":[10,20,30]}}""").getOrThrow()
        assertEquals(Value.Number(JsonNumber.from(20)), parsed.pointer("/a/b/1"))
        assertEquals(Value.Number(JsonNumber.from(10)), parsed.pointer("/a/b/0"))
        assertNull(parsed.pointer("/a/b/99"))
        assertNull(parsed.pointer("/nonexistent"))
    }
}
