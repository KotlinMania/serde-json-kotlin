package io.github.kotlinmania.serdejson.lexical

// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Pre-computed small powers.

// 32 BIT
// Rust cfg item excluded by explicit configuration; original retained in input.rs.
// Rust cfg item excluded by explicit configuration; original retained in input.rs.

// 64 BIT
internal val `POW5_64`: SerdeJsonSmallPowersArray0 get() = SerdeJsonSmallPowersArray0(1uL, 5uL, 25uL, 125uL, 625uL, 3125uL, 15625uL, 78125uL, 390625uL, 1953125uL, 9765625uL, 48828125uL, 244140625uL, 1220703125uL, 6103515625uL, 30517578125uL, 152587890625uL, 762939453125uL, 3814697265625uL, 19073486328125uL, 95367431640625uL, 476837158203125uL, 2384185791015625uL, 11920928955078125uL, 59604644775390625uL, 298023223876953125uL, 1490116119384765625uL, 7450580596923828125uL)

internal val `POW10_64`: SerdeJsonSmallPowersArray1 get() = SerdeJsonSmallPowersArray1(1uL, 10uL, 100uL, 1000uL, 10000uL, 100000uL, 1000000uL, 10000000uL, 100000000uL, 1000000000uL, 10000000000uL, 100000000000uL, 1000000000000uL, 10000000000000uL, 100000000000000uL, 1000000000000000uL, 10000000000000000uL, 100000000000000000uL, 1000000000000000000uL, 10000000000000000000uL)

// Rust fixed array with 28 Copy elements; length is part of the type.
class SerdeJsonSmallPowersArray0(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong, element10: kotlin.ULong, element11: kotlin.ULong, element12: kotlin.ULong, element13: kotlin.ULong, element14: kotlin.ULong, element15: kotlin.ULong, element16: kotlin.ULong, element17: kotlin.ULong, element18: kotlin.ULong, element19: kotlin.ULong, element20: kotlin.ULong, element21: kotlin.ULong, element22: kotlin.ULong, element23: kotlin.ULong, element24: kotlin.ULong, element25: kotlin.ULong, element26: kotlin.ULong, element27: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    private val stored3: kotlin.ULong = element3
    val element3: kotlin.ULong get() = stored3
    private val stored4: kotlin.ULong = element4
    val element4: kotlin.ULong get() = stored4
    private val stored5: kotlin.ULong = element5
    val element5: kotlin.ULong get() = stored5
    private val stored6: kotlin.ULong = element6
    val element6: kotlin.ULong get() = stored6
    private val stored7: kotlin.ULong = element7
    val element7: kotlin.ULong get() = stored7
    private val stored8: kotlin.ULong = element8
    val element8: kotlin.ULong get() = stored8
    private val stored9: kotlin.ULong = element9
    val element9: kotlin.ULong get() = stored9
    private val stored10: kotlin.ULong = element10
    val element10: kotlin.ULong get() = stored10
    private val stored11: kotlin.ULong = element11
    val element11: kotlin.ULong get() = stored11
    private val stored12: kotlin.ULong = element12
    val element12: kotlin.ULong get() = stored12
    private val stored13: kotlin.ULong = element13
    val element13: kotlin.ULong get() = stored13
    private val stored14: kotlin.ULong = element14
    val element14: kotlin.ULong get() = stored14
    private val stored15: kotlin.ULong = element15
    val element15: kotlin.ULong get() = stored15
    private val stored16: kotlin.ULong = element16
    val element16: kotlin.ULong get() = stored16
    private val stored17: kotlin.ULong = element17
    val element17: kotlin.ULong get() = stored17
    private val stored18: kotlin.ULong = element18
    val element18: kotlin.ULong get() = stored18
    private val stored19: kotlin.ULong = element19
    val element19: kotlin.ULong get() = stored19
    private val stored20: kotlin.ULong = element20
    val element20: kotlin.ULong get() = stored20
    private val stored21: kotlin.ULong = element21
    val element21: kotlin.ULong get() = stored21
    private val stored22: kotlin.ULong = element22
    val element22: kotlin.ULong get() = stored22
    private val stored23: kotlin.ULong = element23
    val element23: kotlin.ULong get() = stored23
    private val stored24: kotlin.ULong = element24
    val element24: kotlin.ULong get() = stored24
    private val stored25: kotlin.ULong = element25
    val element25: kotlin.ULong get() = stored25
    private val stored26: kotlin.ULong = element26
    val element26: kotlin.ULong get() = stored26
    private val stored27: kotlin.ULong = element27
    val element27: kotlin.ULong get() = stored27
    fun copyValue(): SerdeJsonSmallPowersArray0 = SerdeJsonSmallPowersArray0(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27)
}
// Rust fixed array with 20 Copy elements; length is part of the type.
class SerdeJsonSmallPowersArray1(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong, element10: kotlin.ULong, element11: kotlin.ULong, element12: kotlin.ULong, element13: kotlin.ULong, element14: kotlin.ULong, element15: kotlin.ULong, element16: kotlin.ULong, element17: kotlin.ULong, element18: kotlin.ULong, element19: kotlin.ULong) {
    private val stored0: kotlin.ULong = element0
    val element0: kotlin.ULong get() = stored0
    private val stored1: kotlin.ULong = element1
    val element1: kotlin.ULong get() = stored1
    private val stored2: kotlin.ULong = element2
    val element2: kotlin.ULong get() = stored2
    private val stored3: kotlin.ULong = element3
    val element3: kotlin.ULong get() = stored3
    private val stored4: kotlin.ULong = element4
    val element4: kotlin.ULong get() = stored4
    private val stored5: kotlin.ULong = element5
    val element5: kotlin.ULong get() = stored5
    private val stored6: kotlin.ULong = element6
    val element6: kotlin.ULong get() = stored6
    private val stored7: kotlin.ULong = element7
    val element7: kotlin.ULong get() = stored7
    private val stored8: kotlin.ULong = element8
    val element8: kotlin.ULong get() = stored8
    private val stored9: kotlin.ULong = element9
    val element9: kotlin.ULong get() = stored9
    private val stored10: kotlin.ULong = element10
    val element10: kotlin.ULong get() = stored10
    private val stored11: kotlin.ULong = element11
    val element11: kotlin.ULong get() = stored11
    private val stored12: kotlin.ULong = element12
    val element12: kotlin.ULong get() = stored12
    private val stored13: kotlin.ULong = element13
    val element13: kotlin.ULong get() = stored13
    private val stored14: kotlin.ULong = element14
    val element14: kotlin.ULong get() = stored14
    private val stored15: kotlin.ULong = element15
    val element15: kotlin.ULong get() = stored15
    private val stored16: kotlin.ULong = element16
    val element16: kotlin.ULong get() = stored16
    private val stored17: kotlin.ULong = element17
    val element17: kotlin.ULong get() = stored17
    private val stored18: kotlin.ULong = element18
    val element18: kotlin.ULong get() = stored18
    private val stored19: kotlin.ULong = element19
    val element19: kotlin.ULong get() = stored19
    fun copyValue(): SerdeJsonSmallPowersArray1 = SerdeJsonSmallPowersArray1(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19)
}
