// port-lint: source lexical/mod.rs
package io.github.kotlinmania.serdejson.lexical

// The code in this module is derived from the `lexical` crate by @Alexhuszagh
// which the author condensed into this minimal subset for use in serde_json.
// For the serde_json use case we care more about reliably round tripping all
// possible floating point values than about parsing any arbitrarily long string
// of digits with perfect accuracy, as the latter would take a high cost in
// compile time and performance.
//
// Dual licensed as MIT and Apache 2.0 just like the rest of serde_json, but
// copyright Alexander Huszagh.

// In the upstream Rust crate, the lexical module implements a complex
// bignum-based algorithm for perfect float round-tripping. For this Kotlin
// Multiplatform port we delegate the actual parsing to the platform-native
// String.toDouble() while preserving the same API entry points.

// ---------------------------------------------------------------------------
// Small powers (port-lint: source src/lexical/small_powers.rs)
// ---------------------------------------------------------------------------

/** Pre-computed powers of 5 for 64-bit values. */
internal val POW5_64: ULongArray =
    ulongArrayOf(
        1UL,
        5UL,
        25UL,
        125UL,
        625UL,
        3125UL,
        15625UL,
        78125UL,
        390625UL,
        1953125UL,
        9765625UL,
        48828125UL,
        244140625UL,
        1220703125UL,
        6103515625UL,
        30517578125UL,
        152587890625UL,
        762939453125UL,
        3814697265625UL,
        19073486328125UL,
        95367431640625UL,
        476837158203125UL,
        2384185791015625UL,
        11920928955078125UL,
        59604644775390625UL,
        298023223876953125UL,
        1490116119384765625UL,
        7450580596923828125UL,
    )

/** Pre-computed powers of 10 for 64-bit values. */
internal val POW10_64: ULongArray =
    ulongArrayOf(
        1UL,
        10UL,
        100UL,
        1000UL,
        10000UL,
        100000UL,
        1000000UL,
        10000000UL,
        100000000UL,
        1000000000UL,
        10000000000UL,
        100000000000UL,
        1000000000000UL,
        10000000000000UL,
        100000000000000UL,
        1000000000000000UL,
        10000000000000000UL,
        100000000000000000UL,
        1000000000000000000UL,
        10000000000000000000UL,
    )

// ---------------------------------------------------------------------------
// Digit helpers (port-lint: source src/lexical/digit.rs)
// ---------------------------------------------------------------------------

/**
 * Converts a single ASCII byte (`b'0'`..`b'9'`) to its numeric digit value,
 * or `null` if the byte is not a decimal digit.
 */
internal fun toDigit(c: Byte): UInt? =
    if (c in b0..b9) (c - b0).toUInt() else null

private val b0: Byte = '0'.code.toByte()
private val b9: Byte = '9'.code.toByte()

/**
 * Multiplies [value] by 10 and adds [digit], returning `null` on overflow.
 */
internal fun addDigit(value: ULong, digit: UInt): ULong? {
    val n = value.checkedMultiply(10UL) ?: return null
    return n.checkedAdd(digit.toULong())
}

private fun ULong.checkedMultiply(other: ULong): ULong? {
    val result = this * other
    // Detect overflow: if `other` is non-zero, the division should reverse exactly.
    return if (other != 0UL && result / other != this) null else result
}

private fun ULong.checkedAdd(other: ULong): ULong? {
    val result = this + other
    // Overflow if the result is smaller than either operand.
    return if (result < this) null else result
}

// ---------------------------------------------------------------------------
// Exponent helpers (port-lint: source src/lexical/exponent.rs)
// ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// ExtendedFloat (port-lint: source src/lexical/float.rs)
// ---------------------------------------------------------------------------

/**
 * Extended precision floating-point type.
 *
 * Used for the bignum-based float round-tripping algorithm.
 */
internal data class ExtendedFloat(
    /** Mantissa for the extended-precision float. */
    var mant: ULong,
    /** Binary exponent for the extended-precision float. */
    var exp: Int,
) {
    /**
     * Normalize the float-point number.
     *
     * Shift the mantissa so the number of leading zeros is 0, or the value
     * itself is 0. Returns the number of bits shifted.
     */
    fun normalize(): Int {
        val shift =
            if (mant == 0UL) {
                0
            } else {
                mant.countLeadingZeroBits()
            }
        if (shift > 0) {
            shl(this, shift)
        }
        return shift
    }

    /**
     * Multiply two normalized extended-precision floats, as if by `a*b`.
     *
     * The result is not normalized.
     */
    fun mul(b: ExtendedFloat): ExtendedFloat {
        val ah = (mant shr MantissaTraits.HALF)
        val al = (mant and MantissaTraits.LOMASK)
        val bh = (b.mant shr MantissaTraits.HALF)
        val bl = (b.mant and MantissaTraits.LOMASK)

        val ahBl = ah * bl
        val alBh = al * bh
        val alBl = al * bl
        val ahBh = ah * bh

        var tmp = (ahBl and MantissaTraits.LOMASK) + (alBh and MantissaTraits.LOMASK) + (alBl shr MantissaTraits.HALF)
        tmp += (1UL shl (MantissaTraits.HALF - 1))

        return ExtendedFloat(
            ahBh + (ahBl shr MantissaTraits.HALF) + (alBh shr MantissaTraits.HALF) + (tmp shr MantissaTraits.HALF),
            exp + b.exp + MantissaTraits.FULL,
        )
    }

    /** Multiply in-place, as if by `a*b`. The result is not normalized. */
    fun imul(b: ExtendedFloat) {
        val result = mul(b)
        mant = result.mant
        exp = result.exp
    }

    /**
     * Lossy round float-point number to native mantissa boundaries.
     */
    internal fun roundToNative(floatTraits: FloatTraits, algorithm: (ExtendedFloat, Int) -> Unit) {
        roundToNative(this, floatTraits, algorithm)
    }

    companion object {
        /** Create extended float from native float. */
        internal fun fromFloat(f: Double, floatTraits: FloatTraits): ExtendedFloat =
            ExtendedFloat(floatTraits.mantissa(f), floatTraits.exponent(f))
    }

    /** Convert into default-rounded, lower-precision native float. */
    internal fun intoFloat(floatTraits: FloatTraits): Double {
        roundToNative(floatTraits, ::roundNearestTieEven)
        return intoFloat(this, floatTraits)
    }

    /** Convert into downward-rounded, lower-precision native float. */
    internal fun intoDownwardFloat(floatTraits: FloatTraits): Double {
        roundToNative(floatTraits, ::roundDownward)
        return intoFloat(this, floatTraits)
    }
}

/** Export extended-precision float to native float. */
internal fun intoFloat(fp: ExtendedFloat, floatTraits: FloatTraits): Double {
    if (fp.mant == 0UL || fp.exp < floatTraits.denormalExponent) {
        return 0.0
    } else if (fp.exp >= floatTraits.maxExponent) {
        return floatTraits.fromBits(floatTraits.infinityBits)
    } else {
        val exp: ULong =
            if (fp.exp == floatTraits.denormalExponent && (fp.mant and floatTraits.hiddenBitMask) == 0UL) {
                0UL
            } else {
                (fp.exp + floatTraits.exponentBias).toULong()
            }
        val expShifted = exp shl floatTraits.mantissaSize
        val mant = fp.mant and floatTraits.mantissaMask
        return floatTraits.fromBits(mant or expShifted)
    }
}

// ---------------------------------------------------------------------------
// Parsers (port-lint: source src/lexical/parse.rs)
// ---------------------------------------------------------------------------

/**
 * Parse a float for which the entire integer and fraction parts fit into a
 * 64-bit mantissa.
 *
 * @param mantissa The 64-bit mantissa containing all significant digits.
 * @param mantExp  The mantissa exponent (scaling factor) computed from the
 *                 raw exponent and the number of fraction / truncated digits.
 * @return The parsed [Double].
 */
internal fun parseConciseFloat(mantissa: ULong, mantExp: Int): Double {
    // Reconstruct the decimal representation and delegate to the platform's
    // native double parser, which provides correct round-tripping on all
    // KMP targets.
    if (mantissa == 0UL) return 0.0
    val mantissaStr = mantissa.toString()
    // mantExp is the exponent of the mantissa: value = mantissa * 10^mantExp
    val floatStr = formatScientific(mantissaStr, mantExp)
    return floatStr.toDouble()
}

/**
 * Parse a float from extracted float components.
 *
 * @param integer  Byte array containing the integer digits (ASCII `b'0'`..`b'9'`).
 * @param fraction Byte array containing the fraction digits (ASCII `b'0'`..`b'9'`).
 * @param exponent The parsed 32-bit exponent.
 * @return The parsed [Double].
 *
 * Precondition: The integer must not have leading zeros.
 */
internal fun parseTruncatedFloat(integer: ByteArray, fraction: ByteArray, exponent: Int): Double {
    // Trim trailing zeros from the fraction part.
    var fracLen = fraction.size
    while (fracLen > 0 && fraction[fracLen - 1] == '0'.code.toByte()) {
        fracLen--
    }

    // Build the full digit string: integer.fraction * 10^exponent
    val intStr = integer.decodeToString()
    val fracStr = if (fracLen > 0) fraction.copyOfRange(0, fracLen).decodeToString() else ""

    // The value is: intStr.fracStr × 10^exponent
    // Reconstruct into a single decimal or scientific string for toDouble().
    val combinedDigits = intStr + fracStr
    if (combinedDigits.isEmpty() || combinedDigits.all { it == '0' }) return 0.0

    // Effective exponent: the decimal point sits after intStr and before
    // fracStr. To represent the value as combinedDigits × 10^effectiveExp,
    // we move the decimal point to the end of the combined digits (right by
    // fracLen positions), which multiplies by 10^fracLen, so we compensate
    // by subtracting fracLen from the exponent.
    val effectiveExp = exponent - fracLen
    val floatStr = formatScientific(combinedDigits, effectiveExp)
    return floatStr.toDouble()
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Formats a sequence of decimal digits and an exponent into a scientific
 * notation string suitable for [String.toDouble].
 *
 * Given digits "12345" and exp -2, the value is 123.45.
 * Given digits "12345" and exp 3,  the value is 12345000.0.
 *
 * The strategy:
 *  1. Place the decimal point after the first digit: "1.2345"
 *  2. Compute the total power-of-10 shift: (len - 1) + exp
 *  3. Emit as "1.2345eN"
 *
 * This handles the full range of exponents without overflow in the mantissa
 * digits themselves.
 */
private fun formatScientific(digits: String, exp: Int): String {
    if (digits.isEmpty()) return "0.0"

    // Normalize: trim leading zeros (they don't contribute to the value but
    // shift the exponent).
    var leadingZeroCount = 0
    while (leadingZeroCount < digits.length - 1 && digits[leadingZeroCount] == '0') {
        leadingZeroCount++
    }
    val significantDigits = if (leadingZeroCount > 0) digits.substring(leadingZeroCount) else digits

    // The value is: significantDigits × 10^exp
    // In scientific notation: d.ddddd × 10^(power)
    //   where power = exp + (significantDigits.length - 1)
    // Leading zeros don't change the value (00123 × 10^2 == 123 × 10^2),
    // so we strip them and use the remaining digits directly.
    val power = exp + (significantDigits.length - 1)

    val mantissaStr =
        if (significantDigits.length <= 1) {
            significantDigits
        } else {
            significantDigits[0].toString() + "." + significantDigits.substring(1)
        }

    return if (power == 0) {
        mantissaStr
    } else {
        mantissaStr + "e" + power
    }
}
