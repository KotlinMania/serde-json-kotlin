// port-lint: source value/de.rs
package io.github.kotlinmania.serdejson

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
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.UnitDeserialize
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.value.intoDeserializer

internal object ValueDeserialize : Deserialize<Value> {
    override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Value> =
        deserializer.deserializeAny(ValueVisitor)
}

private object ValueVisitor : Visitor<Value> {
    override fun expecting(): String = "any valid JSON value"

    override fun visitBool(v: Boolean): SerdeResult<Value> = SerdeResult.success(Value.Bool(v))

    override fun visitI64(v: Long): SerdeResult<Value> = SerdeResult.success(Value.Number(JsonNumber.fromI64(v)))

    override fun visitI128(v: String): SerdeResult<Value> {
        val signed = v.toLongOrNull()
        if (signed != null) return visitI64(signed)
        val unsigned = v.toULongOrNull() ?: return SerdeResult.failure(SerdeError.custom("number out of range"))
        return visitU64(unsigned)
    }

    override fun visitU64(v: ULong): SerdeResult<Value> = SerdeResult.success(Value.Number(JsonNumber.fromU64(v)))

    override fun visitU128(v: String): SerdeResult<Value> {
        val unsigned = v.toULongOrNull() ?: return SerdeResult.failure(SerdeError.custom("number out of range"))
        return visitU64(unsigned)
    }

    override fun visitF64(v: Double): SerdeResult<Value> =
        SerdeResult.success(JsonNumber.fromF64(v)?.let { Value.Number(it) } ?: Value.Null)

    override fun visitStr(v: String): SerdeResult<Value> = SerdeResult.success(Value.Str(v))

    override fun visitNone(): SerdeResult<Value> = SerdeResult.success(Value.Null)

    override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<Value> =
        ValueDeserialize.deserialize(deserializer)

    override fun visitUnit(): SerdeResult<Value> = SerdeResult.success(Value.Null)

    override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Value> =
        serdeCatching {
            val values = mutableListOf<Value>()
            while (true) {
                when (val `__serdeNext0` = `access`.nextElement(`ValueDeserialize`).getOrThrow()) {
                    is io.github.kotlinmania.serdecore.de.SeqElement.Some -> {
                        val `elem` = `__serdeNext0`.value
                        `values`.add(`elem`)
                    }

                    io.github.kotlinmania.serdecore.de.SeqElement.None -> {
                        break
                    }
                }
            }
            Value.Array(values)
        }

    override fun <A : MapAccess> visitMap(access: A): SerdeResult<Value> {
        val firstKey = access.nextKey(StringDeserialize).getOrElse { return SerdeResult.failure(it) }
        if (firstKey == RawValue.TOKEN) {
            val raw = access.nextValueSeed(BoxedFromString).getOrElse { return SerdeResult.failure(it) }
            val parsed = fromStr(raw.json)
            return parsed.toSerdeResult()
        }
        val values = JsonMap()
        if (firstKey != null) {
            val firstValue = access.nextValue(ValueDeserialize).getOrElse { return SerdeResult.failure(it) }
            values.insert(firstKey, firstValue)
            while (true) {
                val entry = access.nextEntry(StringDeserialize, ValueDeserialize).getOrElse { return SerdeResult.failure(it) } ?: break
                values.insert(entry.first, entry.second)
            }
        }
        return SerdeResult.success(Value.Object(values))
    }
}

/**
 * A [Deserializer] that reads from a [Value].
 */
class ValueDeserializer(
    private val value: Value,
) : Deserializer {
    override fun isHumanReadable(): Boolean = true

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        serdeCatching {
            when (value) {
                is Value.Null -> visitor.visitUnit()
                is Value.Bool -> visitor.visitBool(value.value)
                is Value.Number -> value.value.deserializeAny(visitor)
                is Value.Str -> visitor.visitString(value.value)
                is Value.Array -> visitArray(value.value, visitor)
                is Value.Object -> visitMap(value.value, visitor)
            }
        }.flatMap { it }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Bool -> visitor.visitBool(value.value)
            else -> SerdeResult.failure(invalidType(visitor))
        }

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeNumber(value, visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeString(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeString(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Str -> visitor.visitString(value.value)
            else -> SerdeResult.failure(invalidType(visitor))
        }

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeByteBuf(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Str -> visitor.visitString(value.value)
            is Value.Array -> visitArray(value.value, visitor)
            else -> SerdeResult.failure(invalidType(visitor))
        }

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Null -> visitor.visitNone()
            else -> visitor.visitSome(this)
        }

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Null -> visitor.visitUnit()
            else -> SerdeResult.failure(invalidType(visitor))
        }

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        deserializeUnit(visitor)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        if (name == RawValue.TOKEN) {
            toStr(value).toSerdeResult().flatMap { visitor.visitMap(OwnedRawDeserializer(it)) }
        } else {
            visitor.visitNewtypeStruct(this)
        }

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Array -> visitArray(value.value, visitor)
            else -> SerdeResult.failure(invalidType(visitor))
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Object -> visitMap(value.value, visitor)
            else -> SerdeResult.failure(invalidType(visitor))
        }

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Array -> visitArray(value.value, visitor)
            is Value.Object -> visitMap(value.value, visitor)
            else -> SerdeResult.failure(invalidType(visitor))
        }

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Object -> {
                val iter = value.value.iterator()
                val first = if (iter.hasNext()) iter.next() else null
                if (first == null) {
                    SerdeResult.failure(
                        SerdeError.invalidValue(Unexpected.Map, Expected { "map with a single key" }),
                    )
                } else if (iter.hasNext()) {
                    // enums are encoded in json as maps with a single key:value pair
                    SerdeResult.failure(
                        SerdeError.invalidValue(Unexpected.Map, Expected { "map with a single key" }),
                    )
                } else {
                    visitor.visitEnum(ValueEnumAccess(first.first, first.second))
                }
            }

            is Value.Str -> {
                visitor.visitEnum(ValueEnumAccess(value.value, null))
            }

            else -> {
                SerdeResult.failure(
                    SerdeError.invalidType(value.unexpected(), Expected { "string or map" }),
                )
            }
        }

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeString(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()

    private fun invalidType(exp: Expected): SerdeError =
        SerdeError.invalidType(value.unexpected(), exp)
}

/**
 * Deserialize a [Value] as a JSON number or report an invalid-type diagnostic using [visitor].
 */
private fun <V> deserializeNumber(value: Value, visitor: Visitor<V>): SerdeResult<V> =
    when (value) {
        is Value.Number -> value.value.deserializeAny(visitor)
        else -> SerdeResult.failure(SerdeError.invalidType(value.unexpected(), visitor))
    }

/**
 * Visit an array as a sequence.
 */
private fun <V> visitArray(array: List<Value>, visitor: Visitor<V>): SerdeResult<V> {
    val seqAccess = ValueSeqAccess(array)
    val result = visitor.visitSeq(seqAccess).getOrElse { return SerdeResult.failure(it) }
    if (seqAccess.sizeHint() != 0) {
        return SerdeResult.failure(SerdeError.invalidLength(array.size, Expected { "fewer elements in array" }))
    }
    return SerdeResult.success(result)
}

/**
 * Visit a map.
 */
private fun <V> visitMap(map: io.github.kotlinmania.serdejson.JsonMap, visitor: Visitor<V>): SerdeResult<V> {
    val mapAccess = ValueMapAccess(map)
    val result = visitor.visitMap(mapAccess).getOrElse { return SerdeResult.failure(it) }
    if (mapAccess.sizeHint() != 0) {
        return SerdeResult.failure(SerdeError.invalidLength(map.len(), Expected { "fewer elements in map" }))
    }
    return SerdeResult.success(result)
}

/**
 * [SeqAccess] backed by a [List] of [Value].
 */
private class ValueSeqAccess(
    private val list: List<Value>,
) : SeqAccess {
    private val iter = list.iterator()
    private var remaining = list.size

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<SeqElement<T>> =
        if (iter.hasNext()) {
            val value = iter.next()
            remaining -= 1
            val deserializer = ValueDeserializer(value)
            `seed`.deserialize(`deserializer`).map { `__serdeValue0` ->
                io.github.kotlinmania.serdecore.de.SeqElement
                    .Some(`__serdeValue0`)
            }
        } else {
            io.github.kotlinmania.serde.SerdeResult
                .success(io.github.kotlinmania.serdecore.de.SeqElement.None)
        }

    override fun sizeHint(): Int = remaining
}

/**
 * [MapAccess] backed by a [ValueMap].
 */
private class ValueMapAccess(
    private val map: io.github.kotlinmania.serdejson.JsonMap,
) : MapAccess {
    private val iter = map.iterator()
    private var remaining = map.len()
    private var currentValue: Value? = null

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> =
        if (iter.hasNext()) {
            val (key, value) = iter.next()
            remaining -= 1
            currentValue = value
            seed.deserialize(ValueMapKey(key)).map { it }
        } else {
            SerdeResult.success(null)
        }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
        val value = currentValue ?: return SerdeResult.failure(SerdeError.custom("value is missing"))
        currentValue = null
        return seed.deserialize(ValueDeserializer(value))
    }

    override fun sizeHint(): Int = remaining
}

/**
 * [EnumAccess] for deserializing enum variants from a [Value].
 */
private class ValueEnumAccess(
    private val variant: String,
    private val value: Value?,
) : EnumAccess {
    override fun <T> variantSeed(seed: DeserializeSeed<T>): SerdeResult<Pair<T, VariantAccess>> {
        val variant = this.variant.intoDeserializer()
        val visitor = ValueVariantAccess(value)
        return seed.deserialize(variant).map { it to visitor }
    }
}

/**
 * [VariantAccess] for deserializing enum variant values from a [Value].
 */
private class ValueVariantAccess(
    private val value: Value?,
) : VariantAccess {
    override fun unitVariant(): SerdeResult<Unit> =
        if (value != null) {
            UnitDeserialize.deserialize(ValueDeserializer(value))
        } else {
            SerdeResult.success(Unit)
        }

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
        if (value != null) {
            seed.deserialize(ValueDeserializer(value))
        } else {
            SerdeResult.failure(
                SerdeError.invalidType(Unexpected.UnitVariant, Expected { "newtype variant" }),
            )
        }

    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Array -> {
                if (value.value.isEmpty()) visitor.visitUnit() else visitArray(value.value, visitor)
            }

            null -> {
                SerdeResult.failure(
                    SerdeError.invalidType(Unexpected.UnitVariant, Expected { "tuple variant" }),
                )
            }

            else -> {
                SerdeResult.failure(
                    SerdeError.invalidType(value.unexpected(), Expected { "tuple variant" }),
                )
            }
        }

    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Object -> {
                visitMap(value.value, visitor)
            }

            null -> {
                SerdeResult.failure(
                    SerdeError.invalidType(Unexpected.UnitVariant, Expected { "struct variant" }),
                )
            }

            else -> {
                SerdeResult.failure(
                    SerdeError.invalidType(value.unexpected(), Expected { "struct variant" }),
                )
            }
        }
}

private class ValueMapKey(
    private val key: String,
) : Deserializer by ValueDeserializer(Value.Str(key)) {
    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeI8(visitor) }

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeI16(visitor) }

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeI32(visitor) }

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeI64(visitor) }

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = integer128Key(visitor, signed = true)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeU8(visitor) }

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeU16(visitor) }

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeU32(visitor) }

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeU64(visitor) }

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = integer128Key(visitor, signed = false)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeF32(visitor) }

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = numericKey { it.deserializeF64(visitor) }

    private fun <V> numericKey(decode: (JsonDeserializer) -> SerdeResult<V>): SerdeResult<V> {
        val first = key.firstOrNull()
        if (first == null || (first !in '0'..'9' && first != '-')) {
            return SerdeResult.failure(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedNumericKey, 0, 0).toString()))
        }
        val parser = JsonParser(key)
        val value = decode(JsonDeserializer(parser)).getOrElse { return SerdeResult.failure(it) }
        if (parser.peekRaw() != -1) {
            return SerdeResult.failure(SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedNumericKey, 0, 0).toString()))
        }
        return SerdeResult.success(value)
    }

    private fun <V> integer128Key(visitor: Visitor<V>, signed: Boolean): SerdeResult<V> {
        val checked =
            object : Visitor<V> {
                override fun expecting(): String = visitor.expecting()

                private fun withinRange(value: String, maximum: String): Boolean {
                    val magnitude = value.removePrefix("-")
                    return magnitude.length < maximum.length || (magnitude.length == maximum.length && magnitude <= maximum)
                }

                override fun visitI128(v: String): SerdeResult<V> {
                    val maximum = if (v.startsWith('-')) "170141183460469231731687303715884105728" else "170141183460469231731687303715884105727"
                    if (!withinRange(v, maximum)) return SerdeResult.failure(SerdeError.custom(ErrorCode.NumberOutOfRange.toString()))
                    return visitor.visitI128(if (v == "-0") "0" else v)
                }

                override fun visitU128(v: String): SerdeResult<V> {
                    if (!withinRange(v, "340282366920938463463374607431768211455")) {
                        return SerdeResult.failure(SerdeError.custom(ErrorCode.NumberOutOfRange.toString()))
                    }
                    return visitor.visitU128(v)
                }
            }
        return numericKey { if (signed) it.deserializeI128(checked) else it.deserializeU128(checked) }
    }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        when (key) {
            "true" -> visitor.visitBool(true)
            "false" -> visitor.visitBool(false)
            else -> SerdeResult.failure(SerdeError.invalidType(Unexpected.Str(key), visitor))
        }

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSome(this)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = visitor.visitNewtypeStruct(this)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        key.intoDeserializer().deserializeEnum(name, variants, visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)
}

/**
 * The [Unexpected] representation of a [Value], used in error messages.
 */
fun Value.unexpected(): Unexpected =
    when (this) {
        is Value.Null -> Unexpected.UnitValue
        is Value.Bool -> Unexpected.Bool(this.value)
        is Value.Number -> JsonNumber.unexpected(this.value)
        is Value.Str -> Unexpected.Str(this.value)
        is Value.Array -> Unexpected.Seq
        is Value.Object -> Unexpected.Map
    }

/**
 * Deserialize a [Value] into a type [T] using the given [Deserialize] implementation.
 *
 * Converts a [Value] into a typed value.
 */
fun <T> Value.deserialize(deserialize: Deserialize<T>): SerdeResult<T> =
    deserialize.deserialize(ValueDeserializer(this))
