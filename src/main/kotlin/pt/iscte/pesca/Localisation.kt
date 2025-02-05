package pt.iscte.pesca

import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import java.net.URLDecoder
import java.util.Properties
import java.util.jar.JarFile

object Localisation {
    internal val languages = mutableSetOf<Language>()

    init {
        languages.add(Language("en", Properties().apply {
            load(StringReader(loadResource("localisation/en.properties")!!))
        }))
        languages.add(Language("pt", Properties().apply {
            load(StringReader(loadResource("localisation/pt.properties")!!))
        }))
    }

    fun loadResource(path: String): String? {
        return javaClass.classLoader.getResourceAsStream(path)?.bufferedReader(Charsets.ISO_8859_1)?.use { it.readText() }
    }



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

    fun getLocalisation(key: String): String =
        if (properties.containsKey(key)) properties.getProperty(key)
        else throw NoSuchElementException("No $code translation for $key!")

    operator fun get(key: String): String = getLocalisation(key)

}

fun main() {
    //Localisation.languages.forEach {
       // println("${it.code}: ${it.file.path}")
    //}
    val pt = Localisation.getLanguage("en")
    println(pt.getLocalisation("HowManyParams"))
}