package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BooleanDeserialize
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.F64Deserialize
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.U64Deserialize
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ValueMapKeyParityTest {
    private fun <K> decodeKey(key: String, deserialize: Deserialize<K>): SerdeResult<K> {
        val value = Value.Object(JsonMap.from(listOf(key to Value.Null)))
        return ValueDeserializer(value).deserializeMap(
            object : Visitor<K> {
                override fun expecting(): String = "a single entry"

                override fun <A : MapAccess> visitMap(access: A): SerdeResult<K> =
                    access.nextEntry(deserialize, ValueDeserialize).map { entry -> requireNotNull(entry).first }
            },
        )
    }

    @Test
    fun scalarMapKeysDecodeFromValues() {
        assertEquals(-12L, decodeKey("-12", I64Deserialize).getOrThrow())
        assertEquals(ULong.MAX_VALUE, decodeKey(ULong.MAX_VALUE.toString(), U64Deserialize).getOrThrow())
        assertEquals(1.5, decodeKey("1.5", F64Deserialize).getOrThrow())
        assertEquals(true, decodeKey("true", BooleanDeserialize).getOrThrow())
        assertEquals(false, decodeKey("false", BooleanDeserialize).getOrThrow())
        assertEquals("text", decodeKey("text", StringDeserialize).getOrThrow())
    }

    @Test
    fun invalidNumericAndBooleanKeysAreRejected() {
        for (key in listOf("", "+1", " 1", "1 ", "01", "1x", "null", "NaN", "Infinity", "1.5")) {
            assertTrue(decodeKey(key, I64Deserialize).isFailure, key)
        }
        assertTrue(decodeKey("18446744073709551615", I64Deserialize).isFailure)
        assertTrue(decodeKey("-1", U64Deserialize).isFailure)
        assertTrue(decodeKey("True", BooleanDeserialize).isFailure)
        assertTrue(decodeKey("false ", BooleanDeserialize).isFailure)
    }

    private class WideKey(
        private val signed: Boolean,
    ) : Deserialize<String> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<String> {
            val visitor =
                object : Visitor<String> {
                    override fun expecting(): String = "a wide integer"

                    override fun visitI128(v: String): SerdeResult<String> = SerdeResult.success(v)

                    override fun visitU128(v: String): SerdeResult<String> = SerdeResult.success(v)
                }
            return if (signed) deserializer.deserializeI128(visitor) else deserializer.deserializeU128(visitor)
        }
    }

    @Test
    fun wideMapKeysDecodeWithActualIntegerBounds() {
        val minimum = "-170141183460469231731687303715884105728"
        val maximum = "340282366920938463463374607431768211455"
        assertEquals(minimum, decodeKey(minimum, WideKey(true)).getOrThrow())
        assertEquals(maximum, decodeKey(maximum, WideKey(false)).getOrThrow())
        assertTrue(decodeKey("170141183460469231731687303715884105728", WideKey(true)).isFailure)
        assertTrue(decodeKey("-170141183460469231731687303715884105729", WideKey(true)).isFailure)
        assertTrue(decodeKey("340282366920938463463374607431768211456", WideKey(false)).isFailure)
        assertTrue(decodeKey("-1", WideKey(false)).isFailure)
    }

    @Test
    fun optionAndNewtypeMapKeysUseTheKeyDeserializer() {
        val optional =
            object : Deserialize<Long> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Long> =
                    deserializer.deserializeOption(
                        object : Visitor<Long> {
                            override fun expecting(): String = "an optional integer"

                            override fun <D : Deserializer> visitSome(deserializer: D): SerdeResult<Long> = I64Deserialize.deserialize(deserializer)
                        },
                    )
            }
        val newtype =
            object : Deserialize<Long> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Long> =
                    deserializer.deserializeNewtypeStruct(
                        "Key",
                        object : Visitor<Long> {
                            override fun expecting(): String = "an integer key"

                            override fun <D : Deserializer> visitNewtypeStruct(deserializer: D): SerdeResult<Long> = I64Deserialize.deserialize(deserializer)
                        },
                    )
            }
        assertEquals(7L, decodeKey("7", optional).getOrThrow())
        assertEquals(8L, decodeKey("8", newtype).getOrThrow())
    }

    @Test
    fun enumMapKeysRetainUnitVariantBehavior() {
        val variant =
            object : Deserialize<String> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<String> =
                    deserializer.deserializeEnum(
                        "Direction",
                        listOf("Forward"),
                        object : Visitor<String> {
                            override fun expecting(): String = "a direction"

                            override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<String> =
                                access
                                    .variantSeed(
                                        object : DeserializeSeed<String> {
                                            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<String> = StringDeserialize.deserialize(deserializer)
                                        },
                                    ).flatMap { (name, variant) -> variant.unitVariant().map { name } }
                        },
                    )
            }
        assertEquals("Forward", decodeKey("Forward", variant).getOrThrow())
    }

    @Test
    fun keyVisitorFailuresRetainTheirIdentity() {
        val error = SerdeError.custom("key visitor failed")
        val key =
            object : Deserialize<Long> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<Long> =
                    deserializer.deserializeI64(
                        object : Visitor<Long> {
                            override fun expecting(): String = "an integer key"

                            override fun visitU64(v: ULong): SerdeResult<Long> = SerdeResult.failure(error)
                        },
                    )
            }
        assertSame(error, decodeKey("7", key).exceptionOrNull())
    }
}
