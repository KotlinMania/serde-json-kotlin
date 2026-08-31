// port-lint: source serde_json/src/value/de.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.VariantAccess
import io.github.kotlinmania.serdecore.de.Visitor

/**
 * A [Deserializer] that reads from a [Value].
 *
 * This is the Kotlin equivalent of the upstream `impl Deserializer for Value`
 * and `impl Deserializer for &Value` (the owned and borrowed variants are unified
 * here because Kotlin has no lifetimes).
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
                is Value.Number -> deserializeNumberAny(value.value, visitor)
                is Value.Str -> visitor.visitStr(value.value)
                is Value.Array -> visitArray(value.value, visitor)
                is Value.Object -> visitMap(value.value, visitor)
            }
        }.flatMap { it }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Bool -> visitor.visitBool(value.value)
            else -> SerdeResult.failure(typeError("boolean"))
        }

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeI64(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeI64(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeI64(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Number -> {
                val n = value.value
                n.asI64()?.let { visitor.visitI64(it) }
                    ?: n.asU64()?.let { visitor.visitI64(it.toLong()) }
                    ?: n.asF64()?.let { visitor.visitI64(it.toLong()) }
                    ?: SerdeResult.failure(typeError("i64"))
            }
            else -> SerdeResult.failure(typeError("i64"))
        }

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeU64(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeU64(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeU64(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Number -> {
                val n = value.value
                n.asU64()?.let { visitor.visitU64(it) }
                    ?: n.asI64()?.let { if (it >= 0) visitor.visitU64(it.toULong()) else SerdeResult.failure(typeError("u64")) }
                    ?: n.asF64()?.let { visitor.visitU64(it.toULong()) }
                    ?: SerdeResult.failure(typeError("u64"))
            }
            else -> SerdeResult.failure(typeError("u64"))
        }

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeF64(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Number -> {
                val n = value.value
                n.asF64()?.let { visitor.visitF64(it) }
                    ?: n.asI64()?.let { visitor.visitF64(it.toDouble()) }
                    ?: n.asU64()?.let { visitor.visitF64(it.toDouble()) }
                    ?: SerdeResult.failure(typeError("f64"))
            }
            else -> SerdeResult.failure(typeError("f64"))
        }

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Str -> visitor.visitStr(value.value)
            else -> SerdeResult.failure(typeError("string"))
        }

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Str -> visitor.visitBytes(value.value.encodeToByteArray())
            is Value.Array -> visitArray(value.value, visitor)
            else -> SerdeResult.failure(typeError("bytes or array"))
        }

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeBytes(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Null -> visitor.visitNone()
            else -> visitor.visitSome(this)
        }

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Null -> visitor.visitUnit()
            else -> SerdeResult.failure(typeError("null"))
        }

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        deserializeUnit(visitor)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> =
        visitor.visitNewtypeStruct(this)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Array -> visitArray(value.value, visitor)
            else -> SerdeResult.failure(typeError("array"))
        }

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> =
        deserializeSeq(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Object -> visitMap(value.value, visitor)
            else -> SerdeResult.failure(typeError("map"))
        }

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Array -> visitArray(value.value, visitor)
            is Value.Object -> visitMap(value.value, visitor)
            else -> SerdeResult.failure(typeError("array or map"))
        }

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Object -> {
                val iter = value.value.iterator()
                val first = if (iter.hasNext()) iter.next() else null
                if (first == null) {
                    SerdeResult.failure(SerdeError.custom("expected map with a single key"))
                } else if (iter.hasNext()) {
                    SerdeResult.failure(SerdeError.custom("expected map with a single key"))
                } else {
                    visitor.visitEnum(ValueEnumAccess(first.first, first.second))
                }
            }
            is Value.Str -> visitor.visitEnum(ValueEnumAccess(value.value, null))
            else -> SerdeResult.failure(SerdeError.custom("expected string or map for enum, got ${valueTypeName(value)}"))
        }

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeStr(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitUnit()

    private fun typeError(expected: String): SerdeError =
        SerdeError.custom("invalid type: expected $expected, got ${valueTypeName(value)}")
}

/**
 * Deserialize a number value, dispatching to the appropriate visitor method.
 */
private fun <V> deserializeNumberAny(n: JsonNumber, visitor: Visitor<V>): SerdeResult<V> =
    when {
        n.isU64() -> visitor.visitU64(n.asU64()!!)
        n.isI64() -> visitor.visitI64(n.asI64()!!)
        n.isF64() -> visitor.visitF64(n.asF64()!!)
        else -> visitor.visitF64(n.asF64() ?: 0.0)
    }

/**
 * Visit an array as a sequence.
 */
private fun <V> visitArray(array: List<Value>, visitor: Visitor<V>): SerdeResult<V> {
    val seqAccess = ValueSeqAccess(array)
    val result = visitor.visitSeq(seqAccess)
    return result
}

/**
 * Visit a map.
 */
private fun <V> visitMap(map: ValueMap, visitor: Visitor<V>): SerdeResult<V> {
    val mapAccess = ValueMapAccess(map)
    return visitor.visitMap(mapAccess)
}

/**
 * [SeqAccess] backed by a [List] of [Value].
 */
private class ValueSeqAccess(
    private val list: List<Value>,
) : SeqAccess {
    private val iter = list.iterator()

    override fun <T> nextElementSeed(seed: DeserializeSeed<T>): SerdeResult<T?> =
        if (iter.hasNext()) {
            val value = iter.next()
            seed.deserialize(ValueDeserializer(value)).map { it }
        } else {
            SerdeResult.success(null)
        }

    override fun sizeHint(): Int? = if (!iter.hasNext()) 0 else null
}

/**
 * [MapAccess] backed by a [ValueMap].
 */
private class ValueMapAccess(
    private val map: ValueMap,
) : MapAccess {
    private val iter = map.iterator()
    private var currentValue: Value? = null

    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> =
        if (iter.hasNext()) {
            val (key, value) = iter.next()
            currentValue = value
            seed.deserialize(ValueDeserializer(Value.Str(key))).map { it }
        } else {
            SerdeResult.success(null)
        }

    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> {
        val value = currentValue ?: return SerdeResult.failure(SerdeError.custom("value is missing"))
        currentValue = null
        return seed.deserialize(ValueDeserializer(value))
    }

    override fun sizeHint(): Int? = if (!iter.hasNext()) 0 else null
}

/**
 * [EnumAccess] for deserializing enum variants from a [Value].
 */
private class ValueEnumAccess(
    private val variant: String,
    private val value: Value?,
) : EnumAccess {
    override fun <T> variantSeed(seed: DeserializeSeed<T>): SerdeResult<Pair<T, VariantAccess>> {
        val variantResult = seed.deserialize(ValueDeserializer(Value.Str(variant)))
        return variantResult.map { it to ValueVariantAccess(value) }
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
            // Deserialize the value as unit
            ValueDeserializer(value).deserializeUnit(UnitVisitor)
        } else {
            SerdeResult.success(Unit)
        }

    override fun <T> newtypeVariantSeed(seed: DeserializeSeed<T>): SerdeResult<T> =
        if (value != null) {
            seed.deserialize(ValueDeserializer(value))
        } else {
            SerdeResult.failure(SerdeError.custom("expected newtype variant, got unit variant"))
        }

    override fun <V> tupleVariant(len: Int, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Array -> if (value.value.isEmpty()) visitor.visitUnit() else visitArray(value.value, visitor)
            null -> SerdeResult.failure(SerdeError.custom("expected tuple variant, got unit variant"))
            else -> SerdeResult.failure(SerdeError.custom("expected tuple variant, got ${valueTypeName(value)}"))
        }

    override fun <V> structVariant(fields: List<String>, visitor: Visitor<V>): SerdeResult<V> =
        when (value) {
            is Value.Object -> visitMap(value.value, visitor)
            null -> SerdeResult.failure(SerdeError.custom("expected struct variant, got unit variant"))
            else -> SerdeResult.failure(SerdeError.custom("expected struct variant, got ${valueTypeName(value)}"))
        }
}

/** Visitor that accepts unit. */
private object UnitVisitor : Visitor<Unit> {
    override fun expecting(): String = "unit"

    override fun visitUnit(): SerdeResult<Unit> = SerdeResult.success(Unit)
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
 * This is the primary entry point for converting a [Value] into a typed value,
 * equivalent to the upstream `Value::deserialize`.
 */
fun <T> Value.deserialize(deserialize: Deserialize<T>): SerdeResult<T> =
    deserialize.deserialize(ValueDeserializer(this))
