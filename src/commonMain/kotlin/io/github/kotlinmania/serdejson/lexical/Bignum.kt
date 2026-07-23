// port-lint: source tmp/serde_json/src/lexical/bignum.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

/**
 * Big integer type definition.
 *
 * Internal storage for the Bigint, in little-endian order.
 */
class Bigint(
    /** Internal storage for the Bigint, in little-endian order. */
    var data: MutableList<ULong> = ArrayList(20)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Bigint) return false
        return data == other.data
    }

    override fun hashCode(): Int = data.hashCode()

    override fun toString(): String = "Bigint(data=$data)"

    companion object {
        /** Create a [Bigint] with the default capacity. */
        fun default(): Bigint = Bigint(ArrayList(20))
    }
}