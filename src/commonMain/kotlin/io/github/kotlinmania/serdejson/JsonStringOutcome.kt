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

    fun getOrThrow(): String = value ?: error!!.raise()

    fun exceptionOrNull(): SerdeError? = error

    internal fun toSerdeResult(): SerdeResult<String> =
        if (error != null) SerdeResult.failure(error) else SerdeResult.success(value!!)

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
