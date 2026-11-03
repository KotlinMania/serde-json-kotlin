package io.github.kotlinmania.serdejson

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ValueNumberEqualityParityTest {
    private fun assertSymmetricValueEqualsAndHash(a: Value, b: Value) {
        assertTrue(a == b)
        assertTrue(b == a)
        assertEquals(a, b)
        assertEquals(b, a)
        assertEquals(a.hashCode(), b.hashCode())
    }

    private fun assertSymmetricValueNotEquals(a: Value, b: Value) {
        assertFalse(a == b)
        assertFalse(b == a)
        assertNotEquals(a, b)
        assertNotEquals(b, a)
    }

    @Test
    fun integerAndFloatValuesAreUnequalInBothDirections() {
        assertSymmetricValueNotEquals(Value.from(1), Value.from(1.0))
        assertSymmetricValueNotEquals(Value.from(42uL), Value.from(42.0f))
        assertSymmetricValueNotEquals(Value.from(-1), Value.from(-1.0))
        assertSymmetricValueNotEquals(Value.from(-42L), Value.from(-42.0f))
        assertSymmetricValueNotEquals(
            Value.from(Long.MIN_VALUE),
            Value.from(Long.MIN_VALUE.toDouble()),
        )
        assertSymmetricValueNotEquals(
            fromStr("1").getOrThrow(),
            fromStr("1.0").getOrThrow(),
        )
        assertSymmetricValueNotEquals(
            fromStr("-7").getOrThrow(),
            fromStr("-7.0").getOrThrow(),
        )
    }

    @Test
    fun twoPow53NeighborsAndFloatBoundariesRemainDistinct() {
        val exactBoundary = 9_007_199_254_740_992L // 2^53
        val roundedNeighbor = 9_007_199_254_740_993L // 2^53 + 1
        assertEquals(exactBoundary.toDouble(), roundedNeighbor.toDouble())

        val posExact = Value.from(exactBoundary)
        val posNeighbor = Value.from(roundedNeighbor)
        val posUnsignedNeighbor = Value.from(roundedNeighbor.toULong())
        val posFloatBoundary = Value.from(9_007_199_254_740_992.0)

        assertSymmetricValueNotEquals(posExact, posNeighbor)
        assertSymmetricValueNotEquals(posExact, posFloatBoundary)
        assertSymmetricValueNotEquals(posNeighbor, posFloatBoundary)
        assertSymmetricValueNotEquals(posUnsignedNeighbor, posFloatBoundary)
        assertSymmetricValueEqualsAndHash(posNeighbor, posUnsignedNeighbor)

        val negExact = Value.from(-exactBoundary)
        val negNeighbor = Value.from(-roundedNeighbor)
        val negFloatBoundary = Value.from(-9_007_199_254_740_992.0)

        assertSymmetricValueNotEquals(negExact, negNeighbor)
        assertSymmetricValueNotEquals(negExact, negFloatBoundary)
        assertSymmetricValueNotEquals(negNeighbor, negFloatBoundary)

        assertSymmetricValueNotEquals(
            Value.from(ULong.MAX_VALUE),
            Value.from(ULong.MAX_VALUE.toDouble()),
        )
    }

    @Test
    fun signedAndUnsignedOverflowBoundariesDoNotWrapEqual() {
        assertSymmetricValueNotEquals(Value.from(-1L), Value.from(ULong.MAX_VALUE))
        assertSymmetricValueNotEquals(
            fromStr("-1").getOrThrow(),
            fromStr("18446744073709551615").getOrThrow(),
        )

        val firstUnsignedAboveLongMax = Long.MAX_VALUE.toULong() + 1uL // 1uL shl 63 -> Long.MIN_VALUE if wrapped
        assertEquals(Long.MIN_VALUE, firstUnsignedAboveLongMax.toLong())
        assertSymmetricValueNotEquals(Value.from(Long.MIN_VALUE), Value.from(firstUnsignedAboveLongMax))

        val wrappedNegFortyTwoAsULong = (-42L).toULong()
        assertEquals(-42L, wrappedNegFortyTwoAsULong.toLong())
        assertSymmetricValueNotEquals(Value.from(-42L), Value.from(wrappedNegFortyTwoAsULong))

        assertSymmetricValueEqualsAndHash(
            Value.from(Long.MAX_VALUE),
            Value.from(Long.MAX_VALUE.toULong()),
        )
    }

    @Test
    fun normalizedPositiveSignedAndUnsignedValuesEqualWithMatchingHash() {
        val zeroInts =
            listOf(
                Value.from(0.toByte()),
                Value.from(0.toShort()),
                Value.from(0),
                Value.from(0L),
                Value.from(0u.toUByte()),
                Value.from(0u.toUShort()),
                Value.from(0u),
                Value.from(0uL),
                fromStr("0").getOrThrow(),
            )
        for (a in zeroInts) {
            for (b in zeroInts) {
                assertSymmetricValueEqualsAndHash(a, b)
            }
        }

        val fortyTwoInts =
            listOf(
                Value.from(42.toByte()),
                Value.from(42.toShort()),
                Value.from(42),
                Value.from(42L),
                Value.from(42u.toUByte()),
                Value.from(42u.toUShort()),
                Value.from(42u),
                Value.from(42uL),
                fromStr("42").getOrThrow(),
            )
        for (a in fortyTwoInts) {
            for (b in fortyTwoInts) {
                assertSymmetricValueEqualsAndHash(a, b)
            }
        }
    }

    @Test
    fun signedZeroFloatValuesEqualWithSameHashWhileIntegerZeroRemainsDistinct() {
        assertNotEquals(0.0.toRawBits(), (-0.0).toRawBits())

        val zeroFloats =
            listOf(
                Value.from(0.0),
                Value.from(-0.0),
                Value.from(0.0f),
                Value.from(-0.0f),
                fromStr("0.0").getOrThrow(),
                fromStr("-0.0").getOrThrow(),
                fromStr("-0").getOrThrow(),
            )
        for (a in zeroFloats) {
            for (b in zeroFloats) {
                assertSymmetricValueEqualsAndHash(a, b)
            }
        }

        val intZeroSigned = Value.from(0L)
        val intZeroUnsigned = Value.from(0uL)
        val parsedIntZero = fromStr("0").getOrThrow()

        for (zeroFloat in zeroFloats) {
            assertSymmetricValueNotEquals(intZeroSigned, zeroFloat)
            assertSymmetricValueNotEquals(intZeroUnsigned, zeroFloat)
            assertSymmetricValueNotEquals(parsedIntZero, zeroFloat)
        }
    }

    @Test
    fun nestedArraysAndObjectsRecursivelyUseExactNumberEquality() {
        val nestedExact =
            Value.Array(
                listOf(
                    Value.from(1),
                    Value.Object(
                        JsonMap.from(
                            listOf(
                                "max" to Value.from(ULong.MAX_VALUE),
                                "edge" to Value.Array(listOf(Value.from(9_007_199_254_740_993L))),
                            ),
                        ),
                    ),
                ),
            )
        val nestedFloatMismatch =
            Value.Array(
                listOf(
                    Value.from(1.0),
                    Value.Object(
                        JsonMap.from(
                            listOf(
                                "max" to Value.from(ULong.MAX_VALUE),
                                "edge" to Value.Array(listOf(Value.from(9_007_199_254_740_993L))),
                            ),
                        ),
                    ),
                ),
            )
        val nestedOverflowMismatch =
            Value.Array(
                listOf(
                    Value.from(1),
                    Value.Object(
                        JsonMap.from(
                            listOf(
                                "max" to Value.from(-1L),
                                "edge" to Value.Array(listOf(Value.from(9_007_199_254_740_993L))),
                            ),
                        ),
                    ),
                ),
            )
        val nestedRoundedBoundaryMismatch =
            Value.Array(
                listOf(
                    Value.from(1),
                    Value.Object(
                        JsonMap.from(
                            listOf(
                                "max" to Value.from(ULong.MAX_VALUE),
                                "edge" to Value.Array(listOf(Value.from(9_007_199_254_740_992.0))),
                            ),
                        ),
                    ),
                ),
            )

        assertSymmetricValueNotEquals(nestedExact, nestedFloatMismatch)
        assertSymmetricValueNotEquals(nestedExact, nestedOverflowMismatch)
        assertSymmetricValueNotEquals(nestedExact, nestedRoundedBoundaryMismatch)

        val mapUnsignedMax = JsonMap.from(listOf("k" to Value.from(ULong.MAX_VALUE)))
        val mapNegOne = JsonMap.from(listOf("k" to Value.from(-1L)))
        assertFalse(mapUnsignedMax == mapNegOne)
        assertFalse(mapNegOne == mapUnsignedMax)

        val objOrderA =
            Value.Object(
                JsonMap.from(
                    listOf(
                        "zeroFloat" to Value.from(0.0),
                        "items" to Value.Array(listOf(Value.from(42), Value.from(-0.0))),
                    ),
                ),
            )
        val objOrderB =
            Value.Object(
                JsonMap.from(
                    listOf(
                        "items" to Value.Array(listOf(Value.from(42uL), Value.from(0.0))),
                        "zeroFloat" to Value.from(-0.0),
                    ),
                ),
            )
        val objWithIntZero =
            Value.Object(
                JsonMap.from(
                    listOf(
                        "zeroFloat" to Value.from(0),
                        "items" to Value.Array(listOf(Value.from(42), Value.from(-0.0))),
                    ),
                ),
            )

        assertSymmetricValueEqualsAndHash(objOrderA, objOrderB)
        assertSymmetricValueNotEquals(objOrderA, objWithIntZero)
    }

    @Test
    fun hashSetAndHashMapSeparateVariantsAndPreserveSignedZeroCollapse() {
        val zeroSet = HashSet<Value>()
        assertTrue(zeroSet.add(Value.from(0L)))
        assertFalse(zeroSet.add(Value.from(0uL)))
        assertTrue(zeroSet.add(Value.from(0.0)))
        assertFalse(zeroSet.add(Value.from(-0.0)))
        assertEquals(2, zeroSet.size)
        assertTrue(zeroSet.contains(Value.from(0)))
        assertTrue(zeroSet.contains(Value.from(0.0)))
        assertTrue(zeroSet.contains(Value.from(-0.0)))

        val boundarySet =
            hashSetOf(
                Value.from(1),
                Value.from(1uL),
                Value.from(1.0),
                Value.from(-1L),
                Value.from(-1.0),
                Value.from(ULong.MAX_VALUE),
                Value.from(9_007_199_254_740_992L),
                Value.from(9_007_199_254_740_993L),
                Value.from(9_007_199_254_740_992.0),
            )
        assertEquals(8, boundarySet.size)
        assertTrue(boundarySet.contains(Value.from(ULong.MAX_VALUE)))
        assertTrue(boundarySet.contains(Value.from(-1L)))
        assertTrue(boundarySet.contains(Value.from(1.0)))

        val map = HashMap<Value, String>()
        map[Value.from(0L)] = "int-zero"
        map[Value.from(0.0)] = "pos-zero"
        assertEquals("pos-zero", map.put(Value.from(-0.0), "neg-zero"))
        map[Value.from(-1L)] = "neg-one"
        map[Value.from(ULong.MAX_VALUE)] = "u64-max"
        map[Value.from(1)] = "int-one"
        map[Value.from(1.0)] = "float-one"
        assertEquals(6, map.size)
        assertEquals("int-zero", map[Value.from(0uL)])
        assertEquals("neg-zero", map[Value.from(0.0)])
        assertEquals("neg-one", map[Value.from(-1)])
        assertEquals("u64-max", map[Value.from(ULong.MAX_VALUE)])
        assertEquals("int-one", map[Value.from(1uL)])
        assertEquals("float-one", map[Value.from(1.0)])
    }

    @Test
    fun primitivePartialEqHelpersPreserveCoercionContracts() {
        val intFortyTwo = Value.from(42)
        val floatFortyTwo = Value.from(42.0)
        assertSymmetricValueNotEquals(intFortyTwo, floatFortyTwo)

        assertTrue(eqI64(intFortyTwo, 42L))
        assertTrue(eqU64(intFortyTwo, 42uL))
        assertTrue(eqF64(intFortyTwo, 42.0))
        assertTrue(eqF32(intFortyTwo, 42.0f))

        assertFalse(eqI64(floatFortyTwo, 42L))
        assertFalse(eqU64(floatFortyTwo, 42uL))
        assertTrue(eqF64(floatFortyTwo, 42.0))
        assertTrue(eqF32(floatFortyTwo, 42.0f))

        val negOne = Value.from(-1L)
        val u64Max = Value.from(ULong.MAX_VALUE)
        assertTrue(eqI64(negOne, -1L))
        assertFalse(eqU64(negOne, ULong.MAX_VALUE))
        assertTrue(eqU64(u64Max, ULong.MAX_VALUE))
        assertFalse(eqI64(u64Max, -1L))
    }
}
