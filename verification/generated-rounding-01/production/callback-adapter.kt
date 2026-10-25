import io.github.kotlinmania.serdejson.lexical.avoidOverflow
import io.github.kotlinmania.serdejson.lexical.roundToNative
import io.github.kotlinmania.serdejson.lexical.roundToFloat
import io.github.kotlinmania.serdejson.lexical.tieEven
import io.github.kotlinmania.serdejson.lexical.nthBit
import io.github.kotlinmania.serdejson.lexical.lowerNMask
import io.github.kotlinmania.serdejson.lexical.lowerNHalfway
import io.github.kotlinmania.serdejson.lexical.internalNMask
import io.github.kotlinmania.serdejson.lexical.roundDownward
import io.github.kotlinmania.serdejson.lexical.roundNearest
import io.github.kotlinmania.serdejson.lexical.roundNearestTieEven
import io.github.kotlinmania.serdejson.lexical.ExtendedFloat
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
            var callbackState: String? = null
            val callback: (ExtendedFloat, Int) -> Unit = { value, shift ->
                calls += 1
                observed = shift
                check(value === fp)
                callbackState = "(${value.mant}, ${value.exp})"
                when (algorithm) {
                    0 -> roundNearestTieEven(value, shift)
                    1 -> roundDownward(value, shift)
                    2 -> Unit
                    else -> { value.mant = 17UL; throw IllegalStateException("callback") }
                }
            }
            val result = try {
                when (op) {
                    0 -> roundToFloat(fp, provider, callback)
                    1 -> roundToNative(fp, provider, callback)
                    else -> avoidOverflow(fp, provider)
                }
                "O"
            } catch (_: RuntimeException) { "P" }
            println("generic|$width|$op|$algorithm|$mant|$exp|$result|${fp.mant}|${fp.exp}|$calls|${observed?.let { "Some($it)" } ?: "None"}|${callbackState?.let { "Some($it)" } ?: "None"}")
        }
    }
}

fun main() {
    for (n in listOf(0UL, 1UL, 2UL, 23UL, 24UL, 31UL, 32UL, 52UL, 53UL, 63UL, 64UL, 65UL, ULong.MAX_VALUE)) {
        for (op in 0..2) {
            val result = try {
                val value = when (op) { 0 -> nthBit(n); 1 -> lowerNMask(n); else -> lowerNHalfway(n) }
                "O|$value"
            } catch (_: RuntimeException) { "P" }
            println("mask|$op|$n|$result")
        }
        for (bit in listOf(0UL, 1UL, 23UL, 32UL, 52UL, 63UL, 64UL, 65UL, ULong.MAX_VALUE)) {
            val result = try { "O|${internalNMask(bit, n)}" } catch (_: RuntimeException) { "P" }
            println("internal|$bit|$n|$result")
        }
    }
    for (mant in listOf(0UL, 1UL, 2UL, 3UL, 0x8000_0000_0000_0000UL, ULong.MAX_VALUE)) {
        for (exp in listOf(Int.MIN_VALUE, -64, -1, 0, 1, 64, Int.MAX_VALUE)) {
            for (shift in listOf(Int.MIN_VALUE, -1, 0, 1, 2, 23, 32, 52, 63, 64, 65, Int.MAX_VALUE)) for (op in 0..2) {
                val fp = ExtendedFloat(mant, exp)
                var flags = false to false
                val result = try {
                    when (op) { 0 -> flags = roundNearest(fp, shift); 1 -> roundNearestTieEven(fp, shift); else -> roundDownward(fp, shift) }
                    "O"
                } catch (_: RuntimeException) { "P" }
                println("shift|$op|$mant|$exp|$shift|$result|${fp.mant}|${fp.exp}|${flags.first}|${flags.second}")
            }
            for (above in listOf(false, true)) for (halfway in listOf(false, true)) {
                val fp = ExtendedFloat(mant, exp)
                val result = try { tieEven(fp, above, halfway); "O" } catch (_: RuntimeException) { "P" }
                println("tie|$mant|$exp|$above|$halfway|$result|${fp.mant}|${fp.exp}")
            }
        }
    }
    generic(32, F32Float)
    generic(64, F64Float)
}
