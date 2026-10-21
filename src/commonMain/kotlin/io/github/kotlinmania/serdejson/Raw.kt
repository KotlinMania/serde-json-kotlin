// port-lint: source raw.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.Visitor
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
) : Serialize,
    Deserializer {
    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStruct(TOKEN, 1).flatMap { fields ->
            fields.serializeField(TOKEN, RawValueSerialize(this)).flatMap { fields.end() }
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeAny(visitor)

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeBool(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeI8(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeI16(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeI32(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeI64(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeI128(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeU8(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeU16(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeU32(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeU64(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeU128(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeF32(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeF64(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeChar(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeStr(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeString(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeBytes(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeByteBuf(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeOption(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeUnit(visitor)

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeUnitStruct(name, visitor)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeNewtypeStruct(name, visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeSeq(visitor)

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeTuple(len, visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeTupleStruct(name, len, visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeMap(visitor)

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeStruct(name, fields, visitor)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeEnum(name, variants, visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeIdentifier(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> =
        JsonDeserializer(JsonParser(json)).deserializeIgnoredAny(visitor)

    override fun equals(other: Any?): Boolean =
        other is RawValue && other.json == this.json

    override fun hashCode(): Int = json.hashCode()

    override fun toString(): String = json

    companion object : Deserialize<RawValue> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<RawValue> =
            deserializer.deserializeNewtypeStruct(TOKEN, BoxedVisitor)

        private object BoxedVisitor : Visitor<RawValue> {
            override fun expecting(): String = "any valid JSON value"

            override fun <A : MapAccess> visitMap(access: A): SerdeResult<RawValue> {
                val key = access.nextKey(RawKey).getOrElse { return SerdeResult.failure(it) }
                if (key == null) return SerdeResult.failure(SerdeError.invalidType(Unexpected.Map, this))
                return access.nextValueSeed(BoxedFromString)
            }
        }

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
fun rawValueFromString(json: String): JsonRawValueOutcome =
    JsonRawValueOutcome.fromResult(deserializeFromStr(json, RawValue))

/** Converts a serializable value into owned raw JSON text. */
fun toRawValue(value: Serialize): JsonRawValueOutcome =
    JsonRawValueOutcome.fromResult(toStr(value).toSerdeResult().map { RawValue(it) })

private object RawKey : Deserialize<Unit> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Unit> =
        deserializer.deserializeIdentifier(
            object : Visitor<Unit> {
                override fun expecting(): String = "raw value"

                override fun visitStr(v: String): SerdeResult<Unit> =
                    if (v == RawValue.TOKEN) SerdeResult.success(Unit) else SerdeResult.failure(SerdeError.custom("unexpected raw value"))
            },
        )
}

internal object BoxedFromString : DeserializeSeed<RawValue>, Visitor<RawValue> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<RawValue> = deserializer.deserializeStr(this)

    override fun expecting(): String = "raw value"

    override fun visitStr(v: String): SerdeResult<RawValue> = SerdeResult.success(RawValue(v))
}

internal class OwnedRawDeserializer(
    private var rawValue: String?,
) : MapAccess {
    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> =
        if (rawValue == null) SerdeResult.success(null) else seed.deserialize(ValueDeserializer(Value.Str(RawValue.TOKEN))).map { it }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
        val raw = rawValue ?: return SerdeResult.failure(SerdeError.custom("value is missing"))
        rawValue = null
        return seed.deserialize(ValueDeserializer(Value.Str(raw)))
    }
}
