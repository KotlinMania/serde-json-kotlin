// port-lint: source lexical/float.rs
package io.github.kotlinmania.serdejson.lexical

/**
 * Extended precision floating-point type.
 *
 * Used for the bignum-based float round-tripping algorithm.
 */
internal data class ExtendedFloat(
    /** Mantissa for the extended-precision float. */
    var mant: ULong,
    /** Binary exponent for the extended-precision float. */
    var exp: Int,
) {
    /**
     * Normalize the float-point number.
     *
     * Shift the mantissa so the number of leading zeros is 0, or the value
     * itself is 0. Returns the number of bits shifted.
     */
    fun normalize(): Int {
        val shift =
            if (mant == 0UL) {
                0
            } else {
                mant.countLeadingZeroBits()
            }
        if (shift > 0) {
            shl(this, shift)
        }
        return shift
    }

    /**
     * Multiply two normalized extended-precision floats, as if by `a*b`.
     *
     * The result is not normalized.
     */
    fun mul(b: ExtendedFloat): ExtendedFloat {
        val ah = (mant shr MantissaTraits.HALF)
        val al = (mant and MantissaTraits.LOMASK)
        val bh = (b.mant shr MantissaTraits.HALF)
        val bl = (b.mant and MantissaTraits.LOMASK)

        val ahBl = ah * bl
        val alBh = al * bh
        val alBl = al * bl
        val ahBh = ah * bh

        var tmp = (ahBl and MantissaTraits.LOMASK) + (alBh and MantissaTraits.LOMASK) + (alBl shr MantissaTraits.HALF)
        tmp += (1UL shl (MantissaTraits.HALF - 1))

        return ExtendedFloat(
            ahBh + (ahBl shr MantissaTraits.HALF) + (alBh shr MantissaTraits.HALF) + (tmp shr MantissaTraits.HALF),
            exp + b.exp + MantissaTraits.FULL,
        )
    }

    /** Multiply in-place, as if by `a*b`. The result is not normalized. */
    fun imul(b: ExtendedFloat) {
        val result = mul(b)
        mant = result.mant
        exp = result.exp
    }

    /**
     * Lossy round float-point number to native mantissa boundaries.
     */
    internal fun roundToNative(floatTraits: FloatTraits, algorithm: (ExtendedFloat, Int) -> Unit) {
        roundToNative(this, floatTraits, algorithm)
    }

    companion object {
        /** Create extended float from native float. */
        internal fun fromFloat(f: Double, floatTraits: FloatTraits): ExtendedFloat =
            ExtendedFloat(floatTraits.mantissa(f), floatTraits.exponent(f))
    }

    /** Convert into default-rounded, lower-precision native float. */
    internal fun intoFloat(floatTraits: FloatTraits): Double {
        roundToNative(floatTraits, ::roundNearestTieEven)
        return intoFloat(this, floatTraits)
    }

    /** Convert into downward-rounded, lower-precision native float. */
    internal fun intoDownwardFloat(floatTraits: FloatTraits): Double {
        roundToNative(floatTraits, ::roundDownward)
        return intoFloat(this, floatTraits)
    }
}

/** Export extended-precision float to native float. */
internal fun intoFloat(fp: ExtendedFloat, floatTraits: FloatTraits): Double {
    if (fp.mant == 0UL || fp.exp < floatTraits.denormalExponent) {
        return 0.0
    } else if (fp.exp >= floatTraits.maxExponent) {
        return floatTraits.fromBits(floatTraits.infinityBits)
    } else {
        val exp: ULong =
            if (fp.exp == floatTraits.denormalExponent && (fp.mant and floatTraits.hiddenBitMask) == 0UL) {
                0UL
            } else {
                (fp.exp + floatTraits.exponentBias).toULong()
            }
        val expShifted = exp shl floatTraits.mantissaSize
        val mant = fp.mant and floatTraits.mantissaMask
        return floatTraits.fromBits(mant or expShifted)
    }
}
