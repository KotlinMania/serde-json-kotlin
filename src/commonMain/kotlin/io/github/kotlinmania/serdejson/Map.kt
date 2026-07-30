// port-lint: source src/map.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.indexmap.IndexMap

/**
 * Represents a JSON key/value type.
 *
 * By default, the map is backed by an [IndexMap] to preserve insertion order.
 */
class JsonMap<K, V>(
    private val map: IndexMap<K, V>,
) : Iterable<Pair<K, V>>
    where K : Any, V : Any {
    /** Makes a new empty [JsonMap]. */
    constructor() : this(IndexMap())

    /** Makes a new empty [JsonMap] with the given initial capacity. */
    constructor(capacity: Int) : this(IndexMap.withCapacity(capacity))

    /** Clears the map, removing all values. */
    fun clear() {
        map.clear()
    }

    /** Returns the value corresponding to the key, or null. */
    fun get(key: K): V? = map.get(key)

    /** Returns true if the map contains a value for the specified key. */
    fun containsKey(key: K): Boolean = map.containsKey(key)

    /** Inserts a key-value pair into the map. Returns the old value if present, null otherwise. */
    fun insert(k: K, v: V): V? = map.insert(k, v)

    /** Removes a key from the map, returning the value at the key if it was previously in the map. */
    fun remove(key: K): V? = map.remove(key)

    /** Returns a mutable reference to the value corresponding to the key. */
    fun getMut(key: K): V? = map.get(key)

    /** Returns the key-value pair matching the given key. */
    fun getKeyValue(key: K): Pair<K, V>? = map.getKeyValue(key)

    /** Returns the number of elements in the map. */
    fun len(): Int = map.len()

    /** Returns true if the map contains no elements. */
    fun isEmpty(): Boolean = map.isEmpty()

    /** Returns true if the map contains elements. */
    fun isNotEmpty(): Boolean = !isEmpty()

    /** Returns an iterator over the map's entries. */
    override fun iterator(): Iterator<Pair<K, V>> = map.iterator()

    /** Returns an iterator over the map's keys. */
    fun keys(): List<K> = map.keys()

    /** Returns an iterator over the map's values. */
    fun values(): List<V> = map.values()

    /** Returns the list of key-value pairs in this map. */
    fun entries(): List<Pair<K, V>> = map.asEntries()

    /** Insert a key-value pair in the map at the given index. */
    fun shiftInsert(index: Int, k: K, v: V): V? = map.shiftInsert(index, k, v)

    /** Swaps the position of two entries. */
    fun swapRemove(key: K): V? = map.swapRemove(key)

    companion object {
        /** Creates a new empty [JsonMap]. */
        fun <K : Any, V : Any> new(): JsonMap<K, V> = JsonMap()

        /** Creates a new [JsonMap] from the given entries. */
        fun <K : Any, V : Any> from(entries: Iterable<Pair<K, V>>): JsonMap<K, V> =
            JsonMap(IndexMap.from(entries))

        /** Creates a new [JsonMap] with the given capacity. */
        fun <K : Any, V : Any> withCapacity(capacity: Int): JsonMap<K, V> = JsonMap(capacity)
    }
}

/**
 * A [JsonMap] specialized for String keys and [Value] values, matching the upstream
 * `Map<String, Value>` type alias.
 */
typealias ValueMap = JsonMap<String, Value>