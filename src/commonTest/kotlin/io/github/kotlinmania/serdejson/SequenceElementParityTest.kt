package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serde.serdeCatching
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.de.mutableListDeserialize
import io.github.kotlinmania.serdecore.de.nullableDeserialize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SequenceElementParityTest {
    private val decoder = mutableListDeserialize(nullableDeserialize(I64Deserialize))

    @Test
    fun nullableElementsAndEndOfSequenceRemainDistinct() {
        val cases = listOf(
            "[]" to emptyList<Long?>(),
            "[null]" to listOf(null),
            "[null,7,null,9]" to listOf(null, 7L, null, 9L),
        )
        for ((text, expected) in cases) {
            assertEquals(expected, fromStr(text, decoder).getOrThrow())
            val value = fromStr(text).getOrThrow()
            assertEquals(expected, fromValue(value, decoder).getOrThrow())
            assertEquals(value, fromValue(value, ValueDeserialize).getOrThrow())
        }
    }

    @Test
    fun failuresAfterNullAreNotMistakenForEndOfSequence() {
        assertEquals(
            "invalid type: string \"bad\", expected i64 at line 1 column 11",
            assertNotNull(fromStr("[null,\"bad\",9]", decoder).exceptionOrNull()).message,
        )
        assertEquals(
            "trailing comma at line 1 column 7",
            assertNotNull(fromStr("[null,]", decoder).exceptionOrNull()).message,
        )
    }

    @Test
    fun valueSequenceConsumesTheFailedItemAndRetainsRemainingState() {
        val visitor = object : Visitor<Unit> {
            override fun expecting(): String = "sequence"

            override fun <A : SeqAccess> visitSeq(access: A): SerdeResult<Unit> = serdeCatching {
                assertEquals(2, access.sizeHint())
                assertTrue(access.nextElement(I64Deserialize).isFailure)
                assertEquals(1, access.sizeHint())
                assertEquals(SeqElement.Some(7L), access.nextElement(I64Deserialize).getOrThrow())
                assertEquals(0, access.sizeHint())
                assertEquals(SeqElement.None, access.nextElement(I64Deserialize).getOrThrow())
            }
        }
        val value = Value.Array(listOf(Value.Str("bad"), Value.Number(JsonNumber.fromI64(7))))
        ValueDeserializer(value).deserializeSeq(visitor).getOrThrow()
    }
}
