// port-lint: source io/core.rs
package io.github.kotlinmania.serdejson.io

/**
 * The kind of I/O error.
 */
internal enum class IoErrorKind {
    Other,
}

/**
 * An I/O error.
 */
internal class IoError(
    val kind: IoErrorKind,
    val message: String,
) {
    override fun toString(): String = message

    fun fmt(): String = message

    companion object {
        fun new(kind: IoErrorKind, message: String): IoError = IoError(kind, message)
    }
}

/**
 * A result with the error type [IoError].
 */
internal sealed class IoResult<out T> {
    class Ok<out T>(
        val value: T,
    ) : IoResult<T>()

    class Err(
        val error: IoError,
    ) : IoResult<Nothing>()

    fun isOk(): Boolean = this is Ok

    fun isErr(): Boolean = this is Err

    fun getOrNull(): T? = (this as? Ok)?.value

    fun errorOrNull(): IoError? = (this as? Err)?.error

    inline fun <R> map(transform: (T) -> R): IoResult<R> =
        when (this) {
            is Ok -> Ok(transform(value))
            is Err -> this
        }

    companion object {
        fun <T> ok(value: T): IoResult<T> = Ok(value)

        fun err(error: IoError): IoResult<Nothing> = Err(error)
    }
}

/**
 * The Read interface for reading bytes from a source.
 */
internal fun interface IoReader {
    fun readByte(): IoResult<Byte>?
}

/**
 * Adapts an [IoReader] into a byte-by-byte [Iterator] of [IoResult].
 */
internal fun IoReader.bytes(): Iterator<IoResult<Byte>> =
    object : Iterator<IoResult<Byte>> {
        private var buffered: IoResult<Byte>? = null
        private var bufferedReady: Boolean = false
        private var eof: Boolean = false

        private fun ensureBuffered() {
            if (!bufferedReady && !eof) {
                val next = readByte()
                if (next == null) {
                    eof = true
                } else {
                    buffered = next
                    bufferedReady = true
                }
            }
        }

        override fun hasNext(): Boolean {
            ensureBuffered()
            return bufferedReady
        }

        override fun next(): IoResult<Byte> {
            ensureBuffered()
            if (!bufferedReady) {
                throw NoSuchElementException("end of IoReader byte stream")
            }
            bufferedReady = false
            val result = buffered!!
            buffered = null
            return result
        }
    }

/**
 * A [ByteArray] source that implements [IoReader].
 */
internal class SliceIoReader(
    private val bytes: ByteArray,
) : IoReader {
    private var pos: Int = 0

    override fun readByte(): IoResult<Byte>? =
        if (pos < bytes.size) {
            IoResult.ok(bytes[pos++])
        } else {
            null
        }
}

/**
 * The Write interface for writing bytes to a sink.
 */
internal fun interface IoWrite {
    fun write(buf: ByteArray): IoResult<Unit>

    fun writeAll(buf: ByteArray): IoResult<Unit> = write(buf)

    fun flush(): IoResult<Unit> = IoResult.ok(Unit)
}

/**
 * A [ByteArray] sink that implements [IoWrite].
 */
internal class VecIoWrite : IoWrite {
    private val buffer: MutableList<Byte> = mutableListOf()

    val bytes: ByteArray
        get() = buffer.toByteArray()

    override fun write(buf: ByteArray): IoResult<Unit> {
        buffer.addAll(buf.toList())
        return IoResult.ok(Unit)
    }

    override fun writeAll(buf: ByteArray): IoResult<Unit> = write(buf)

    override fun flush(): IoResult<Unit> = IoResult.ok(Unit)
}
