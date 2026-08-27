// port-lint: source serde_json/tests/lexical/math.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Building-blocks for arbitrary-precision math.
 *
 * These algorithms assume little-endian order for the large integer
 * buffers, so for a list [0, 1, 2, 3], 3 is the most significant limb,
 * and 0 is the least significant limb.
 */

// Type for a single limb of the big integer. The port uses 64-bit limbs.
internal typealias Limb = ULong

// Wide type for intermediate multiplication results (128-bit in Rust).
// Kotlin has no u128; we use ULong and check overflow manually.
private typealias Wide = ULong

/** Pre-computed powers of 5 for the limb type. */
internal val POW5_LIMB: ULongArray = POW5_64

/** Pre-computed powers of 10 for the limb type. */
internal val POW10_LIMB: ULongArray = POW10_64

// SCALAR
// ------

/**
 * Scalar-to-scalar operations, building-blocks for arbitrary-precision operations.
 */
private object Scalar {
    /** Add two small integers and return the resulting value and if overflow happens. */
    fun add(x: Limb, y: Limb): Pair<Limb, Boolean> {
        val result = x + y
        val overflow = result < x
        return Pair(result, overflow)
    }

    /** AddAssign two small integers and return if overflow happens. */
    fun iadd(x: Limb, y: Limb): Pair<Limb, Boolean> {
        val (result, overflow) = add(x, y)
        return Pair(result, overflow)
    }

    /** Subtract two small integers and return the resulting value and if overflow happens. */
    fun sub(x: Limb, y: Limb): Pair<Limb, Boolean> {
        val result = x - y
        val overflow = result > x
        return Pair(result, overflow)
    }

    /** SubAssign two small integers and return if overflow happens. */
    fun isub(x: Limb, y: Limb): Pair<Limb, Boolean> {
        val (result, overflow) = sub(x, y)
        return Pair(result, overflow)
    }

    /**
     * Multiply two small integers with carry and return the (low, high) components.
     *
     * Kotlin has no native 128-bit type, so we split each 64-bit value into
     * 32-bit halves and perform four 32x32→64 multiplications, then combine
     * with carries. This is equivalent to the Rust `Wide = u128` path.
     */
    fun mul(x: Limb, y: Limb, carry: Limb): Pair<Limb, Limb> {
        // Split into 32-bit halves.
        val xLo = x and 0xFFFF_FFFFU
        val xHi = x shr 32
        val yLo = y and 0xFFFF_FFFFU
        val yHi = y shr 32

        // Four partial products (each fits in 64 bits).
        val ll = xLo * yLo
        val lh = xLo * yHi
        val hl = xHi * yLo
        val hh = xHi * yHi

        // mid = lh + hl (may overflow into a 65th bit).
        val mid = lh + hl
        val midCarry = if (mid < lh) 1UL else 0UL

        // Low 64 = ll + (mid << 32).  The upper 32 bits of mid shift out.
        val midShl32Lo = mid shl 32
        val midShl32Hi = mid shr 32

        val s1 = ll + midShl32Lo
        val s1Carry = if (s1 < ll) 1UL else 0UL

        // Add the external carry to the low word.
        val low = s1 + carry
        val s2Carry = if (low < s1) 1UL else 0UL

        // High 64 = hh + midCarry << 32 + midShl32Hi + s1Carry + s2Carry.
        val high = hh + (midCarry shl 32) + midShl32Hi + s1Carry + s2Carry

        return Pair(low, high)
    }

    /** Multiply two small integers with carry and return the overflow contribution. */
    fun imul(x: Limb, y: Limb, carry: Limb): Pair<Limb, Limb> = mul(x, y, carry)
}

// HI64
// ----

/**
 * Check if any of the remaining bits are non-zero.
 */
internal fun nonzero(x: List<Limb>, rindex: Int): Boolean {
    val len = x.size
    val end = len - rindex
    for (i in end - 1 downTo 0) {
        if (x[i] != 0UL) return true
    }
    return false
}

/** Shift 64-bit integer to high 64-bits. */
private fun u64ToHi64_1(r0: ULong): Pair<ULong, Boolean> {
    val ls = r0.countLeadingZeroBits()
    return Pair(r0 shl ls, false)
}

/** Shift 2 64-bit integers to high 64-bits. */
private fun u64ToHi64_2(r0: ULong, r1: ULong): Pair<ULong, Boolean> {
    val ls = r0.countLeadingZeroBits()
    val rs = 64 - ls
    val v =
        if (ls == 0) {
            r0
        } else {
            (r0 shl ls) or (r1 shr rs)
        }
    val n = (r1 shl ls) != 0UL
    return Pair(v, n)
}

/**
 * Extract the high 64 bits from a little-endian limb slice.
 */
internal fun hi64(data: List<Limb>): Pair<ULong, Boolean> =
    when (data.size) {
        0 -> Pair(0UL, false)
        1 -> u64ToHi64_1(data[0])
        2 -> {
            val r0 = data[data.size - 1]
            val r1 = data[data.size - 2]
            val (v, n) = u64ToHi64_2(r0, r1)
            Pair(v, n || nonzero(data, 2))
        }
        else -> {
            val r0 = data[data.size - 1]
            val r1 = data[data.size - 2]
            val (v, n) = u64ToHi64_2(r0, r1)
            Pair(v, n || nonzero(data, 3))
        }
    }

// SMALL
// -----

/**
 * Large-to-small operations, to modify a big integer from a native scalar.
 */
private object Small {
    /** Implied AddAssign implementation for adding a small integer to bigint at a start index. */
    fun iaddImpl(x: MutableList<Limb>, y: Limb, xstart: Int) {
        if (x.size <= xstart) {
            x.add(y)
        } else {
            var (value, carry) = Scalar.iadd(x[xstart], y)
            x[xstart] = value
            var size = xstart + 1
            while (carry && size < x.size) {
                val t = Scalar.iadd(x[size], 1UL)
                x[size] = t.first
                carry = t.second
                size++
            }
            if (carry) {
                x.add(1UL)
            }
        }
    }

    /** AddAssign small integer to bigint. */
    fun iadd(x: MutableList<Limb>, y: Limb) {
        iaddImpl(x, y, 0)
    }

    /** SubAssign small integer to bigint at a start index. */
    fun isubImpl(x: MutableList<Limb>, y: Limb, xstart: Int) {
        var (value, carry) = Scalar.isub(x[xstart], y)
        x[xstart] = value
        var size = xstart + 1
        while (carry && size < x.size) {
            val t = Scalar.isub(x[size], 1UL)
            x[size] = t.first
            carry = t.second
            size++
        }
        normalize(x)
    }

    /** MulAssign small integer to bigint. */
    fun imul(x: MutableList<Limb>, y: Limb) {
        var carry: Limb = 0UL
        for (i in x.indices) {
            val (low, high) = Scalar.imul(x[i], y, carry)
            x[i] = low
            carry = high
        }
        if (carry != 0UL) {
            x.add(carry)
        }
    }

    /** Mul small integer to bigint, returning a new list. */
    fun mul(x: List<Limb>, y: Limb): MutableList<Limb> {
        val z = x.toMutableList()
        imul(z, y)
        return z
    }

    /** MulAssign by a power of 5. */
    fun imulPow5(x: MutableList<Limb>, n: Int) {
        if (n == 0) return

        val smallPowers = POW5_LIMB
        val largePowers = POW5

        val bitLength = 32 - n.countLeadingZeroBits()
        if (x.size + largePowers[bitLength - 1].size < 2 * KARATSUBA_CUTOFF) {
            val step = smallPowers.size - 1
            val power = smallPowers[step]
            var remaining = n
            while (remaining >= step) {
                imul(x, power)
                remaining -= step
            }
            imul(x, smallPowers[remaining])
        } else {
            var idx = 0
            var bit = 1
            var remaining = n
            while (remaining != 0) {
                if (remaining and bit != 0) {
                    Large.imul(x, largePowers[idx].toList())
                    remaining = remaining xor bit
                }
                idx++
                bit = bit shl 1
            }
        }
    }

    /** Get number of leading zero bits in the storage. */
    fun leadingZeros(x: List<Limb>): Int =
        if (x.isEmpty()) 0 else x.last().countLeadingZeroBits()

    /** Calculate the bit-length of the big-integer. */
    fun bitLength(x: List<Limb>): Int {
        val bits = 64
        val nlz = leadingZeros(x)
        val total = bits * x.size
        return if (x.isEmpty()) 0 else total - nlz
    }

    /** Shift-left bits inside a buffer. Assumes n < 64. */
    fun ishlBits(x: MutableList<Limb>, n: Int) {
        if (n == 0) return
        val rshift = 64 - n
        val lshift = n
        var prev: Limb = 0UL
        for (i in x.indices) {
            val tmp = x[i]
            x[i] = (tmp shl lshift) or (prev shr rshift)
            prev = tmp
        }
        val carry = prev shr rshift
        if (carry != 0UL) {
            x.add(carry)
        }
    }

    /** Shift-left n digits (limbs) inside a buffer. Assumes n is not 0. */
    fun ishlLimbs(x: MutableList<Limb>, n: Int) {
        if (x.isNotEmpty()) {
            repeat(n) { x.add(0, 0UL) }
        }
    }

    /** Shift-left buffer by n bits. */
    fun ishl(x: MutableList<Limb>, n: Int) {
        val bits = 64
        val rem = n % bits
        val div = n / bits
        ishlBits(x, rem)
        if (div != 0) {
            ishlLimbs(x, div)
        }
    }

    /** Normalize the container by popping any leading zeros. */
    fun normalize(x: MutableList<Limb>) {
        while (x.isNotEmpty() && x.last() == 0UL) {
            x.removeAt(x.size - 1)
        }
    }
}

// LARGE
// -----

/**
 * Large-to-large operations, to modify a big integer from another big integer.
 */
private object Large {
    /** Compare x to y, in little-endian order. */
    fun compare(x: List<Limb>, y: List<Limb>): Int {
        if (x.size > y.size) return 1
        if (x.size < y.size) return -1
        for (i in x.size - 1 downTo 0) {
            if (x[i] > y[i]) return 1
            if (x[i] < y[i]) return -1
        }
        return 0
    }

    /** Check if x is less than y. */
    fun less(x: List<Limb>, y: List<Limb>): Boolean = compare(x, y) < 0

    /** Check if x is greater than or equal to y. */
    fun greaterEqual(x: List<Limb>, y: List<Limb>): Boolean = !less(x, y)

    /** Implied AddAssign implementation for bigints at a start index. */
    fun iaddImpl(x: MutableList<Limb>, y: List<Limb>, xstart: Int) {
        if (y.size > x.size - xstart) {
            repeat(y.size - (x.size - xstart)) { x.add(0UL) }
        }
        var carry = false
        for (i in y.indices) {
            val (value, overflow) = Scalar.iadd(x[xstart + i], y[i])
            x[xstart + i] = value
            if (overflow) carry = true
            if (carry) {
                val t = Scalar.iadd(x[xstart + i], 1UL)
                x[xstart + i] = t.first
                carry = t.second
            }
        }
        if (carry) {
            Small.iaddImpl(x, 1UL, y.size + xstart)
        }
    }

    /** AddAssign bigint to bigint. */
    fun iadd(x: MutableList<Limb>, y: List<Limb>) {
        iaddImpl(x, y, 0)
    }

    /** Add bigint to bigint, returning a new list. */
    fun add(x: List<Limb>, y: List<Limb>): MutableList<Limb> {
        val z = x.toMutableList()
        iadd(z, y)
        return z
    }

    /** SubAssign bigint to bigint. */
    fun isub(x: MutableList<Limb>, y: List<Limb>) {
        var carry = false
        for (i in y.indices) {
            val (value, overflow) = Scalar.isub(x[i], y[i])
            x[i] = value
            if (overflow) carry = true
            if (carry) {
                val t = Scalar.isub(x[i], 1UL)
                x[i] = t.first
                carry = t.second
            }
        }
        if (carry) {
            Small.isubImpl(x, 1UL, y.size)
        } else {
            Small.normalize(x)
        }
    }

    /** Grade-school multiplication algorithm. */
    private fun longMul(x: List<Limb>, y: List<Limb>): MutableList<Limb> {
        val z = Small.mul(x, y[0])
        while (z.size < x.size + y.size) z.add(0UL)

        for (i in 1 until y.size) {
            val zi = Small.mul(x, y[i])
            iaddImpl(z, zi, i)
        }

        Small.normalize(z)
        return z
    }

    /** Split two buffers into halfway, into (lo, hi). */
    private fun karatsubaSplit(z: List<Limb>, m: Int): Pair<List<Limb>, List<Limb>> = Pair(z.subList(0, m), z.subList(m, z.size))

    /** Karatsuba multiplication algorithm with roughly equal input sizes. Assumes y.size >= x.size. */
    private fun karatsubaMul(x: List<Limb>, y: List<Limb>): MutableList<Limb> {
        if (y.size <= KARATSUBA_CUTOFF) {
            return longMul(x, y)
        } else if (x.size < y.size / 2) {
            return karatsubaUnevenMul(x, y)
        } else {
            val m = y.size / 2
            val (xl, xh) = karatsubaSplit(x, m)
            val (yl, yh) = karatsubaSplit(y, m)
            val sumx = add(xl, xh)
            val sumy = add(yl, yh)
            val z0 = karatsubaMul(xl, yl)
            val z1 = karatsubaMul(sumx, sumy)
            val z2 = karatsubaMul(xh, yh)
            isub(z1, z2)
            isub(z1, z0)

            val len = maxOf(z0.size, m + z1.size, 2 * m + z2.size)
            val result = z0
            while (result.size < len) result.add(0UL)
            iaddImpl(result, z1, m)
            iaddImpl(result, z2, 2 * m)
            return result
        }
    }

    /** Karatsuba multiplication algorithm where y is substantially larger than x. */
    private fun karatsubaUnevenMul(x: List<Limb>, y: List<Limb>): MutableList<Limb> {
        val result = MutableList<Limb>(x.size + y.size) { 0UL }
        var start = 0
        var remaining = y
        while (remaining.isNotEmpty()) {
            val m = minOf(x.size, remaining.size)
            val (yl, yh) = karatsubaSplit(remaining, m)
            val prod = karatsubaMul(x, yl)
            iaddImpl(result, prod, start)
            remaining = yh
            start += m
        }
        Small.normalize(result)
        return result
    }

    /** Forwarder to the proper Karatsuba algorithm. */
    private fun karatsubaMulFwd(x: List<Limb>, y: List<Limb>): MutableList<Limb> =
        if (x.size < y.size) karatsubaMul(x, y) else karatsubaMul(y, x)

    /** MulAssign bigint to bigint. */
    fun imul(x: MutableList<Limb>, y: List<Limb>) {
        if (y.size == 1) {
            Small.imul(x, y[0])
        } else {
            val result = karatsubaMulFwd(x, y)
            x.clear()
            x.addAll(result)
        }
    }
}

/** Number of digits to bottom-out to asymptotically slow algorithms. */
private const val KARATSUBA_CUTOFF: Int = 32

// TRAITS
// ------

/**
 * Math operations on a big integer, implemented as extension functions on [Bigint].
 *
 * In the upstream Rust crate these are a trait; in Kotlin we use direct functions
 * on the Bigint data class.
 */

/** Compare self to y. Returns -1 for less, 0 for equal, 1 for greater. */
internal fun Bigint.compare(other: Bigint): Int =
    Large.compare(this.data, other.data)

/** Get the high 64 bits from the bigint and if there are remaining bits. */
internal fun Bigint.hi64(): Pair<ULong, Boolean> =
    hi64(this.data)

/** Calculate the bit-length of the big-integer. */
internal fun Bigint.bitLength(): Int =
    Small.bitLength(this.data)

/** Create a new big integer from a ULong. */
internal fun Bigint.fromU64(x: ULong): Bigint {
    val v = Bigint.default()
    v.data.add(x)
    v.normalize()
    return v
}

/** Normalize the integer, removing any leading zero values. */
internal fun Bigint.normalize() {
    Small.normalize(this.data)
}

/** AddAssign a small integer. */
internal fun Bigint.iaddSmall(y: Limb) {
    Small.iadd(this.data, y)
}

/** MulAssign a small integer. */
internal fun Bigint.imulSmall(y: Limb) {
    Small.imul(this.data, y)
}

/** Multiply by a power of 2. */
internal fun Bigint.imulPow2(n: Int) {
    Small.ishl(this.data, n)
}

/** Multiply by a power of 5. */
internal fun Bigint.imulPow5(n: Int) {
    Small.imulPow5(this.data, n)
}

/** MulAssign by a power of 10. */
internal fun Bigint.imulPow10(n: Int) {
    this.imulPow5(n)
    this.imulPow2(n)
}

/** Shift-left the entire buffer n bits. */
internal fun Bigint.ishl(n: Int) {
    Small.ishl(this.data, n)
}
