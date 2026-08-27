// port-lint: source de.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.Visitor

/**
 * A JSON parser that reads from a string.
 */
class JsonParser(
    private val input: String,
) {
    private var pos: Int = 0
    private var line: Int = 1
    private var col: Int = 0

    /** Returns the current position. */
    fun position(): Pair<Int, Int> = Pair(line, col)

    /** Returns the byte offset. */
    fun byteOffset(): Int = pos

    /** Peeks at the next character without consuming it. Returns -1 at end of input. */
    fun peek(): Int {
        skipWhitespace()
        return if (pos < input.length) input[pos].code else -1
    }

    /** Returns the next character, or -1 at end of input. */
    fun next(): Int {
        skipWhitespace()
        if (pos >= input.length) return -1
        val c = input[pos].code
        pos++
        if (c == '\n'.code) {
            line++
            col = 0
        } else {
            col++
        }
        return c
    }

    /** Consumes whitespace. */
    private fun skipWhitespace() {
        while (pos < input.length) {
            when (input[pos]) {
                ' ', '\t', '\n', '\r' -> pos++
                else -> return
            }
        }
    }

    /** Parses a JSON value. */
    fun parseValue(): SerdeResult<Value> =
        serdeCatching {
            skipWhitespace()
            if (pos >= input.length) {
                throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.EofWhileParsingValue, line, col).toString()))
            }
            when (val c = input[pos]) {
                'n' -> parseNull()
                't' -> parseTrue()
                'f' -> parseFalse()
                '"' -> parseString()
                '[' -> parseArray()
                '{' -> parseObject()
                else -> {
                    if (c == '-' || c.isDigit()) {
                        parseNumber()
                    } else {
                        throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedSomeValue, line, col).toString()))
                    }
                }
            }
        }

    private fun parseNull(): Value {
        expect("null")
        return Value.Null
    }

    private fun parseTrue(): Value {
        expect("true")
        return Value.Bool(true)
    }

    private fun parseFalse(): Value {
        expect("false")
        return Value.Bool(false)
    }

    private fun parseString(): Value {
        expectChar('"')
        val sb = StringBuilder()
        while (pos < input.length) {
            val c = input[pos++]
            when {
                c == '"' -> return Value.Str(sb.toString())
                c == '\\' -> {
                    if (pos >= input.length) throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.EofWhileParsingString, line, col).toString()))
                    val esc = input[pos++]
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
                            val hex = input.substring(pos, minOf(pos + 4, input.length))
                            pos += 4
                            val codePoint = hex.toInt(16)
                            sb.append(Char(codePoint))
                        }
                        else -> throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.InvalidEscape, line, col).toString()))
                    }
                }
                c.code < 0x20 -> throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ControlCharacterWhileParsingString, line, col).toString()))
                else -> sb.append(c)
            }
        }
        throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.EofWhileParsingString, line, col).toString()))
    }

    private fun parseNumber(): Value {
        val start = pos
        if (pos < input.length && input[pos] == '-') pos++
        while (pos < input.length && input[pos].isDigit()) pos++
        var isFloat = false
        if (pos < input.length && input[pos] == '.') {
            isFloat = true
            pos++
            while (pos < input.length && input[pos].isDigit()) pos++
        }
        if (pos < input.length && (input[pos] == 'e' || input[pos] == 'E')) {
            isFloat = true
            pos++
            if (pos < input.length && (input[pos] == '+' || input[pos] == '-')) pos++
            while (pos < input.length && input[pos].isDigit()) pos++
        }
        val numStr = input.substring(start, pos)
        val number =
            if (isFloat) {
                JsonNumber.fromF64(numStr.toDouble())
            } else {
                val longVal = numStr.toLongOrNull()
                if (longVal != null) {
                    if (longVal >= 0) {
                        JsonNumber.fromU64(longVal.toULong())
                    } else {
                        JsonNumber.fromI64(longVal)
                    }
                } else {
                    JsonNumber.fromF64(numStr.toDouble())
                }
            }
        return number?.let { Value.Number(it) } ?: Value.Str(numStr)
    }

    private fun parseArray(): Value {
        expectChar('[')
        val list = mutableListOf<Value>()
        skipWhitespace()
        if (pos < input.length && input[pos] == ']') {
            pos++
            return Value.Array(list)
        }
        while (true) {
            val value = parseValue().getOrThrow()
            list.add(value)
            skipWhitespace()
            if (pos >= input.length) throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.EofWhileParsingList, line, col).toString()))
            when (input[pos]) {
                ',' -> {
                    pos++
                    skipWhitespace()
                }
                ']' -> {
                    pos++
                    return Value.Array(list)
                }
                else -> throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedListCommaOrEnd, line, col).toString()))
            }
        }
    }

    private fun parseObject(): Value {
        expectChar('{')
        val map = ValueMap()
        skipWhitespace()
        if (pos < input.length && input[pos] == '}') {
            pos++
            return Value.Object(map)
        }
        while (true) {
            skipWhitespace()
            if (pos >= input.length || input[pos] != '"') {
                throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.KeyMustBeAString, line, col).toString()))
            }
            val key = (parseString() as Value.Str).value
            skipWhitespace()
            if (pos >= input.length || input[pos] != ':') {
                throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedColon, line, col).toString()))
            }
            pos++
            val value = parseValue().getOrThrow()
            map.insert(key, value)
            skipWhitespace()
            if (pos >= input.length) throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.EofWhileParsingObject, line, col).toString()))
            when (input[pos]) {
                ',' -> {
                    pos++
                    skipWhitespace()
                }
                '}' -> {
                    pos++
                    return Value.Object(map)
                }
                else -> throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedObjectCommaOrEnd, line, col).toString()))
            }
        }
    }

    private fun expect(s: String) {
        for (c in s) {
            if (pos >= input.length || input[pos] != c) {
                throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedSomeIdent, line, col).toString()))
            }
            pos++
        }
    }

    private fun expectChar(c: Char) {
        if (pos >= input.length || input[pos] != c) {
            throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedDoubleQuote, line, col).toString()))
        }
        pos++
    }

    /** Returns true if there is more input to consume. */
    fun hasMore(): Boolean {
        skipWhitespace()
        return pos < input.length
    }
}

/**
 * A JSON deserializer that wraps a [JsonParser] and implements the serde [Deserializer] interface.
 */
class JsonDeserializer(
    private val parser: JsonParser,
) : Deserializer {
    override fun isHumanReadable(): Boolean = true

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val c = parser.peek()
            when {
                c < 0 -> throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.EofWhileParsingValue, parser.position().first, parser.position().second).toString()))
                c == 'n'.code -> {
                    parser.next()
                    visitor.visitUnit()
                }
                c == 't'.code -> {
                    parser.next()
                    expectIdent("true")
                    visitor.visitBool(true)
                }
                c == 'f'.code -> {
                    parser.next()
                    expectIdent("false")
                    visitor.visitBool(false)
                }
                c == '"'.code -> visitor.visitStr(parseStringValue())
                c == '['.code -> visitor.visitSeq(JsonSeqAccess(parser))
                c == '{'.code -> visitor.visitMap(JsonMapAccess(parser))
                else -> {
                    val num = parseNumberValue()
                    when (num) {
                        is NumberValue.F64 -> visitor.visitF64(num.value)
                        is NumberValue.U64 -> visitor.visitU64(num.value)
                        is NumberValue.I64 -> visitor.visitI64(num.value)
                    }
                }
            }
        }.flatMap { it }

    private fun expectIdent(s: String) {
        for (c in s) {
            parser.next()
        }
    }

    private fun parseStringValue(): String {
        val v = parser.parseValue().getOrThrow()
        return (v as Value.Str).value
    }

    private fun parseNumberValue(): NumberValue {
        val v = parser.parseValue().getOrThrow()
        val n: JsonNumber = (v as Value.Number).value
        return when {
            n.isI64() && (n.asI64() ?: 0L) >= 0 -> NumberValue.U64(n.asU64()!!)
            n.isI64() -> NumberValue.I64(n.asI64()!!)
            else -> NumberValue.F64(n.asF64()!!)
        }
    }

    private sealed class NumberValue {
        class F64(
            val value: Double,
        ) : NumberValue()

        class U64(
            val value: ULong,
        ) : NumberValue()

        class I64(
            val value: Long,
        ) : NumberValue()
    }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val c = parser.peek()
            when (c) {
                't'.code -> {
                    parser.next()
                    expectIdent("true")
                    true
                }
                'f'.code -> {
                    parser.next()
                    expectIdent("false")
                    false
                }
                else -> throw SerdeException(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedSomeValue, parser.position().first, parser.position().second).toString()))
            }
        }.flatMap { visitor.visitBool(it) }

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeI64(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeI64(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeI64(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val v = parser.parseValue().getOrThrow()
            val n = (v as Value.Number).value
            n.asI64() ?: n.asU64()?.toLong() ?: n.asF64()?.toLong() ?: throw SerdeException(SerdeError.custom("expected i64"))
        }.flatMap { visitor.visitI64(it) }

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeU64(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeU64(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeU64(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val v = parser.parseValue().getOrThrow()
            val n = (v as Value.Number).value
            n.asU64() ?: n.asI64()?.toULong() ?: n.asF64()?.toULong() ?: throw SerdeException(SerdeError.custom("expected u64"))
        }.flatMap { visitor.visitU64(it) }

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeF64(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val v = parser.parseValue().getOrThrow()
            val n = (v as Value.Number).value
            n.asF64() ?: n.asI64()?.toDouble() ?: n.asU64()?.toDouble() ?: throw SerdeException(SerdeError.custom("expected f64"))
        }.flatMap { visitor.visitF64(it) }

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val v = parser.parseValue().getOrThrow()
            val s = (v as Value.Str).value
            if (s.length != 1) throw SerdeException(SerdeError.custom("expected char"))
            s[0]
        }.flatMap { visitor.visitChar(it) }

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val v = parser.parseValue().getOrThrow()
            (v as Value.Str).value
        }.flatMap { visitor.visitStr(it) }

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.custom("bytes are not supported in JSON"))

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.custom("bytes are not supported in JSON"))

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val c = parser.peek()
            if (c == 'n'.code) {
                parser.next()
                expectIdent("null")
                true
            } else {
                false
            }
        }.flatMap { isNull ->
            if (isNull) visitor.visitNone() else visitor.visitSome(this)
        }

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val c = parser.peek()
            if (c != 'n'.code) throw SerdeException(SerdeError.custom("expected null"))
            parser.next()
            expectIdent("null")
        }.flatMap { visitor.visitUnit() }

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeUnit(visitor)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = visitor.visitNewtypeStruct(this)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val c = parser.peek()
            if (c != '['.code) throw SerdeException(SerdeError.custom("expected array"))
        }.flatMap { visitor.visitSeq(JsonSeqAccess(parser)) }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeSeq(visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeSeq(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            val c = parser.peek()
            if (c != '{'.code) throw SerdeException(SerdeError.custom("expected object"))
        }.flatMap { visitor.visitMap(JsonMapAccess(parser)) }

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = deserializeMap(visitor)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        SerdeResult.failure(SerdeError.custom("enum deserialization not yet implemented"))

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> {
        parser.parseValue()
        return visitor.visitUnit()
    }
}

/**
 * Sequence access for JSON arrays.
 */
private class JsonSeqAccess(
    private val parser: JsonParser,
) : SeqAccess {
    private var first = true

    override fun <T> nextElementSeed(seed: io.github.kotlinmania.serdecore.de.DeserializeSeed<T>): SerdeResult<T?> =
        serdeCatching {
            if (first) {
                parser.next() // consume '['
                first = false
                parser.peek()
                if (parser.peek() == ']'.code) {
                    parser.next()
                    return@serdeCatching null
                }
            }
            parser.peek()
            if (parser.peek() == ']'.code) {
                parser.next()
                return@serdeCatching null
            }
            val value = seed.deserialize(JsonDeserializer(parser)).getOrThrow()
            parser.peek()
            if (parser.peek() == ','.code) {
                parser.next()
                parser.peek()
            }
            value
        }
}

/**
 * Map access for JSON objects.
 */
private class JsonMapAccess(
    private val parser: JsonParser,
) : MapAccess {
    private var first = true

    override fun <K> nextKeySeed(seed: io.github.kotlinmania.serdecore.de.DeserializeSeed<K>): SerdeResult<K?> =
        serdeCatching {
            if (first) {
                parser.next() // consume '{'
                first = false
                parser.peek()
                if (parser.peek() == '}'.code) {
                    parser.next()
                    return@serdeCatching null
                }
            }
            parser.peek()
            if (parser.peek() == '}'.code) {
                parser.next()
                return@serdeCatching null
            }
            val key = seed.deserialize(JsonDeserializer(parser)).getOrThrow()
            parser.peek()
            if (parser.peek() != ':'.code) throw SerdeException(SerdeError.custom("expected colon"))
            parser.next()
            key
        }

    override fun <V> nextValueSeed(seed: io.github.kotlinmania.serdecore.de.DeserializeSeed<V>): SerdeResult<V> =
        serdeCatching {
            val value = seed.deserialize(JsonDeserializer(parser)).getOrThrow()
            parser.peek()
            if (parser.peek() == ','.code) {
                parser.next()
            }
            value
        }
}

private val <T> T?.orThrow: T
    get() = this ?: throw SerdeException(SerdeError.custom("unexpected null"))
