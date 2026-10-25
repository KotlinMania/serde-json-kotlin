# Sequence fragments regenerated from complete Rust inputs

The corrected existing Syn parser accepts the unchanged complete `de.rs` (58
top-level items) and `value/de.rs` (61 items), with the translator's AST/token
correspondence checks. The first pass's `de.rs:64` failure on an attributed struct
field is fixed; its excerpt wrappers remain historical evidence in pass 01.

All five fragments are regenerated here from complete original source files.
`generation.json` records commands, source hashes and the rebuilt executable hash.
Each output directory contains the full input, selected source span, untouched
Kotlin output and explicit binding contract. All five ordinary `diff -u` results
against the previously adopted outputs are empty. Production code is unchanged.
This is complete-file parsing plus selected-fragment emission, **not** translation
of the entire JSON deserializer or value module.

The preceding repair's 119 JVM / 119 macOS tests and Rust behavior comparisons
apply to the identical adopted fragments; they were not rerun solely to increase
test counts. The rebuilt translator passes all 186 tests using normal local Maven
dependencies, without frozen-library substitution. See `frontend-validation.json`.
Syn passes 422 JVM / 420 macOS tests; its repair is checkpointed as `027dec1` and
published locally at 0.4.0. No targets, source fields, configuration attributes or
tests were removed. No remote publication or CI was used.
