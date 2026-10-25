package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.serdejson.lexical.generated.cachedfloat80.get_powers

/** Integration adapter for the unchanged machine-generated cached_float80.rs.
 * The existing cached.rs port uses primitive arrays and methods. Convert the
 * generated read-only data once; no constants or arithmetic are reimplemented.
 * This adapter is not counted as generated Rust output.
 */
internal object CachedFloat80 {
    private val powers: ModeratePathPowers = get_powers().let { source ->
        ModeratePathPowers(
            small = ExtendedFloatArray(source.small.mant.toULongArray(), source.small.exp.toIntArray()),
            large = ExtendedFloatArray(source.large.mant.toULongArray(), source.large.exp.toIntArray()),
            smallInt = source.small_int.toULongArray(),
            step = source.step,
            bias = source.bias,
        )
    }

    fun getPowers(): ModeratePathPowers = powers
}
