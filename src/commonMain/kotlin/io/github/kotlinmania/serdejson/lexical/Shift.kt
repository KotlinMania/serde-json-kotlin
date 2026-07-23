// port-lint: source tmp/serde_json/src/lexical/shift.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Bit-shift helpers for extended-precision floats.
 */

private const val BITS: Int = 64 // size of ULong in bits

/**
 * Shift extended-precision float right [shift] bytes.
 */
internal fun shr(fp: ExtendedFloat, shift: Int) {
    require(shift.toLong() < BITS.toLong()) { "shr() overflow in shift right." }
    fp.mant = fp.mant shr shift
    fp.exp += shift
}

/**
 * Shift extended-precision float right [shift] bytes.
 *
 * Accepts when the shift is the same as the type size, and
 * sets the value to 0.
 */
internal fun overflowingShr(fp: ExtendedFloat, shift: Int) {
    require(shift.toLong() <= BITS.toLong()) { "overflowingShr() overflow in shift right." }
    fp.mant = if (shift.toLong() == BITS.toLong()) {
        0UL
    } else {
        fp.mant shr shift
    }
    fp.exp += shift
}

/**
 * Shift extended-precision float left [shift] bytes.
 */
internal fun shl(fp: ExtendedFloat, shift: Int) {
    require(shift.toLong() < BITS.toLong()) { "shl() overflow in shift left." }
    fp.mant = fp.mant shl shift
    fp.exp -= shift
}