# JSON map UTF-8 ordering parity

This bounded repair starts from clean JSON `4cb85c6`, preserving its pointer
fix and the separate clean Rand `46e4736`. Graph review found 21 distinct JSON
consumers. The neighboring IndexMap checkout has concurrent dirty work; it was
read-only and none of those changes were staged. No pre-existing JSON dirty
files were included. Existing source, 159 original tests and the seven pointer
regressions are prior work, not new coverage attributed to this repair.

## Source comparison

The bounded CLI translation of unchanged serde_json 1.0.149 `src/map.rs`
completed with exit 0. `build/rust-kotlin-map-sort/MapReview.kt:300–301` retains
upstream's `sort_unstable_keys` call. This is a manual consumer representation
adaptation, not an installed generated file or generator fix. No mod.rs guard
was bypassed. Upstream map.rs documents Rust str order and delegates sorting to
IndexMap when preserve_order is enabled. Kotlin's existing JsonMap comparator
used String.compareTo, which compares UTF-16 code units instead of UTF-8 bytes.
For example, Rust puts U+E000 before U+10000; Kotlin put U+10000 first.

JsonMap now encodes each key once using strict UTF-8, then supplies unsigned
lexicographic byte comparison to its existing canonical IndexMap.sortKeys API.
Prefix-equal keys compare by byte length. This exactly follows Rust String's
ordering for valid Unicode strings, with no normalization, collation table,
Unicode-version approximation, duplicate shared type or alternate map. The cache
adds memory proportional to total encoded key bytes; it avoids encoding keys
again for each comparison. This does not claim equivalent allocations or sort
implementation internals across runtimes.

Strict encoding rejects an unpaired UTF-16 surrogate before sorting starts,
rather than silently substituting another character. Rust String cannot contain
that input. This Kotlin representation guard is tested separately and is not
presented as an eighth Rust oracle behavior or a new Rust error contract.

## Actual red/green evidence

Run from the repository root:

```sh
cargo test --offline --manifest-path verification/map-sort-oracle/Cargo.toml --target-dir build/rust-kotlin-map-sort/rust-target
./gradlew jvmTest macosArm64Test detekt ktlintCheck --continue --console=plain
```

The Rust fixture uses unchanged local serde_json with preserve_order enabled,
matching Kotlin's existing insertion-ordered representation and exercising a
real sort operation. Offline Cargo compiled cached Rust IndexMap 2.13.0; the
full resolved versions and lock hash are in receipt.json. This feature is set
only in the standalone Rust oracle, not project configuration. The generated
oracle lock remains ignored under existing repository policy; project pins
are unchanged.

All seven actual Rust cases passed. They check ASCII/prefix order, U+E000 versus
U+10000, U+FFFF versus U+10FFFF, composed/decomposed normalization controls,
supplementary prefixes, recursive objects inside arrays, preserved values and
repeat sorting. The Kotlin suite mirrors these seven and adds one strict
malformed-UTF16/no-reordering check. Before repair, five valid-order tests and
the Kotlin-only guard failed; ASCII and normalization controls passed. JUnit's
red failure XML renders U+FFFF as '?'; the hashed Kotlin fixture uses \uFFFF and
the Rust fixture \u{ffff}. No expectation was changed to the display character.
Pre-patch Native was not run.

Final-source suites pass 174 JVM and 174 macOS ARM tests, including all eight new
tests and all seven preserved pointer tests. Detekt and every configured ktlint
Check report have zero findings, and the combined Gradle invocation succeeded.
All seven actual Rust oracle tests pass. Existing assertions were not changed.
Raw logs, final-source hashes and attribution are in build/rust-kotlin-map-sort/;
receipt.json preserves exact commands, red evidence, suite timestamps and hashes.

## Limits

Public signatures and lookup semantics are unchanged, so no caller migration
was required. Value.sortAllObjects uses the repaired map comparator; recursive
behavior is tested. The IndexMap dependency, project pins and pointer parser
are unchanged. Other platforms, downstream compilation, fresh publication
metadata, exclusive-borrow enforcement, stable/unstable implementation details
and allocation equivalence are unverified. Existing Kotlin-versus-Rust malformed
string representation differences outside sorting are not resolved. No remote
CI, push, PR, merge, tag, release, publication or settings changes were performed.
