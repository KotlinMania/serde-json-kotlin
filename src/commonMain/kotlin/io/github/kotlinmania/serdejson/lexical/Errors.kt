// port-lint: source tmp/serde_json/src/lexical/errors.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Estimate the error in an 80-bit approximation of a float.
 *
 * This implementation is loosely based off the Golang implementation:
 * https://golang.org/src/strconv/atof.go
 */

/**
 * Trait for float error estimation.
 */
internal interface FloatErrors {
    /** Get the full error scale. */
    fun errorScale(): UInt

    /** Get the half error scale. */
    fun errorHalfscale(): UInt

    /** Determine if the number of errors is tolerable for float precision. */
    fun errorIsAccurate(count: UInt, fp: ExtendedFloat): Boolean
}

/**
 * Check if the error is accurate with a round-nearest rounding scheme.
 */
private fun nearestErrorIsAccurate(errors: ULong, fp: ExtendedFloat, extrabits: ULong): Boolean {
    if (extrabits == 65UL) {
        // Underflow: we have a shift larger than the mantissa.
        // Representation is valid only if the value does not overflow
        // to the next bit within errors.
        val result = fp.mant + errors
        // Check for overflow: if result < fp.mant, overflow occurred.
        return result >= fp.mant
    } else {
        val mask = lowerNMask(extrabits)
        val extra = fp.mant and mask

        // Round-to-nearest, need to check if we're close to halfway.
        val halfway = lowerNHalfway(extrabits)
        val cmp1 = halfway - errors < extra
        val cmp2 = extra < halfway + errors

        // If both comparisons are true, we have significant rounding error,
        // and the value cannot be exactly represented.
        return !(cmp1 && cmp2)
    }
}

/**
 * Check if the error is accurate for a specific [FloatTraits] type.
 */
internal fun errorIsAccurate(
    floatTraits: FloatTraits,
    count: UInt,
    fp: ExtendedFloat
): Boolean {
    val bias = -(floatTraits.exponentBias - floatTraits.mantissaSize)
    val denormalExp = bias - 63

    val extrabits: Long = if (fp.exp <= denormalExp) {
        64L - floatTraits.mantissaSize + denormalExp - fp.exp
    } else {
        63L - floatTraits.mantissaSize
    }

    val extrabitsU = extrabits.toULong()
    val errors = count.toULong()

    if (extrabitsU > 65UL) {
        // Underflow, we have a literal 0.
        return true
    }

    return nearestErrorIsAccurate(errors, fp, extrabitsU)
}

/**
 * Implementation of [FloatErrors] for 64-bit mantissa (u64).
 */
internal object ULongFloatErrors : FloatErrors {
    override fun errorScale(): UInt = 8u

    override fun errorHalfscale(): UInt = errorScale() / 2u

    override fun errorIsAccurate(count: UInt, fp: ExtendedFloat): Boolean {
        // Delegate to the generic implementation using f64 traits.
        return io.github.kotlinmania.serdejson.lexical.errorIsAccurate(F64Float, count, fp)
    }
}