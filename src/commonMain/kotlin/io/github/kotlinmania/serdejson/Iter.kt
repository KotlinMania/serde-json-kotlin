// port-lint: source iter.rs
package io.github.kotlinmania.serdejson

/**
 * An iterator that tracks line and column positions while iterating over bytes.
 *
 * Characters in the first line of the input (before the first newline character)
 * are in line 1. The first character in the input and any characters immediately
 * following a newline character are in column 1. The column is 0 immediately after
 * a newline character has been read.
 */
internal class LineColIterator<I>(
    private val iter: I,
) where I : Iterable<Int> {
    private val iterator = iter.iterator()

    /** Index of the current line. */
    private var line: Int = 1

    /** Index of the current column. */
    private var col: Int = 0

    /** Byte offset of the start of the current line. */
    private var startOfLine: Int = 0

    fun line(): Int = line

    fun col(): Int = col

    fun byteOffset(): Int = startOfLine + col

    /** Returns the next byte, or -1 if no more data. */
    fun next(): Int {
        if (!iterator.hasNext()) return -1
        val c = iterator.next()
        if (c == '\n'.code) {
            startOfLine += col + 1
            line++
            col = 0
        } else {
            col++
        }
        return c
    }

    fun hasNext(): Boolean = iterator.hasNext()
}
