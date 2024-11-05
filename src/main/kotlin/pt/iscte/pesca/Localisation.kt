package pt.iscte.pesca

import java.io.File
import java.io.FileInputStream
import java.util.Properties

object Localisation {
    internal val languages = mutableSetOf<Language>()

    init {
        val root = File(this::class.java.getResource("/localisation")!!.path)
        root.listFiles()?.forEach { register(it) }
    }

    val DefaultLanguage: Language = languages.first { it.code ==  "en" }

    fun register(file: File): Language {
        val code = file.nameWithoutExtension
        if (languages.any { it.code == code })
            throw IllegalArgumentException("Cannot register language ${file.name}: duplicated code $code!")
        val lang = Language(code, file)
        languages.add(lang)
        return lang
    }

    fun getLanguage(code: String): Language =
        languages.firstOrNull { it.code == code } ?: DefaultLanguage
}

data class Language(val code: String, val file: File) {
    private val properties = Properties().apply { load(file.inputStream()) }

    companion object {
        val DEFAULT: Language = Localisation.DefaultLanguage
    }

    fun getLocalisation(key: String): String =
        if (properties.containsKey(key)) properties.getProperty(key)
        else throw NoSuchElementException("No $code translation for $key!")

    operator fun get(key: String): String = getLocalisation(key)

}

fun main() {
    Localisation.languages.forEach {
        println("${it.code}: ${it.file.path}")
    }
    val pt = Localisation.getLanguage("pt")
    println(pt.getLocalisation("HowManyParameters"))
}