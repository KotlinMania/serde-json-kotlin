package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult

/** The concrete value or original diagnostic from a JSON operation. */
class JsonRawValueOutcome private constructor(
    val value: RawValue?,
    val error: SerdeError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow(): RawValue = value ?: error!!.raise()

    fun exceptionOrNull(): SerdeError? = error

    internal fun toSerdeResult(): SerdeResult<RawValue> =
        if (error != null) SerdeResult.failure(error) else SerdeResult.success(value!!)

    companion object {
        fun success(value: RawValue): JsonRawValueOutcome = JsonRawValueOutcome(value, null)

        fun failure(error: SerdeError): JsonRawValueOutcome = JsonRawValueOutcome(null, error)

        internal fun fromResult(result: SerdeResult<RawValue>): JsonRawValueOutcome =
            when (result) {
                is SerdeResult.Success -> success(result.value)
                is SerdeResult.Failure -> failure(result.error)
            }
    }
}
