import io.github.kotlinmania.serdejson.lexical.*
import io.github.kotlinmania.serdejson.lexical.generated.shift.__rtkPanic
fun main() {
    for (op in 0..2) {
        for (mant in listOf(0uL,1uL,3uL,0x8000000000000000uL,ULong.MAX_VALUE,0x123456789abcdef0uL)) {
            for (exp in listOf(Int.MIN_VALUE,Int.MIN_VALUE+1,-64,-1,0,1,64,Int.MAX_VALUE-1,Int.MAX_VALUE)) {
                for (amount in listOf(Int.MIN_VALUE,-65,-64,-1,0,1,2,31,32,63,64,65,Int.MAX_VALUE)) {
                    val value=ExtendedFloat(mant,exp)
                    val result=try {
                        when(op) {
                            0 -> shr(value,amount)
                            1 -> overflowingShr(value,amount)
                            else -> shl(value,amount)
                        }
                        "O"
                    } catch (_: __rtkPanic) { "P" }
                    println("$op|$mant|$exp|$amount|$result|${value.mant}|${value.exp}")
                }
            }
        }
    }
}
