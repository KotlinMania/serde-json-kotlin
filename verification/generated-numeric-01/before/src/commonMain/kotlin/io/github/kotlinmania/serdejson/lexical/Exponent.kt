// port-lint: source lexical/exponent.rs
package io.github.kotlinmania.serdejson.lexical

/**
 * Converts an integer to [Int] clamping to [Int] range (no overflow).
 */
private fun intoI32(value: Long): Int =
    if (value > Int.MAX_VALUE.toLong()) Int.MAX_VALUE else value.toInt()

/**
 * Calculate the scientific notation exponent without overflow.
 *
 * For example, `0.1` would be `-1`, and `10` would be `1` in base 10.
 *
 * @param exponent       The raw exponent from the parsed float.
 * @param integerDigits  Number of digits in the integer part.
 * @param fractionStart  Index of the first significant digit in the fraction part.
 */
internal fun scientificExponent(
    exponent: Int,
    integerDigits: Int,
    fractionStart: Int,
): Int =
    if (integerDigits == 0) {
        val fs = intoI32(fractionStart.toLong())
        exponent.saturationSub(fs).saturationSub(1)
    } else {
        val integerShift = intoI32((integerDigits - 1).toLong())
        exponent.saturationAdd(integerShift)
    }

/**
 * Calculate the mantissa exponent without overflow.
 *
 * Removes the number of digits that contributed to the mantissa past the dot
 * and adds the number of truncated digits, to calculate the scaling factor
 * for the mantissa from a raw exponent.
 *
 * @param exponent       The raw exponent from the parsed float.
 * @param fractionDigits Number of digits in the fraction part.
 * @param truncated      Number of digits truncated from the mantissa.
 */
internal fun mantissaExponent(
    exponent: Int,
    fractionDigits: Int,
    truncated: Int,
): Int =
    if (fractionDigits > truncated) {
        exponent.saturationSub(intoI32((fractionDigits - truncated).toLong()))
    } else {
        exponent.saturationAdd(intoI32((truncated - fractionDigits).toLong()))
    }

/** Saturating subtraction (clamps to [Int] range, no overflow). */
private fun Int.saturationSub(other: Int): Int {
    val r = this.toLong() - other.toLong()
    return if (r < Int.MIN_VALUE.toLong()) {
        Int.MIN_VALUE
    } else if (r > Int.MAX_VALUE.toLong()) {
        Int.MAX_VALUE
    } else {
        r.toInt()
    }
}

/** Saturating addition (clamps to [Int] range, no overflow). */
private fun Int.saturationAdd(other: Int): Int {
    val r = this.toLong() + other.toLong()
    return if (r < Int.MIN_VALUE.toLong()) {
        Int.MIN_VALUE
    } else if (r > Int.MAX_VALUE.toLong()) {
        Int.MAX_VALUE
    } else {
        r.toInt()
    }
}
