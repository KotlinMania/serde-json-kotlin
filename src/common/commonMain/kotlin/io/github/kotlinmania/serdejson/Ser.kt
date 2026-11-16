// port-lint: source ser.rs
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
import io.github.kotlinmania.serdejson.io.IoError
import io.github.kotlinmania.serdejson.io.IoResult
import io.github.kotlinmania.serdejson.io.IoWrite
import io.github.kotlinmania.serdejson.io.VecIoWrite

private val NULL_BYTES: ByteArray = "null".encodeToByteArray()
private val TRUE_BYTES: ByteArray = "true".encodeToByteArray()
private val FALSE_BYTES: ByteArray = "false".encodeToByteArray()
private val QUOTE_BYTES: ByteArray = byteArrayOf('"'.code.toByte())
private val OPEN_BRACKET_BYTES: ByteArray = byteArrayOf('['.code.toByte())
private val CLOSE_BRACKET_BYTES: ByteArray = byteArrayOf(']'.code.toByte())
private val OPEN_BRACE_BYTES: ByteArray = byteArrayOf('{'.code.toByte())
private val CLOSE_BRACE_BYTES: ByteArray = byteArrayOf('}'.code.toByte())
private val COMMA_BYTES: ByteArray = byteArrayOf(','.code.toByte())
private val COLON_BYTES: ByteArray = byteArrayOf(':'.code.toByte())
private val COLON_SPACE_BYTES: ByteArray = ": ".encodeToByteArray()
private val NEWLINE_BYTES: ByteArray = byteArrayOf('\n'.code.toByte())
private val COMMA_NEWLINE_BYTES: ByteArray = ",\n".encodeToByteArray()
private val DEFAULT_INDENT_BYTES: ByteArray = "  ".encodeToByteArray()
private val HEX_DIGITS: ByteArray = "0123456789abcdef".encodeToByteArray()

private inline fun ioToSerde(block: () -> IoResult<Unit>): SerdeResult<Unit> =
    serdeCatching {
        when (val result = block()) {
            is IoResult.Ok -> Unit
            is IoResult.Err -> ioErrorToSerde(result.error).raise()
        }
    }

private fun ioErrorToSerde(err: IoError): SerdeError =
    SerdeError.custom(JsonError.io(err.message).toString())

private fun formatFiniteFloat(f: Float): String {
    if (f == Float.POSITIVE_INFINITY) return "9e999"
    if (f == Float.NEGATIVE_INFINITY) return "-9e999"
    if (f.isNaN()) return "0"
    if (f == 0.0f && (1.0f / f) == Float.NEGATIVE_INFINITY) return "-0.0"
    val s = f.toString()
    return if (!s.contains('.') && !s.contains('e') && !s.contains('E')) {
        "$s.0"
    } else {
        s
    }
}

private fun normalizeWideInteger(value: String, signed: Boolean): SerdeResult<String> {
    val negative = value.startsWith('-')
    val digits = if (negative) value.drop(1) else value
    if (digits.isEmpty() || digits.any { it !in '0'..'9' } || (negative && !signed)) {
        return SerdeResult.failure(invalidNumberError())
    }
    val magnitude = digits.trimStart('0').ifEmpty { "0" }
    val maximum =
        if (!signed) {
            "340282366920938463463374607431768211455"
        } else if (negative) {
            "170141183460469231731687303715884105728"
        } else {
            "170141183460469231731687303715884105727"
        }
    if (magnitude.length > maximum.length || (magnitude.length == maximum.length && magnitude > maximum)) {
        return SerdeResult.failure(
            SerdeError.custom(JsonError.syntax(ErrorCode.NumberOutOfRange, 0, 0).toString()),
        )
    }
    return SerdeResult.success(if (negative && magnitude != "0") "-$magnitude" else magnitude)
}

/**
 * A structure for serializing values into JSON.
 */
internal class JsonSerializer(
    internal val writer: IoWrite,
    internal val formatter: Formatter,
) : Serializer<Unit> {
    constructor(writer: IoWrite, pretty: Boolean = false) : this(
        writer,
        if (pretty) PrettyFormatter.new() else CompactFormatter(),
    )

    /** Unwrap the [IoWrite] from the [JsonSerializer]. */
    fun intoInner(): IoWrite = writer

    override fun serializeBool(v: Boolean): SerdeResult<Unit> =
        ioToSerde { formatter.writeBool(writer, v) }

    override fun serializeI8(v: Byte): SerdeResult<Unit> =
        ioToSerde { formatter.writeI8(writer, v) }

    override fun serializeI16(v: Short): SerdeResult<Unit> =
        ioToSerde { formatter.writeI16(writer, v) }

    override fun serializeI32(v: Int): SerdeResult<Unit> =
        ioToSerde { formatter.writeI32(writer, v) }

    override fun serializeI64(v: Long): SerdeResult<Unit> =
        ioToSerde { formatter.writeI64(writer, v) }

    override fun serializeI128(value: String): SerdeResult<Unit> {
        val normalized = normalizeWideInteger(value, signed = true).getOrElse { return SerdeResult.failure(it) }
        return ioToSerde { formatter.writeI128(writer, normalized) }
    }

    override fun serializeU8(v: UByte): SerdeResult<Unit> =
        ioToSerde { formatter.writeU8(writer, v) }

    override fun serializeU16(v: UShort): SerdeResult<Unit> =
        ioToSerde { formatter.writeU16(writer, v) }

    override fun serializeU32(v: UInt): SerdeResult<Unit> =
        ioToSerde { formatter.writeU32(writer, v) }

    override fun serializeU64(v: ULong): SerdeResult<Unit> =
        ioToSerde { formatter.writeU64(writer, v) }

    override fun serializeU128(value: String): SerdeResult<Unit> {
        val normalized = normalizeWideInteger(value, signed = false).getOrElse { return SerdeResult.failure(it) }
        return ioToSerde { formatter.writeU128(writer, normalized) }
    }

    override fun serializeF32(v: Float): SerdeResult<Unit> =
        if (!v.isFinite()) {
            ioToSerde { formatter.writeNull(writer) }
        } else {
            ioToSerde { formatter.writeF32(writer, v) }
        }

    override fun serializeF64(v: Double): SerdeResult<Unit> =
        if (!v.isFinite()) {
            ioToSerde { formatter.writeNull(writer) }
        } else {
            ioToSerde { formatter.writeF64(writer, v) }
        }

    override fun serializeChar(v: Char): SerdeResult<Unit> =
        serializeStr(charArrayOf(v).concatToString())

    override fun serializeStr(v: String): SerdeResult<Unit> =
        ioToSerde { formatEscapedStr(writer, formatter, v) }

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> =
        ioToSerde { formatter.writeByteArray(writer, v) }

    override fun serializeUnit(): SerdeResult<Unit> =
        ioToSerde { formatter.writeNull(writer) }

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = serializeUnit()

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> = serializeStr(variant)

    override fun <T : Serialize> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit> = value.serialize(this)

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit> = serializeNewtypeVariantEntry(variant, value)

    private fun <T : Serialize> serializeNewtypeVariantEntry(
        variant: String,
        value: T,
    ): SerdeResult<Unit> {
        ioToSerde { formatter.beginObject(writer) }.getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.beginObjectKey(writer, first = true) }.getOrElse { return SerdeResult.failure(it) }
        serializeStr(variant).getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.endObjectKey(writer) }.getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.beginObjectValue(writer) }.getOrElse { return SerdeResult.failure(it) }
        value.serialize(this).getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.endObjectValue(writer) }.getOrElse { return SerdeResult.failure(it) }
        return ioToSerde { formatter.endObject(writer) }
    }

    override fun serializeNone(): SerdeResult<Unit> = serializeUnit()

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<Unit> = value.serialize(this)

    internal fun beginSeqCompound(
        len: Int?,
        kind: CompoundKind,
    ): SerdeResult<Compound> {
        ioToSerde { formatter.beginArray(writer) }.getOrElse { return SerdeResult.failure(it) }
        return if (len == 0) {
            ioToSerde { formatter.endArray(writer) }.getOrElse { return SerdeResult.failure(it) }
            SerdeResult.success(Compound.Map(this, State.Empty, kind))
        } else {
            SerdeResult.success(Compound.Map(this, State.First, kind))
        }
    }

    internal fun beginMapCompound(
        len: Int?,
        kind: CompoundKind,
    ): SerdeResult<Compound> {
        ioToSerde { formatter.beginObject(writer) }.getOrElse { return SerdeResult.failure(it) }
        return if (len == 0) {
            ioToSerde { formatter.endObject(writer) }.getOrElse { return SerdeResult.failure(it) }
            SerdeResult.success(Compound.Map(this, State.Empty, kind))
        } else {
            SerdeResult.success(Compound.Map(this, State.First, kind))
        }
    }

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> =
        beginSeqCompound(len, CompoundKind.Seq).map { it }

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> =
        beginSeqCompound(len, CompoundKind.Seq).map { it }

    override fun serializeTupleStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeTupleStruct<Unit>> =
        beginSeqCompound(len, CompoundKind.Seq).map { it }

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> = serializeTupleVariantEntry(variant, len)

    private fun serializeTupleVariantEntry(
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> {
        ioToSerde { formatter.beginObject(writer) }.getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.beginObjectKey(writer, first = true) }.getOrElse { return SerdeResult.failure(it) }
        serializeStr(variant).getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.endObjectKey(writer) }.getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.beginObjectValue(writer) }.getOrElse { return SerdeResult.failure(it) }
        return beginSeqCompound(len, CompoundKind.TupleVariant).map { it }
    }

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> =
        beginMapCompound(len, CompoundKind.Map).map { it }

    override fun serializeStruct(
        name: String,
        len: Int,
    ): SerdeResult<SerializeStruct<Unit>> =
        when (name) {
            NUMBER_TOKEN -> SerdeResult.success(Compound.Number(this))
            RawValue.TOKEN -> SerdeResult.success(Compound.RawValue(this))
            else -> beginMapCompound(len, CompoundKind.Map).map { it }
        }

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> = serializeStructVariantEntry(variant, len)

    private fun serializeStructVariantEntry(
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> {
        ioToSerde { formatter.beginObject(writer) }.getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.beginObjectKey(writer, first = true) }.getOrElse { return SerdeResult.failure(it) }
        serializeStr(variant).getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.endObjectKey(writer) }.getOrElse { return SerdeResult.failure(it) }
        ioToSerde { formatter.beginObjectValue(writer) }.getOrElse { return SerdeResult.failure(it) }
        return beginMapCompound(len, CompoundKind.StructVariant).map { it }
    }

    override fun collectStr(value: Any): SerdeResult<Unit> = serializeStr(value.toString())

    companion object {
        /** Creates a new JSON serializer. */
        fun new(writer: IoWrite): JsonSerializer = withFormatter(writer, CompactFormatter())

        /** Creates a new JSON pretty print serializer. */
        fun pretty(writer: IoWrite): JsonSerializer = withFormatter(writer, PrettyFormatter.new())

        /** Creates a new JSON serializer whose output will be written to the specified writer. */
        fun withFormatter(writer: IoWrite, formatter: Formatter): JsonSerializer =
            JsonSerializer(writer, formatter)
    }
}

/**
 * Compound serialization state.
 */
internal enum class State {
    Empty,
    First,
    Rest,
}

internal enum class CompoundKind {
    Seq,
    TupleVariant,
    Map,
    StructVariant,
}

/**
 * Compound serializer state machine for sequences, tuples, maps, structs, and enum variants.
 */
internal sealed class Compound :
    SerializeSeq<Unit>,
    SerializeTuple<Unit>,
    SerializeTupleStruct<Unit>,
    SerializeTupleVariant<Unit>,
    SerializeMap<Unit>,
    SerializeStruct<Unit>,
    SerializeStructVariant<Unit> {
    class Map(
        val ser: JsonSerializer,
        var state: State,
        val kind: CompoundKind,
    ) : Compound()

    class Number(
        val ser: JsonSerializer,
        var written: Boolean = false,
    ) : Compound()

    class RawValue(
        val ser: JsonSerializer,
        var written: Boolean = false,
    ) : Compound()

    override fun <T : Serialize> serializeElement(value: T): SerdeResult<Unit> =
        when (this) {
            is Map -> {
                ioToSerde {
                    ser.formatter.beginArrayValue(ser.writer, first = state == State.First)
                }.getOrElse { return SerdeResult.failure(it) }
                state = State.Rest
                value.serialize(ser).getOrElse { return SerdeResult.failure(it) }
                ioToSerde { ser.formatter.endArrayValue(ser.writer) }
            }

            is Number -> {
                kotlin.error("unreachable")
            }

            is RawValue -> {
                kotlin.error("unreachable")
            }
        }

    override fun <T : Serialize> serializeField(value: T): SerdeResult<Unit> = serializeElement(value)

    override fun <T : Serialize> serializeKey(key: T): SerdeResult<Unit> =
        when (this) {
            is Map -> {
                ioToSerde {
                    ser.formatter.beginObjectKey(ser.writer, first = state == State.First)
                }.getOrElse { return SerdeResult.failure(it) }
                state = State.Rest
                key.serialize(StreamMapKeySerializer(ser)).getOrElse { return SerdeResult.failure(it) }
                ioToSerde { ser.formatter.endObjectKey(ser.writer) }
            }

            is Number -> {
                kotlin.error("unreachable")
            }

            is RawValue -> {
                kotlin.error("unreachable")
            }
        }

    override fun <T : Serialize> serializeValue(value: T): SerdeResult<Unit> =
        when (this) {
            is Map -> {
                ioToSerde { ser.formatter.beginObjectValue(ser.writer) }.getOrElse { return SerdeResult.failure(it) }
                value.serialize(ser).getOrElse { return SerdeResult.failure(it) }
                ioToSerde { ser.formatter.endObjectValue(ser.writer) }
            }

            is Number -> {
                kotlin.error("unreachable")
            }

            is RawValue -> {
                kotlin.error("unreachable")
            }
        }

    override fun <K : Serialize, V : Serialize> serializeEntry(key: K, value: V): SerdeResult<Unit> {
        serializeKey(key).getOrElse { return SerdeResult.failure(it) }
        return serializeValue(value)
    }

    override fun <T : Serialize> serializeField(key: String, value: T): SerdeResult<Unit> =
        when (this) {
            is Map -> {
                serializeEntry(Value.Str(key), value)
            }

            is Number -> {
                if (key == NUMBER_TOKEN && !written) {
                    value.serialize(NumberStrEmitter(ser)).also {
                        if (it.isSuccess) written = true
                    }
                } else {
                    SerdeResult.failure(invalidNumberError())
                }
            }

            is RawValue -> {
                if (key == io.github.kotlinmania.serdejson.RawValue.TOKEN && !written) {
                    value.serialize(RawValueStrEmitter(ser)).also {
                        if (it.isSuccess) written = true
                    }
                } else {
                    SerdeResult.failure(invalidRawValueError())
                }
            }
        }

    override fun skipField(key: String): SerdeResult<Unit> = SerdeResult.success(Unit)

    override fun end(): SerdeResult<Unit> =
        when (this) {
            is Map -> {
                when (kind) {
                    CompoundKind.Seq -> {
                        if (state == State.Empty) {
                            SerdeResult.success(Unit)
                        } else {
                            ioToSerde { ser.formatter.endArray(ser.writer) }
                        }
                    }

                    CompoundKind.TupleVariant -> {
                        if (state != State.Empty) {
                            ioToSerde { ser.formatter.endArray(ser.writer) }.getOrElse { return SerdeResult.failure(it) }
                        }
                        ioToSerde { ser.formatter.endObjectValue(ser.writer) }.getOrElse { return SerdeResult.failure(it) }
                        ioToSerde { ser.formatter.endObject(ser.writer) }
                    }

                    CompoundKind.Map -> {
                        if (state == State.Empty) {
                            SerdeResult.success(Unit)
                        } else {
                            ioToSerde { ser.formatter.endObject(ser.writer) }
                        }
                    }

                    CompoundKind.StructVariant -> {
                        if (state != State.Empty) {
                            ioToSerde { ser.formatter.endObject(ser.writer) }.getOrElse { return SerdeResult.failure(it) }
                        }
                        ioToSerde { ser.formatter.endObjectValue(ser.writer) }.getOrElse { return SerdeResult.failure(it) }
                        ioToSerde { ser.formatter.endObject(ser.writer) }
                    }
                }
            }

            is Number -> {
                if (written) SerdeResult.success(Unit) else SerdeResult.failure(invalidNumberError())
            }

            is RawValue -> {
                if (written) SerdeResult.success(Unit) else SerdeResult.failure(invalidRawValueError())
            }
        }
}

private fun invalidNumberError(): SerdeError =
    SerdeError.custom(JsonError.syntax(ErrorCode.InvalidNumber, 0, 0).toString())

private fun invalidRawValueError(): SerdeError =
    SerdeError.custom(JsonError.syntax(ErrorCode.ExpectedSomeValue, 0, 0).toString())

private fun keyMustBeAStringError(): SerdeError =
    SerdeError.custom(JsonError.syntax(ErrorCode.KeyMustBeAString, 0, 0).toString())

private fun floatKeyMustBeFiniteError(): SerdeError =
    SerdeError.custom(JsonError.syntax(ErrorCode.FloatKeyMustBeFinite, 0, 0).toString())

internal class StreamMapKeySerializer(
    private val ser: JsonSerializer,
) : Serializer<Unit> {
    private inline fun writeQuotedScalar(writeValue: () -> IoResult<Unit>): SerdeResult<Unit> {
        ioToSerde { ser.formatter.beginString(ser.writer) }.getOrElse { return SerdeResult.failure(it) }
        ioToSerde(writeValue).getOrElse { return SerdeResult.failure(it) }
        return ioToSerde { ser.formatter.endString(ser.writer) }
    }

    override fun serializeStr(v: String): SerdeResult<Unit> = ser.serializeStr(v)

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> = ser.serializeStr(variant)

    override fun <T : Serialize> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit> = value.serialize(this)

    override fun serializeBool(v: Boolean): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeBool(ser.writer, v) }

    override fun serializeI8(v: Byte): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeI8(ser.writer, v) }

    override fun serializeI16(v: Short): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeI16(ser.writer, v) }

    override fun serializeI32(v: Int): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeI32(ser.writer, v) }

    override fun serializeI64(v: Long): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeI64(ser.writer, v) }

    override fun serializeI128(value: String): SerdeResult<Unit> {
        val normalized = normalizeWideInteger(value, signed = true).getOrElse { return SerdeResult.failure(it) }
        return writeQuotedScalar { ser.formatter.writeI128(ser.writer, normalized) }
    }

    override fun serializeU8(v: UByte): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeU8(ser.writer, v) }

    override fun serializeU16(v: UShort): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeU16(ser.writer, v) }

    override fun serializeU32(v: UInt): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeU32(ser.writer, v) }

    override fun serializeU64(v: ULong): SerdeResult<Unit> =
        writeQuotedScalar { ser.formatter.writeU64(ser.writer, v) }

    override fun serializeU128(value: String): SerdeResult<Unit> {
        val normalized = normalizeWideInteger(value, signed = false).getOrElse { return SerdeResult.failure(it) }
        return writeQuotedScalar { ser.formatter.writeU128(ser.writer, normalized) }
    }

    override fun serializeF32(v: Float): SerdeResult<Unit> =
        if (!v.isFinite()) {
            SerdeResult.failure(floatKeyMustBeFiniteError())
        } else {
            writeQuotedScalar { ser.formatter.writeF32(ser.writer, v) }
        }

    override fun serializeF64(v: Double): SerdeResult<Unit> =
        if (!v.isFinite()) {
            SerdeResult.failure(floatKeyMustBeFiniteError())
        } else {
            writeQuotedScalar { ser.formatter.writeF64(ser.writer, v) }
        }

    override fun serializeChar(v: Char): SerdeResult<Unit> =
        ser.serializeStr(charArrayOf(v).concatToString())

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun serializeUnit(): SerdeResult<Unit> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit> = SerdeResult.failure(keyMustBeAStringError())

    override fun serializeNone(): SerdeResult<Unit> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<Unit> =
        value.serialize(this)

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Unit>> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> = SerdeResult.failure(keyMustBeAStringError())

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Unit>> =
        SerdeResult.failure(keyMustBeAStringError())

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> = SerdeResult.failure(keyMustBeAStringError())

    override fun collectStr(value: Any): SerdeResult<Unit> = ser.collectStr(value)
}

internal class NumberStrEmitter(
    private val ser: JsonSerializer,
) : Serializer<Unit> {
    override fun serializeBool(v: Boolean): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeI8(v: Byte): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeI16(v: Short): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeI32(v: Int): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeI64(v: Long): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeI128(value: String): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeU8(v: UByte): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeU16(v: UShort): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeU32(v: UInt): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeU64(v: ULong): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeU128(value: String): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeF32(v: Float): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeF64(v: Double): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeChar(v: Char): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeStr(v: String): SerdeResult<Unit> =
        ioToSerde { ser.formatter.writeNumberStr(ser.writer, v) }

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeNone(): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeUnit(): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun <T : Serialize> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit> = SerdeResult.failure(invalidNumberError())

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> = SerdeResult.failure(invalidNumberError())

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> = SerdeResult.failure(invalidNumberError())

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Unit>> =
        SerdeResult.failure(invalidNumberError())

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> = SerdeResult.failure(invalidNumberError())

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> = SerdeResult.failure(invalidNumberError())

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Unit>> =
        SerdeResult.failure(invalidNumberError())

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> = SerdeResult.failure(invalidNumberError())
}

internal class RawValueStrEmitter(
    private val ser: JsonSerializer,
) : Serializer<Unit> {
    private fun expectedRawValueError(): SerdeError = SerdeError.custom("expected RawValue")

    override fun serializeBool(v: Boolean): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeI8(v: Byte): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeI16(v: Short): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeI32(v: Int): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeI64(v: Long): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeI128(value: String): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeU8(v: UByte): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeU16(v: UShort): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeU32(v: UInt): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeU64(v: ULong): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeU128(value: String): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeF32(v: Float): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeF64(v: Double): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeChar(v: Char): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeStr(v: String): SerdeResult<Unit> {
        fromStr(v).toSerdeResult().getOrElse { return SerdeResult.failure(it) }
        return ioToSerde { ser.formatter.writeRawFragment(ser.writer, v) }
    }

    override fun serializeBytes(v: ByteArray): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeNone(): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun <T : Serialize> serializeSome(value: T): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeUnit(): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeUnitStruct(name: String): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeUnitVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
    ): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun <T : Serialize> serializeNewtypeStruct(
        name: String,
        value: T,
    ): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun <T : Serialize> serializeNewtypeVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        value: T,
    ): SerdeResult<Unit> = SerdeResult.failure(expectedRawValueError())

    override fun serializeSeq(len: Int?): SerdeResult<SerializeSeq<Unit>> = SerdeResult.failure(expectedRawValueError())

    override fun serializeTuple(len: Int): SerdeResult<SerializeTuple<Unit>> = SerdeResult.failure(expectedRawValueError())

    override fun serializeTupleStruct(name: String, len: Int): SerdeResult<SerializeTupleStruct<Unit>> =
        SerdeResult.failure(expectedRawValueError())

    override fun serializeTupleVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeTupleVariant<Unit>> = SerdeResult.failure(expectedRawValueError())

    override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Unit>> = SerdeResult.failure(expectedRawValueError())

    override fun serializeStruct(name: String, len: Int): SerdeResult<SerializeStruct<Unit>> =
        SerdeResult.failure(expectedRawValueError())

    override fun serializeStructVariant(
        name: String,
        variantIndex: UInt,
        variant: String,
        len: Int,
    ): SerdeResult<SerializeStructVariant<Unit>> = SerdeResult.failure(expectedRawValueError())

    override fun collectStr(value: Any): SerdeResult<Unit> = serializeStr(value.toString())
}

/**
 * Represents a character escape code in a type-safe manner.
 */
sealed class CharEscape {
    /** An escaped quote `"`. */
    object Quote : CharEscape()

    /** An escaped reverse solidus `\`. */
    object ReverseSolidus : CharEscape()

    /** An escaped solidus `/`. */
    object Solidus : CharEscape()

    /** An escaped backspace character (usually escaped as `\b`). */
    object Backspace : CharEscape()

    /** An escaped form feed character (usually escaped as `\f`). */
    object FormFeed : CharEscape()

    /** An escaped line feed character (usually escaped as `\n`). */
    object LineFeed : CharEscape()

    /** An escaped carriage return character (usually escaped as `\r`). */
    object CarriageReturn : CharEscape()

    /** An escaped tab character (usually escaped as `\t`). */
    object Tab : CharEscape()

    /** An escaped ASCII plane control character (usually escaped as `\u00XX`). */
    class AsciiControl(
        val byte: UByte,
    ) : CharEscape()
}

/**
 * Abstracts away serializing JSON control characters, allowing optional pretty-printing.
 */
internal interface Formatter {
    /** Writes a `null` value to the specified writer. */
    fun writeNull(writer: IoWrite): IoResult<Unit> = writer.writeAll(NULL_BYTES)

    /** Writes a `true` or `false` value to the specified writer. */
    fun writeBool(writer: IoWrite, value: Boolean): IoResult<Unit> =
        writer.writeAll(if (value) TRUE_BYTES else FALSE_BYTES)

    /** Writes a signed 8-bit integer value to the specified writer. */
    fun writeI8(writer: IoWrite, value: Byte): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes a signed 16-bit integer value to the specified writer. */
    fun writeI16(writer: IoWrite, value: Short): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes a signed 32-bit integer value to the specified writer. */
    fun writeI32(writer: IoWrite, value: Int): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes a signed 64-bit integer value to the specified writer. */
    fun writeI64(writer: IoWrite, value: Long): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes a signed 128-bit integer value to the specified writer. */
    fun writeI128(writer: IoWrite, value: String): IoResult<Unit> =
        writer.writeAll(value.encodeToByteArray())

    /** Writes an unsigned 8-bit integer value to the specified writer. */
    fun writeU8(writer: IoWrite, value: UByte): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes an unsigned 16-bit integer value to the specified writer. */
    fun writeU16(writer: IoWrite, value: UShort): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes an unsigned 32-bit integer value to the specified writer. */
    fun writeU32(writer: IoWrite, value: UInt): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes an unsigned 64-bit integer value to the specified writer. */
    fun writeU64(writer: IoWrite, value: ULong): IoResult<Unit> =
        writer.writeAll(value.toString().encodeToByteArray())

    /** Writes an unsigned 128-bit integer value to the specified writer. */
    fun writeU128(writer: IoWrite, value: String): IoResult<Unit> =
        writer.writeAll(value.encodeToByteArray())

    /** Writes a finite 32-bit floating point value to the specified writer. */
    fun writeF32(writer: IoWrite, value: Float): IoResult<Unit> =
        writer.writeAll(formatFiniteFloat(value).encodeToByteArray())

    /** Writes a finite 64-bit floating point value to the specified writer. */
    fun writeF64(writer: IoWrite, value: Double): IoResult<Unit> =
        writer.writeAll(formatFinite(value).encodeToByteArray())

    /** Writes a number that has already been rendered to a string. */
    fun writeNumberStr(writer: IoWrite, value: String): IoResult<Unit> =
        writer.writeAll(value.encodeToByteArray())

    /** Called before each series of [writeStringFragment] and [writeCharEscape]. */
    fun beginString(writer: IoWrite): IoResult<Unit> = writer.writeAll(QUOTE_BYTES)

    /** Called after each series of [writeStringFragment] and [writeCharEscape]. */
    fun endString(writer: IoWrite): IoResult<Unit> = writer.writeAll(QUOTE_BYTES)

    /** Writes a string fragment that does not need any escaping to the specified writer. */
    fun writeStringFragment(writer: IoWrite, fragment: String): IoResult<Unit> =
        writer.writeAll(fragment.encodeToByteArray())

    /** Writes a character escape code to the specified writer. */
    fun writeCharEscape(writer: IoWrite, charEscape: CharEscape): IoResult<Unit> {
        val escapeChar: Byte =
            when (charEscape) {
                CharEscape.Quote -> '"'.code.toByte()
                CharEscape.ReverseSolidus -> '\\'.code.toByte()
                CharEscape.Solidus -> '/'.code.toByte()
                CharEscape.Backspace -> 'b'.code.toByte()
                CharEscape.FormFeed -> 'f'.code.toByte()
                CharEscape.LineFeed -> 'n'.code.toByte()
                CharEscape.CarriageReturn -> 'r'.code.toByte()
                CharEscape.Tab -> 't'.code.toByte()
                is CharEscape.AsciiControl -> 'u'.code.toByte()
            }

        return when (charEscape) {
            is CharEscape.AsciiControl -> {
                val b = charEscape.byte.toInt() and 0xFF
                val bytes =
                    byteArrayOf(
                        '\\'.code.toByte(),
                        escapeChar,
                        '0'.code.toByte(),
                        '0'.code.toByte(),
                        HEX_DIGITS[b ushr 4],
                        HEX_DIGITS[b and 0x0F],
                    )
                writer.writeAll(bytes)
            }

            else -> {
                writer.writeAll(byteArrayOf('\\'.code.toByte(), escapeChar))
            }
        }
    }

    /** Writes the representation of a byte array as a JSON array of unsigned integers. */
    fun writeByteArray(writer: IoWrite, value: ByteArray): IoResult<Unit> {
        val begin = beginArray(writer)
        if (begin is IoResult.Err) return begin
        var first = true
        for (byte in value) {
            val beginVal = beginArrayValue(writer, first)
            if (beginVal is IoResult.Err) return beginVal
            val writeVal = writeU8(writer, byte.toUByte())
            if (writeVal is IoResult.Err) return writeVal
            val endVal = endArrayValue(writer)
            if (endVal is IoResult.Err) return endVal
            first = false
        }
        return endArray(writer)
    }

    /** Called before every array. Writes `[` to the specified writer. */
    fun beginArray(writer: IoWrite): IoResult<Unit> = writer.writeAll(OPEN_BRACKET_BYTES)

    /** Called after every array. Writes `]` to the specified writer. */
    fun endArray(writer: IoWrite): IoResult<Unit> = writer.writeAll(CLOSE_BRACKET_BYTES)

    /** Called before every array value. Writes `,` if needed to the specified writer. */
    fun beginArrayValue(writer: IoWrite, first: Boolean): IoResult<Unit> =
        if (first) IoResult.ok(Unit) else writer.writeAll(COMMA_BYTES)

    /** Called after every array value. */
    fun endArrayValue(writer: IoWrite): IoResult<Unit> = IoResult.ok(Unit)

    /** Called before every object. Writes `{` to the specified writer. */
    fun beginObject(writer: IoWrite): IoResult<Unit> = writer.writeAll(OPEN_BRACE_BYTES)

    /** Called after every object. Writes `}` to the specified writer. */
    fun endObject(writer: IoWrite): IoResult<Unit> = writer.writeAll(CLOSE_BRACE_BYTES)

    /** Called before every object key. */
    fun beginObjectKey(writer: IoWrite, first: Boolean): IoResult<Unit> =
        if (first) IoResult.ok(Unit) else writer.writeAll(COMMA_BYTES)

    /** Called after every object key. */
    fun endObjectKey(writer: IoWrite): IoResult<Unit> = IoResult.ok(Unit)

    /** Called before every object value. Writes `:` to the specified writer. */
    fun beginObjectValue(writer: IoWrite): IoResult<Unit> = writer.writeAll(COLON_BYTES)

    /** Called after every object value. */
    fun endObjectValue(writer: IoWrite): IoResult<Unit> = IoResult.ok(Unit)

    /** Writes a raw JSON fragment that does not need any escaping to the specified writer. */
    fun writeRawFragment(writer: IoWrite, fragment: String): IoResult<Unit> =
        writer.writeAll(fragment.encodeToByteArray())
}

/**
 * Compacts a JSON value with no extra whitespace.
 */
internal class CompactFormatter : Formatter {
    companion object {
        fun default(): CompactFormatter = CompactFormatter()
    }
}

/**
 * Pretty-prints a JSON value to make it human-readable.
 */
internal class PrettyFormatter(
    private val indent: ByteArray = DEFAULT_INDENT_BYTES,
) : Formatter {
    private var currentIndent: Int = 0
    private var hasValue: Boolean = false

    override fun beginArray(writer: IoWrite): IoResult<Unit> {
        currentIndent += 1
        hasValue = false
        return writer.writeAll(OPEN_BRACKET_BYTES)
    }

    override fun endArray(writer: IoWrite): IoResult<Unit> {
        currentIndent -= 1
        if (hasValue) {
            val nl = writer.writeAll(NEWLINE_BYTES)
            if (nl is IoResult.Err) return nl
            val ind = indent(writer, currentIndent, indent)
            if (ind is IoResult.Err) return ind
        }
        return writer.writeAll(CLOSE_BRACKET_BYTES)
    }

    override fun beginArrayValue(writer: IoWrite, first: Boolean): IoResult<Unit> {
        val sep = writer.writeAll(if (first) NEWLINE_BYTES else COMMA_NEWLINE_BYTES)
        if (sep is IoResult.Err) return sep
        return indent(writer, currentIndent, indent)
    }

    override fun endArrayValue(writer: IoWrite): IoResult<Unit> =
        IoResult.ok(Unit).also { hasValue = true }

    override fun beginObject(writer: IoWrite): IoResult<Unit> {
        currentIndent += 1
        hasValue = false
        return writer.writeAll(OPEN_BRACE_BYTES)
    }

    override fun endObject(writer: IoWrite): IoResult<Unit> {
        currentIndent -= 1
        if (hasValue) {
            val nl = writer.writeAll(NEWLINE_BYTES)
            if (nl is IoResult.Err) return nl
            val ind = indent(writer, currentIndent, indent)
            if (ind is IoResult.Err) return ind
        }
        return writer.writeAll(CLOSE_BRACE_BYTES)
    }

    override fun beginObjectKey(writer: IoWrite, first: Boolean): IoResult<Unit> {
        val sep = writer.writeAll(if (first) NEWLINE_BYTES else COMMA_NEWLINE_BYTES)
        if (sep is IoResult.Err) return sep
        return indent(writer, currentIndent, indent)
    }

    override fun beginObjectValue(writer: IoWrite): IoResult<Unit> =
        writer.writeAll(COLON_SPACE_BYTES)

    override fun endObjectValue(writer: IoWrite): IoResult<Unit> =
        IoResult.ok(Unit).also { hasValue = true }

    companion object {
        /** Construct a pretty printer formatter that defaults to using two spaces for indentation. */
        fun new(): PrettyFormatter = withIndent(DEFAULT_INDENT_BYTES)

        /** Construct a pretty printer formatter that uses the [indent] byte sequence for indentation. */
        fun withIndent(indent: ByteArray): PrettyFormatter = PrettyFormatter(indent)

        /** Default pretty printer formatter using two spaces for indentation. */
        fun default(): PrettyFormatter = new()
    }
}

internal fun formatEscapedStr(
    writer: IoWrite,
    formatter: Formatter,
    value: String,
): IoResult<Unit> {
    val begin = formatter.beginString(writer)
    if (begin is IoResult.Err) return begin
    val contents = formatEscapedStrContents(writer, formatter, value)
    if (contents is IoResult.Err) return contents
    return formatter.endString(writer)
}

internal fun formatEscapedStrContents(
    writer: IoWrite,
    formatter: Formatter,
    value: String,
): IoResult<Unit> {
    val bytes = value.encodeToByteArray()
    var start = 0
    var i = 0
    while (i < bytes.size) {
        val byte = bytes[i].toInt() and 0xFF
        val escape = ESCAPE[byte]
        if (escape == 0) {
            i += 1
            continue
        }

        if (start < i) {
            // Safe to decode as UTF-8: ESCAPE is non-zero only for ASCII bytes (< 0x80),
            // so start and i are always valid UTF-8 codepoint boundaries.
            val stringRun = bytes.decodeToString(start, i)
            val fragResult = formatter.writeStringFragment(writer, stringRun)
            if (fragResult is IoResult.Err) return fragResult
        }

        val charEscape =
            when (escape) {
                BB -> CharEscape.Backspace
                TT -> CharEscape.Tab
                NN -> CharEscape.LineFeed
                FF -> CharEscape.FormFeed
                RR -> CharEscape.CarriageReturn
                QU -> CharEscape.Quote
                BS -> CharEscape.ReverseSolidus
                UU -> CharEscape.AsciiControl(byte.toUByte())
                else -> kotlin.error("unreachable escape entry")
            }
        val escResult = formatter.writeCharEscape(writer, charEscape)
        if (escResult is IoResult.Err) return escResult

        i += 1
        start = i
    }

    if (start == bytes.size) {
        return IoResult.ok(Unit)
    }

    val tailRun = if (start == 0) value else bytes.decodeToString(start, bytes.size)
    return formatter.writeStringFragment(writer, tailRun)
}

private const val BB: Int = 'b'.code
private const val TT: Int = 't'.code
private const val NN: Int = 'n'.code
private const val FF: Int = 'f'.code
private const val RR: Int = 'r'.code
private const val QU: Int = '"'.code
private const val BS: Int = '\\'.code
private const val UU: Int = 'u'.code
private const val NONE_ESC: Int = 0

// Lookup table of escape sequences. A non-zero value at index i indicates that byte
// i is escaped in JSON; 0 indicates that byte i is written unescaped.
private val ESCAPE: IntArray =
    intArrayOf(
        //   0        1        2        3        4        5        6        7        8        9        A        B        C        D        E        F
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        BB,
        TT,
        NN,
        UU,
        FF,
        RR,
        UU,
        UU, // 0
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU,
        UU, // 1
        NONE_ESC,
        NONE_ESC,
        QU,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 2
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 3
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 4
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        BS,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 5
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 6
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 7
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 8
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // 9
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // A
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // B
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // C
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // D
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // E
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC,
        NONE_ESC, // F
    )

/**
 * Serialize the given data structure as JSON into the I/O stream.
 */
internal fun toWriter(writer: IoWrite, value: Serialize): SerdeResult<Unit> =
    value.serialize(JsonSerializer.new(writer))

/**
 * Serialize the given data structure as pretty-printed JSON into the I/O stream.
 */
internal fun toWriterPretty(writer: IoWrite, value: Serialize): SerdeResult<Unit> =
    value.serialize(JsonSerializer.pretty(writer))

/**
 * Serialize the given data structure as a JSON byte array.
 */
fun toVec(value: Serialize): JsonBytesOutcome =
    JsonBytesOutcome.fromResult(
        serdeCatching {
            val writer = VecIoWrite()
            toWriter(writer, value).getOrThrow()
            writer.bytes
        },
    )

/**
 * Serialize the given data structure as a pretty-printed JSON byte array.
 */
fun toVecPretty(value: Serialize): JsonBytesOutcome =
    JsonBytesOutcome.fromResult(
        serdeCatching {
            val writer = VecIoWrite()
            toWriterPretty(writer, value).getOrThrow()
            writer.bytes
        },
    )

/**
 * Serialize the given data structure as a String of JSON.
 */
fun toStr(value: Serialize): JsonStringOutcome =
    JsonStringOutcome.fromResult(
        toVec(value).toSerdeResult().map { it.decodeToString() },
    )

/**
 * Serialize the given data structure as a pretty-printed String of JSON.
 */
fun toStrPretty(value: Serialize): JsonStringOutcome =
    JsonStringOutcome.fromResult(
        toVecPretty(value).toSerdeResult().map { it.decodeToString() },
    )

/**
 * Serialize a [Value] to a JSON string.
 */
fun valueToStr(value: Value): String = toStr(value).getOrThrow()

/**
 * Serialize a [Value] to a pretty JSON string.
 */
fun valueToStrPretty(value: Value): String = toStrPretty(value).getOrThrow()

/**
 * Serialize a [Value] directly to an [IoWrite].
 */
internal fun toJsonString(output: IoWrite, value: Value, pretty: Boolean = false) {
    if (pretty) {
        toWriterPretty(output, value).getOrThrow()
    } else {
        toWriter(output, value).getOrThrow()
    }
}

internal fun indent(wr: IoWrite, n: Int, s: ByteArray): IoResult<Unit> {
    repeat(n) {
        val res = wr.writeAll(s)
        if (res is IoResult.Err) return res
    }
    return IoResult.ok(Unit)
}
