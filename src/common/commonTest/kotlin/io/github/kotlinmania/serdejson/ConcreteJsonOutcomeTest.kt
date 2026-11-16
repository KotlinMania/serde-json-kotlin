package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeDecoding
import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ConcreteJsonOutcomeTest {
    @Test
    fun parsedValueHasConcreteTypeAndOriginalIdentity() {
        val parsed: JsonValueOutcome = fromStr("\"value\"")
        val value: Value = parsed.getOrThrow()
        assertEquals("value", assertIs<Value.Str>(value).value)
        assertSame(value, parsed.value)
        assertNull(parsed.error)
    }

    @Test
    fun concreteFailureRetainsOriginalDiagnostic() {
        val original = SerdeError.custom("original diagnostic")
        val outcome = JsonValueOutcome.failure(original)
        assertNull(outcome.value)
        assertSame(original, outcome.error)
        assertSame(original, outcome.toSerdeResult().exceptionOrNull())
    }

    @Test
    fun serializationHasConcreteTextAndBytes() {
        val value = Value.Str("value")
        val text: String = toStr(value).getOrThrow()
        val bytes: ByteArray = toVec(value).getOrThrow()
        assertEquals("\"value\"", text)
        assertEquals(text, bytes.decodeToString())
    }

    @Test
    fun nullIsAValueRatherThanAnAbsentOutcome() {
        val result = fromStr("null")
        assertSame(Value.Null, result.value)
        assertTrue(result.isSuccess)
        assertNull(result.error)
    }

    @Test
    fun trailingInputFailsForValueAndTypedDecoders() {
        assertTrue(fromStr("null false").isFailure)
        assertTrue(fromStr("\"first\" \"second\"", StringDeserialize).isFailure)
        assertTrue(fromStr("[]{}").isFailure)
    }

    @Test
    fun trailingWhitespaceRemainsValid() {
        assertSame(Value.Null, fromStr("null \t\r\n").getOrThrow())
        assertEquals("value", fromStr("\"value\" \t\r\n", StringDeserialize).getOrThrow())
    }

    @Test
    fun nonfiniteFloatsSerializeAsNull() {
        for (number in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            val value =
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeF64(number)
                }
            assertEquals("null", toStr(value).getOrThrow())
        }
        for (number in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            val value =
                object : Serialize {
                    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeF32(number)
                }
            assertEquals("null", toStr(value).getOrThrow())
        }
    }

    @Test
    fun formatInputSupportsConcretePrimitiveOperations() {
        assertEquals("value", SerdeDecoding.text(JsonDecoding.input("\"value\"")).getOrThrow())
        assertEquals(0L, SerdeDecoding.signed(JsonDecoding.input("0")).getOrThrow())
        assertFalse(SerdeDecoding.boolean(JsonDecoding.input("false")).getOrThrow())
        assertTrue(SerdeDecoding.unit(JsonDecoding.input("null")).isSuccess)
        assertTrue(SerdeDecoding.signed(JsonDecoding.input("\"invalid\"")).isFailure)
    }

    @Test
    fun sequenceVisitorFailurePrecedesTerminatorValidation() {
        val original = SerdeError.custom("sequence visitor failed")
        val visitor = failedVisitor(original)
        assertSame(original, JsonDeserializer(JsonParser("[1]")).deserializeAny(visitor).exceptionOrNull())
        assertSame(original, JsonDeserializer(JsonParser("[1]")).deserializeSeq(visitor).exceptionOrNull())
        assertSame(original, JsonDeserializer(JsonParser("[1]")).deserializeStruct("State", emptyList(), visitor).exceptionOrNull())
    }

    @Test
    fun mapAndEnumVisitorFailuresPrecedeTerminatorValidation() {
        val original = SerdeError.custom("map visitor failed")
        val visitor = failedVisitor(original)
        assertSame(original, JsonDeserializer(JsonParser("{\"field\":1}")).deserializeAny(visitor).exceptionOrNull())
        assertSame(original, JsonDeserializer(JsonParser("{\"field\":1}")).deserializeMap(visitor).exceptionOrNull())
        assertSame(original, JsonDeserializer(JsonParser("{\"field\":1}")).deserializeStruct("State", listOf("field"), visitor).exceptionOrNull())
        assertSame(original, JsonDeserializer(JsonParser("{\"Forward\":1}")).deserializeEnum("Direction", listOf("Forward"), visitor).exceptionOrNull())
    }

    private fun failedVisitor(error: SerdeError): Visitor<Unit> =
        object : Visitor<Unit> {
            override fun expecting(): String = "a value"

            override fun <A> visitSeq(access: A): SerdeResult<Unit>
                where A : SeqAccess = SerdeResult.failure(error)

            override fun <A> visitMap(access: A): SerdeResult<Unit>
                where A : MapAccess = SerdeResult.failure(error)

            override fun <A> visitEnum(access: A): SerdeResult<Unit>
                where A : EnumAccess = SerdeResult.failure(error)
        }
}
