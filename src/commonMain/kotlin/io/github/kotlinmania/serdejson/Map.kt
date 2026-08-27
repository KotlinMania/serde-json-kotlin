// port-lint: source serde_json/tests/map.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.indexmap.IndexMap

/**
 * Represents a JSON key/value type.
 *
 * By default, the map is backed by an [IndexMap] to preserve insertion order.
 */
class JsonMap(
    private val map: IndexMap<String, Value>,
) : Iterable<Pair<String, Value>> {
    /** Makes a new empty [JsonMap]. */
    constructor() : this(IndexMap())

    /** Makes a new empty [JsonMap] with the given initial capacity. */
    constructor(capacity: Int) : this(IndexMap.withCapacity(capacity))

    /** Clears the map, removing all values. */
    fun clear() {
        map.clear()
    }

    /** Returns the value corresponding to the key, or null. */
    fun get(key: String): Value? = map.get(key)

    /** Returns true if the map contains a value for the specified key. */
    fun containsKey(key: String): Boolean = map.containsKey(key)

    /** Inserts a key-value pair into the map. Returns the old value if present, null otherwise. */
    fun insert(k: String, v: Value): Value? = map.insert(k, v)

    /** Removes a key from the map, returning the value at the key if it was previously in the map. */
    fun remove(key: String): Value? = map.remove(key)

    /** Returns a mutable reference to the value corresponding to the key. */
    fun getMut(key: String): Value? = map.get(key)

    /** Returns the key-value pair matching the given key. */
    fun getKeyValue(key: String): Pair<String, Value>? = map.getKeyValue(key)

    /** Returns the number of elements in the map. */
    fun len(): Int = map.len()

    /** Returns true if the map contains no elements. */
    fun isEmpty(): Boolean = map.isEmpty()

    /** Returns true if the map contains elements. */
    fun isNotEmpty(): Boolean = !isEmpty()

    /** Returns an iterator over the map's entries. */
    override fun iterator(): Iterator<Pair<String, Value>> = map.iterator()

    /** Returns an iterator over the map's keys. */
    fun keys(): List<String> = map.keys()

    /** Returns an iterator over the map's values. */
    fun values(): List<Value> = map.values()

    /** Returns the list of key-value pairs in this map. */
    fun entries(): List<Pair<String, Value>> = map.asEntries()

    /** Insert a key-value pair in the map at the given index. */
    fun shiftInsert(index: Int, k: String, v: Value): Value? = map.shiftInsert(index, k, v)

    /** Swaps the position of two entries. */
    fun swapRemove(key: String): Value? = map.swapRemove(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonMap) return false
        if (this.len() != other.len()) return false
        for ((k, v) in this) {
            val otherVal = other.get(k) ?: return false
            if (v != otherVal) return false
        }
        return true
    }

    override fun hashCode(): Int {
        var h = 0
        for ((k, v) in this) {
            h += (k.hashCode() xor v.hashCode())
        }
        return h
    }

    companion object {
        /** Creates a new empty [JsonMap]. */
        fun new(): JsonMap = JsonMap()

        /** Creates a new [JsonMap] from the given entries. */
        fun from(entries: Iterable<Pair<String, Value>>): JsonMap =
            JsonMap(IndexMap.from(entries))

        /** Creates a new [JsonMap] with the given capacity. */
        fun withCapacity(capacity: Int): JsonMap = JsonMap(capacity)
    }
}

/**
 * A [JsonMap] specialized for String keys and [Value] values, matching the upstream
 * `Map<String, Value>` type alias.
 */
typealias ValueMap = JsonMap
