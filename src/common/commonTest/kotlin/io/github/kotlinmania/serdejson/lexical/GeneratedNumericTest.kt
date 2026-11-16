package io.github.kotlinmania.serdejson.lexical

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GeneratedNumericTest {
    private fun <T : Any> show(value: T?): String =
        if (value == null) {
            "None"
        } else {
            "Some($value)"
        }

    @Test
    fun everyByteMatchesTheRecordedRustDigitOracle() {
        val some =
            mapOf(
                48 to 0u,
                49 to 1u,
                50 to 2u,
                51 to 3u,
                52 to 4u,
                53 to 5u,
                54 to 6u,
                55 to 7u,
                56 to 8u,
                57 to 9u,
            )
        for (byte in 0..255) {
            val expected = some[byte]?.let { "Some($it)" } ?: "None"
            assertEquals(expected, show(toDigit(byte.toUByte())), "Byte $byte")
        }
    }

    @Test
    fun checkedArithmeticUsesThePublishedNumTraitsContract() {
        val cases =
            listOf(
                0uL to 0u,
                1uL to 9u,
                1844674407370955161uL to 5u,
                1844674407370955161uL to 6u,
                1844674407370955162uL to 0u,
                ULong.MAX_VALUE to 0u,
                ULong.MAX_VALUE to UInt.MAX_VALUE,
                0uL to UInt.MAX_VALUE,
            )
        val expected =
            listOf(
                "Some(0)",
                "Some(19)",
                "Some(18446744073709551615)",
                "None",
                "None",
                "None",
                "None",
                "Some(4294967295)",
            )
        assertEquals(expected, cases.map { (value, digit) -> show(addDigit(value, digit)) })
    }

    @Test
    fun usize64ExponentBoundariesMatchRust() {
        val cases =
            listOf(
                Triple(0, 0uL, 0uL),
                Triple(0, 1uL, 0uL),
                Triple(0, 2uL, 1uL),
                Triple(Int.MAX_VALUE, 0uL, 1uL),
                Triple(Int.MIN_VALUE, 0uL, 1uL),
                Triple(Int.MAX_VALUE, 2uL, 0uL),
                Triple(Int.MIN_VALUE, 2uL, 0uL),
                Triple(7, 2147483647uL, 0uL),
                Triple(-7, 2147483648uL, 0uL),
                Triple(0, ULong.MAX_VALUE, 0uL),
                Triple(0, 0uL, ULong.MAX_VALUE),
                Triple(Int.MAX_VALUE, ULong.MAX_VALUE, ULong.MAX_VALUE - 1uL),
                Triple(Int.MIN_VALUE, ULong.MAX_VALUE - 1uL, ULong.MAX_VALUE),
                Triple(-19, 100uL, 103uL),
                Triple(19, 103uL, 100uL),
                Triple(0, 4294967296uL, 4294967295uL),
            )
        val expected =
            listOf(
                "-1|0",
                "0|-1",
                "1|-1",
                "2147483645|2147483647",
                "-2147483648|-2147483647",
                "2147483647|2147483645",
                "-2147483647|-2147483648",
                "2147483647|-2147483640",
                "2147483640|-2147483648",
                "2147483647|-2147483647",
                "-2147483648|2147483647",
                "2147483647|2147483646",
                "-1|-2147483647",
                "80|-16",
                "121|16",
                "2147483647|-1",
            )
        val actual =
            cases.map { (exponent, first, second) ->
                "${scientificExponent(exponent, first, second)}|${mantissaExponent(exponent, first, second)}"
            }
        assertEquals(expected, actual)
    }

    @Test
    fun migratedBhcompCallersMatchRustBits() {
        val actual =
            listOf(
                bhcomp(F64Float, 1.0, "1".encodeToByteArray(), byteArrayOf(), 0),
                bhcomp(F64Float, 0.00125, byteArrayOf(), "00125".encodeToByteArray(), 0),
                bhcomp(F64Float, 12.5, "12".encodeToByteArray(), "5".encodeToByteArray(), 0),
                bhcomp(F64Float, 0.9, "9".encodeToByteArray(), byteArrayOf(), -1),
            ).map { it.toBits().toULong() }
        assertEquals(
            listOf(4607182418800017408uL, 4563407430421976187uL, 4623226492472524800uL, 4606281698874543309uL),
            actual,
        )
    }

    @Test
    fun migratedDigitCallerRetainsRustUnwrapFailure() {
        for (byte in listOf('A'.code.toByte(), 0xff.toByte())) {
            val failure =
                assertFailsWith<LexicalPanicException> {
                    bhcomp(F64Float, 1.0, byteArrayOf(byte), byteArrayOf(), 0)
                }
            assertEquals("called `Option::unwrap()` on a `None` value", failure.message)
        }
    }
}
