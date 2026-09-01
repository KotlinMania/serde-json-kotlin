// port-lint: source value/index.rs
package io.github.kotlinmania.serdejson

/**
 * A type that can be used to index into a [Value].
 *
 * In the upstream Rust crate this is a sealed trait implemented for `usize`,
 * `str`, `String`, and references. In Kotlin we use function overloading
 * directly on [Value] — see [Value.get] — but this interface preserves the
 * contract for callers that want to pass an index generically.
 */
sealed interface ValueIndex {
    /** Return null if the key is not already in the array or object. */
    fun indexInto(v: Value): Value?

    /** Panic if the array index is out of bounds, or insert null for a missing object key. */
    fun indexOrInsert(v: Value): Value
}

/** Index by integer position into a [Value.Array]. */
class IntIndex(
    private val index: Int,
) : ValueIndex {
    override fun indexInto(v: Value): Value? =
        when (v) {
            is Value.Array -> v.value.getOrNull(index)
            else -> null
        }

    override fun indexOrInsert(v: Value): Value {
        when (v) {
            is Value.Array -> {
                val len = v.value.size
                return v.value.getOrNull(index)
                    ?: throw IndexOutOfBoundsException("cannot access index $index of JSON array of length $len")
            }
            else -> throw IllegalStateException("cannot access index $index of JSON ${valueTypeName(v)}")
        }
    }
}

/** Index by string key into a [Value.Object]. */
class StrIndex(
    private val key: String,
) : ValueIndex {
    override fun indexInto(v: Value): Value? =
        when (v) {
            is Value.Object -> v.value.get(key)
            else -> null
        }

    override fun indexOrInsert(v: Value): Value {
        var current = v
        if (current is Value.Null) {
            current = Value.Object(ValueMap())
        }
        when (current) {
            is Value.Object -> {
                if (!current.value.containsKey(key)) {
                    current.value.insert(key, Value.Null)
                }
                return current.value.get(key)!!
            }
            else -> throw IllegalStateException("cannot access key \"$key\" in JSON ${valueTypeName(current)}")
        }
    }
}

/** Returns a human-readable type name for a [Value], used in panic messages. */
internal fun valueTypeName(v: Value): String =
    when (v) {
        is Value.Null -> "null"
        is Value.Bool -> "boolean"
        is Value.Number -> "number"
        is Value.Str -> "string"
        is Value.Array -> "array"
        is Value.Object -> "object"
    }

/**
 * Index into a [Value] using an [Int] (array index) or [String] (object key).
 *
 * Returns [Value.Null] if the type of this value does not match the index type,
 * or if the key/index does not exist.
 */
operator fun Value.get(index: Int): Value =
    IntIndex(index).indexInto(this) ?: Value.Null

/** Index into a [Value] using a string key. Returns [Value.Null] if the key is absent. */
operator fun Value.get(key: String): Value =
    StrIndex(key).indexInto(this) ?: Value.Null
