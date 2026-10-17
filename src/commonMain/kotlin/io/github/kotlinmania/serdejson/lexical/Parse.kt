// port-lint: source lexical/parse.rs
package io.github.kotlinmania.serdejson.lexical

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
    if (mantissa == 0UL) return 0.0
    val mantissaStr = mantissa.toString()
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
    var fracLen = fraction.size
    while (fracLen > 0 && fraction[fracLen - 1] == '0'.code.toByte()) {
        fracLen--
    }

    val intStr = integer.decodeToString()
    val fracStr = if (fracLen > 0) fraction.copyOfRange(0, fracLen).decodeToString() else ""

    val combinedDigits = intStr + fracStr
    if (combinedDigits.isEmpty() || combinedDigits.all { it == '0' }) return 0.0

    val effectiveExp = exponent - fracLen
    val floatStr = formatScientific(combinedDigits, effectiveExp)
    return floatStr.toDouble()
}

private fun formatScientific(digits: String, exp: Int): String {
    if (digits.isEmpty()) return "0.0"

    var leadingZeroCount = 0
    while (leadingZeroCount < digits.length - 1 && digits[leadingZeroCount] == '0') {
        leadingZeroCount++
    }
    val significantDigits = if (leadingZeroCount > 0) digits.substring(leadingZeroCount) else digits

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
