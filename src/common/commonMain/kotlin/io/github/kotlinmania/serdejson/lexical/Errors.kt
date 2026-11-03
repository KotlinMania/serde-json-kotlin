// port-lint: source lexical/errors.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.numtraits.I32Num
import io.github.kotlinmania.numtraits.ops.overflowingAdd
import io.github.kotlinmania.numtraits.ops.wrappingAdd
import io.github.kotlinmania.numtraits.ops.wrappingSub

/**
 * Error estimation parameters for extended-precision floating-point approximations.
 */
internal interface FloatErrors {
    /** Return the full error scale. */
    fun errorScale(): UInt

    /** Return half of the error scale. */
    fun errorHalfscale(): UInt

    /** Determine whether [count] accumulated error units remain accurate for [fp]. */
    fun errorIsAccurate(count: UInt, fp: ExtendedFloat): Boolean
}

/** Check whether [errors] is accurate under round-to-nearest rounding. */
private fun nearestErrorIsAccurate(errors: ULong, fp: ExtendedFloat, extrabits: ULong): Boolean {
    if (extrabits == 65uL) {
        return !fp.mant.overflowingAdd(errors).overflowed
    }
    val mask = lowerNMask(extrabits)
    val extra = fp.mant and mask
    val halfway = lowerNHalfway(extrabits)
    val cmp1 = halfway.wrappingSub(errors) < extra
    val cmp2 = extra < halfway.wrappingAdd(errors)
    return !(cmp1 && cmp2)
}

/** Determine whether [count] error units can be resolved accurately for [floatTraits]. */
internal fun errorIsAccurate(floatTraits: FloatTraits, count: UInt, fp: ExtendedFloat): Boolean {
    val unshiftedBias =
        I32Num.checkedSub(floatTraits.exponentBias, floatTraits.mantissaSize)
            ?: throw LexicalPanicException("attempt to subtract with overflow")
    val bias =
        I32Num.checkedSub(0, unshiftedBias)
            ?: throw LexicalPanicException("attempt to subtract with overflow")
    val denormalExp =
        I32Num.checkedSub(bias, 63)
            ?: throw LexicalPanicException("attempt to subtract with overflow")
    val extrabitsInt =
        if (fp.exp <= denormalExp) {
            val base =
                I32Num.checkedSub(64, floatTraits.mantissaSize)
                    ?: throw LexicalPanicException("attempt to subtract with overflow")
            val withDenormal =
                I32Num.checkedAdd(base, denormalExp)
                    ?: throw LexicalPanicException("attempt to add with overflow")
            I32Num.checkedSub(withDenormal, fp.exp)
                ?: throw LexicalPanicException("attempt to subtract with overflow")
        } else {
            I32Num.checkedSub(63, floatTraits.mantissaSize)
                ?: throw LexicalPanicException("attempt to subtract with overflow")
        }
    val extrabits = extrabitsInt.toLong().toULong()
    val errors = count.toULong()
    if (extrabits > 65uL) {
        return true
    }
    return nearestErrorIsAccurate(errors, fp, extrabits)
}

/** [FloatErrors] implementation for 64-bit mantissas. */
internal object ULongFloatErrors : FloatErrors {
    override fun errorScale(): UInt = 8u

    override fun errorHalfscale(): UInt = errorScale() / 2u

    override fun errorIsAccurate(count: UInt, fp: ExtendedFloat): Boolean =
        errorIsAccurate(F64Float, count, fp)
}
