import io.github.kotlinmania.serdejson.lexical.generated.rounding.*
import io.github.kotlinmania.serdejson.lexical.FloatTraits
import io.github.kotlinmania.serdejson.lexical.F32Float
import io.github.kotlinmania.serdejson.lexical.F64Float

private fun generic(width: Int, provider: FloatTraits) {
    val exponents = listOf(Int.MIN_VALUE, Int.MIN_VALUE + 64, provider.denormalExponent - 65,
        provider.denormalExponent - 64, provider.denormalExponent - provider.defaultShift - 1,
        provider.denormalExponent - provider.defaultShift, provider.denormalExponent,
        -1, 0, provider.maxExponent - 1, provider.maxExponent, provider.maxExponent + 1,
        Int.MAX_VALUE - 64, Int.MAX_VALUE)
    for (mant in listOf(0UL, 1UL, 3UL, 0x7fff_ffff_ffff_ffffUL, 0x8000_0000_0000_0000UL, ULong.MAX_VALUE)) {
        for (exp in exponents) for (op in 0..2) for (algorithm in 0..3) {
            val fp = ExtendedFloat(mant, exp)
            var calls = 0
            var observed: Int? = null
            val callback: (ExtendedFloat, Int) -> Unit = { value, shift ->
                calls += 1
                observed = shift
                when (algorithm) {
                    0 -> round_nearest_tie_even(value, shift)
                    1 -> round_downward(value, shift)
                    2 -> Unit
                    else -> { value.mant = 17UL; throw IllegalStateException("callback") }
                }
            }
            val result = try {
                when (op) {
                    0 -> round_to_float(fp, callback, provider)
                    1 -> round_to_native(fp, callback, provider)
                    else -> avoid_overflow(fp, provider)
                }
                "O"
            } catch (_: RuntimeException) { "P" }
            println("generic|$width|$op|$algorithm|$mant|$exp|$result|${fp.mant}|${fp.exp}|$calls|${observed?.let { "Some($it)" } ?: "None"}")
        }
    }
}

fun main() {
    for (n in listOf(0UL, 1UL, 2UL, 23UL, 24UL, 31UL, 32UL, 52UL, 53UL, 63UL, 64UL, 65UL, ULong.MAX_VALUE)) {
        for (op in 0..2) {
            val result = try {
                val value = when (op) { 0 -> nth_bit(n); 1 -> lower_n_mask(n); else -> lower_n_halfway(n) }
                "O|$value"
            } catch (_: RuntimeException) { "P" }
            println("mask|$op|$n|$result")
        }
        for (bit in listOf(0UL, 1UL, 23UL, 32UL, 52UL, 63UL, 64UL, 65UL, ULong.MAX_VALUE)) {
            val result = try { "O|${internal_n_mask(bit, n)}" } catch (_: RuntimeException) { "P" }
            println("internal|$bit|$n|$result")
        }
    }
    for (mant in listOf(0UL, 1UL, 2UL, 3UL, 0x8000_0000_0000_0000UL, ULong.MAX_VALUE)) {
        for (exp in listOf(Int.MIN_VALUE, -64, -1, 0, 1, 64, Int.MAX_VALUE)) {
            for (shift in listOf(Int.MIN_VALUE, -1, 0, 1, 2, 23, 32, 52, 63, 64, 65, Int.MAX_VALUE)) for (op in 0..2) {
                val fp = ExtendedFloat(mant, exp)
                var flags = false to false
                val result = try {
                    when (op) { 0 -> flags = round_nearest(fp, shift); 1 -> round_nearest_tie_even(fp, shift); else -> round_downward(fp, shift) }
                    "O"
                } catch (_: RuntimeException) { "P" }
                println("shift|$op|$mant|$exp|$shift|$result|${fp.mant}|${fp.exp}|${flags.first}|${flags.second}")
            }
            for (above in listOf(false, true)) for (halfway in listOf(false, true)) {
                val fp = ExtendedFloat(mant, exp)
                val result = try { tie_even(fp, above, halfway); "O" } catch (_: RuntimeException) { "P" }
                println("tie|$mant|$exp|$above|$halfway|$result|${fp.mant}|${fp.exp}")
            }
        }
    }
    generic(32, F32Float)
    generic(64, F64Float)
}
