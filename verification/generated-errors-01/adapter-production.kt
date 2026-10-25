import io.github.kotlinmania.serdejson.lexical.*
import io.github.kotlinmania.serdejson.lexical.F64Float
fun main() {
    println("constants|32|${F32Float.exponentBias}|${F32Float.mantissaSize}")
    println("constants|64|${F64Float.exponentBias}|${F64Float.mantissaSize}")
    println("scale|${ULongFloatErrors.errorScale()}|${ULongFloatErrors.errorHalfscale()}")
    while(true) {
        val line=readlnOrNull() ?: break
        val row=line.split('|');val width=row[0].toInt();val count=row[1].toUInt()
        val fp=ExtendedFloat(row[2].toULong(),row[3].toInt())
        val output=try {"O|${errorIsAccurate(if(width==32) F32Float else F64Float,count,fp)}"}catch(_:Exception){"P"}
        println("$line|$output|${fp.mant}|${fp.exp}")
    }
}
