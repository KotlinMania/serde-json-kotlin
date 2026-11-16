package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.BooleanDeserialize
import io.github.kotlinmania.serdecore.de.CharDeserialize
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.DeserializeSeed
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.DurationDeserialize
import io.github.kotlinmania.serdecore.de.EnumAccess
import io.github.kotlinmania.serdecore.de.F32Deserialize
import io.github.kotlinmania.serdecore.de.I128Deserialize
import io.github.kotlinmania.serdecore.de.I16Deserialize
import io.github.kotlinmania.serdecore.de.I32Deserialize
import io.github.kotlinmania.serdecore.de.I64Deserialize
import io.github.kotlinmania.serdecore.de.I8Deserialize
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.SeqAccess
import io.github.kotlinmania.serdecore.de.SeqElement
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.U128Deserialize
import io.github.kotlinmania.serdecore.de.U16Deserialize
import io.github.kotlinmania.serdecore.de.U32Deserialize
import io.github.kotlinmania.serdecore.de.U8Deserialize
import io.github.kotlinmania.serdecore.de.UnitDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ValueDeserializerDiagnosticParityTest {
    private fun <T> failureMessage(value: Value, deserialize: Deserialize<T>): String =
        assertNotNull(fromValue(value, deserialize).exceptionOrNull()).message

    private fun <T> decode(action: (Deserializer) -> SerdeResult<T>): Deserialize<T> =
        object : Deserialize<T> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<T> = action(deserializer)
        }

    private fun <T> seedOf(deserialize: Deserialize<T>): DeserializeSeed<T> =
        object : DeserializeSeed<T> {
            override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<T> =
                deserialize.deserialize(deserializer)
        }

    private fun expectingVisitor(expected: String): Visitor<Unit> =
        object : Visitor<Unit> {
            override fun expecting(): String = expected
        }

    @Test
    fun scalarAndStringTypeMismatchesPreserveUnexpectedPayloadAndVisitorExpecting() {
        assertEquals(
            "invalid type: string \"bad\", expected i8",
            failureMessage(Value.Str("bad"), I8Deserialize),
        )
        val streamMsg = assertNotNull(fromStr("\"bad\"", I8Deserialize).exceptionOrNull()).message
        assertEquals(
            streamMsg.substringBefore(" at line "),
            failureMessage(Value.Str("bad"), I8Deserialize),
        )

        assertEquals(
            "invalid type: boolean `true`, expected i16",
            failureMessage(Value.Bool(true), I16Deserialize),
        )
        assertEquals(
            "invalid type: unit value, expected i32",
            failureMessage(Value.Null, I32Deserialize),
        )
        assertEquals(
            "invalid type: sequence, expected i128",
            failureMessage(Value.Array(emptyList()), I128Deserialize),
        )
        assertEquals(
            "invalid type: map, expected u8",
            failureMessage(Value.Object(JsonMap()), U8Deserialize),
        )
        assertEquals(
            "invalid type: boolean `false`, expected u16",
            failureMessage(Value.Bool(false), U16Deserialize),
        )
        assertEquals(
            "invalid type: string \"x\", expected u32",
            failureMessage(Value.Str("x"), U32Deserialize),
        )
        assertEquals(
            "invalid type: unit value, expected u128",
            failureMessage(Value.Null, U128Deserialize),
        )
        assertEquals(
            "invalid type: string \"nan\", expected f32",
            failureMessage(Value.Str("nan"), F32Deserialize),
        )
        assertEquals(
            "invalid type: integer `-7`, expected a boolean",
            failureMessage(Value.Number(JsonNumber.fromI64(-7)), BooleanDeserialize),
        )
        assertEquals(
            "invalid type: integer `18446744073709551615`, expected a character",
            failureMessage(Value.Number(JsonNumber.fromU64(ULong.MAX_VALUE)), CharDeserialize),
        )
        assertEquals(
            "invalid type: floating point `1.5`, expected a string",
            failureMessage(Value.Number(assertNotNull(JsonNumber.fromF64(1.5))), StringDeserialize),
        )
        assertEquals(
            "invalid type: floating point `2.0`, expected unit",
            failureMessage(Value.Number(assertNotNull(JsonNumber.fromF64(2.0))), UnitDeserialize),
        )
    }

    @Test
    fun compoundAndCustomVisitorTypeMismatchesPreserveVisitorExpecting() {
        val bytesDecoder = decode { it.deserializeBytes(expectingVisitor("raw packet bytes")) }
        val byteBufDecoder = decode { it.deserializeByteBuf(expectingVisitor("owned packet bytes")) }
        val unitStructDecoder = decode { it.deserializeUnitStruct("Marker", expectingVisitor("unit struct Marker")) }
        val seqDecoder = decode { it.deserializeSeq(expectingVisitor("a custom list")) }
        val tupleDecoder = decode { it.deserializeTuple(2, expectingVisitor("a 2-tuple")) }
        val tupleStructDecoder =
            decode { it.deserializeTupleStruct("Pair", 2, expectingVisitor("tuple struct Pair")) }
        val mapDecoder = decode { it.deserializeMap(expectingVisitor("a string-keyed map")) }
        val structDecoder =
            decode { it.deserializeStruct("Config", listOf("host"), expectingVisitor("struct Config")) }
        val identifierDecoder = decode { it.deserializeIdentifier(expectingVisitor("field identifier")) }

        assertEquals(
            "invalid type: boolean `true`, expected raw packet bytes",
            failureMessage(Value.Bool(true), bytesDecoder),
        )
        assertEquals(
            "invalid type: integer `9`, expected owned packet bytes",
            failureMessage(Value.Number(JsonNumber.fromU64(9uL)), byteBufDecoder),
        )
        assertEquals(
            "invalid type: map, expected unit struct Marker",
            failureMessage(Value.Object(JsonMap()), unitStructDecoder),
        )
        assertEquals(
            "invalid type: unit value, expected a custom list",
            failureMessage(Value.Null, seqDecoder),
        )
        assertEquals(
            "invalid type: integer `-1`, expected a 2-tuple",
            failureMessage(Value.Number(JsonNumber.fromI64(-1)), tupleDecoder),
        )
        assertEquals(
            "invalid type: string \"not-seq\", expected tuple struct Pair",
            failureMessage(Value.Str("not-seq"), tupleStructDecoder),
        )
        assertEquals(
            "invalid type: sequence, expected a string-keyed map",
            failureMessage(Value.Array(emptyList()), mapDecoder),
        )
        assertEquals(
            "invalid type: boolean `false`, expected struct Config",
            failureMessage(Value.Bool(false), structDecoder),
        )
        assertEquals(
            "invalid type: boolean `true`, expected struct Duration",
            failureMessage(Value.Bool(true), DurationDeserialize),
        )
        assertEquals(
            "invalid type: integer `3`, expected field identifier",
            failureMessage(Value.Number(JsonNumber.fromU64(3uL)), identifierDecoder),
        )
    }

    @Test
    fun stringAndByteBufStringPathsInvokeVisitStringForOwnershipAwareVisitors() {
        val ownershipVisitor =
            object : Visitor<String> {
                override fun expecting(): String = "an owned or borrowed string"

                override fun visitStr(v: String): SerdeResult<String> = SerdeResult.success("borrowed:$v")

                override fun visitString(v: String): SerdeResult<String> = SerdeResult.success("owned:$v")
            }

        val input = Value.Str("alpha")
        assertEquals("owned:alpha", fromValue(input, decode { it.deserializeAny(ownershipVisitor) }).getOrThrow())
        assertEquals("owned:alpha", fromValue(input, decode { it.deserializeString(ownershipVisitor) }).getOrThrow())
        assertEquals("owned:alpha", fromValue(input, decode { it.deserializeStr(ownershipVisitor) }).getOrThrow())
        assertEquals("owned:alpha", fromValue(input, decode { it.deserializeByteBuf(ownershipVisitor) }).getOrThrow())
        assertEquals("owned:alpha", fromValue(input, decode { it.deserializeBytes(ownershipVisitor) }).getOrThrow())
    }

    @Test
    fun enumContainerRejectsEmptyMultiKeyAndNonMapNonStringWithUpstreamDiagnostics() {
        val enumDecoder =
            decode<String> { deserializer ->
                deserializer.deserializeEnum(
                    "Sample",
                    listOf("A", "B"),
                    object : Visitor<String> {
                        override fun expecting(): String = "enum Sample"

                        override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<String> =
                            access.variantSeed(seedOf(StringDeserialize)).flatMap { (name, variant) ->
                                variant.unitVariant().map { name }
                            }
                    },
                )
            }

        assertEquals(
            "invalid value: map, expected map with a single key",
            failureMessage(Value.Object(JsonMap()), enumDecoder),
        )
        assertEquals(
            "invalid value: map, expected map with a single key",
            failureMessage(
                Value.Object(JsonMap.from(listOf("A" to Value.Null, "B" to Value.Null))),
                enumDecoder,
            ),
        )
        assertEquals(
            "invalid type: integer `-3`, expected string or map",
            failureMessage(Value.Number(JsonNumber.fromI64(-3)), enumDecoder),
        )
        assertEquals(
            "invalid type: boolean `true`, expected string or map",
            failureMessage(Value.Bool(true), enumDecoder),
        )
        assertEquals(
            "invalid type: unit value, expected string or map",
            failureMessage(Value.Null, enumDecoder),
        )
        assertEquals(
            "invalid type: sequence, expected string or map",
            failureMessage(Value.Array(emptyList()), enumDecoder),
        )
    }

    @Test
    fun enumVariantIdentifierUsesStringIntoDeserializerSemantics() {
        val optionForwardedSeed =
            object : DeserializeSeed<String> {
                override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<String> =
                    deserializer.deserializeOption(OptionForwardedVisitor)
            }

        val enumDecoder =
            decode<String> { deserializer ->
                deserializer.deserializeEnum(
                    "Tagged",
                    listOf("Alpha"),
                    object : Visitor<String> {
                        override fun expecting(): String = "enum Tagged"

                        override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<String> =
                            access.variantSeed(optionForwardedSeed).flatMap { (tag, variant) ->
                                variant.unitVariant().map { tag }
                            }
                    },
                )
            }

        assertEquals("string:Alpha", fromValue(Value.Str("Alpha"), enumDecoder).getOrThrow())
        assertEquals(
            "string:Alpha",
            fromValue(Value.Object(JsonMap.from(listOf("Alpha" to Value.Null))), enumDecoder).getOrThrow(),
        )
    }

    @Test
    fun variantAccessUnitNewtypeTupleAndStructDiagnosticsAndUnitBehaviorMatchUpstream() {
        val unitVariantDecoder =
            decode<String> { deserializer ->
                deserializer.deserializeEnum(
                    "E",
                    listOf("Unit"),
                    object : Visitor<String> {
                        override fun expecting(): String = "enum E"

                        override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<String> =
                            access.variantSeed(seedOf(StringDeserialize)).flatMap { (name, variant) ->
                                variant.unitVariant().map { name }
                            }
                    },
                )
            }

        assertEquals("Unit", fromValue(Value.Str("Unit"), unitVariantDecoder).getOrThrow())
        assertEquals(
            "Unit",
            fromValue(Value.Object(JsonMap.from(listOf("Unit" to Value.Null))), unitVariantDecoder).getOrThrow(),
        )
        assertEquals(
            "invalid type: map, expected unit",
            failureMessage(
                Value.Object(JsonMap.from(listOf("Unit" to Value.Object(JsonMap())))),
                unitVariantDecoder,
            ),
        )
        assertEquals(
            "invalid type: integer `7`, expected unit",
            failureMessage(
                Value.Object(JsonMap.from(listOf("Unit" to Value.Number(JsonNumber.fromU64(7uL))))),
                unitVariantDecoder,
            ),
        )

        val newtypeVariantDecoder =
            decode<Long> { deserializer ->
                deserializer.deserializeEnum(
                    "E",
                    listOf("Newtype"),
                    object : Visitor<Long> {
                        override fun expecting(): String = "enum E"

                        override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<Long> =
                            access.variantSeed(seedOf(StringDeserialize)).flatMap { (_, variant) ->
                                variant.newtypeVariantSeed(seedOf(I64Deserialize))
                            }
                    },
                )
            }

        assertEquals(
            42L,
            fromValue(
                Value.Object(JsonMap.from(listOf("Newtype" to Value.Number(JsonNumber.fromI64(42))))),
                newtypeVariantDecoder,
            ).getOrThrow(),
        )
        assertEquals(
            "invalid type: unit variant, expected newtype variant",
            failureMessage(Value.Str("Newtype"), newtypeVariantDecoder),
        )

        val tupleVariantDecoder =
            decode<String> { deserializer ->
                deserializer.deserializeEnum(
                    "E",
                    listOf("Tuple"),
                    object : Visitor<String> {
                        override fun expecting(): String = "enum E"

                        override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<String> =
                            access.variantSeed(seedOf(StringDeserialize)).flatMap { (_, variant) ->
                                variant.tupleVariant(2, TuplePayloadVisitor)
                            }
                    },
                )
            }

        assertEquals(
            "empty",
            fromValue(
                Value.Object(JsonMap.from(listOf("Tuple" to Value.Array(emptyList())))),
                tupleVariantDecoder,
            ).getOrThrow(),
        )
        assertEquals(
            "1:2",
            fromValue(
                Value.Object(
                    JsonMap.from(
                        listOf(
                            "Tuple" to
                                Value.Array(
                                    listOf(
                                        Value.Number(JsonNumber.fromI64(1)),
                                        Value.Number(JsonNumber.fromI64(2)),
                                    ),
                                ),
                        ),
                    ),
                ),
                tupleVariantDecoder,
            ).getOrThrow(),
        )
        assertEquals(
            "invalid type: unit variant, expected tuple variant",
            failureMessage(Value.Str("Tuple"), tupleVariantDecoder),
        )
        assertEquals(
            "invalid type: map, expected tuple variant",
            failureMessage(
                Value.Object(JsonMap.from(listOf("Tuple" to Value.Object(JsonMap())))),
                tupleVariantDecoder,
            ),
        )
        assertEquals(
            "invalid type: boolean `false`, expected tuple variant",
            failureMessage(
                Value.Object(JsonMap.from(listOf("Tuple" to Value.Bool(false)))),
                tupleVariantDecoder,
            ),
        )

        val structVariantDecoder =
            decode<Long> { deserializer ->
                deserializer.deserializeEnum(
                    "E",
                    listOf("Struct"),
                    object : Visitor<Long> {
                        override fun expecting(): String = "enum E"

                        override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<Long> =
                            access.variantSeed(seedOf(StringDeserialize)).flatMap { (_, variant) ->
                                variant.structVariant(listOf("x"), StructPayloadVisitor)
                            }
                    },
                )
            }

        assertEquals(
            5L,
            fromValue(
                Value.Object(
                    JsonMap.from(
                        listOf(
                            "Struct" to
                                Value.Object(JsonMap.from(listOf("x" to Value.Number(JsonNumber.fromI64(5))))),
                        ),
                    ),
                ),
                structVariantDecoder,
            ).getOrThrow(),
        )
        assertEquals(
            "invalid type: unit variant, expected struct variant",
            failureMessage(Value.Str("Struct"), structVariantDecoder),
        )
        assertEquals(
            "invalid type: sequence, expected struct variant",
            failureMessage(
                Value.Object(JsonMap.from(listOf("Struct" to Value.Array(emptyList())))),
                structVariantDecoder,
            ),
        )
        assertEquals(
            "invalid type: string \"bad\", expected struct variant",
            failureMessage(
                Value.Object(JsonMap.from(listOf("Struct" to Value.Str("bad")))),
                structVariantDecoder,
            ),
        )
    }

    @Test
    fun enumMapKeyNonUnitVariantsReportUnitVariantInvalidType() {
        fun decodeKeyAsNewtypeEnum(key: String): SerdeResult<Long> {
            val mapValue = Value.Object(JsonMap.from(listOf(key to Value.Null)))
            val keyDecoder =
                decode<Long> { deserializer ->
                    deserializer.deserializeEnum(
                        "KeyEnum",
                        listOf("Tag"),
                        object : Visitor<Long> {
                            override fun expecting(): String = "enum KeyEnum"

                            override fun <A : EnumAccess> visitEnum(access: A): SerdeResult<Long> =
                                access.variantSeed(seedOf(StringDeserialize)).flatMap { (_, variant) ->
                                    variant.newtypeVariantSeed(seedOf(I64Deserialize))
                                }
                        },
                    )
                }
            return ValueDeserializer(mapValue).deserializeMap(
                object : Visitor<Long> {
                    override fun expecting(): String = "map with enum key"

                    override fun <M : MapAccess> visitMap(access: M): SerdeResult<Long> =
                        access.nextEntry(keyDecoder, ValueDeserialize).map { entry ->
                            requireNotNull(entry).first
                        }
                },
            )
        }

        val err = assertNotNull(decodeKeyAsNewtypeEnum("Tag").exceptionOrNull())
        assertEquals("invalid type: unit variant, expected newtype variant", err.message)
    }
}

private object OptionForwardedVisitor : Visitor<String> {
    override fun expecting(): String = "a variant string"

    override fun visitString(v: String): SerdeResult<String> = SerdeResult.success("string:$v")

    override fun <Inner : Deserializer> visitSome(deserializer: Inner): SerdeResult<String> =
        SerdeResult.success("some")
}

private object TuplePayloadVisitor : Visitor<String> {
    override fun expecting(): String = "tuple payload"

    override fun visitUnit(): SerdeResult<String> = SerdeResult.success("empty")

    override fun <S : SeqAccess> visitSeq(access: S): SerdeResult<String> {
        val a = access.nextElement(I64Deserialize).getOrElse { return SerdeResult.failure(it) }
        val b = access.nextElement(I64Deserialize).getOrElse { return SerdeResult.failure(it) }
        val first = (a as SeqElement.Some).value
        val second = (b as SeqElement.Some).value
        return SerdeResult.success("$first:$second")
    }
}

private object StructPayloadVisitor : Visitor<Long> {
    override fun expecting(): String = "struct payload"

    override fun <M : MapAccess> visitMap(access: M): SerdeResult<Long> =
        access.nextEntry(StringDeserialize, I64Deserialize).map { entry ->
            requireNotNull(entry).second
        }
}
