// port-lint: source src/ser.rs
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
import io.github.kotlin.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * A serializer that writes JSON to an [IoWrite].
 */
class JsonSerializer(
    private val writer: IoWrite,
    private val pretty: Boolean = false,
    private val indent: Int = 0,
) : Serializer<Unit> {
    private fun writeString(s: String) {
        writer.writeAll(quoteByte)
        escapeString(s)
        writer.writeAll(quoteByte)
    }

    private fun escapeString(s: String) {
        for (ch in s) {
            when (ch) {
                '"' -> writer.writeAll(escapeQuote)
                '\\' -> writer.writeAll(escapeBackslash)
                '\n' -> writer.writeAll(escapeNewline)
                '\r' -> writer.writeAll(escapeCarriageReturn)
                '\t' -> writer.writeAll(escapeTab)
                '\b' -> writer.writeAll(escapeBackspace)
                '\u000C' -> writer.writeAll(escapeFormFeed)
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

    private fun writeIndent() {
        if (pretty) {
            writer.writeAll(newline)
            repeat(indent) { writer.writeAll(twoSpaces) }
        }
    }

    private fun beforeEntry() {
        if (pretty) writer.writeAll(space)
    }

    override fun serializeBool(v: Boolean): SerdeResult<Unit> = serdeCatching {
        writer.writeAll(if (v) trueBytes else falseBytes)
    }

    override fun serializeI8(v: Byte): SerdeResult<Unit> = serializeI64(v.toLong())
    override fun serializeI16(v: Short): SerdeResult<Unit> = serializeI64(v.toLong())
    override fun serializeI32(v: Int): SerdeResult<Unit> = serializeI64(v.toLong())

    override fun serializeI64(v: Long): SerdeResult<Unit> = serdeCatching {
        writer.writeAll(v.toString().encodeToByteArray())
    }

    override fun serializeU8(v: UByte): SerdeResult<Unit> = serializeU64(v.toULong())
    override fun serializeU16(v: UShort): SerdeResult<Unit> = serializeU64(v.toULong())
    override fun serializeU32(v: UInt): SerdeResult<Unit> = serializeU64(v.toULong())

    override fun serializeU64(v: ULong): SerdeResult<Unit> = serdeCatching {
        writer.writeAll(v.toString().encodeToByteArray())
    }

    override fun serializeF32(v: Float): SerdeResult<Unit> = serializeF64(v.toDouble())

    override fun serializeF64(v: Double): SerdeResult<Unit> = serdeCatching {
        if (v.isFinite()) {
            writer.writeAll(formatFinite(v).encodeToByteArray())
        } else {
            SerdeResult.failure(SerdeError.custom("cannot serialize non-finite float"))
        }
    }

    override fun serializeChar(v: Char): SerdeResult<Unit> = serdeCatching {
        writeString(v.toString())
    }

    override fun serializeStr(v: String): SerdeResult<Unit> = serdeCatching {
        writeString(v)
    }

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = serdeCatching {
        // Serialize as array of u8
        val seq = serializeSeq(v.size).getOrThrow()
        for (b in v) {
            seq.serializeElement(b).getOrThrow()
        }
        seq.end()
    }

    override fun serializeNone(): SerdeResult<Unit> = serdeCatching {
        writer.writeAll(nullBytes)
    }

    override fun <T> serializeSome(value: T): SerdeResult<Unit>
        where T : Serialize = value.serialize(this)

    override fun serializeUnit(): SerdeResult<Unit> = serdeCatching {
        writer.writeAll(nullBytes)
    }

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = serializeUnit()

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> = serdeCatching {
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
        where T : Serialize = serdeCatching {
            // Serialize as { "variant": value }
            val s = serializeStruct(name, 1).getOrThrow()
            s.serializeField(variant, value).getOrThrow()
            s.end()
        }

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> = serdeCatching {
        writer.writeAll(openBracket)
        JsonSeqSerialize(this, writer, pretty, indent)
    }

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> = serializeSeq(len)

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Unit>> = serializeSeq(len) as SerdeResult<SerializeTupleStruct<Unit>>

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> = serdeCatching {
        val s = serializeStruct(name, 1).getOrThrow()
        s.serializeField(variant, Unit).getOrThrow()
        s.end()
    } as SerdeResult<SerializeTupleVariant<Unit>>

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> = serdeCatching {
        writer.writeAll(openBrace)
        JsonMapSerialize(this, writer, pretty, indent)
    }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Unit>> = serializeMap(len) as SerdeResult<SerializeStruct<Unit>>

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> = serdeCatching {
        writer.writeAll(openBrace)
        val map = JsonMapSerialize(this, writer, pretty, indent)
        map.serializeEntry(variant, Unit).getOrThrow()
        map as SerializeStructVariant<Unit>
    }

    companion object {
        private val quoteByte = byteArrayOf('"'.code.toByte())
        private val escapeQuote = "\\\"".encodeToByteArray()
        private val escapeBackslash = "\\\\".encodeToByteArray()
        private val escapeNewline = "\\n".encodeToByteArray()
        private val escapeCarriageReturn = "\\r".encodeToByteArray()
        private val escapeTab = "\\t".encodeToByteArray()
        private val escapeBackspace = "\\b".encodeToByteArray()
        private val escapeFormFeed = "\\f".encodeToByteArray()
        private val trueBytes = "true".encodeToByteArray()
        private val falseBytes = "false".encodeToByteArray()
        private val nullBytes = "null".encodeToByteArray()
        private val openBracket = byteArrayOf('['.code.toByte())
        private val closeBracket = byteArrayOf(']'.code.toByte())
        private val openBrace = byteArrayOf('{'.code.toByte())
        private val closeBrace = byteArrayOf('}'.code.toByte())
        private val comma = ",".encodeToByteArray()
        private val colon = ":".encodeToByteArray()
        private val space = " ".encodeToByteArray()
        private val newline = "\n".encodeToByteArray()
        private val twoSpaces = "  ".encodeToByteArray()
    }
}

/**
 * Serializer for a JSON sequence (array).
 */
private class JsonSeqSerialize(
    private val parent: JsonSerializer,
    private val writer: IoWrite,
    private val pretty: Boolean,
    private val indent: Int,
) : SerializeSeq<Unit> {
    private var first = true
    private val childIndent = indent + 1

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize = serdeCatching {
            if (!first) writer.writeAll(parent.companion.comma)
            first = false
            parent.writeIndent(childIndent)
            value.serialize(JsonSerializer(writer, pretty, childIndent))
        }

    override fun end(): SerdeResult<Unit> = serdeCatching {
        if (pretty && !first) {
            parent.writeIndent(indent)
        }
        writer.writeAll(parent.companion.closeBracket)
    }
}

/**
 * Serializer for a JSON map (object).
 */
private class JsonMapSerialize(
    private val parent: JsonSerializer,
    private val writer: IoWrite,
    private val pretty: Boolean,
    private val indent: Int,
) : SerializeMap<Unit> {
    private var first = true
    private val childIndent = indent + 1

    override fun <K, V> serializeEntry(key: K, value: V): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize = serdeCatching {
            if (!first) writer.writeAll(parent.companion.comma)
            first = false
            parent.writeIndent(childIndent)
            key.serialize(JsonSerializer(writer, pretty, childIndent))
            writer.writeAll(parent.companion.colon)
            parent.beforeEntry()
            value.serialize(JsonSerializer(writer, pretty, childIndent))
        }

    override fun end(): SerdeResult<Unit> = serdeCatching {
        if (pretty && !first) {
            parent.writeIndent(indent)
        }
        writer.writeAll(parent.companion.closeBrace)
    }
}