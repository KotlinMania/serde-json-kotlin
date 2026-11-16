package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class ValueScalarParityTest {
    private class FloatValue(
        private val value: Float,
    ) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeF32(value)
    }

    private class DoubleValue(
        private val value: Double,
    ) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeF64(value)
    }

    private object BytesValue : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeBytes(byteArrayOf(0, 127, -128, -1))
    }

    @Test
    fun nonfiniteSinglePrecisionValuesBecomeNull() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(Value.Null, toValue(FloatValue(value)).getOrThrow())
        }
        assertEquals(Value.Number(assertNotNull(JsonNumber.fromF32(1.5f))), toValue(FloatValue(1.5f)).getOrThrow())
    }

    @Test
    fun nonfiniteDoublePrecisionValuesBecomeNull() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertEquals(Value.Null, toValue(DoubleValue(value)).getOrThrow())
        }
        assertEquals(Value.Number(assertNotNull(JsonNumber.fromF64(1.5))), toValue(DoubleValue(1.5)).getOrThrow())
    }

    @Test
    fun serializedBytesKeepTheirUnsignedValues() {
        val values = assertIs<Value.Array>(toValue(BytesValue).getOrThrow()).value
        assertEquals(listOf(0uL, 127uL, 128uL, 255uL).map { Value.Number(JsonNumber.fromU64(it)) }, values)
    }
}
