package io.github.kotlinmania.serdejson.lexical

import kotlin.test.Test
import kotlin.test.assertEquals

class GeneratedCachedPowersTest {
    @Test
    fun integratedTablesMatchEveryRustValue() {
        val p = ExtendedFloatCache.getPowers()
        val actual =
            buildList<String> {
                for ((tag, values) in listOf("small-mant" to p.small.mant, "large-mant" to p.large.mant, "small-int" to p.smallInt)) {
                    add("length|$tag|${values.size}")
                    values.forEachIndexed { i, v -> add("$tag|$i|$v") }
                }
                for ((tag, values) in listOf("small-exp" to p.small.exp, "large-exp" to p.large.exp)) {
                    add("length|$tag|${values.size}")
                    values.forEachIndexed { i, v -> add("$tag|$i|$v") }
                }
                add("step|${p.step}")
                add("bias|${p.bias}")
            }
        // Complete unchanged Rust cached_float80.rs oracle; not old Kotlin values.
        val expected =
            """
            length|small-mant|10
            small-mant|0|9223372036854775808
            small-mant|1|11529215046068469760
            small-mant|2|14411518807585587200
            small-mant|3|18014398509481984000
            small-mant|4|11258999068426240000
            small-mant|5|14073748835532800000
            small-mant|6|17592186044416000000
            small-mant|7|10995116277760000000
            small-mant|8|13743895347200000000
            small-mant|9|17179869184000000000
            length|large-mant|66
            large-mant|0|11555125961253852697
            large-mant|1|13451937075301367670
            large-mant|2|15660115838168849784
            large-mant|3|18230774251475056848
            large-mant|4|10611707258198326947
            large-mant|5|12353653155963782858
            large-mant|6|14381545078898527261
            large-mant|7|16742321987285426889
            large-mant|8|9745314011399999080
            large-mant|9|11345038669416679861
            large-mant|10|13207363278391631158
            large-mant|11|15375394465392026070
            large-mant|12|17899314949046850752
            large-mant|13|10418772551374772303
            large-mant|14|12129047596099288555
            large-mant|15|14120069793541087484
            large-mant|16|16437924692338667210
            large-mant|17|9568131466127621947
            large-mant|18|11138771039116687545
            large-mant|19|12967236152753102995
            large-mant|20|15095849699286165408
            large-mant|21|17573882009934360870
            large-mant|22|10229345649675443343
            large-mant|23|11908525658859223294
            large-mant|24|13863348470604074297
            large-mant|25|16139061738043178685
            large-mant|26|9394170331095332911
            large-mant|27|10936253623915059621
            large-mant|28|12731474852090538039
            large-mant|29|14821387422376473014
            large-mant|30|17254365866976409468
            large-mant|31|10043362776618689222
            large-mant|32|11692013098647223345
            large-mant|33|13611294676837538538
            large-mant|34|15845632502852867518
            large-mant|35|9223372036854775808
            large-mant|36|10737418240000000000
            large-mant|37|12500000000000000000
            large-mant|38|14551915228366851806
            large-mant|39|16940658945086006781
            large-mant|40|9860761315262647567
            large-mant|41|11479437019748901445
            large-mant|42|13363823550460978230
            large-mant|43|15557538194652854267
            large-mant|44|18111358157653424735
            large-mant|45|10542197943230523224
            large-mant|46|12272733663244316382
            large-mant|47|14287342391028437277
            large-mant|48|16632655625031838749
            large-mant|49|9681479787123295682
            large-mant|50|11270725851789228247
            large-mant|51|13120851772591970218
            large-mant|52|15274681817498023410
            large-mant|53|17782069995880619867
            large-mant|54|10350527006597618960
            large-mant|55|12049599325514420588
            large-mant|56|14027579833653779454
            large-mant|57|16330252207878254650
            large-mant|58|9505457831475799117
            large-mant|59|11065809325636130661
            large-mant|60|12882297539194266616
            large-mant|61|14996968138956309548
            large-mant|62|17458768723248864463
            large-mant|63|10162340898095201970
            large-mant|64|11830521861667747109
            large-mant|65|13772540099066387756
            length|small-int|10
            small-int|0|1
            small-int|1|10
            small-int|2|100
            small-int|3|1000
            small-int|4|10000
            small-int|5|100000
            small-int|6|1000000
            small-int|7|10000000
            small-int|8|100000000
            small-int|9|1000000000
            length|small-exp|10
            small-exp|0|-63
            small-exp|1|-60
            small-exp|2|-57
            small-exp|3|-54
            small-exp|4|-50
            small-exp|5|-47
            small-exp|6|-44
            small-exp|7|-40
            small-exp|8|-37
            small-exp|9|-34
            length|large-exp|66
            large-exp|0|-1226
            large-exp|1|-1193
            large-exp|2|-1160
            large-exp|3|-1127
            large-exp|4|-1093
            large-exp|5|-1060
            large-exp|6|-1027
            large-exp|7|-994
            large-exp|8|-960
            large-exp|9|-927
            large-exp|10|-894
            large-exp|11|-861
            large-exp|12|-828
            large-exp|13|-794
            large-exp|14|-761
            large-exp|15|-728
            large-exp|16|-695
            large-exp|17|-661
            large-exp|18|-628
            large-exp|19|-595
            large-exp|20|-562
            large-exp|21|-529
            large-exp|22|-495
            large-exp|23|-462
            large-exp|24|-429
            large-exp|25|-396
            large-exp|26|-362
            large-exp|27|-329
            large-exp|28|-296
            large-exp|29|-263
            large-exp|30|-230
            large-exp|31|-196
            large-exp|32|-163
            large-exp|33|-130
            large-exp|34|-97
            large-exp|35|-63
            large-exp|36|-30
            large-exp|37|3
            large-exp|38|36
            large-exp|39|69
            large-exp|40|103
            large-exp|41|136
            large-exp|42|169
            large-exp|43|202
            large-exp|44|235
            large-exp|45|269
            large-exp|46|302
            large-exp|47|335
            large-exp|48|368
            large-exp|49|402
            large-exp|50|435
            large-exp|51|468
            large-exp|52|501
            large-exp|53|534
            large-exp|54|568
            large-exp|55|601
            large-exp|56|634
            large-exp|57|667
            large-exp|58|701
            large-exp|59|734
            large-exp|60|767
            large-exp|61|800
            large-exp|62|833
            large-exp|63|867
            large-exp|64|900
            large-exp|65|933
            step|10
            bias|350
            """.trimIndent().lines()
        assertEquals(expected, actual)
    }

    @Test
    fun existingCachedMethodsUseGeneratedValues() {
        val p = ExtendedFloatCache.getPowers()
        assertEquals(9223372036854775808uL, p.getSmall(0).mant)
        assertEquals(-63, p.getSmall(0).exp)
        assertEquals(11555125961253852697uL, p.getLarge(0).mant)
        assertEquals(1000000000uL, p.getSmallInt(9))
    }
}
