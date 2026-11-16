package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BooleanDeserialize
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.F64Deserialize
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.U64Deserialize
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FromValueParityTest {
    @Test
    fun stringDecodingRetainsTheSuppliedString() {
        val text = buildString { repeat(32) { append("value") } }
        assertSame(text, fromValue(Value.Str(text), StringDeserialize).getOrThrow())
    }

    @Test
    fun floatingPointDecodingRetainsExactBits() {
        for (number in listOf(-0.0, Double.MIN_VALUE, Double.MAX_VALUE, 1.2345678901234567)) {
            val value = Value.Number(assertNotNull(JsonNumber.fromF64(number)))
            val decoded = fromValue(value, F64Deserialize).getOrThrow()
            assertEquals(number.toBits(), decoded.toBits())
        }
    }

    @Test
    fun existingNestedValuesDoNotAcquireATextParserDepthLimit() {
        var value: Value = Value.Bool(true)
        repeat(160) { value = Value.Array(listOf(value)) }
        var decoded = fromValue(value, ValueDeserialize).getOrThrow()
        repeat(160) {
            val array = assertIs<Value.Array>(decoded)
            assertEquals(1, array.value.size)
            decoded = array.value.single()
        }
        assertEquals(Value.Bool(true), decoded)
    }

    @Test
    fun sequenceDecodingReportsRemainingElementsAndExactSizeHints() {
        val decoder =
            decode<Unit> { deserializer ->
                deserializer.deserializeSeq(
                    object : Visitor<Unit> {
                        override fun expecting(): String = "one element"

                        override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Unit> {
                            assertEquals(2, access.sizeHint())
                            assertEquals(1L, access.nextElement(I64Deserialize).getOrThrow())
                            assertEquals(1, access.sizeHint())
                            return SerdeResult.success(Unit)
                        }
                    },
                )
            }
        val value = Value.Array(listOf(Value.Number(JsonNumber.fromI64(1)), Value.Null))
        val error = assertNotNull(fromValue(value, decoder).exceptionOrNull())
        assertEquals("invalid length 2, expected fewer elements in array", error.message)
    }

    @Test
    fun mapDecodingReportsRemainingEntriesAndExactSizeHints() {
        val decoder =
            decode<Unit> { deserializer ->
                deserializer.deserializeMap(
                    object : Visitor<Unit> {
                        override fun expecting(): String = "one entry"

                        override fun <A : MapAccess> visitMap(access: A): SerdeResult<Unit> {
                            assertEquals(2, access.sizeHint())
                            assertNotNull(access.nextEntry(StringDeserialize, I64Deserialize).getOrThrow())
                            assertEquals(1, access.sizeHint())
                            return SerdeResult.success(Unit)
                        }
                    },
                )
            }
        val value = Value.Object(JsonMap.from(listOf("a" to Value.Number(JsonNumber.fromI64(1)), "b" to Value.Null)))
        val error = assertNotNull(fromValue(value, decoder).exceptionOrNull())
        assertEquals("invalid length 2, expected fewer elements in map", error.message)
    }

    @Test
    fun visitorFailurePreservesOriginalDiagnosticAndCause() {
        val source = SerdeError.custom("source")
        val error = SerdeError("visitor failed", source)
        val decoder =
            decode<Unit> { deserializer ->
                deserializer.deserializeSeq(
                    object : Visitor<Unit> {
                        override fun expecting(): String = "a sequence"

                        override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Unit> = SerdeResult.failure(error)
                    },
                )
            }
        val result = fromValue(Value.Array(listOf(Value.Null)), decoder)
        assertSame(error, result.exceptionOrNull())
        assertSame(source, result.exceptionOrNull()?.source)
    }

    @Test
    fun genericDecodingRetainsConcreteTypesAndRejectsMismatches() {
        val flag: Boolean = fromValue(Value.Bool(false), BooleanDeserialize).getOrThrow()
        val signed: Long = fromValue(Value.Number(JsonNumber.fromI64(Long.MIN_VALUE)), I64Deserialize).getOrThrow()
        val unsigned: ULong = fromValue(Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)), U64Deserialize).getOrThrow()
        assertEquals(false, flag)
        assertEquals(Long.MIN_VALUE, signed)
        assertEquals(ULong.MAX_VALUE, unsigned)
        assertTrue(fromValue(Value.Str("false"), BooleanDeserialize).isFailure)
        assertTrue(fromValue(Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)), I64Deserialize).isFailure)
    }

    private fun <T> decode(action: (Deserializer) -> SerdeResult<T>): Deserialize<T> =
        object : Deserialize<T> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<T> = action(deserializer)
        }
}
