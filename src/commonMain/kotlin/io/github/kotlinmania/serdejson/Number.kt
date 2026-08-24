// port-lint: source src/number.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Unexpected
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

/**
 * Represents a JSON number, whether integer or floating point.
 */
class JsonNumber private constructor(
    private val n: N,
) : Serialize {
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

    companion object {
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
    }

    /** Returns true if the [Number] is an integer between [Long.MIN_VALUE] and [Long.MAX_VALUE]. */
    fun isI64(): Boolean =
        when (n) {
            is N.PosInt -> n.value <= Long.MAX_VALUE.toULong()
            is N.NegInt -> true
            is N.Float -> false
        }

    /** Returns true if the [Number] is an integer between zero and [ULong.MAX_VALUE]. */
    fun isU64(): Boolean =
        when (n) {
            is N.PosInt -> true
            is N.NegInt, is N.Float -> false
        }

    /** Returns true if the [Number] can be represented by [Double]. */
    fun isF64(): Boolean =
        when (n) {
            is N.Float -> true
            is N.PosInt, is N.NegInt -> false
        }

    /** If the [Number] is an integer, represent it as [Long] if possible. Returns null otherwise. */
    fun asI64(): Long? =
        when (n) {
            is N.PosInt -> if (n.value <= Long.MAX_VALUE.toULong()) n.value.toLong() else null
            is N.NegInt -> n.value
            is N.Float -> null
        }

    /** If the [Number] is an integer, represent it as [ULong] if possible. Returns null otherwise. */
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

    /** Represents the number as [Float] if possible. Returns null otherwise. */
    internal fun asF32(): Float? =
        when (n) {
            is N.PosInt -> n.value.toFloat()
            is N.NegInt -> n.value.toFloat()
            is N.Float -> n.value.toFloat()
        }

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonNumber) return false
        return when (val thisN = n) {
            is N.PosInt ->
                when (val otherN = other.n) {
                    is N.PosInt -> thisN.value == otherN.value
                    is N.NegInt -> false
                    is N.Float -> thisN.value.toDouble() == otherN.value
                }
            is N.NegInt ->
                when (val otherN = other.n) {
                    is N.NegInt -> thisN.value == otherN.value
                    is N.PosInt -> false
                    is N.Float -> thisN.value.toDouble() == otherN.value
                }
            is N.Float ->
                when (val otherN = other.n) {
                    is N.Float -> thisN.value == otherN.value
                    is N.PosInt -> thisN.value == otherN.value.toDouble()
                    is N.NegInt -> thisN.value == otherN.value.toDouble()
                }
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

/** Converts an [Int] to a [JsonNumber]. */
fun JsonNumber.Companion.from(i: Int): JsonNumber = fromI64(i.toLong())

/** Converts a [Long] to a [JsonNumber]. */
fun JsonNumber.Companion.from(i: Long): JsonNumber = fromI64(i)

/** Converts a [ULong] to a [JsonNumber]. */
fun JsonNumber.Companion.from(u: ULong): JsonNumber = fromU64(u)

/** Converts a [Double] to a [JsonNumber], or null if not finite. */
fun JsonNumber.Companion.from(f: Double): JsonNumber? = fromF64(f)

/** Converts a [Float] to a [JsonNumber], or null if not finite. */
fun JsonNumber.Companion.from(f: Float): JsonNumber? = fromF32(f)
