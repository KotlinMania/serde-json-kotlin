// port-lint: source lexical/parse.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Parse a float for which the entire integer and fraction parts fit into a
 * 64-bit mantissa.
 */
internal fun parseConciseFloat(
    mantissa: ULong,
    mantExp: Int,
    floatTraits: FloatTraits = F64Float,
): Double {
    val fast = fastPath(floatTraits, mantissa, mantExp)
    if (fast != null) {
        return fast
    }

    val truncated = false
    val (fp, valid) = moderatePath(floatTraits, mantissa, mantExp, truncated)
    if (valid) {
        return fp.intoFloat(floatTraits)
    }

    val b = fp.intoDownwardFloat(floatTraits)
    if (floatTraits.isSpecial(b)) {
        return b
    }

    val integer = mantissa.toString().encodeToByteArray()
    val fraction = ByteArray(0)
    return bhcomp(floatTraits, b, integer, fraction, mantExp)
}

/**
 * Parse a float from extracted integer, fraction, and exponent components.
 *
 * Precondition: [integer] must not have leading zeros.
 */
internal fun parseTruncatedFloat(
    integer: ByteArray,
    fraction: ByteArray,
    exponent: Int,
    floatTraits: FloatTraits = F64Float,
): Double {
    var fracLen = fraction.size
    while (fracLen > 0 && fraction[fracLen - 1] == '0'.code.toByte()) {
        fracLen--
    }
    val trimmedFraction =
        if (fracLen == fraction.size) {
            fraction
        } else {
            fraction.copyOfRange(0, fracLen)
        }

    val totalDigits = integer.size + trimmedFraction.size
    var truncated = 0uL
    var mantissa = 0uL
    for (idx in 0 until totalDigits) {
        val c = if (idx < integer.size) integer[idx] else trimmedFraction[idx - integer.size]
        val digit =
            toDigit(c.toUByte())
                ?: throw LexicalPanicException("called `Option::unwrap()` on a `None` value")
        val next = addDigit(mantissa, digit)
        if (next != null) {
            mantissa = next
        } else {
            truncated = (totalDigits - idx).toULong()
            break
        }
    }

    val mantExp = mantissaExponent(exponent, trimmedFraction.size.toULong(), truncated)
    val isTruncated = true
    return fallbackPath(
        floatTraits,
        integer,
        trimmedFraction,
        mantissa,
        exponent,
        mantExp,
        isTruncated,
    )
}
