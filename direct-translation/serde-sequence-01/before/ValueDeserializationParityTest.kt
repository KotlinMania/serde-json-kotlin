package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.U64Deserialize
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ValueDeserializationParityTest {
    private fun sample(): JsonMap =
        JsonMap.from(
            listOf(
                "null" to Value.Null,
                "boolean" to Value.Bool(true),
                "signed" to Value.Number(JsonNumber.fromI64(-7)),
                "unsigned" to Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)),
                "float" to Value.Number(assertNotNull(JsonNumber.fromF64(1.5))),
                "string" to Value.Str("text"),
                "array" to Value.Array(listOf(Value.Null, Value.Bool(false))),
                "object" to Value.Object(JsonMap.from(listOf("nested" to Value.Str("value")))),
            ),
        )

    @Test
    fun mapDeserializationRetainsEveryValueKind() {
        val expected = sample()
        assertEquals(expected, JsonMap.deserialize(ValueDeserializer(Value.Object(expected))).getOrThrow())
        assertEquals(Value.Object(expected), ValueDeserialize.deserialize(ValueDeserializer(Value.Object(expected))).getOrThrow())
        assertEquals(expected, JsonMap.deserialize(JsonDeserializer(JsonParser(toStr(Value.Object(expected)).getOrThrow()))).getOrThrow())
    }

    @Test
    fun mapKeyAndValueFailuresRetainTheirIdentity() {
        for (failKey in listOf(true, false)) {
            val error = SerdeError.custom("nested map failure")
            val access =
                object : MapAccess {
                    override fun <K> nextKeySeed(seed: DeserializeSeed<K>): SerdeResult<K?> =
                        if (failKey) SerdeResult.failure(error) else seed.deserialize(ValueDeserializer(Value.Str("key"))).map { it }

                    override fun <V> nextValueSeed(seed: DeserializeSeed<V>): SerdeResult<V> = SerdeResult.failure(error)
                }
            val deserializer =
                object : Deserializer by ValueDeserializer(Value.Null) {
                    override fun <V> deserializeMap(visitor: Visitor<V>): SerdeResult<V> = visitor.visitMap(access)
                }
            assertSame(error, JsonMap.deserialize(deserializer).exceptionOrNull())
        }
    }

    @Test
    fun integerDecodingRejectsFloatingPointValuesWithoutTruncation() {
        for (number in listOf(1.0, 1.5, -1.5)) {
            val value = Value.Number(assertNotNull(JsonNumber.fromF64(number)))
            assertTrue(value.deserialize(I64Deserialize).isFailure)
            assertTrue(value.deserialize(U64Deserialize).isFailure)
        }
    }

    @Test
    fun integerDecodingChecksBoundsAndPreservesValidValues() {
        assertTrue(Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)).deserialize(I64Deserialize).isFailure)
        assertTrue(Value.Number(JsonNumber.fromI64(-1)).deserialize(U64Deserialize).isFailure)
        assertEquals(Long.MIN_VALUE, Value.Number(JsonNumber.fromI64(Long.MIN_VALUE)).deserialize(I64Deserialize).getOrThrow())
        assertEquals(Long.MAX_VALUE, Value.Number(JsonNumber.fromI64(Long.MAX_VALUE)).deserialize(I64Deserialize).getOrThrow())
        assertEquals(ULong.MAX_VALUE, Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)).deserialize(U64Deserialize).getOrThrow())
    }

    @Test
    fun numericHintsPreserveTheOriginalNumberKind() {
        val visitor =
            object : Visitor<String> {
                override fun expecting(): String = "a number"

                override fun visitI64(v: Long): SerdeResult<String> = SerdeResult.success("signed:$v")

                override fun visitU64(v: ULong): SerdeResult<String> = SerdeResult.success("unsigned:$v")

                override fun visitF64(v: Double): SerdeResult<String> = SerdeResult.success("float:$v")
            }
        val cases =
            listOf(
                Value.Number(JsonNumber.fromI64(-1)) to "signed:-1",
                Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)) to "unsigned:18446744073709551615",
                Value.Number(assertNotNull(JsonNumber.fromF64(1.5))) to "float:1.5",
            )
        for ((value, expected) in cases) {
            val deserializer = ValueDeserializer(value)
            assertEquals(expected, deserializer.deserializeI64(visitor).getOrThrow())
            assertEquals(expected, deserializer.deserializeU64(visitor).getOrThrow())
            assertEquals(expected, deserializer.deserializeF64(visitor).getOrThrow())
            assertEquals(expected, deserializer.deserializeI128(visitor).getOrThrow())
            assertEquals(expected, deserializer.deserializeU128(visitor).getOrThrow())
        }
    }

    @Test
    fun nonfiniteAndOptionalValuesDecodeThroughTheVisitor() {
        for (number in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val deserializer =
                object : Deserializer by ValueDeserializer(Value.Null) {
                    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitF64(number)
                }
            assertEquals(Value.Null, ValueDeserialize.deserialize(deserializer).getOrThrow())
        }
        val optional =
            object : Deserializer by ValueDeserializer(Value.Null) {
                override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitSome(ValueDeserializer(Value.Str("present")))
            }
        assertEquals(Value.Str("present"), ValueDeserialize.deserialize(optional).getOrThrow())
    }

    @Test
    fun wideIntegerValuesRespectTheNumberRange() {
        fun signed(value: String): SerdeResult<Value> {
            val deserializer =
                object : Deserializer by ValueDeserializer(Value.Null) {
                    override fun <V> deserializeAny(visitor: Visitor<V>): SerdeResult<V> = visitor.visitI128(value)
                }
            return ValueDeserialize.deserialize(deserializer)
        }
        assertEquals(Value.Number(JsonNumber.fromI64(Long.MIN_VALUE)), signed(Long.MIN_VALUE.toString()).getOrThrow())
        assertEquals(Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)), signed(ULong.MAX_VALUE.toString()).getOrThrow())
        assertTrue(signed("18446744073709551616").isFailure)
        assertTrue(signed("-9223372036854775809").isFailure)
    }

    @Test
    fun partialSequenceConsumptionFailsAndReportsExactSizeHints() {
        val visitor =
            object : Visitor<Unit> {
                override fun expecting(): String = "one element"

                override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Unit> {
                    assertEquals(2, access.sizeHint())
                    assertEquals(1L, access.nextElement(I64Deserialize).getOrThrow())
                    assertEquals(1, access.sizeHint())
                    return SerdeResult.success(Unit)
                }
            }
        val result = ValueDeserializer(Value.Array(listOf(Value.Number(JsonNumber.fromI64(1)), Value.Null))).deserializeSeq(visitor)
        assertEquals("invalid length 2, expected fewer elements in array", assertNotNull(result.exceptionOrNull()).message)
    }

    @Test
    fun partialMapConsumptionFailsAndReportsExactSizeHints() {
        val visitor =
            object : Visitor<Unit> {
                override fun expecting(): String = "one entry"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<Unit> {
                    assertEquals(2, access.sizeHint())
                    assertNotNull(access.nextEntry(StringDeserialize, I64Deserialize).getOrThrow())
                    assertEquals(1, access.sizeHint())
                    return SerdeResult.success(Unit)
                }
            }
        val map = JsonMap.from(listOf("a" to Value.Number(JsonNumber.fromI64(1)), "b" to Value.Number(JsonNumber.fromI64(2))))
        val result = ValueDeserializer(Value.Object(map)).deserializeMap(visitor)
        assertEquals("invalid length 2, expected fewer elements in map", assertNotNull(result.exceptionOrNull()).message)
    }

    @Test
    fun containerVisitorFailureTakesPrecedenceOverRemainingElements() {
        val error = SerdeError.custom("visitor failed")
        val visitor =
            object : Visitor<Unit> {
                override fun expecting(): String = "a container"

                override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Unit> = SerdeResult.failure(error)

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<Unit> = SerdeResult.failure(error)
            }
        assertSame(error, ValueDeserializer(Value.Array(listOf(Value.Null))).deserializeSeq(visitor).exceptionOrNull())
        assertSame(error, ValueDeserializer(Value.Object(sample())).deserializeMap(visitor).exceptionOrNull())
    }
}
