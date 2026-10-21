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

    fun getOrThrow(): ByteArray = value ?: error!!.raise()

    fun exceptionOrNull(): SerdeError? = error

    internal fun toSerdeResult(): SerdeResult<ByteArray> =
        if (error != null) SerdeResult.failure(error) else SerdeResult.success(value!!)

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
