package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BytesDeserialize
import io.github.kotlinmania.serdecore.de.F32Deserialize
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.U64Deserialize
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.io.SliceIoReader
import io.github.kotlinmania.serdejson.io.VecIoWrite
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnicodeAndParityTest {
    @Test
    fun supplementaryEmojiStringSerializesAndParsesWithoutQuestionMarks() {
        val sample = "tensor😀"
        val value = Value.Str(sample)

        val encoded = toStr(value).getOrThrow()
        assertEquals("\"tensor😀\"", encoded)
        assertFalse(encoded.contains("??"))
        assertEquals("\"tensor😀\"", valueToStr(value))
        assertEquals("\"tensor😀\"", value.toString())

        val utf8Bytes = toVec(value).getOrThrow()
        val expectedBytes =
            byteArrayOf(
                0x22,
                0x74,
                0x65,
                0x6E,
                0x73,
                0x6F,
                0x72,
                0xF0.toByte(),
                0x9F.toByte(),
                0x98.toByte(),
                0x80.toByte(),
                0x22,
            )
        assertContentEquals(expectedBytes, utf8Bytes)

        assertEquals(value, fromStr(encoded).getOrThrow())
        assertEquals(value, fromSlice(utf8Bytes).getOrThrow())
        assertEquals(value, fromStr("\"tensor\\uD83D\\uDE00\"").getOrThrow())
        assertEquals(value, fromSlice("\"tensor\\uD83D\\uDE00\"".encodeToByteArray()).getOrThrow())

        val raw = toRawValue(value).getOrThrow()
        assertEquals("\"tensor😀\"", raw.get())
        assertEquals(value, ValueDeserialize.deserialize(raw.intoDeserializer()).getOrThrow())
    }

    @Test
    fun multiScriptAndEmojiPayloadsRoundTripAcrossReadersAndWriters() {
        val text = "line1\n\"tensor😀\" 🦀🧪𐍈 カフェ☕\t✓"
        val map =
            JsonMap.from(
                listOf(
                    "label" to Value.Str("tensor😀"),
                    "mixed" to Value.Str(text),
                ),
            )
        val root = Value.Object(map)

        val sink = VecIoWrite()
        assertTrue(toWriter(sink, root).isSuccess)
        val compactBytes = sink.bytes
        assertFalse(compactBytes.decodeToString().contains("??"))

        assertEquals(root, fromSlice(compactBytes).getOrThrow())
        assertEquals(root, fromReader(SliceIoReader(compactBytes), ValueDeserialize).getOrThrow())

        // Direct SliceRead, StrRead, and IoRead string decoding after opening quote
        val quotedTextBytes = toVec(Value.Str(text)).getOrThrow()
        val sliceRead = SliceRead.new(quotedTextBytes)
        assertEquals('"'.code.toByte(), sliceRead.next())
        assertEquals(text, sliceRead.parseStr())

        val strRead = StrRead.new(quotedTextBytes.decodeToString())
        assertEquals('"'.code, strRead.next())
        assertEquals(text, strRead.parseStr())

        val ioRead = IoRead.new(SliceIoReader(quotedTextBytes))
        assertEquals('"'.code.toByte(), ioRead.next().getOrThrow())
        val scratch = ArrayList<Byte>()
        assertEquals(text, ioRead.parseStr(scratch).getOrThrow().deref())
    }

    @Test
    fun loneSurrogatesAndInvalidUtf8AreRejectedWhileRawBytesAllowWtf8() {
        val invalidEscapes =
            listOf(
                "\"\\uD83D\"",
                "\"\\uDC00\"",
                "\"\\uD83D\\u0041\"",
                "\"\\uD83D\\n\"",
                "\"\\uD83D\"",
                "\"\\u12G4\"",
                "\"\\u12\"",
            )
        for (input in invalidEscapes) {
            assertTrue(fromStr(input).isFailure, "Expected fromStr failure for $input")
            val sliceReader = SliceRead.new(input.encodeToByteArray())
            assertEquals('"'.code.toByte(), sliceReader.next())
            assertTrue(sliceReader.parseStrOutcome().isFailure, "Expected SliceRead failure for $input")
        }

        // Unescaped lone UTF-16 surrogates constructed without constant surrogate String literals
        val loneHighInput = charArrayOf('"', '\uD83D', '"').concatToString()
        val loneLowInput = charArrayOf('"', '\uDC00', '"').concatToString()
        assertTrue(fromStr(loneHighInput).isFailure)
        assertTrue(fromStr(loneLowInput).isFailure)

        // Invalid raw UTF-8 byte sequences in fromSlice
        val invalidRawByte = byteArrayOf(0x22, 0xFF.toByte(), 0x22)
        val utf8EncodedSurrogate = byteArrayOf(0x22, 0xED.toByte(), 0xA0.toByte(), 0x80.toByte(), 0x22)
        val sliceErr1 = fromSlice(invalidRawByte)
        val sliceErr2 = fromSlice(utf8EncodedSurrogate)
        assertTrue(sliceErr1.isFailure)
        assertTrue(sliceErr2.isFailure)
        assertTrue(assertNotNull(sliceErr1.error).message.contains("invalid unicode code point"))
        assertTrue(assertNotNull(sliceErr2.error).message.contains("invalid unicode code point"))

        // deserializeBytes allows WTF-8 lone surrogates in byte-string mode
        val wtf8Bytes = deserializeFromStr("\"\\uD800\"", BytesDeserialize).getOrThrow()
        assertContentEquals(
            byteArrayOf(0xED.toByte(), 0xA0.toByte(), 0x80.toByte()),
            wtf8Bytes,
        )
    }

    @Test
    fun tupleAndStructVariantsEmitBalancedBracesInCompactAndPrettyJson() {
        val nonEmptyTupleVariant =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serializer.serializeTupleVariant("Shape", 0u, "Pair", 2).flatMap { tv ->
                        tv
                            .serializeElement(Value.Number(JsonNumber.from(10)))
                            .flatMap { tv.serializeElement(Value.Str("tensor😀")) }
                            .flatMap { tv.end() }
                    }
            }
        val emptyTupleVariant =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serializer.serializeTupleVariant("Shape", 1u, "Empty", 0).flatMap { it.end() }
            }
        val structVariant =
            object : Serialize {
                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                    serializer.serializeStructVariant("Shape", 2u, "Named", 2).flatMap { sv ->
                        sv
                            .serializeField("id", Value.Number(JsonNumber.from(7)))
                            .flatMap { sv.serializeField("tag", Value.Str("tensor😀")) }
                            .flatMap { sv.end() }
                    }
            }

        assertEquals("""{"Pair":[10,"tensor😀"]}""", toStr(nonEmptyTupleVariant).getOrThrow())
        assertEquals("""{"Empty":[]}""", toStr(emptyTupleVariant).getOrThrow())
        assertEquals("""{"Named":{"id":7,"tag":"tensor😀"}}""", toStr(structVariant).getOrThrow())

        val prettyTuple = toStrPretty(nonEmptyTupleVariant).getOrThrow()
        assertEquals(
            "{\n  \"Pair\": [\n    10,\n    \"tensor😀\"\n  ]\n}",
            prettyTuple,
        )
        assertEquals(fromStr("""{"Pair":[10,"tensor😀"]}""").getOrThrow(), fromStr(prettyTuple).getOrThrow())

        val prettyStruct = toStrPretty(structVariant).getOrThrow()
        assertEquals(
            "{\n  \"Named\": {\n    \"id\": 7,\n    \"tag\": \"tensor😀\"\n  }\n}",
            prettyStruct,
        )
        assertEquals(fromStr("""{"Named":{"id":7,"tag":"tensor😀"}}""").getOrThrow(), fromStr(prettyStruct).getOrThrow())
    }

    @Test
    fun multiLevelPrettyFormattingAndSortAllObjectsMatchUpstream() {
        val innerObj =
            JsonMap.from(
                listOf(
                    "z" to Value.Str("tensor😀"),
                    "a" to Value.Array(emptyList()),
                ),
            )
        val arrItem =
            JsonMap.from(
                listOf(
                    "second" to Value.Bool(true),
                    "first" to Value.Object(JsonMap()),
                ),
            )
        val rootMap =
            JsonMap.from(
                listOf(
                    "omega" to Value.Array(listOf(Value.Object(arrItem))),
                    "alpha" to Value.Object(innerObj),
                ),
            )
        val root = Value.Object(rootMap)
        root.sortAllObjects()

        val expectedPretty =
            """
            {
              "alpha": {
                "a": [],
                "z": "tensor😀"
              },
              "omega": [
                {
                  "first": {},
                  "second": true
                }
              ]
            }
            """.trimIndent()
        assertEquals(expectedPretty, root.toPrettyString())
        assertEquals(expectedPretty, toStrPretty(root).getOrThrow())
        assertEquals(expectedPretty, toVecPretty(root).getOrThrow().decodeToString())
    }

    @Test
    fun jsonNumberRawValueRecursionLimitAndLexicalFloatsPreserveParity() {
        // JsonNumber Deserialize + Deserializer + 128-bit & small integer helpers
        val parsedNum = deserializeFromStr("18446744073709551615", JsonNumber).getOrThrow()
        assertEquals(ULong.MAX_VALUE, parsedNum.asU64())
        assertEquals("18446744073709551615", parsedNum.asU128())
        assertEquals("18446744073709551615", parsedNum.asI128())
        assertEquals(ULong.MAX_VALUE, U64Deserialize.deserialize(parsedNum.intoDeserializer()).getOrThrow())

        val negNum = assertNotNull(JsonNumber.fromI128(Long.MIN_VALUE.toString()))
        assertEquals(Long.MIN_VALUE, negNum.asI64())
        assertNull(negNum.asU128())
        assertEquals(Long.MIN_VALUE, I64Deserialize.deserialize(negNum).getOrThrow())
        assertNull(JsonNumber.fromI128("18446744073709551616"))
        assertNull(JsonNumber.fromU128("18446744073709551616"))

        assertEquals(7L, JsonNumber.from(7.toByte()).asI64())
        assertEquals(-12L, JsonNumber.from((-12).toShort()).asI64())
        assertEquals(250UL, JsonNumber.from(250.toUByte()).asU64())
        assertEquals(65000UL, JsonNumber.from(65000.toUShort()).asU64())
        assertEquals(4000000000UL, JsonNumber.from(4000000000u).asU64())

        // RawValue companion & methods
        assertEquals("null", RawValue.default().get())
        val ownedRaw = RawValue.fromString(" [ 1, \"tensor😀\" ] ").getOrThrow()
        assertEquals("[ 1, \"tensor😀\" ]", ownedRaw.clone().intoOwned())
        val borrowedAccess = BorrowedRawDeserializer("[1]")
        assertEquals(RawValue.TOKEN, borrowedAccess.nextKey(StringDeserialize).getOrThrow())
        assertEquals("[1]", borrowedAccess.nextValue(StringDeserialize).getOrThrow())

        // Recursion limit default vs disableRecursionLimit
        var deepJson = "0"
        repeat(140) {
            deepJson = "[$deepJson]"
        }
        assertTrue(fromStr(deepJson).isFailure)
        val unboundedDe = JsonDeserializer.fromStr(deepJson)
        unboundedDe.disableRecursionLimit()
        val deepParsed = ValueDeserialize.deserialize(unboundedDe).getOrThrow()
        assertIs<Value.Array>(deepParsed)

        // Exact IEEE-754 float parsing via lexical (concise, truncated long decimals, f32)
        assertEquals(Double.MAX_VALUE, fromStr("1.7976931348623157e308").getOrThrow().asF64())
        assertEquals(Double.MIN_VALUE, fromStr("5e-324").getOrThrow().asF64())
        assertEquals(
            0.12345678901234568,
            fromStr("0.12345678901234567890123456789").getOrThrow().asF64(),
        )
        assertEquals(
            1.2345679e24,
            fromStr("1234567890123456789012345.0").getOrThrow().asF64()!!,
            1e9,
        )
        val singlePrecision = deserializeFromStr("1.00000017881393421514957253748434595763683319091796875001", F32Deserialize).getOrThrow()
        assertEquals(Float.fromBits(0x3F800002), singlePrecision)
    }
}
