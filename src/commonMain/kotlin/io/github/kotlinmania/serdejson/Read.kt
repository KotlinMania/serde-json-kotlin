// port-lint: source read.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdejson.io.IoReader
import io.github.kotlinmania.serdejson.io.IoResult
import io.github.kotlinmania.serdejson.io.bytes

/**
 * Position information for error reporting.
 */
data class Position(
    /** One-based line number. */
    val line: Int,
    /** One-based column number. */
    val column: Int,
)

/**
 * Trait used by the deserializer for iterating over input.
 */
internal interface Read {
    val shouldEarlyReturnIfFailed: Boolean

    fun next(): SerdeResult<Byte?>

    fun peek(): SerdeResult<Byte?>

    /** Only valid after a call to [peek]. Discards the peeked byte. */
    fun discard()

    /** Position of the most recent call to [next]. */
    fun position(): Position

    /** Position of the current [peek] byte. */
    fun peekPosition(): Position

    /** Offset from the beginning of the input to the next byte that would be returned by [next]. */
    fun byteOffset(): Int

    /** Assumes the previous byte was a quotation mark. Parses a JSON-escaped string until the next quote. */
    fun parseStr(scratch: MutableList<Byte>): SerdeResult<Reference<String>>

    /** Assumes the previous byte was a quotation mark. Parses raw string bytes until the next quote. */
    fun parseStrRaw(scratch: MutableList<Byte>): SerdeResult<Reference<ByteArray>>

    /** Assumes the previous byte was a quotation mark. Skips past the closing quotation mark. */
    fun ignoreStr(): SerdeResult<Unit>

    /** Assumes the previous characters were `\u` within a string. Decodes 4 hex digits into a `UShort`. */
    fun decodeHexEscape(): SerdeResult<UShort>

    /** Switch raw buffering mode on. */
    fun beginRawBuffering()

    /** Switch raw buffering mode off and provide the raw buffered data to the given [visitor]. */
    fun <V> endRawBuffering(visitor: Visitor<V>): SerdeResult<V>

    /** Record a persistent failure flag for stream readers that must stop on error. */
    fun setFailed(failed: BooleanArray)
}

/**
 * Borrowed-or-copied reference returned by [Read.parseStr] and [Read.parseStrRaw].
 */
internal sealed class Reference<out T> {
    class Borrowed<out T>(
        val value: T,
    ) : Reference<T>()

    class Copied<out T>(
        val value: T,
    ) : Reference<T>()

    fun deref(): T =
        when (this) {
            is Borrowed -> value
            is Copied -> value
        }
}

/**
 * Marker interface for input sources guaranteed not to produce more values after EOF.
 */
internal interface Fused

/**
 * JSON input source that reads from a byte-oriented [IoReader] stream.
 */
internal class IoRead<R : IoReader>(
    reader: R,
) : Read,
    Fused {
    private val iter: LineColIterator<Iterator<IoResult<Byte>>> = LineColIterator.new(reader.bytes())

    /** Temporary storage of peeked byte. */
    private var ch: Byte? = null

    /** Temporary storage of raw buffering bytes. */
    private val rawBuffer: MutableList<Byte> = ArrayList()
    private var rawBuffering: Boolean = false

    override val shouldEarlyReturnIfFailed: Boolean
        get() = true

    private fun <T> parseStrBytes(
        scratch: MutableList<Byte>,
        validate: Boolean,
        result: (IoRead<R>, ByteArray) -> SerdeResult<T>,
    ): SerdeResult<Reference<T>> {
        while (true) {
            val b = nextOrEof(this).getOrElse { return SerdeResult.failure(it) }
            val u = b.toInt() and 0xFF
            if (!isEscape(u, validate)) {
                scratch.add(b)
                continue
            }
            when (b) {
                '"'.code.toByte() -> {
                    return result(this, scratch.toByteArray()).map { Reference.Copied(it) }
                }
                '\\'.code.toByte() -> {
                    parseEscape(this, validate, scratch).getOrElse { return SerdeResult.failure(it) }
                }
                else -> {
                    if (validate) {
                        return error(this, ErrorCode.ControlCharacterWhileParsingString)
                    }
                    scratch.add(b)
                }
            }
        }
    }

    override fun next(): SerdeResult<Byte?> {
        val peeked = ch
        if (peeked != null) {
            ch = null
            if (rawBuffering) {
                rawBuffer.add(peeked)
            }
            return SerdeResult.success(peeked)
        }
        return when (val item = iter.nextOrNull()) {
            null -> SerdeResult.success(null)
            is IoResult.Ok -> {
                if (rawBuffering) {
                    rawBuffer.add(item.value)
                }
                SerdeResult.success(item.value)
            }
            is IoResult.Err -> SerdeResult.failure(SerdeError.custom(JsonError.io(item.error.fmt()).toString()))
        }
    }

    override fun peek(): SerdeResult<Byte?> {
        val peeked = ch
        if (peeked != null) {
            return SerdeResult.success(peeked)
        }
        return when (val item = iter.nextOrNull()) {
            null -> SerdeResult.success(null)
            is IoResult.Ok -> {
                ch = item.value
                SerdeResult.success(ch)
            }
            is IoResult.Err -> SerdeResult.failure(SerdeError.custom(JsonError.io(item.error.fmt()).toString()))
        }
    }

    override fun discard() {
        val peeked = ch
        if (peeked != null && rawBuffering) {
            rawBuffer.add(peeked)
        }
        ch = null
    }

    override fun position(): Position = Position(line = iter.line(), column = iter.col())

    override fun peekPosition(): Position {
        peek()
        return position()
    }

    override fun byteOffset(): Int =
        if (ch != null) {
            iter.byteOffset() - 1
        } else {
            iter.byteOffset()
        }

    override fun parseStr(scratch: MutableList<Byte>): SerdeResult<Reference<String>> =
        parseStrBytes(scratch, true) { reader, bytes -> asStr(reader, bytes) }

    override fun parseStrRaw(scratch: MutableList<Byte>): SerdeResult<Reference<ByteArray>> =
        parseStrBytes(scratch, false) { _, bytes -> SerdeResult.success(bytes) }

    override fun ignoreStr(): SerdeResult<Unit> {
        while (true) {
            val b = nextOrEof(this).getOrElse { return SerdeResult.failure(it) }
            val u = b.toInt() and 0xFF
            if (!isEscape(u, true)) {
                continue
            }
            when (b) {
                '"'.code.toByte() -> {
                    return SerdeResult.success(Unit)
                }
                '\\'.code.toByte() -> {
                    ignoreEscape(this).getOrElse { return SerdeResult.failure(it) }
                }
                else -> {
                    return error(this, ErrorCode.ControlCharacterWhileParsingString)
                }
            }
        }
    }

    override fun decodeHexEscape(): SerdeResult<UShort> {
        val a = nextOrEof(this).getOrElse { return SerdeResult.failure(it) }
        val b = nextOrEof(this).getOrElse { return SerdeResult.failure(it) }
        val c = nextOrEof(this).getOrElse { return SerdeResult.failure(it) }
        val d = nextOrEof(this).getOrElse { return SerdeResult.failure(it) }
        val decoded = decodeFourHexDigits(a, b, c, d)
        return if (decoded != null) {
            SerdeResult.success(decoded)
        } else {
            error(this, ErrorCode.InvalidEscape)
        }
    }

    override fun beginRawBuffering() {
        rawBuffer.clear()
        rawBuffering = true
    }

    override fun <V> endRawBuffering(visitor: Visitor<V>): SerdeResult<V> {
        if (!rawBuffering) {
            return SerdeResult.failure(SerdeError.custom("raw buffering was not started"))
        }
        val raw = rawBuffer.toByteArray()
        rawBuffer.clear()
        rawBuffering = false
        val str = asStr(this, raw).getOrElse { return SerdeResult.failure(it) }
        return visitor.visitMap(OwnedRawDeserializer(str))
    }

    override fun setFailed(failed: BooleanArray) {
        if (failed.isNotEmpty()) {
            failed[0] = true
        }
    }

    companion object {
        fun <R : IoReader> new(reader: R): IoRead<R> = IoRead(reader)
    }
}

/**
 * JSON input source that reads from a slice of UTF-8 bytes.
 */
class SliceRead(
    internal val slice: ByteArray,
) : Fused {
    internal var index: Int = 0
    internal var rawBufferingStartIndex: Int = 0

    internal fun positionOfIndex(i: Int): Position {
        var startOfLine = 0
        var line = 1
        val limit = minOf(i, slice.size)
        for (pos in 0 until limit) {
            if (slice[pos] == '\n'.code.toByte()) {
                line += 1
                startOfLine = pos + 1
            }
        }
        return Position(line = line, column = 1 + limit - startOfLine)
    }

    internal fun skipToEscape(forbidControlCharacters: Boolean) {
        if (index == slice.size || isEscape(slice[index].toInt() and 0xFF, forbidControlCharacters)) {
            return
        }
        index += 1
        skipToEscapeSlow(forbidControlCharacters)
    }

    internal fun skipToEscapeSlow(forbidControlCharacters: Boolean) {
        while (index < slice.size && !isEscape(slice[index].toInt() and 0xFF, forbidControlCharacters)) {
            index += 1
        }
    }

    internal fun <T> parseStrBytes(
        scratch: MutableList<Byte>,
        validate: Boolean,
        result: (Read, ByteArray) -> SerdeResult<T>,
    ): SerdeResult<Reference<T>> {
        val adapter = toRead()
        var start = index
        while (true) {
            skipToEscape(validate)
            if (index == slice.size) {
                return error(adapter, ErrorCode.EofWhileParsingString)
            }
            when (val ch = slice[index]) {
                '"'.code.toByte() -> {
                    if (scratch.isEmpty()) {
                        val borrowed = slice.copyOfRange(start, index)
                        index += 1
                        return result(adapter, borrowed).map { Reference.Borrowed(it) }
                    } else {
                        for (k in start until index) {
                            scratch.add(slice[k])
                        }
                        index += 1
                        return result(adapter, scratch.toByteArray()).map { Reference.Copied(it) }
                    }
                }
                '\\'.code.toByte() -> {
                    for (k in start until index) {
                        scratch.add(slice[k])
                    }
                    index += 1
                    parseEscape(adapter, validate, scratch).getOrElse { return SerdeResult.failure(it) }
                    start = index
                }
                else -> {
                    index += 1
                    if (validate && (ch.toInt() and 0xFF) < 0x20) {
                        return error(adapter, ErrorCode.ControlCharacterWhileParsingString)
                    }
                }
            }
        }
    }

    /** Returns the next byte, or null at end of input. */
    fun next(): Byte? =
        if (index < slice.size) {
            val ch = slice[index]
            index += 1
            ch
        } else {
            null
        }

    /** Peeks at the next byte without consuming it, or null at end of input. */
    fun peek(): Byte? =
        if (index < slice.size) {
            slice[index]
        } else {
            null
        }

    /** Discards the previously peeked byte. */
    fun discard() {
        index += 1
    }

    /** Position of the most recent call to [next]. */
    fun position(): Position = positionOfIndex(index)

    /** Position of the current [peek] byte. */
    fun peekPosition(): Position = positionOfIndex(minOf(slice.size, index + 1))

    /** Offset from the beginning of the input to the next byte that would be returned by [next]. */
    fun byteOffset(): Int = index

    /** Parses a JSON-escaped UTF-8 string starting after the opening quote into a [JsonStringOutcome]. */
    fun parseStrOutcome(): JsonStringOutcome {
        val scratch = ArrayList<Byte>()
        return JsonStringOutcome.fromResult(toRead().parseStr(scratch).map { it.deref() })
    }

    /** Parses a JSON-escaped UTF-8 string starting after the opening quote. */
    fun parseStr(): String = parseStrOutcome().getOrThrow()

    internal fun toRead(): Read = SliceReadAdapter(this)

    companion object {
        fun new(slice: ByteArray): SliceRead = SliceRead(slice)
    }
}

internal class SliceReadAdapter(
    private val delegate: SliceRead,
) : Read,
    Fused {
    override val shouldEarlyReturnIfFailed: Boolean
        get() = false

    override fun next(): SerdeResult<Byte?> = SerdeResult.success(delegate.next())

    override fun peek(): SerdeResult<Byte?> = SerdeResult.success(delegate.peek())

    override fun discard() {
        delegate.discard()
    }

    override fun position(): Position = delegate.position()

    override fun peekPosition(): Position = delegate.peekPosition()

    override fun byteOffset(): Int = delegate.byteOffset()

    override fun parseStr(scratch: MutableList<Byte>): SerdeResult<Reference<String>> =
        delegate.parseStrBytes(scratch, true) { reader, bytes -> asStr(reader, bytes) }

    override fun parseStrRaw(scratch: MutableList<Byte>): SerdeResult<Reference<ByteArray>> =
        delegate.parseStrBytes(scratch, false) { _, bytes -> SerdeResult.success(bytes) }

    override fun ignoreStr(): SerdeResult<Unit> {
        while (true) {
            delegate.skipToEscape(true)
            if (delegate.index == delegate.slice.size) {
                return error(this, ErrorCode.EofWhileParsingString)
            }
            when (delegate.slice[delegate.index]) {
                '"'.code.toByte() -> {
                    delegate.index += 1
                    return SerdeResult.success(Unit)
                }
                '\\'.code.toByte() -> {
                    delegate.index += 1
                    ignoreEscape(this).getOrElse { return SerdeResult.failure(it) }
                }
                else -> {
                    return error(this, ErrorCode.ControlCharacterWhileParsingString)
                }
            }
        }
    }

    override fun decodeHexEscape(): SerdeResult<UShort> {
        if (delegate.index + 4 > delegate.slice.size) {
            delegate.index = delegate.slice.size
            return error(this, ErrorCode.EofWhileParsingString)
        }
        val a = delegate.slice[delegate.index]
        val b = delegate.slice[delegate.index + 1]
        val c = delegate.slice[delegate.index + 2]
        val d = delegate.slice[delegate.index + 3]
        delegate.index += 4
        val decoded = decodeFourHexDigits(a, b, c, d)
        return if (decoded != null) {
            SerdeResult.success(decoded)
        } else {
            error(this, ErrorCode.InvalidEscape)
        }
    }

    override fun beginRawBuffering() {
        delegate.rawBufferingStartIndex = delegate.index
    }

    override fun <V> endRawBuffering(visitor: Visitor<V>): SerdeResult<V> {
        val raw = delegate.slice.copyOfRange(delegate.rawBufferingStartIndex, delegate.index)
        val str = asStr(this, raw).getOrElse { return SerdeResult.failure(it) }
        return visitor.visitMap(BorrowedRawDeserializer(str))
    }

    override fun setFailed(failed: BooleanArray) = Unit
}

/**
 * JSON input source that reads from a UTF-8 string.
 */
class StrRead(
    source: String,
) : Fused {
    private val delegate: SliceRead = SliceRead(source.encodeToByteArray())

    /** Returns the next UTF-8 byte (`0..255`), or `-1` at end of input. */
    fun next(): Int = delegate.next()?.let { it.toInt() and 0xFF } ?: -1

    /** Peeks at the next UTF-8 byte (`0..255`) without consuming it, or `-1` at end of input. */
    fun peek(): Int = delegate.peek()?.let { it.toInt() and 0xFF } ?: -1

    /** Discards the previously peeked byte. */
    fun discard() {
        delegate.discard()
    }

    /** Position of the most recent call to [next]. */
    fun position(): Position = delegate.position()

    /** Position of the current [peek] byte. */
    fun peekPosition(): Position = delegate.peekPosition()

    /** Offset in UTF-8 bytes from the beginning of the input. */
    fun byteOffset(): Int = delegate.byteOffset()

    /** Parses a JSON-escaped UTF-8 string starting after the opening quote into a [JsonStringOutcome]. */
    fun parseStrOutcome(): JsonStringOutcome = delegate.parseStrOutcome()

    /** Parses a JSON-escaped UTF-8 string starting after the opening quote. */
    fun parseStr(): String = delegate.parseStr()

    internal fun toRead(): Read = StrReadAdapter(delegate)

    companion object {
        fun new(s: String): StrRead = StrRead(s)
    }
}

internal class StrReadAdapter(
    private val delegate: SliceRead,
) : Read,
    Fused {
    private val sliceAdapter: SliceReadAdapter = SliceReadAdapter(delegate)

    override val shouldEarlyReturnIfFailed: Boolean
        get() = false

    override fun next(): SerdeResult<Byte?> = sliceAdapter.next()

    override fun peek(): SerdeResult<Byte?> = sliceAdapter.peek()

    override fun discard() {
        sliceAdapter.discard()
    }

    override fun position(): Position = sliceAdapter.position()

    override fun peekPosition(): Position = sliceAdapter.peekPosition()

    override fun byteOffset(): Int = sliceAdapter.byteOffset()

    override fun parseStr(scratch: MutableList<Byte>): SerdeResult<Reference<String>> =
        sliceAdapter.parseStr(scratch)

    override fun parseStrRaw(scratch: MutableList<Byte>): SerdeResult<Reference<ByteArray>> =
        sliceAdapter.parseStrRaw(scratch)

    override fun ignoreStr(): SerdeResult<Unit> = sliceAdapter.ignoreStr()

    override fun decodeHexEscape(): SerdeResult<UShort> = sliceAdapter.decodeHexEscape()

    override fun beginRawBuffering() {
        sliceAdapter.beginRawBuffering()
    }

    override fun <V> endRawBuffering(visitor: Visitor<V>): SerdeResult<V> =
        sliceAdapter.endRawBuffering(visitor)

    override fun setFailed(failed: BooleanArray) = sliceAdapter.setFailed(failed)
}

/**
 * Forwarding [Read] wrapper corresponding to mutable reference delegation.
 */
internal class MutRead<R : Read>(
    private val delegate: R,
) : Read {
    override val shouldEarlyReturnIfFailed: Boolean
        get() = delegate.shouldEarlyReturnIfFailed

    override fun next(): SerdeResult<Byte?> = delegate.next()

    override fun peek(): SerdeResult<Byte?> = delegate.peek()

    override fun discard() {
        delegate.discard()
    }

    override fun position(): Position = delegate.position()

    override fun peekPosition(): Position = delegate.peekPosition()

    override fun byteOffset(): Int = delegate.byteOffset()

    override fun parseStr(scratch: MutableList<Byte>): SerdeResult<Reference<String>> =
        delegate.parseStr(scratch)

    override fun parseStrRaw(scratch: MutableList<Byte>): SerdeResult<Reference<ByteArray>> =
        delegate.parseStrRaw(scratch)

    override fun ignoreStr(): SerdeResult<Unit> = delegate.ignoreStr()

    override fun decodeHexEscape(): SerdeResult<UShort> = delegate.decodeHexEscape()

    override fun beginRawBuffering() {
        delegate.beginRawBuffering()
    }

    override fun <V> endRawBuffering(visitor: Visitor<V>): SerdeResult<V> =
        delegate.endRawBuffering(visitor)

    override fun setFailed(failed: BooleanArray) {
        delegate.setFailed(failed)
    }
}

// Lookup table of bytes that must be escaped or rejected inside a JSON string:
// ASCII control characters 0x00..0x1F, '"' (0x22), and '\\' (0x5C).
private val ESCAPE: BooleanArray =
    BooleanArray(256) { ch ->
        ch < 0x20 || ch == '"'.code || ch == '\\'.code
    }

internal fun isEscape(ch: Int, includingControlCharacters: Boolean): Boolean =
    ESCAPE[ch and 0xFF] && (includingControlCharacters || (ch and 0xFF) >= 0x20)

internal fun nextOrEof(read: Read): SerdeResult<Byte> {
    val byte = read.next().getOrElse { return SerdeResult.failure(it) }
    return if (byte != null) {
        SerdeResult.success(byte)
    } else {
        error(read, ErrorCode.EofWhileParsingString)
    }
}

internal fun peekOrEof(read: Read): SerdeResult<Byte> {
    val byte = read.peek().getOrElse { return SerdeResult.failure(it) }
    return if (byte != null) {
        SerdeResult.success(byte)
    } else {
        error(read, ErrorCode.EofWhileParsingString)
    }
}

internal fun <T> error(read: Read, reason: ErrorCode): SerdeResult<T> {
    val pos = read.position()
    return SerdeResult.failure(SerdeError.custom(JsonError.syntax(reason, pos.line, pos.column).toString()))
}

internal fun asStr(read: Read, slice: ByteArray): SerdeResult<String> =
    try {
        SerdeResult.success(slice.decodeToString(throwOnInvalidSequence = true))
    } catch (_: CharacterCodingException) {
        error(read, ErrorCode.InvalidUnicodeCodePoint)
    }

/**
 * Parses a JSON escape sequence and appends it into the scratch space. Assumes
 * the previous byte read was a backslash.
 */
internal fun parseEscape(
    read: Read,
    validate: Boolean,
    scratch: MutableList<Byte>,
): SerdeResult<Unit> {
    val ch = nextOrEof(read).getOrElse { return SerdeResult.failure(it) }
    return when (ch) {
        '"'.code.toByte() -> {
            scratch.add('"'.code.toByte())
            SerdeResult.success(Unit)
        }
        '\\'.code.toByte() -> {
            scratch.add('\\'.code.toByte())
            SerdeResult.success(Unit)
        }
        '/'.code.toByte() -> {
            scratch.add('/'.code.toByte())
            SerdeResult.success(Unit)
        }
        'b'.code.toByte() -> {
            scratch.add(0x08.toByte())
            SerdeResult.success(Unit)
        }
        'f'.code.toByte() -> {
            scratch.add(0x0C.toByte())
            SerdeResult.success(Unit)
        }
        'n'.code.toByte() -> {
            scratch.add('\n'.code.toByte())
            SerdeResult.success(Unit)
        }
        'r'.code.toByte() -> {
            scratch.add('\r'.code.toByte())
            SerdeResult.success(Unit)
        }
        't'.code.toByte() -> {
            scratch.add('\t'.code.toByte())
            SerdeResult.success(Unit)
        }
        'u'.code.toByte() -> parseUnicodeEscape(read, validate, scratch)
        else -> error(read, ErrorCode.InvalidEscape)
    }
}

/**
 * Parses a JSON `\u` escape and appends it into the scratch space. Assumes `\u`
 * has just been read.
 */
internal fun parseUnicodeEscape(
    read: Read,
    validate: Boolean,
    scratch: MutableList<Byte>,
): SerdeResult<Unit> {
    var n = read.decodeHexEscape().getOrElse { return SerdeResult.failure(it) }.toInt()

    // Non-BMP characters are encoded as a sequence of two hex escapes,
    // representing UTF-16 surrogates. If deserializing a UTF-8 string the
    // surrogates are required to be paired, whereas deserializing a byte string
    // accepts lone surrogates.
    if (validate && n in 0xDC00..0xDFFF) {
        return error(read, ErrorCode.LoneLeadingSurrogateInHexEscape)
    }

    while (true) {
        if (n !in 0xD800..0xDBFF) {
            // Every u16 outside of the surrogate ranges is guaranteed to be a legal codepoint.
            pushWtf8Codepoint(n, scratch)
            return SerdeResult.success(Unit)
        }

        // n is a leading surrogate; we are now expecting a trailing surrogate.
        val n1 = n

        if (peekOrEof(read).getOrElse { return SerdeResult.failure(it) } == '\\'.code.toByte()) {
            read.discard()
        } else {
            return if (validate) {
                read.discard()
                error(read, ErrorCode.UnexpectedEndOfHexEscape)
            } else {
                pushWtf8Codepoint(n1, scratch)
                SerdeResult.success(Unit)
            }
        }

        if (peekOrEof(read).getOrElse { return SerdeResult.failure(it) } == 'u'.code.toByte()) {
            read.discard()
        } else {
            return if (validate) {
                read.discard()
                error(read, ErrorCode.UnexpectedEndOfHexEscape)
            } else {
                pushWtf8Codepoint(n1, scratch)
                parseEscape(read, validate, scratch)
            }
        }

        val n2 = read.decodeHexEscape().getOrElse { return SerdeResult.failure(it) }.toInt()

        if (n2 !in 0xDC00..0xDFFF) {
            if (validate) {
                return error(read, ErrorCode.LoneLeadingSurrogateInHexEscape)
            }
            pushWtf8Codepoint(n1, scratch)
            // If n2 is a leading surrogate, we need to restart.
            n = n2
            continue
        }

        // This value is in range U+10000..=U+10FFFF, which is always a valid codepoint.
        val combined = (((n1 - 0xD800) shl 10) or (n2 - 0xDC00)) + 0x10000
        pushWtf8Codepoint(combined, scratch)
        return SerdeResult.success(Unit)
    }
}

/**
 * Adds a WTF-8 codepoint to the end of the buffer. This is a faster
 * implementation of encoding a 1..4 byte sequence since `0 <= n <= 0x10FFFF`.
 */
internal fun pushWtf8Codepoint(n: Int, scratch: MutableList<Byte>) {
    when {
        n <= 0x7F -> {
            scratch.add(n.toByte())
        }
        n <= 0x7FF -> {
            scratch.add((0xC0 or (n shr 6)).toByte())
            scratch.add((0x80 or (n and 0x3F)).toByte())
        }
        n <= 0xFFFF -> {
            scratch.add((0xE0 or (n shr 12)).toByte())
            scratch.add((0x80 or ((n shr 6) and 0x3F)).toByte())
            scratch.add((0x80 or (n and 0x3F)).toByte())
        }
        else -> {
            scratch.add((0xF0 or (n shr 18)).toByte())
            scratch.add((0x80 or ((n shr 12) and 0x3F)).toByte())
            scratch.add((0x80 or ((n shr 6) and 0x3F)).toByte())
            scratch.add((0x80 or (n and 0x3F)).toByte())
        }
    }
}

/**
 * Parses a JSON escape sequence and discards the value. Assumes the previous
 * byte read was a backslash.
 */
internal fun ignoreEscape(read: Read): SerdeResult<Unit> {
    val ch = nextOrEof(read).getOrElse { return SerdeResult.failure(it) }
    return when (ch) {
        '"'.code.toByte(), '\\'.code.toByte(), '/'.code.toByte(), 'b'.code.toByte() -> SerdeResult.success(Unit)
        'f'.code.toByte(), 'n'.code.toByte(), 'r'.code.toByte(), 't'.code.toByte() -> SerdeResult.success(Unit)
        'u'.code.toByte() -> read.decodeHexEscape().map {}
        else -> error(read, ErrorCode.InvalidEscape)
    }
}

internal fun decodeHexValSlow(value: Int): Int? =
    when {
        value in '0'.code..'9'.code -> value - '0'.code
        value in 'a'.code..'f'.code -> value - 'a'.code + 10
        value in 'A'.code..'F'.code -> value - 'A'.code + 10
        else -> null
    }

private fun buildHexTable(shift: Int): ShortArray =
    ShortArray(256) { ch ->
        val decoded = decodeHexValSlow(ch)
        if (decoded != null) {
            (decoded shl shift).toShort()
        } else {
            (-1).toShort()
        }
    }

private val HEX0: ShortArray = buildHexTable(0)
private val HEX1: ShortArray = buildHexTable(4)

internal fun decodeFourHexDigits(a: Byte, b: Byte, c: Byte, d: Byte): UShort? {
    val aVal = HEX1[a.toInt() and 0xFF].toInt()
    val bVal = HEX0[b.toInt() and 0xFF].toInt()
    val cVal = HEX1[c.toInt() and 0xFF].toInt()
    val dVal = HEX0[d.toInt() and 0xFF].toInt()

    val codepoint = ((aVal or bVal) shl 8) or cVal or dVal
    return if (codepoint >= 0) {
        codepoint.toUShort()
    } else {
        null
    }
}
