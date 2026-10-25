// port-lint: source lexical/cached.rs
package io.github.kotlinmania.serdejson.lexical.generated.cached

// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Cached powers trait for extended-precision floats.
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

// POWERS

/// Precalculated powers that uses two-separate arrays for memory-efficiency.
internal class `ExtendedFloatArray` internal constructor(
    // Pre-calculated mantissa for the powers.
        internal val `mant`: kotlin.collections.List<kotlin.ULong>,
    // Pre-calculated binary exponents for the powers.
        internal val `exp`: kotlin.collections.List<kotlin.Int>
) { companion object }

/// Allow indexing of values without bounds checking
// Rust #[inline]: optimization hint.
internal fun `ExtendedFloatArray`.`get_extended_float`(`index`: kotlin.ULong): ExtendedFloat = kotlin.run {
    val `mant`: kotlin.ULong = __rtkSliceGet((this).`mant`, `index`)
    val `exp`: kotlin.Int = __rtkSliceGet((this).`exp`, `index`)
    kotlin.run {
        val __rtkValue0: kotlin.ULong = `mant`
        val __rtkValue1: kotlin.Int = `exp`
        `ExtendedFloat`(__rtkValue0, __rtkValue1)
    }
}

// Rust #[inline]: optimization hint.
internal fun `ExtendedFloatArray`.`len`(): kotlin.ULong = ((this).`mant`).size.toULong()


// MODERATE PATH POWERS

/// Precalculated powers of base N for the moderate path.
internal class `ModeratePathPowers` internal constructor(
    // Pre-calculated small powers.
        internal val `small`: `ExtendedFloatArray`,
    // Pre-calculated large powers.
        internal val `large`: `ExtendedFloatArray`,
    /// Pre-calculated small powers as 64-bit integers
    internal val `small_int`: kotlin.collections.List<kotlin.ULong>,
    // Step between large powers and number of small powers.
        internal val `step`: kotlin.Int,
    // Exponent bias for the large powers.
        internal val `bias`: kotlin.Int
) { companion object }

/// Allow indexing of values without bounds checking
// Rust #[inline]: optimization hint.
internal fun `ModeratePathPowers`.`get_small`(`index`: kotlin.ULong): ExtendedFloat = ((this).`small`).`get_extended_float`(`index`)

// Rust #[inline]: optimization hint.
internal fun `ModeratePathPowers`.`get_large`(`index`: kotlin.ULong): ExtendedFloat = ((this).`large`).`get_extended_float`(`index`)

// Rust #[inline]: optimization hint.
internal fun `ModeratePathPowers`.`get_small_int`(`index`: kotlin.ULong): kotlin.ULong = __rtkSliceGet((this).`small_int`, `index`)


// CACHED EXTENDED POWERS

/// Cached powers as a trait for a floating-point type.
internal interface `ModeratePathCache` {
    interface `__rtkStatic`<`__rtkSelf`> {
        /// Get cached powers.
                fun `get_powers`(): `ModeratePathPowers`
    }
}

internal object `__rtkImpl0` : `ModeratePathCache`.`__rtkStatic`<ExtendedFloat> {
// Rust #[inline]: optimization hint.
    override fun `get_powers`(): `ModeratePathPowers` = io.github.kotlinmania.serdejson.lexical.generatedCachedPowers()

}

class __rtkPanic(message: String) : kotlin.RuntimeException(message)

private fun <T> __rtkSliceGet(values: kotlin.collections.List<T>, index: kotlin.ULong): T {
    if (index >= values.size.toULong()) throw __rtkPanic("index out of bounds")
    return values[index.toInt()]
}
