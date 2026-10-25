package io.github.kotlinmania.serdejson.lexical

import io.github.kotlinmania.serdejson.lexical.generated.errors.ExtendedFloat as ErrorState
import io.github.kotlinmania.serdejson.lexical.generated.errors.__rtkImpl0 as GeneratedFloatErrors

/** Existing internal API over the complete raw-generated lexical/errors.rs.
 * The caller supplies the matching Rust Float constant provider. The read-only
 * scalar snapshot preserves the input value; it makes no allocation/ABI claim.
 * Uses the verified 64-bit profile with overflow checks and debug assertions.
 */
internal interface FloatErrors {
    fun errorScale(): UInt
    fun errorHalfscale(): UInt
    fun errorIsAccurate(count: UInt, fp: ExtendedFloat): Boolean
}

internal fun errorIsAccurate(floatTraits: FloatTraits, count: UInt, fp: ExtendedFloat): Boolean =
    GeneratedFloatErrors.error_is_accurate(count, ErrorState(fp.mant, fp.exp), floatTraits)

internal object ULongFloatErrors : FloatErrors {
    override fun errorScale(): UInt = GeneratedFloatErrors.error_scale()
    override fun errorHalfscale(): UInt = GeneratedFloatErrors.error_halfscale()
    override fun errorIsAccurate(count: UInt, fp: ExtendedFloat): Boolean =
        errorIsAccurate(F64Float, count, fp)
}
