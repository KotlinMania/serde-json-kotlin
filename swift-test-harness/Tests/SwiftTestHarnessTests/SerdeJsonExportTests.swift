import Testing
import SerdeJson
import IoGithubKotlinmaniaSerdeCore

@Suite("Concrete JSON outcomes")
struct SerdeJsonExportTests {
    @Test func parsedNullHasConcreteValueType() {
        let result: JsonValueOutcome = fromStr(json: "null")
        let value: Value? = result.value
        #expect(value?.isNull() == true)
        #expect(result.error == nil)
    }

    @Test func serializationHasConcreteStringType() {
        let parsed = fromStr(json: "\"value\"")
        let value: Value = parsed.value!
        let encoded: JsonStringOutcome = toStr(value: value)
        let text: String? = encoded.value
        #expect(text == "\"value\"")
        #expect(encoded.error == nil)
    }

    @Test func parseFailureHasConcreteDiagnosticType() {
        let result = fromStr(json: "null false")
        let value: Value? = result.value
        let error: io.github.kotlinmania.serde.SerdeError? = result.error
        #expect(value == nil)
        #expect(error != nil)
        #expect(result.exceptionOrNull() === error)
    }

    @Test func outcomeRetainsOriginalDiagnosticIdentity() {
        let error = io.github.kotlinmania.serde.SerdeError(message: "original", source: nil)
        let result = JsonValueOutcome.companion.failure(error: error)
        let value: Value? = result.value
        #expect(value == nil)
        #expect(result.error === error)
    }

    @Test func jsonInputFeedsConcreteModelDecoding() {
        let input = JsonDecoding.shared.input(json: "0")
        let result = io.github.kotlinmania.serde.SerdeDecoding.shared.signed(input: input)
        let value: Int64? = result.value
        #expect(value == 0)
        #expect(result.error == nil)
    }

    @Test func rawValueRetainsFormatting() {
        let text = "{ \"value\" : [ 1, 2 ] }"
        let parsed: JsonRawValueOutcome = rawValueFromString(json: text)
        let value: RawValue? = parsed.value
        #expect(value != nil)
        let encoded = toStr(value: value!)
        let actual: String? = encoded.value
        #expect(actual == text)
        #expect(encoded.error == nil)
    }
}
