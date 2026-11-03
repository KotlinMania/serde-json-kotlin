// port-lint: source lexical/algorithm.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Algorithms to efficiently convert strings to floats.
 */

// FAST
// ----

/**
 * Convert mantissa to exact value for a non-base2 power.
 *
 * Returns the resulting float and if the value can be represented exactly.
 */
internal fun fastPath(
    floatTraits: FloatTraits,
    mantissa: ULong,
    exponent: Int,
): Double? {
    val (minExp, maxExp) = floatTraits.exponentLimit()
    val shiftExp = floatTraits.mantissaLimit()
    val mantissaSize = floatTraits.mantissaSize + 1
    return if (mantissa == 0UL) {
        0.0
    } else if (mantissa shr mantissaSize != 0UL) {
        null
    } else if (exponent == 0) {
        floatTraits.asCast(mantissa)
    } else if (exponent >= minExp && exponent <= maxExp) {
        val float = floatTraits.asCast(mantissa)
        floatTraits.pow10(float, exponent)
    } else if (exponent >= 0 && exponent <= maxExp + shiftExp) {
        val smallPowers = POW10_64
        val shift = exponent - maxExp
        val power = smallPowers[shift]

        val value = mantissa * power
        if (value / power != mantissa) {
            null
        } else if (value shr mantissaSize != 0UL) {
            null
        } else {
            val float = floatTraits.asCast(value)
            floatTraits.pow10(float, maxExp)
        }
    } else {
        null
    }
}

private fun saturatingAddI32(lhs: Int, rhs: Int): Int {
    val sum = lhs.toLong() + rhs.toLong()
    return when {
        sum > Int.MAX_VALUE.toLong() -> Int.MAX_VALUE
        sum < Int.MIN_VALUE.toLong() -> Int.MIN_VALUE
        else -> sum.toInt()
    }
}

// MODERATE
// --------

/**
 * Multiply the floating-point by the exponent.
 *
 * Modify the extended-float and return if the new value can be represented
 * accurately.
 */
private fun multiplyExponentExtended(
    floatTraits: FloatTraits,
    fp: ExtendedFloat,
    exponent: Int,
    truncated: Boolean,
): Boolean {
    val powers = ExtendedFloatCache.getPowers()
    val exp = saturatingAddI32(exponent, powers.bias)
    if (exp < 0) {
        fp.mant = 0UL
        return true
    }
    val smallIndex = exp % powers.step
    val largeIndex = exp / powers.step
    return if (largeIndex >= powers.large.length) {
        fp.mant = 1UL shl 63
        fp.exp = 0x7FF
        true
    } else {
        var errors: UInt = 0u
        if (truncated) {
            errors += ULongFloatErrors.errorHalfscale()
        }

        val smallInt = powers.getSmallInt(smallIndex)
        val product = fp.mant * smallInt
        if (fp.mant != 0UL && product / fp.mant != smallInt) {
            fp.normalize()
            fp.imul(powers.getSmall(smallIndex))
            errors += ULongFloatErrors.errorHalfscale()
        } else {
            fp.mant = product
            fp.normalize()
        }

        fp.imul(powers.getLarge(largeIndex))
        if (errors > 0u) {
            errors += 1u
        }
        errors += ULongFloatErrors.errorHalfscale()

        val shift = fp.normalize()
        errors = errors shl shift

        errorIsAccurate(floatTraits, errors, fp)
    }
}

/**
 * Create a precise native float using an intermediate extended-precision float.
 *
 * Return the float approximation and if the value can be accurately
 * represented with mantissa bits of precision.
 */
internal fun moderatePath(
    floatTraits: FloatTraits,
    mantissa: ULong,
    exponent: Int,
    truncated: Boolean,
): Pair<ExtendedFloat, Boolean> {
    val fp = ExtendedFloat(mantissa, 0)
    val valid = multiplyExponentExtended(floatTraits, fp, exponent, truncated)
    return Pair(fp, valid)
}

// FALLBACK
// --------

/**
 * Fallback path when the fast path does not work.
 *
 * Uses the moderate path, if applicable, otherwise uses the slow path
 * as required.
 */
internal fun fallbackPath(
    floatTraits: FloatTraits,
    integer: ByteArray,
    fraction: ByteArray,
    mantissa: ULong,
    exponent: Int,
    mantissaExponent: Int,
    truncated: Boolean,
): Double {
    val (fp, valid) = moderatePath(floatTraits, mantissa, mantissaExponent, truncated)
    if (valid) {
        return fp.intoFloat(floatTraits)
    }

    val b = fp.intoDownwardFloat(floatTraits)
    return if (floatTraits.isSpecial(b)) {
        b
    } else {
        bhcomp(floatTraits, b, integer, fraction, exponent)
    }
}
