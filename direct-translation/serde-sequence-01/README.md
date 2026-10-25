# Serde JSON sequence repair

This is a bounded Serde API migration and source repair, not a full-file translation.
The existing readers returned nullable payloads; Serde 0.1.12 now exposes
`SeqElement.Some(value)` versus `SeqElement.None`. A successful null item must
not terminate a sequence. The compiler failures exposed that API mismatch; these
old readers were not produced by the current direct generator.

## Generated and integrated

Five fragments were generated using rust-kotlin and the actual proc-macro2/Syn
engines: a `while let Some` vector-appending loop, two successful-result lifts and
two end-of-sequence expressions. `generation.json` records exact commands, failures
and executable hash. Each successful directory retains raw generated Kotlin,
selected Rust, input and bindings. `adoption.json` names the explicit Kotlin API
integration. Only indentation changes when integrating generated text.

`value/de.rs` parses as a complete file. Full `de.rs` fails Syn at line 64 on an
attribute inside a struct initializer. `excerpts.json` records the two exact source
expressions in minimal function wrappers, their original lines and file hash.
`original-de.rs` preserves the complete source. This is not a successful whole-file
parse, and no cfg field was removed to hide the failure.

The Rust `tri!` definition in `tmp/serde_json/src/lib.rs:410` propagates the original
Result error; the command explicitly binds that contract. The vector receiver
binds Rust `Vec.push` to Kotlin `MutableList.add`. Result expressions use Serde's
existing `map` so errors pass through and successful nulls remain tagged.

## Verification

The Rust oracle uses original serde_json 1.0.149 and cached Serde 1.0.228 offline.
It checks empty/single-null/mixed arrays via text and Value, Value roundtrips,
errors after null, and iterator state after a failed item. The Kotlin regression
uses the same inputs and exact expected errors and state. The first comparison
exposed missing error locations in `peekInvalidType` and an off-by-one peek
position. Those are explicit source-derived integration repairs, not generated
fragments; see `error-location-source.txt`. No assertions were weakened.

The tests cover this sequence behavior, not all JSON parsing or Unicode byte
position behavior. Full-source parser support, generic ownership, and complete
Serde JSON translation remain separate obligations. No target, test or source
exclusion was added. No Kotlinx serialization substitution was introduced.

`repair.diff` is the ordinary diff against the saved hand port. Runtime test and
publication receipts are recorded in `validation.json`; partial checks must not be
reported as all-platform runtime acceptance.
