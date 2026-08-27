// port-lint: source macros.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serdecore.ser.Serialize

/**
 * Builder DSL for constructing [Value] objects, the Kotlin equivalent of the
 * upstream `json!` macro.
 *
 * Usage:
 * ```
 * val v = json {
 *     "code" to 200
 *     "success" to true
 *     "payload" to obj {
 *         "features" to arr("serde", "json")
 *         "homepage" to null
 *     }
 * }
 * ```
 */
fun json(block: JsonObjectBuilder.() -> Unit): Value {
    val builder = JsonObjectBuilder()
    builder.block()
    return builder.build()
}

/** Build a JSON object. */
class JsonObjectBuilder {
    private val map = ValueMap()

    /** Add a key-value pair to the object. */
    infix fun String.to(value: Value) {
        map.insert(this, value)
    }

    /** Add a key-value pair where the value is a [String]. */
    infix fun String.to(value: String) {
        map.insert(this, Value.Str(value))
    }

    /** Add a key-value pair where the value is a [Boolean]. */
    infix fun String.to(value: Boolean) {
        map.insert(this, Value.Bool(value))
    }

    /** Add a key-value pair where the value is an [Int]. */
    infix fun String.to(value: Int) {
        map.insert(this, Value.Number(JsonNumber.fromI64(value.toLong())))
    }

    /** Add a key-value pair where the value is a [Long]. */
    infix fun String.to(value: Long) {
        map.insert(this, Value.Number(JsonNumber.fromI64(value)))
    }

    /** Add a key-value pair where the value is a [ULong]. */
    infix fun String.to(value: ULong) {
        map.insert(this, Value.Number(JsonNumber.fromU64(value)))
    }

    /** Add a key-value pair where the value is a [Double]. */
    infix fun String.to(value: Double) {
        val n = JsonNumber.fromF64(value)
        map.insert(this, if (n != null) Value.Number(n) else Value.Null)
    }

    /** Add a key-value pair where the value is a [Float]. */
    infix fun String.to(value: Float) {
        val n = JsonNumber.fromF32(value)
        map.insert(this, if (n != null) Value.Number(n) else Value.Null)
    }

    /** Add a key-value pair where the value is null. */
    infix fun String.to(value: Nothing?) {
        map.insert(this, Value.Null)
    }

    /** Add a key-value pair where the value is a [Serialize]. */
    infix fun String.to(value: Serialize) {
        map.insert(this, toValue(value).getOrThrow())
    }

    /** Build the final [Value.Object]. */
    fun build(): Value = Value.Object(map)
}

/** Build a JSON array from the given values. */
fun arr(vararg elements: Value): Value = Value.Array(elements.toList())

/** Build a JSON array from a list of values. */
fun arr(elements: List<Value>): Value = Value.Array(elements)

/** Build a JSON object from key-value pairs. */
fun obj(vararg pairs: Pair<String, Value>): Value {
    val map = ValueMap()
    for ((k, v) in pairs) map.insert(k, v)
    return Value.Object(map)
}

/** Build a JSON object from a block. */
fun obj(block: JsonObjectBuilder.() -> Unit): Value = json(block)
