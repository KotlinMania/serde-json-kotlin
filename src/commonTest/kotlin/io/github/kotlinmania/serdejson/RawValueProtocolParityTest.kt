package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BooleanDeserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RawValueProtocolParityTest {
    @Test
    fun rawValuesDriveFreshTypedDeserializers() {
        val number = rawValueFromString("7").getOrThrow()
        assertEquals(7L, I64Deserialize.deserialize(number).getOrThrow())
        assertEquals(7L, I64Deserialize.deserialize(number).getOrThrow())
        val boolean = rawValueFromString("true").getOrThrow()
        assertEquals(true, BooleanDeserialize.deserialize(boolean).getOrThrow())
        assertEquals(true, BooleanDeserialize.deserialize(boolean).getOrThrow())
        assertTrue(I64Deserialize.deserialize(boolean).isFailure)
        val sequence = rawValueFromString("[true,null]").getOrThrow()
        val array = assertIs<Value.Array>(ValueDeserialize.deserialize(sequence).getOrThrow())
        assertEquals(true, assertIs<Value.Bool>(array.value[0]).value)
        assertSame(Value.Null, array.value[1])
    }

    @Test
    fun ownedRawInputTrimsOutsideWhitespaceAndPreservesContents() {
        val contents = "{ \"value\" : [ 1, 2 ] }"
        val input = " \n$contents\t "
        assertEquals(contents, rawValueFromString(input).getOrThrow().json)
        assertEquals(contents, deserializeFromStr(input, RawValue).getOrThrow().json)
        assertEquals(contents, toStr(rawValueFromString(input).getOrThrow()).getOrThrow())
        for (invalid in listOf("", "[", "null false", "{\"x\":}")) {
            assertTrue(rawValueFromString(invalid).isFailure, invalid)
            assertTrue(deserializeFromStr(invalid, RawValue).isFailure, invalid)
        }
    }

    @Test
    fun rawNewtypeUsesASingleEntryMap() {
        val text = "[ true, null ]"
        val visitor =
            object : Visitor<String> {
                override fun expecting(): String = "raw value"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<String> {
                    assertEquals(RawValue.TOKEN, access.nextKey(StringDeserialize).getOrThrow())
                    val raw = access.nextValue(StringDeserialize).getOrThrow()
                    assertNull(access.nextKey(StringDeserialize).getOrThrow())
                    return SerdeResult.success(raw)
                }
            }
        val result = JsonDeserializer(JsonParser(text)).deserializeNewtypeStruct(RawValue.TOKEN, visitor)
        assertEquals(text, result.getOrThrow())
    }

    @Test
    fun valuesDeserializeIntoOwnedRawText() {
        val value = Value.Object(JsonMap.from(listOf("x" to Value.Array(listOf(Value.Bool(true), Value.Null)))))
        assertEquals("{\"x\":[true,null]}", RawValue.deserialize(ValueDeserializer(value)).getOrThrow().json)
        assertEquals("\"text\"", RawValue.deserialize(ValueDeserializer(Value.Str("text"))).getOrThrow().json)
        assertEquals("null", RawValue.deserialize(ValueDeserializer(Value.Null)).getOrThrow().json)
    }

    @Test
    fun reservedRawMapDecodesItsJsonContents() {
        val encoded = """{"${RawValue.TOKEN}":"[true, null]"}"""
        val parsed = assertIs<Value.Array>(fromStr(encoded).getOrThrow())
        assertEquals(true, assertIs<Value.Bool>(parsed.value[0]).value)
        assertSame(Value.Null, parsed.value[1])
        val map = Value.Object(JsonMap.from(listOf(RawValue.TOKEN to Value.Str("{\"x\":false}"))))
        val decoded = assertIs<Value.Object>(ValueDeserialize.deserialize(ValueDeserializer(map)).getOrThrow())
        assertEquals(false, assertIs<Value.Bool>(decoded.value["x"]).value)
    }

    @Test
    fun invalidReservedRawPayloadsAreRejected() {
        assertTrue(fromStr("""{"${RawValue.TOKEN}":1}""").isFailure)
        assertTrue(fromStr("""{"${RawValue.TOKEN}":"["}""").isFailure)
        assertTrue(fromStr("""{"${RawValue.TOKEN}":"null","extra":true}""").isFailure)
    }

    @Test
    fun ordinaryKeysAndTheBoundedNumberProfileRemainOrdinary() {
        val number = assertIs<Value.Object>(fromStr("""{"$NUMBER_TOKEN":"123"}""").getOrThrow())
        assertEquals("123", assertIs<Value.Str>(number.value[NUMBER_TOKEN]).value)
        val ordinary = assertIs<Value.Object>(fromStr("""{"ordinary":1,"${RawValue.TOKEN}":"true"}""").getOrThrow())
        assertEquals("true", assertIs<Value.Str>(ordinary.value[RawValue.TOKEN]).value)
        assertEquals(2, ordinary.value.len())
    }

    @Test
    fun rawVisitorFailuresPreserveTheirDiagnosticIdentity() {
        val error = SerdeError.custom("raw visitor failed")
        val visitor =
            object : Visitor<String> {
                override fun expecting(): String = "raw value"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<String> = SerdeResult.failure(error)
            }
        assertSame(error, JsonDeserializer(JsonParser("true")).deserializeNewtypeStruct(RawValue.TOKEN, visitor).exceptionOrNull())
        assertSame(error, ValueDeserializer(Value.Bool(true)).deserializeNewtypeStruct(RawValue.TOKEN, visitor).exceptionOrNull())
        val access = OwnedRawDeserializer("true")
        val seed =
            object : DeserializeSeed<String> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<String> = SerdeResult.failure(error)
            }
        assertSame(error, access.nextValueSeed(seed).exceptionOrNull())
        assertNull(access.nextKey(StringDeserialize).getOrThrow())
    }

    @Test
    fun valueByteHintsPreserveStringVisitorDispatch() {
        val error = SerdeError.custom("unexpected bytes")
        val visitor =
            object : Visitor<String> {
                override fun expecting(): String = "string"

                override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success(v)

                override fun visitBytes(v: ByteArray): SerdeResult<String> = SerdeResult.failure(error)
            }
        val value = ValueDeserializer(Value.Str("é"))
        assertEquals("é", value.deserializeBytes(visitor).getOrThrow())
        assertEquals("é", value.deserializeByteBuf(visitor).getOrThrow())
    }
}
