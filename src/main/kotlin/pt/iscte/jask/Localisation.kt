package pt.iscte.jask

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.vm.IValue
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.StringReader
import java.net.URL
import java.util.Properties

object Localisation {
    internal val languages = mutableMapOf<String, Language>()

    internal var argumentFormatter: (String) -> String = { "[$it]" }
        private set

    init {
        languages["en"] = Language("en", loadResource("localisation/en")!!.file)
        languages["pt"] = Language("pt", loadResource("localisation/pt")!!.file)
    }

    fun setArgumentFormat(format: (String) -> String) {
        argumentFormatter = format
    }

    fun resetArgumentFormat() {
        argumentFormatter = { "[$it]" }
    }

    fun <T> withArgumentFormat(format: (String) -> String, block: () -> T): T {
        setArgumentFormat(format)
        val result = block()
        resetArgumentFormat()
        return result
    }

    private fun loadResource(path: String): URL? =
        javaClass.classLoader.getResource(path)

    val DefaultLanguage: Language =
        languages["en"]!!

    fun register(file: File): Language {
        val code = file.nameWithoutExtension
        if (code in languages)
            throw IllegalArgumentException("Cannot register language ${file.name}: duplicated code $code!")
        val lang = Language(code, file)
        languages[code] = lang
        return lang
    }

    fun getLanguage(code: String): Language =
        languages[code] ?: throw NoSuchElementException("No language with code $code!")
}

data class Language(val code: String, val folder: File) {
    //private val properties = Properties().apply { load(file.inputStream()) }

    constructor(code: String, path: String): this(code, File(path))

    init {
        require(folder.isDirectory) { "Language root is not a folder: $folder" }
    }

    companion object {
        val DEFAULT: Language = Localisation.DefaultLanguage
        internal const val POSTFIX_ANONYMOUS = "AnonCall"
    }

    val properties = Properties().apply {
        folder.listFiles()?.filter { it.extension == "properties" }?.forEach {
            this@apply.load(it.inputStream())
        }
    }

    inner class Entry(val key: String, val template: String) {
        fun format(vararg args: Any?): String =
            template.format(*args.map { when (it) {
                is String -> Localisation.argumentFormatter(it)
                else -> it
            } }.toTypedArray())

        fun orAnonymous(arguments: List<IValue>, procedure: IProcedureDeclaration): Entry =
            if (
                arguments.isEmpty() && procedure.id == "main" && procedure.parameters.isEmpty()
                && !key.endsWith(POSTFIX_ANONYMOUS)
                && this@Language.properties.containsKey("$key$POSTFIX_ANONYMOUS")
            )
                this@Language.getLocalisation("$key$POSTFIX_ANONYMOUS")
            else
                this

        fun orAnonymous(method: MethodDeclaration): Entry =
            if (
                method.parameters.isEmpty() && method.nameAsString == "main"
                && !key.endsWith(POSTFIX_ANONYMOUS)
                && this@Language.properties.containsKey("$key$POSTFIX_ANONYMOUS")
            )
                this@Language.getLocalisation("$key$POSTFIX_ANONYMOUS")
            else
                this

        override fun toString(): String = template
    }

    fun getLocalisation(key: String): Entry =
        if (properties.containsKey(key)) this.Entry(key, properties.getProperty(key))
        else throw NoSuchElementException("No $code translation for $key!")

    operator fun get(key: String): Entry = getLocalisation(key)
}

fun main() {
    //Localisation.languages.forEach {
       // println("${it.code}: ${it.file.path}")
    //}
    val pt = Localisation.getLanguage("en")
    println(pt.getLocalisation("HowManyParams"))
}