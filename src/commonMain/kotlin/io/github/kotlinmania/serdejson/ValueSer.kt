// port-lint: source src/value/ser.rs
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
        result = Value.Number(Number.fromI64(v))
        return SerdeResult.success(result)
    }

    override fun serializeU8(v: UByte): SerdeResult<Value> = serializeU64(v.toULong())
    override fun serializeU16(v: UShort): SerdeResult<Value> = serializeU64(v.toULong())
    override fun serializeU32(v: UInt): SerdeResult<Value> = serializeU64(v.toULong())

    override fun serializeU64(v: ULong): SerdeResult<Value> {
        result = Value.Number(Number.fromU64(v))
        return SerdeResult.success(result)
    }

    override fun serializeF32(v: Float): SerdeResult<Value> = serializeF64(v.toDouble())

    override fun serializeF64(v: Double): SerdeResult<Value> {
        val n = Number.fromF64(v) ?: return SerdeResult.failure(SerdeError.custom("not a JSON number"))
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
        val list = v.map { Value.Number(Number.fromI64(it.toLong())) }
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
        result = Value.Str(variant)
        return SerdeResult.success(result)
    }

    override fun <T> serializeNewtypeStruct(name: String, value: T): SerdeResult<Value>
        where T : Serialize = value.serialize(this)

    override fun <T> serializeNewtypeVariant(name: String, variantIndex: UInt, variant: String, value: T): SerdeResult<Value>
        where T : Serialize = serdeCatching {
            val map = ValueMap()
            map.insert(variant, value.let { val s = ValueSerializer(); it.serialize(s); s.result })
            result = Value.Object(map)
            result
        }

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Value>> = serdeCatching {
        ValueSeqSerialize()
    }

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Value>> = serializeSeq(len)

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Value>> =
        serializeSeq(len) as SerdeResult<SerializeTupleStruct<Value>>

    override fun serializeTupleVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeTupleVariant<Value>> = serdeCatching {
        val s = ValueSeqSerialize()
        s as SerializeTupleVariant<Value>
    }

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Value>> = serdeCatching {
        ValueMapSerialize()
    }

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Value>> =
        serializeMap(len) as SerdeResult<SerializeStruct<Value>>

    override fun serializeStructVariant(name: String, variantIndex: UInt, variant: String, len: Int): SerdeResult<SerializeStructVariant<Value>> =
        serializeMap(len) as SerdeResult<SerializeStructVariant<Value>>
}

/**
 * SerializeSeq implementation that builds a [Value.Array].
 */
private class ValueSeqSerialize : SerializeSeq<Value> {
    private val list = mutableListOf<Value>()

    override fun <T> serializeElement(value: T): SerdeResult<Value>
        where T : Serialize {
        val s = ValueSerializer()
        value.serialize(s)
        list.add(s.result)
        return SerdeResult.success(Value.Null)
    }

    override fun end(): SerdeResult<Value> {
        return SerdeResult.success(Value.Array(list))
    }
}

/**
 * SerializeMap implementation that builds a [Value.Object].
 */
private class ValueMapSerialize : SerializeMap<Value> {
    private val map = ValueMap()
    private var currentKey: String? = null

    override fun <K, V> serializeEntry(key: K, value: V): SerdeResult<Value>
        where K : Serialize,
              V : Serialize = serdeCatching {
            val ks = ValueSerializer()
            key.serialize(ks)
            val keyStr = when (val kv = ks.result) {
                is Value.Str -> kv.value
                else -> kv.toString()
            }
            val vs = ValueSerializer()
            value.serialize(vs)
            map.insert(keyStr, vs.result)
            Value.Null
        }

    override fun end(): SerdeResult<Value> = SerdeResult.success(Value.Object(map))
}