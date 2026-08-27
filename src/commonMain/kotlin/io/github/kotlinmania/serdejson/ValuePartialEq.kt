// port-lint: source value/partial_eq.rs
package io.github.kotlinmania.serdejson

/** Compare a [Value] with a [Long] for equality. */
fun eqI64(value: Value, other: Long): Boolean = value.asI64() == other

/** Compare a [Value] with a [ULong] for equality. */
fun eqU64(value: Value, other: ULong): Boolean = value.asU64() == other

/** Compare a [Value] with a [Float] for equality. */
fun eqF32(value: Value, other: Float): Boolean =
    when (value) {
        is Value.Number -> value.value.asF32() == other
        else -> false
    }

/** Compare a [Value] with a [Double] for equality. */
fun eqF64(value: Value, other: Double): Boolean = value.asF64() == other

/** Compare a [Value] with a [Boolean] for equality. */
fun eqBool(value: Value, other: Boolean): Boolean = value.asBool() == other

/** Compare a [Value] with a [String] for equality. */
fun eqStr(value: Value, other: String): Boolean = value.asStr() == other

/** Compare a [Value] with another [Value] for structural equality. */
fun eqValue(a: Value, b: Value): Boolean {
    if (a is Value.Number && b is Value.Number) {
        val an = a.value
        val bn = b.value
        if (an.isF64() || bn.isF64()) return an.asF64() == bn.asF64()
        if (an.isI64() && bn.isI64()) return an.asI64() == bn.asI64()
        if (an.isU64() && bn.isU64()) return an.asU64() == bn.asU64()
        // Cross-compare i64/u64 by converting through Long
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
