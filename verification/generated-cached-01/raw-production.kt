import io.github.kotlinmania.serdejson.lexical.generated.cached.*
private fun show(label:String,index:ULong,f:()->ExtendedFloat) {
    val result=try {val x=f();"O|${x.mant}|${x.exp}"}catch(_:__rtkPanic){"P"}
    println("$label|$index|$result")
}
fun main() {
    val powers=__rtkImpl0.get_powers()
    println("static|${powers === __rtkImpl0.get_powers()}")
    println("meta|${powers.small.len()}|${powers.large.len()}|${powers.small_int.size}|${powers.step}|${powers.bias}")
    val indices=(0..powers.large.len().toInt()+1).map { it.toULong() }+listOf(ULong.MAX_VALUE,1uL shl 32,Long.MAX_VALUE.toULong())
    for(index in indices) {
        show("small",index){powers.get_small(index)};show("large",index){powers.get_large(index)}
        val result=try {"O|${powers.get_small_int(index)}"}catch(_:__rtkPanic){"P"}
        println("int|$index|$result")
    }
    for((mant,exp) in listOf(
        emptyList<ULong>() to emptyList(),
        listOf(0uL,ULong.MAX_VALUE,1uL,42uL) to listOf(Int.MIN_VALUE,Int.MAX_VALUE),
        listOf(7uL) to listOf(-63,0,63),
    )) {
        val values=ExtendedFloatArray(mant,exp);println("array-len|${values.len()}")
        for(index in listOf(0uL,1uL,2uL,3uL,4uL,ULong.MAX_VALUE,1uL shl 32)) show("array",index){values.get_extended_float(index)}
    }
}
