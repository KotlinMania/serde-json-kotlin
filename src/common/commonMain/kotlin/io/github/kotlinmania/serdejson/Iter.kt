// port-lint: source iter.rs
package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serdejson.io.IoResult

/**
 * An iterator that tracks line and column positions while iterating over bytes.
 *
 * Characters in the first line of the input (before the first newline character)
 * are in line 1. The first character in the input and any characters immediately
 * following a newline character are in column 1. The column is 0 immediately after
 * a newline character has been read.
 */
internal class LineColIterator<I : Iterator<IoResult<Byte>>>(
    private val iter: I,
) : Iterator<IoResult<Byte>> {
    /** Index of the current line. */
    private var line: Int = 1

    /** Index of the current column. */
    private var col: Int = 0

    /** Byte offset of the start of the current line. */
    private var startOfLine: Int = 0

    fun line(): Int = line

    fun col(): Int = col

    fun byteOffset(): Int = startOfLine + col

    override fun hasNext(): Boolean = iter.hasNext()

    override fun next(): IoResult<Byte> {
        if (!hasNext()) throw NoSuchElementException()
        val item = iter.next()
        if (item is IoResult.Ok) {
            if (item.value == '\n'.code.toByte()) {
                startOfLine += col + 1
                line += 1
                col = 0
            } else {
                col += 1
            }
        }
        return item
    }

    fun nextOrNull(): IoResult<Byte>? =
        if (iter.hasNext()) {
            next()
        } else {
            null
        }

    companion object {
        fun <I : Iterator<IoResult<Byte>>> new(iter: I): LineColIterator<I> = LineColIterator(iter)
    }
}
