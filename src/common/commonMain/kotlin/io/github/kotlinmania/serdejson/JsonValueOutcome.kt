package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult

/** The concrete value or original diagnostic from a JSON operation. */
class JsonValueOutcome private constructor(
    val value: Value?,
    val error: SerdeError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow(): Value =
        when {
            value != null -> value
            error != null -> error.raise()
            else -> kotlin.error("JsonValueOutcome invariant violated")
        }

    fun exceptionOrNull(): SerdeError? = error

    internal fun toSerdeResult(): SerdeResult<Value> =
        when {
            value != null -> SerdeResult.success(value)
            error != null -> SerdeResult.failure(error)
            else -> kotlin.error("JsonValueOutcome invariant violated")
        }

    companion object {
        fun success(value: Value): JsonValueOutcome = JsonValueOutcome(value, null)

        fun failure(error: SerdeError): JsonValueOutcome = JsonValueOutcome(null, error)

        internal fun fromResult(result: SerdeResult<Value>): JsonValueOutcome =
            when (result) {
                is SerdeResult.Success -> success(result.value)
                is SerdeResult.Failure -> failure(result.error)
            }
    }
}
