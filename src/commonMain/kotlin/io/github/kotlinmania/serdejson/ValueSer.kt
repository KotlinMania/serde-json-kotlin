// port-lint: source value/ser.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeSeq
import io.github.kotlinmania.serdecore.ser.SerializeStruct
import io.github.kotlinmania.serdecore.ser.SerializeStructVariant
import io.github.kotlinmania.serdecore.ser.SerializeTuple
import io.github.kotlinmania.serdecore.ser.SerializeTupleStruct
import io.github.kotlinmania.serdecore.ser.SerializeTupleVariant
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdecore.ser.SerializeMap as SerdeSerializeMap

/**
 * Serializes a [Value] using the given [serializer].
 */
fun <Ok> serialize(value: Value, serializer: Serializer<Ok>): SerdeResult<Ok> {
    return when (value) {
        is Value.Null -> serializer.serializeUnit()
        is Value.Bool -> serializer.serializeBool(value.value)
        is Value.Number -> value.value.serialize(serializer)
        is Value.Str -> serializer.serializeStr(value.value)
        is Value.Array -> {
            val seq = serializer.serializeSeq(value.value.size).getOrElse { return SerdeResult.failure(it) }
            for (element in value.value) {
                seq.serializeElement(element).getOrElse { return SerdeResult.failure(it) }
            }
            seq.end()
        }
        is Value.Object -> {
            val map = serializer.serializeMap(value.value.len()).getOrElse { return SerdeResult.failure(it) }
            for ((k, v) in value.value) {
                map.serializeEntry(Value.Str(k), v).getOrElse { return SerdeResult.failure(it) }
            }
            map.end()
        }
    }
}

/**
 * Serializer whose output is a [Value].
 *
 * This is the serializer that backs [toValue]. Unlike the main serializer
 * which goes from some serializable value of type `T` to JSON text, this one
 * goes from `T` to [Value].
 */
open class Serializer : io.github.kotlinmania.serdecore.ser.Serializer<Value> {
    var result: Value = Value.Null
        private set

    override fun serializeBool(v: Boolean): SerdeResult<Value> {
        val res = Value.Bool(v)
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeI8(v: Byte): SerdeResult<Value> = serializeI64(v.toLong())

    override fun serializeI16(v: Short): SerdeResult<Value> = serializeI64(v.toLong())

    override fun serializeI32(v: Int): SerdeResult<Value> = serializeI64(v.toLong())

    override fun serializeI64(v: Long): SerdeResult<Value> {
        val res = Value.Number(JsonNumber.fromI64(v))
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeI128(value: String): SerdeResult<Value> {
        val longVal = value.toLongOrNull()
        if (longVal != null) {
            val res = Value.Number(JsonNumber.fromI64(longVal))
            result = res
            return SerdeResult.success(res)
        }
        val ulongVal = value.toULongOrNull()
        if (ulongVal != null) {
            val res = Value.Number(JsonNumber.fromU64(ulongVal))
            result = res
            return SerdeResult.success(res)
        }
        return SerdeResult.failure(
            SerdeError.custom(JsonError.syntax(ErrorCode.NumberOutOfRange, 0, 0).toString()),
        )
    }

    override fun serializeU8(v: UByte): SerdeResult<Value> = serializeU64(v.toULong())

    override fun serializeU16(v: UShort): SerdeResult<Value> = serializeU64(v.toULong())

    override fun serializeU32(v: UInt): SerdeResult<Value> = serializeU64(v.toULong())

    override fun serializeU64(v: ULong): SerdeResult<Value> {
        val res = Value.Number(JsonNumber.fromU64(v))
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeU128(value: String): SerdeResult<Value> {
        val ulongVal = value.toULongOrNull()
        if (ulongVal != null) {
            val res = Value.Number(JsonNumber.fromU64(ulongVal))
            result = res
            return SerdeResult.success(res)
        }
        return SerdeResult.failure(
            SerdeError.custom(JsonError.syntax(ErrorCode.NumberOutOfRange, 0, 0).toString()),
        )
    }

    override fun serializeF32(v: Float): SerdeResult<Value> {
        val n =
            JsonNumber.fromF32(v)
                ?: return SerdeResult.failure(SerdeError.custom("not a JSON number"))
        val res = Value.Number(n)
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeF64(v: Double): SerdeResult<Value> {
        val n =
            JsonNumber.fromF64(v)
                ?: return SerdeResult.failure(SerdeError.custom("not a JSON number"))
        val res = Value.Number(n)
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeChar(v: Char): SerdeResult<Value> {
        var s = ""
        s += v
        val res = Value.Str(s)
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeStr(v: String): SerdeResult<Value> {
        val res = Value.Str(v)
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeBytes(v: ByteArray): SerdeResult<Value> {
        val vec = v.map { Value.Number(JsonNumber.fromI64(it.toLong())) }
        val res = Value.Array(vec)
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeUnit(): SerdeResult<Value> {
        val res = Value.Null
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeUnitStruct(name: String): SerdeResult<Value> = serializeUnit()

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Value> = serializeStr(variant)

    override fun <T : Serialize> serializeNewtypeStruct(name: String, value: T): SerdeResult<Value> = value.serialize(this)

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Value> {
        val values =
            io.github.kotlinmania.serdejson
                .JsonMap()
        val inner = toValue(value).getOrElse { return SerdeResult.failure(it) }
        values.insert(variant, inner)
        val res = Value.Object(values)
        result = res
        return SerdeResult.success(res)
    }

    override fun serializeNone(): SerdeResult<Value> = serializeUnit()

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<Value> = value.serialize(this)

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Value>> = SerdeResult.success(SerializeVec(ArrayList(len ?: 0)))

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Value>> = SerdeResult.success(SerializeVec(ArrayList(len)))

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Value>> = SerdeResult.success(SerializeVec(ArrayList(len)))

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Value>> =
        SerdeResult.success(
            SerializeTupleVariant(variant, ArrayList(len)),
        )

    override fun serializeMap(len: Int?): SerdeResult<SerdeSerializeMap<Value>> =
        SerdeResult.success(
            SerializeMap.Map(
                io.github.kotlinmania.serdejson.JsonMap
                    .withCapacity(len ?: 0),
                null,
            ),
        )

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Value>> =
        when (name) {
            NUMBER_TOKEN -> SerdeResult.success(SerializeMap.Number(null))
            RAW_VALUE_TOKEN -> SerdeResult.success(SerializeMap.RawValue(null))
            else -> {
                val map =
                    io.github.kotlinmania.serdejson.JsonMap
                        .withCapacity(len)
                SerdeResult.success(SerializeMap.Map(map, null))
            }
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Value>> =
        SerdeResult.success(
            SerializeStructVariant(
                variant,
                io.github.kotlinmania.serdejson
                    .JsonMap(),
            ),
        )

    override fun collectStr(value: Any): SerdeResult<Value> {
        val res = Value.Str(value.toString())
        result = res
        return SerdeResult.success(res)
    }
}

internal const val NUMBER_TOKEN: String = "\$serde_json::private::Number"
internal const val RAW_VALUE_TOKEN: String = "\$serde_json::private::RawValue"

class SerializeVec internal constructor(
    private val vec: MutableList<Value>,
) : SerializeSeq<Value>,
    SerializeTuple<Value>,
    SerializeTupleStruct<Value> {
    constructor() : this(mutableListOf())

    override fun <T : Serialize> serializeElement(value: T): SerdeResult<Unit> {
        val v = toValue(value).getOrElse { return SerdeResult.failure(it) }
        vec.add(v)
        return SerdeResult.success(Unit)
    }

    @kotlin.jvm.JvmName("serializeElementTuple")
    internal fun serializeElement(value: Value): SerdeResult<Unit> {
        vec.add(value)
        return SerdeResult.success(Unit)
    }

    override fun <T : Serialize> serializeField(value: T): SerdeResult<Unit> = serializeElement(value)

    override fun end(): SerdeResult<Value> = SerdeResult.success(Value.Array(vec))

    @kotlin.jvm.JvmName("endTuple")
    internal fun end(dummy: Unit): SerdeResult<Value> = end()

    @kotlin.jvm.JvmName("endTupleStruct")
    internal fun end(dummy1: Unit, dummy2: Unit): SerdeResult<Value> = end()
}

class SerializeTupleVariant internal constructor(
    private val name: String,
    private val vec: MutableList<Value>,
) : io.github.kotlinmania.serdecore.ser.SerializeTupleVariant<Value> {
    constructor(name: String) : this(name, mutableListOf())

    override fun <T : Serialize> serializeField(value: T): SerdeResult<Unit> {
        val v = toValue(value).getOrElse { return SerdeResult.failure(it) }
        vec.add(v)
        return SerdeResult.success(Unit)
    }

    override fun end(): SerdeResult<Value> {
        val objectMap =
            io.github.kotlinmania.serdejson
                .JsonMap()
        objectMap.insert(name, Value.Array(vec))
        return SerdeResult.success(Value.Object(objectMap))
    }
}

sealed class SerializeMap :
    SerdeSerializeMap<Value>,
    SerializeStruct<Value> {
    class Map internal constructor(
        val map: io.github.kotlinmania.serdejson.JsonMap,
        var nextKey: String? = null,
    ) : SerializeMap() {
        constructor() : this(
            io.github.kotlinmania.serdejson
                .JsonMap(),
            null,
        )
    }

    class Number internal constructor(
        var outValue: Value? = null,
    ) : SerializeMap() {
        constructor() : this(null)
    }

    class RawValue internal constructor(
        var outValue: Value? = null,
    ) : SerializeMap() {
        constructor() : this(null)
    }

    override fun <T : Serialize> serializeKey(key: T): SerdeResult<Unit> {
        return when (this) {
            is Map -> {
                val k = key.serialize(MapKeySerializer()).getOrElse { return SerdeResult.failure(it) }
                nextKey = k
                SerdeResult.success(Unit)
            }
            is Number -> error("unreachable")
            is RawValue -> error("unreachable")
        }
    }

    override fun <T : Serialize> serializeValue(value: T): SerdeResult<Unit> {
        return when (this) {
            is Map -> {
                val k = nextKey ?: error("serialize_value called before serialize_key")
                nextKey = null
                val v = toValue(value).getOrElse { return SerdeResult.failure(it) }
                map.insert(k, v)
                SerdeResult.success(Unit)
            }
            is Number -> error("unreachable")
            is RawValue -> error("unreachable")
        }
    }

    override fun <T : Serialize> serializeField(key: String, value: T): SerdeResult<Unit> {
        return when (this) {
            is Map -> {
                serializeEntry(Value.Str(key), value)
            }
            is Number -> {
                if (key == NUMBER_TOKEN) {
                    val emitted = value.serialize(NumberValueEmitter()).getOrElse { return SerdeResult.failure(it) }
                    outValue = emitted
                    SerdeResult.success(Unit)
                } else {
                    SerdeResult.failure(invalidNumber())
                }
            }
            is RawValue -> {
                if (key == RAW_VALUE_TOKEN) {
                    val emitted = value.serialize(RawValueEmitter()).getOrElse { return SerdeResult.failure(it) }
                    outValue = emitted
                    SerdeResult.success(Unit)
                } else {
                    SerdeResult.failure(invalidRawValue())
                }
            }
        }
    }

    override fun end(): SerdeResult<Value> =
        when (this) {
            is Map -> SerdeResult.success(Value.Object(map))
            is Number -> {
                val v = outValue ?: error("number value was not emitted")
                SerdeResult.success(v)
            }
            is RawValue -> {
                val v = outValue ?: error("raw value was not emitted")
                SerdeResult.success(v)
            }
        }

    @kotlin.jvm.JvmName("endStruct")
    internal fun end(dummy: Unit): SerdeResult<Value> = end()
}

class SerializeStructVariant internal constructor(
    private val name: String,
    private val map: io.github.kotlinmania.serdejson.JsonMap,
) : io.github.kotlinmania.serdecore.ser.SerializeStructVariant<Value> {
    constructor(name: String) : this(
        name,
        io.github.kotlinmania.serdejson
            .JsonMap(),
    )

    override fun <T : Serialize> serializeField(key: String, value: T): SerdeResult<Unit> {
        val v = toValue(value).getOrElse { return SerdeResult.failure(it) }
        map.insert(key, v)
        return SerdeResult.success(Unit)
    }

    override fun end(): SerdeResult<Value> {
        val objectMap =
            io.github.kotlinmania.serdejson
                .JsonMap()
        objectMap.insert(name, Value.Object(map))
        return SerdeResult.success(Value.Object(objectMap))
    }
}

fun keyMustBeAString(): SerdeError = SerdeError.custom(JsonError.syntax(ErrorCode.KeyMustBeAString, 0, 0).toString())

fun floatKeyMustBeFinite(): SerdeError = SerdeError.custom(JsonError.syntax(ErrorCode.FloatKeyMustBeFinite, 0, 0).toString())

open class MapKeySerializer : io.github.kotlinmania.serdecore.ser.Serializer<String> {
    override fun serializeBool(v: Boolean): SerdeResult<String> = SerdeResult.success(if (v) "true" else "false")

    override fun serializeI8(v: Byte): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeI16(v: Short): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeI32(v: Int): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeI64(v: Long): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeI128(value: String): SerdeResult<String> = SerdeResult.success(value)

    override fun serializeU8(v: UByte): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeU16(v: UShort): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeU32(v: UInt): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeU64(v: ULong): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeU128(value: String): SerdeResult<String> = SerdeResult.success(value)

    override fun serializeF32(v: Float): SerdeResult<String> =
        if (v.isFinite()) {
            SerdeResult.success(formatFinite(v.toDouble()))
        } else {
            SerdeResult.failure(floatKeyMustBeFinite())
        }

    override fun serializeF64(v: Double): SerdeResult<String> =
        if (v.isFinite()) {
            SerdeResult.success(formatFinite(v))
        } else {
            SerdeResult.failure(floatKeyMustBeFinite())
        }

    override fun serializeChar(v: Char): SerdeResult<String> = SerdeResult.success(v.toString())

    override fun serializeStr(v: String): SerdeResult<String> = SerdeResult.success(v)

    override fun serializeBytes(v: ByteArray): SerdeResult<String> = SerdeResult.failure(keyMustBeAString())

    override fun serializeNone(): SerdeResult<String> = SerdeResult.failure(keyMustBeAString())

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<String> = SerdeResult.failure(keyMustBeAString())

    override fun serializeUnit(): SerdeResult<String> = SerdeResult.failure(keyMustBeAString())

    override fun serializeUnitStruct(name: String): SerdeResult<String> = SerdeResult.failure(keyMustBeAString())

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<String> = SerdeResult.success(variant)

    override fun <T : Serialize> serializeNewtypeStruct(name: String, value: T): SerdeResult<String> = value.serialize(this)

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<String> = SerdeResult.failure(keyMustBeAString())

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<String>> = SerdeResult.failure(keyMustBeAString())

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<String>> = SerdeResult.failure(keyMustBeAString())

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<String>> = SerdeResult.failure(keyMustBeAString())

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<String>> = SerdeResult.failure(keyMustBeAString())

    override fun serializeMap(len: Int?): SerdeResult<SerdeSerializeMap<String>> = SerdeResult.failure(keyMustBeAString())

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<String>> = SerdeResult.failure(keyMustBeAString())

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<String>> = SerdeResult.failure(keyMustBeAString())

    override fun collectStr(value: Any): SerdeResult<String> = SerdeResult.success(value.toString())
}

fun invalidNumber(): SerdeError = SerdeError.custom(JsonError.syntax(ErrorCode.InvalidNumber, 0, 0).toString())

open class NumberValueEmitter : io.github.kotlinmania.serdecore.ser.Serializer<Value> {
    override fun serializeBool(v: Boolean): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeI8(v: Byte): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeI16(v: Short): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeI32(v: Int): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeI64(v: Long): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeU8(v: UByte): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeU16(v: UShort): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeU32(v: UInt): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeU64(v: ULong): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeF32(v: Float): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeF64(v: Double): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeChar(v: Char): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeStr(v: String): SerdeResult<Value> {
        val longVal = v.toLongOrNull()
        if (longVal != null) {
            val num = if (longVal >= 0) JsonNumber.fromU64(longVal.toULong()) else JsonNumber.fromI64(longVal)
            return SerdeResult.success(Value.Number(num))
        }
        val ulongVal = v.toULongOrNull()
        if (ulongVal != null) {
            return SerdeResult.success(Value.Number(JsonNumber.fromU64(ulongVal)))
        }
        val doubleVal = v.toDoubleOrNull()
        if (doubleVal != null && doubleVal.isFinite()) {
            val num = JsonNumber.fromF64(doubleVal) ?: return SerdeResult.failure(invalidNumber())
            return SerdeResult.success(Value.Number(num))
        }
        return SerdeResult.failure(invalidNumber())
    }

    override fun serializeBytes(v: ByteArray): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeNone(): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeUnit(): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeUnitStruct(name: String): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun <T : Serialize> serializeNewtypeStruct(name: String, value: T): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Value> = SerdeResult.failure(invalidNumber())

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Value>> = SerdeResult.failure(invalidNumber())

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Value>> = SerdeResult.failure(invalidNumber())

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Value>> = SerdeResult.failure(invalidNumber())

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Value>> = SerdeResult.failure(invalidNumber())

    override fun serializeMap(len: Int?): SerdeResult<SerdeSerializeMap<Value>> = SerdeResult.failure(invalidNumber())

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Value>> = SerdeResult.failure(invalidNumber())

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Value>> = SerdeResult.failure(invalidNumber())
}

fun invalidRawValue(): SerdeError = SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedSomeValue, 0, 0).toString())

open class RawValueEmitter : io.github.kotlinmania.serdecore.ser.Serializer<Value> {
    override fun serializeBool(v: Boolean): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeI8(v: Byte): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeI16(v: Short): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeI32(v: Int): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeI64(v: Long): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeU8(v: UByte): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeU16(v: UShort): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeU32(v: UInt): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeU64(v: ULong): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeF32(v: Float): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeF64(v: Double): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeChar(v: Char): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeStr(v: String): SerdeResult<Value> = fromStr(v)

    override fun serializeBytes(v: ByteArray): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeNone(): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeUnit(): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeUnitStruct(name: String): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun <T : Serialize> serializeNewtypeStruct(name: String, value: T): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Value> = SerdeResult.failure(invalidRawValue())

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Value>> = SerdeResult.failure(invalidRawValue())

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Value>> = SerdeResult.failure(invalidRawValue())

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Value>> = SerdeResult.failure(invalidRawValue())

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Value>> = SerdeResult.failure(invalidRawValue())

    override fun serializeMap(len: Int?): SerdeResult<SerdeSerializeMap<Value>> = SerdeResult.failure(invalidRawValue())

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Value>> = SerdeResult.failure(invalidRawValue())

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Value>> = SerdeResult.failure(invalidRawValue())

    override fun collectStr(value: Any): SerdeResult<Value> = serializeStr(value.toString())
}
