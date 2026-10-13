// port-lint: source value/partial_eq.rs
package io.github.kotlinmania.serdejson

fun eqI64(value: Value, other: Long): Boolean = value.asI64() == other

fun eqU64(value: Value, other: ULong): Boolean = value.asU64() == other

fun eqF32(value: Value, other: Float): Boolean =
    when (value) {
        is Value.Number -> {
            val n = value.value
            n.asF32() == other
        }
        else -> false
    }

fun eqF64(value: Value, other: Double): Boolean = value.asF64() == other

fun eqBool(value: Value, other: Boolean): Boolean = value.asBool() == other

fun eqStr(value: Value, other: String): Boolean = value.asStr() == other

fun Value.eq(other: CharSequence): Boolean = eqStr(this, other.toString())

fun Value.eq(other: StringBuilder): Boolean = eqStr(this, other.toString())

fun String.eq(other: Value): Boolean = eqStr(other, this)

fun StringBuilder.eq(other: Value): Boolean = eqStr(other, this.toString())

fun Value.eq(other: String): Boolean = eqStr(this, other)

fun CharSequence.eq(other: Value): Boolean = eqStr(other, this.toString())
