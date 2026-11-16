# JSON Pointer unsigned index parity

This bounded repair starts from clean `e9c0f43`. Existing JSON source/checkpoints
and its 159 Kotlin tests are prior work, not authored by this step. Rand
`46e4736` is preserved separately. Dependency graph review found 21 distinct
JSON consumers. No concurrent dirty files were included.

## Source and translation boundary

Upstream serde_json 1.0.149 `src/value/mod.rs:259–264` rejects plus signs and
leading zeros, then parses usize. Kotlin Value.parseIndex instead uses signed
Int parsing. A bounded translation of this exact upstream file was attempted;
the CLI exited 2 at its intentional mod.rs guard. No ValueReview.kt was emitted,
no source was renamed/extracted to evade the guard, and no generator was changed.
The recorded comparison therefore uses exact unchanged upstream source and a
real Rust oracle, not claimed generated implementation output.

The first scratch command was denied before execution for scope. The identical
retry was accepted after presenting the user's foundational-library authorization.
The test-only setup and exact production guard operation also passed review.
No alternate route was used. Translation stderr remains in
`build/rust-kotlin-pointer-index/translation.log`.

## Actual red/green evidence

Run from the repository root:

```sh
cargo test --offline --manifest-path verification/pointer-index-oracle/Cargo.toml --target-dir build/rust-kotlin-pointer-index/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The oracle compiles unchanged local serde_json 1.0.149 with already cached Rust
dependencies. Seven actual Rust tests cover negative zero, Arabic/Devanagari and
fullwidth digits, canonical indices, arbitrary object keys, escapes/empty keys,
leading zeros, overflow and malformed tokens. All seven passed before the
production repair. The Kotlin red run then failed three test functions: /-0,
Arabic zero and fullwidth zero incorrectly selected element zero. Other controls
passed. Failed loops stop at their first token; no claim is made that every
listed token independently failed before repair. Pre-patch Native was not run.

The repair adds one ASCII-digit guard inside the private array-index parser,
then retains the existing Int conversion and plus/leading-zero checks. This
matches unsigned decimal syntax while preserving Kotlin container indexing.
Object lookup does not call this helper, so keys such as -0, Unicode digits,
01 and +0 remain unrestricted. Public signatures, pointer token unescaping,
existing tests and dependency pins are unchanged. Source search found the private
helper's only call in the array branch of Value.pointer; no caller migration
was needed.

Final-source suites pass 166 JVM and 166 macOS ARM tests, including all seven
new cases and all 159 existing tests. Detekt and all configured ktlint Check
reports have zero findings; the combined Gradle invocation succeeded. The Rust
oracle passes all seven tests. Old Format reports are excluded from attribution.
Raw logs, final-source hashes and static receipts are in
`build/rust-kotlin-pointer-index/`. Committed receipt.json preserves commands,
red observations, suite timestamps, source hashes and resolved Rust versions.
The generated oracle Cargo.lock remains ignored per existing repository policy;
its hash and actual versions are recorded. No project dependency pin was changed.

## Limits

Rust usize can represent larger indices than Kotlin Int. This helper is private
and addresses Int-sized Kotlin lists; oversized positive indices return no value,
as do out-of-bounds Rust slice accesses in the tested cases. This does not claim
full usize type parity, allocation at extreme sizes, invalid UTF-16/UTF-8 parity,
mutable pointer/take behavior, other targets or downstream compilation. There
is no RNG behavior in this parser repair. No publication, remote CI, push, PR,
merge, tag, provider/configuration change or unrelated edit was performed.
