package io.github.kotlinmania.serdejson.lexical

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratedPowerTablesTest {
    @Test
    fun sourceTableDimensionsAndBoundaryValues() {
        // Recorded from the unchanged Rust sources with fast_arithmetic="64".
        assertEquals(listOf(1, 1, 1, 1, 1, 2, 3, 5, 10, 19, 38, 75, 149, 298), POW5.asSlice().map { it.size })
        assertEquals(28, POW5_64.asSlice().size)
        assertEquals(7450580596923828125uL, POW5_64.asSlice().last())
        assertEquals(20, POW10_64.asSlice().size)
        assertEquals(10000000000000000000uL, POW10_64.asSlice().last())
    }

    @Test
    fun tableViewsCannotMutateFreshConstantValues() {
        val small = POW5_64.asSlice()
        if (small is MutableList<ULong>) small[0] = 999uL
        assertEquals(1uL, POW5_64.asSlice()[0])
        val outer = POW5.asSlice()
        if (outer is MutableList<List<ULong>>) outer[0] = emptyList()
        assertEquals(listOf(5uL), POW5.asSlice()[0])
    }

    @Test
    fun existingMultiplicationConsumesGeneratedTableViews() {
        // Rust oracle: [u64::MAX, 1] * 5^28, little-endian limbs.
        val value = Bigint(mutableListOf(ULong.MAX_VALUE, 1uL))
        value.imulPow5(28)
        assertEquals(listOf(18087329236509514223uL, 718829674400074783uL, 4uL), value.data)
    }
}
