package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.serdejson.lexical.generated.shift.__rtkPanic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeneratedShiftAdoptionTest {
    @Test fun exponentOverflowRetainsEarlierMantissaWrite() {
        val right = ExtendedFloat(8uL, Int.MAX_VALUE)
        assertFailsWith<__rtkPanic> { shr(right, 1) }
        assertEquals(4uL, right.mant)
        assertEquals(Int.MAX_VALUE, right.exp)
        val left = ExtendedFloat(3uL, Int.MIN_VALUE)
        assertFailsWith<__rtkPanic> { shl(left, 1) }
        assertEquals(6uL, left.mant)
        assertEquals(Int.MIN_VALUE, left.exp)
    }

    @Test fun invalidCountsRejectBeforeWritingAndWidthCountIsDistinct() {
        val value = ExtendedFloat(ULong.MAX_VALUE, -64)
        assertFailsWith<__rtkPanic> { shr(value, -1) }
        assertEquals(ULong.MAX_VALUE, value.mant)
        assertEquals(-64, value.exp)
        assertFailsWith<__rtkPanic> { shr(value, 64) }
        overflowingShr(value, 64)
        assertEquals(0uL, value.mant)
        assertEquals(0, value.exp)
    }
}
