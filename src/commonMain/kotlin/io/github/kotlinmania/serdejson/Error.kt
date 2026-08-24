// port-lint: source src/error.rs
package io.github.kotlinmania.serdejson

/**
 * Categorizes the cause of a [JsonError].
 */
enum class Category {
    /** The error was caused by a failure to read or write bytes on an I/O stream. */
    IO,

    /** The error was caused by input that is not syntactically valid JSON. */
    SYNTAX,

    /** The error was caused by input data that was semantically incorrect. */
    DATA,

    /** The error was caused by prematurely reaching the end of the input data. */
    EOF,
}

/**
 * The internal error code identifying the specific kind of JSON error.
 */
sealed class ErrorCode {
    /** Catchall for syntax error messages. */
    class Message(
        val msg: String,
    ) : ErrorCode()

    /** Some I/O error occurred while serializing or deserializing. */
    class Io(
        val message: String,
    ) : ErrorCode()

    /** EOF while parsing a list. */
    object EofWhileParsingList : ErrorCode()

    /** EOF while parsing an object. */
    object EofWhileParsingObject : ErrorCode()

    /** EOF while parsing a string. */
    object EofWhileParsingString : ErrorCode()

    /** EOF while parsing a JSON value. */
    object EofWhileParsingValue : ErrorCode()

    /** Expected this character to be a ':'. */
    object ExpectedColon : ErrorCode()

    /** Expected this character to be either a ',' or a ']'. */
    object ExpectedListCommaOrEnd : ErrorCode()

    /** Expected this character to be either a ',' or a '}'. */
    object ExpectedObjectCommaOrEnd : ErrorCode()

    /** Expected to parse either a true, false, or a null. */
    object ExpectedSomeIdent : ErrorCode()

    /** Expected this character to start a JSON value. */
    object ExpectedSomeValue : ErrorCode()

    /** Expected this character to be a double quote. */
    object ExpectedDoubleQuote : ErrorCode()

    /** Invalid hex escape code. */
    object InvalidEscape : ErrorCode()

    /** Invalid number. */
    object InvalidNumber : ErrorCode()

    /** Number is bigger than the maximum value of its type. */
    object NumberOutOfRange : ErrorCode()

    /** Invalid unicode code point. */
    object InvalidUnicodeCodePoint : ErrorCode()

    /** Control character found while parsing a string. */
    object ControlCharacterWhileParsingString : ErrorCode()

    /** Object key is not a string. */
    object KeyMustBeAString : ErrorCode()

    /** Contents of key were supposed to be a number. */
    object ExpectedNumericKey : ErrorCode()

    /** Object key is a non-finite float value. */
    object FloatKeyMustBeFinite : ErrorCode()

    /** Lone leading surrogate in hex escape. */
    object LoneLeadingSurrogateInHexEscape : ErrorCode()

    /** JSON has a comma after the last value in an array or map. */
    object TrailingComma : ErrorCode()

    /** JSON has non-whitespace trailing characters after the value. */
    object TrailingCharacters : ErrorCode()

    /** Unexpected end of hex escape. */
    object UnexpectedEndOfHexEscape : ErrorCode()

    /** Encountered nesting of JSON maps and arrays more than 128 layers deep. */
    object RecursionLimitExceeded : ErrorCode()

    override fun toString(): String =
        when (this) {
            is Message -> msg
            is Io -> message
            EofWhileParsingList -> "EOF while parsing a list"
            EofWhileParsingObject -> "EOF while parsing an object"
            EofWhileParsingString -> "EOF while parsing a string"
            EofWhileParsingValue -> "EOF while parsing a value"
            ExpectedColon -> "expected `:`"
            ExpectedListCommaOrEnd -> "expected `,` or `]`"
            ExpectedObjectCommaOrEnd -> "expected `,` or `}`"
            ExpectedSomeIdent -> "expected ident"
            ExpectedSomeValue -> "expected value"
            ExpectedDoubleQuote -> "expected `\"`"
            InvalidEscape -> "invalid escape"
            InvalidNumber -> "invalid number"
            NumberOutOfRange -> "number out of range"
            InvalidUnicodeCodePoint -> "invalid unicode code point"
            ControlCharacterWhileParsingString -> "control character (\\u0000-\\u001F) found while parsing a string"
            KeyMustBeAString -> "key must be a string"
            ExpectedNumericKey -> "invalid value: expected key to be a number in quotes"
            FloatKeyMustBeFinite -> "float key must be finite (got NaN or +/-inf)"
            LoneLeadingSurrogateInHexEscape -> "lone leading surrogate in hex escape"
            TrailingComma -> "trailing comma"
            TrailingCharacters -> "trailing characters"
            UnexpectedEndOfHexEscape -> "unexpected end of hex escape"
            RecursionLimitExceeded -> "recursion limit exceeded"
        }
}

/**
 * This type represents all possible errors that can occur when serializing or
 * deserializing JSON data.
 */
class JsonError(
    val code: ErrorCode,
    lineNumber: Int = 0,
    columnNumber: Int = 0,
) {
    private val errorLine: Int = lineNumber
    private val errorColumn: Int = columnNumber

    /** One-based line number at which the error was detected. */
    fun line(): Int = errorLine

    /** One-based column number at which the error was detected. */
    fun column(): Int = errorColumn

    /** Categorizes the cause of this error. */
    fun classify(): Category =
        when (code) {
            is ErrorCode.Message -> Category.DATA
            is ErrorCode.Io -> Category.IO
            ErrorCode.EofWhileParsingList,
            ErrorCode.EofWhileParsingObject,
            ErrorCode.EofWhileParsingString,
            ErrorCode.EofWhileParsingValue,
            -> Category.EOF
            ErrorCode.ExpectedColon,
            ErrorCode.ExpectedListCommaOrEnd,
            ErrorCode.ExpectedObjectCommaOrEnd,
            ErrorCode.ExpectedSomeIdent,
            ErrorCode.ExpectedSomeValue,
            ErrorCode.ExpectedDoubleQuote,
            ErrorCode.InvalidEscape,
            ErrorCode.InvalidNumber,
            ErrorCode.NumberOutOfRange,
            ErrorCode.InvalidUnicodeCodePoint,
            ErrorCode.ControlCharacterWhileParsingString,
            ErrorCode.KeyMustBeAString,
            ErrorCode.ExpectedNumericKey,
            ErrorCode.FloatKeyMustBeFinite,
            ErrorCode.LoneLeadingSurrogateInHexEscape,
            ErrorCode.TrailingComma,
            ErrorCode.TrailingCharacters,
            ErrorCode.UnexpectedEndOfHexEscape,
            ErrorCode.RecursionLimitExceeded,
            -> Category.SYNTAX
        }

    /** Returns true if this error was caused by a failure to read or write bytes on an I/O stream. */
    fun isIo(): Boolean = classify() == Category.IO

    /** Returns true if this error was caused by input that was not syntactically valid JSON. */
    fun isSyntax(): Boolean = classify() == Category.SYNTAX

    /** Returns true if this error was caused by input data that was semantically incorrect. */
    fun isData(): Boolean = classify() == Category.DATA

    /** Returns true if this error was caused by prematurely reaching the end of the input data. */
    fun isEof(): Boolean = classify() == Category.EOF

    override fun toString(): String =
        if (line() == 0) {
            code.toString()
        } else {
            "$code at line ${line()} column ${column()}"
        }

    override fun equals(other: Any?): Boolean =
        other is JsonError && other.code == code && other.line() == line() && other.column() == column()

    override fun hashCode(): Int = code.hashCode() * 31 + line() * 7 + column()

    companion object {
        /** Creates a syntax error at the given position. */
        fun syntax(code: ErrorCode, line: Int, column: Int): JsonError = JsonError(code, line, column)

        /** Creates an I/O error. */
        fun io(message: String): JsonError = JsonError(ErrorCode.Io(message), 0, 0)

        /** Creates a custom error from a message, parsing line/column if present. */
        fun custom(msg: String): JsonError {
            val (line, column, truncated) = parseLineCol(msg)
            return JsonError(ErrorCode.Message(truncated), line, column)
        }

        /** Fix the position of this error if it doesn't already have one. */
        fun fixPosition(error: JsonError, f: (ErrorCode) -> JsonError): JsonError =
            if (error.line() == 0) {
                f(error.code)
            } else {
                error
            }

        private fun parseLineCol(msg: String): Triple<Int, Int, String> {
            val suffixIndex = msg.lastIndexOf(" at line ")
            if (suffixIndex < 0) return Triple(0, 0, msg)

            val startOfLine = suffixIndex + " at line ".length
            var endOfLine = startOfLine
            while (endOfLine < msg.length && msg[endOfLine].isDigit()) {
                endOfLine++
            }

            if (!msg.substring(endOfLine).startsWith(" column ")) return Triple(0, 0, msg)

            val startOfColumn = endOfLine + " column ".length
            var endOfColumn = startOfColumn
            while (endOfColumn < msg.length && msg[endOfColumn].isDigit()) {
                endOfColumn++
            }

            if (endOfColumn < msg.length) return Triple(0, 0, msg)

            val line = msg.substring(startOfLine, endOfLine).toIntOrNull() ?: return Triple(0, 0, msg)
            val column = msg.substring(startOfColumn, endOfColumn).toIntOrNull() ?: return Triple(0, 0, msg)

            return Triple(line, column, msg.substring(0, suffixIndex))
        }
    }
}

/**
 * Alias for a result with the error type [JsonError].
 */
typealias JsonResult<T> = kotlin.Result<T>
