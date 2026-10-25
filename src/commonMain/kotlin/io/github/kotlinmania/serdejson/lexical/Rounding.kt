// Compatibility entry points for the complete generated lexical/rounding.rs.
package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.serdejson.lexical.generated.rounding.ExtendedFloat as RoundingState
import io.github.kotlinmania.serdejson.lexical.generated.rounding.avoid_overflow as generatedAvoidOverflow
import io.github.kotlinmania.serdejson.lexical.generated.rounding.internal_n_mask as generatedInternalNMask
import io.github.kotlinmania.serdejson.lexical.generated.rounding.lower_n_halfway as generatedLowerNHalfway
import io.github.kotlinmania.serdejson.lexical.generated.rounding.lower_n_mask as generatedLowerNMask
import io.github.kotlinmania.serdejson.lexical.generated.rounding.nth_bit as generatedNthBit
import io.github.kotlinmania.serdejson.lexical.generated.rounding.round_downward as generatedRoundDownward
import io.github.kotlinmania.serdejson.lexical.generated.rounding.round_nearest as generatedRoundNearest
import io.github.kotlinmania.serdejson.lexical.generated.rounding.round_nearest_tie_even as generatedRoundNearestTieEven
import io.github.kotlinmania.serdejson.lexical.generated.rounding.round_to_float as generatedRoundToFloat
import io.github.kotlinmania.serdejson.lexical.generated.rounding.round_to_native as generatedRoundToNative
import io.github.kotlinmania.serdejson.lexical.generated.rounding.tie_even as generatedTieEven

/** Preserve scalar state on ordinary and exceptional exits, including callback mutations. */
private inline fun <R> withRoundingState(fp: ExtendedFloat, operation: (RoundingState) -> R): R {
    val state = RoundingState(fp.mant, fp.exp)
    try {
        return operation(state)
    } finally {
        fp.mant = state.mant
        fp.exp = state.exp
    }
}

/** Callers observe their original object with the current generated state.
 * Copy callback changes back even when the callback throws.
 */
private fun roundingCallback(
    fp: ExtendedFloat,
    algorithm: (ExtendedFloat, Int) -> Unit,
): (RoundingState, Int) -> Unit = { state, shift ->
    fp.mant = state.mant
    fp.exp = state.exp
    try {
        algorithm(fp, shift)
    } finally {
        state.mant = fp.mant
        state.exp = fp.exp
    }
}

// Representation-only bridge: normalization remains in the existing Float.kt operation.
internal fun normalizeForGenerated(fp: ExtendedFloat): UInt = fp.normalize().toUInt()

internal fun nthBit(n: ULong): ULong = generatedNthBit(n)
internal fun lowerNMask(n: ULong): ULong = generatedLowerNMask(n)
internal fun lowerNHalfway(n: ULong): ULong = generatedLowerNHalfway(n)
internal fun internalNMask(bit: ULong, n: ULong): ULong = generatedInternalNMask(bit, n)
internal fun roundNearest(fp: ExtendedFloat, shift: Int): Pair<Boolean, Boolean> =
    withRoundingState(fp) { generatedRoundNearest(it, shift) }
internal fun tieEven(fp: ExtendedFloat, isAbove: Boolean, isHalfway: Boolean) =
    withRoundingState(fp) { generatedTieEven(it, isAbove, isHalfway) }
internal fun roundNearestTieEven(fp: ExtendedFloat, shift: Int) =
    withRoundingState(fp) { generatedRoundNearestTieEven(it, shift) }
internal fun roundDownward(fp: ExtendedFloat, shift: Int) =
    withRoundingState(fp) { generatedRoundDownward(it, shift) }
internal fun roundToFloat(fp: ExtendedFloat, floatTraits: FloatTraits, algorithm: (ExtendedFloat, Int) -> Unit) =
    withRoundingState(fp) { generatedRoundToFloat(it, roundingCallback(fp, algorithm), floatTraits) }
internal fun avoidOverflow(fp: ExtendedFloat, floatTraits: FloatTraits) =
    withRoundingState(fp) { generatedAvoidOverflow(it, floatTraits) }
internal fun roundToNative(fp: ExtendedFloat, floatTraits: FloatTraits, algorithm: (ExtendedFloat, Int) -> Unit) =
    withRoundingState(fp) { generatedRoundToNative(it, roundingCallback(fp, algorithm), floatTraits) }
