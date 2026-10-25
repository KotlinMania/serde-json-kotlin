package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.serdejson.lexical.generated.cachedfloat80.get_powers
import io.github.kotlinmania.serdejson.lexical.generated.cached.ExtendedFloatArray as GeneratedArray
import io.github.kotlinmania.serdejson.lexical.generated.cached.ModeratePathPowers as GeneratedPowers
import io.github.kotlinmania.serdejson.lexical.generated.cached.__rtkImpl0 as GeneratedCache

/** Integration adapter for the unchanged machine-generated cached_float80.rs.
 * The cached.rs compatibility adapter uses primitive arrays. Convert the
 * generated read-only data once; no constants or arithmetic are reimplemented.
 * This adapter is not counted as generated Rust output.
 */
internal object CachedFloat80 {
    private val powers: ModeratePathPowers = GeneratedCache.get_powers().let { source ->
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

// Read the previously generated table once, retaining its immutable values in
// the complete generated cached.rs record types. No constants are reimplemented.
private object GeneratedCachedData {
    val powers: GeneratedPowers = get_powers().let { source ->
        GeneratedPowers(
            GeneratedArray(source.small.mant, source.small.exp),
            GeneratedArray(source.large.mant, source.large.exp),
            source.small_int, source.step, source.bias,
        )
    }
}

internal fun generatedCachedPowers(): GeneratedPowers = GeneratedCachedData.powers
