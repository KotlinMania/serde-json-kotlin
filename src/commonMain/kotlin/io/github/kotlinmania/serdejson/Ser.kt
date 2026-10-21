// port-lint: source ser.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import io.github.kotlinmania.serdecore.ser.SerializeSeq
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTuple
import io.github.kotlinmania.serdecore.ser.SerializeTupleStruct
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.io.IoResult
import io.github.kotlinmania.serdejson.io.IoWrite
import io.github.kotlinmania.serdejson.io.VecIoWrite

private val QUOTE = byteArrayOf('"'.code.toByte())
private val ESC_QUOTE = "\\\"".encodeToByteArray()
private val ESC_BACKSLASH = "\\\\".encodeToByteArray()
private val ESC_NEWLINE = "\\n".encodeToByteArray()
private val ESC_CR = "\\r".encodeToByteArray()
private val ESC_TAB = "\\t".encodeToByteArray()
private val ESC_BS = "\\b".encodeToByteArray()
private val ESC_FF = "\\f".encodeToByteArray()
private val TRUE_BYTES = "true".encodeToByteArray()
private val FALSE_BYTES = "false".encodeToByteArray()
private val NULL_BYTES = "null".encodeToByteArray()
private val OPEN_BRACKET = byteArrayOf('['.code.toByte())
private val CLOSE_BRACKET = byteArrayOf(']'.code.toByte())
private val OPEN_BRACE = byteArrayOf('{'.code.toByte())
private val CLOSE_BRACE = byteArrayOf('}'.code.toByte())
private val COMMA = ",".encodeToByteArray()
private val COLON = ":".encodeToByteArray()
private val SPACE = " ".encodeToByteArray()
private val NEWLINE = "\n".encodeToByteArray()
private val TWO_SPACES = "  ".encodeToByteArray()

private class CheckedIoWrite(
    private val output: IoWrite,
) : IoWrite {
    private fun checked(result: IoResult<Unit>): IoResult<Unit> =
        when (result) {
            is IoResult.Ok -> result
            is IoResult.Err -> SerdeError.custom(JsonError.io(result.error.message).toString()).raise()
        }

    override fun write(buf: ByteArray): IoResult<Unit> = checked(output.write(buf))

    override fun writeAll(buf: ByteArray): IoResult<Unit> = checked(output.writeAll(buf))

    override fun flush(): IoResult<Unit> = checked(output.flush())
}

private fun escapeStringToWriter(writer: IoWrite, s: String) {
    for (ch in s) {
        when (ch) {
            '"' -> writer.writeAll(ESC_QUOTE)
            '\\' -> writer.writeAll(ESC_BACKSLASH)
            '\n' -> writer.writeAll(ESC_NEWLINE)
            '\r' -> writer.writeAll(ESC_CR)
            '\t' -> writer.writeAll(ESC_TAB)
            '\b' -> writer.writeAll(ESC_BS)
            '\u000C' -> writer.writeAll(ESC_FF)
            else -> {
                if (ch.code < 0x20) {
                    writer.writeAll("\\u${ch.code.toString(16).padStart(4, '0')}".encodeToByteArray())
                } else {
                    writer.writeAll(ch.toString().encodeToByteArray())
                }
            }
        }
    }
}

/**
 * A serializer that writes JSON to an [IoWrite].
 */
internal class JsonSerializer(
    writer: IoWrite,
    private val pretty: Boolean = false,
    private val indent: Int = 0,
) : Serializer<Unit> {
    private val writer: IoWrite = if (writer is CheckedIoWrite) writer else CheckedIoWrite(writer)

    private fun writeString(s: String) {
        writer.writeAll(QUOTE)
        escapeStringToWriter(writer, s)
        writer.writeAll(QUOTE)
    }

    override fun serializeBool(v: Boolean): SerdeResult<Unit> =
        serdeCatching {
            writer.writeAll(if (v) TRUE_BYTES else FALSE_BYTES)
        }

    override fun serializeI8(v: Byte): SerdeResult<Unit> = serializeI64(v.toLong())

    override fun serializeI16(v: Short): SerdeResult<Unit> = serializeI64(v.toLong())

    override fun serializeI32(v: Int): SerdeResult<Unit> = serializeI64(v.toLong())

    override fun serializeI64(v: Long): SerdeResult<Unit> =
        serdeCatching {
            writer.writeAll(v.toString().encodeToByteArray())
        }

    override fun serializeU8(v: UByte): SerdeResult<Unit> = serializeU64(v.toULong())

    override fun serializeU16(v: UShort): SerdeResult<Unit> = serializeU64(v.toULong())

    override fun serializeU32(v: UInt): SerdeResult<Unit> = serializeU64(v.toULong())

    override fun serializeU64(v: ULong): SerdeResult<Unit> =
        serdeCatching {
            writer.writeAll(v.toString().encodeToByteArray())
        }

    override fun serializeF32(v: Float): SerdeResult<Unit> =
        serdeCatching {
            writer.writeAll(if (v.isFinite()) v.toString().encodeToByteArray() else NULL_BYTES)
        }

    override fun serializeF64(v: Double): SerdeResult<Unit> =
        serdeCatching {
            if (v.isFinite()) {
                writer.writeAll(formatFinite(v).encodeToByteArray())
            } else {
                writer.writeAll(NULL_BYTES)
            }
        }

    override fun serializeChar(v: Char): SerdeResult<Unit> =
        serdeCatching {
            writeString(v.toString())
        }

    override fun serializeStr(v: String): SerdeResult<Unit> =
        serdeCatching {
            writeString(v)
        }

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> =
        serdeCatching {
            val seq = serializeSeq(v.size).getOrThrow()
            for (b in v) {
                seq.serializeElement(JsonNumber.fromU64(b.toUByte().toULong())).getOrThrow()
            }
            seq.end().getOrThrow()
        }

    override fun serializeNone(): SerdeResult<Unit> =
        serdeCatching {
            writer.writeAll(NULL_BYTES)
        }

    override fun <T> serializeSome(value: T): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeUnit(): SerdeResult<Unit> =
        serdeCatching {
            writer.writeAll(NULL_BYTES)
        }

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = serializeUnit()

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> =
        serdeCatching {
            writeString(variant)
        }

    override fun <T> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            val s = serializeStruct(name, 1).getOrThrow()
            s.serializeField(variant, value).getOrThrow()
            s.end().getOrThrow()
        }

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> =
        serdeCatching {
            writer.writeAll(OPEN_BRACKET)
            JsonSeqSerialize(writer, pretty, indent)
        }

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> =
        serdeCatching {
            writer.writeAll(OPEN_BRACKET)
            JsonSeqSerialize(writer, pretty, indent)
        }

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Unit>> =
        serdeCatching {
            writer.writeAll(OPEN_BRACKET)
            JsonSeqSerialize(writer, pretty, indent)
        }

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> =
        serdeCatching {
            writer.writeAll(OPEN_BRACE)
            writeString(variant)
            writer.writeAll(COLON)
            if (pretty) writer.writeAll(SPACE)
            writer.writeAll(OPEN_BRACKET)
            JsonSeqSerialize(writer, pretty, indent)
        }

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> =
        serdeCatching {
            writer.writeAll(OPEN_BRACE)
            JsonMapSerialize(writer, pretty, indent)
        }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Unit>> =
        if (name == RawValue.TOKEN) {
            SerdeResult.success(JsonRawStructSerialize(writer))
        } else {
            serdeCatching {
                writer.writeAll(OPEN_BRACE)
                JsonMapSerialize(writer, pretty, indent)
            }
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> =
        serdeCatching {
            writer.writeAll(OPEN_BRACE)
            writeString(variant)
            writer.writeAll(COLON)
            if (pretty) writer.writeAll(SPACE)
            val inner = JsonMapSerialize(writer, pretty, indent + 1)
            writer.writeAll(OPEN_BRACE)
            JsonStructVariantSerialize(inner, writer, pretty, indent)
        }
}

/**
 * Serializer for a JSON sequence (array).
 */
private class JsonSeqSerialize(
    private val writer: IoWrite,
    private val pretty: Boolean,
    private val indent: Int,
) : SerializeSeq<Unit>,
    SerializeTuple<Unit>,
    SerializeTupleStruct<Unit>,
    SerializeTupleVariant<Unit> {
    private var first = true
    private val childIndent = indent + 1

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            if (!first) writer.writeAll(COMMA)
            first = false
            if (pretty) {
                writer.writeAll(NEWLINE)
                repeat(childIndent) { writer.writeAll(TWO_SPACES) }
            }
            value.serialize(JsonSerializer(writer, pretty, childIndent)).getOrThrow()
        }

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize = serializeElement(value)

    override fun end(): SerdeResult<Unit> =
        serdeCatching {
            if (pretty && !first) {
                writer.writeAll(NEWLINE)
                repeat(indent) { writer.writeAll(TWO_SPACES) }
            }
            writer.writeAll(CLOSE_BRACKET)
        }
}

/**
 * Serializer for a JSON map (object).
 */
private class JsonMapSerialize(
    private val writer: IoWrite,
    private val pretty: Boolean,
    private val indent: Int,
) : SerializeMap<Unit>,
    SerializeStruct<Unit>,
    SerializeStructVariant<Unit> {
    private var first = true
    private val childIndent = indent + 1

    private fun writeKey(key: String) {
        if (!first) writer.writeAll(COMMA)
        first = false
        if (pretty) {
            writer.writeAll(NEWLINE)
            repeat(childIndent) { writer.writeAll(TWO_SPACES) }
        }
        writer.writeAll(QUOTE)
        escapeStringToWriter(writer, key)
        writer.writeAll(QUOTE)
        writer.writeAll(COLON)
        if (pretty) writer.writeAll(SPACE)
    }

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            if (!first) writer.writeAll(COMMA)
            first = false
            if (pretty) {
                writer.writeAll(NEWLINE)
                repeat(childIndent) { writer.writeAll(TWO_SPACES) }
            }
            key.serialize(JsonMapKeySerializer(writer)).getOrThrow()
        }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            writer.writeAll(COLON)
            if (pretty) writer.writeAll(SPACE)
            value.serialize(JsonSerializer(writer, pretty, childIndent)).getOrThrow()
        }

    override fun <K, V> serializeEntry(key: K, value: V): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize =
        serdeCatching {
            if (!first) writer.writeAll(COMMA)
            first = false
            if (pretty) {
                writer.writeAll(NEWLINE)
                repeat(childIndent) { writer.writeAll(TWO_SPACES) }
            }
            key.serialize(JsonMapKeySerializer(writer)).getOrThrow()
            writer.writeAll(COLON)
            if (pretty) writer.writeAll(SPACE)
            value.serialize(JsonSerializer(writer, pretty, childIndent)).getOrThrow()
        }

    override fun <T> serializeField(key: String, value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            writeKey(key)
            value.serialize(JsonSerializer(writer, pretty, childIndent)).getOrThrow()
        }

    override fun skipField(key: String): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun end(): SerdeResult<Unit> =
        serdeCatching {
            if (pretty && !first) {
                writer.writeAll(NEWLINE)
                repeat(indent) { writer.writeAll(TWO_SPACES) }
            }
            writer.writeAll(CLOSE_BRACE)
        }
}

private class JsonRawStructSerialize(
    private val writer: IoWrite,
) : SerializeStruct<Unit> {
    private var written = false

    override fun <T> serializeField(key: String, value: T): SerdeResult<Unit>
        where T : Serialize =
        if (key != RawValue.TOKEN || written) {
            SerdeResult.failure(invalidRawValue())
        } else {
            serdeCatching {
                value
                    .serialize(
                        object : RawValueEmitter() {
                            override fun serializeStr(v: String): SerdeResult<Value> =
                                fromStr(v).toSerdeResult().onSuccess { writer.writeAll(v.encodeToByteArray()) }
                        },
                    ).getOrThrow()
                written = true
            }
        }

    override fun end(): SerdeResult<Unit> =
        if (written) SerdeResult.success(Unit) else SerdeResult.failure(invalidRawValue())
}

private class JsonMapKeySerializer(
    private val writer: IoWrite,
) : Serializer<Unit> {
    private fun quoted(value: String): SerdeResult<Unit> = JsonSerializer(writer).serializeStr(value)

    private fun <T> invalidKey(): SerdeResult<T> =
        SerdeResult.failure(SerdeError.custom(ErrorCode.KeyMustBeAString.toString()))

    private fun invalidFloat(): SerdeResult<Unit> =
        SerdeResult.failure(SerdeError.custom(ErrorCode.FloatKeyMustBeFinite.toString()))

    override fun serializeBool(v: Boolean): SerdeResult<Unit> = quoted(v.toString())

    override fun serializeI8(v: Byte): SerdeResult<Unit> = serializeI64(v.toLong())

    override fun serializeI16(v: Short): SerdeResult<Unit> = serializeI64(v.toLong())

    override fun serializeI32(v: Int): SerdeResult<Unit> = serializeI64(v.toLong())

    override fun serializeI64(v: Long): SerdeResult<Unit> = quoted(v.toString())

    private fun wideInteger(value: String, signed: Boolean): SerdeResult<Unit> {
        val negative = value.startsWith('-')
        val digits = if (negative) value.drop(1) else value
        if (digits.isEmpty() || digits.any { it !in '0'..'9' } || (negative && !signed)) {
            return SerdeResult.failure(SerdeError.custom(ErrorCode.InvalidNumber.toString()))
        }
        val magnitude = digits.trimStart('0').ifEmpty { "0" }
        val maximum =
            if (!signed) {
                "340282366920938463463374607431768211455"
            } else if (negative) {
                "170141183460469231731687303715884105728"
            } else {
                "170141183460469231731687303715884105727"
            }
        if (magnitude.length > maximum.length || (magnitude.length == maximum.length && magnitude > maximum)) {
            return SerdeResult.failure(SerdeError.custom(ErrorCode.NumberOutOfRange.toString()))
        }
        return quoted(if (negative && magnitude != "0") "-$magnitude" else magnitude)
    }

    override fun serializeI128(value: String): SerdeResult<Unit> = wideInteger(value, signed = true)

    override fun serializeU8(v: UByte): SerdeResult<Unit> = serializeU64(v.toULong())

    override fun serializeU16(v: UShort): SerdeResult<Unit> = serializeU64(v.toULong())

    override fun serializeU32(v: UInt): SerdeResult<Unit> = serializeU64(v.toULong())

    override fun serializeU64(v: ULong): SerdeResult<Unit> = quoted(v.toString())

    override fun serializeU128(value: String): SerdeResult<Unit> = wideInteger(value, signed = false)

    override fun serializeF32(v: Float): SerdeResult<Unit> =
        if (v.isFinite()) quoted(v.toString()) else invalidFloat()

    override fun serializeF64(v: Double): SerdeResult<Unit> =
        if (v.isFinite()) quoted(formatFinite(v)) else invalidFloat()

    override fun serializeChar(v: Char): SerdeResult<Unit> = quoted(v.toString())

    override fun serializeStr(v: String): SerdeResult<Unit> = quoted(v)

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = invalidKey()

    override fun serializeNone(): SerdeResult<Unit> = invalidKey()

    override fun <T> serializeSome(value: T): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeUnit(): SerdeResult<Unit> = invalidKey()

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = invalidKey()

    override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): SerdeResult<Unit> = quoted(variant)

    override fun <T> serializeNewtypeStruct(name: String, value: T): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): SerdeResult<Unit>
        where T : Serialize = invalidKey()

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> = invalidKey()

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> = invalidKey()

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Unit>> = invalidKey()

    override fun serializeTupleVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeTupleVariant<Unit>> =
        invalidKey()

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> = invalidKey()

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Unit>> = invalidKey()

    override fun serializeStructVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeStructVariant<Unit>> =
        invalidKey()
}

/**
 * Wrapper for struct variant serialization. Delegates field serialization to the inner
 * [JsonMapSerialize], and writes an extra closing brace on [end] to close the outer object.
 */
private class JsonStructVariantSerialize(
    private val inner: JsonMapSerialize,
    private val writer: IoWrite,
    private val pretty: Boolean,
    private val indent: Int,
) : SerializeStructVariant<Unit> {
    override fun <T> serializeField(key: String, value: T): SerdeResult<Unit>
        where T : Serialize = inner.serializeField(key, value)

    override fun skipField(key: String): SerdeResult<Unit> = inner.skipField(key)

    override fun end(): SerdeResult<Unit> =
        serdeCatching {
            inner.end().getOrThrow()
            if (pretty) {
                writer.writeAll(NEWLINE)
                repeat(indent) { writer.writeAll(TWO_SPACES) }
            }
            writer.writeAll(CLOSE_BRACE)
        }
}

/**
 * Serialize a [Serialize] value to a JSON string.
 */
fun toStr(value: Serialize): JsonStringOutcome =
    JsonStringOutcome.fromResult(
        serdeCatching {
            val writer = VecIoWrite()
            val serializer = JsonSerializer(writer)
            value.serialize(serializer).getOrThrow()
            writer.bytes.decodeToString()
        },
    )

/**
 * Serialize a [Serialize] value to a JSON string with pretty-printing.
 */
fun toStrPretty(value: Serialize): JsonStringOutcome =
    JsonStringOutcome.fromResult(
        serdeCatching {
            val writer = VecIoWrite()
            val serializer = JsonSerializer(writer, pretty = true)
            value.serialize(serializer).getOrThrow()
            writer.bytes.decodeToString()
        },
    )

/**
 * Serialize a [Serialize] value to a byte array.
 */
fun toVec(value: Serialize): JsonBytesOutcome =
    JsonBytesOutcome.fromResult(
        serdeCatching {
            val writer = VecIoWrite()
            val serializer = JsonSerializer(writer)
            value.serialize(serializer).getOrThrow()
            writer.bytes
        },
    )

/**
 * Serialize a [Value] to a JSON string.
 */
fun valueToStr(value: Value): String {
    val writer = VecIoWrite()
    toJsonString(writer, value, pretty = false)
    return writer.bytes.decodeToString()
}

/**
 * Serialize a [Value] to a pretty JSON string.
 */
fun valueToStrPretty(value: Value): String {
    val writer = VecIoWrite()
    toJsonString(writer, value, pretty = true)
    return writer.bytes.decodeToString()
}

/**
 * Serialize a [Value] directly to an [IoWrite].
 */
internal fun toJsonString(output: IoWrite, value: Value, pretty: Boolean = false) {
    val writer: IoWrite = if (output is CheckedIoWrite) output else CheckedIoWrite(output)
    when (value) {
        is Value.Null -> writer.writeAll(NULL_BYTES)
        is Value.Bool -> writer.writeAll(if (value.value) TRUE_BYTES else FALSE_BYTES)
        is Value.Number -> writer.writeAll(value.value.toString().encodeToByteArray())
        is Value.Str -> {
            writer.writeAll(QUOTE)
            escapeStringToWriter(writer, value.value)
            writer.writeAll(QUOTE)
        }
        is Value.Array -> {
            writer.writeAll(OPEN_BRACKET)
            for ((i, v) in value.value.withIndex()) {
                if (i > 0) writer.writeAll(COMMA)
                if (pretty) writer.writeAll(NEWLINE)
                if (pretty) repeat(1) { writer.writeAll(TWO_SPACES) }
                toJsonString(writer, v, pretty)
            }
            if (pretty && value.value.isNotEmpty()) writer.writeAll(NEWLINE)
            writer.writeAll(CLOSE_BRACKET)
        }
        is Value.Object -> {
            writer.writeAll(OPEN_BRACE)
            var first = true
            for ((k, v) in value.value) {
                if (!first) writer.writeAll(COMMA)
                first = false
                if (pretty) writer.writeAll(NEWLINE)
                if (pretty) repeat(1) { writer.writeAll(TWO_SPACES) }
                writer.writeAll(QUOTE)
                escapeStringToWriter(writer, k)
                writer.writeAll(QUOTE)
                writer.writeAll(if (pretty) ": ".encodeToByteArray() else COLON)
                toJsonString(writer, v, pretty)
            }
            if (pretty && value.value.isNotEmpty()) writer.writeAll(NEWLINE)
            writer.writeAll(CLOSE_BRACE)
        }
    }
}
