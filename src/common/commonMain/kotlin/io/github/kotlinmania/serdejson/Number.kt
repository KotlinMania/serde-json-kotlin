// port-lint: source number.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * Represents a JSON number, whether integer or floating point.
 */
class JsonNumber private constructor(
    private val n: N,
) : Serialize,
    Deserializer {
    /**
     * The internal representation of a JSON number.
     */
    private sealed class N {
        class PosInt(
            val value: ULong,
        ) : N()

        class NegInt(
            val value: Long,
        ) : N()

        class Float(
            val value: Double,
        ) : N()
    }

    companion object : Deserialize<JsonNumber> {
        /** Converts a finite [Double] to a [JsonNumber]. Infinite or NaN values are not JSON numbers. */
        fun fromF64(f: Double): JsonNumber? =
            if (f.isFinite()) {
                JsonNumber(N.Float(f))
            } else {
                null
            }

        /** Converts a finite [Float] to a [JsonNumber]. */
        fun fromF32(f: Float): JsonNumber? =
            if (f.isFinite()) {
                JsonNumber(N.Float(f.toDouble()))
            } else {
                null
            }

        /** Converts a [Long] to a [JsonNumber]. */
        fun fromI64(i: Long): JsonNumber =
            JsonNumber(
                if (i < 0) {
                    N.NegInt(i)
                } else {
                    N.PosInt(i.toULong())
                },
            )

        /** Converts a [ULong] to a [JsonNumber]. */
        fun fromU64(u: ULong): JsonNumber = JsonNumber(N.PosInt(u))

        /** Converts an [Int] to a [JsonNumber]. */
        fun fromI32(i: Int): JsonNumber = fromI64(i.toLong())

        /** Converts a [UInt] to a [JsonNumber]. */
        fun fromU32(u: UInt): JsonNumber = fromU64(u.toULong())

        /** Converts a signed 128-bit decimal integer representation to a [JsonNumber] if in range. */
        fun fromI128(i: String): JsonNumber? =
            if (i.startsWith('-')) {
                val parsed = i.toLongOrNull() ?: return null
                if (parsed < 0L) {
                    JsonNumber(N.NegInt(parsed))
                } else {
                    JsonNumber(N.PosInt(parsed.toULong()))
                }
            } else {
                val parsed = i.toULongOrNull() ?: return null
                JsonNumber(N.PosInt(parsed))
            }

        /** Converts an unsigned 128-bit decimal integer representation to a [JsonNumber] if in range. */
        fun fromU128(i: String): JsonNumber? {
            val parsed = i.toULongOrNull() ?: return null
            return JsonNumber(N.PosInt(parsed))
        }

        /** Creates a [JsonNumber] from a [ParserNumber]. */
        internal fun fromParserNumber(value: ParserNumber): JsonNumber =
            when (value) {
                is ParserNumber.F64 -> JsonNumber(N.Float(value.value))
                is ParserNumber.U64 -> JsonNumber(N.PosInt(value.value))
                is ParserNumber.I64 -> JsonNumber(N.NegInt(value.value))
            }

        /** Returns the [Unexpected] representation of this number. */
        internal fun unexpected(n: JsonNumber): Unexpected =
            when (val inner = n.n) {
                is N.PosInt -> Unexpected.Unsigned(inner.value)
                is N.NegInt -> Unexpected.Signed(inner.value)
                is N.Float -> Unexpected.FloatValue(inner.value)
            }

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<JsonNumber> =
            deserializer.deserializeAny(NumberVisitor)

        private object NumberVisitor : Visitor<JsonNumber> {
            override fun expecting(): String = "a JSON number"

            override fun visitI64(v: Long): SerdeResult<JsonNumber> =
                SerdeResult.success(fromI64(v))

            override fun visitI128(v: String): SerdeResult<JsonNumber> {
                val num =
                    fromI128(v)
                        ?: return SerdeResult.failure(SerdeError.custom("JSON number out of range"))
                return SerdeResult.success(num)
            }

            override fun visitU64(v: ULong): SerdeResult<JsonNumber> =
                SerdeResult.success(fromU64(v))

            override fun visitU128(v: String): SerdeResult<JsonNumber> {
                val num =
                    fromU128(v)
                        ?: return SerdeResult.failure(SerdeError.custom("JSON number out of range"))
                return SerdeResult.success(num)
            }

            override fun visitF64(v: Double): SerdeResult<JsonNumber> {
                val num =
                    fromF64(v)
                        ?: return SerdeResult.failure(SerdeError.custom("not a JSON number"))
                return SerdeResult.success(num)
            }
        }
    }

    /** Returns true if the [JsonNumber] is an integer between [Long.MIN_VALUE] and [Long.MAX_VALUE]. */
    fun isI64(): Boolean =
        when (n) {
            is N.PosInt -> n.value <= Long.MAX_VALUE.toULong()
            is N.NegInt -> true
            is N.Float -> false
        }

    /** Returns true if the [JsonNumber] is an integer between zero and [ULong.MAX_VALUE]. */
    fun isU64(): Boolean =
        when (n) {
            is N.PosInt -> true
            is N.NegInt, is N.Float -> false
        }

    /** Returns true if the [JsonNumber] can be represented by [Double]. */
    fun isF64(): Boolean =
        when (n) {
            is N.Float -> true
            is N.PosInt, is N.NegInt -> false
        }

    /** If the [JsonNumber] is an integer, represent it as [Long] if possible. Returns null otherwise. */
    fun asI64(): Long? =
        when (n) {
            is N.PosInt -> if (n.value <= Long.MAX_VALUE.toULong()) n.value.toLong() else null
            is N.NegInt -> n.value
            is N.Float -> null
        }

    /** If the [JsonNumber] is an integer, represent it as [ULong] if possible. Returns null otherwise. */
    fun asU64(): ULong? =
        when (n) {
            is N.PosInt -> n.value
            is N.NegInt, is N.Float -> null
        }

    /** Represents the number as [Double] if possible. Returns null otherwise. */
    fun asF64(): Double? =
        when (n) {
            is N.PosInt -> n.value.toDouble()
            is N.NegInt -> n.value.toDouble()
            is N.Float -> n.value
        }

    /** If the [JsonNumber] is an integer, represent it as a signed 128-bit decimal string. */
    fun asI128(): String? =
        when (n) {
            is N.PosInt -> n.value.toString()
            is N.NegInt -> n.value.toString()
            is N.Float -> null
        }

    /** If the [JsonNumber] is a non-negative integer, represent it as an unsigned 128-bit decimal string. */
    fun asU128(): String? =
        when (n) {
            is N.PosInt -> n.value.toString()
            is N.NegInt, is N.Float -> null
        }

    /** Represents the number as [Float] if possible. Returns null otherwise. */
    internal fun asF32(): Float? =
        when (n) {
            is N.PosInt -> n.value.toFloat()
            is N.NegInt -> n.value.toFloat()
            is N.Float -> n.value.toFloat()
        }

    /** Returns a copy of this [JsonNumber]. */
    fun clone(): JsonNumber = JsonNumber(n)

    /** Formats this [JsonNumber] for debug output. */
    fun fmtDebug(): String = "Number($this)"

    /** Returns this [JsonNumber] as a [Deserializer]. */
    fun intoDeserializer(): Deserializer = this

    override fun toString(): String =
        when (n) {
            is N.PosInt -> n.value.toString()
            is N.NegInt -> n.value.toString()
            is N.Float -> formatFinite(n.value)
        }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        when (n) {
            is N.PosInt -> serializer.serializeU64(n.value)
            is N.NegInt -> serializer.serializeI64(n.value)
            is N.Float -> serializer.serializeF64(n.value)
        }

    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> =
        when (n) {
            is N.PosInt -> visitor.visitU64(n.value)
            is N.NegInt -> visitor.visitI64(n.value)
            is N.Float -> visitor.visitF64(n.value)
        }

    override fun <V> deserializeBool(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeI128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU8(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU16(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeU128(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF32(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeF64(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeChar(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStr(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeString(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeBytes(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeByteBuf(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeOption(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnit(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeUnitStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeNewtypeStruct(name: String, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeSeq(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTuple(len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeTupleStruct(name: String, len: Int, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeStruct(name: String, fields: List<String>, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeEnum(name: String, variants: List<String>, visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIdentifier(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    override fun <V> deserializeIgnoredAny(visitor: Visitor<V>): SerdeResult<V> = deserializeAny(visitor)

    // Implementing reflexive equality is fine since any float values are always finite.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonNumber) return false
        val otherN = other.n
        return when (val thisN = n) {
            is N.PosInt -> otherN is N.PosInt && thisN.value == otherN.value
            is N.NegInt -> otherN is N.NegInt && thisN.value == otherN.value
            is N.Float -> otherN is N.Float && thisN.value == otherN.value
        }
    }

    override fun hashCode(): Int =
        when (n) {
            is N.PosInt -> n.value.hashCode()
            is N.NegInt -> n.value.hashCode()
            is N.Float -> if (n.value == 0.0) 0.0.toBits().hashCode() else n.value.toBits().hashCode()
        }
}

/**
 * A number as parsed by the JSON deserializer.
 */
sealed class ParserNumber {
    class F64(
        val value: Double,
    ) : ParserNumber()

    class U64(
        val value: ULong,
    ) : ParserNumber()

    class I64(
        val value: Long,
    ) : ParserNumber()
}

/**
 * Formats a finite double as a JSON number string.
 * Uses the platform's default double-to-string representation.
 */
internal fun formatFinite(f: Double): String {
    if (f == Double.POSITIVE_INFINITY) return "9e999"
    if (f == Double.NEGATIVE_INFINITY) return "-9e999"
    if (f.isNaN()) return "0"
    val s = f.toString()
    return if (!s.contains('.') && !s.contains('e') && !s.contains('E')) {
        "$s.0"
    } else {
        s
    }
}

/** Converts a [Byte] to a [JsonNumber]. */
fun JsonNumber.Companion.from(i: Byte): JsonNumber = fromI64(i.toLong())

/** Converts a [Short] to a [JsonNumber]. */
fun JsonNumber.Companion.from(i: Short): JsonNumber = fromI64(i.toLong())

/** Converts an [Int] to a [JsonNumber]. */
fun JsonNumber.Companion.from(i: Int): JsonNumber = fromI64(i.toLong())

/** Converts a [Long] to a [JsonNumber]. */
fun JsonNumber.Companion.from(i: Long): JsonNumber = fromI64(i)

/** Converts a [UByte] to a [JsonNumber]. */
fun JsonNumber.Companion.from(u: UByte): JsonNumber = fromU64(u.toULong())

/** Converts a [UShort] to a [JsonNumber]. */
fun JsonNumber.Companion.from(u: UShort): JsonNumber = fromU64(u.toULong())

/** Converts a [UInt] to a [JsonNumber]. */
fun JsonNumber.Companion.from(u: UInt): JsonNumber = fromU64(u.toULong())

/** Converts a [ULong] to a [JsonNumber]. */
fun JsonNumber.Companion.from(u: ULong): JsonNumber = fromU64(u)

/** Converts a [Double] to a [JsonNumber], or null if not finite. */
fun JsonNumber.Companion.from(f: Double): JsonNumber? = fromF64(f)

/** Converts a [Float] to a [JsonNumber], or null if not finite. */
fun JsonNumber.Companion.from(f: Float): JsonNumber? = fromF32(f)
