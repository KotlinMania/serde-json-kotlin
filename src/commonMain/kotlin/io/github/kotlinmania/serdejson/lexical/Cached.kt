// Integration adapter for the unchanged generated lexical/cached.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.serdejson.lexical.generated.cached.ExtendedFloatArray as GeneratedArray
import io.github.kotlinmania.serdejson.lexical.generated.cached.ModeratePathPowers as GeneratedPowers
import io.github.kotlinmania.serdejson.lexical.generated.cached.get_extended_float
import io.github.kotlinmania.serdejson.lexical.generated.cached.get_small
import io.github.kotlinmania.serdejson.lexical.generated.cached.get_large
import io.github.kotlinmania.serdejson.lexical.generated.cached.get_small_int
import io.github.kotlinmania.serdejson.lexical.generated.cached.len

/**
 * Cached powers for extended-precision floats.
 */

/**
 * Precalculated powers that uses two-separate arrays for memory-efficiency.
 */
internal class ExtendedFloatArray(
    /** Pre-calculated mantissa for the powers. */
    val mant: ULongArray,
    /** Pre-calculated binary exponents for the powers. */
    val exp: IntArray,
) {
    // Primitive-array views preserve the existing internal adapter API. Owners
    // must keep cached table storage immutable while the view is retained.
    internal val generated = GeneratedArray(mant.asList(), exp.asList())

    /** Get the [ExtendedFloat] at [index] with the source's bounds checks. */
    fun getExtendedFloat(index: Int): ExtendedFloat =
        generated.get_extended_float(index.toULong()).let { ExtendedFloat(it.mant, it.exp) }

    /** Length of the cached arrays. */
    val length: Int get() = generated.len().toInt()
}

/**
 * Precalculated powers of base N for the moderate path.
 */
internal class ModeratePathPowers(
    /** Pre-calculated small powers. */
    val small: ExtendedFloatArray,
    /** Pre-calculated large powers. */
    val large: ExtendedFloatArray,
    /** Pre-calculated small powers as 64-bit integers. */
    val smallInt: ULongArray,
    /** Step between large powers and number of small powers. */
    val step: Int,
    /** Exponent bias for the large powers. */
    val bias: Int,
) {
    private val generated = GeneratedPowers(small.generated, large.generated, smallInt.asList(), step, bias)

    /** Get a small [ExtendedFloat] at [index]. */
    fun getSmall(index: Int): ExtendedFloat =
        generated.get_small(index.toULong()).let { ExtendedFloat(it.mant, it.exp) }

    /** Get a large [ExtendedFloat] at [index]. */
    fun getLarge(index: Int): ExtendedFloat =
        generated.get_large(index.toULong()).let { ExtendedFloat(it.mant, it.exp) }

    /** Get a small integer power at [index]. */
    fun getSmallInt(index: Int): ULong = generated.get_small_int(index.toULong())
}

/**
 * Cached powers as an interface for a floating-point type.
 */
internal interface ModeratePathCache {
    /** Get cached powers. */
    fun getPowers(): ModeratePathPowers
}

/**
 * [ModeratePathCache] implementation for [ExtendedFloat].
 */
internal object ExtendedFloatCache : ModeratePathCache {
    override fun getPowers(): ModeratePathPowers = CachedFloat80.getPowers()
}
