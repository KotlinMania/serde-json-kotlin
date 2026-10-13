// port-lint: source value/from.rs
package io.github.kotlinmania.serdejson

/** Convert a [Byte] to a [Value]. */
fun Value.Companion.from(n: Byte): Value = Value.Number(JsonNumber.fromI64(n.toLong()))

/** Convert a [Short] to a [Value]. */
fun Value.Companion.from(n: Short): Value = Value.Number(JsonNumber.fromI64(n.toLong()))

/** Convert an [Int] to a [Value]. */
fun Value.Companion.from(n: Int): Value = Value.Number(JsonNumber.fromI64(n.toLong()))

/** Convert a [Long] to a [Value]. */
fun Value.Companion.from(n: Long): Value = Value.Number(JsonNumber.fromI64(n))

/** Convert a [UByte] to a [Value]. */
fun Value.Companion.from(n: UByte): Value = Value.Number(JsonNumber.fromU64(n.toULong()))

/** Convert a [UShort] to a [Value]. */
fun Value.Companion.from(n: UShort): Value = Value.Number(JsonNumber.fromU64(n.toULong()))

/** Convert a [UInt] to a [Value]. */
fun Value.Companion.from(n: UInt): Value = Value.Number(JsonNumber.fromU64(n.toULong()))

/** Convert a [ULong] to a [Value]. */
fun Value.Companion.from(n: ULong): Value = Value.Number(JsonNumber.fromU64(n))

/** Convert a [Float] to a [Value], or [Value.Null] if infinite or NaN. */
fun Value.Companion.from(f: Float): Value =
    JsonNumber.fromF32(f)?.let { Value.Number(it) } ?: Value.Null

/** Convert a [Double] to a [Value], or [Value.Null] if infinite or NaN. */
fun Value.Companion.from(f: Double): Value =
    JsonNumber.fromF64(f)?.let { Value.Number(it) } ?: Value.Null

/** Convert a [Boolean] to a [Value]. */
fun Value.Companion.from(b: Boolean): Value = Value.Bool(b)

/** Convert a [String] to a [Value]. */
fun Value.Companion.from(s: String): Value = Value.Str(s)

/** Convert a [JsonNumber] to a [Value]. */
fun Value.Companion.from(n: JsonNumber): Value = Value.Number(n)

/** Convert a [ValueMap] to a [Value]. */
fun Value.Companion.from(m: ValueMap): Value = Value.Object(m)

/** Convert a [List] of [Value] to a [Value.Array]. */
fun Value.Companion.from(list: List<Value>): Value = Value.Array(list)

/** Convert an array of [Value] to a [Value.Array]. */
fun Value.Companion.from(array: Array<Value>): Value = Value.Array(array.toList())

/** Convert `Unit` to [Value.Null]. */
fun Value.Companion.from(unit: Unit): Value = Value.Null

/** Convert an optional value to [Value]. */
fun Value.Companion.from(opt: Value?): Value = opt ?: Value.Null

/** Create a [Value.Array] by collecting an iterator of array elements. */
fun Value.Companion.fromIter(iter: Iterable<Value>): Value = Value.Array(iter.toList())

/** Create a [Value.Object] by collecting an iterator of key-value pairs. */
@kotlin.jvm.JvmName("fromIterPairs")
fun Value.Companion.fromIter(iter: Iterable<Pair<String, Value>>): Value {
    val map = ValueMap()
    for ((k, v) in iter) {
        map.insert(k, v)
    }
    return Value.Object(map)
}
