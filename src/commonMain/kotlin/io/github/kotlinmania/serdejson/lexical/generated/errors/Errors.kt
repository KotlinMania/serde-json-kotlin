// port-lint: source lexical/errors.rs
package io.github.kotlinmania.serdejson.lexical.generated.errors

import io.github.kotlinmania.numtraits.ops.overflowingAdd as __rtkoverflowingAdd
import io.github.kotlinmania.numtraits.ops.wrappingSub as __rtkwrappingSub
import io.github.kotlinmania.numtraits.ops.wrappingAdd as __rtkwrappingAdd
// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Estimate the error in an 80-bit approximation of a float.
//!
//! This estimates the error in a floating-point representation.
//!
//! This implementation is loosely based off the Golang implementation,
//! found here: <https://golang.org/src/strconv/atof.go>
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
internal interface `FloatErrors` {
    interface `__rtkStatic`<`__rtkSelf`> {
        /// Get the full error scale.
                fun `error_scale`(): kotlin.UInt
        /// Get the half error scale.
                fun `error_halfscale`(): kotlin.UInt
        /// Determine if the number of errors is tolerable for float precision.
                fun `error_is_accurate`(`count`: kotlin.UInt, `fp`: ExtendedFloat, `__rtkConst0`: `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`FloatTraits`): kotlin.Boolean
    }
}

/// Check if the error is accurate with a round-nearest rounding scheme.
// Rust #[inline]: optimization hint.
private fun `nearest_error_is_accurate`(`errors`: kotlin.ULong, `fp`: ExtendedFloat, `extrabits`: kotlin.ULong): kotlin.Boolean = (// Round-to-nearest, need to use the halfway point.
(if ((`extrabits` == 65uL)) (// Underflow, we have a shift larger than the mantissa.
// Representation is valid **only** if the value is close enough
// overflow to the next bit within errors. If it overflows,
// the representation is **not** valid.
(!(kotlin.run { val __rtkValue0 = ((`fp`).`mant`).__rtkoverflowingAdd(`errors`); kotlin.Pair(__rtkValue0.value, __rtkValue0.overflowed) }).second)
) else (kotlin.run {
    val `mask`: kotlin.ULong = `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`lowerNMask`(`extrabits`)
    val `extra`: kotlin.ULong = ((`fp`).`mant` and `mask`)
    
    // Round-to-nearest, need to check if we're close to halfway.
    // IE, b10100 | 100000, where `|` signifies the truncation point.
    val `halfway`: kotlin.ULong = `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`lowerNHalfway`(`extrabits`)
    val `cmp1`: kotlin.Boolean = ((`halfway`).__rtkwrappingSub(`errors`) < `extra`)
    val `cmp2`: kotlin.Boolean = (`extra` < (`halfway`).__rtkwrappingAdd(`errors`))
    (
    // If both comparisons are true, we have significant rounding error,
    // and the value cannot be exactly represented. Otherwise, the
    // representation is valid.
    (!((`cmp1` && `cmp2`)))
    )
}))
)

internal object `__rtkImpl0` : `FloatErrors`.`__rtkStatic`<kotlin.ULong> {
// Rust #[inline]: optimization hint.
    override fun `error_scale`(): kotlin.UInt = 8u

// Rust #[inline]: optimization hint.
    override fun `error_halfscale`(): kotlin.UInt = (io.github.kotlinmania.numtraits.U32Num.checkedDiv(`__rtkImpl0`.`error_scale`(), 2u) ?: throw __rtkPanic("invalid integer division"))

// Rust #[inline]: optimization hint.
    override fun `error_is_accurate`(`count`: kotlin.UInt, `fp`: ExtendedFloat, `__rtkConst0`: `io`.`github`.`kotlinmania`.`serdejson`.`lexical`.`FloatTraits`): kotlin.Boolean {
        return kotlin.run {
            // Determine if extended-precision float is a good approximation.
            // If the error has affected too many units, the float will be
            // inaccurate, or if the representation is too close to halfway
            // that any operations could affect this halfway representation.
            // See the documentation for dtoa for more information.
            val `bias`: kotlin.Int = __rtkSubI32(0, (__rtkSubI32(`__rtkConst0`.`exponentBias`, `__rtkConst0`.`mantissaSize`)))
            val `denormal_exp`: kotlin.Int = __rtkSubI32(`bias`, 63)
            // This is always a valid u32, since (denormal_exp - fp.exp)
            // will always be positive and the significand size is {23, 52}.
            val `extrabits`: kotlin.Int = (if (((`fp`).`exp` <= `denormal_exp`)) __rtkSubI32(__rtkAdd(__rtkSubI32(64, `__rtkConst0`.`mantissaSize`), `denormal_exp`), (`fp`).`exp`) else (__rtkSubI32(63, `__rtkConst0`.`mantissaSize`)))
    
            // Our logic is as follows: we want to determine if the actual
            // mantissa and the errors during calculation differ significantly
            // from the rounding point. The rounding point for round-nearest
            // is the halfway point, IE, this when the truncated bits start
            // with b1000..., while the rounding point for the round-toward
            // is when the truncated bits are equal to 0.
            // To do so, we can check whether the rounding point +/- the error
            // are >/< the actual lower n bits.
            //
            // For whether we need to use signed or unsigned types for this
            // analysis, see this example, using u8 rather than u64 to simplify
            // things.
            //
            // # Comparisons
            //      cmp1 = (halfway - errors) < extra
            //      cmp1 = extra < (halfway + errors)
            //
            // # Large Extrabits, Low Errors
            //
            //      extrabits = 8
            //      halfway          =  0b10000000
            //      extra            =  0b10000010
            //      errors           =  0b00000100
            //      halfway - errors =  0b01111100
            //      halfway + errors =  0b10000100
            //
            //      Unsigned:
            //          halfway - errors = 124
            //          halfway + errors = 132
            //          extra            = 130
            //          cmp1             = true
            //          cmp2             = true
            //      Signed:
            //          halfway - errors = 124
            //          halfway + errors = -124
            //          extra            = -126
            //          cmp1             = false
            //          cmp2             = true
            //
            // # Conclusion
            //
            // Since errors will always be small, and since we want to detect
            // if the representation is accurate, we need to use an **unsigned**
            // type for comparisons.
    
            val `extrabits__binding0`: kotlin.ULong = (`extrabits`).toLong().toULong()
            val `errors`: kotlin.ULong = (`count`).toULong()
            kotlin.run { if ((`extrabits__binding0` > 65uL)) kotlin.run {
                // Underflow, we have a literal 0.
                return true
            } }
            `nearest_error_is_accurate`(`errors`, `fp`, `extrabits__binding0`)
        }
    }

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
