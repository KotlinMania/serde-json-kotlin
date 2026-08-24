// port-lint: source src/ser.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
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
    private val writer: IoWrite,
    private val pretty: Boolean = false,
    private val indent: Int = 0,
) : Serializer<Unit> {
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

    override fun serializeF32(v: Float): SerdeResult<Unit> = serializeF64(v.toDouble())

    override fun serializeF64(v: Double): SerdeResult<Unit> =
        serdeCatching {
            if (v.isFinite()) {
                writer.writeAll(formatFinite(v).encodeToByteArray())
            } else {
                throw SerdeException(SerdeError.custom("cannot serialize non-finite float"))
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
                seq.serializeElement(JsonNumber.fromI64(b.toLong())).getOrThrow()
            }
            seq.end()
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
            s.end()
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
        serdeCatching {
            writer.writeAll(OPEN_BRACE)
            JsonMapSerialize(writer, pretty, indent)
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
            value.serialize(JsonSerializer(writer, pretty, childIndent))
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
            key.serialize(JsonSerializer(writer, pretty, childIndent))
        }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            writer.writeAll(COLON)
            if (pretty) writer.writeAll(SPACE)
            value.serialize(JsonSerializer(writer, pretty, childIndent))
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
            key.serialize(JsonSerializer(writer, pretty, childIndent))
            writer.writeAll(COLON)
            if (pretty) writer.writeAll(SPACE)
            value.serialize(JsonSerializer(writer, pretty, childIndent))
        }

    override fun <T> serializeField(key: String, value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            writeKey(key)
            value.serialize(JsonSerializer(writer, pretty, childIndent))
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
