// port-lint: tests map.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapTest {
    @Test
    fun testPreserveOrder() {
        val expected = listOf("b", "a", "c")

        val v: Value = fromStr("""{"b":null,"a":null,"c":null}""").getOrThrow()
        val keys: List<String> = v.asObject()!!.keys().collect()
        assertEquals(keys, expected)
    }

    @Test
    fun testShiftInsert() {
        val v: Value = fromStr("""{"b":null,"a":null,"c":null}""").getOrThrow()
        val valMap = v.asObjectMut()!!
        valMap.shiftInsert(0, "d", Value.Null)

        val keys: List<String> = valMap.keys().collect()
        assertEquals(keys, listOf("d", "b", "a", "c"))
    }

    @Test
    fun testAppend() {
        val expected = listOf("b", "a", "c")

        val v: Value = fromStr("""{"b":null,"a":null,"c":null}""").getOrThrow()
        val valMap = v.asObjectMut()!!
        val m =
            io.github.kotlinmania.serdejson.JsonMap
                .new()
        m.append(valMap)
        val keys: List<String> = m.keys().collect()

        assertEquals(keys, expected)
        assertTrue(valMap.isEmpty())
    }

    @Test
    fun testRetain() {
        val v: Value = fromStr("""{"b":null,"a":null,"c":null}""").getOrThrow()
        val valMap = v.asObjectMut()!!
        valMap.retain { k, _ -> k != "b" }

        val keys: List<String> = valMap.keys().collect()
        assertEquals(keys, listOf("a", "c"))
    }
}
