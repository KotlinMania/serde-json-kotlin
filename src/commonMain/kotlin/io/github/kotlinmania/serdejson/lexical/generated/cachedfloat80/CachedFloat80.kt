package io.github.kotlinmania.serdejson.lexical.generated.cachedfloat80

// Generated from Syn AST; exact original retained in input.rs.
// Adapted from https://github.com/Alexhuszagh/rust-lexical.

//! Cached exponents for basen values with 80-bit extended floats.
//!
//! Exact versions of base**n as an extended-precision float, with both
//! large and small powers. Use the large powers to minimize the amount
//! of compounded error.
//!
//! These values were calculated using Python, using the arbitrary-precision
//! integer to calculate exact extended-representation of each value.
//! These values are all normalized.
// Read-only constant-data contract crate::lexical::cached::ExtendedFloatArray
// Source /Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/cached.rs:12; methods/traits are not translated here.


// POWERS

/// Precalculated powers that uses two-separate arrays for memory-efficiency.
internal class `ExtendedFloatArray` internal constructor(
    
        // Pre-calculated mantissa for the powers.
        val `mant`: kotlin.collections.List<kotlin.ULong>,
    
        // Pre-calculated binary exponents for the powers.
        val `exp`: kotlin.collections.List<kotlin.Int>,
)

// Read-only constant-data contract crate::lexical::cached::ModeratePathPowers
// Source /Volumes/stuff/Projects/kotlinmania/serde-json-kotlin/tmp/serde_json/src/lexical/cached.rs:38; methods/traits are not translated here.


// MODERATE PATH POWERS

/// Precalculated powers of base N for the moderate path.
internal class `ModeratePathPowers` internal constructor(
    
        // Pre-calculated small powers.
        val `small`: `ExtendedFloatArray`,
    
        // Pre-calculated large powers.
        val `large`: `ExtendedFloatArray`,
    /// Pre-calculated small powers as 64-bit integers
    val `small_int`: kotlin.collections.List<kotlin.ULong>,
    
        // Step between large powers and number of small powers.
        val `step`: kotlin.Int,
    
        // Exponent bias for the large powers.
        val `bias`: kotlin.Int,
)

// Rust use resolved through explicit library bindings.

// LOW-LEVEL
// ---------

// BASE10

private val `BASE10_SMALL_MANTISSA`: __rtkArray0 get() = __rtkArray0(9223372036854775808uL,  // 10^0
    
11529215046068469760uL, // 10^1
    
14411518807585587200uL, // 10^2
    
18014398509481984000uL, // 10^3
    
11258999068426240000uL, // 10^4
    
14073748835532800000uL, // 10^5
    
17592186044416000000uL, // 10^6
    
10995116277760000000uL, // 10^7
    
13743895347200000000uL, // 10^8
    
17179869184000000000uL, // 10^9

)

private val `BASE10_SMALL_EXPONENT`: __rtkArray1 get() = __rtkArray1((-63), // 10^0
    
(-60), // 10^1
    
(-57), // 10^2
    
(-54), // 10^3
    
(-50), // 10^4
    
(-47), // 10^5
    
(-44), // 10^6
    
(-40), // 10^7
    
(-37), // 10^8
    
(-34), // 10^9

)

private val `BASE10_LARGE_MANTISSA`: __rtkArray2 get() = __rtkArray2(11555125961253852697uL, // 10^-350
    
13451937075301367670uL, // 10^-340
    
15660115838168849784uL, // 10^-330
    
18230774251475056848uL, // 10^-320
    
10611707258198326947uL, // 10^-310
    
12353653155963782858uL, // 10^-300
    
14381545078898527261uL, // 10^-290
    
16742321987285426889uL, // 10^-280
    
9745314011399999080uL,  // 10^-270
    
11345038669416679861uL, // 10^-260
    
13207363278391631158uL, // 10^-250
    
15375394465392026070uL, // 10^-240
    
17899314949046850752uL, // 10^-230
    
10418772551374772303uL, // 10^-220
    
12129047596099288555uL, // 10^-210
    
14120069793541087484uL, // 10^-200
    
16437924692338667210uL, // 10^-190
    
9568131466127621947uL,  // 10^-180
    
11138771039116687545uL, // 10^-170
    
12967236152753102995uL, // 10^-160
    
15095849699286165408uL, // 10^-150
    
17573882009934360870uL, // 10^-140
    
10229345649675443343uL, // 10^-130
    
11908525658859223294uL, // 10^-120
    
13863348470604074297uL, // 10^-110
    
16139061738043178685uL, // 10^-100
    
9394170331095332911uL,  // 10^-90
    
10936253623915059621uL, // 10^-80
    
12731474852090538039uL, // 10^-70
    
14821387422376473014uL, // 10^-60
    
17254365866976409468uL, // 10^-50
    
10043362776618689222uL, // 10^-40
    
11692013098647223345uL, // 10^-30
    
13611294676837538538uL, // 10^-20
    
15845632502852867518uL, // 10^-10
    
9223372036854775808uL,  // 10^0
    
10737418240000000000uL, // 10^10
    
12500000000000000000uL, // 10^20
    
14551915228366851806uL, // 10^30
    
16940658945086006781uL, // 10^40
    
9860761315262647567uL,  // 10^50
    
11479437019748901445uL, // 10^60
    
13363823550460978230uL, // 10^70
    
15557538194652854267uL, // 10^80
    
18111358157653424735uL, // 10^90
    
10542197943230523224uL, // 10^100
    
12272733663244316382uL, // 10^110
    
14287342391028437277uL, // 10^120
    
16632655625031838749uL, // 10^130
    
9681479787123295682uL,  // 10^140
    
11270725851789228247uL, // 10^150
    
13120851772591970218uL, // 10^160
    
15274681817498023410uL, // 10^170
    
17782069995880619867uL, // 10^180
    
10350527006597618960uL, // 10^190
    
12049599325514420588uL, // 10^200
    
14027579833653779454uL, // 10^210
    
16330252207878254650uL, // 10^220
    
9505457831475799117uL,  // 10^230
    
11065809325636130661uL, // 10^240
    
12882297539194266616uL, // 10^250
    
14996968138956309548uL, // 10^260
    
17458768723248864463uL, // 10^270
    
10162340898095201970uL, // 10^280
    
11830521861667747109uL, // 10^290
    
13772540099066387756uL, // 10^300

)

private val `BASE10_LARGE_EXPONENT`: __rtkArray3 get() = __rtkArray3((-1226), // 10^-350
    
(-1193), // 10^-340
    
(-1160), // 10^-330
    
(-1127), // 10^-320
    
(-1093), // 10^-310
    
(-1060), // 10^-300
    
(-1027), // 10^-290
    
(-994),  // 10^-280
    
(-960),  // 10^-270
    
(-927),  // 10^-260
    
(-894),  // 10^-250
    
(-861),  // 10^-240
    
(-828),  // 10^-230
    
(-794),  // 10^-220
    
(-761),  // 10^-210
    
(-728),  // 10^-200
    
(-695),  // 10^-190
    
(-661),  // 10^-180
    
(-628),  // 10^-170
    
(-595),  // 10^-160
    
(-562),  // 10^-150
    
(-529),  // 10^-140
    
(-495),  // 10^-130
    
(-462),  // 10^-120
    
(-429),  // 10^-110
    
(-396),  // 10^-100
    
(-362),  // 10^-90
    
(-329),  // 10^-80
    
(-296),  // 10^-70
    
(-263),  // 10^-60
    
(-230),  // 10^-50
    
(-196),  // 10^-40
    
(-163),  // 10^-30
    
(-130),  // 10^-20
    
(-97),   // 10^-10
    
(-63),   // 10^0
    
(-30),   // 10^10
    
3,     // 10^20
    
36,    // 10^30
    
69,    // 10^40
    
103,   // 10^50
    
136,   // 10^60
    
169,   // 10^70
    
202,   // 10^80
    
235,   // 10^90
    
269,   // 10^100
    
302,   // 10^110
    
335,   // 10^120
    
368,   // 10^130
    
402,   // 10^140
    
435,   // 10^150
    
468,   // 10^160
    
501,   // 10^170
    
534,   // 10^180
    
568,   // 10^190
    
601,   // 10^200
    
634,   // 10^210
    
667,   // 10^220
    
701,   // 10^230
    
734,   // 10^240
    
767,   // 10^250
    
800,   // 10^260
    
833,   // 10^270
    
867,   // 10^280
    
900,   // 10^290
    
933,   // 10^300

)

private val `BASE10_SMALL_INT_POWERS`: __rtkArray0 get() = __rtkArray0(1uL, 10uL, 100uL, 1000uL, 10000uL, 100000uL, 1000000uL, 10000000uL, 100000000uL, 1000000000uL)

private val `BASE10_STEP`: kotlin.Int get() = 10

private val `BASE10_BIAS`: kotlin.Int get() = 350


// HIGH LEVEL
// ----------

private val `BASE10_POWERS`: ModeratePathPowers get() = kotlin.run {
    val __rtkValue2: ExtendedFloatArray = kotlin.run {
        val __rtkValue0: kotlin.collections.List<kotlin.ULong> = (`BASE10_SMALL_MANTISSA`).asSlice()
        val __rtkValue1: kotlin.collections.List<kotlin.Int> = (`BASE10_SMALL_EXPONENT`).asSlice()
        `ExtendedFloatArray`(__rtkValue0, __rtkValue1)
    }
    val __rtkValue5: ExtendedFloatArray = kotlin.run {
        val __rtkValue3: kotlin.collections.List<kotlin.ULong> = (`BASE10_LARGE_MANTISSA`).asSlice()
        val __rtkValue4: kotlin.collections.List<kotlin.Int> = (`BASE10_LARGE_EXPONENT`).asSlice()
        `ExtendedFloatArray`(__rtkValue3, __rtkValue4)
    }
    val __rtkValue6: kotlin.collections.List<kotlin.ULong> = (`BASE10_SMALL_INT_POWERS`).asSlice()
    val __rtkValue7: kotlin.Int = `BASE10_STEP`
    val __rtkValue8: kotlin.Int = `BASE10_BIAS`
    `ModeratePathPowers`(__rtkValue2, __rtkValue5, __rtkValue6, __rtkValue7, __rtkValue8)
}

/// Get powers from base.
internal fun `get_powers`(): ModeratePathPowers = `BASE10_POWERS`

// Rust fixed array with 10 Copy elements; length is part of the type.
class __rtkArray0(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong) {
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
    fun copyValue(): __rtkArray0 = __rtkArray0(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9)
}
// Rust fixed array with 10 Copy elements; length is part of the type.
class __rtkArray1(element0: kotlin.Int, element1: kotlin.Int, element2: kotlin.Int, element3: kotlin.Int, element4: kotlin.Int, element5: kotlin.Int, element6: kotlin.Int, element7: kotlin.Int, element8: kotlin.Int, element9: kotlin.Int) {
    private val stored0: kotlin.Int = element0
    val element0: kotlin.Int get() = stored0
    private val stored1: kotlin.Int = element1
    val element1: kotlin.Int get() = stored1
    private val stored2: kotlin.Int = element2
    val element2: kotlin.Int get() = stored2
    private val stored3: kotlin.Int = element3
    val element3: kotlin.Int get() = stored3
    private val stored4: kotlin.Int = element4
    val element4: kotlin.Int get() = stored4
    private val stored5: kotlin.Int = element5
    val element5: kotlin.Int get() = stored5
    private val stored6: kotlin.Int = element6
    val element6: kotlin.Int get() = stored6
    private val stored7: kotlin.Int = element7
    val element7: kotlin.Int get() = stored7
    private val stored8: kotlin.Int = element8
    val element8: kotlin.Int get() = stored8
    private val stored9: kotlin.Int = element9
    val element9: kotlin.Int get() = stored9
    fun copyValue(): __rtkArray1 = __rtkArray1(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9)
    fun asSlice(): kotlin.collections.List<kotlin.Int> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9)
}
// Rust fixed array with 66 Copy elements; length is part of the type.
class __rtkArray2(element0: kotlin.ULong, element1: kotlin.ULong, element2: kotlin.ULong, element3: kotlin.ULong, element4: kotlin.ULong, element5: kotlin.ULong, element6: kotlin.ULong, element7: kotlin.ULong, element8: kotlin.ULong, element9: kotlin.ULong, element10: kotlin.ULong, element11: kotlin.ULong, element12: kotlin.ULong, element13: kotlin.ULong, element14: kotlin.ULong, element15: kotlin.ULong, element16: kotlin.ULong, element17: kotlin.ULong, element18: kotlin.ULong, element19: kotlin.ULong, element20: kotlin.ULong, element21: kotlin.ULong, element22: kotlin.ULong, element23: kotlin.ULong, element24: kotlin.ULong, element25: kotlin.ULong, element26: kotlin.ULong, element27: kotlin.ULong, element28: kotlin.ULong, element29: kotlin.ULong, element30: kotlin.ULong, element31: kotlin.ULong, element32: kotlin.ULong, element33: kotlin.ULong, element34: kotlin.ULong, element35: kotlin.ULong, element36: kotlin.ULong, element37: kotlin.ULong, element38: kotlin.ULong, element39: kotlin.ULong, element40: kotlin.ULong, element41: kotlin.ULong, element42: kotlin.ULong, element43: kotlin.ULong, element44: kotlin.ULong, element45: kotlin.ULong, element46: kotlin.ULong, element47: kotlin.ULong, element48: kotlin.ULong, element49: kotlin.ULong, element50: kotlin.ULong, element51: kotlin.ULong, element52: kotlin.ULong, element53: kotlin.ULong, element54: kotlin.ULong, element55: kotlin.ULong, element56: kotlin.ULong, element57: kotlin.ULong, element58: kotlin.ULong, element59: kotlin.ULong, element60: kotlin.ULong, element61: kotlin.ULong, element62: kotlin.ULong, element63: kotlin.ULong, element64: kotlin.ULong, element65: kotlin.ULong) {
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
    private val stored28: kotlin.ULong = element28
    val element28: kotlin.ULong get() = stored28
    private val stored29: kotlin.ULong = element29
    val element29: kotlin.ULong get() = stored29
    private val stored30: kotlin.ULong = element30
    val element30: kotlin.ULong get() = stored30
    private val stored31: kotlin.ULong = element31
    val element31: kotlin.ULong get() = stored31
    private val stored32: kotlin.ULong = element32
    val element32: kotlin.ULong get() = stored32
    private val stored33: kotlin.ULong = element33
    val element33: kotlin.ULong get() = stored33
    private val stored34: kotlin.ULong = element34
    val element34: kotlin.ULong get() = stored34
    private val stored35: kotlin.ULong = element35
    val element35: kotlin.ULong get() = stored35
    private val stored36: kotlin.ULong = element36
    val element36: kotlin.ULong get() = stored36
    private val stored37: kotlin.ULong = element37
    val element37: kotlin.ULong get() = stored37
    private val stored38: kotlin.ULong = element38
    val element38: kotlin.ULong get() = stored38
    private val stored39: kotlin.ULong = element39
    val element39: kotlin.ULong get() = stored39
    private val stored40: kotlin.ULong = element40
    val element40: kotlin.ULong get() = stored40
    private val stored41: kotlin.ULong = element41
    val element41: kotlin.ULong get() = stored41
    private val stored42: kotlin.ULong = element42
    val element42: kotlin.ULong get() = stored42
    private val stored43: kotlin.ULong = element43
    val element43: kotlin.ULong get() = stored43
    private val stored44: kotlin.ULong = element44
    val element44: kotlin.ULong get() = stored44
    private val stored45: kotlin.ULong = element45
    val element45: kotlin.ULong get() = stored45
    private val stored46: kotlin.ULong = element46
    val element46: kotlin.ULong get() = stored46
    private val stored47: kotlin.ULong = element47
    val element47: kotlin.ULong get() = stored47
    private val stored48: kotlin.ULong = element48
    val element48: kotlin.ULong get() = stored48
    private val stored49: kotlin.ULong = element49
    val element49: kotlin.ULong get() = stored49
    private val stored50: kotlin.ULong = element50
    val element50: kotlin.ULong get() = stored50
    private val stored51: kotlin.ULong = element51
    val element51: kotlin.ULong get() = stored51
    private val stored52: kotlin.ULong = element52
    val element52: kotlin.ULong get() = stored52
    private val stored53: kotlin.ULong = element53
    val element53: kotlin.ULong get() = stored53
    private val stored54: kotlin.ULong = element54
    val element54: kotlin.ULong get() = stored54
    private val stored55: kotlin.ULong = element55
    val element55: kotlin.ULong get() = stored55
    private val stored56: kotlin.ULong = element56
    val element56: kotlin.ULong get() = stored56
    private val stored57: kotlin.ULong = element57
    val element57: kotlin.ULong get() = stored57
    private val stored58: kotlin.ULong = element58
    val element58: kotlin.ULong get() = stored58
    private val stored59: kotlin.ULong = element59
    val element59: kotlin.ULong get() = stored59
    private val stored60: kotlin.ULong = element60
    val element60: kotlin.ULong get() = stored60
    private val stored61: kotlin.ULong = element61
    val element61: kotlin.ULong get() = stored61
    private val stored62: kotlin.ULong = element62
    val element62: kotlin.ULong get() = stored62
    private val stored63: kotlin.ULong = element63
    val element63: kotlin.ULong get() = stored63
    private val stored64: kotlin.ULong = element64
    val element64: kotlin.ULong get() = stored64
    private val stored65: kotlin.ULong = element65
    val element65: kotlin.ULong get() = stored65
    fun copyValue(): __rtkArray2 = __rtkArray2(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37, stored38, stored39, stored40, stored41, stored42, stored43, stored44, stored45, stored46, stored47, stored48, stored49, stored50, stored51, stored52, stored53, stored54, stored55, stored56, stored57, stored58, stored59, stored60, stored61, stored62, stored63, stored64, stored65)
    fun asSlice(): kotlin.collections.List<kotlin.ULong> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37, stored38, stored39, stored40, stored41, stored42, stored43, stored44, stored45, stored46, stored47, stored48, stored49, stored50, stored51, stored52, stored53, stored54, stored55, stored56, stored57, stored58, stored59, stored60, stored61, stored62, stored63, stored64, stored65)
}
// Rust fixed array with 66 Copy elements; length is part of the type.
class __rtkArray3(element0: kotlin.Int, element1: kotlin.Int, element2: kotlin.Int, element3: kotlin.Int, element4: kotlin.Int, element5: kotlin.Int, element6: kotlin.Int, element7: kotlin.Int, element8: kotlin.Int, element9: kotlin.Int, element10: kotlin.Int, element11: kotlin.Int, element12: kotlin.Int, element13: kotlin.Int, element14: kotlin.Int, element15: kotlin.Int, element16: kotlin.Int, element17: kotlin.Int, element18: kotlin.Int, element19: kotlin.Int, element20: kotlin.Int, element21: kotlin.Int, element22: kotlin.Int, element23: kotlin.Int, element24: kotlin.Int, element25: kotlin.Int, element26: kotlin.Int, element27: kotlin.Int, element28: kotlin.Int, element29: kotlin.Int, element30: kotlin.Int, element31: kotlin.Int, element32: kotlin.Int, element33: kotlin.Int, element34: kotlin.Int, element35: kotlin.Int, element36: kotlin.Int, element37: kotlin.Int, element38: kotlin.Int, element39: kotlin.Int, element40: kotlin.Int, element41: kotlin.Int, element42: kotlin.Int, element43: kotlin.Int, element44: kotlin.Int, element45: kotlin.Int, element46: kotlin.Int, element47: kotlin.Int, element48: kotlin.Int, element49: kotlin.Int, element50: kotlin.Int, element51: kotlin.Int, element52: kotlin.Int, element53: kotlin.Int, element54: kotlin.Int, element55: kotlin.Int, element56: kotlin.Int, element57: kotlin.Int, element58: kotlin.Int, element59: kotlin.Int, element60: kotlin.Int, element61: kotlin.Int, element62: kotlin.Int, element63: kotlin.Int, element64: kotlin.Int, element65: kotlin.Int) {
    private val stored0: kotlin.Int = element0
    val element0: kotlin.Int get() = stored0
    private val stored1: kotlin.Int = element1
    val element1: kotlin.Int get() = stored1
    private val stored2: kotlin.Int = element2
    val element2: kotlin.Int get() = stored2
    private val stored3: kotlin.Int = element3
    val element3: kotlin.Int get() = stored3
    private val stored4: kotlin.Int = element4
    val element4: kotlin.Int get() = stored4
    private val stored5: kotlin.Int = element5
    val element5: kotlin.Int get() = stored5
    private val stored6: kotlin.Int = element6
    val element6: kotlin.Int get() = stored6
    private val stored7: kotlin.Int = element7
    val element7: kotlin.Int get() = stored7
    private val stored8: kotlin.Int = element8
    val element8: kotlin.Int get() = stored8
    private val stored9: kotlin.Int = element9
    val element9: kotlin.Int get() = stored9
    private val stored10: kotlin.Int = element10
    val element10: kotlin.Int get() = stored10
    private val stored11: kotlin.Int = element11
    val element11: kotlin.Int get() = stored11
    private val stored12: kotlin.Int = element12
    val element12: kotlin.Int get() = stored12
    private val stored13: kotlin.Int = element13
    val element13: kotlin.Int get() = stored13
    private val stored14: kotlin.Int = element14
    val element14: kotlin.Int get() = stored14
    private val stored15: kotlin.Int = element15
    val element15: kotlin.Int get() = stored15
    private val stored16: kotlin.Int = element16
    val element16: kotlin.Int get() = stored16
    private val stored17: kotlin.Int = element17
    val element17: kotlin.Int get() = stored17
    private val stored18: kotlin.Int = element18
    val element18: kotlin.Int get() = stored18
    private val stored19: kotlin.Int = element19
    val element19: kotlin.Int get() = stored19
    private val stored20: kotlin.Int = element20
    val element20: kotlin.Int get() = stored20
    private val stored21: kotlin.Int = element21
    val element21: kotlin.Int get() = stored21
    private val stored22: kotlin.Int = element22
    val element22: kotlin.Int get() = stored22
    private val stored23: kotlin.Int = element23
    val element23: kotlin.Int get() = stored23
    private val stored24: kotlin.Int = element24
    val element24: kotlin.Int get() = stored24
    private val stored25: kotlin.Int = element25
    val element25: kotlin.Int get() = stored25
    private val stored26: kotlin.Int = element26
    val element26: kotlin.Int get() = stored26
    private val stored27: kotlin.Int = element27
    val element27: kotlin.Int get() = stored27
    private val stored28: kotlin.Int = element28
    val element28: kotlin.Int get() = stored28
    private val stored29: kotlin.Int = element29
    val element29: kotlin.Int get() = stored29
    private val stored30: kotlin.Int = element30
    val element30: kotlin.Int get() = stored30
    private val stored31: kotlin.Int = element31
    val element31: kotlin.Int get() = stored31
    private val stored32: kotlin.Int = element32
    val element32: kotlin.Int get() = stored32
    private val stored33: kotlin.Int = element33
    val element33: kotlin.Int get() = stored33
    private val stored34: kotlin.Int = element34
    val element34: kotlin.Int get() = stored34
    private val stored35: kotlin.Int = element35
    val element35: kotlin.Int get() = stored35
    private val stored36: kotlin.Int = element36
    val element36: kotlin.Int get() = stored36
    private val stored37: kotlin.Int = element37
    val element37: kotlin.Int get() = stored37
    private val stored38: kotlin.Int = element38
    val element38: kotlin.Int get() = stored38
    private val stored39: kotlin.Int = element39
    val element39: kotlin.Int get() = stored39
    private val stored40: kotlin.Int = element40
    val element40: kotlin.Int get() = stored40
    private val stored41: kotlin.Int = element41
    val element41: kotlin.Int get() = stored41
    private val stored42: kotlin.Int = element42
    val element42: kotlin.Int get() = stored42
    private val stored43: kotlin.Int = element43
    val element43: kotlin.Int get() = stored43
    private val stored44: kotlin.Int = element44
    val element44: kotlin.Int get() = stored44
    private val stored45: kotlin.Int = element45
    val element45: kotlin.Int get() = stored45
    private val stored46: kotlin.Int = element46
    val element46: kotlin.Int get() = stored46
    private val stored47: kotlin.Int = element47
    val element47: kotlin.Int get() = stored47
    private val stored48: kotlin.Int = element48
    val element48: kotlin.Int get() = stored48
    private val stored49: kotlin.Int = element49
    val element49: kotlin.Int get() = stored49
    private val stored50: kotlin.Int = element50
    val element50: kotlin.Int get() = stored50
    private val stored51: kotlin.Int = element51
    val element51: kotlin.Int get() = stored51
    private val stored52: kotlin.Int = element52
    val element52: kotlin.Int get() = stored52
    private val stored53: kotlin.Int = element53
    val element53: kotlin.Int get() = stored53
    private val stored54: kotlin.Int = element54
    val element54: kotlin.Int get() = stored54
    private val stored55: kotlin.Int = element55
    val element55: kotlin.Int get() = stored55
    private val stored56: kotlin.Int = element56
    val element56: kotlin.Int get() = stored56
    private val stored57: kotlin.Int = element57
    val element57: kotlin.Int get() = stored57
    private val stored58: kotlin.Int = element58
    val element58: kotlin.Int get() = stored58
    private val stored59: kotlin.Int = element59
    val element59: kotlin.Int get() = stored59
    private val stored60: kotlin.Int = element60
    val element60: kotlin.Int get() = stored60
    private val stored61: kotlin.Int = element61
    val element61: kotlin.Int get() = stored61
    private val stored62: kotlin.Int = element62
    val element62: kotlin.Int get() = stored62
    private val stored63: kotlin.Int = element63
    val element63: kotlin.Int get() = stored63
    private val stored64: kotlin.Int = element64
    val element64: kotlin.Int get() = stored64
    private val stored65: kotlin.Int = element65
    val element65: kotlin.Int get() = stored65
    fun copyValue(): __rtkArray3 = __rtkArray3(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37, stored38, stored39, stored40, stored41, stored42, stored43, stored44, stored45, stored46, stored47, stored48, stored49, stored50, stored51, stored52, stored53, stored54, stored55, stored56, stored57, stored58, stored59, stored60, stored61, stored62, stored63, stored64, stored65)
    fun asSlice(): kotlin.collections.List<kotlin.Int> = kotlin.collections.listOf(stored0, stored1, stored2, stored3, stored4, stored5, stored6, stored7, stored8, stored9, stored10, stored11, stored12, stored13, stored14, stored15, stored16, stored17, stored18, stored19, stored20, stored21, stored22, stored23, stored24, stored25, stored26, stored27, stored28, stored29, stored30, stored31, stored32, stored33, stored34, stored35, stored36, stored37, stored38, stored39, stored40, stored41, stored42, stored43, stored44, stored45, stored46, stored47, stored48, stored49, stored50, stored51, stored52, stored53, stored54, stored55, stored56, stored57, stored58, stored59, stored60, stored61, stored62, stored63, stored64, stored65)
}
