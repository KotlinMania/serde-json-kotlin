package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.io.VecIoWrite

/**
 * Represents any valid JSON value.
 */
sealed class Value : Serialize {
    /** Represents a JSON null value. */
    object Null : Value()

    /** Represents a JSON boolean. */
    class Bool(
        val value: Boolean,
    ) : Value()

    /** Represents a JSON number, whether integer or floating point. */
    class Number(
        val value: io.github.kotlinmania.serdejson.JsonNumber,
    ) : Value()

    /** Represents a JSON string. */
    class Str(
        val value: String,
    ) : Value()

    /** Represents a JSON array. */
    class Array(
        val value: List<Value>,
    ) : Value()

    /** Represents a JSON object. */
    class Object(
        val value: io.github.kotlinmania.serdejson.JsonMap,
    ) : Value()

    /** Index into a JSON array or map. Returns null if the type doesn't match. */
    fun get(index: String): Value? =
        when (this) {
            is Object -> value.get(index)
            else -> null
        }

    /** Index into a JSON array. Returns null if the type doesn't match or the index is out of bounds. */
    fun get(index: Int): Value? =
        when (this) {
            is Array -> value.getOrNull(index)
            else -> null
        }

    /** Returns true if the [Value] is an Object. */
    fun isObject(): Boolean = this is Object

    /** If the [Value] is an Object, returns the associated [ValueMap]. Returns null otherwise. */
    fun asObject(): io.github.kotlinmania.serdejson.JsonMap? = (this as? Object)?.value

    /** If the [Value] is an Object, returns a mutable reference to the associated [ValueMap]. Returns null otherwise. */
    fun asObjectMut(): io.github.kotlinmania.serdejson.JsonMap? = asObject()

    /** Returns true if the [Value] is an Array. */
    fun isArray(): Boolean = this is Array

    /** If the [Value] is an Array, returns the associated list. Returns null otherwise. */
    fun asArray(): List<Value>? = (this as? Array)?.value

    /** If the [Value] is an Array, returns a mutable reference to the associated list. Returns null otherwise. */
    fun asArrayMut(): List<Value>? = asArray()

    /** Returns true if the [Value] is a String. */
    fun isString(): Boolean = this is Str

    /** If the [Value] is a String, returns the associated string. Returns null otherwise. */
    fun asStr(): String? = (this as? Str)?.value

    /** Returns true if the [Value] is a Number. */
    fun isNumber(): Boolean = this is Number

    /** If the [Value] is a Number, returns the associated [Number]. Returns null otherwise. */
    fun asNumber(): io.github.kotlinmania.serdejson.JsonNumber? = (this as? Number)?.value

    /** Returns true if the [Value] is an integer between [Long.MIN_VALUE] and [Long.MAX_VALUE]. */
    fun isI64(): Boolean = (this as? Number)?.value?.isI64() ?: false

    /** Returns true if the [Value] is an integer between zero and [ULong.MAX_VALUE]. */
    fun isU64(): Boolean = (this as? Number)?.value?.isU64() ?: false

    /** Returns true if the [Value] is a number that can be represented by Double. */
    fun isF64(): Boolean = (this as? Number)?.value?.isF64() ?: false

    /** If the [Value] is an integer, represent it as [Long] if possible. Returns null otherwise. */
    fun asI64(): Long? = (this as? Number)?.value?.asI64()

    /** If the [Value] is an integer, represent it as [ULong] if possible. Returns null otherwise. */
    fun asU64(): ULong? = (this as? Number)?.value?.asU64()

    /** If the [Value] is a number, represent it as [Double] if possible. Returns null otherwise. */
    fun asF64(): Double? = (this as? Number)?.value?.asF64()

    /** Returns true if the [Value] is a Boolean. */
    fun isBoolean(): Boolean = this is Bool

    /** If the [Value] is a Boolean, returns the associated boolean. Returns null otherwise. */
    fun asBool(): Boolean? = (this as? Bool)?.value

    /** Returns true if the [Value] is a Null. */
    fun isNull(): Boolean = this is Null

    /** If the [Value] is a Null, returns Unit. Returns null otherwise. */
    fun asNull(): Unit? = if (this is Null) Unit else null

    /** Looks up a value by a JSON Pointer (RFC 6901). */
    fun pointer(pointer: String): Value? {
        if (pointer.isEmpty()) return this
        if (!pointer.startsWith('/')) return null
        var target: Value = this
        for (token in pointer.split('/').drop(1)) {
            val key = token.replace("~1", "/").replace("~0", "~")
            target =
                when (target) {
                    is Object -> target.value.get(key) ?: return null
                    is Array -> parseIndex(key)?.let { target.value.getOrNull(it) } ?: return null
                    else -> return null
                }
        }
        return target
    }

    /** Takes the value out of the [Value], leaving a [Null] in its place. */
    fun take(): Value = this

    /** Reorders the entries of all [Value.Object] nested within this JSON value. */
    fun sortAllObjects() {
        when (this) {
            is Object -> {
                // IndexMap preserves order; sorting would require sortKeys support
                value.values().forEach { it.sortAllObjects() }
            }
            is Array -> value.forEach { it.sortAllObjects() }
            else -> {}
        }
    }

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serialize(this, serializer)

    override fun toString(): String {
        val writer = VecIoWrite()
        toJsonString(writer, this, pretty = false)
        return writer.bytes.decodeToString()
    }

    fun toPrettyString(): String {
        val writer = VecIoWrite()
        toJsonString(writer, this, pretty = true)
        return writer.bytes.decodeToString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Value) return false
        return eqValue(this, other)
    }

    private fun eqValue(a: Value, b: Value): Boolean {
        if (a is Value.Number && b is Value.Number) {
            val an = a.value
            val bn = b.value
            if (an.isF64() || bn.isF64()) return an.asF64() == bn.asF64()
            if (an.isI64() && bn.isI64()) return an.asI64() == bn.asI64()
            if (an.isU64() && bn.isU64()) return an.asU64() == bn.asU64()
            val al = an.asI64() ?: an.asU64()?.toLong()
            val bl = bn.asI64() ?: bn.asU64()?.toLong()
            return al == bl
        }
        if (a is Value.Str && b is Value.Str) return a.value == b.value
        if (a is Value.Bool && b is Value.Bool) return a.value == b.value
        if (a is Value.Null && b is Value.Null) return true
        if (a is Value.Array && b is Value.Array) {
            if (a.value.size != b.value.size) return false
            return a.value.zip(b.value).all { (x, y) -> eqValue(x, y) }
        }
        if (a is Value.Object && b is Value.Object) {
            if (a.value.len() != b.value.len()) return false
            for ((k, v) in a.value) {
                if (!b.value.containsKey(k)) return false
                if (!eqValue(v, b.value.get(k)!!)) return false
            }
            return true
        }
        return false
    }

    override fun hashCode(): Int =
        when (this) {
            is Null -> 0
            is Bool -> value.hashCode()
            is Number -> value.hashCode()
            is Str -> value.hashCode()
            is Array -> value.hashCode()
            is Object -> value.hashCode()
        }

    companion object {
        /** Parses a string index, returning null for invalid indices. */
        private fun parseIndex(s: String): Int? {
            if (s.startsWith('+') || (s.startsWith('0') && s.length != 1)) return null
            return s.toIntOrNull()
        }

        /** The default value is [Value.Null]. */
        fun default(): Value = Null
    }
}

/**
 * Convert a [Serialize] value into a [Value] which is an enum that can represent any valid JSON data.
 */
fun toValue(value: Serialize): JsonValueOutcome =
    JsonValueOutcome.fromResult(value.serialize(io.github.kotlinmania.serdejson.Serializer()))

/**
 * Interpret a [Value] as an instance of type T.
 */
fun <T> fromValue(value: Value, deserialize: Deserialize<T>): SerdeResult<T> {
    val json = valueToStr(value)
    return fromStr(json, deserialize)
}
