// port-lint: source lexical/shift.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.numtraits.I32Num
import io.github.kotlinmania.numtraits.checkedShl
import io.github.kotlinmania.numtraits.checkedShr

/** Exception raised when lexical arithmetic violates checked bounds or panics. */
internal class LexicalPanicException(
    message: String,
) : RuntimeException(message)

/** Shift extended-precision float right by [shift] bits. */
internal fun shr(fp: ExtendedFloat, shift: Int) {
    val bits = 64uL
    if (shift.toLong().toULong() >= bits) {
        throw LexicalPanicException("shr() overflow in shift right.")
    }
    fp.mant = fp.mant.checkedShr(shift.toUInt()) ?: throw LexicalPanicException("attempt to shift with overflow")
    fp.exp = I32Num.checkedAdd(fp.exp, shift) ?: throw LexicalPanicException("attempt to add with overflow")
}

/**
 * Shift extended-precision float right by [shift] bits, zeroing the mantissa
 * when [shift] equals the 64-bit mantissa width.
 */
internal fun overflowingShr(fp: ExtendedFloat, shift: Int) {
    val bits = 64uL
    val unsignedShift = shift.toLong().toULong()
    if (unsignedShift > bits) {
        throw LexicalPanicException("overflowing_shr() overflow in shift right.")
    }
    fp.mant =
        if (unsignedShift == bits) {
            0uL
        } else {
            fp.mant.checkedShr(shift.toUInt()) ?: throw LexicalPanicException("attempt to shift with overflow")
        }
    fp.exp = I32Num.checkedAdd(fp.exp, shift) ?: throw LexicalPanicException("attempt to add with overflow")
}

/** Shift extended-precision float left by [shift] bits. */
internal fun shl(fp: ExtendedFloat, shift: Int) {
    val bits = 64uL
    if (shift.toLong().toULong() >= bits) {
        throw LexicalPanicException("shl() overflow in shift left.")
    }
    fp.mant = fp.mant.checkedShl(shift.toUInt()) ?: throw LexicalPanicException("attempt to shift with overflow")
    fp.exp = I32Num.checkedSub(fp.exp, shift) ?: throw LexicalPanicException("attempt to subtract with overflow")
}
