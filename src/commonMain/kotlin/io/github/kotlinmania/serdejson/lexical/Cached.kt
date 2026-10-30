// port-lint: source lexical/cached.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Precalculated powers stored in parallel mantissa and exponent arrays.
 */
internal class ExtendedFloatArray(
    /** Pre-calculated mantissas for the powers. */
    val mant: ULongArray,
    /** Pre-calculated binary exponents for the powers. */
    val exp: IntArray,
) {
    /** Return the [ExtendedFloat] at [index]. */
    fun getExtendedFloat(index: Int): ExtendedFloat = ExtendedFloat(mant[index], exp[index])

    /** Return the number of cached entries. */
    fun len(): Int = mant.size

    /** Length property alias for internal callers. */
    val length: Int
        get() = len()
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
    /** Get a small [ExtendedFloat] power at [index]. */
    fun getSmall(index: Int): ExtendedFloat = small.getExtendedFloat(index)

    /** Get a large [ExtendedFloat] power at [index]. */
    fun getLarge(index: Int): ExtendedFloat = large.getExtendedFloat(index)

    /** Get a small integer power at [index]. */
    fun getSmallInt(index: Int): ULong = smallInt[index]
}

/**
 * Cached powers provider for an extended-precision floating-point type.
 */
internal interface ModeratePathCache {
    /** Get cached powers. */
    fun getPowers(): ModeratePathPowers
}

/**
 * [ModeratePathCache] implementation for [ExtendedFloat].
 */
internal object ExtendedFloatCache : ModeratePathCache {
    override fun getPowers(): ModeratePathPowers =
        io.github.kotlinmania.serdejson.lexical.getPowers()
}
