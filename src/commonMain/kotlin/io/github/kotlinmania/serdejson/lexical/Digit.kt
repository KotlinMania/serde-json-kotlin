// port-lint: source lexical/digit.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.numtraits.U64Num

/** Convert an ASCII byte to its base-10 digit value. */
internal fun toDigit(c: UByte): UInt? =
    if (c in 48u..57u) {
        (c - 48u).toUInt()
    } else {
        null
    }

/** Multiply [value] by 10 and add [digit], returning null on overflow. */
internal fun addDigit(value: ULong, digit: UInt): ULong? {
    val multiplied = U64Num.checkedMul(value, 10uL) ?: return null
    return U64Num.checkedAdd(multiplied, digit.toULong())
}
