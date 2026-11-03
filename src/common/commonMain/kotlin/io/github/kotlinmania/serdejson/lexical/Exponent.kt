// port-lint: source lexical/exponent.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.numtraits.I32Num
import io.github.kotlinmania.numtraits.U64Num

/** Convert an unsigned digit count into a signed 32-bit integer without overflow. */
private fun intoI32(value: ULong): Int =
    if (value > Int.MAX_VALUE.toULong()) {
        Int.MAX_VALUE
    } else {
        value.toInt()
    }

/** Calculate the scientific notation exponent without overflow. */
internal fun scientificExponent(
    exponent: Int,
    integerDigits: ULong,
    fractionStart: ULong,
): Int =
    if (integerDigits == 0uL) {
        val shift = intoI32(fractionStart)
        I32Num.saturatingSub(I32Num.saturatingSub(exponent, shift), 1)
    } else {
        val diff =
            U64Num.checkedSub(integerDigits, 1uL)
                ?: throw LexicalPanicException("attempt to subtract with overflow")
        val integerShift = intoI32(diff)
        I32Num.saturatingAdd(exponent, integerShift)
    }

/** Calculate the mantissa scaling exponent without overflow. */
internal fun mantissaExponent(
    exponent: Int,
    fractionDigits: ULong,
    truncated: ULong,
): Int =
    if (fractionDigits > truncated) {
        val diff =
            U64Num.checkedSub(fractionDigits, truncated)
                ?: throw LexicalPanicException("attempt to subtract with overflow")
        I32Num.saturatingSub(exponent, intoI32(diff))
    } else {
        val diff =
            U64Num.checkedSub(truncated, fractionDigits)
                ?: throw LexicalPanicException("attempt to subtract with overflow")
        I32Num.saturatingAdd(exponent, intoI32(diff))
    }
