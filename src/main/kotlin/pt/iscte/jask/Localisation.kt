package pt.iscte.jask

import java.io.File
import java.io.StringReader
import java.util.Properties

object Localisation {
    internal val languages = mutableSetOf<Language>()

    internal var argumentFormatter: (String) -> String = { "[$it]" }
        private set

    init {
        languages.add(Language("en", Properties().apply {
            load(StringReader(loadResource("localisation/en.properties")!!))
        }))
        languages.add(Language("pt", Properties().apply {
            load(StringReader(loadResource("localisation/pt.properties")!!))
        }))
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

    fun loadResource(path: String): String? =
        javaClass.classLoader.getResourceAsStream(path)?.bufferedReader(Charsets.ISO_8859_1)?.use { it.readText() }

    val DefaultLanguage: Language = languages.first { it.code ==  "en" }

    fun register(file: File): Language {
        val code = file.nameWithoutExtension
        if (languages.any { it.code == code })
            throw IllegalArgumentException("Cannot register language ${file.name}: duplicated code $code!")
        val lang = Language(code, Properties().apply { load(file.inputStream()) })
        languages.add(lang)
        return lang
    }

    fun getLanguage(code: String): Language =
        languages.firstOrNull { it.code == code } ?: DefaultLanguage
}

data class Language(val code: String, val properties: Properties) {
    //private val properties = Properties().apply { load(file.inputStream()) }

    companion object {
        val DEFAULT: Language = Localisation.DefaultLanguage
    }

    data class Entry(val template: String) {
        fun format(vararg args: Any?): String =
            template.format(*args.map { when (it) {
                is String -> Localisation.argumentFormatter(it)
                else -> it
            } }.toTypedArray())

        override fun toString(): String = template
    }

    fun getLocalisation(key: String): Entry =
        if (properties.containsKey(key)) Entry(properties.getProperty(key))
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