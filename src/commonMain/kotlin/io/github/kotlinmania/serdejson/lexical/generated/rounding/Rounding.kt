// port-lint: source lexical/rounding.rs
package io.github.kotlinmania.serdejson.lexical.generated.rounding

import io.github.kotlinmania.numtraits.checkedShl as __rtkcheckedShl
// Generated from Syn AST; exact original retained in input.rs.
// FnOnce parameters transfer callback use to this call. Kotlin providers must relinquish the callback and keep mutable loans nonescaping; custom capture Drop and allocation timing are not modeled.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Defines rounding schemes for floating-point numbers.
// Mutable record arguments require exclusive caller access during each call; shared arguments must not be mutated through aliases.
// Compiler-checked scalar Copy contract crate::lexical::float::ExtendedFloat
// Source /Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/float.rs:14; methods/traits are not translated here.
/// Extended precision floating-point type.
///
/// Private implementation, exposed only for testing purposes.
internal class `ExtendedFloat` internal constructor(
    /// Mantissa for the extended-precision float.
    var `mant`: kotlin.ULong,
    /// Binary exponent for the extended-precision float.
    var `exp`: kotlin.Int,
) {
    fun copyValue(): `ExtendedFloat` = `ExtendedFloat`(`mant`, `exp`)
}

// Rust use resolved through explicit library bindings.
// Rust use resolved through explicit library bindings.
// Rust use resolved through explicit library bindings.
// Rust use resolved through explicit library bindings.

// MASKS

/// Calculate a scalar factor of 2 above the halfway point.
// Rust #[inline]: optimization hint.
internal fun `nth_bit`(`n`: kotlin.ULong): kotlin.ULong = kotlin.run {
    val `bits`: kotlin.ULong = (io.github.kotlinmania.numtraits.U64Num.checkedMul(8uL, 8uL) ?: throw __rtkPanic("attempt to multiply with overflow"))
    kotlin.run { if (true && !((`n` < `bits`))) throw __rtkPanic("" + "nth_bit() overflow in shl.") }
    kotlin.run { val __rtkValue0 = 1uL; val __rtkValue1 = `n`; if (__rtkValue1 > kotlin.UInt.MAX_VALUE.toULong()) throw __rtkPanic("attempt to shift with overflow"); __rtkValue0.__rtkcheckedShl(__rtkValue1.toUInt()) ?: throw __rtkPanic("attempt to shift with overflow") }
}

/// Generate a bitwise mask for the lower `n` bits.
// Rust #[inline]: optimization hint.
internal fun `lower_n_mask`(`n`: kotlin.ULong): kotlin.ULong = kotlin.run {
    val `bits`: kotlin.ULong = (io.github.kotlinmania.numtraits.U64Num.checkedMul(8uL, 8uL) ?: throw __rtkPanic("attempt to multiply with overflow"))
    kotlin.run { if (true && !((`n` <= `bits`))) throw __rtkPanic("" + "lower_n_mask() overflow in shl.") }
    (if ((`n` == `bits`)) kotlin.ULong.MAX_VALUE else ((io.github.kotlinmania.numtraits.U64Num.checkedSub((kotlin.run { val __rtkValue0 = 1uL; val __rtkValue1 = `n`; if (__rtkValue1 > kotlin.UInt.MAX_VALUE.toULong()) throw __rtkPanic("attempt to shift with overflow"); __rtkValue0.__rtkcheckedShl(__rtkValue1.toUInt()) ?: throw __rtkPanic("attempt to shift with overflow") }), 1uL) ?: throw __rtkPanic("attempt to subtract with overflow"))))
}

/// Calculate the halfway point for the lower `n` bits.
// Rust #[inline]: optimization hint.
internal fun `lower_n_halfway`(`n`: kotlin.ULong): kotlin.ULong = kotlin.run {
    val `bits`: kotlin.ULong = (io.github.kotlinmania.numtraits.U64Num.checkedMul(8uL, 8uL) ?: throw __rtkPanic("attempt to multiply with overflow"))
    kotlin.run { if (true && !((`n` <= `bits`))) throw __rtkPanic("" + "lower_n_halfway() overflow in shl.") }
    (if ((`n` == 0uL)) 0uL else (`nth_bit`((io.github.kotlinmania.numtraits.U64Num.checkedSub(`n`, 1uL) ?: throw __rtkPanic("attempt to subtract with overflow")))))
}

/// Calculate a bitwise mask with `n` 1 bits starting at the `bit` position.
// Rust #[inline]: optimization hint.
internal fun `internal_n_mask`(`bit`: kotlin.ULong, `n`: kotlin.ULong): kotlin.ULong = kotlin.run {
    val `bits`: kotlin.ULong = (io.github.kotlinmania.numtraits.U64Num.checkedMul(8uL, 8uL) ?: throw __rtkPanic("attempt to multiply with overflow"))
    kotlin.run { if (true && !((`bit` <= `bits`))) throw __rtkPanic("" + "internal_n_halfway() overflow in shl.") }
    kotlin.run { if (true && !((`n` <= `bits`))) throw __rtkPanic("" + "internal_n_halfway() overflow in shl.") }
    kotlin.run { if (true && !((`bit` >= `n`))) throw __rtkPanic("" + "internal_n_halfway() overflow in sub.") }
    (`lower_n_mask`(`bit`) xor `lower_n_mask`((io.github.kotlinmania.numtraits.U64Num.checkedSub(`bit`, `n`) ?: throw __rtkPanic("attempt to subtract with overflow"))))
}


// NEAREST ROUNDING

// Shift right N-bytes and round to the nearest.
//
// Return if we are above halfway and if we are halfway.
// Rust #[inline]: optimization hint.
internal fun `round_nearest`(`fp`: ExtendedFloat, `shift`: kotlin.Int): kotlin.Pair<kotlin.Boolean, kotlin.Boolean> = kotlin.run {
    // Extract the truncated bits using mask.
    // Calculate if the value of the truncated bits are either above
    // the mid-way point, or equal to it.
    //
    // For example, for 4 truncated bytes, the mask would be b1111
    // and the midway point would be b1000.
    val `mask`: kotlin.ULong = `lower_n_mask`((`shift`).toLong().toULong())
    val `halfway`: kotlin.ULong = `lower_n_halfway`((`shift`).toLong().toULong())
    val `truncated_bits`: kotlin.ULong = ((`fp`).`mant` and `mask`)
    val `is_above`: kotlin.Boolean = (`truncated_bits` > `halfway`)
    val `is_halfway`: kotlin.Boolean = (`truncated_bits` == `halfway`)
    
    // Bit shift so the leading bit is in the hidden bit.
    kotlin.run {
        val __rtkValue0 = `fp`
        val __rtkValue2: kotlin.Int = `shift`
        val __rtkValue1 = `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`ExtendedFloat`(`mant` = __rtkValue0.`mant`, `exp` = __rtkValue0.`exp`)
        try {
            `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`overflowingShr`(__rtkValue1, __rtkValue2)
        } finally {
            __rtkValue0.`mant` = __rtkValue1.`mant`
            __rtkValue0.`exp` = __rtkValue1.`exp`
        }
    }
    kotlin.Pair(`is_above`, `is_halfway`)
}


// Tie rounded floating point to event.
// Rust #[inline]: optimization hint.
internal fun `tie_even`(`fp`: ExtendedFloat, `is_above`: kotlin.Boolean, `is_halfway`: kotlin.Boolean): kotlin.Unit = kotlin.run {
    // Extract the last bit after shifting (and determine if it is odd).
    val `is_odd`: kotlin.Boolean = (((`fp`).`mant` and 1uL) == 1uL)
    (
    // Calculate if we need to roundup.
    // We need to roundup if we are above halfway, or if we are odd
    // and at half-way (need to tie-to-even).
    kotlin.run { if ((`is_above` || ((`is_odd` && `is_halfway`)))) kotlin.run {
        kotlin.run { val __rtkValue0: kotlin.ULong = 1uL; (`fp`).`mant` = (io.github.kotlinmania.numtraits.U64Num.checkedAdd((`fp`).`mant`, __rtkValue0) ?: throw __rtkPanic("attempt to add with overflow")) }
    } }
    )
}


// Shift right N-bytes and round nearest, tie-to-even.
//
// Floating-point arithmetic uses round to nearest, ties to even,
// which rounds to the nearest value, if the value is halfway in between,
// round to an even value.
// Rust #[inline]: optimization hint.
internal fun `round_nearest_tie_even`(`fp`: ExtendedFloat, `shift`: kotlin.Int): kotlin.Unit = kotlin.run {
    val __rtkValue0: kotlin.Pair<kotlin.Boolean, kotlin.Boolean> = `round_nearest`(`fp`, `shift`)
    val `is_above`: kotlin.Boolean = (__rtkValue0).first
    val `is_halfway`: kotlin.Boolean = (__rtkValue0).second
    `tie_even`(`fp`, `is_above`, `is_halfway`)
}


// DIRECTED ROUNDING

// Shift right N-bytes and round towards a direction.
//
// Return if we have any truncated bytes.
// Rust #[inline]: optimization hint.
private fun `round_toward`(`fp`: ExtendedFloat, `shift`: kotlin.Int): kotlin.Boolean = kotlin.run {
    val `mask`: kotlin.ULong = `lower_n_mask`((`shift`).toLong().toULong())
    val `truncated_bits`: kotlin.ULong = ((`fp`).`mant` and `mask`)
    
    // Bit shift so the leading bit is in the hidden bit.
    kotlin.run {
        val __rtkValue0 = `fp`
        val __rtkValue2: kotlin.Int = `shift`
        val __rtkValue1 = `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`ExtendedFloat`(`mant` = __rtkValue0.`mant`, `exp` = __rtkValue0.`exp`)
        try {
            `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`overflowingShr`(__rtkValue1, __rtkValue2)
        } finally {
            __rtkValue0.`mant` = __rtkValue1.`mant`
            __rtkValue0.`exp` = __rtkValue1.`exp`
        }
    }
    (`truncated_bits` != 0uL)
}


// Round down.
// Rust #[inline]: optimization hint.
private fun `downard`(`__rtkValue0`: ExtendedFloat, `__rtkValue1`: kotlin.Boolean): kotlin.Unit = kotlin.Unit


// Shift right N-bytes and round toward zero.
//
// Floating-point arithmetic defines round toward zero, which rounds
// towards positive zero.
// Rust #[inline]: optimization hint.
internal fun `round_downward`(`fp`: ExtendedFloat, `shift`: kotlin.Int): kotlin.Unit = kotlin.run {
    // Bit shift so the leading bit is in the hidden bit.
    // No rounding schemes, so we just ignore everything else.
    val `is_truncated`: kotlin.Boolean = `round_toward`(`fp`, `shift`)
    `downard`(`fp`, `is_truncated`)
}


// ROUND TO FLOAT

// Shift the ExtendedFloat fraction to the fraction bits in a native float.
//
// Floating-point arithmetic uses round to nearest, ties to even,
// which rounds to the nearest value, if the value is halfway in between,
// round to an even value.
// Rust #[inline]: optimization hint.
internal fun `round_to_float`(`fp`: ExtendedFloat, `algorithm`: (ExtendedFloat, kotlin.Int) -> kotlin.Unit, `__rtkConst0`: `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`FloatTraits`): kotlin.Unit = kotlin.run {
    // Calculate the difference to allow a single calculation
    // rather than a loop, to minimize the number of ops required.
    // This does underflow detection.
    val `final_exp`: kotlin.Int = __rtkAdd((`fp`).`exp`, `__rtkConst0`.`defaultShift`)
    (if ((`final_exp` < `__rtkConst0`.`denormalExponent`)) kotlin.run {
        // We would end up with a denormal exponent, try to round to more
        // digits. Only shift right if we can avoid zeroing out the value,
        // which requires the exponent diff to be < M::BITS. The value
        // is already normalized, so we shouldn't have any issue zeroing
        // out the value.
        val `diff`: kotlin.Int = __rtkSubI32(`__rtkConst0`.`denormalExponent`, (`fp`).`exp`)
        (if ((`diff` <= `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`MantissaTraits`.`FULL`)) kotlin.run {
            // We can avoid underflow, can get a valid representation.
            (`algorithm`)(`fp`, `diff`)
        } else (kotlin.run {
            // Certain underflow, assign literal 0s.
            kotlin.run { val __rtkValue0: kotlin.ULong = 0uL; (`fp`).`mant` = __rtkValue0 }
            kotlin.run { val __rtkValue1: kotlin.Int = 0; (`fp`).`exp` = __rtkValue1 }
        }))
    } else (kotlin.run {
        (`algorithm`)(`fp`, `__rtkConst0`.`defaultShift`)
    }))
    kotlin.run { if ((((`fp`).`mant` and `__rtkConst0`.`carryMask`) == `__rtkConst0`.`carryMask`)) kotlin.run {
        // Roundup carried over to 1 past the hidden bit.
        kotlin.run {
            val __rtkValue2 = `fp`
            val __rtkValue4: kotlin.Int = 1
            val __rtkValue3 = `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`ExtendedFloat`(`mant` = __rtkValue2.`mant`, `exp` = __rtkValue2.`exp`)
            try {
                `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`shr`(__rtkValue3, __rtkValue4)
            } finally {
                __rtkValue2.`mant` = __rtkValue3.`mant`
                __rtkValue2.`exp` = __rtkValue3.`exp`
            }
        }
    } }
}


// AVOID OVERFLOW/UNDERFLOW

// Avoid overflow for large values, shift left as needed.
//
// Shift until a 1-bit is in the hidden bit, if the mantissa is not 0.
// Rust #[inline]: optimization hint.
internal fun `avoid_overflow`(`fp`: ExtendedFloat, `__rtkConst0`: `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`FloatTraits`): kotlin.Unit = (// Calculate the difference to allow a single calculation
// rather than a loop, minimizing the number of ops required.
kotlin.run { if (((`fp`).`exp` >= `__rtkConst0`.`maxExponent`)) kotlin.run {
    val `diff`: kotlin.Int = __rtkSubI32((`fp`).`exp`, `__rtkConst0`.`maxExponent`)
    kotlin.run { if ((`diff` <= `__rtkConst0`.`mantissaSize`)) kotlin.run {
        // Our overflow mask needs to start at the hidden bit, or at
        // `F::MANTISSA_SIZE+1`, and needs to have `diff+1` bits set,
        // to see if our value overflows.
        val `bit`: kotlin.ULong = ((__rtkAdd(`__rtkConst0`.`mantissaSize`, 1))).toLong().toULong()
        val `n`: kotlin.ULong = ((__rtkAdd(`diff`, 1))).toLong().toULong()
        val `mask`: kotlin.ULong = `internal_n_mask`(`bit`, `n`)
        kotlin.run { if (((((`fp`).`mant` and `mask`)) == 0uL)) kotlin.run {
            // If we have no 1-bit in the hidden-bit position,
            // which is index 0, we need to shift 1.
            val `shift`: kotlin.Int = __rtkAdd(`diff`, 1)
            kotlin.run {
                val __rtkValue0 = `fp`
                val __rtkValue2: kotlin.Int = `shift`
                val __rtkValue1 = `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`ExtendedFloat`(`mant` = __rtkValue0.`mant`, `exp` = __rtkValue0.`exp`)
                try {
                    `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`shl`(__rtkValue1, __rtkValue2)
                } finally {
                    __rtkValue0.`mant` = __rtkValue1.`mant`
                    __rtkValue0.`exp` = __rtkValue1.`exp`
                }
            }
        } }
    } }
} }
)


// ROUND TO NATIVE

// Round an extended-precision float to a native float representation.
// Rust #[inline]: optimization hint.
internal fun `round_to_native`(`fp`: ExtendedFloat, `algorithm`: (ExtendedFloat, kotlin.Int) -> kotlin.Unit, `__rtkConst0`: `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`FloatTraits`): kotlin.Unit = kotlin.run {
    // Shift all the way left, to ensure a consistent representation.
    // The following right-shifts do not work for a non-normalized number.
    __rtkDiscard(kotlin.run {
        val __rtkValue0 = `fp`
        val __rtkValue1 = `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`ExtendedFloat`(`mant` = __rtkValue0.`mant`, `exp` = __rtkValue0.`exp`)
        try {
            `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`normalizeForGenerated`(__rtkValue1)
        } finally {
            __rtkValue0.`mant` = __rtkValue1.`mant`
            __rtkValue0.`exp` = __rtkValue1.`exp`
        }
    })
    
    // Round so the fraction is in a native mantissa representation,
    // and avoid overflow/underflow.
    `round_to_float`(`fp`, `algorithm`, `__rtkConst0`)
    `avoid_overflow`(`fp`, `__rtkConst0`)
}

class __rtkPanic(message: String) : kotlin.RuntimeException(message)
private fun __rtkSubI32(left: Int, right: Int): Int {
    val difference = left.toLong() - right.toLong()
    if (difference < Int.MIN_VALUE || difference > Int.MAX_VALUE) throw __rtkPanic("attempt to subtract with overflow")
    return difference.toInt()
}
// Selected Rust overflow-checks=on, panic=unwind; host hooks/ABI are not modeled.
private fun __rtkAdd(left: Int, right: Int): Int {
    val sum = left.toLong() + right.toLong()
    if (sum < Int.MIN_VALUE || sum > Int.MAX_VALUE) throw __rtkPanic("attempt to add with overflow")
    return sum.toInt()
}
@Suppress("UNUSED_PARAMETER") private fun <T> __rtkDiscard(value: T): kotlin.Unit = kotlin.Unit
