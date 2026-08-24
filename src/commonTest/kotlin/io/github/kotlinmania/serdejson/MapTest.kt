// port-lint: tests tests/map.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapTest {
    @Test
    fun testPreserveOrder() {
        val v = fromStr("""{"b":null,"a":null,"c":null}""").getOrThrow()
        val keys = v.asObject()!!.keys()
        assertEquals(listOf("b", "a", "c"), keys)
    }

    @Test
    fun testShiftInsert() {
        val v = fromStr("""{"b":null,"a":null,"c":null}""").getOrThrow()
        val map = v.asObject()!!
        map.shiftInsert(0, "d", Value.Null)
        val keys = map.keys()
        assertEquals(listOf("d", "b", "a", "c"), keys)
    }

    @Test
    fun testInsertAndRemove() {
        val map = JsonMap()
        map.insert("a", Value.Bool(true))
        map.insert("b", Value.Bool(false))
        assertEquals(2, map.len())
        assertTrue(map.containsKey("a"))
        assertTrue(map.containsKey("b"))
        assertEquals(Value.Bool(true), map.get("a"))

        val removed = map.remove("a")
        assertEquals(Value.Bool(true), removed)
        assertEquals(1, map.len())
        assertEquals(listOf("b"), map.keys())
    }

    @Test
    fun testClear() {
        val map = JsonMap()
        map.insert("x", Value.Null)
        map.insert("y", Value.Null)
        assertEquals(2, map.len())
        map.clear()
        assertEquals(0, map.len())
        assertTrue(map.isEmpty())
    }
}
