// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Helpers to convert and add digits from characters.

// Convert u8 to digit.
// Rust #[inline]: optimization hint.
internal fun `to_digit`(`c`: kotlin.UByte): __rtkOption<kotlin.UInt> = __rtkCharToDigit((__rtkChar((`c`).toInt())), 10u)


// Add digit to mantissa.
// Rust #[inline]: optimization hint.
internal fun `add_digit`(`value`: kotlin.ULong, `digit`: kotlin.UInt): __rtkOption<kotlin.ULong> = (when (val __rtkValue1: __rtkOption<kotlin.ULong> = kotlin.run { val __rtkValue0 = io.github.kotlinmania.numtraits.U64Num.checkedMul(`value`, 10uL); if (__rtkValue0 == null) __rtkOption.None else __rtkOption.Some(__rtkValue0) }) {
    __rtkOption.None -> __rtkOption.None
    is __rtkOption.Some -> kotlin.run {
        val `n`: kotlin.ULong = __rtkValue1.`value`
        kotlin.run { val __rtkValue2 = io.github.kotlinmania.numtraits.U64Num.checkedAdd(`n`, (`digit`).toULong()); if (__rtkValue2 == null) __rtkOption.None else __rtkOption.Some(__rtkValue2) }
    }
})

// Tagged Rust Option; owned payloads require a checked fresh-value context.
sealed class __rtkOption<out T> {
    data object None : __rtkOption<kotlin.Nothing>()
    data class Some<T>(val value: T) : __rtkOption<T>()
}
private fun <T> __rtkUnwrapOr(value: __rtkOption<T>, fallback: T): T = when (value) {
    __rtkOption.None -> fallback
    is __rtkOption.Some -> value.value
}

class __rtkPanic(message: String) : kotlin.RuntimeException(message)

data class __rtkChar(val codePoint: Int) {
    init { require(codePoint in 0..0x10ffff && codePoint !in 0xd800..0xdfff) }
    fun lenUtf8(): ULong = scalarString().encodeToByteArray(throwOnInvalidSequence = true).size.toULong()
    fun scalarString(): String = if (codePoint < 0x10000) codePoint.toChar().toString() else
        (0xd800 + ((codePoint - 0x10000) shr 10)).toChar().toString() +
            (0xdc00 + ((codePoint - 0x10000) and 1023)).toChar()
}

private fun __rtkCharToDigit(value: __rtkChar, radix: UInt): __rtkOption<UInt> {
    if (radix < 2u || radix > 36u) throw __rtkPanic("to_digit: invalid radix -- radix must be in the range 2 to 36 inclusive")
    val digit = if (value.codePoint < 128) value.codePoint.toChar().digitToIntOrNull(radix.toInt()) else null
    return if (digit == null) __rtkOption.None else __rtkOption.Some(digit.toUInt())
}
