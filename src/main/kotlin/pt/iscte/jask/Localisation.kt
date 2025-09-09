package pt.iscte.jask

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.vm.IValue
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.StringReader
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.extension

object Localisation {
    internal val languages = mutableMapOf<String, Language>()

    internal var argumentFormatter: (String) -> String = { "[$it]" }
        private set

    init {
        loadLanguageFromResource("en")
        loadLanguageFromResource("pt")
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

    val DefaultLanguage: Language =
        languages["en"]!!

    // Thank you, ChatGPT (I'm quite certain java.nio.file.FileSystems is dark magic)
    fun loadLanguageFromResource(code: String): Language {
        val path = "localisation/$code"
        val url = this::class.java.classLoader.getResource(path)
            ?: throw FileNotFoundException("No such resource: $path")

        val properties = Properties()

        when (url.protocol) {
            // Run in IDE
            "file" -> {
                val dir = Paths.get(url.toURI())
                Files.list(dir).filter { it.extension == "properties" }.forEach { p ->
                    Files.newInputStream(p).use { properties.load(it) }
                }
            }

            // Run from packaged JAR
            "jar" -> {
                FileSystems.newFileSystem(url.toURI(), emptyMap<String, Any>()).use { fileSystem ->
                    val dir = fileSystem.getPath(path)
                    Files.list(dir).filter { it.extension == "properties" }.forEach { p ->
                        fileSystem.provider().newInputStream(p).use { properties.load(it) }
                    }
                }
            }

            else -> throw UnsupportedOperationException("Unsupported URL protocol: ${url.protocol}")
        }

        val lang = Language(code, properties)
        languages[code] = lang
        return lang
    }

    fun getLanguage(code: String): Language =
        languages[code] ?: throw NoSuchElementException("No language with code $code!")
}

data class Language(val code: String, val properties: Properties) {
    //private val properties = Properties().apply { load(file.inputStream()) }

    companion object {
        val DEFAULT: Language = Localisation.DefaultLanguage
        internal const val POSTFIX_ANONYMOUS = "AnonCall"
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