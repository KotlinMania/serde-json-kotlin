// port-lint: source lexical/small_powers.rs
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

package io.github.kotlinmania.serdejson.lexical

internal val POW5_32: UIntArray =
    uintArrayOf(
        1u,
        5u,
        25u,
        125u,
        625u,
        3125u,
        15625u,
        78125u,
        390625u,
        1953125u,
        9765625u,
        48828125u,
        244140625u,
        1220703125u,
    )

internal val POW10_32: UIntArray =
    uintArrayOf(
        1u,
        10u,
        100u,
        1000u,
        10000u,
        100000u,
        1000000u,
        10000000u,
        100000000u,
        1000000000u,
    )

internal val POW5_64: ULongArray =
    ulongArrayOf(
        1uL,
        5uL,
        25uL,
        125uL,
        625uL,
        3125uL,
        15625uL,
        78125uL,
        390625uL,
        1953125uL,
        9765625uL,
        48828125uL,
        244140625uL,
        1220703125uL,
        6103515625uL,
        30517578125uL,
        152587890625uL,
        762939453125uL,
        3814697265625uL,
        19073486328125uL,
        95367431640625uL,
        476837158203125uL,
        2384185791015625uL,
        11920928955078125uL,
        59604644775390625uL,
        298023223876953125uL,
        1490116119384765625uL,
        7450580596923828125uL,
    )

internal val POW10_64: ULongArray =
    ulongArrayOf(
        1uL,
        10uL,
        100uL,
        1000uL,
        10000uL,
        100000uL,
        1000000uL,
        10000000uL,
        100000000uL,
        1000000000uL,
        10000000000uL,
        100000000000uL,
        1000000000000uL,
        10000000000000uL,
        100000000000000uL,
        1000000000000000uL,
        10000000000000000uL,
        100000000000000000uL,
        1000000000000000000uL,
        10000000000000000000uL,
    )
