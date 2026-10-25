package io.github.kotlinmania.serdejson.lexical.generated.exponent

// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Utilities to calculate exponents.
/// Convert usize into i32 without overflow.
///
/// This is needed to ensure when adjusting the exponent relative to
/// the mantissa we do not overflow for comically-long exponents.
// Rust #[inline]: optimization hint.
private fun `into_i32`(`value`: kotlin.ULong): kotlin.Int = (if ((`value` > (kotlin.Int.MAX_VALUE).toLong().toULong())) kotlin.Int.MAX_VALUE else ((`value`).toInt()))


// EXPONENT CALCULATION

// Calculate the scientific notation exponent without overflow.
//
// For example, 0.1 would be -1, and 10 would be 1 in base 10.
// Rust #[inline]: optimization hint.
internal fun `scientific_exponent`(`exponent`: kotlin.Int, `integer_digits`: kotlin.ULong, `fraction_start`: kotlin.ULong): kotlin.Int = (if ((`integer_digits` == 0uL)) kotlin.run {
    val `fraction_start__binding0`: kotlin.Int = `into_i32`(`fraction_start`)
    io.github.kotlinmania.numtraits.I32Num.saturatingSub(io.github.kotlinmania.numtraits.I32Num.saturatingSub(`exponent`, `fraction_start__binding0`), 1)
} else (kotlin.run {
    val `integer_shift`: kotlin.Int = `into_i32`(__rtkSub(`integer_digits`, 1uL))
    io.github.kotlinmania.numtraits.I32Num.saturatingAdd(`exponent`, `integer_shift`)
}))


// Calculate the mantissa exponent without overflow.
//
// Remove the number of digits that contributed to the mantissa past
// the dot, and add the number of truncated digits from the mantissa,
// to calculate the scaling factor for the mantissa from a raw exponent.
// Rust #[inline]: optimization hint.
internal fun `mantissa_exponent`(`exponent`: kotlin.Int, `fraction_digits`: kotlin.ULong, `truncated`: kotlin.ULong): kotlin.Int = (if ((`fraction_digits` > `truncated`)) io.github.kotlinmania.numtraits.I32Num.saturatingSub(`exponent`, `into_i32`(__rtkSub(`fraction_digits`, `truncated`))) else (io.github.kotlinmania.numtraits.I32Num.saturatingAdd(`exponent`, `into_i32`(__rtkSub(`truncated`, `fraction_digits`)))))

class __rtkPanic(message: String) : kotlin.RuntimeException(message)
private fun __rtkSub(left: ULong, right: ULong): ULong {
    if (left < right) throw __rtkPanic("attempt to subtract with overflow")
    return left - right
}
