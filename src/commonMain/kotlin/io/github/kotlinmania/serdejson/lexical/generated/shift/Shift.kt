package io.github.kotlinmania.serdejson.lexical.generated.shift

import io.github.kotlinmania.numtraits.checkedShr as __rtkcheckedShr
import io.github.kotlinmania.numtraits.checkedShl as __rtkcheckedShl
// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Bit-shift helpers.
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

// Shift extended-precision float right `shift` bytes.
// Rust #[inline]: optimization hint.
internal fun `shr`(`fp`: ExtendedFloat, `shift`: kotlin.Int): kotlin.Unit = kotlin.run {
    val `bits`: kotlin.ULong = (io.github.kotlinmania.numtraits.U64Num.checkedMul(8uL, 8uL) ?: throw __rtkPanic("attempt to multiply with overflow"))
    kotlin.run { if (true && !((((`shift`).toLong().toULong()) < `bits`))) throw __rtkPanic("" + "shr() overflow in shift right.") }
    kotlin.run { val __rtkValue0: kotlin.Int = `shift`; (`fp`).`mant` = kotlin.run { val __rtkValue1 = (`fp`).`mant`; val __rtkValue2 = __rtkValue0; if (__rtkValue2.toULong() > kotlin.UInt.MAX_VALUE.toULong()) throw __rtkPanic("attempt to shift with overflow"); __rtkValue1.__rtkcheckedShr(__rtkValue2.toUInt()) ?: throw __rtkPanic("attempt to shift with overflow") } }
    kotlin.run { val __rtkValue3: kotlin.Int = `shift`; (`fp`).`exp` = __rtkAdd((`fp`).`exp`, __rtkValue3) }
}


// Shift extended-precision float right `shift` bytes.
//
// Accepts when the shift is the same as the type size, and
// sets the value to 0.
// Rust #[inline]: optimization hint.
internal fun `overflowing_shr`(`fp`: ExtendedFloat, `shift`: kotlin.Int): kotlin.Unit = kotlin.run {
    val `bits`: kotlin.ULong = (io.github.kotlinmania.numtraits.U64Num.checkedMul(8uL, 8uL) ?: throw __rtkPanic("attempt to multiply with overflow"))
    kotlin.run { if (true && !((((`shift`).toLong().toULong()) <= `bits`))) throw __rtkPanic("" + "overflowing_shr() overflow in shift right.") }
    kotlin.run { val __rtkValue2: kotlin.ULong = (if (((`shift`).toLong().toULong() == `bits`)) 0uL else (kotlin.run { val __rtkValue0 = (`fp`).`mant`; val __rtkValue1 = `shift`; if (__rtkValue1.toULong() > kotlin.UInt.MAX_VALUE.toULong()) throw __rtkPanic("attempt to shift with overflow"); __rtkValue0.__rtkcheckedShr(__rtkValue1.toUInt()) ?: throw __rtkPanic("attempt to shift with overflow") })); (`fp`).`mant` = __rtkValue2 }
    kotlin.run { val __rtkValue3: kotlin.Int = `shift`; (`fp`).`exp` = __rtkAdd((`fp`).`exp`, __rtkValue3) }
}


// Shift extended-precision float left `shift` bytes.
// Rust #[inline]: optimization hint.
internal fun `shl`(`fp`: ExtendedFloat, `shift`: kotlin.Int): kotlin.Unit = kotlin.run {
    val `bits`: kotlin.ULong = (io.github.kotlinmania.numtraits.U64Num.checkedMul(8uL, 8uL) ?: throw __rtkPanic("attempt to multiply with overflow"))
    kotlin.run { if (true && !((((`shift`).toLong().toULong()) < `bits`))) throw __rtkPanic("" + "shl() overflow in shift left.") }
    kotlin.run { val __rtkValue0: kotlin.Int = `shift`; (`fp`).`mant` = kotlin.run { val __rtkValue1 = (`fp`).`mant`; val __rtkValue2 = __rtkValue0; if (__rtkValue2.toULong() > kotlin.UInt.MAX_VALUE.toULong()) throw __rtkPanic("attempt to shift with overflow"); __rtkValue1.__rtkcheckedShl(__rtkValue2.toUInt()) ?: throw __rtkPanic("attempt to shift with overflow") } }
    kotlin.run { val __rtkValue3: kotlin.Int = `shift`; (`fp`).`exp` = __rtkSubI32((`fp`).`exp`, __rtkValue3) }
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
