// port-lint: source serde_json/src/value/from.rs
package io.github.kotlinmania.serdejson

/** Convert a [Byte] to a [Value]. */
internal fun Value.from(n: Byte): Value = Value.Number(JsonNumber.fromI64(n.toLong()))

/** Convert a [Short] to a [Value]. */
internal fun Value.from(n: Short): Value = Value.Number(JsonNumber.fromI64(n.toLong()))

/** Convert an [Int] to a [Value]. */
internal fun Value.from(n: Int): Value = Value.Number(JsonNumber.fromI64(n.toLong()))

/** Convert a [Long] to a [Value]. */
internal fun Value.from(n: Long): Value = Value.Number(JsonNumber.fromI64(n))

/** Convert a [UByte] to a [Value]. */
internal fun Value.from(n: UByte): Value = Value.Number(JsonNumber.fromU64(n.toULong()))

/** Convert a [UShort] to a [Value]. */
internal fun Value.from(n: UShort): Value = Value.Number(JsonNumber.fromU64(n.toULong()))

/** Convert a [UInt] to a [Value]. */
internal fun Value.from(n: UInt): Value = Value.Number(JsonNumber.fromU64(n.toULong()))

/** Convert a [ULong] to a [Value]. */
internal fun Value.from(n: ULong): Value = Value.Number(JsonNumber.fromU64(n))

/** Convert a [Float] to a [Value], or [Value.Null] if infinite or NaN. */
internal fun Value.from(f: Float): Value =
    JsonNumber.fromF32(f)?.let { Value.Number(it) } ?: Value.Null

/** Convert a [Double] to a [Value], or [Value.Null] if infinite or NaN. */
internal fun Value.from(f: Double): Value =
    JsonNumber.fromF64(f)?.let { Value.Number(it) } ?: Value.Null

/** Convert a [Boolean] to a [Value]. */
internal fun Value.from(b: Boolean): Value = Value.Bool(b)

/** Convert a [String] to a [Value]. */
internal fun Value.from(s: String): Value = Value.Str(s)

/** Convert a [JsonNumber] to a [Value]. */
internal fun Value.from(n: JsonNumber): Value = Value.Number(n)

/** Convert a [ValueMap] to a [Value]. */
internal fun Value.from(m: ValueMap): Value = Value.Object(m)

/** Convert a [List] of [Value] to a [Value]. */
internal fun Value.fromList(list: List<Value>): Value = Value.Array(list)

/** Convert a list of pairs (key, value) to a [Value.Object]. */
internal fun Value.fromEntries(entries: List<Pair<String, Value>>): Value {
    val map = ValueMap()
    for ((k, v) in entries) map.insert(k, v)
    return Value.Object(map)
}

/** Convert an [Option] (nullable) to a [Value]: null → [Value.Null], some → value. */
internal fun <T> Value.fromOrNull(opt: T?, convert: (T) -> Value): Value =
    opt?.let { convert(it) } ?: Value.Null

/** Collect an iterable of values into a [Value.Array]. */
internal fun Value.fromIterable(iter: Iterable<Value>): Value = Value.Array(iter.toList())

/** Collect an iterable of key-value pairs into a [Value.Object]. */
internal fun Value.fromIterablePairs(iter: Iterable<Pair<String, Value>>): Value {
    val map = ValueMap()
    for ((k, v) in iter) map.insert(k, v)
    return Value.Object(map)
}
