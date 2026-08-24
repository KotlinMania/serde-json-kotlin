// port-lint: source src/lib.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.ser.Serialize

/**
 * Parse a JSON string into a [Value].
 */
fun fromStr(json: String): SerdeResult<Value> =
    JsonParser(json).parseValue()

/**
 * Parse a JSON string into a type T using the given [Deserialize] implementation.
 */
fun <T> fromStr(json: String, deserialize: Deserialize<T>): SerdeResult<T> {
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
 * Parse a JSON byte array into a type T using the given [Deserialize] implementation.
 */
fun <T> fromSlice(json: ByteArray, deserialize: Deserialize<T>): SerdeResult<T> =
    fromStr(json.decodeToString(), deserialize)

/**
 * Serialize a [Serialize] value to a JSON string.
 */
fun toStr(value: Serialize): SerdeResult<String> =
    serdeCatching {
        val writer = VecIoWrite()
        val serializer = JsonSerializer(writer)
        value.serialize(serializer).getOrThrow()
        writer.bytes.decodeToString()
    }

/**
 * Serialize a [Serialize] value to a JSON string with pretty-printing.
 */
fun toStrPretty(value: Serialize): SerdeResult<String> =
    serdeCatching {
        val writer = VecIoWrite()
        val serializer = JsonSerializer(writer, pretty = true)
        value.serialize(serializer).getOrThrow()
        writer.bytes.decodeToString()
    }

/**
 * Serialize a [Serialize] value to a byte array.
 */
fun toVec(value: Serialize): SerdeResult<ByteArray> =
    serdeCatching {
        val writer = VecIoWrite()
        val serializer = JsonSerializer(writer)
        value.serialize(serializer).getOrThrow()
        writer.bytes
    }

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
internal fun toJsonString(writer: IoWrite, value: Value, pretty: Boolean = false) {
    when (value) {
        is Value.Null -> writer.writeAll("null".encodeToByteArray())
        is Value.Bool -> writer.writeAll(if (value.value) "true".encodeToByteArray() else "false".encodeToByteArray())
        is Value.Number -> writer.writeAll(value.value.toString().encodeToByteArray())
        is Value.Str -> {
            writer.writeAll(byteArrayOf('"'.code.toByte()))
            escapeJsonString(writer, value.value)
            writer.writeAll(byteArrayOf('"'.code.toByte()))
        }
        is Value.Array -> {
            writer.writeAll(byteArrayOf('['.code.toByte()))
            for ((i, v) in value.value.withIndex()) {
                if (i > 0) writer.writeAll(",".encodeToByteArray())
                if (pretty) writer.writeAll("\n".encodeToByteArray())
                if (pretty) repeat(1) { writer.writeAll("  ".encodeToByteArray()) }
                toJsonString(writer, v, pretty)
            }
            if (pretty && value.value.isNotEmpty()) writer.writeAll("\n".encodeToByteArray())
            writer.writeAll(byteArrayOf(']'.code.toByte()))
        }
        is Value.Object -> {
            writer.writeAll(byteArrayOf('{'.code.toByte()))
            var first = true
            for ((k, v) in value.value) {
                if (!first) writer.writeAll(",".encodeToByteArray())
                first = false
                if (pretty) writer.writeAll("\n".encodeToByteArray())
                if (pretty) repeat(1) { writer.writeAll("  ".encodeToByteArray()) }
                writer.writeAll(byteArrayOf('"'.code.toByte()))
                escapeJsonString(writer, k)
                writer.writeAll(byteArrayOf('"'.code.toByte()))
                writer.writeAll(if (pretty) ": ".encodeToByteArray() else ":".encodeToByteArray())
                toJsonString(writer, v, pretty)
            }
            if (pretty && value.value.isNotEmpty()) writer.writeAll("\n".encodeToByteArray())
            writer.writeAll(byteArrayOf('}'.code.toByte()))
        }
    }
}

private fun escapeJsonString(writer: IoWrite, s: String) {
    for (ch in s) {
        when (ch) {
            '"' -> writer.writeAll("\\\"".encodeToByteArray())
            '\\' -> writer.writeAll("\\\\".encodeToByteArray())
            '\n' -> writer.writeAll("\\n".encodeToByteArray())
            '\r' -> writer.writeAll("\\r".encodeToByteArray())
            '\t' -> writer.writeAll("\\t".encodeToByteArray())
            '\b' -> writer.writeAll("\\b".encodeToByteArray())
            '\u000C' -> writer.writeAll("\\f".encodeToByteArray())
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
 * Build a [Value] from a JSON string (convenience function).
 */
fun json(jsonStr: String): Value = fromStr(jsonStr).getOrThrow()
