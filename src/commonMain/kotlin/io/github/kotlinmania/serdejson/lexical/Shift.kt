package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.serdejson.lexical.generated.shift.ExtendedFloat as ShiftState
import io.github.kotlinmania.serdejson.lexical.generated.shift.shr as generatedShr
import io.github.kotlinmania.serdejson.lexical.generated.shift.overflowing_shr as generatedOverflowingShr
import io.github.kotlinmania.serdejson.lexical.generated.shift.shl as generatedShl

/** Compatibility adapter for complete raw-generated lexical/shift.rs.
 * Uses the verified 64-bit profile with overflow checks and debug assertions.
 * Copy back on exceptional exits too: Rust may update mant before exp overflows.
 * No shift arithmetic is implemented by this adapter.
 */
private inline fun withShiftState(fp: ExtendedFloat, operation: (ShiftState) -> Unit) {
    val state = ShiftState(fp.mant, fp.exp)
    try {
        operation(state)
    } finally {
        fp.mant = state.mant
        fp.exp = state.exp
    }
}

internal fun shr(fp: ExtendedFloat, shift: Int) = withShiftState(fp) { generatedShr(it, shift) }

internal fun overflowingShr(fp: ExtendedFloat, shift: Int) = withShiftState(fp) { generatedOverflowingShr(it, shift) }

internal fun shl(fp: ExtendedFloat, shift: Int) = withShiftState(fp) { generatedShl(it, shift) }
