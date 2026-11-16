@file:OptIn(kotlin.ExperimentalUnsignedTypes::class)
import io.github.kotlinmania.serdejson.lexical.*
import io.github.kotlinmania.serdejson.lexical.generated.cached.__rtkPanic
private fun legacyIndex(index:ULong) = index <= Int.MAX_VALUE.toULong() || index == ULong.MAX_VALUE
private fun toLegacyIndex(index:ULong):Int { require(legacyIndex(index)); return index.toInt() }
private fun show(label:String,index:ULong,f:()->ExtendedFloat) {
    if (!legacyIndex(index)) return
    val result=try {val x=f();"O|${x.mant}|${x.exp}"}catch(_:__rtkPanic){"P"}
    println("$label|$index|$result")
}
fun main() {
    val powers=ExtendedFloatCache.getPowers()
    println("static|${powers === ExtendedFloatCache.getPowers()}")
    println("meta|${powers.small.length.toULong()}|${powers.large.length.toULong()}|${powers.smallInt.size}|${powers.step}|${powers.bias}")
    val indices=(0..powers.large.length.toULong().toInt()+1).map { it.toULong() }+listOf(ULong.MAX_VALUE,1uL shl 32,Long.MAX_VALUE.toULong())
    for(index in indices) {
        if (!legacyIndex(index)) continue
        show("small",index){powers.getSmall(toLegacyIndex(index))};show("large",index){powers.getLarge(toLegacyIndex(index))}
        val result=try {"O|${powers.getSmallInt(toLegacyIndex(index))}"}catch(_:__rtkPanic){"P"}
        println("int|$index|$result")
    }
    for((mant,exp) in listOf(
        emptyList<ULong>() to emptyList(),
        listOf(0uL,ULong.MAX_VALUE,1uL,42uL) to listOf(Int.MIN_VALUE,Int.MAX_VALUE),
        listOf(7uL) to listOf(-63,0,63),
    )) {
        val values=ExtendedFloatArray(mant.toULongArray(),exp.toIntArray());println("array-len|${values.length.toULong()}")
        for(index in listOf(0uL,1uL,2uL,3uL,4uL,ULong.MAX_VALUE,1uL shl 32)) show("array",index){values.getExtendedFloat(toLegacyIndex(index))}
    }
}
