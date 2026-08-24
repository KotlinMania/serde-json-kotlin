// port-lint: source tmp/serde_json/src/lexical/rounding.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Defines rounding schemes for floating-point numbers.
 */

private const val BITS: Int = 64 // size of ULong in bits

// MASKS

/**
 * Calculate a scalar factor of 2 above the halfway point.
 */
internal fun nthBit(n: ULong): ULong {
    require(n < BITS.toULong()) { "nthBit() overflow in shl." }
    return 1UL shl n.toInt()
}

/**
 * Generate a bitwise mask for the lower [n] bits.
 */
internal fun lowerNMask(n: ULong): ULong {
    require(n <= BITS.toULong()) { "lowerNMask() overflow in shl." }
    return if (n == BITS.toULong()) {
        ULong.MAX_VALUE
    } else {
        (1UL shl n.toInt()) - 1UL
    }
}

/**
 * Calculate the halfway point for the lower [n] bits.
 */
internal fun lowerNHalfway(n: ULong): ULong {
    require(n <= BITS.toULong()) { "lowerNHalfway() overflow in shl." }
    return if (n == 0UL) {
        0UL
    } else {
        nthBit(n - 1UL)
    }
}

/**
 * Calculate a bitwise mask with [n] 1 bits starting at the [bit] position.
 */
internal fun internalNMask(bit: ULong, n: ULong): ULong {
    require(bit <= BITS.toULong()) { "internalNMask() overflow in shl." }
    require(n <= BITS.toULong()) { "internalNMask() overflow in shl." }
    require(bit >= n) { "internalNMask() overflow in sub." }
    return lowerNMask(bit) xor lowerNMask(bit - n)
}

// NEAREST ROUNDING

/**
 * Shift right N-bytes and round to the nearest.
 *
 * Return whether we are above halfway and whether we are halfway.
 */
internal fun roundNearest(fp: ExtendedFloat, shift: Int): Pair<Boolean, Boolean> {
    val mask = lowerNMask(shift.toULong())
    val halfway = lowerNHalfway(shift.toULong())

    val truncatedBits = fp.mant and mask
    val isAbove = truncatedBits > halfway
    val isHalfway = truncatedBits == halfway

    // Bit shift so the leading bit is in the hidden bit.
    overflowingShr(fp, shift)

    return Pair(isAbove, isHalfway)
}

/**
 * Tie rounded floating point to even.
 */
internal fun tieEven(fp: ExtendedFloat, isAbove: Boolean, isHalfway: Boolean) {
    val isOdd = (fp.mant and 1UL) == 1UL
    if (isAbove || (isOdd && isHalfway)) {
        fp.mant += 1UL
    }
}

/**
 * Shift right N-bytes and round nearest, tie-to-even.
 */
internal fun roundNearestTieEven(fp: ExtendedFloat, shift: Int) {
    val (isAbove, isHalfway) = roundNearest(fp, shift)
    tieEven(fp, isAbove, isHalfway)
}

// DIRECTED ROUNDING

/**
 * Shift right N-bytes and round towards a direction.
 *
 * Return whether we have any truncated bytes.
 */
private fun roundToward(fp: ExtendedFloat, shift: Int): Boolean {
    val mask = lowerNMask(shift.toULong())
    val truncatedBits = fp.mant and mask
    overflowingShr(fp, shift)
    return truncatedBits != 0UL
}

/**
 * Shift right N-bytes and round toward zero.
 */
internal fun roundDownward(fp: ExtendedFloat, shift: Int) {
    roundToward(fp, shift)
}

// ROUND TO FLOAT

/**
 * Shift the ExtendedFloat fraction to the fraction bits in a native float.
 *
 * [floatTraits] provides the float-type constants.
 * [algorithm] is the rounding function to apply.
 */
internal fun roundToFloat(
    fp: ExtendedFloat,
    floatTraits: FloatTraits,
    algorithm: (ExtendedFloat, Int) -> Unit,
) {
    val finalExp = fp.exp + floatTraits.defaultShift
    if (finalExp < floatTraits.denormalExponent) {
        val diff = floatTraits.denormalExponent - fp.exp
        if (diff <= MantissaTraits.FULL) {
            algorithm(fp, diff)
        } else {
            fp.mant = 0UL
            fp.exp = 0
        }
    } else {
        algorithm(fp, floatTraits.defaultShift)
    }

    if ((fp.mant and floatTraits.carryMask) == floatTraits.carryMask) {
        shr(fp, 1)
    }
}

// AVOID OVERFLOW/UNDERFLOW

/**
 * Avoid overflow for large values, shift left as needed.
 *
 * Shift until a 1-bit is in the hidden bit, if the mantissa is not 0.
 */
internal fun avoidOverflow(fp: ExtendedFloat, floatTraits: FloatTraits) {
    if (fp.exp >= floatTraits.maxExponent) {
        val diff = fp.exp - floatTraits.maxExponent
        if (diff <= floatTraits.mantissaSize) {
            val bit = (floatTraits.mantissaSize + 1).toULong()
            val n = (diff + 1).toULong()
            val mask = internalNMask(bit, n)
            if ((fp.mant and mask) == 0UL) {
                val shift = diff + 1
                shl(fp, shift)
            }
        }
    }
}

// ROUND TO NATIVE

/**
 * Round an extended-precision float to a native float representation.
 */
internal fun roundToNative(
    fp: ExtendedFloat,
    floatTraits: FloatTraits,
    algorithm: (ExtendedFloat, Int) -> Unit,
) {
    // Shift all the way left, to ensure a consistent representation.
    fp.normalize()

    // Round so the fraction is in a native mantissa representation,
    // and avoid overflow/underflow.
    roundToFloat(fp, floatTraits, algorithm)
    avoidOverflow(fp, floatTraits)
}
