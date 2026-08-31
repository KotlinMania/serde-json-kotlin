// port-lint: source serde_json/src/raw.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * Reference to a range of bytes encompassing a single valid JSON value.
 *
 * A [RawValue] can be used to defer parsing parts of a payload until later,
 * or to avoid parsing it at all in the case that part of the payload just
 * needs to be transferred verbatim into a different output object.
 *
 * When serializing, a value of this type will retain its original formatting
 * and will not be minified or pretty-printed.
 */
class RawValue(
    /** The raw JSON text underlying this value. */
    val json: String,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        // Serialize as a struct with a single field named after the token,
        // so the serializer emits the raw JSON verbatim.
        val s = serializer.serializeStruct(TOKEN, 1).getOrThrow()
        s.serializeField(TOKEN, RawValueSerialize(this))
        return s.end()
    }

    override fun equals(other: Any?): Boolean =
        other is RawValue && other.json == this.json

    override fun hashCode(): Int = json.hashCode()

    override fun toString(): String = json

    companion object {
        /** A constant [RawValue] with the JSON value `null`. */
        val NULL: RawValue = RawValue("null")

        /** A constant [RawValue] with the JSON value `true`. */
        val TRUE: RawValue = RawValue("true")

        /** A constant [RawValue] with the JSON value `false`. */
        val FALSE: RawValue = RawValue("false")

        /** The sentinel key used to identify raw values during (de)serialization. */
        const val TOKEN: String = "\$serde_json::private::RawValue"
    }
}

/**
 * A [Serialize] wrapper that writes the raw JSON string directly.
 */
private class RawValueSerialize(
    private val raw: RawValue,
) : Serialize {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStr(raw.json)
}

/**
 * Convert an owned [String] of JSON data to a [RawValue].
 *
 * This function validates that the string is valid JSON.
 */
fun rawValueFromString(json: String): SerdeResult<RawValue> =
    serdeCatching {
        // Validate by parsing
        fromStr(json).getOrThrow()
        RawValue(json)
    }
