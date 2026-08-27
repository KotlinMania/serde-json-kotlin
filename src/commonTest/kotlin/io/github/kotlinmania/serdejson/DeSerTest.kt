// port-lint: tests serde_json/tests/test.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeSerTest {
    @Test
    fun testParsePrimitives() {
        assertEquals(Value.Null, fromStr("null").getOrThrow())
        assertEquals(Value.Bool(true), fromStr("true").getOrThrow())
        assertEquals(Value.Bool(false), fromStr("false").getOrThrow())
        assertEquals(Value.Number(JsonNumber.from(123)), fromStr("123").getOrThrow())
        assertEquals(Value.Number(JsonNumber.from(-456)), fromStr("-456").getOrThrow())
        assertEquals(Value.Str("hello world"), fromStr("\"hello world\"").getOrThrow())
    }

    @Test
    fun testParseArray() {
        val parsed = fromStr("[1, 2, 3]").getOrThrow()
        assertTrue(parsed.isArray())
        val arr = parsed.asArray()!!
        assertEquals(3, arr.size)
        assertEquals(Value.Number(JsonNumber.from(1)), arr[0])
        assertEquals(Value.Number(JsonNumber.from(2)), arr[1])
        assertEquals(Value.Number(JsonNumber.from(3)), arr[2])
    }

    @Test
    fun testParseObject() {
        val parsed = fromStr("""{"name": "Alice", "age": 30}""").getOrThrow()
        assertTrue(parsed.isObject())
        val obj = parsed.asObject()!!
        assertEquals(Value.Str("Alice"), obj.get("name"))
        assertEquals(Value.Number(JsonNumber.from(30)), obj.get("age"))
    }

    @Test
    fun testRoundTrip() {
        val original = """{"a":[1,2,true],"b":{"nested":"value"},"c":null}"""
        val parsed = fromStr(original).getOrThrow()
        val serialized = valueToStr(parsed)
        val reparsed = fromStr(serialized).getOrThrow()
        assertEquals(parsed, reparsed)
    }

    @Test
    fun testEscapeStrings() {
        val input = """"Hello\n\t\"World\"""""
        val parsed = fromStr(input).getOrThrow()
        assertEquals(Value.Str("Hello\n\t\"World\""), parsed)
        val serialized = valueToStr(parsed)
        val reparsed = fromStr(serialized).getOrThrow()
        assertEquals(parsed, reparsed)
    }
}
