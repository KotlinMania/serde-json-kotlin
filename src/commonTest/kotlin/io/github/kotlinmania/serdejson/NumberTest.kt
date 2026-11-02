// port-lint: tests number.rs
package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumberTest {
    private fun assertSymmetricEqualsAndHash(a: JsonNumber, b: JsonNumber) {
        assertTrue(a == b)
        assertTrue(b == a)
        assertEquals(a, b)
        assertEquals(b, a)
        assertEquals(a.hashCode(), b.hashCode())
    }

    private fun assertSymmetricNotEquals(a: JsonNumber, b: JsonNumber) {
        assertFalse(a == b)
        assertFalse(b == a)
        assertNotEquals(a, b)
        assertNotEquals(b, a)
    }

    @Test
    fun testIntNumbers() {
        val n1 = JsonNumber.from(42)
        assertTrue(n1.isI64())
        assertTrue(n1.isU64())
        assertFalse(n1.isF64())
        assertEquals(42L, n1.asI64())
        assertEquals(42UL, n1.asU64())
        assertEquals(42.0, n1.asF64())

        val nNeg = JsonNumber.from(-100L)
        assertTrue(nNeg.isI64())
        assertFalse(nNeg.isU64())
        assertEquals(-100L, nNeg.asI64())
        assertNull(nNeg.asU64())
    }

    @Test
    fun testFloatNumbers() {
        val nf = JsonNumber.from(3.14)!!
        assertTrue(nf.isF64())
        assertFalse(nf.isI64())
        assertFalse(nf.isU64())
        assertEquals(3.14, nf.asF64())

        assertNull(JsonNumber.from(Double.NaN))
        assertNull(JsonNumber.from(Double.POSITIVE_INFINITY))
        assertNull(JsonNumber.from(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun testNumberEquality() {
        val n1 = JsonNumber.from(100)
        val n2 = JsonNumber.from(100L)
        val n3 = assertNotNull(JsonNumber.from(100.0))
        assertSymmetricEqualsAndHash(n1, n2)
        assertSymmetricNotEquals(n1, n3)
    }

    @Test
    fun testPositiveIntAndFloatInequalityBothDirections() {
        assertSymmetricNotEquals(JsonNumber.from(1), assertNotNull(JsonNumber.from(1.0)))
        assertSymmetricNotEquals(JsonNumber.fromU64(1uL), assertNotNull(JsonNumber.fromF64(1.0)))
        assertSymmetricNotEquals(JsonNumber.fromU64(42uL), assertNotNull(JsonNumber.fromF32(42.0f)))
        assertSymmetricNotEquals(
            deserializeFromStr("1", JsonNumber).getOrThrow(),
            deserializeFromStr("1.0", JsonNumber).getOrThrow(),
        )
    }

    @Test
    fun testNegativeIntAndFloatInequalityBothDirections() {
        assertSymmetricNotEquals(JsonNumber.from(-1), assertNotNull(JsonNumber.from(-1.0)))
        assertSymmetricNotEquals(JsonNumber.fromI64(-42L), assertNotNull(JsonNumber.fromF64(-42.0)))
        assertSymmetricNotEquals(JsonNumber.fromI64(-42L), assertNotNull(JsonNumber.fromF32(-42.0f)))
        assertSymmetricNotEquals(
            JsonNumber.fromI64(Long.MIN_VALUE),
            assertNotNull(JsonNumber.fromF64(Long.MIN_VALUE.toDouble())),
        )
        assertSymmetricNotEquals(JsonNumber.from(1), JsonNumber.from(-1))
        assertSymmetricNotEquals(
            deserializeFromStr("-7", JsonNumber).getOrThrow(),
            deserializeFromStr("-7.0", JsonNumber).getOrThrow(),
        )
    }

    @Test
    fun testTwoPow53PrecisionBoundaryInequality() {
        val exactBoundaryI64 = 9_007_199_254_740_992L // 2^53
        val roundedNeighborI64 = 9_007_199_254_740_993L // 2^53 + 1 (rounds to 2^53 in IEEE-754 Double)
        assertEquals(exactBoundaryI64.toDouble(), roundedNeighborI64.toDouble())

        val posExact = JsonNumber.fromI64(exactBoundaryI64)
        val posNeighbor = JsonNumber.fromI64(roundedNeighborI64)
        val posUnsignedNeighbor = JsonNumber.fromU64(roundedNeighborI64.toULong())
        val posBoundaryFloat = assertNotNull(JsonNumber.fromF64(9_007_199_254_740_992.0))

        assertSymmetricNotEquals(posExact, posNeighbor)
        assertSymmetricNotEquals(posExact, posBoundaryFloat)
        assertSymmetricNotEquals(posNeighbor, posBoundaryFloat)
        assertSymmetricNotEquals(posUnsignedNeighbor, posBoundaryFloat)
        assertSymmetricEqualsAndHash(posNeighbor, posUnsignedNeighbor)

        val negExact = JsonNumber.fromI64(-exactBoundaryI64)
        val negNeighbor = JsonNumber.fromI64(-roundedNeighborI64)
        val negBoundaryFloat = assertNotNull(JsonNumber.fromF64(-9_007_199_254_740_992.0))

        assertSymmetricNotEquals(negExact, negNeighbor)
        assertSymmetricNotEquals(negExact, negBoundaryFloat)
        assertSymmetricNotEquals(negNeighbor, negBoundaryFloat)

        val maxUnsigned = JsonNumber.fromU64(ULong.MAX_VALUE)
        val maxUnsignedRoundedFloat = assertNotNull(JsonNumber.fromF64(ULong.MAX_VALUE.toDouble()))
        assertSymmetricNotEquals(maxUnsigned, maxUnsignedRoundedFloat)
    }

    @Test
    fun testNormalizedPositiveSignedAndUnsignedConstructorsEqual() {
        val zeroRepresentations =
            listOf(
                JsonNumber.from(0.toByte()),
                JsonNumber.from(0.toShort()),
                JsonNumber.from(0),
                JsonNumber.from(0L),
                JsonNumber.fromI32(0),
                JsonNumber.fromI64(0L),
                JsonNumber.from(0u.toUByte()),
                JsonNumber.from(0u.toUShort()),
                JsonNumber.from(0u),
                JsonNumber.from(0uL),
                JsonNumber.fromU32(0u),
                JsonNumber.fromU64(0uL),
                assertNotNull(JsonNumber.fromI128("0")),
                assertNotNull(JsonNumber.fromI128("-0")),
                assertNotNull(JsonNumber.fromU128("0")),
                deserializeFromStr("0", JsonNumber).getOrThrow(),
            )
        for (a in zeroRepresentations) {
            for (b in zeroRepresentations) {
                assertSymmetricEqualsAndHash(a, b)
            }
        }

        val fortyTwoRepresentations =
            listOf(
                JsonNumber.from(42.toByte()),
                JsonNumber.from(42.toShort()),
                JsonNumber.from(42),
                JsonNumber.from(42L),
                JsonNumber.fromI32(42),
                JsonNumber.fromI64(42L),
                JsonNumber.from(42u.toUByte()),
                JsonNumber.from(42u.toUShort()),
                JsonNumber.from(42u),
                JsonNumber.from(42uL),
                JsonNumber.fromU32(42u),
                JsonNumber.fromU64(42uL),
                assertNotNull(JsonNumber.fromI128("42")),
                assertNotNull(JsonNumber.fromU128("42")),
                deserializeFromStr("42", JsonNumber).getOrThrow(),
            )
        for (a in fortyTwoRepresentations) {
            for (b in fortyTwoRepresentations) {
                assertSymmetricEqualsAndHash(a, b)
            }
        }

        assertSymmetricEqualsAndHash(
            JsonNumber.fromI64(Long.MAX_VALUE),
            JsonNumber.fromU64(Long.MAX_VALUE.toULong()),
        )
        assertSymmetricEqualsAndHash(
            assertNotNull(JsonNumber.fromI128(Long.MAX_VALUE.toString())),
            assertNotNull(JsonNumber.fromU128(Long.MAX_VALUE.toString())),
        )
    }

    @Test
    fun testSameRepresentationEqualityAndHashConsistency() {
        val maxPosA = JsonNumber.fromU64(ULong.MAX_VALUE)
        val maxPosB = deserializeFromStr("18446744073709551615", JsonNumber).getOrThrow()
        assertSymmetricEqualsAndHash(maxPosA, maxPosB)
        assertSymmetricEqualsAndHash(maxPosA, maxPosA.clone())
        assertSymmetricNotEquals(maxPosA, JsonNumber.fromU64(ULong.MAX_VALUE - 1uL))

        val negReps =
            listOf(
                JsonNumber.from((-17).toByte()),
                JsonNumber.from((-17).toShort()),
                JsonNumber.from(-17),
                JsonNumber.from(-17L),
                JsonNumber.fromI32(-17),
                JsonNumber.fromI64(-17L),
                assertNotNull(JsonNumber.fromI128("-17")),
                deserializeFromStr("-17", JsonNumber).getOrThrow(),
                JsonNumber.fromI64(-17L).clone(),
            )
        for (a in negReps) {
            for (b in negReps) {
                assertSymmetricEqualsAndHash(a, b)
            }
        }
        assertSymmetricEqualsAndHash(
            JsonNumber.fromI64(Long.MIN_VALUE),
            assertNotNull(JsonNumber.fromI128(Long.MIN_VALUE.toString())),
        )
        assertSymmetricNotEquals(JsonNumber.fromI64(Long.MIN_VALUE), JsonNumber.fromI64(Long.MIN_VALUE + 1L))

        val floatA = assertNotNull(JsonNumber.fromF64(1.5))
        val floatB = assertNotNull(JsonNumber.fromF32(1.5f))
        val floatParsed = deserializeFromStr("1.5", JsonNumber).getOrThrow()
        assertSymmetricEqualsAndHash(floatA, floatB)
        assertSymmetricEqualsAndHash(floatA, floatParsed)
        assertSymmetricEqualsAndHash(floatA, floatA.clone())
        assertSymmetricNotEquals(floatA, assertNotNull(JsonNumber.fromF64(2.5)))

        val nonNullTarget: Any? = null
        assertFalse(floatA.equals(nonNullTarget))
        assertFalse(floatA.equals(1.5))
        assertFalse(JsonNumber.from(1).equals(1))
    }

    @Test
    fun testSignedZeroFloatsEqualWithSameHashWhileIntegerZeroDistinct() {
        assertNotEquals(0.0.toRawBits(), (-0.0).toRawBits())

        val posZeroF64 = assertNotNull(JsonNumber.fromF64(0.0))
        val negZeroF64 = assertNotNull(JsonNumber.fromF64(-0.0))
        val posZeroF32 = assertNotNull(JsonNumber.fromF32(0.0f))
        val negZeroF32 = assertNotNull(JsonNumber.fromF32(-0.0f))
        val parsedPosZeroFloat = deserializeFromStr("0.0", JsonNumber).getOrThrow()
        val parsedNegZeroFloat = deserializeFromStr("-0.0", JsonNumber).getOrThrow()
        val parsedNegZeroIntSyntax = deserializeFromStr("-0", JsonNumber).getOrThrow()

        val zeroFloats =
            listOf(
                posZeroF64,
                negZeroF64,
                posZeroF32,
                negZeroF32,
                parsedPosZeroFloat,
                parsedNegZeroFloat,
                parsedNegZeroIntSyntax,
            )
        for (a in zeroFloats) {
            for (b in zeroFloats) {
                assertSymmetricEqualsAndHash(a, b)
            }
        }

        val intZeroSigned = JsonNumber.fromI64(0L)
        val intZeroUnsigned = JsonNumber.fromU64(0uL)
        val parsedIntZero = deserializeFromStr("0", JsonNumber).getOrThrow()
        assertSymmetricEqualsAndHash(intZeroSigned, intZeroUnsigned)
        assertSymmetricEqualsAndHash(intZeroSigned, parsedIntZero)

        for (zeroFloat in zeroFloats) {
            assertSymmetricNotEquals(intZeroSigned, zeroFloat)
            assertSymmetricNotEquals(intZeroUnsigned, zeroFloat)
            assertSymmetricNotEquals(parsedIntZero, zeroFloat)
        }
    }

    @Test
    fun testHashSetAndHashMapBehaviorAcrossVariantsAndSignedZero() {
        val zeroSet = HashSet<JsonNumber>()
        assertTrue(zeroSet.add(JsonNumber.fromI64(0L)))
        assertFalse(zeroSet.add(JsonNumber.fromU64(0uL)))
        assertTrue(zeroSet.add(assertNotNull(JsonNumber.fromF64(0.0))))
        assertFalse(zeroSet.add(assertNotNull(JsonNumber.fromF64(-0.0))))
        assertEquals(2, zeroSet.size)
        assertTrue(zeroSet.contains(JsonNumber.from(0)))
        assertTrue(zeroSet.contains(assertNotNull(JsonNumber.from(0.0))))
        assertTrue(zeroSet.contains(assertNotNull(JsonNumber.from(-0.0))))

        assertTrue(zeroSet.remove(assertNotNull(JsonNumber.fromF64(-0.0))))
        assertEquals(1, zeroSet.size)
        assertTrue(zeroSet.contains(JsonNumber.from(0)))
        assertFalse(zeroSet.contains(assertNotNull(JsonNumber.from(0.0))))

        val mixedSet =
            hashSetOf(
                JsonNumber.from(1),
                JsonNumber.from(1uL),
                assertNotNull(JsonNumber.from(1.0)),
                JsonNumber.from(-1),
                assertNotNull(JsonNumber.from(-1.0)),
                JsonNumber.fromI64(9_007_199_254_740_992L),
                JsonNumber.fromI64(9_007_199_254_740_993L),
                assertNotNull(JsonNumber.fromF64(9_007_199_254_740_992.0)),
            )
        assertEquals(7, mixedSet.size)
        assertTrue(mixedSet.contains(JsonNumber.fromI64(1L)))
        assertTrue(mixedSet.contains(assertNotNull(JsonNumber.fromF64(1.0))))
        assertTrue(mixedSet.contains(JsonNumber.fromI64(-1L)))
        assertTrue(mixedSet.contains(assertNotNull(JsonNumber.fromF64(-1.0))))
        assertTrue(mixedSet.contains(JsonNumber.fromU64(9_007_199_254_740_992uL)))
        assertTrue(mixedSet.contains(JsonNumber.fromU64(9_007_199_254_740_993uL)))
        assertTrue(mixedSet.contains(assertNotNull(JsonNumber.fromF64(9_007_199_254_740_992.0))))

        val map = HashMap<JsonNumber, String>()
        map[JsonNumber.fromI64(0L)] = "int-zero"
        map[assertNotNull(JsonNumber.fromF64(0.0))] = "pos-float-zero"
        val replacedZeroFloat = map.put(assertNotNull(JsonNumber.fromF64(-0.0)), "neg-float-zero")
        assertEquals("pos-float-zero", replacedZeroFloat)
        assertEquals(2, map.size)
        assertEquals("int-zero", map[JsonNumber.fromU64(0uL)])
        assertEquals("neg-float-zero", map[assertNotNull(JsonNumber.fromF64(0.0))])
        assertEquals("neg-float-zero", map[assertNotNull(JsonNumber.fromF64(-0.0))])

        map[JsonNumber.fromI64(1L)] = "int-one"
        map[assertNotNull(JsonNumber.fromF64(1.0))] = "float-one"
        map[JsonNumber.fromI64(9_007_199_254_740_992L)] = "int-2p53"
        map[JsonNumber.fromI64(9_007_199_254_740_993L)] = "int-2p53-plus-1"
        map[assertNotNull(JsonNumber.fromF64(9_007_199_254_740_992.0))] = "float-2p53"
        assertEquals(7, map.size)
        assertEquals("int-one", map[JsonNumber.fromU64(1uL)])
        assertEquals("float-one", map[assertNotNull(JsonNumber.fromF64(1.0))])
        assertEquals("int-2p53", map[JsonNumber.fromU64(9_007_199_254_740_992uL)])
        assertEquals("int-2p53-plus-1", map[JsonNumber.fromU64(9_007_199_254_740_993uL)])
        assertEquals("float-2p53", map[assertNotNull(JsonNumber.fromF64(9_007_199_254_740_992.0))])
    }
}
