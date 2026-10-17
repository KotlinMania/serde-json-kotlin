// port-lint: source de.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.Expected
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor

// ─── ParserNumber ─────────────────────────────────────────────────────────────

/**
 * A number as parsed by the JSON deserializer's number-parsing machinery.
 */
internal sealed class DeParserNumber {
    class F64(
        val value: Double,
    ) : DeParserNumber()

    class U64(
        val value: ULong,
    ) : DeParserNumber()

    class I64(
        val value: Long,
    ) : DeParserNumber()

    fun <V> visit(visitor: Visitor<V>): SerdeResult<V> =
        when (this) {
            is F64 -> visitor.visitF64(value)
            is U64 -> visitor.visitU64(value)
            is I64 -> visitor.visitI64(value)
        }

    fun invalidType(exp: Expected): SerdeError =
        when (this) {
            is F64 -> SerdeError.invalidType(Unexpected.FloatValue(value), exp)
            is U64 -> SerdeError.invalidType(Unexpected.Unsigned(value), exp)
            is I64 -> SerdeError.invalidType(Unexpected.Signed(value), exp)
        }
}

// ─── JsonParser ───────────────────────────────────────────────────────────────
//
// A JSON parser that reads from a string. This is the core character-level scanner
// used by the deserializer.
//
// The parser is designed to be driven by [JsonDeserializer]: each deserialize
// method on the deserializer calls the parser to peek/consume bytes and dispatch
// to the appropriate visitor method.

/**
 * A JSON parser that reads from a string, tracking position for error reporting.
 *
 * Provides the low-level scanning primitives:
 * whitespace skipping, identifier parsing, number parsing, string parsing, etc.
 */
class JsonParser(
    private val input: String,
) {
    internal var pos: Int = 0
        private set
    private var line: Int = 1
    private var col: Int = 0

    /** Returns the current position as (line, column). */
    fun position(): Pair<Int, Int> = Pair(line, col)

    /** Returns the byte/char offset from the beginning. */
    fun byteOffset(): Int = pos

    // ── Basic byte operations ──

    /**
     * Returns the first non-whitespace character code without consuming it,
     * or -1 at EOF.
     */
    internal fun parseWhitespace(): Int {
        skipWhitespace()
        return if (pos < input.length) input[pos].code else -1
    }

    /** Peeks at the current character (after skipping whitespace). Returns -1 at EOF. */
    fun peek(): Int {
        skipWhitespace()
        return if (pos < input.length) input[pos].code else -1
    }

    /** Peeks at the current character without skipping whitespace. Returns -1 at EOF. */
    internal fun peekRaw(): Int =
        if (pos < input.length) input[pos].code else -1

    /** Peeks at the current character or returns 0 (NUL) at EOF. */
    internal fun peekOrNull(): Int = peekRaw().let { if (it < 0) 0 else it }

    /** Consumes whitespace. */
    private fun skipWhitespace() {
        while (pos < input.length) {
            when (input[pos]) {
                ' ', '\t', '\n', '\r' -> {
                    if (input[pos] == '\n') {
                        line++
                        col = 0
                    }
                    pos++
                    col++
                }
                else -> return
            }
        }
    }

    /** Returns the next non-whitespace character code, or -1 at EOF. */
    fun next(): Int {
        skipWhitespace()
        return nextChar()
    }

    /** Returns the next character code without skipping whitespace, or -1 at EOF. */
    internal fun nextChar(): Int =
        if (pos < input.length) {
            val c = input[pos].code
            pos++
            if (c == '\n'.code) {
                line++
                col = 0
            } else {
                col++
            }
            c
        } else {
            -1
        }

    /** Returns the next character or 0 (NUL) at EOF. */
    internal fun nextCharOrNull(): Int = nextChar().let { if (it < 0) 0 else it }

    /** Discards the previously peeked byte (advances one char without line tracking). */
    internal fun eatChar() {
        if (pos < input.length) {
            if (input[pos] == '\n') {
                line++
                col = 0
            }
            pos++
            col++
        }
    }

    // ── Error helpers ──

    /** Error caused by a byte from nextChar(). */
    internal fun error(code: ErrorCode): SerdeError =
        SerdeError.custom(JsonError.syntax(code, line, col).toString())

    /** Error caused by a byte from peek(). */
    internal fun peekError(code: ErrorCode): SerdeError =
        SerdeError.custom(JsonError.syntax(code, line, col).toString())

    // ── Identifier parsing ──

    /**
     * Parses an identifier (e.g., "ull" for null, "rue" for true, "alse" for false).
     * The first character (n/t/f) has already been consumed by the caller.
     */
    internal fun parseIdent(ident: String) {
        for (expected in ident) {
            when (val next = nextChar()) {
                -1 -> throw SerdeException(error(ErrorCode.EofWhileParsingValue))
                else -> if (next != expected.code) throw SerdeException(error(ErrorCode.ExpectedSomeIdent))
            }
        }
    }

    // ── Number parsing ──

    /**
     * Parses a signed number: handles the optional `-` sign then delegates to
     * [parseAnyNumber]. Returns a [DeParserNumber].
     */
    internal fun parseAnySignedNumber(): DeParserNumber {
        val peek = peekRaw()
        if (peek < 0) throw SerdeException(peekError(ErrorCode.EofWhileParsingValue))
        val value =
            when (peek.toChar()) {
                '-' -> {
                    eatChar()
                    parseAnyNumber(false)
                }
                in '0'..'9' -> parseAnyNumber(true)
                else -> throw SerdeException(peekError(ErrorCode.InvalidNumber))
            }
        // After a complete number, the next char must not be a digit or
        // other number continuation — otherwise it's invalid.
        val after = peekRaw()
        if (after >= 0) throw SerdeException(peekError(ErrorCode.InvalidNumber))
        return value
    }

    /** Parses an unsigned number after the sign has been handled. */
    internal fun parseAnyNumber(positive: Boolean): DeParserNumber =
        parseInteger(positive)

    /**
     * Parses the integer part of a JSON number, then dispatches to
     * [parseNumber] for fractional/exponent parts.
     *
     * `positive` is false when a `-` was consumed.
     */
    internal fun parseInteger(positive: Boolean): DeParserNumber {
        val next = nextCharOrNull()
        return when (next.toChar()) {
            '0' -> {
                // Only one leading zero allowed.
                if (peekOrNull().toChar() in '0'..'9') {
                    throw SerdeException(peekError(ErrorCode.InvalidNumber))
                }
                parseNumber(positive, 0uL)
            }
            in '1'..'9' -> {
                var significand = (next - '0'.code).toULong()
                while (peekOrNull().toChar() in '0'..'9') {
                    val digit = (peekOrNull() - '0'.code).toULong()
                    // Overflow check: if significand * 10 + digit > ULong.MAX_VALUE
                    if (overflow(significand, digit, ULong.MAX_VALUE)) {
                        return DeParserNumber.F64(parseLongInteger(positive, significand))
                    }
                    eatChar()
                    significand = significand * 10uL + digit
                }
                parseNumber(positive, significand)
            }
            else -> throw SerdeException(error(ErrorCode.InvalidNumber))
        }
    }

    /**
     * Checks if `a * 10 + b` would overflow `max`.
     */
    private fun overflow(a: ULong, b: ULong, max: ULong): Boolean =
        a >= max / 10uL && (a > max / 10uL || b > max % 10uL)

    private fun overflow(a: Int, b: Int, max: Int): Boolean =
        a >= max / 10 && (a > max / 10 || b > max % 10)

    /**
     * After parsing the integer part, checks for a decimal point or exponent.
     * If neither is present, returns the integer as U64 or I64 (or F64 for -0).
     */
    private fun parseNumber(positive: Boolean, significand: ULong): DeParserNumber =
        when (peekOrNull().toChar()) {
            '.' -> DeParserNumber.F64(parseDecimal(positive, significand, 0))
            'e', 'E' -> DeParserNumber.F64(parseExponent(positive, significand, 0))
            else -> {
                if (positive) {
                    DeParserNumber.U64(significand)
                } else {
                    val neg = (significand.toLong()).inv() + 1 // wrapping neg
                    // Convert to float on underflow or -0
                    if (neg >= 0) {
                        DeParserNumber.F64(-(significand.toDouble()))
                    } else {
                        DeParserNumber.I64(neg)
                    }
                }
            }
        }

    /** Parses the fractional part after a `.` */
    private fun parseDecimal(
        positive: Boolean,
        significand: ULong,
        exponentBefore: Int,
    ): Double {
        eatChar() // consume '.'
        var sig = significand
        var expAfter = 0
        while (peekOrNull().toChar() in '0'..'9') {
            val digit = (peekOrNull() - '0'.code).toULong()
            if (overflow(sig, digit, ULong.MAX_VALUE)) {
                val exp = exponentBefore + expAfter
                return parseDecimalOverflow(positive, sig, exp)
            }
            eatChar()
            sig = sig * 10uL + digit
            expAfter--
        }
        if (expAfter == 0) {
            // No digits after decimal point
            if (peekRaw() >= 0) throw SerdeException(peekError(ErrorCode.InvalidNumber))
            throw SerdeException(peekError(ErrorCode.EofWhileParsingValue))
        }
        val exp = exponentBefore + expAfter
        return when (peekOrNull().toChar()) {
            'e', 'E' -> parseExponent(positive, sig, exp)
            else -> f64FromParts(positive, sig, exp)
        }
    }

    /** Parses the exponent part after `e` or `E`. */
    private fun parseExponent(
        positive: Boolean,
        significand: ULong,
        startingExp: Int,
    ): Double {
        eatChar() // consume exponent marker
        val positiveExp =
            when (peekOrNull().toChar()) {
                '+' -> {
                    eatChar()
                    true
                }
                '-' -> {
                    eatChar()
                    false
                }
                else -> true
            }
        val next = nextCharOrNull()
        if (next.toChar() !in '0'..'9') throw SerdeException(error(ErrorCode.InvalidNumber))
        var exp = (next - '0'.code)
        while (peekOrNull().toChar() in '0'..'9') {
            val digit = (peekOrNull() - '0'.code)
            if (overflow(exp, digit, Int.MAX_VALUE)) {
                val zeroSig = significand == 0uL
                return parseExponentOverflow(positive, zeroSig, positiveExp)
            }
            eatChar()
            exp = exp * 10 + digit
        }
        val finalExp =
            if (positiveExp) {
                startingExp.saturatingAdd(exp)
            } else {
                startingExp.saturatingSub(exp)
            }
        return f64FromParts(positive, significand, finalExp)
    }

    private fun Int.saturatingAdd(other: Int): Int {
        val res = this.toLong() + other.toLong()
        return when {
            res > Int.MAX_VALUE -> Int.MAX_VALUE
            res < Int.MIN_VALUE -> Int.MIN_VALUE
            else -> res.toInt()
        }
    }

    private fun Int.saturatingSub(other: Int): Int {
        val res = this.toLong() - other.toLong()
        return when {
            res > Int.MAX_VALUE -> Int.MAX_VALUE
            res < Int.MIN_VALUE -> Int.MIN_VALUE
            else -> res.toInt()
        }
    }

    /** Handles overflow in the integer part by switching to f64 parsing without losing precision. */
    private fun parseLongInteger(positive: Boolean, significand: ULong): Double {
        val sb = StringBuilder()
        sb.append(significand)
        while (true) {
            when (val ch = peekOrNull().toChar()) {
                in '0'..'9' -> {
                    eatChar()
                    sb.append(ch)
                }
                '.' -> {
                    eatChar()
                    sb.append('.')
                    var hasDigits = false
                    while (peekOrNull().toChar() in '0'..'9') {
                        val digit = peekOrNull().toChar()
                        eatChar()
                        sb.append(digit)
                        hasDigits = true
                    }
                    if (!hasDigits) {
                        if (peekRaw() >= 0) throw SerdeException(peekError(ErrorCode.InvalidNumber))
                        throw SerdeException(peekError(ErrorCode.EofWhileParsingValue))
                    }
                }
                'e', 'E' -> {
                    eatChar()
                    sb.append('e')
                    val expSign = peekOrNull().toChar()
                    if (expSign == '+' || expSign == '-') {
                        eatChar()
                        sb.append(expSign)
                    }
                    val next = nextCharOrNull()
                    if (next.toChar() !in '0'..'9') throw SerdeException(error(ErrorCode.InvalidNumber))
                    sb.append(next.toChar())
                    while (peekOrNull().toChar() in '0'..'9') {
                        val digit = peekOrNull().toChar()
                        eatChar()
                        sb.append(digit)
                    }
                    val d =
                        sb.toString().toDoubleOrNull()
                            ?: throw SerdeException(error(ErrorCode.NumberOutOfRange))
                    if (d.isInfinite()) throw SerdeException(error(ErrorCode.NumberOutOfRange))
                    return if (positive) d else -d
                }
                else -> {
                    val d =
                        sb.toString().toDoubleOrNull()
                            ?: throw SerdeException(error(ErrorCode.NumberOutOfRange))
                    if (d.isInfinite()) throw SerdeException(error(ErrorCode.NumberOutOfRange))
                    return if (positive) d else -d
                }
            }
        }
    }

    /** Handles overflow in the decimal part — discards remaining digits. */
    private fun parseDecimalOverflow(
        positive: Boolean,
        significand: ULong,
        exponent: Int,
    ): Double {
        while (peekOrNull().toChar() in '0'..'9') eatChar()
        return when (peekOrNull().toChar()) {
            'e', 'E' -> parseExponent(positive, significand, exponent)
            else -> f64FromParts(positive, significand, exponent)
        }
    }

    /** Handles overflow in the exponent part. */
    private fun parseExponentOverflow(
        positive: Boolean,
        zeroSig: Boolean,
        positiveExp: Boolean,
    ): Double {
        if (!zeroSig && positiveExp) throw SerdeException(error(ErrorCode.NumberOutOfRange))
        while (peekOrNull().toChar() in '0'..'9') eatChar()
        return if (positive) 0.0 else -0.0
    }

    /**
     * Constructs a Double from a significand and exponent using the POW10 table.
     */
    private fun f64FromParts(positive: Boolean, significand: ULong, exponent: Int): Double {
        var f = significand.toDouble()
        var exp = exponent
        loop@ while (true) {
            val absExp = if (exp == Int.MIN_VALUE) Int.MAX_VALUE else kotlin.math.abs(exp)
            val pow = POW10.get(absExp)
            if (pow != null) {
                if (exp >= 0) {
                    f *= pow
                    if (f.isInfinite()) throw SerdeException(error(ErrorCode.NumberOutOfRange))
                } else {
                    f /= pow
                }
                break@loop
            } else {
                if (f == 0.0) break@loop
                if (exp >= 0) throw SerdeException(error(ErrorCode.NumberOutOfRange))
                f /= 1e308
                exp += 308
            }
        }
        return if (positive) f else -f
    }

    // ── 128-bit integer scanning ──

    /**
     * Scans an integer into a string buffer for 128-bit parsing.
     * The sign has already been handled by the caller.
     */
    internal fun scanInteger128(buf: StringBuilder) {
        val next = nextCharOrNull()
        when (next.toChar()) {
            '0' -> {
                buf.append('0')
                if (peekOrNull().toChar() in '0'..'9') {
                    throw SerdeException(peekError(ErrorCode.InvalidNumber))
                }
            }
            in '1'..'9' -> {
                buf.append(next.toChar())
                while (peekOrNull().toChar() in '0'..'9') {
                    buf.append(peekOrNull().toChar())
                    eatChar()
                }
            }
            else -> throw SerdeException(error(ErrorCode.InvalidNumber))
        }
    }

    private fun StringBuilder.appendCodePoint(codePoint: Int) {
        if (codePoint < 0x10000) {
            append(codePoint.toChar())
        } else {
            val cpPrime = codePoint - 0x10000
            append(((cpPrime shr 10) + 0xD800).toChar())
            append(((cpPrime and 0x3FF) + 0xDC00).toChar())
        }
    }

    // ── String parsing ──

    /**
     * Parses a JSON string. The opening `"` must have already been consumed.
     * Returns the decoded string.
     */
    internal fun parseStr(): String {
        val sb = StringBuilder()
        while (pos < input.length) {
            val c = input[pos]
            pos++
            if (c == '\n') {
                line++
                col = 0
            } else {
                col++
            }
            when {
                c == '"' -> return sb.toString()
                c == '\\' -> {
                    if (pos >= input.length) throw SerdeException(error(ErrorCode.EofWhileParsingString))
                    val esc = input[pos]
                    pos++
                    col++
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            val cp = decodeHexEscape()
                            // Handle surrogate pairs
                            if (cp in 0xD800..0xDBFF) {
                                // High surrogate — look for low surrogate
                                if (pos + 1 < input.length && input[pos] == '\\' && input[pos + 1] == 'u') {
                                    pos += 2
                                    col += 2
                                    val low = decodeHexEscape()
                                    if (low in 0xDC00..0xDFFF) {
                                        val full = 0x10000 + ((cp - 0xD800) shl 10) + (low - 0xDC00)
                                        sb.appendCodePoint(full)
                                    } else {
                                        // Invalid low surrogate — emit replacement
                                        sb.appendCodePoint(cp)
                                        sb.appendCodePoint(low)
                                    }
                                } else {
                                    // Lone high surrogate
                                    sb.appendCodePoint(cp)
                                }
                            } else if (cp in 0xDC00..0xDFFF) {
                                // Lone low surrogate
                                sb.appendCodePoint(cp)
                            } else {
                                sb.appendCodePoint(cp)
                            }
                        }
                        else -> throw SerdeException(error(ErrorCode.InvalidEscape))
                    }
                }
                c.code < 0x20 -> throw SerdeException(error(ErrorCode.ControlCharacterWhileParsingString))
                else -> sb.append(c)
            }
        }
        throw SerdeException(error(ErrorCode.EofWhileParsingString))
    }

    /** Parses a 4-hex-digit unicode escape. Assumes `\\u` has been consumed. */
    private fun decodeHexEscape(): Int {
        if (pos + 4 > input.length) throw SerdeException(error(ErrorCode.UnexpectedEndOfHexEscape))
        val hex = input.substring(pos, pos + 4)
        pos += 4
        col += 4
        val cp =
            hex.toIntOrNull(16)
                ?: throw SerdeException(error(ErrorCode.InvalidUnicodeCodePoint))
        return cp
    }

    /** Parses a JSON string as raw bytes (for deserializeBytes). */
    internal fun parseStrRaw(): ByteArray = parseStr().encodeToByteArray()

    /** Ignores a JSON string (skips past it). The opening `"` must be consumed. */
    internal fun ignoreStr() {
        while (pos < input.length) {
            val c = input[pos]
            pos++
            if (c == '\n') {
                line++
                col = 0
            } else {
                col++
            }
            when {
                c == '"' -> return
                c == '\\' -> {
                    if (pos >= input.length) throw SerdeException(error(ErrorCode.EofWhileParsingString))
                    if (input[pos] == 'u') {
                        pos++
                        col++
                        decodeHexEscape()
                    } else {
                        pos++
                        col++
                    }
                }
            }
        }
        throw SerdeException(error(ErrorCode.EofWhileParsingString))
    }

    // ── Value parsing (used by fromStr for Value) ──

    /** Parses a JSON value into a [Value]. */
    fun parseValue(): SerdeResult<Value> =
        serdeCatching {
            val peek = parseWhitespace()
            if (peek < 0) throw SerdeException(error(ErrorCode.EofWhileParsingValue))
            when (peek.toChar()) {
                'n' -> {
                    eatChar()
                    parseIdent("ull")
                    Value.Null
                }
                't' -> {
                    eatChar()
                    parseIdent("rue")
                    Value.Bool(true)
                }
                'f' -> {
                    eatChar()
                    parseIdent("alse")
                    Value.Bool(false)
                }
                '"' -> {
                    eatChar()
                    Value.Str(parseStr())
                }
                '[' -> parseArrayValue()
                '{' -> parseObjectValue()
                '-' -> {
                    eatChar()
                    val n = parseAnyNumber(false)
                    numberToValue(n)
                }
                in '0'..'9' -> {
                    val n = parseAnyNumber(true)
                    numberToValue(n)
                }
                else -> throw SerdeException(peekError(ErrorCode.ExpectedSomeValue))
            }
        }

    private fun numberToValue(n: DeParserNumber): Value =
        when (n) {
            is DeParserNumber.F64 -> Value.Number(JsonNumber.fromF64(n.value)!!)
            is DeParserNumber.U64 -> Value.Number(JsonNumber.fromU64(n.value))
            is DeParserNumber.I64 -> Value.Number(JsonNumber.fromI64(n.value))
        }

    private fun parseArrayValue(): Value {
        eatChar() // consume '['
        val list = mutableListOf<Value>()
        if (parseWhitespace().toChar() == ']') {
            eatChar()
            return Value.Array(list)
        }
        while (true) {
            list.add(parseValue().getOrThrow())
            when (parseWhitespace().toChar()) {
                ',' -> {
                    eatChar()
                }
                ']' -> {
                    eatChar()
                    return Value.Array(list)
                }
                else -> {
                    if (pos >= input.length) throw SerdeException(peekError(ErrorCode.EofWhileParsingList))
                    throw SerdeException(peekError(ErrorCode.ExpectedListCommaOrEnd))
                }
            }
        }
    }

    private fun parseObjectValue(): Value {
        eatChar() // consume '{'
        val map = ValueMap()
        if (parseWhitespace().toChar() == '}') {
            eatChar()
            return Value.Object(map)
        }
        while (true) {
            if (parseWhitespace().toChar() != '"') {
                if (pos >= input.length) throw SerdeException(peekError(ErrorCode.EofWhileParsingObject))
                throw SerdeException(peekError(ErrorCode.KeyMustBeAString))
            }
            eatChar() // consume '"'
            val key = parseStr()
            if (parseWhitespace().toChar() != ':') {
                if (pos >= input.length) throw SerdeException(peekError(ErrorCode.EofWhileParsingObject))
                throw SerdeException(peekError(ErrorCode.ExpectedColon))
            }
            eatChar() // consume ':'
            val value = parseValue().getOrThrow()
            map.insert(key, value)
            when (parseWhitespace().toChar()) {
                ',' -> {
                    eatChar()
                }
                '}' -> {
                    eatChar()
                    return Value.Object(map)
                }
                else -> {
                    if (pos >= input.length) throw SerdeException(peekError(ErrorCode.EofWhileParsingObject))
                    throw SerdeException(peekError(ErrorCode.ExpectedObjectCommaOrEnd))
                }
            }
        }
    }

    // ── Ignore value ──

    /**
     * Skips over a JSON value without parsing it into any type.
     * Used by `deserializeIgnoredAny`.
     */
    internal fun ignoreValue() {
        val stack = ArrayDeque<Char>()
        while (true) {
            val peek = parseWhitespace()
            if (peek < 0) throw SerdeException(peekError(ErrorCode.EofWhileParsingValue))
            val frame: Char? =
                when (peek.toChar()) {
                    'n' -> {
                        eatChar()
                        parseIdent("ull")
                        null
                    }
                    't' -> {
                        eatChar()
                        parseIdent("rue")
                        null
                    }
                    'f' -> {
                        eatChar()
                        parseIdent("alse")
                        null
                    }
                    '-' -> {
                        eatChar()
                        ignoreInteger()
                        null
                    }
                    in '0'..'9' -> {
                        ignoreInteger()
                        null
                    }
                    '"' -> {
                        eatChar()
                        ignoreStr()
                        null
                    }
                    '[', '{' -> {
                        eatChar()
                        peek.toChar()
                    }
                    else -> throw SerdeException(peekError(ErrorCode.ExpectedSomeValue))
                }

            var acceptComma: Boolean
            var currentFrame: Char
            when {
                frame != null -> {
                    acceptComma = false
                    currentFrame = frame
                }
                stack.isNotEmpty() -> {
                    acceptComma = true
                    currentFrame = stack.removeLast()
                }
                else -> return
            }

            while (true) {
                val p = parseWhitespace()
                if (p < 0) {
                    throw SerdeException(
                        peekError(
                            if (currentFrame == '[') ErrorCode.EofWhileParsingList else ErrorCode.EofWhileParsingObject,
                        ),
                    )
                }
                when (p.toChar()) {
                    ',' -> {
                        if (acceptComma) {
                            eatChar()
                            break
                        }
                    }
                    ']' -> {
                        if (currentFrame == '[') {
                            eatChar()
                            if (stack.isEmpty()) return
                            currentFrame = stack.removeLast()
                            acceptComma = true
                        }
                    }
                    '}' -> {
                        if (currentFrame == '{') {
                            eatChar()
                            if (stack.isEmpty()) return
                            currentFrame = stack.removeLast()
                            acceptComma = true
                        }
                    }
                    else -> {
                        if (acceptComma) {
                            throw SerdeException(
                                peekError(
                                    if (currentFrame == '[') ErrorCode.ExpectedListCommaOrEnd else ErrorCode.ExpectedObjectCommaOrEnd,
                                ),
                            )
                        } else {
                            break
                        }
                    }
                }
            }

            // After comma in an object, expect a key
            if (currentFrame == '{') {
                val p = parseWhitespace()
                if (p < 0) throw SerdeException(peekError(ErrorCode.EofWhileParsingObject))
                if (p.toChar() == '"') {
                    eatChar()
                    ignoreStr()
                    val cp = parseWhitespace()
                    if (cp < 0) throw SerdeException(peekError(ErrorCode.EofWhileParsingObject))
                    if (cp.toChar() != ':') throw SerdeException(peekError(ErrorCode.ExpectedColon))
                    eatChar()
                } else {
                    throw SerdeException(peekError(ErrorCode.KeyMustBeAString))
                }
            }
            stack.addLast(currentFrame)
        }
    }

    /** Ignores an integer (and optional decimal/exponent). */
    private fun ignoreInteger() {
        val next = nextCharOrNull()
        when (next.toChar()) {
            '0' -> {
                if (peekOrNull().toChar() in '0'..'9') {
                    throw SerdeException(peekError(ErrorCode.InvalidNumber))
                }
            }
            in '1'..'9' -> {
                while (peekOrNull().toChar() in '0'..'9') eatChar()
            }
            else -> throw SerdeException(error(ErrorCode.InvalidNumber))
        }
        when (peekOrNull().toChar()) {
            '.' -> ignoreDecimal()
            'e', 'E' -> ignoreExponent()
            else -> {}
        }
    }

    private fun ignoreDecimal() {
        eatChar()
        var atLeastOne = false
        while (peekOrNull().toChar() in '0'..'9') {
            eatChar()
            atLeastOne = true
        }
        if (!atLeastOne) throw SerdeException(peekError(ErrorCode.InvalidNumber))
        when (peekOrNull().toChar()) {
            'e', 'E' -> ignoreExponent()
            else -> {}
        }
    }

    private fun ignoreExponent() {
        eatChar()
        when (peekOrNull().toChar()) {
            '+', '-' -> eatChar()
            else -> {}
        }
        val next = nextCharOrNull()
        if (next.toChar() !in '0'..'9') throw SerdeException(error(ErrorCode.InvalidNumber))
        while (peekOrNull().toChar() in '0'..'9') eatChar()
    }

    // ── Trailing characters check ──

    /**
     * Verifies that the input stream is at the end, or only has trailing whitespace.
     * Called after a value has been fully deserialized.
     */
    internal fun end(): SerdeResult<Unit> {
        val peek = parseWhitespace()
        return if (peek >= 0) {
            SerdeResult.failure(peekError(ErrorCode.TrailingCharacters))
        } else {
            SerdeResult.success(Unit)
        }
    }

    // ── Object colon ──

    /** Parses the `:` between a map key and value. */
    internal fun parseObjectColon() {
        when (parseWhitespace()) {
            ':'.code -> {
                eatChar()
            }
            -1 -> throw SerdeException(peekError(ErrorCode.EofWhileParsingObject))
            else -> throw SerdeException(peekError(ErrorCode.ExpectedColon))
        }
    }

    // ── End of sequence/map ──

    /** Checks for `]` or `,` `]` at end of a sequence. */
    internal fun endSeq() {
        when (parseWhitespace()) {
            ']'.code -> {
                eatChar()
            }
            ','.code -> {
                eatChar()
                val p2 = parseWhitespace()
                if (p2 == ']'.code) throw SerdeException(peekError(ErrorCode.TrailingComma))
                throw SerdeException(peekError(ErrorCode.TrailingCharacters))
            }
            -1 -> throw SerdeException(peekError(ErrorCode.EofWhileParsingList))
            else -> throw SerdeException(peekError(ErrorCode.TrailingCharacters))
        }
    }

    /** Checks for `}` or `,` `}` at end of a map. */
    internal fun endMap() {
        when (parseWhitespace()) {
            '}'.code -> {
                eatChar()
            }
            ','.code -> throw SerdeException(peekError(ErrorCode.TrailingComma))
            -1 -> throw SerdeException(peekError(ErrorCode.EofWhileParsingObject))
            else -> throw SerdeException(peekError(ErrorCode.TrailingCharacters))
        }
    }

    /** Returns true if there is more input to consume. */
    fun hasMore(): Boolean {
        skipWhitespace()
        return pos < input.length
    }

    // ── Raw value buffering ──

    /**
     * Records the current position, ignores a value, then returns the raw
     * substring spanning that value. Used by `deserializeRawValue`.
     */
    internal fun parseRawValue(): String {
        skipWhitespace()
        val start = pos
        ignoreValue()
        return input.substring(start, pos)
    }
}

// ─── POW10 table ─────────────────────────────────────────────────────────────

/**
 * Table of powers of 10 for fast Double construction.
 */
private val POW10: kotlin.collections.Map<Int, Double> =
    buildMap {
        var v = 1.0
        for (i in 0..308) {
            put(i, v)
            v *= 10.0
        }
    }

// ─── JsonDeserializer ─────────────────────────────────────────────────────────
//
// The serde [Deserializer] implementation backed by [JsonParser].
// Each `deserialize_*` method peeks at the next byte, validates it matches
// the expected type, consumes it, and dispatches to the visitor.

/**
 * A JSON deserializer that wraps a [JsonParser] and implements the serde
 * [Deserializer] interface.
 *
 * This is the primary entry point for deserializing JSON into typed values
 * via the serde framework. For direct parsing into [Value], use [fromStr]
 * or [JsonParser.parseValue] instead.
 */
class JsonDeserializer(
    internal val parser: JsonParser,
) : Deserializer {
    /** Recursion depth limit, decremented on each nested container. */
    internal var remainingDepth: Int = 128
        private set

    /** Decrements recursion depth; throws if limit exceeded. */
    private inline fun <T> checkRecursion(block: () -> T): T {
        remainingDepth--
        if (remainingDepth == 0) {
            throw SerdeException(parser.peekError(ErrorCode.RecursionLimitExceeded))
        }
        try {
            return block()
        } finally {
            remainingDepth++
        }
    }

    override fun isHumanReadable(): Boolean = true

    // ── deserializeAny ──

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            when (peek.toChar()) {
                'n' -> {
                    parser.eatChar()
                    parser.parseIdent("ull")
                    visitor.visitUnit()
                }
                't' -> {
                    parser.eatChar()
                    parser.parseIdent("rue")
                    visitor.visitBool(true)
                }
                'f' -> {
                    parser.eatChar()
                    parser.parseIdent("alse")
                    visitor.visitBool(false)
                }
                '-' -> {
                    parser.eatChar()
                    parser.parseAnyNumber(false).visit(visitor)
                }
                in '0'..'9' -> {
                    parser.parseAnyNumber(true).visit(visitor)
                }
                '"' -> {
                    parser.eatChar()
                    visitor.visitStr(parser.parseStr())
                }
                '[' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitSeq(JsonSeqAccess(this))
                        parser.endSeq()
                        ret
                    }
                }
                '{' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitMap(JsonMapAccess(this))
                        parser.endMap()
                        ret
                    }
                }
                else -> throw SerdeException(parser.peekError(ErrorCode.ExpectedSomeValue))
            }
        }.flatMap { it }

    // ── deserializeBool ──

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            when (peek.toChar()) {
                't' -> {
                    parser.eatChar()
                    parser.parseIdent("rue")
                    true
                }
                'f' -> {
                    parser.eatChar()
                    parser.parseIdent("alse")
                    false
                }
                else -> throw SerdeException(peekInvalidType(visitor))
            }
        }.flatMap { visitor.visitBool(it) }

    // ── deserialize numbers ──

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            if (peek == '-'.code) {
                parser.eatChar()
                val buf = StringBuilder("-")
                parser.scanInteger128(buf)
                buf.toString()
            } else {
                val buf = StringBuilder()
                parser.scanInteger128(buf)
                buf.toString()
            }
        }.flatMap { str -> visitor.visitI128(str) }

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            if (peek == '-'.code) throw SerdeException(parser.peekError(ErrorCode.NumberOutOfRange))
            val buf = StringBuilder()
            parser.scanInteger128(buf)
            buf.toString()
        }.flatMap { str -> visitor.visitU128(str) }

    /**
     * Parses a JSON number and dispatches to the visitor's appropriate
     * visit method.
     */
    private fun <V> deserializeNumber(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            when (peek.toChar()) {
                '-' -> {
                    parser.eatChar()
                    parser.parseInteger(false).visit(visitor)
                }
                in '0'..'9' -> {
                    parser.parseInteger(true).visit(visitor)
                }
                else -> throw SerdeException(peekInvalidType(visitor))
            }
        }.flatMap { it }

    // ── deserializeChar ──

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    // ── deserializeStr / string ──

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            if (peek != '"'.code) throw SerdeException(peekInvalidType(visitor))
            parser.eatChar()
            parser.parseStr()
        }.flatMap { visitor.visitStr(it) }

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    // ── deserializeBytes / byteBuf ──

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> {
        val peek = parser.parseWhitespace()
        if (peek < 0) return SerdeResult.failure(parser.peekError(ErrorCode.EofWhileParsingValue))
        return when (peek.toChar()) {
            '"' -> {
                serdeCatching {
                    parser.eatChar()
                    parser.parseStrRaw()
                }.flatMap { visitor.visitBytes(it) }
            }
            '[' -> deserializeSeq(visitor)
            else -> SerdeResult.failure(peekInvalidType(visitor))
        }
    }

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeBytes(visitor)

    // ── deserializeOption ──

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek == 'n'.code) {
                parser.eatChar()
                parser.parseIdent("ull")
            } else {
                null // non-null — will visitSome
            }
        }.flatMap { isNull ->
            if (isNull == null) visitor.visitSome(this) else visitor.visitNone()
        }

    // ── deserializeUnit ──

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            if (peek != 'n'.code) throw SerdeException(peekInvalidType(visitor))
            parser.eatChar()
            parser.parseIdent("ull")
        }.flatMap { visitor.visitUnit() }

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        deserializeUnit(visitor)

    // ── deserializeNewtypeStruct ──

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> {
        if (name == RawValue.TOKEN) {
            return deserializeRawValue(visitor)
        }
        return visitor.visitNewtypeStruct(this)
    }

    // ── deserializeSeq ──

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            if (peek != '['.code) throw SerdeException(peekInvalidType(visitor))
            checkRecursion {
                parser.eatChar()
                val ret = visitor.visitSeq(JsonSeqAccess(this))
                parser.endSeq()
                ret
            }
        }.flatMap { it }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    // ── deserializeMap ──

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            if (peek != '{'.code) throw SerdeException(peekInvalidType(visitor))
            checkRecursion {
                parser.eatChar()
                val ret = visitor.visitMap(JsonMapAccess(this))
                parser.endMap()
                ret
            }
        }.flatMap { it }

    // ── deserializeStruct ──

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            when (peek.toChar()) {
                '[' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitSeq(JsonSeqAccess(this))
                        parser.endSeq()
                        ret
                    }
                }
                '{' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitMap(JsonMapAccess(this))
                        parser.endMap()
                        ret
                    }
                }
                else -> throw SerdeException(peekInvalidType(visitor))
            }
        }.flatMap { it }

    // ── deserializeEnum ──

    /**
     * Parses an enum as either `{"$KEY":$VALUE}` or a bare `"variantName"`.
     *
     * For the object form, the deserializer creates a [JsonVariantAccess]
     * that provides access to the variant's content.
     * For the bare string form (unit variant), a [JsonUnitVariantAccess] is used.
     */
    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            when (peek.toChar()) {
                '{' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitEnum(JsonVariantAccess(this))
                        // After the variant, expect '}'
                        val p = parser.parseWhitespace()
                        if (p < 0) throw SerdeException(parser.error(ErrorCode.EofWhileParsingObject))
                        if (p != '}'.code) throw SerdeException(parser.error(ErrorCode.ExpectedSomeValue))
                        parser.eatChar()
                        ret
                    }
                }
                '"' -> {
                    visitor.visitEnum(JsonUnitVariantAccess(this))
                }
                else -> throw SerdeException(parser.peekError(ErrorCode.ExpectedSomeValue))
            }
        }.flatMap { it }

    // ── deserializeIdentifier ──

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    // ── deserializeIgnoredAny ──

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            parser.ignoreValue()
        }.flatMap { visitor.visitUnit() }

    // ── peekInvalidType ──

    /**
     * Peeks at the current value and produces an invalidType error
     * describing what was found vs. what was expected.
     */
    private fun <V> peekInvalidType(visitor: Visitor<V>): SerdeError {
        val peek = parser.peekOrNull()
        return when (peek.toChar()) {
            'n' -> {
                parser.eatChar()
                parser.parseIdent("ull")
                SerdeError.invalidType(Unexpected.UnitValue, visitor)
            }
            't' -> {
                parser.eatChar()
                parser.parseIdent("rue")
                SerdeError.invalidType(Unexpected.Bool(true), visitor)
            }
            'f' -> {
                parser.eatChar()
                parser.parseIdent("alse")
                SerdeError.invalidType(Unexpected.Bool(false), visitor)
            }
            '-' -> {
                parser.eatChar()
                try {
                    parser.parseAnyNumber(false).invalidType(visitor)
                } catch (e: SerdeException) {
                    e.error
                }
            }
            in '0'..'9' -> {
                try {
                    parser.parseAnyNumber(true).invalidType(visitor)
                } catch (e: SerdeException) {
                    e.error
                }
            }
            '"' -> {
                parser.eatChar()
                val s = parser.parseStr()
                SerdeError.invalidType(Unexpected.Str(s), visitor)
            }
            '[' -> SerdeError.invalidType(Unexpected.Seq, visitor)
            '{' -> SerdeError.invalidType(Unexpected.Map, visitor)
            else -> parser.peekError(ErrorCode.ExpectedSomeValue)
        }
    }

    // ── Raw value deserialization ──

    /**
     * Deserializes a raw JSON value — the raw text of any valid JSON value
     * is captured and passed to the visitor as a string.
     */
    internal fun <V> deserializeRawValue(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            parser.parseRawValue()
        }.flatMap { raw ->
            visitor.visitStr(raw)
        }
}

// ─── SeqAccess ─────────────────────────────────────────────────────────────────

/**
 * Sequence access for JSON arrays.
 *
 * Created by [JsonDeserializer.deserializeSeq] and passed to the visitor's
 * `visitSeq`. Each call to `nextElementSeed` checks for `]` (end) or `,`
 * (separator) and deserializes the next element.
 */
internal class JsonSeqAccess(
    private val de: JsonDeserializer,
) : SeqAccess {
    private var first = true

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?> =
        serdeCatching { hasNextElement() }.flatMap { hasNext ->
            if (hasNext) {
                seed.deserialize(de)
            } else {
                SerdeResult.success(null)
            }
        }

    private fun hasNextElement(): Boolean {
        val parser = de.parser
        val peek = parser.parseWhitespace()
        if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingList))
        return when {
            peek == ']'.code -> false
            first -> {
                first = false
                true
            }
            peek == ','.code -> {
                parser.eatChar()
                val p = parser.parseWhitespace()
                if (p == ']'.code) throw SerdeException(parser.peekError(ErrorCode.TrailingComma))
                if (p < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
                true
            }
            else -> throw SerdeException(parser.peekError(ErrorCode.ExpectedListCommaOrEnd))
        }
    }
}

// ─── MapAccess ─────────────────────────────────────────────────────────────────

/**
 * Map access for JSON objects.
 *
 * Created by [JsonDeserializer.deserializeMap] and passed to the visitor's
 * `visitMap`. Each call to `nextKeySeed` checks for `}` (end) or `,`
 * (separator), then deserializes the key via [JsonMapKey] (to support
 * numeric keys). `nextValueSeed` parses the `:` then deserializes the value.
 */
internal class JsonMapAccess(
    private val de: JsonDeserializer,
) : MapAccess {
    private var first = true

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> =
        serdeCatching { hasNextKey() }.flatMap { hasNext ->
            if (hasNext) {
                seed.deserialize(JsonMapKey(de))
            } else {
                SerdeResult.success(null)
            }
        }

    private fun hasNextKey(): Boolean {
        val parser = de.parser
        val peek = parser.parseWhitespace()
        if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingObject))
        return when {
            peek == '}'.code -> false
            first -> {
                first = false
                if (peek == '"'.code) {
                    true
                } else {
                    throw SerdeException(parser.peekError(ErrorCode.KeyMustBeAString))
                }
            }
            peek == ','.code -> {
                parser.eatChar()
                val p = parser.parseWhitespace()
                if (p == '"'.code) {
                    true
                } else if (p == '}'.code) {
                    throw SerdeException(parser.peekError(ErrorCode.TrailingComma))
                } else if (p < 0) {
                    throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
                } else {
                    throw SerdeException(parser.peekError(ErrorCode.KeyMustBeAString))
                }
            }
            else -> throw SerdeException(parser.peekError(ErrorCode.ExpectedObjectCommaOrEnd))
        }
    }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> =
        serdeCatching { de.parser.parseObjectColon() }.flatMap {
            seed.deserialize(de)
        }
}

// ─── VariantAccess (enum object form) ─────────────────────────────────────────

/**
 * Enum access for the `{"variant": value}` form.
 *
 * Created by [JsonDeserializer.deserializeEnum] when the input is a JSON object.
 * The `variantSeed` method deserializes the key (variant name) and consumes
 * the `:`. The returned [VariantAccess] then provides access to the variant's
 * content.
 */
internal class JsonVariantAccess(
    private val de: JsonDeserializer,
) : EnumAccess,
    VariantAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        serdeCatching {
            val value = seed.deserialize(de).getOrThrow()
            de.parser.parseObjectColon()
            Pair(value, this as VariantAccess)
        }

    override fun unitVariant(): SerdeResult<Unit> =
        serdeCatching {
            de.parser.parseValue().getOrThrow()
        }

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
        seed.deserialize(de)

    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        de.deserializeSeq(visitor)

    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        de.deserializeStruct("", fields, visitor)
}

// ─── UnitVariantAccess (enum bare string form) ─────────────────────────────────

/**
 * Enum access for the bare `"variantName"` form (unit variants).
 *
 * Created by [JsonDeserializer.deserializeEnum] when the input is a JSON string.
 * The variant name is the string itself. Since this is a unit variant, the
 * `unitVariant` method is a no-op, and the other methods return errors.
 */
internal class JsonUnitVariantAccess(
    private val de: JsonDeserializer,
) : EnumAccess,
    VariantAccess {
    override fun <V> variantSeed(seed: DeserializeSeed<V>): SerdeResult<Pair<V, VariantAccess>> =
        serdeCatching {
            val variant = seed.deserialize(de).getOrThrow()
            Pair(variant, this as VariantAccess)
        }

    override fun unitVariant(): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
        SerdeResult.failure(SerdeError.invalidType(Unexpected.UnitVariant, Expected { "newtype variant" }))

    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.invalidType(Unexpected.UnitVariant, Expected { "tuple variant" }))

    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.invalidType(Unexpected.UnitVariant, Expected { "struct variant" }))
}

// ─── MapKey deserializer ───────────────────────────────────────────────────────
//
// A specialized [Deserializer] for deserializing map keys.
// In JSON, map keys are always strings, but serde allows deserializing them
// as numbers, booleans, etc. This deserializer handles the string-to-number
// conversion for numeric keys.
//
// Only call this after peeking a `"` byte — it consumes the opening quote
// and parses the string content, then checks for the closing quote.

/**
 * A deserializer for JSON map keys.
 *
 * Map keys in JSON are always strings, but serde may request them as numbers
 * or booleans. This deserializer handles the conversion:
 * - For `deserializeAny` / `deserializeStr`, it parses the string normally.
 * - For numeric types, it parses the string content as a number in quotes
 *   (e.g., `"42"` → 42).
 * - For `deserializeBool`, it reads `true`/`false` within the quotes.
 */
internal class JsonMapKey(
    private val de: JsonDeserializer,
) : Deserializer {
    private val parser = de.parser

    override fun isHumanReadable(): Boolean = true

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            parser.eatChar() // consume opening '"'
            parser.parseStr()
        }.flatMap { visitor.visitStr(it) }

    // Numeric keys: parse the string content as a number, then expect closing quote
    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeI8(it) }

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeI16(it) }

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeI32(it) }

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeI64(it) }

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeI128(it) }

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeU8(it) }

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeU16(it) }

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeU32(it) }

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeU64(it) }

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeU128(it) }

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeF32(it) }

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumericKey(visitor) { de.deserializeF64(it) }

    /**
     * Parses a numeric key: consumes the opening `"`, checks that the content
     * starts with a digit or `-`, delegates to the main deserializer's
     * numeric parsing, then checks for the closing `"`.
     */
    private fun <V> deserializeNumericKey(visitor: Visitor<V>, delegate: (Visitor<V>) -> SerdeResult<V>): SerdeResult<V> =
        serdeCatching {
            parser.eatChar() // consume opening '"'
            val peek = parser.peekOrNull()
            if (peek.toChar() !in '0'..'9' && peek != '-'.code) {
                throw SerdeException(parser.error(ErrorCode.ExpectedNumericKey))
            }
            val result = delegate(visitor).getOrThrow()
            val after = parser.peekOrNull()
            if (after != '"'.code) throw SerdeException(parser.peekError(ErrorCode.ExpectedDoubleQuote))
            parser.eatChar() // consume closing '"'
            result
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            parser.eatChar() // consume opening '"'
            val peek = parser.nextCharOrNull()
            if (peek < 0) throw SerdeException(parser.peekError(ErrorCode.EofWhileParsingValue))
            when (peek.toChar()) {
                't' -> {
                    parser.parseIdent("rue\"")
                    true
                }
                'f' -> {
                    parser.parseIdent("alse\"")
                    false
                }
                else -> {
                    // Not a bool — parse as string and return invalidType error
                    val s = parser.parseStr()
                    throw SerdeException(SerdeError.invalidType(Unexpected.Str(s), visitor))
                }
            }
        }.flatMap { visitor.visitBool(it) }

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        visitor.visitSome(this)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> {
        if (name == RawValue.TOKEN) return de.deserializeRawValue(visitor)
        return visitor.visitNewtypeStruct(this)
    }

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        de.deserializeEnum(name, variants, visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> =
        de.deserializeBytes(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> =
        de.deserializeBytes(visitor)

    // Forward to deserializeAny for string-like types
    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.invalidType(Unexpected.Str("map key"), Expected { "unit" }))

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        deserializeUnit(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.invalidType(Unexpected.Str("map key"), Expected { "sequence" }))

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.invalidType(Unexpected.Str("map key"), Expected { "map" }))

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        deserializeMap(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
}

// ─── StreamDeserializer ─────────────────────────────────────────────────────────
//
// An iterator that deserializes a stream into multiple JSON values.
// Each call to `next` parses one value from the input, allowing processing
// of concatenated JSON values (e.g., `{"k": 3}1"cool""stuff" 3{} [0, 1, 2]`).

/**
 * An iterator that deserializes a stream of concatenated JSON values.
 *
 * Created from [JsonParser] — each call to [next] parses one JSON value
 * from the remaining input. Values need to be self-delineating (arrays,
 * objects, strings) or followed by whitespace.
 *
 * Example:
 * ```
 * val stream = StreamDeserializer(MyDeserialize)
 * for (value in stream) { ... }
 * ```
 */
class StreamDeserializer<T>(
    private val deserialize: io.github.kotlinmania.serdecore.de.Deserialize<T>,
    private val parser: JsonParser,
) : Iterator<SerdeResult<T>> {
    private var offset: Int = 0
    private var failed: Boolean = false

    /** Returns the byte offset of the last successfully deserialized value. */
    fun byteOffset(): Int = offset

    override fun hasNext(): Boolean {
        if (failed) return false
        return parser.hasMore()
    }

    override fun next(): SerdeResult<T> {
        if (failed) throw NoSuchElementException("stream deserializer has failed")
        val peek = parser.parseWhitespace()
        if (peek < 0) {
            offset = parser.byteOffset()
            throw NoSuchElementException("end of stream")
        }
        offset = parser.byteOffset()
        val selfDelineated = peek == '['.code || peek == '"'.code || peek == '{'.code
        val deserializer = JsonDeserializer(parser)
        val result = deserialize.deserialize(deserializer)
        if (result is SerdeResult.Failure) {
            failed = true
            return result
        }
        offset = parser.byteOffset()
        if (selfDelineated) {
            return result
        }
        // For non-self-delineated values (numbers, null, true, false),
        // check that the next byte is whitespace or a self-delineating char.
        val after = parser.peekRaw()
        if (after >= 0) {
            val c = after.toChar()
            if (c !in setOf(' ', '\n', '\t', '\r', '"', '[', ']', '{', '}', ',', ':')) {
                failed = true
                return SerdeResult.failure(parser.peekError(ErrorCode.TrailingCharacters))
            }
        }
        return result
    }
}

/**
 * Creates a [StreamDeserializer] from a JSON string and a [Deserialize] implementation.
 */
fun <T> streamDeserializer(json: String, deserialize: io.github.kotlinmania.serdecore.de.Deserialize<T>): StreamDeserializer<T> =
    StreamDeserializer(deserialize, JsonParser(json))

// ─── Top-level deserialization functions ───────────────────────────────────────

/**
 * Deserialize an instance of type T from a string of JSON text using the given [Deserialize].
 *
 * Verifies that the entire input is consumed (no trailing characters).
 */
fun <T> deserializeFromStr(json: String, deserialize: io.github.kotlinmania.serdecore.de.Deserialize<T>): SerdeResult<T> =
    serdeCatching {
        val parser = JsonParser(json)
        val de = JsonDeserializer(parser)
        val value = deserialize.deserialize(de).getOrThrow()
        parser.end().getOrThrow()
        value
    }

/**
 * Deserialize an instance of type T from a byte array of JSON text using the given [Deserialize].
 */
fun <T> deserializeFromSlice(json: ByteArray, deserialize: io.github.kotlinmania.serdecore.de.Deserialize<T>): SerdeResult<T> =
    deserializeFromStr(json.decodeToString(), deserialize)

/**
 * Parse a JSON string into a [Value].
 */
fun fromStr(json: String): SerdeResult<Value> =
    JsonParser(json).parseValue()

/**
 * Parse a JSON string into a type T using the given [io.github.kotlinmania.serdecore.de.Deserialize] implementation.
 */
fun <T> fromStr(json: String, deserialize: io.github.kotlinmania.serdecore.de.Deserialize<T>): SerdeResult<T> {
    val parser = JsonParser(json)
    val deserializer = JsonDeserializer(parser)
    return deserialize.deserialize(deserializer)
}

/**
 * Parse a JSON byte array into a [Value].
 */
fun fromSlice(json: ByteArray): SerdeResult<Value> =
    fromStr(json.decodeToString())

/**
 * Parse a JSON byte array into a type T using the given [io.github.kotlinmania.serdecore.de.Deserialize] implementation.
 */
fun <T> fromSlice(json: ByteArray, deserialize: io.github.kotlinmania.serdecore.de.Deserialize<T>): SerdeResult<T> =
    fromStr(json.decodeToString(), deserialize)
