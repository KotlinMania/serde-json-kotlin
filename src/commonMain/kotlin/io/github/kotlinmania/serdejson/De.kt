// port-lint: source de.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeDiagnosticCarrier
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.Expected
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdejson.io.IoReader
import io.github.kotlinmania.serdejson.lexical.F32Float
import io.github.kotlinmania.serdejson.lexical.F64Float
import io.github.kotlinmania.serdejson.lexical.parseConciseFloat
import io.github.kotlinmania.serdejson.lexical.parseTruncatedFloat

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
    internal var singlePrecision: Boolean = false
    private val scratch: MutableList<Byte> = ArrayList()

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

                else -> {
                    return
                }
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
    internal fun peekError(code: ErrorCode): SerdeError {
        val position =
            when {
                pos >= input.length -> line to col
                input[pos] == '\n' -> (line + 1) to 0
                else -> line to (col + 1)
            }
        return SerdeError.custom(JsonError.syntax(code, position.first, position.second).toString())
    }

    // ── Identifier parsing ──

    /**
     * Parses an identifier (e.g., "ull" for null, "rue" for true, "alse" for false).
     * The first character (n/t/f) has already been consumed by the caller.
     */
    internal fun parseIdent(ident: String) {
        for (expected in ident) {
            when (val next = nextChar()) {
                -1 -> (error(ErrorCode.EofWhileParsingValue)).raise()
                else -> if (next != expected.code) (error(ErrorCode.ExpectedSomeIdent)).raise()
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
        if (peek < 0) (peekError(ErrorCode.EofWhileParsingValue)).raise()
        val value =
            when (peek.toChar()) {
                '-' -> {
                    eatChar()
                    parseAnyNumber(false)
                }

                in '0'..'9' -> {
                    parseAnyNumber(true)
                }

                else -> {
                    (peekError(ErrorCode.InvalidNumber)).raise()
                }
            }
        // After a complete number, the next char must not be a digit or
        // other number continuation — otherwise it's invalid.
        val after = peekRaw()
        if (after >= 0) (peekError(ErrorCode.InvalidNumber)).raise()
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
                    (peekError(ErrorCode.InvalidNumber)).raise()
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

            else -> {
                (error(ErrorCode.InvalidNumber)).raise()
            }
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
            '.' -> {
                DeParserNumber.F64(parseDecimal(positive, significand, 0))
            }

            'e', 'E' -> {
                DeParserNumber.F64(parseExponent(positive, significand, 0))
            }

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
            if (peekRaw() >= 0) (peekError(ErrorCode.InvalidNumber)).raise()
            (peekError(ErrorCode.EofWhileParsingValue)).raise()
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

                else -> {
                    true
                }
            }
        val next = nextChar()
        if (next < 0) (error(ErrorCode.EofWhileParsingValue)).raise()
        if (next.toChar() !in '0'..'9') (error(ErrorCode.InvalidNumber)).raise()
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

    /** Handles overflow in the integer part by switching to truncated lexical float parsing without losing precision. */
    private fun parseLongInteger(positive: Boolean, partialSignificand: ULong): Double {
        scratch.clear()
        for (b in partialSignificand.toString().encodeToByteArray()) {
            scratch.add(b)
        }
        while (true) {
            when (val ch = peekOrNull().toChar()) {
                in '0'..'9' -> {
                    scratch.add(ch.code.toByte())
                    eatChar()
                }

                '.' -> {
                    eatChar()
                    return parseLongDecimal(positive, scratch.size)
                }

                'e', 'E' -> {
                    return parseLongExponent(positive, scratch.size)
                }

                else -> {
                    return f64LongFromParts(positive, scratch.size, 0)
                }
            }
        }
    }

    private fun parseLongDecimal(positive: Boolean, integerEnd: Int): Double {
        var atLeastOneDigit = integerEnd < scratch.size
        while (peekOrNull().toChar() in '0'..'9') {
            val ch = peekOrNull()
            scratch.add(ch.toByte())
            eatChar()
            atLeastOneDigit = true
        }
        if (!atLeastOneDigit) {
            if (peekRaw() >= 0) (peekError(ErrorCode.InvalidNumber)).raise()
            (peekError(ErrorCode.EofWhileParsingValue)).raise()
        }
        return when (peekOrNull().toChar()) {
            'e', 'E' -> parseLongExponent(positive, integerEnd)
            else -> f64LongFromParts(positive, integerEnd, 0)
        }
    }

    private fun parseLongExponent(positive: Boolean, integerEnd: Int): Double {
        eatChar()
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

                else -> {
                    true
                }
            }
        val next = nextChar()
        if (next < 0) (error(ErrorCode.EofWhileParsingValue)).raise()
        if (next.toChar() !in '0'..'9') (error(ErrorCode.InvalidNumber)).raise()
        var exp = next - '0'.code
        while (peekOrNull().toChar() in '0'..'9') {
            val digit = peekOrNull() - '0'.code
            eatChar()
            if (overflow(exp, digit, Int.MAX_VALUE)) {
                val zeroSignificand = scratch.all { it == '0'.code.toByte() }
                return parseExponentOverflow(positive, zeroSignificand, positiveExp)
            }
            exp = exp * 10 + digit
        }
        val finalExp = if (positiveExp) exp else -exp
        return f64LongFromParts(positive, integerEnd, finalExp)
    }

    /** Handles overflow in the decimal part by switching to truncated lexical float parsing. */
    private fun parseDecimalOverflow(
        positive: Boolean,
        significand: ULong,
        exponent: Int,
    ): Double {
        val sigText = significand.toString()
        val fractionDigits = -exponent
        scratch.clear()
        val zeros = fractionDigits - (sigText.length + 1)
        if (zeros >= 0) {
            repeat(zeros + 1) {
                scratch.add('0'.code.toByte())
            }
        }
        for (b in sigText.encodeToByteArray()) {
            scratch.add(b)
        }
        val integerEnd = scratch.size - fractionDigits
        return parseLongDecimal(positive, integerEnd)
    }

    /** Handles overflow in the exponent part. */
    private fun parseExponentOverflow(
        positive: Boolean,
        zeroSig: Boolean,
        positiveExp: Boolean,
    ): Double {
        if (!zeroSig && positiveExp) (error(ErrorCode.NumberOutOfRange)).raise()
        while (peekOrNull().toChar() in '0'..'9') eatChar()
        return if (positive) 0.0 else -0.0
    }

    private fun f64LongFromParts(
        positive: Boolean,
        integerEnd: Int,
        exponent: Int,
    ): Double {
        val raw = scratch.toByteArray()
        val integer = raw.copyOfRange(0, integerEnd)
        val fraction = raw.copyOfRange(integerEnd, raw.size)
        val traits = if (singlePrecision) F32Float else F64Float
        val f = parseTruncatedFloat(integer, fraction, exponent, traits)
        if (f.isInfinite()) (error(ErrorCode.NumberOutOfRange)).raise()
        return if (positive) f else -f
    }

    /**
     * Constructs an exact IEEE-754 floating-point value from a 64-bit significand and decimal exponent.
     */
    private fun f64FromParts(positive: Boolean, significand: ULong, exponent: Int): Double {
        val traits = if (singlePrecision) F32Float else F64Float
        val f = parseConciseFloat(significand, exponent, traits)
        if (f.isInfinite()) (error(ErrorCode.NumberOutOfRange)).raise()
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
                    (peekError(ErrorCode.InvalidNumber)).raise()
                }
            }

            in '1'..'9' -> {
                buf.append(next.toChar())
                while (peekOrNull().toChar() in '0'..'9') {
                    buf.append(peekOrNull().toChar())
                    eatChar()
                }
            }

            else -> {
                (error(ErrorCode.InvalidNumber)).raise()
            }
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
                c == '"' -> {
                    return sb.toString()
                }

                c == '\\' -> {
                    if (pos >= input.length) (error(ErrorCode.EofWhileParsingString)).raise()
                    val esc = input[pos]
                    pos++
                    if (esc == '\n') {
                        line++
                        col = 0
                    } else {
                        col++
                    }
                    when (esc) {
                        '"' -> {
                            sb.append('"')
                        }

                        '\\' -> {
                            sb.append('\\')
                        }

                        '/' -> {
                            sb.append('/')
                        }

                        'n' -> {
                            sb.append('\n')
                        }

                        'r' -> {
                            sb.append('\r')
                        }

                        't' -> {
                            sb.append('\t')
                        }

                        'b' -> {
                            sb.append('\b')
                        }

                        'f' -> {
                            sb.append('\u000C')
                        }

                        'u' -> {
                            val cp = decodeHexEscape()
                            if (cp in 0xDC00..0xDFFF) {
                                (error(ErrorCode.LoneLeadingSurrogateInHexEscape)).raise()
                            } else if (cp in 0xD800..0xDBFF) {
                                val slash = nextChar()
                                if (slash < 0) (error(ErrorCode.EofWhileParsingString)).raise()
                                if (slash != '\\'.code) (error(ErrorCode.UnexpectedEndOfHexEscape)).raise()
                                val uChar = nextChar()
                                if (uChar < 0) (error(ErrorCode.EofWhileParsingString)).raise()
                                if (uChar != 'u'.code) (error(ErrorCode.UnexpectedEndOfHexEscape)).raise()
                                val low = decodeHexEscape()
                                if (low !in 0xDC00..0xDFFF) {
                                    (error(ErrorCode.LoneLeadingSurrogateInHexEscape)).raise()
                                }
                                val full = 0x10000 + ((cp - 0xD800) shl 10) + (low - 0xDC00)
                                sb.appendCodePoint(full)
                            } else {
                                sb.appendCodePoint(cp)
                            }
                        }

                        else -> {
                            (error(ErrorCode.InvalidEscape)).raise()
                        }
                    }
                }

                c.code < 0x20 -> {
                    (error(ErrorCode.ControlCharacterWhileParsingString)).raise()
                }

                c.isHighSurrogate() -> {
                    if (pos < input.length && input[pos].isLowSurrogate()) {
                        val low = input[pos]
                        pos++
                        col++
                        sb.append(c)
                        sb.append(low)
                    } else {
                        (error(ErrorCode.InvalidUnicodeCodePoint)).raise()
                    }
                }

                c.isLowSurrogate() -> {
                    (error(ErrorCode.InvalidUnicodeCodePoint)).raise()
                }

                else -> {
                    sb.append(c)
                }
            }
        }
        (error(ErrorCode.EofWhileParsingString)).raise()
    }

    /** Parses a 4-hex-digit unicode escape. Assumes `\\u` has been consumed. */
    private fun decodeHexEscape(): Int {
        if (pos + 4 > input.length) {
            while (pos < input.length) {
                eatChar()
            }
            (error(ErrorCode.EofWhileParsingString)).raise()
        }
        val c0 = input[pos]
        val c1 = input[pos + 1]
        val c2 = input[pos + 2]
        val c3 = input[pos + 3]
        pos += 4
        col += 4
        if (c0.code > 0xFF || c1.code > 0xFF || c2.code > 0xFF || c3.code > 0xFF) {
            (error(ErrorCode.InvalidEscape)).raise()
        }
        val decoded =
            decodeFourHexDigits(
                c0.code.toByte(),
                c1.code.toByte(),
                c2.code.toByte(),
                c3.code.toByte(),
            ) ?: (error(ErrorCode.InvalidEscape)).raise()
        return decoded.toInt()
    }

    private fun parseRawEscapeInto(rawBytes: MutableList<Byte>) {
        if (pos >= input.length) (error(ErrorCode.EofWhileParsingString)).raise()
        val esc = nextChar().toChar()
        when (esc) {
            '"' -> rawBytes.add('"'.code.toByte())
            '\\' -> rawBytes.add('\\'.code.toByte())
            '/' -> rawBytes.add('/'.code.toByte())
            'n' -> rawBytes.add('\n'.code.toByte())
            'r' -> rawBytes.add('\r'.code.toByte())
            't' -> rawBytes.add('\t'.code.toByte())
            'b' -> rawBytes.add(0x08.toByte())
            'f' -> rawBytes.add(0x0C.toByte())
            'u' -> parseRawUnicodeEscapeInto(rawBytes)
            else -> (error(ErrorCode.InvalidEscape)).raise()
        }
    }

    private fun parseRawUnicodeEscapeInto(rawBytes: MutableList<Byte>) {
        var n = decodeHexEscape()
        while (true) {
            if (n !in 0xD800..0xDBFF) {
                pushWtf8Codepoint(n, rawBytes)
                return
            }
            val n1 = n
            if (peekRaw() < 0) (error(ErrorCode.EofWhileParsingString)).raise()
            if (peekRaw() == '\\'.code) {
                eatChar()
            } else {
                pushWtf8Codepoint(n1, rawBytes)
                return
            }
            if (peekRaw() < 0) (error(ErrorCode.EofWhileParsingString)).raise()
            if (peekRaw() == 'u'.code) {
                eatChar()
            } else {
                pushWtf8Codepoint(n1, rawBytes)
                parseRawEscapeInto(rawBytes)
                return
            }
            val n2 = decodeHexEscape()
            if (n2 !in 0xDC00..0xDFFF) {
                pushWtf8Codepoint(n1, rawBytes)
                n = n2
                continue
            }
            val combined = (((n1 - 0xD800) shl 10) or (n2 - 0xDC00)) + 0x10000
            pushWtf8Codepoint(combined, rawBytes)
            return
        }
    }

    /** Parses a JSON string as raw bytes (for deserializeBytes), allowing WTF-8 lone surrogates. */
    internal fun parseStrRaw(): ByteArray {
        val rawBytes = ArrayList<Byte>()
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
                c == '"' -> {
                    return rawBytes.toByteArray()
                }

                c == '\\' -> {
                    parseRawEscapeInto(rawBytes)
                }

                c.isHighSurrogate() -> {
                    if (pos < input.length && input[pos].isLowSurrogate()) {
                        val low = input[pos]
                        pos++
                        col++
                        val combined = 0x10000 + ((c.code - 0xD800) shl 10) + (low.code - 0xDC00)
                        pushWtf8Codepoint(combined, rawBytes)
                    } else {
                        pushWtf8Codepoint(c.code, rawBytes)
                    }
                }

                else -> {
                    pushWtf8Codepoint(c.code, rawBytes)
                }
            }
        }
        (error(ErrorCode.EofWhileParsingString)).raise()
    }

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
                c == '"' -> {
                    return
                }

                c == '\\' -> {
                    if (pos >= input.length) (error(ErrorCode.EofWhileParsingString)).raise()
                    when (nextChar().toChar()) {
                        '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> {
                            Unit
                        }

                        'u' -> {
                            decodeHexEscape()
                        }

                        else -> {
                            (error(ErrorCode.InvalidEscape)).raise()
                        }
                    }
                }

                c.code < 0x20 -> {
                    (error(ErrorCode.ControlCharacterWhileParsingString)).raise()
                }
            }
        }
        (error(ErrorCode.EofWhileParsingString)).raise()
    }

    // ── Value parsing (used by fromStr for Value) ──

    /** Parses a JSON value into a [Value]. */
    fun parseValue(): SerdeResult<Value> =
        serdeCatching {
            val peek = parseWhitespace()
            if (peek < 0) (error(ErrorCode.EofWhileParsingValue)).raise()
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

                '[' -> {
                    parseArrayValue()
                }

                '{' -> {
                    parseObjectValue()
                }

                '-' -> {
                    eatChar()
                    val n = parseAnyNumber(false)
                    numberToValue(n)
                }

                in '0'..'9' -> {
                    val n = parseAnyNumber(true)
                    numberToValue(n)
                }

                else -> {
                    (peekError(ErrorCode.ExpectedSomeValue)).raise()
                }
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
                    if (pos >= input.length) (peekError(ErrorCode.EofWhileParsingList)).raise()
                    (peekError(ErrorCode.ExpectedListCommaOrEnd)).raise()
                }
            }
        }
    }

    private fun parseObjectValue(): Value {
        eatChar() // consume '{'
        val map =
            io.github.kotlinmania.serdejson
                .JsonMap()
        if (parseWhitespace().toChar() == '}') {
            eatChar()
            return Value.Object(map)
        }
        while (true) {
            if (parseWhitespace().toChar() != '"') {
                if (pos >= input.length) (peekError(ErrorCode.EofWhileParsingObject)).raise()
                (peekError(ErrorCode.KeyMustBeAString)).raise()
            }
            eatChar() // consume '"'
            val key = parseStr()
            if (parseWhitespace().toChar() != ':') {
                if (pos >= input.length) (peekError(ErrorCode.EofWhileParsingObject)).raise()
                (peekError(ErrorCode.ExpectedColon)).raise()
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
                    if (pos >= input.length) (peekError(ErrorCode.EofWhileParsingObject)).raise()
                    (peekError(ErrorCode.ExpectedObjectCommaOrEnd)).raise()
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
            if (peek < 0) (peekError(ErrorCode.EofWhileParsingValue)).raise()
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

                    else -> {
                        (peekError(ErrorCode.ExpectedSomeValue)).raise()
                    }
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

                else -> {
                    return
                }
            }

            while (true) {
                val p = parseWhitespace()
                if (p < 0) {
                    (
                        peekError(
                            if (currentFrame == '[') ErrorCode.EofWhileParsingList else ErrorCode.EofWhileParsingObject,
                        )
                    ).raise()
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
                            (
                                peekError(
                                    if (currentFrame == '[') ErrorCode.ExpectedListCommaOrEnd else ErrorCode.ExpectedObjectCommaOrEnd,
                                )
                            ).raise()
                        } else {
                            break
                        }
                    }
                }
            }

            // After comma in an object, expect a key
            if (currentFrame == '{') {
                val p = parseWhitespace()
                if (p < 0) (peekError(ErrorCode.EofWhileParsingObject)).raise()
                if (p.toChar() == '"') {
                    eatChar()
                    ignoreStr()
                    val cp = parseWhitespace()
                    if (cp < 0) (peekError(ErrorCode.EofWhileParsingObject)).raise()
                    if (cp.toChar() != ':') (peekError(ErrorCode.ExpectedColon)).raise()
                    eatChar()
                } else {
                    (peekError(ErrorCode.KeyMustBeAString)).raise()
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
                    (peekError(ErrorCode.InvalidNumber)).raise()
                }
            }

            in '1'..'9' -> {
                while (peekOrNull().toChar() in '0'..'9') eatChar()
            }

            else -> {
                (error(ErrorCode.InvalidNumber)).raise()
            }
        }
        when (peekOrNull().toChar()) {
            '.' -> {
                ignoreDecimal()
            }

            'e', 'E' -> {
                ignoreExponent()
            }

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
        if (!atLeastOne) (peekError(ErrorCode.InvalidNumber)).raise()
        when (peekOrNull().toChar()) {
            'e', 'E' -> {
                ignoreExponent()
            }

            else -> {}
        }
    }

    private fun ignoreExponent() {
        eatChar()
        when (peekOrNull().toChar()) {
            '+', '-' -> {
                eatChar()
            }

            else -> {}
        }
        val next = nextCharOrNull()
        if (next.toChar() !in '0'..'9') (error(ErrorCode.InvalidNumber)).raise()
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

            -1 -> {
                (peekError(ErrorCode.EofWhileParsingObject)).raise()
            }

            else -> {
                (peekError(ErrorCode.ExpectedColon)).raise()
            }
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
                if (p2 == ']'.code) (peekError(ErrorCode.TrailingComma)).raise()
                (peekError(ErrorCode.TrailingCharacters)).raise()
            }

            -1 -> {
                (peekError(ErrorCode.EofWhileParsingList)).raise()
            }

            else -> {
                (peekError(ErrorCode.TrailingCharacters)).raise()
            }
        }
    }

    /** Checks for `}` or `,` `}` at end of a map. */
    internal fun endMap() {
        when (parseWhitespace()) {
            '}'.code -> {
                eatChar()
            }

            ','.code -> {
                (peekError(ErrorCode.TrailingComma)).raise()
            }

            -1 -> {
                (peekError(ErrorCode.EofWhileParsingObject)).raise()
            }

            else -> {
                (peekError(ErrorCode.TrailingCharacters)).raise()
            }
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

// ─── JsonDeserializer ─────────────────────────────────────────────────────────
//
// The serde [Deserializer] implementation backed by [JsonParser].
// Each `deserialize*` method peeks at the next byte, validates it matches
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

    private var recursionLimitDisabled: Boolean = false

    /** Disables the default 128-level container recursion limit on this deserializer. */
    fun disableRecursionLimit() {
        recursionLimitDisabled = true
    }

    /** Validates that the input stream is at the end or only has trailing whitespace. */
    fun end(): SerdeResult<Unit> = parser.end()

    /** Turns this JSON deserializer into an iterator over concatenated JSON values of type [T]. */
    fun <T> intoIter(deserialize: Deserialize<T>): StreamDeserializer<T> =
        StreamDeserializer(deserialize, parser)

    /** Decrements recursion depth; raises [ErrorCode.RecursionLimitExceeded] if limit exceeded. */
    private inline fun <T> checkRecursion(block: () -> T): T {
        if (recursionLimitDisabled) {
            return block()
        }
        remainingDepth--
        if (remainingDepth == 0) {
            (parser.peekError(ErrorCode.RecursionLimitExceeded)).raise()
        }
        try {
            return block()
        } finally {
            remainingDepth++
        }
    }

    companion object {
        /** Creates a [JsonDeserializer] from a JSON string. */
        fun fromStr(s: String): JsonDeserializer = JsonDeserializer(JsonParser(s))
    }

    override fun isHumanReadable(): Boolean = true

    // ── deserializeAny ──

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
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
                        ret.getOrThrow()
                        parser.endSeq()
                        ret
                    }
                }

                '{' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitMap(JsonMapAccess(this))
                        ret.getOrThrow()
                        parser.endMap()
                        ret
                    }
                }

                else -> {
                    (parser.peekError(ErrorCode.ExpectedSomeValue)).raise()
                }
            }
        }.flatMap { it }

    // ── deserializeBool ──

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
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

                else -> {
                    (peekInvalidType(visitor)).raise()
                }
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

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> {
        parser.singlePrecision = true
        return try {
            deserializeNumber(visitor)
        } finally {
            parser.singlePrecision = false
        }
    }

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
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
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            if (peek == '-'.code) (parser.peekError(ErrorCode.NumberOutOfRange)).raise()
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
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            when (peek.toChar()) {
                '-' -> {
                    parser.eatChar()
                    parser.parseInteger(false).visit(visitor)
                }

                in '0'..'9' -> {
                    parser.parseInteger(true).visit(visitor)
                }

                else -> {
                    (peekInvalidType(visitor)).raise()
                }
            }
        }.flatMap { it }

    // ── deserializeChar ──

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    // ── deserializeStr / string ──

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            if (peek != '"'.code) (peekInvalidType(visitor)).raise()
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

            '[' -> {
                deserializeSeq(visitor)
            }

            else -> {
                SerdeResult.failure(peekInvalidType(visitor))
            }
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
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            if (peek != 'n'.code) (peekInvalidType(visitor)).raise()
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
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            if (peek != '['.code) (peekInvalidType(visitor)).raise()
            checkRecursion {
                parser.eatChar()
                val ret = visitor.visitSeq(JsonSeqAccess(this))
                ret.getOrThrow()
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
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            if (peek != '{'.code) (peekInvalidType(visitor)).raise()
            checkRecursion {
                parser.eatChar()
                val ret = visitor.visitMap(JsonMapAccess(this))
                ret.getOrThrow()
                parser.endMap()
                ret
            }
        }.flatMap { it }

    // ── deserializeStruct ──

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val peek = parser.parseWhitespace()
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            when (peek.toChar()) {
                '[' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitSeq(JsonSeqAccess(this))
                        ret.getOrThrow()
                        parser.endSeq()
                        ret
                    }
                }

                '{' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitMap(JsonMapAccess(this))
                        ret.getOrThrow()
                        parser.endMap()
                        ret
                    }
                }

                else -> {
                    (peekInvalidType(visitor)).raise()
                }
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
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
            when (peek.toChar()) {
                '{' -> {
                    checkRecursion {
                        parser.eatChar()
                        val ret = visitor.visitEnum(JsonVariantAccess(this))
                        ret.getOrThrow()
                        // After the variant, expect '}'
                        val p = parser.parseWhitespace()
                        if (p < 0) (parser.error(ErrorCode.EofWhileParsingObject)).raise()
                        if (p != '}'.code) (parser.error(ErrorCode.ExpectedSomeValue)).raise()
                        parser.eatChar()
                        ret
                    }
                }

                '"' -> {
                    visitor.visitEnum(JsonUnitVariantAccess(this))
                }

                else -> {
                    (parser.peekError(ErrorCode.ExpectedSomeValue)).raise()
                }
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
        val error =
            when (peek.toChar()) {
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
                    } catch (e: Throwable) {
                        if (e is SerdeDiagnosticCarrier) e.error else throw e
                    }
                }

                in '0'..'9' -> {
                    try {
                        parser.parseAnyNumber(true).invalidType(visitor)
                    } catch (e: Throwable) {
                        if (e is SerdeDiagnosticCarrier) e.error else throw e
                    }
                }

                '"' -> {
                    parser.eatChar()
                    val s = parser.parseStr()
                    SerdeError.invalidType(Unexpected.Str(s), visitor)
                }

                '[' -> {
                    SerdeError.invalidType(Unexpected.Seq, visitor)
                }

                '{' -> {
                    SerdeError.invalidType(Unexpected.Map, visitor)
                }

                else -> {
                    parser.peekError(ErrorCode.ExpectedSomeValue)
                }
            }
        val positioned = JsonError.custom(error.message)
        return if (positioned.line() == 0) parser.error(positioned.code) else error
    }

    // ── Raw value deserialization ──

    /**
     * Deserializes a raw JSON value — the raw text of any valid JSON value
     * is captured and passed to the visitor in a single-entry map.
     */
    internal fun <V> deserializeRawValue(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            parser.parseRawValue()
        }.flatMap { raw ->
            visitor.visitMap(OwnedRawDeserializer(raw))
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

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
        serdeCatching { hasNextElement() }.flatMap { hasNext ->
            if (hasNext) {
                seed.deserialize(de).map { value -> SeqElement.Some(value) }
            } else {
                SerdeResult.success(SeqElement.None)
            }
        }

    private fun hasNextElement(): Boolean {
        val parser = de.parser
        val peek = parser.parseWhitespace()
        if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingList)).raise()
        return when {
            peek == ']'.code -> {
                false
            }

            first -> {
                first = false
                true
            }

            peek == ','.code -> {
                parser.eatChar()
                val p = parser.parseWhitespace()
                if (p == ']'.code) (parser.peekError(ErrorCode.TrailingComma)).raise()
                if (p < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
                true
            }

            else -> {
                (parser.peekError(ErrorCode.ExpectedListCommaOrEnd)).raise()
            }
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
        if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingObject)).raise()
        return when {
            peek == '}'.code -> {
                false
            }

            first -> {
                first = false
                if (peek == '"'.code) {
                    true
                } else {
                    (parser.peekError(ErrorCode.KeyMustBeAString)).raise()
                }
            }

            peek == ','.code -> {
                parser.eatChar()
                val p = parser.parseWhitespace()
                if (p == '"'.code) {
                    true
                } else if (p == '}'.code) {
                    (parser.peekError(ErrorCode.TrailingComma)).raise()
                } else if (p < 0) {
                    (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
                } else {
                    (parser.peekError(ErrorCode.KeyMustBeAString)).raise()
                }
            }

            else -> {
                (parser.peekError(ErrorCode.ExpectedObjectCommaOrEnd)).raise()
            }
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
        io.github.kotlinmania.serdecore.de.UnitDeserialize
            .deserialize(de)

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
                (parser.error(ErrorCode.ExpectedNumericKey)).raise()
            }
            val result = delegate(visitor).getOrThrow()
            val after = parser.peekOrNull()
            if (after != '"'.code) (parser.peekError(ErrorCode.ExpectedDoubleQuote)).raise()
            parser.eatChar() // consume closing '"'
            result
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            parser.eatChar() // consume opening '"'
            val peek = parser.nextCharOrNull()
            if (peek < 0) (parser.peekError(ErrorCode.EofWhileParsingValue)).raise()
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
                    (SerdeError.invalidType(Unexpected.Str(s), visitor)).raise()
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
fun <T> streamDeserializer(json: String, deserialize: Deserialize<T>): StreamDeserializer<T> =
    StreamDeserializer(deserialize, JsonParser(json))

// ─── Top-level deserialization functions ───────────────────────────────────────

private fun invalidUtf8SliceError(json: ByteArray): SerdeError {
    var line = 1
    var col = 1
    var i = 0
    while (i < json.size) {
        val b0 = json[i].toInt() and 0xFF
        when {
            b0 < 0x80 -> {
                if (b0 == '\n'.code) {
                    line++
                    col = 1
                } else {
                    col++
                }
                i++
            }

            b0 in 0xC2..0xDF -> {
                if (i + 1 >= json.size || (json[i + 1].toInt() and 0xC0) != 0x80) {
                    break
                }
                col += 2
                i += 2
            }

            b0 in 0xE0..0xEF -> {
                if (i + 2 >= json.size) break
                val b1 = json[i + 1].toInt() and 0xFF
                val b2 = json[i + 2].toInt() and 0xFF
                val validLead =
                    when (b0) {
                        0xE0 -> b1 in 0xA0..0xBF
                        0xED -> b1 in 0x80..0x9F
                        else -> (b1 and 0xC0) == 0x80
                    }
                if (!validLead || (b2 and 0xC0) != 0x80) break
                col += 3
                i += 3
            }

            b0 in 0xF0..0xF4 -> {
                if (i + 3 >= json.size) break
                val b1 = json[i + 1].toInt() and 0xFF
                val b2 = json[i + 2].toInt() and 0xFF
                val b3 = json[i + 3].toInt() and 0xFF
                val validLead =
                    when (b0) {
                        0xF0 -> b1 in 0x90..0xBF
                        0xF4 -> b1 in 0x80..0x8F
                        else -> (b1 and 0xC0) == 0x80
                    }
                if (!validLead || (b2 and 0xC0) != 0x80 || (b3 and 0xC0) != 0x80) break
                col += 4
                i += 4
            }

            else -> {
                break
            }
        }
    }
    return SerdeError.custom(JsonError.syntax(ErrorCode.InvalidUnicodeCodePoint, line, col).toString())
}

private fun decodeUtf8JsonSlice(json: ByteArray): SerdeResult<String> =
    try {
        SerdeResult.success(json.decodeToString(throwOnInvalidSequence = true))
    } catch (_: CharacterCodingException) {
        SerdeResult.failure(invalidUtf8SliceError(json))
    }

/**
 * Deserialize an instance of type [T] from a string of JSON text using the given [Deserialize].
 *
 * Verifies that the entire input is consumed (no trailing characters).
 */
fun <T> deserializeFromStr(json: String, deserialize: Deserialize<T>): SerdeResult<T> =
    serdeCatching {
        val parser = JsonParser(json)
        val de = JsonDeserializer(parser)
        val value = deserialize.deserialize(de).getOrThrow()
        parser.end().getOrThrow()
        value
    }

/**
 * Deserialize an instance of type [T] from a byte array of JSON text using the given [Deserialize].
 */
fun <T> deserializeFromSlice(json: ByteArray, deserialize: Deserialize<T>): SerdeResult<T> =
    decodeUtf8JsonSlice(json).flatMap { text -> deserializeFromStr(text, deserialize) }

/**
 * Deserialize an instance of type [T] from an [IoReader] byte stream using the given [Deserialize].
 */
internal fun <R : IoReader, T> fromReader(rdr: R, deserialize: Deserialize<T>): SerdeResult<T> {
    val ioRead = IoRead.new(rdr)
    val buffer = ArrayList<Byte>()
    while (true) {
        val next = ioRead.next().getOrElse { return SerdeResult.failure(it) } ?: break
        buffer.add(next)
    }
    return deserializeFromSlice(buffer.toByteArray(), deserialize)
}

/**
 * Parse a JSON string into a [Value].
 */
fun fromStr(json: String): JsonValueOutcome =
    JsonValueOutcome.fromResult(deserializeFromStr(json, ValueDeserialize))

/**
 * Parse a JSON string into a type [T] using the given [Deserialize] implementation.
 */
fun <T> fromStr(json: String, deserialize: Deserialize<T>): SerdeResult<T> =
    deserializeFromStr(json, deserialize)

/**
 * Parse a JSON byte array into a [Value].
 */
fun fromSlice(json: ByteArray): JsonValueOutcome =
    JsonValueOutcome.fromResult(fromSlice(json, ValueDeserialize))

/**
 * Parse a JSON byte array into a type [T] using the given [Deserialize] implementation.
 */
fun <T> fromSlice(json: ByteArray, deserialize: Deserialize<T>): SerdeResult<T> =
    deserializeFromSlice(json, deserialize)
