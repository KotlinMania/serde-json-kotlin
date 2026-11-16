package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeDecodeInput
import io.github.kotlinmania.serde.SerdeDecoding

/** Supplies the JSON format to concrete data-model decoding operations. */
object JsonDecoding {
    val model: SerdeDecoding
        get() = SerdeDecoding

    fun input(json: String): SerdeDecodeInput =
        SerdeDecodeInput.fromDeserializer(JsonDeserializer(JsonParser(json)))
}
