// port-lint: tests lib.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibTest {
    @Test
    fun testConvenienceHelpers() {
        val parsed = json("{\"key\":\"value\"}")
        assertTrue(parsed is Value.Object)

        val str = valueToStr(parsed)
        assertEquals("{\"key\":\"value\"}", str)

        val slice = "true".encodeToByteArray()
        val boolVal = fromSlice(slice).getOrThrow()
        assertEquals(Value.Bool(true), boolVal)
    }
}
