// port-lint: source value/ser.rs
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

/**
 * A serializer that builds a [Value] from any [Serialize] type.
 *
 * This is the serializer that backs [toValue]. Unlike the main JSON serializer
 * which goes from some serializable value of type `T` to JSON text, this one
 * goes from `T` to [Value].
 */
class ValueSerializer : Serializer<Value> {
    var result: Value = Value.Null
        private set

    override fun serializeBool(v: Boolean): SerdeResult<Value> {
        result = Value.Bool(v)
        return SerdeResult.success(result)
    }

    override fun serializeI8(v: Byte): SerdeResult<Value> = serializeI64(v.toLong())

    override fun serializeI16(v: Short): SerdeResult<Value> = serializeI64(v.toLong())

    override fun serializeI32(v: Int): SerdeResult<Value> = serializeI64(v.toLong())

    override fun serializeI64(v: Long): SerdeResult<Value> {
        result = Value.Number(JsonNumber.fromI64(v))
        return SerdeResult.success(result)
    }

    override fun serializeU8(v: UByte): SerdeResult<Value> = serializeU64(v.toULong())

    override fun serializeU16(v: UShort): SerdeResult<Value> = serializeU64(v.toULong())

    override fun serializeU32(v: UInt): SerdeResult<Value> = serializeU64(v.toULong())

    override fun serializeU64(v: ULong): SerdeResult<Value> {
        result = Value.Number(JsonNumber.fromU64(v))
        return SerdeResult.success(result)
    }

    override fun serializeF32(v: Float): SerdeResult<Value> = serializeF64(v.toDouble())

    override fun serializeF64(v: Double): SerdeResult<Value> {
        val n = JsonNumber.fromF64(v) ?: return SerdeResult.failure(SerdeError.custom("not a JSON number"))
        result = Value.Number(n)
        return SerdeResult.success(result)
    }

    override fun serializeChar(v: Char): SerdeResult<Value> {
        result = Value.Str(v.toString())
        return SerdeResult.success(result)
    }

    override fun serializeStr(v: String): SerdeResult<Value> {
        result = Value.Str(v)
        return SerdeResult.success(result)
    }

    override fun serializeBytes(v: ByteArray): SerdeResult<Value> {
        val list = v.map { Value.Number(JsonNumber.fromI64(it.toLong())) }
        result = Value.Array(list)
        return SerdeResult.success(result)
    }

    override fun serializeNone(): SerdeResult<Value> {
        result = Value.Null
        return SerdeResult.success(result)
    }

    override fun <T> serializeSome(value: T): SerdeResult<Value>
        where T : Serialize = value.serialize(this)

    override fun serializeUnit(): SerdeResult<Value> {
        result = Value.Null
        return SerdeResult.success(result)
    }

    override fun serializeUnitStruct(name: String): SerdeResult<Value> = serializeUnit()

    override fun serializeUnitVariant(name: String, variantIndex: UInt, variant: String): SerdeResult<Value> {
        check(name.isNotEmpty() || name.isEmpty())
        check(variantIndex >= 0u)
        result = Value.Str(variant)
        return SerdeResult.success(result)
    }

    override fun <T> serializeNewtypeStruct(name: String, value: T): SerdeResult<Value>
        where T : Serialize = value.serialize(this)

    override fun <T> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Value>
        where T : Serialize =
        serdeCatching {
            val inner = toValue(value).getOrThrow()
            val map = ValueMap()
            map.insert(variant, inner)
            result = Value.Object(map)
            result
        }

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Value>> =
        SerdeResult.success(ValueVec())

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Value>> =
        SerdeResult.success(ValueVec())

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Value>> =
        SerdeResult.success(ValueVec())

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Value>> =
        SerdeResult.success(ValueTupleVariant(variant))

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Value>> =
        SerdeResult.success(ValueMapSerialize())

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Value>> =
        SerdeResult.success(ValueMapSerialize())

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Value>> =
        SerdeResult.success(ValueStructVariant(variant))
}

/**
 * SerializeSeq / SerializeTuple / SerializeTupleStruct implementation that builds a [Value.Array].
 *
 * In the upstream Rust code, a single `SerializeVec` type implements all three
 * serde traits. Here, [ValueVec] implements all three Kotlin interfaces.
 */
private class ValueVec :
    SerializeSeq<Value>,
    SerializeTuple<Value>,
    SerializeTupleStruct<Value> {
    private val list = mutableListOf<Value>()

    override fun <T> serializeElement(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            list.add(toValue(value).getOrThrow())
        }

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            list.add(toValue(value).getOrThrow())
        }

    override fun end(): SerdeResult<Value> = SerdeResult.success(Value.Array(list))
}

/**
 * SerializeTupleVariant implementation that builds a [Value.Object] wrapping a [Value.Array]
 * under the variant name, matching the upstream `SerializeTupleVariant` behavior.
 */
private class ValueTupleVariant(
    private val name: String,
) : SerializeTupleVariant<Value> {
    private val list = mutableListOf<Value>()

    override fun <T> serializeField(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            list.add(toValue(value).getOrThrow())
        }

    override fun end(): SerdeResult<Value> =
        serdeCatching {
            val map = ValueMap()
            map.insert(name, Value.Array(list))
            Value.Object(map)
        }
}

/**
 * SerializeMap / SerializeStruct implementation that builds a [Value.Object].
 *
 * In the upstream Rust code, a single `SerializeMap` enum implements both
 * `SerializeMap` and `SerializeStruct`. Here, [ValueMapSerialize] implements
 * both Kotlin interfaces.
 */
private class ValueMapSerialize :
    SerializeMap<Value>,
    SerializeStruct<Value> {
    private val map = ValueMap()
    private var currentKey: String? = null

    override fun <T> serializeKey(key: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            val keyVal = toValue(key).getOrThrow()
            currentKey =
                when (keyVal) {
                    is Value.Str -> keyVal.value
                    else -> keyVal.toString()
                }
        }

    override fun <T> serializeValue(value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            val k = currentKey ?: error("serializeValue called before serializeKey")
            currentKey = null
            map.insert(k, toValue(value).getOrThrow())
        }

    override fun <K, V> serializeEntry(key: K, value: V): SerdeResult<Unit>
        where K : Serialize,
              V : Serialize =
        serdeCatching {
            val keyVal = toValue(key).getOrThrow()
            val keyStr =
                when (keyVal) {
                    is Value.Str -> keyVal.value
                    else -> keyVal.toString()
                }
            map.insert(keyStr, toValue(value).getOrThrow())
        }

    override fun <T> serializeField(key: String, value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            map.insert(key, toValue(value).getOrThrow())
        }

    override fun end(): SerdeResult<Value> = SerdeResult.success(Value.Object(map))
}

/**
 * SerializeStructVariant implementation that builds a [Value.Object] wrapping
 * a [ValueMap] under the variant name, matching the upstream `SerializeStructVariant` behavior.
 */
private class ValueStructVariant(
    private val name: String,
) : SerializeStructVariant<Value> {
    private val map = ValueMap()

    override fun <T> serializeField(key: String, value: T): SerdeResult<Unit>
        where T : Serialize =
        serdeCatching {
            map.insert(key, toValue(value).getOrThrow())
        }

    override fun end(): SerdeResult<Value> =
        serdeCatching {
            val outer = ValueMap()
            outer.insert(name, Value.Object(map))
            Value.Object(outer)
        }
}
