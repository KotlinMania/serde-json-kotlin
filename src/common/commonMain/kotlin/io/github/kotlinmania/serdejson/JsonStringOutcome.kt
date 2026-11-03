package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult

/** The concrete value or original diagnostic from a JSON operation. */
class JsonStringOutcome private constructor(
    val value: String?,
    val error: SerdeError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow(): String =
        when {
            value != null -> value
            error != null -> error.raise()
            else -> kotlin.error("JsonStringOutcome invariant violated")
        }

    fun exceptionOrNull(): SerdeError? = error

    internal fun toSerdeResult(): SerdeResult<String> =
        when {
            value != null -> SerdeResult.success(value)
            error != null -> SerdeResult.failure(error)
            else -> kotlin.error("JsonStringOutcome invariant violated")
        }

    companion object {
        fun success(value: String): JsonStringOutcome = JsonStringOutcome(value, null)

        fun failure(error: SerdeError): JsonStringOutcome = JsonStringOutcome(null, error)

        internal fun fromResult(result: SerdeResult<String>): JsonStringOutcome =
            when (result) {
                is SerdeResult.Success -> success(result.value)
                is SerdeResult.Failure -> failure(result.error)
            }
    }
}
