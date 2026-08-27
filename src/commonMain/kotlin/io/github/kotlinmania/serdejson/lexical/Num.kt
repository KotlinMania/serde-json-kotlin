// port-lint: source lexical/num.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Utilities for numbers and floating-point traits.
 */

// Precalculated values of radix**i for i in range [0, arr.len()-1].
// Each value can be exactly represented as that type.

/** Pre-calculated powers of 10 for f32. */
internal val F32_POW10: FloatArray =
    floatArrayOf(
        1.0f,
        10.0f,
        100.0f,
        1000.0f,
        10000.0f,
        100000.0f,
        1000000.0f,
        10000000.0f,
        100000000.0f,
        1000000000.0f,
        10000000000.0f,
    )

/** Pre-calculated powers of 10 for f64. */
internal val F64_POW10: DoubleArray =
    doubleArrayOf(
        1.0,
        10.0,
        100.0,
        1000.0,
        10000.0,
        100000.0,
        1000000.0,
        10000000.0,
        100000000.0,
        1000000000.0,
        10000000000.0,
        100000000000.0,
        1000000000000.0,
        10000000000000.0,
        100000000000000.0,
        1000000000000000.0,
        10000000000000000.0,
        100000000000000000.0,
        1000000000000000000.0,
        10000000000000000000.0,
        100000000000000000000.0,
        1000000000000000000000.0,
        10000000000000000000000.0,
    )

/**
 * An interface for casting between machine scalars.
 */
internal interface AsPrimitive {
    fun asU32(): UInt

    fun asU64(): ULong

    fun asU128(): ULong // Kotlin has no u128; represented as ULong

    fun asUSize(): Int

    fun asF32(): Float

    fun asF64(): Double
}

/**
 * Numerical type trait.
 */
internal interface Number

/**
 * Defines a trait that supports integral operations.
 */
internal interface Integer : Number {
    companion object {
        const val ZERO_INT: Int = 0
    }
}

/**
 * Type trait for the mantissa type.
 */
internal interface Mantissa : Integer {
    /** Mask to extract the high bits from the integer. */
    val hiMask: ULong

    /** Mask to extract the low bits from the integer. */
    val loMask: ULong

    /** Full size of the integer, in bits. */
    val full: Int

    /** Half size of the integer, in bits. */
    val half: Int
        get() = full / 2
}

/**
 * Mantissa traits for u64 (ULong).
 */
internal object MantissaTraits : Mantissa {
    override val hiMask: ULong = 0xFFFFFFFF00000000UL
    override val loMask: ULong = 0x00000000FFFFFFFFUL
    override val full: Int = 64
    override val half: Int = 32

    const val ZERO: ULong = 0UL
    const val HIMASK: ULong = 0xFFFFFFFF00000000UL
    const val LOMASK: ULong = 0x00000000FFFFFFFFUL
    const val FULL: Int = 64
    const val HALF: Int = 32
}

/**
 * Float traits interface (equivalent to Rust `Float` trait).
 */
internal interface FloatTraits : Number {
    /** Literal zero. */
    val zero: Double

    /** Maximum number of digits that can contribute in the mantissa. */
    val maxDigits: Int

    // MASKS

    /** Bitmask for the exponent, including the hidden bit. */
    val exponentMask: ULong

    /** Bitmask for the hidden bit in exponent. */
    val hiddenBitMask: ULong

    /** Bitmask for the mantissa (fraction), excluding the hidden bit. */
    val mantissaMask: ULong

    // PROPERTIES

    /** Positive infinity as bits. */
    val infinityBits: ULong

    /** Size of the significand (mantissa) without hidden bit. */
    val mantissaSize: Int

    /** Bias of the exponent. */
    val exponentBias: Int

    /** Exponent portion of a denormal float. */
    val denormalExponent: Int

    /** Maximum exponent value in float. */
    val maxExponent: Int

    // ROUNDING

    /** Default number of bits to shift (or 64 - mantissa size - 1). */
    val defaultShift: Int

    /** Mask to determine if a full-carry occurred (1 in bit above hidden bit). */
    val carryMask: ULong

    /** Get min and max exponent limits (exact) from radix. */
    fun exponentLimit(): Pair<Int, Int>

    /** Get the number of digits that can be shifted from exponent to mantissa. */
    fun mantissaLimit(): Int

    /** Compute self * 10^n. */
    fun pow10(value: Double, n: Int): Double

    /** Create a float from its bit representation. */
    fun fromBits(u: ULong): Double

    /** Get the bit representation of a float. */
    fun toBits(value: Double): ULong

    /** Check if the float's sign is positive. */
    fun isSignPositive(value: Double): Boolean

    /** Returns true if the float is a denormal. */
    fun isDenormal(value: Double): Boolean = (toBits(value) and exponentMask) == 0UL

    /** Returns true if the float is a NaN or Infinite. */
    fun isSpecial(value: Double): Boolean = (toBits(value) and exponentMask) == exponentMask

    /** Returns true if the float is infinite. */
    fun isInf(value: Double): Boolean = isSpecial(value) && (toBits(value) and mantissaMask) == 0UL

    /** Get exponent component from the float. */
    fun exponent(value: Double): Int {
        if (isDenormal(value)) {
            return denormalExponent
        }
        val bits = toBits(value)
        val biasedE = ((bits and exponentMask) shr mantissaSize).toInt()
        return biasedE - exponentBias
    }

    /** Get mantissa (significand) component from float. */
    fun mantissa(value: Double): ULong {
        val bits = toBits(value)
        val s = bits and mantissaMask
        return if (!isDenormal(value)) {
            s + hiddenBitMask
        } else {
            s
        }
    }

    /** Get next greater float for a positive float. Value must be >= 0.0 and < INFINITY. */
    fun nextPositive(value: Double): Double = fromBits(toBits(value) + 1UL)

    /** Round a positive number to even. */
    fun roundPositiveEven(value: Double): Double =
        if ((mantissa(value) and 1UL) == 1UL) {
            nextPositive(value)
        } else {
            value
        }
}

/** f32 Float trait implementation. */
internal object F32Float : FloatTraits {
    override val zero: Double = 0.0
    override val maxDigits: Int = 114
    override val exponentMask: ULong = 0x7F800000UL
    override val hiddenBitMask: ULong = 0x00800000UL
    override val mantissaMask: ULong = 0x007FFFFFUL
    override val infinityBits: ULong = 0x7F800000UL
    override val mantissaSize: Int = 23
    override val exponentBias: Int = 127 + 23
    override val denormalExponent: Int = 1 - exponentBias
    override val maxExponent: Int = 0xFF - exponentBias
    override val defaultShift: Int = MantissaTraits.FULL - mantissaSize - 1
    override val carryMask: ULong = 0x1000000UL

    override fun exponentLimit(): Pair<Int, Int> = Pair(-10, 10)

    override fun mantissaLimit(): Int = 7

    override fun pow10(value: Double, n: Int): Double =
        if (n > 0) {
            value * F32_POW10[n]
        } else {
            value / F32_POW10[-n]
        }

    override fun fromBits(u: ULong): Double = Float.fromBits(u.toInt()).toDouble()

    override fun toBits(value: Double): ULong = value.toFloat().toRawBits().toULong()

    override fun isSignPositive(value: Double): Boolean = !value.toFloat().isNaN() && value >= 0.0
}

/** f64 Float trait implementation. */
internal object F64Float : FloatTraits {
    override val zero: Double = 0.0
    override val maxDigits: Int = 769
    override val exponentMask: ULong = 0x7FF0000000000000UL
    override val hiddenBitMask: ULong = 0x0010000000000000UL
    override val mantissaMask: ULong = 0x000FFFFFFFFFFFFFUL
    override val infinityBits: ULong = 0x7FF0000000000000UL
    override val mantissaSize: Int = 52
    override val exponentBias: Int = 1023 + 52
    override val denormalExponent: Int = 1 - exponentBias
    override val maxExponent: Int = 0x7FF - exponentBias
    override val defaultShift: Int = MantissaTraits.FULL - mantissaSize - 1
    override val carryMask: ULong = 0x20000000000000UL

    override fun exponentLimit(): Pair<Int, Int> = Pair(-22, 22)

    override fun mantissaLimit(): Int = 15

    override fun pow10(value: Double, n: Int): Double =
        if (n > 0) {
            value * F64_POW10[n]
        } else {
            value / F64_POW10[-n]
        }

    override fun fromBits(u: ULong): Double = Double.fromBits(u.toLong())

    override fun toBits(value: Double): ULong = value.toRawBits().toULong()

    override fun isSignPositive(value: Double): Boolean = !value.isNaN() && value >= 0.0
}
