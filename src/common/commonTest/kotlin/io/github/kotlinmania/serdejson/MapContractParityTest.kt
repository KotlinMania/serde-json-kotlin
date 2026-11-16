package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.SerializeMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MapContractParityTest {
    @Test
    fun iteratorsReportRemainingEntries() {
        val map = sampleMap()
        val iter = map.iter()
        assertRemainingEntries(iter, iter::len)
        val iterMut = map.iterMut()
        assertRemainingEntries(iterMut, iterMut::len)
        val intoIter = map.intoIter()
        assertRemainingEntries(intoIter, intoIter::len)
        assertEquals(0, JsonMap().iter().len())
        assertEquals(0, JsonMap().iterMut().len())
        assertEquals(0, JsonMap().intoIter().len())
    }

    @Test
    fun equalMapsHaveEqualHashesRegardlessOfInsertionOrder() {
        val first = sampleMap()
        val second = JsonMap.from(first.entries().reversed())
        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertTrue(second in hashSetOf(first))
        assertEquals(Value.Object(first).hashCode(), Value.Object(second).hashCode())
    }

    @Test
    fun mapCreationFailureReturnsOriginalDiagnostic() {
        val error = SerdeError.custom("map creation failed")
        val serializer =
            object : Serializer() {
                override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Value>> = SerdeResult.failure(error)
            }
        assertSame(error, sampleMap().serialize(serializer).exceptionOrNull())
    }

    @Test
    fun entryFailureStopsBeforeNextEntryAndEnd() {
        val error = SerdeError.custom("entry failed")
        var entries = 0
        var ended = false
        val delegate = Serializer().serializeMap(2).getOrThrow()
        val mapSerializer =
            object : SerializeMap<Value> by delegate {
                override fun <K : Serialize, V : Serialize> serializeEntry(key: K, value: V): SerdeResult<Unit> {
                    entries += 1
                    return SerdeResult.failure(error)
                }

                override fun end(): SerdeResult<Value> {
                    ended = true
                    return delegate.end()
                }
            }
        val serializer =
            object : Serializer() {
                override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Value>> = SerdeResult.success(mapSerializer)
            }
        assertSame(error, sampleMap().serialize(serializer).exceptionOrNull())
        assertEquals(1, entries)
        assertFalse(ended)
    }

    @Test
    fun endFailureReturnsOriginalDiagnostic() {
        val error = SerdeError.custom("map end failed")
        val delegate = Serializer().serializeMap(2).getOrThrow()
        val mapSerializer =
            object : SerializeMap<Value> by delegate {
                override fun end(): SerdeResult<Value> = SerdeResult.failure(error)
            }
        val serializer =
            object : Serializer() {
                override fun serializeMap(len: Int?): SerdeResult<SerializeMap<Value>> = SerdeResult.success(mapSerializer)
            }
        assertSame(error, sampleMap().serialize(serializer).exceptionOrNull())
    }

    @Test
    fun successfulSerializationRetainsEveryEntry() {
        val map = sampleMap()
        assertEquals(Value.Object(map), map.serialize(Serializer()).getOrThrow())
    }

    private fun sampleMap(): JsonMap =
        JsonMap.from(listOf("z" to Value.Str("first"), "a" to Value.Bool(true)))

    private fun assertRemainingEntries(iterator: Iterator<Pair<String, Value>>, remaining: () -> Int) {
        assertEquals(2, remaining())
        assertTrue(iterator.hasNext())
        assertEquals(2, remaining())
        assertEquals("z", iterator.next().first)
        assertEquals(1, remaining())
        assertEquals("a", iterator.next().first)
        assertEquals(0, remaining())
        assertFalse(iterator.hasNext())
        assertFailsWith<NoSuchElementException> { iterator.next() }
        assertEquals(0, remaining())
    }
}
