// port-lint: source src/read.rs
package io.github.kotlinmania.serdejson

/**
 * Position information for error reporting.
 *
 * In the upstream Rust crate this is returned by the `Read` trait. The Kotlin
 * port uses [JsonParser] directly, which tracks position internally. This
 * class preserves the public API for consumers that need position data.
 */
data class Position(
    /** One-based line number. */
    val line: Int,
    /** One-based column number. */
    val column: Int,
)

/**
 * JSON input source that reads from a slice of bytes.
 *
 * In the upstream Rust crate, `SliceRead` provides byte-level input to the
 * deserializer. In the Kotlin port, [JsonParser] operates on [String] directly
 * and handles all input reading. This class provides a bridge for callers that
 * have a [ByteArray] input.
 */
class SliceRead(private val slice: ByteArray) {
    private var index: Int = 0

    /** Returns the next byte, or null at end of input. */
    fun next(): Byte? =
        if (index < slice.size) slice[index++] else null

    /** Peeks at the next byte without consuming it, or null at end of input. */
    fun peek(): Byte? =
        if (index < slice.size) slice[index] else null

    /** Discards the previously peeked byte. */
    fun discard() {
        index++
    }

    /** Returns the current byte offset from the beginning. */
    fun byteOffset(): Int = index

    /** Returns the current position as a [Position] (line, column). */
    fun position(): Position {
        var line = 1
        var col = 1
        for (i in 0 until index) {
            when (slice[i]) {
                '\n'.code.toByte() -> { line++; col = 1 }
                '\r'.code.toByte() -> { /* handled by \n */ }
                else -> col++
            }
        }
        return Position(line, col)
    }

    /** Parses a JSON-escaped string starting after the opening quote. Returns the decoded string. */
    fun parseStr(): String {
        val sb = StringBuilder()
        while (index < slice.size) {
            val b = slice[index++]
            when {
                b == '"'.code.toByte() -> return sb.toString()
                b == '\\'.code.toByte() -> {
                    if (index >= slice.size) throw IllegalStateException("unexpected end of string")
                    val esc = slice[index++]
                    when (esc) {
                        '"'.code.toByte() -> sb.append('"')
                        '\\'.code.toByte() -> sb.append('\\')
                        '/'.code.toByte() -> sb.append('/')
                        'n'.code.toByte() -> sb.append('\n')
                        'r'.code.toByte() -> sb.append('\r')
                        't'.code.toByte() -> sb.append('\t')
                        'b'.code.toByte() -> sb.append('\b')
                        'f'.code.toByte() -> sb.append('\u000C')
                        'u'.code.toByte() -> {
                            val hexBytes = slice.copyOfRange(index, minOf(index + 4, slice.size))
                            index += 4
                            sb.append(hexBytes.decodeToString().toInt(16).toChar())
                        }
                        else -> throw IllegalStateException("invalid escape: \\${esc.toInt().toChar()}")
                    }
                }
                b < 0x20 -> throw IllegalStateException("control character in string")
                else -> sb.append(b.toInt().toChar())
            }
        }
        throw IllegalStateException("unexpected end of string")
    }
}

/**
 * JSON input source that reads from a UTF-8 string.
 *
 * This is the Kotlin equivalent of the upstream `StrRead`. The primary
 * implementation used by [JsonParser] internally.
 */
class StrRead(private val source: String) {
    private var index: Int = 0

    /** Returns the next character code, or -1 at end of input. */
    fun next(): Int =
        if (index < source.length) source[index++].code else -1

    /** Peeks at the next character code without consuming it, or -1 at end of input. */
    fun peek(): Int =
        if (index < source.length) source[index].code else -1

    /** Discards the previously peeked character. */
    fun discard() {
        index++
    }

    /** Returns the current byte offset. */
    fun byteOffset(): Int = index

    /** Returns the current position (line, column). */
    fun position(): Position {
        var line = 1
        var col = 1
        for (i in 0 until index) {
            when (source[i]) {
                '\n' -> { line++; col = 1 }
                '\r' -> { /* handled by \n */ }
                else -> col++
            }
        }
        return Position(line, col)
    }

    /** Parses a JSON-escaped string starting after the opening quote. */
    fun parseStr(): String {
        val sb = StringBuilder()
        while (index < source.length) {
            val c = source[index++]
            when {
                c == '"' -> return sb.toString()
                c == '\\' -> {
                    if (index >= source.length) throw IllegalStateException("unexpected end of string")
                    val esc = source[index++]
                    when (esc) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            val hex = source.substring(index, minOf(index + 4, source.length))
                            index += 4
                            sb.append(hex.toInt(16).toChar())
                        }
                        else -> throw IllegalStateException("invalid escape: \\$esc")
                    }
                }
                c.code < 0x20 -> throw IllegalStateException("control character in string")
                else -> sb.append(c)
            }
        }
        throw IllegalStateException("unexpected end of string")
    }
}