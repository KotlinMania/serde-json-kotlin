package io.github.kotlinmania.serdejson

import io.github.kotlinmania.serde.SerdeDecodeInput

/** Supplies the JSON format to concrete data-model decoding operations. */
object JsonDecoding {
    fun input(json: String): SerdeDecodeInput =
        SerdeDecodeInput.fromDeserializer(JsonDeserializer(JsonParser(json)))
}
