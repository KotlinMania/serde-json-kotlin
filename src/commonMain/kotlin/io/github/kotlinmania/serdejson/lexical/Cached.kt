// port-lint: source serde_json/src/lexical/cached.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

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
    /** Get the [ExtendedFloat] at [index] without bounds checking. */
    fun getExtendedFloat(index: Int): ExtendedFloat =
        ExtendedFloat(mant[index], exp[index])

    /** Length of the cached arrays. */
    val length: Int get() = mant.size
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
    /** Get a small [ExtendedFloat] at [index]. */
    fun getSmall(index: Int): ExtendedFloat = small.getExtendedFloat(index)

    /** Get a large [ExtendedFloat] at [index]. */
    fun getLarge(index: Int): ExtendedFloat = large.getExtendedFloat(index)

    /** Get a small integer power at [index]. */
    fun getSmallInt(index: Int): ULong = smallInt[index]
}

/**
 * Cached powers as a trait for a floating-point type.
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
