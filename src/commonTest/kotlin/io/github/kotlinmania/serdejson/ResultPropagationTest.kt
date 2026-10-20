package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeException
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.io.IoResult
import io.github.kotlinmania.serdejson.io.IoWrite
import io.github.kotlinmania.serdejson.io.VecIoWrite
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ResultPropagationTest {
    private class FailedValue(
        val error: SerdeError,
    ) : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = SerdeResult.failure(error)
    }

    private object TextValue : Serialize {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr("value")
    }

    private fun assertFailure(operation: (JsonSerializer, FailedValue) -> SerdeResult<Unit>) {
        val error = SerdeError.custom("nested serialization failed")
        val result = operation(JsonSerializer(VecIoWrite()), FailedValue(error))
        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun sequencePreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeSeq(1).getOrThrow().serializeElement(value)
    }

    @Test
    fun tuplePreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeTuple(1).getOrThrow().serializeElement(value)
    }

    @Test
    fun tupleStructPreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeTupleStruct("Tuple", 1).getOrThrow().serializeField(value)
    }

    @Test
    fun tupleVariantPreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeTupleVariant("Enum", 0u, "Tuple", 1).getOrThrow().serializeField(value)
    }

    @Test
    fun mapKeyPreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeMap(1).getOrThrow().serializeKey(value)
    }

    @Test
    fun mapValuePreservesNestedFailure() = assertFailure { serializer, value ->
        val map = serializer.serializeMap(1).getOrThrow()
        map.serializeKey(TextValue).getOrThrow()
        map.serializeValue(value)
    }

    @Test
    fun mapEntryKeyPreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeMap(1).getOrThrow().serializeEntry(value, TextValue)
    }

    @Test
    fun mapEntryValuePreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeMap(1).getOrThrow().serializeEntry(TextValue, value)
    }

    @Test
    fun structFieldPreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeStruct("State", 1).getOrThrow().serializeField("offset", value)
    }

    @Test
    fun stringSerializationPreservesStructFailure() {
        val error = SerdeError.custom("negative offset")
        val state = object : Serialize {
            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
                serializer.serializeStruct("State", 1).flatMap { fields ->
                    fields.serializeField("offset", FailedValue(error)).flatMap { fields.end() }
                }
        }
        val result = toStr(state)
        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun structVariantPreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeStructVariant("Enum", 0u, "State", 1).getOrThrow().serializeField("offset", value)
    }

    @Test
    fun newtypeVariantPreservesNestedFailure() = assertFailure { serializer, value ->
        serializer.serializeNewtypeVariant("Enum", 0u, "State", value)
    }

    @Test
    fun newtypeVariantPreservesEndFailure() {
        val error = SerdeError.custom("closing object failed")
        val writer = IoWrite { bytes ->
            if (bytes.contentEquals(byteArrayOf('}'.code.toByte()))) throw SerdeException(error)
            IoResult.ok(Unit)
        }
        val result = JsonSerializer(writer).serializeNewtypeVariant("Enum", 0u, "State", TextValue)
        assertTrue(result.isFailure)
        assertSame(error, result.exceptionOrNull())
    }

    private object Forward : Deserialize<String> {
        override fun <D> deserialize(deserializer: D): SerdeResult<String>
            where D : Deserializer =
            deserializer.deserializeEnum("ScrollDirection", listOf("Forward"), object : Visitor<String> {
                override fun expecting(): String = "ScrollDirection"

                override fun visitEnum(data: EnumAccess): SerdeResult<String> =
                    data.variantSeed(object : DeserializeSeed<String> {
                        override fun <D> deserialize(deserializer: D): SerdeResult<String>
                            where D : Deserializer = StringDeserialize.deserialize(deserializer)
                    }).flatMap { (name, variant) ->
                        if (name == "Forward") {
                            variant.unitVariant().map { name }
                        } else {
                            SerdeResult.failure(SerdeError.unknownVariant(name, listOf("Forward")))
                        }
                    }
            })
    }

    @Test
    fun unitEnumRejectsNonUnitPayload() {
        for (payload in listOf("1", "true", "\"value\"", "[]", "{}")) {
            assertTrue(fromStr("{\"Forward\":$payload}", Forward).isFailure, payload)
        }
    }

    @Test
    fun unitEnumAcceptsNullPayload() {
        assertEquals("Forward", fromStr("{\"Forward\":null}", Forward).getOrThrow())
    }

    @Test
    fun unitEnumAcceptsBareVariant() {
        assertEquals("Forward", fromStr("\"Forward\"", Forward).getOrThrow())
    }
}
