// port-lint: source lexical/digit.rs
package io.github.kotlinmania.serdejson.lexical

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
    return if (other != 0UL && result / other != this) null else result
}

private fun ULong.checkedAdd(other: ULong): ULong? {
    val result = this + other
    return if (result < this) null else result
}
