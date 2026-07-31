// port-lint: source tmp/serde_json/src/lexical/bhcomp.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Compare the mantissa to the halfway representation of the float.
 *
 * Compares the actual significant digits of the mantissa to the
 * theoretical digits from `b+h`, scaled into the proper range.
 */

// MANTISSA

/**
 * Parse the full mantissa into a big integer.
 *
 * Max digits is the maximum number of digits plus one.
 */
private fun parseMantissa(
    floatTraits: FloatTraits,
    integer: ByteArray,
    fraction: ByteArray,
): Bigint {
    val smallPowers = POW10_LIMB
    val step = smallPowers.size - 2
    val maxDigits = floatTraits.maxDigits - 1
    var counter = 0
    var value: Limb = 0UL
    var i = 0
    val result = Bigint.default()

    val allDigits = integer.toList() + fraction.toList()

    for (digit in allDigits) {
        if (counter == step) {
            result.imulSmall(smallPowers[counter])
            result.iaddSmall(value)
            counter = 0
            value = 0UL
        }

        value = value * 10UL
        value += toDigit(digit) ?: 0u

        i++
        counter++
        if (i == maxDigits) break
    }

    if (counter != 0) {
        result.imulSmall(smallPowers[counter])
        result.iaddSmall(value)
    }

    if (i < integer.size + fraction.size) {
        result.imulSmall(10UL)
        result.iaddSmall(1UL)
    }

    return result
}

// FLOAT OPS

/** Calculate `b` from a representation of `b` as a float. */
private fun bExtended(floatTraits: FloatTraits, f: Double): ExtendedFloat =
    ExtendedFloat.fromFloat(f, floatTraits)

/** Calculate `b+h` from a representation of `b` as a float. */
private fun bhExtended(floatTraits: FloatTraits, f: Double): ExtendedFloat {
    val b = bExtended(floatTraits, f)
    return ExtendedFloat(
        (b.mant shl 1) + 1UL,
        b.exp - 1
    )
}

// ROUNDING

/** Custom round-nearest, tie-even algorithm for bhcomp. */
private fun roundNearestTieEvenBhcomp(fp: ExtendedFloat, shift: Int, isTruncated: Boolean) {
    val (isAbove, isHalfway) = roundNearest(fp, shift)
    var above = isAbove
    var halfway = isHalfway
    if (halfway && isTruncated) {
        above = true
        halfway = false
    }
    tieEven(fp, above, halfway)
}

// BHCOMP

/** Calculate the mantissa for a big integer with a positive exponent. */
private fun largeAtof(floatTraits: FloatTraits, mantissa: Bigint, exponent: Int): Double {
    val bits = 64

    val bigmant = mantissa
    bigmant.imulPow10(exponent)

    val (mant, isTruncated) = bigmant.hi64()
    val exp = bigmant.bitLength() - bits
    val fp = ExtendedFloat(mant, exp)
    fp.roundToNative(floatTraits) { f, shift -> roundNearestTieEvenBhcomp(f, shift, isTruncated) }
    return intoFloat(fp, floatTraits)
}

/** Calculate the mantissa for a big integer with a negative exponent. */
private fun smallAtof(floatTraits: FloatTraits, mantissa: Bigint, exponent: Int, f: Double): Double {
    val realDigits = mantissa
    val realExp = exponent

    val theor = bhExtended(floatTraits, f)
    val theorDigits = Bigint().apply { data.add(theor.mant); normalize() }
    val theorExp = theor.exp

    val binaryExp = theorExp - realExp
    val halfradixExp = -realExp
    val radixExp = 0

    if (halfradixExp != 0) {
        theorDigits.imulPow5(halfradixExp)
    }
    if (radixExp != 0) {
        theorDigits.imulPow10(radixExp)
    }
    if (binaryExp > 0) {
        theorDigits.imulPow2(binaryExp)
    } else if (binaryExp < 0) {
        realDigits.imulPow2(-binaryExp)
    }

    val cmp = realDigits.compare(theorDigits)
    return when {
        cmp > 0 -> floatTraits.nextPositive(f)
        cmp < 0 -> f
        else -> floatTraits.roundPositiveEven(f)
    }
}

/**
 * Calculate the exact value of the float.
 *
 * The fraction must not have trailing zeros.
 */
internal fun bhcomp(
    floatTraits: FloatTraits,
    b: Double,
    integer: ByteArray,
    fraction: ByteArray,
    exponent: Int,
): Double {
    val integerDigits = integer.size
    var fractionDigits = fraction
    val fractionLen = fractionDigits.size
    val digitsStart = if (integerDigits == 0) {
        var start = 0
        while (start < fractionLen && fractionDigits[start] == '0'.code.toByte()) {
            start++
        }
        fractionDigits = fractionDigits.copyOfRange(start, fractionLen)
        start
    } else {
        0
    }
    val sciExp = scientificExponent(exponent, integerDigits, digitsStart)
    val count = minOf(floatTraits.maxDigits, integerDigits + fractionLen - digitsStart)
    val scaledExponent = sciExp + 1 - count

    val mantissa = parseMantissa(floatTraits, integer, fractionDigits)
    return if (scaledExponent >= 0) {
        largeAtof(floatTraits, mantissa, scaledExponent)
    } else {
        smallAtof(floatTraits, mantissa, scaledExponent, b)
    }
}