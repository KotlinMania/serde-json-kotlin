// port-lint: source lexical/rounding.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.numtraits.I32Num
import io.github.kotlinmania.numtraits.U64Num
import io.github.kotlinmania.numtraits.checkedShl

/** Calculate a scalar power of two at bit position [n]. */
internal fun nthBit(n: ULong): ULong {
    val bits = 64uL
    if (n >= bits) {
        throw LexicalPanicException("nthBit() overflow in shl.")
    }
    return 1uL.checkedShl(n.toUInt()) ?: throw LexicalPanicException("attempt to shift with overflow")
}

/** Generate a bitwise mask for the lower [n] bits. */
internal fun lowerNMask(n: ULong): ULong {
    val bits = 64uL
    if (n > bits) {
        throw LexicalPanicException("lowerNMask() overflow in shl.")
    }
    return if (n == bits) {
        ULong.MAX_VALUE
    } else {
        val shifted = 1uL.checkedShl(n.toUInt()) ?: throw LexicalPanicException("attempt to shift with overflow")
        U64Num.checkedSub(shifted, 1uL) ?: throw LexicalPanicException("attempt to subtract with overflow")
    }
}

/** Calculate the halfway point for the lower [n] bits. */
internal fun lowerNHalfway(n: ULong): ULong {
    val bits = 64uL
    if (n > bits) {
        throw LexicalPanicException("lowerNHalfway() overflow in shl.")
    }
    return if (n == 0uL) {
        0uL
    } else {
        val prev = U64Num.checkedSub(n, 1uL) ?: throw LexicalPanicException("attempt to subtract with overflow")
        nthBit(prev)
    }
}

/** Calculate a bitwise mask with [n] one-bits ending below [bit]. */
internal fun internalNMask(bit: ULong, n: ULong): ULong {
    val bits = 64uL
    if (bit > bits || n > bits || bit < n) {
        throw LexicalPanicException("internalNHalfway() overflow.")
    }
    val lowBit = U64Num.checkedSub(bit, n) ?: throw LexicalPanicException("attempt to subtract with overflow")
    return lowerNMask(bit) xor lowerNMask(lowBit)
}

/**
 * Shift right by [shift] bits and evaluate nearest-rounding flags.
 *
 * Returns `(isAbove, isHalfway)`.
 */
internal fun roundNearest(fp: ExtendedFloat, shift: Int): Pair<Boolean, Boolean> {
    val shiftBits = shift.toLong().toULong()
    val mask = lowerNMask(shiftBits)
    val halfway = lowerNHalfway(shiftBits)

    val truncatedBits = fp.mant and mask
    val isAbove = truncatedBits > halfway
    val isHalfway = truncatedBits == halfway

    overflowingShr(fp, shift)
    return Pair(isAbove, isHalfway)
}

/** Tie a rounded extended-precision float to even. */
internal fun tieEven(fp: ExtendedFloat, isAbove: Boolean, isHalfway: Boolean) {
    val isOdd = (fp.mant and 1uL) == 1uL
    if (isAbove || (isOdd && isHalfway)) {
        fp.mant = U64Num.checkedAdd(fp.mant, 1uL) ?: throw LexicalPanicException("attempt to add with overflow")
    }
}

/** Shift right by [shift] bits and round to nearest, ties to even. */
internal fun roundNearestTieEven(fp: ExtendedFloat, shift: Int) {
    val (isAbove, isHalfway) = roundNearest(fp, shift)
    tieEven(fp, isAbove, isHalfway)
}

/** Shift right by [shift] bits and return whether any truncated bits were non-zero. */
private fun roundToward(fp: ExtendedFloat, shift: Int): Boolean {
    val mask = lowerNMask(shift.toLong().toULong())
    val truncatedBits = fp.mant and mask
    overflowingShr(fp, shift)
    return truncatedBits != 0uL
}

/** Directed downward rounding post-step (no-op toward zero). */
private fun downard(fp: ExtendedFloat, isTruncated: Boolean) {
    if (isTruncated && fp.mant == 0uL) {
        fp.mant = 0uL
    }
}

/** Shift right by [shift] bits and round toward zero. */
internal fun roundDownward(fp: ExtendedFloat, shift: Int) {
    val isTruncated = roundToward(fp, shift)
    downard(fp, isTruncated)
}

/** Shift the [ExtendedFloat] significand to the target float fraction width. */
internal fun roundToFloat(
    fp: ExtendedFloat,
    floatTraits: FloatTraits,
    algorithm: (ExtendedFloat, Int) -> Unit,
) {
    val finalExp =
        I32Num.checkedAdd(fp.exp, floatTraits.defaultShift)
            ?: throw LexicalPanicException("attempt to add with overflow")
    if (finalExp < floatTraits.denormalExponent) {
        val diff =
            I32Num.checkedSub(floatTraits.denormalExponent, fp.exp)
                ?: throw LexicalPanicException("attempt to subtract with overflow")
        if (diff <= MantissaTraits.FULL) {
            algorithm(fp, diff)
        } else {
            fp.mant = 0uL
            fp.exp = 0
        }
    } else {
        algorithm(fp, floatTraits.defaultShift)
    }

    if ((fp.mant and floatTraits.carryMask) == floatTraits.carryMask) {
        shr(fp, 1)
    }
}

/** Shift left if needed to avoid false overflow near the maximum exponent. */
internal fun avoidOverflow(fp: ExtendedFloat, floatTraits: FloatTraits) {
    if (fp.exp >= floatTraits.maxExponent) {
        val diff =
            I32Num.checkedSub(fp.exp, floatTraits.maxExponent)
                ?: throw LexicalPanicException("attempt to subtract with overflow")
        if (diff <= floatTraits.mantissaSize) {
            val bitCount =
                I32Num.checkedAdd(floatTraits.mantissaSize, 1)
                    ?: throw LexicalPanicException("attempt to add with overflow")
            val widthCount =
                I32Num.checkedAdd(diff, 1)
                    ?: throw LexicalPanicException("attempt to add with overflow")
            val mask = internalNMask(bitCount.toLong().toULong(), widthCount.toLong().toULong())
            if ((fp.mant and mask) == 0uL) {
                shl(fp, widthCount)
            }
        }
    }
}

/** Normalize and round an [ExtendedFloat] to native [floatTraits] precision. */
internal fun roundToNative(
    fp: ExtendedFloat,
    floatTraits: FloatTraits,
    algorithm: (ExtendedFloat, Int) -> Unit,
) {
    fp.normalize()
    roundToFloat(fp, floatTraits, algorithm)
    avoidOverflow(fp, floatTraits)
}
