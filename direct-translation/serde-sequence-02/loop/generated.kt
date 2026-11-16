while (true) {
    when (val `__serdeNext0` = `access`.nextElement(`ValueDeserialize`).getOrThrow()) {
        is io.github.kotlinmania.serdecore.de.SeqElement.Some -> {
            val `elem` = `__serdeNext0`.value
            `values`.add(`elem`)
        }
        io.github.kotlinmania.serdecore.de.SeqElement.None -> break
    }
}
