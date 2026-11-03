package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult

/** The concrete value or original diagnostic from a JSON operation. */
class JsonBytesOutcome private constructor(
    val value: ByteArray?,
    val error: SerdeError?,
) {
    init {
        require((value == null) != (error == null))
    }

    val isSuccess: Boolean get() = error == null

    val isFailure: Boolean get() = error != null

    fun getOrThrow(): ByteArray =
        when {
            value != null -> value
            error != null -> error.raise()
            else -> kotlin.error("JsonBytesOutcome invariant violated")
        }

    fun exceptionOrNull(): SerdeError? = error

    internal fun toSerdeResult(): SerdeResult<ByteArray> =
        when {
            value != null -> SerdeResult.success(value)
            error != null -> SerdeResult.failure(error)
            else -> kotlin.error("JsonBytesOutcome invariant violated")
        }

    companion object {
        fun success(value: ByteArray): JsonBytesOutcome = JsonBytesOutcome(value, null)

        fun failure(error: SerdeError): JsonBytesOutcome = JsonBytesOutcome(null, error)

        internal fun fromResult(result: SerdeResult<ByteArray>): JsonBytesOutcome =
            when (result) {
                is SerdeResult.Success -> success(result.value)
                is SerdeResult.Failure -> failure(result.error)
            }
    }
}
