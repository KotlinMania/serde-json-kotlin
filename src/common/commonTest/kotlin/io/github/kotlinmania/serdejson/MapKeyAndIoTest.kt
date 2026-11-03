package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.io.IoError
import io.github.kotlinmania.serdejson.io.IoErrorKind
import io.github.kotlinmania.serdejson.io.IoResult
import io.github.kotlinmania.serdejson.io.IoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapKeyAndIoTest {
    private enum class Kind {
        Text,
        Boolean,
        Signed,
        Unsigned,
        WideSigned,
        WideUnsigned,
        Float,
        Double,
        Character,
        Variant,
        Newtype,
        Some,
        Bytes,
        None,
        Unit,
        Sequence,
        Map,
    }

    private class Key(
        val kind: Kind,
        val text: String = "",
    ) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            when (kind) {
                Kind.Text -> serializer.serializeStr(text)
                Kind.Boolean -> serializer.serializeBool(text.toBoolean())
                Kind.Signed -> serializer.serializeI64(text.toLong())
                Kind.Unsigned -> serializer.serializeU64(text.toULong())
                Kind.WideSigned -> serializer.serializeI128(text)
                Kind.WideUnsigned -> serializer.serializeU128(text)
                Kind.Float -> serializer.serializeF32(text.toFloat())
                Kind.Double -> serializer.serializeF64(text.toDouble())
                Kind.Character -> serializer.serializeChar(text.single())
                Kind.Variant -> serializer.serializeUnitVariant("Enum", 0u, text)
                Kind.Newtype -> serializer.serializeNewtypeStruct("Key", Key(Kind.Text, text))
                Kind.Some -> serializer.serializeSome(Key(Kind.Text, text))
                Kind.Bytes -> serializer.serializeBytes(byteArrayOf(1))
                Kind.None -> serializer.serializeNone()
                Kind.Unit -> serializer.serializeUnit()
                Kind.Sequence -> serializer.serializeSeq(0).flatMap { it.end() }
                Kind.Map -> serializer.serializeMap(0).flatMap { it.end() }
            }
    }

    private class Entry(
        val key: Serialize,
        val split: Boolean,
    ) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeMap(1).flatMap { map ->
                val result =
                    if (split) {
                        map.serializeKey(key).flatMap { map.serializeValue(Value.Null) }
                    } else {
                        map.serializeEntry(key, Value.Null)
                    }
                result.flatMap { map.end() }
            }
    }

    @Test
    fun scalarKeysAreQuotedInEntryAndSplitPaths() {
        val cases =
            listOf(
                Key(Kind.Text, "key") to "key",
                Key(Kind.Boolean, "true") to "true",
                Key(Kind.Signed, "-12") to "-12",
                Key(Kind.Unsigned, "18446744073709551615") to "18446744073709551615",
                Key(Kind.Float, "1.5") to "1.5",
                Key(Kind.Double, "1.5") to "1.5",
                Key(Kind.Character, "x") to "x",
                Key(Kind.Variant, "Forward") to "Forward",
                Key(Kind.Newtype, "wrapped") to "wrapped",
                Key(Kind.Some, "present") to "present",
                Key(Kind.WideSigned, "-170141183460469231731687303715884105728") to "-170141183460469231731687303715884105728",
                Key(Kind.WideUnsigned, "340282366920938463463374607431768211455") to "340282366920938463463374607431768211455",
            )
        for ((key, expected) in cases) {
            for (split in listOf(false, true)) {
                assertEquals("{\"$expected\":null}", toStr(Entry(key, split)).getOrThrow())
            }
        }
    }

    @Test
    fun stringKeysUseTheNormalEscapingRules() {
        assertEquals("{\"a\\\"b\\nc\":null}", toStr(Entry(Key(Kind.Text, "a\"b\nc"), false)).getOrThrow())
    }

    @Test
    fun unsupportedKeysFailInsteadOfWritingInvalidJson() {
        for (kind in listOf(Kind.Bytes, Kind.None, Kind.Unit, Kind.Sequence, Kind.Map)) {
            for (split in listOf(false, true)) {
                val result = toStr(Entry(Key(kind), split))
                assertTrue(result.isFailure, kind.name)
                assertEquals("key must be a string", result.error?.message)
            }
        }
    }

    @Test
    fun nonfiniteMapKeysFailWhileOrdinaryValuesRemainNull() {
        for (kind in listOf(Kind.Float, Kind.Double)) {
            for (number in listOf("NaN", "Infinity", "-Infinity")) {
                val result = toStr(Entry(Key(kind, number), false))
                assertTrue(result.isFailure)
                assertEquals("float key must be finite (got NaN or +/-inf)", result.error?.message)
            }
        }
    }

    @Test
    fun wideIntegerKeysRejectInvalidOrOutOfRangeRepresentations() {
        for (key in listOf(
            Key(Kind.WideSigned, "170141183460469231731687303715884105728"),
            Key(Kind.WideUnsigned, "340282366920938463463374607431768211456"),
            Key(Kind.WideUnsigned, "-1"),
            Key(Kind.WideSigned, "not a number"),
        )) {
            assertTrue(toStr(Entry(key, false)).isFailure)
        }
    }

    @Test
    fun rawValuesPreserveFormattingAndRejectInvalidInput() {
        val text = "{ \"value\" : [ 1, 2 ] }"
        assertEquals(text, toStr(rawValueFromString(text).getOrThrow()).getOrThrow())
        assertTrue(rawValueFromString("null false").isFailure)
        assertTrue(toStr(RawValue("not json")).isFailure)
        assertTrue(toValue(RawValue("not json")).isFailure)
    }

    @Test
    fun bytesAreUnsignedAndFloatPrecisionRemainsSingle() {
        val bytes =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serializer.serializeBytes(byteArrayOf(0, 127, -128, -1))
            }
        assertEquals("[0,127,128,255]", toStr(bytes).getOrThrow())
        assertEquals("0.1", toStr(Key(Kind.Float, "0.1")).getOrThrow())
    }

    private class FailingWriter(
        val failAt: Int?,
    ) : IoWrite {
        var calls = 0

        override fun write(buf: ByteArray): IoResult<Unit> {
            calls++
            return if (calls == failAt) IoResult.err(IoError(IoErrorKind.Other, "writer failed")) else IoResult.ok(Unit)
        }
    }

    @Test
    fun returnedWriterErrorsStopEverySerializationPath() {
        val values =
            listOf(
                Value.Null,
                Value.Str("a\"b\nc"),
                Value.Array(listOf(Value.Str("element"), Value.Null)),
                Entry(Key(Kind.Text, "field"), false),
                RawValue("{ \"value\" : [ 1, 2 ] }"),
            )
        for (value in values) {
            val successful = FailingWriter(null)
            assertTrue(value.serialize(JsonSerializer(successful)).isSuccess)
            for (position in 1..successful.calls) {
                val writer = FailingWriter(position)
                val result = value.serialize(JsonSerializer(writer))
                assertTrue(result.isFailure, "write $position")
                assertEquals("writer failed", result.exceptionOrNull()?.message)
                assertEquals(position, writer.calls)
            }
        }
    }

    @Test
    fun returnedClosingDelimiterErrorIsNotLost() {
        val writer =
            IoWrite { bytes ->
                if (bytes.contentEquals(byteArrayOf(']'.code.toByte()))) {
                    IoResult.err(IoError(IoErrorKind.Other, "closing bracket failed"))
                } else {
                    IoResult.ok(Unit)
                }
            }
        val result = JsonSerializer(writer).serializeBytes(byteArrayOf(1, 2))
        assertTrue(result.isFailure)
        assertEquals("closing bracket failed", result.exceptionOrNull()?.message)
    }
}
