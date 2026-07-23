// port-lint: source src/io/mod.rs
package io.github.kotlinmania.serdejson

/**
 * A tiny facade around I/O operations. In Kotlin Multiplatform, we use expect/actual
 * for platform-specific I/O, but for the core JSON parser/serializer, all I/O
 * goes through this abstraction.
 */

/**
 * The kind of I/O error.
 */
enum class IoErrorKind {
    Other,
}

/**
 * An I/O error. In practice, JSON parsing/serialization in this port works on
 * in-memory buffers, so I/O errors are rare. The type exists to match the
 * upstream API surface.
 */
class IoError(val kind: IoErrorKind, val message: String) {
    override fun toString(): String = message

    companion object {
        fun new(kind: IoErrorKind, message: String): IoError = IoError(kind, message)
    }
}

/**
 * Alias for a result with the error type [IoError].
 */
typealias IoResult<T> = Result<IoError, T>

/**
 * A simple discriminated union for I/O results, matching the upstream pattern
 * without depending on kotlin.Result (which has Swift Export issues).
 */
sealed class Result<out E, out T> {
    class Ok<out T>(val value: T) : Result<Nothing, T>()
    class Err<out E>(val error: E) : Result<E, Nothing>()

    fun isOk(): Boolean = this is Ok
    fun isErr(): Boolean = this is Err

    fun getOrNull(): T? = (this as? Ok)?.value
    fun errorOrNull(): E? = (this as? Err)?.error

    inline fun <R> map(transform: (T) -> R): Result<E, R> =
        when (this) {
            is Ok -> Ok(transform(value))
            is Err -> this
        }

    inline fun <R> mapErr(transform: (E) -> R): Result<R, T> =
        when (this) {
            is Ok -> this
            is Err -> Err(transform(error))
        }
}

/**
 * The Write trait for writing bytes to a sink.
 */
fun interface IoWrite {
    fun write(buf: ByteArray): IoResult<Unit>

    fun writeAll(buf: ByteArray): IoResult<Unit> = write(buf)

    fun flush(): IoResult<Unit> = IoResult.Ok(Unit)
}

/**
 * A [ByteArray] sink that implements [IoWrite].
 */
class VecIoWrite : IoWrite {
    private val buffer: MutableList<Byte> = mutableListOf()

    val bytes: ByteArray
        get() = buffer.toByteArray()

    override fun write(buf: ByteArray): IoResult<Unit> {
        buffer.addAll(buf.toList())
        return IoResult.Ok(Unit)
    }

    override fun writeAll(buf: ByteArray): IoResult<Unit> = write(buf)

    override fun flush(): IoResult<Unit> = IoResult.Ok(Unit)
}