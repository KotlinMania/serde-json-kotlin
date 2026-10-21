// port-lint: source map.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.indexmap.IndexMap
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer

typealias MapImpl<K, V> = IndexMap<K, V>

/**
 * Represents a JSON key/value type.
 *
 * By default the map is backed by an [IndexMap] to preserve order.
 */
class JsonMap(
    internal val map: MapImpl<String, Value>,
) : Iterable<Pair<String, Value>>,
    Serialize {
    /** Makes a new empty Map. */
    constructor() : this(MapImpl.new())

    /** Makes a new empty Map with the given initial capacity. */
    constructor(capacity: Int) : this(MapImpl.withCapacity(capacity))

    /** Clears the map, removing all values. */
    fun clear() {
        map.clear()
    }

    /** Returns a reference to the value corresponding to the key. */
    operator fun get(key: String): Value? = map.get(key)

    /** Returns true if the map contains a value for the specified key. */
    fun containsKey(key: String): Boolean = map.containsKey(key)

    /** Returns a mutable reference to the value corresponding to the key. */
    fun getMut(key: String): Value? = map.get(key)

    /** Returns the key-value pair matching the given key. */
    fun getKeyValue(key: String): Pair<String, Value>? = map.getKeyValue(key)

    /**
     * Inserts a key-value pair into the map.
     *
     * If the map did not have this key present, `null` is returned.
     * If the map did have this key present, the value is updated, and the old value is returned.
     */
    fun insert(k: String, v: Value): Value? = map.insert(k, v)

    /**
     * Insert a key-value pair in the map at the given index.
     *
     * If the map did not have this key present, `null` is returned.
     * If the map did have this key present, the key is moved to the new position, the value is updated, and the old value is returned.
     */
    fun shiftInsert(index: Int, k: String, v: Value): Value? = map.shiftInsert(index, k, v)

    /**
     * Removes a key from the map, returning the value at the key if the key was previously in the map.
     *
     * Equivalent to [swapRemove], replacing this entry's position with the last element.
     * If you need to preserve the relative order of the keys in the map, use [shiftRemove] instead.
     */
    fun remove(key: String): Value? = swapRemove(key)

    /**
     * Removes a key from the map, returning the stored key and value if the key was previously in the map.
     *
     * Equivalent to [swapRemoveEntry], replacing this entry's position with the last element.
     * If you need to preserve the relative order of the keys in the map, use [shiftRemoveEntry] instead.
     */
    fun removeEntry(key: String): Pair<String, Value>? = swapRemoveEntry(key)

    /**
     * Removes and returns the value corresponding to the key from the map.
     *
     * The entry is removed by swapping it with the last element of the map and popping it off.
     */
    fun swapRemove(key: String): Value? = map.swapRemove(key)

    /**
     * Remove and return the key-value pair.
     *
     * The entry is removed by swapping it with the last element of the map and popping it off.
     */
    fun swapRemoveEntry(key: String): Pair<String, Value>? = map.swapRemoveEntry(key)

    /**
     * Removes and returns the value corresponding to the key from the map.
     *
     * The entry is removed by shifting all of the elements that follow it, preserving their relative order.
     */
    fun shiftRemove(key: String): Value? = map.shiftRemove(key)

    /**
     * Remove and return the key-value pair.
     *
     * The entry is removed by shifting all of the elements that follow it, preserving their relative order.
     */
    fun shiftRemoveEntry(key: String): Pair<String, Value>? = map.shiftRemoveEntry(key)

    /** Moves all elements from other into self, leaving other empty. */
    fun append(other: JsonMap) {
        map.append(other.map)
    }

    /** Gets the given key's corresponding entry in the map for in-place manipulation. */
    fun entry(key: String): Entry =
        when (val e = map.entry(key)) {
            is io.github.kotlinmania.indexmap.map.Entry.Vacant ->
                Entry.Vacant(VacantEntry(e.entry))
            is io.github.kotlinmania.indexmap.map.Entry.Occupied ->
                Entry.Occupied(OccupiedEntry(e.entry))
        }

    /** Returns the number of elements in the map. */
    fun len(): Int = map.len()

    /** Returns true if the map contains no elements. */
    fun isEmpty(): Boolean = map.isEmpty()

    /** Returns true if the map contains elements. */
    fun isNotEmpty(): Boolean = !isEmpty()

    /** Gets an iterator over the entries of the map. */
    fun iter(): Iter = Iter(map.iterator(), len())

    /** Gets a mutable iterator over the entries of the map. */
    fun iterMut(): IterMut = IterMut(map.iterator(), len())

    /** Gets an iterator over the keys of the map. */
    fun keys(): Keys = Keys(map.keys())

    /** Gets an iterator over the values of the map. */
    fun values(): Values = Values(map.values())

    /** Gets an iterator over mutable values of the map. */
    fun valuesMut(): ValuesMut = ValuesMut(map.values())

    /** Gets an iterator over the values of the map. */
    fun intoValues(): IntoValues = IntoValues(map.values())

    /**
     * Retains only the elements specified by the predicate.
     *
     * In other words, remove all pairs `(k, v)` such that `f(k, v)` returns `false`.
     */
    fun retain(f: (String, Value) -> Boolean) {
        map.retain(f)
    }

    /** Sorts this map's entries in-place using String's usual ordering. */
    fun sortKeys() {
        map.sortKeys { a, b -> a.compareTo(b) }
    }

    fun clone(): JsonMap = JsonMap(map.clone())

    fun cloneFrom(source: JsonMap) {
        map.cloneFrom(source.map)
    }

    fun eq(other: JsonMap): Boolean = this == other

    fun hash(): Int = hashCode()

    fun index(key: String): Value =
        get(key) ?: throw NoSuchElementException("no entry found for key")

    fun indexMut(key: String): Value = index(key)

    fun fmt(): String = toString()

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> {
        val serMap = serializer.serializeMap(len()).getOrElse { return SerdeResult.failure(it) }
        for ((k, v) in this) {
            serMap.serializeEntry(Value.Str(k), v).getOrElse { return SerdeResult.failure(it) }
        }
        return serMap.end()
    }

    override fun iterator(): Iterator<Pair<String, Value>> = map.iterator()

    fun intoIter(): IntoIter = IntoIter(map.iterator(), len())

    fun extend(iter: Iterable<Pair<String, Value>>) {
        map.extend(iter)
    }

    fun entries(): List<Pair<String, Value>> = map.asEntries()

    fun intoDeserializer(): Deserializer = ValueDeserializer(Value.Object(this))

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
        for ((k, v) in entries().sortedBy { it.first }) {
            h = 31 * h + (k.hashCode() xor v.hashCode())
        }
        return h
    }

    override fun toString(): String = map.toString()

    companion object : Deserialize<JsonMap> {
        /** Makes a new empty Map. */
        fun new(): JsonMap = JsonMap()

        /** Makes a new empty Map with the given initial capacity. */
        fun withCapacity(capacity: Int): JsonMap = JsonMap(capacity)

        fun default(): JsonMap = new()

        fun fromIter(iter: Iterable<Pair<String, Value>>): JsonMap = from(iter)

        fun from(entries: Iterable<Pair<String, Value>>): JsonMap {
            val m = new()
            m.extend(entries)
            return m
        }

        fun intoDeserializer(map: JsonMap): Deserializer = map.intoDeserializer()

        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<JsonMap> =
            deserializer.deserializeMap(MapVisitor)
    }
}

internal object MapVisitor : Visitor<JsonMap> {
    override fun expecting(): String = "a map"

    override fun visitUnit(): SerdeResult<JsonMap> = SerdeResult.success(JsonMap.new())

    override fun <A : MapAccess> visitMap(access: A): SerdeResult<JsonMap> {
        val values = JsonMap.new()
        while (true) {
            val entry = access.nextEntry(StringDeserialize, ValueDeserialize).getOrElse { return SerdeResult.failure(it) } ?: break
            values.insert(entry.first, entry.second)
        }
        return SerdeResult.success(values)
    }
}

/**
 * A view into a single entry in a map, which may either be vacant or occupied.
 * This enum is constructed from the [JsonMap.entry] method.
 */
sealed class Entry {
    /** Returns a reference to this entry's key. */
    abstract fun key(): String

    /**
     * Ensures a value is in the entry by inserting the default if empty, and
     * returns the value in the entry.
     */
    fun orInsert(default: Value): Value =
        when (this) {
            is Vacant -> vacant.insert(default)
            is Occupied -> occupied.intoMut()
        }

    /**
     * Ensures a value is in the entry by inserting the result of the default
     * function if empty, and returns the value in the entry.
     */
    fun orInsertWith(default: () -> Value): Value =
        when (this) {
            is Vacant -> vacant.insert(default())
            is Occupied -> occupied.intoMut()
        }

    /**
     * Provides in-place mutable access to an occupied entry before any
     * potential inserts into the map.
     */
    fun andModify(f: (Value) -> Value): Entry =
        when (this) {
            is Occupied -> {
                val newVal = f(occupied.getMut())
                occupied.insert(newVal)
                this
            }
            is Vacant -> this
        }

    /** A vacant Entry. */
    class Vacant(
        val vacant: VacantEntry,
    ) : Entry() {
        override fun key(): String = vacant.key()
    }

    /** An occupied Entry. */
    class Occupied(
        val occupied: OccupiedEntry,
    ) : Entry() {
        override fun key(): String = occupied.key()
    }
}

typealias VacantEntryImpl = io.github.kotlinmania.indexmap.map.VacantEntry<String, Value>

/** A vacant Entry. It is part of the [Entry] enum. */
class VacantEntry internal constructor(
    internal val vacant: VacantEntryImpl,
) {
    /** Gets a reference to the key that would be used when inserting a value through the VacantEntry. */
    fun key(): String = vacant.key()

    /** Sets the value of the entry with the VacantEntry's key, and returns the value. */
    fun insert(value: Value): Value = vacant.insert(value)
}

typealias OccupiedEntryImpl = io.github.kotlinmania.indexmap.map.OccupiedEntry<String, Value>

/** An occupied Entry. It is part of the [Entry] enum. */
class OccupiedEntry internal constructor(
    internal val occupied: OccupiedEntryImpl,
) {
    /** Gets a reference to the key in the entry. */
    fun key(): String = occupied.key()

    /** Gets a reference to the value in the entry. */
    fun get(): Value = occupied.get()

    /** Gets a mutable reference to the value in the entry. */
    fun getMut(): Value = occupied.getMut()

    /** Converts the entry into a reference to its value. */
    fun intoMut(): Value = occupied.intoMut()

    /** Sets the value of the entry with the OccupiedEntry's key, and returns the entry's old value. */
    fun insert(value: Value): Value = occupied.insert(value)

    /** Takes the value of the entry out of the map, and returns it. */
    fun remove(): Value = swapRemove()

    /** Takes the value of the entry out of the map by swapping it with the last element and popping it off. */
    fun swapRemove(): Value = occupied.swapRemove()

    /** Takes the value of the entry out of the map by shifting following elements down. */
    fun shiftRemove(): Value = occupied.shiftRemove()

    /** Removes the entry from the map, returning the stored key and value. */
    fun removeEntry(): Pair<String, Value> = swapRemoveEntry()

    /** Removes the entry from the map by swapping it with the last element and popping it off. */
    fun swapRemoveEntry(): Pair<String, Value> = occupied.swapRemoveEntry()

    /** Removes the entry from the map by shifting following elements down. */
    fun shiftRemoveEntry(): Pair<String, Value> = occupied.shiftRemoveEntry()
}

/** An iterator over a [JsonMap]'s entries. */
class Iter internal constructor(
    private val iter: Iterator<Pair<String, Value>>,
    private var remaining: Int,
) : Iterator<Pair<String, Value>> by iter {
    override fun next(): Pair<String, Value> {
        val entry = iter.next()
        remaining -= 1
        return entry
    }

    fun len(): Int = remaining

    fun intoIter(): Iter = this
}

/** A mutable iterator over a [JsonMap]'s entries. */
class IterMut internal constructor(
    private val iter: Iterator<Pair<String, Value>>,
    private var remaining: Int,
) : Iterator<Pair<String, Value>> by iter {
    override fun next(): Pair<String, Value> {
        val entry = iter.next()
        remaining -= 1
        return entry
    }

    fun len(): Int = remaining

    fun intoIter(): IterMut = this
}

/** An owning iterator over a [JsonMap]'s entries. */
class IntoIter internal constructor(
    private val iter: Iterator<Pair<String, Value>>,
    private var remaining: Int,
) : Iterator<Pair<String, Value>> by iter {
    override fun next(): Pair<String, Value> {
        val entry = iter.next()
        remaining -= 1
        return entry
    }

    fun len(): Int = remaining

    fun intoIter(): IntoIter = this
}

/** An iterator over a [JsonMap]'s keys. */
class Keys internal constructor(
    private val list: List<String>,
) : List<String> by list {
    fun len(): Int = list.size

    fun collect(): List<String> = list

    override fun equals(other: Any?): Boolean = if (other is List<*>) list == other else false

    override fun hashCode(): Int = list.hashCode()

    override fun toString(): String = list.toString()
}

/** An iterator over a [JsonMap]'s values. */
class Values internal constructor(
    private val list: List<Value>,
) : List<Value> by list {
    fun len(): Int = list.size

    fun collect(): List<Value> = list

    override fun equals(other: Any?): Boolean = if (other is List<*>) list == other else false

    override fun hashCode(): Int = list.hashCode()

    override fun toString(): String = list.toString()
}

/** A mutable iterator over a [JsonMap]'s values. */
class ValuesMut internal constructor(
    private val list: List<Value>,
) : List<Value> by list {
    fun len(): Int = list.size

    fun collect(): List<Value> = list

    override fun equals(other: Any?): Boolean = if (other is List<*>) list == other else false

    override fun hashCode(): Int = list.hashCode()

    override fun toString(): String = list.toString()
}

/** An owning iterator over a [JsonMap]'s values. */
class IntoValues internal constructor(
    private val list: List<Value>,
) : List<Value> by list {
    fun len(): Int = list.size

    fun collect(): List<Value> = list

    override fun equals(other: Any?): Boolean = if (other is List<*>) list == other else false

    override fun hashCode(): Int = list.hashCode()

    override fun toString(): String = list.toString()
}
