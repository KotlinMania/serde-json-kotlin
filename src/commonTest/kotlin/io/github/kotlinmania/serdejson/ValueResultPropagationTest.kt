package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ValueResultPropagationTest {
    private class FailedValue(
        private val error: SerdeError,
    ) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = SerdeResult.failure(error)
    }

    private fun assertFailure(result: SerdeResult<*>, error: SerdeError) {
        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun newtypeVariantPreservesFailureAndSuccessfulPayload() {
        val error = SerdeError.custom("nested value failed")
        val serializer = io.github.kotlinmania.serdejson.Serializer()
        assertFailure(serializer.serializeNewtypeVariant("State", 0u, "Ready", FailedValue(error)), error)
        val value = serializer.serializeNewtypeVariant("State", 0u, "Ready", Value.Str("payload")).getOrThrow()
        assertEquals(Value.Str("payload"), assertIs<Value.Object>(value).value["Ready"])
    }

    @Test
    fun sequenceFailureDoesNotAppendAnElement() {
        val error = SerdeError.custom("element failed")
        val sequence = SerializeVec()
        assertFailure(sequence.serializeElement(FailedValue(error)), error)
        assertTrue(sequence.serializeElement<Serialize>(Value.Str("payload")).isSuccess)
        assertEquals(listOf(Value.Str("payload")), assertIs<Value.Array>(sequence.end().getOrThrow()).value)
    }

    @Test
    fun tupleVariantFailureDoesNotAppendAField() {
        val error = SerdeError.custom("tuple field failed")
        val tuple = SerializeTupleVariant("Ready")
        assertFailure(tuple.serializeField(FailedValue(error)), error)
        assertTrue(tuple.serializeField(Value.Str("payload")).isSuccess)
        val value = assertIs<Value.Object>(tuple.end().getOrThrow()).value["Ready"]
        assertEquals(listOf(Value.Str("payload")), assertIs<Value.Array>(value).value)
    }

    @Test
    fun mapFailureConsumesTheKeyWithoutInsertingAValue() {
        val error = SerdeError.custom("map value failed")
        val map = SerializeMap.Map()
        assertTrue(map.serializeKey(Value.Str("failed")).isSuccess)
        assertFailure(map.serializeValue(FailedValue(error)), error)
        assertEquals(null, map.nextKey)
        assertEquals(null, map.map["failed"])
        assertTrue(map.serializeKey(Value.Str("ready")).isSuccess)
        assertTrue(map.serializeValue(Value.Str("payload")).isSuccess)
        assertEquals(Value.Str("payload"), assertIs<Value.Object>(map.end().getOrThrow()).value["ready"])
    }

    @Test
    fun structVariantFailureDoesNotInsertAField() {
        val error = SerdeError.custom("struct field failed")
        val fields = SerializeStructVariant("Ready")
        assertFailure(fields.serializeField("failed", FailedValue(error)), error)
        assertTrue(fields.serializeField("ready", Value.Str("payload")).isSuccess)
        val value = assertIs<Value.Object>(fields.end().getOrThrow()).value["Ready"]
        val members = assertIs<Value.Object>(value).value
        assertEquals(null, members["failed"])
        assertEquals(Value.Str("payload"), members["ready"])
    }
}
