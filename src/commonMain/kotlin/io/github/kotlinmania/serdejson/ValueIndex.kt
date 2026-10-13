// port-lint: source value/index.rs
package io.github.kotlinmania.serdejson

/**
 * A type that can be used to index into a [Value].
 *
 * The [Value.get] and [Value.getMut] methods of [Value] accept any type that
 * implements [Index], as does the square-bracket indexing operator. This
 * interface is implemented for strings which are used as the index into a JSON
 * object, and for [Int] which is used as the index into a JSON array.
 *
 * This interface is sealed and cannot be implemented for types outside of
 * serde-json.
 */
sealed interface Index {
    /** Return null if the key is not already in the array or object. */
    fun indexInto(v: Value): Value?

    /** Return null if the key is not already in the array or object. */
    fun indexIntoMut(v: Value): Value?

    /**
     * Panic if array index out of bounds. If key is not already in the object,
     * insert it with a value of null. Panic if Value is a type that cannot be
     * indexed into, except if Value is null then it can be treated as an empty
     * object.
     */
    fun indexOrInsert(v: Value): Value
}

typealias ValueIndex = Index

/** Index by integer position into a [Value.Array]. */
class IntIndex(
    private val index: Int,
) : Index {
    override fun indexInto(v: Value): Value? =
        when (v) {
            is Value.Array -> v.value.getOrNull(index)
            else -> null
        }

    override fun indexIntoMut(v: Value): Value? =
        when (v) {
            is Value.Array -> v.value.getOrNull(index)
            else -> null
        }

    override fun indexOrInsert(v: Value): Value {
        when (v) {
            is Value.Array -> {
                val len = v.value.size
                return v.value.getOrNull(index)
                    ?: throw IndexOutOfBoundsException(
                        "cannot access index $index of JSON array of length $len",
                    )
            }
            else -> throw IllegalStateException("cannot access index $index of JSON ${Type(v)}")
        }
    }
}

/** Index by string key into a [Value.Object]. */
class StrIndex(
    private val key: String,
) : Index {
    override fun indexInto(v: Value): Value? =
        when (v) {
            is Value.Object -> v.value.get(key)
            else -> null
        }

    override fun indexIntoMut(v: Value): Value? =
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
            else -> throw IllegalStateException("cannot access key \"$key\" in JSON ${Type(current)}")
        }
    }
}

/** Index by string key into a [Value.Object]. */
class StringIndex(
    private val key: String,
) : Index {
    override fun indexInto(v: Value): Value? = StrIndex(key).indexInto(v)

    override fun indexIntoMut(v: Value): Value? = StrIndex(key).indexIntoMut(v)

    override fun indexOrInsert(v: Value): Value = StrIndex(key).indexOrInsert(v)
}

/** Reference index forwarding to underlying [Index]. */
class RefIndex<T : Index>(
    private val target: T,
) : Index {
    override fun indexInto(v: Value): Value? = target.indexInto(v)

    override fun indexIntoMut(v: Value): Value? = target.indexIntoMut(v)

    override fun indexOrInsert(v: Value): Value = target.indexOrInsert(v)
}

/** Used in panic messages. */
internal class Type(
    val value: Value,
) {
    fun fmt(formatter: StringBuilder): String {
        val s =
            when (value) {
                is Value.Null -> "null"
                is Value.Bool -> "boolean"
                is Value.Number -> "number"
                is Value.Str -> "string"
                is Value.Array -> "array"
                is Value.Object -> "object"
            }
        formatter.append(s)
        return s
    }

    override fun toString(): String {
        val sb = StringBuilder()
        fmt(sb)
        return sb.toString()
    }
}

/**
 * Index into a [Value].
 *
 * Returns [Value.Null] if the type does not match or the key/index does not exist.
 */
fun Value.index(index: Index): Value =
    index.indexInto(this) ?: Value.Null

/** Write into a [Value]. */
fun Value.indexMut(index: Index): Value =
    index.indexOrInsert(this)

/**
 * Index into a [Value] using an [Int] (array index) or [String] (object key).
 *
 * Returns [Value.Null] if the type of this value does not match the index type,
 * or if the key/index does not exist.
 */
operator fun Value.get(index: Int): Value =
    this.index(IntIndex(index))

/** Index into a [Value] using a string key. Returns [Value.Null] if the key is absent. */
operator fun Value.get(key: String): Value =
    this.index(StrIndex(key))
