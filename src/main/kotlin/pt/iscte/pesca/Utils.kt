package pt.iscte.pt.iscte.pesca

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration

//Options that are always below the others
val NONE_OF_THE_ABOVE_OPTION = SimpleTextOptionData(textEN = "None of the above.", textPT =  "Nenhuma das anteriores.")
val ALL_OF_THE_ABOVE_OPTION = SimpleTextOptionData(textEN = "All of the above.", textPT = "Todas as anteriores.")
val YES_OPTION = SimpleTextOptionData(textEN = "Yes", textPT = "Sim")
val NO_OPTION = SimpleTextOptionData(textEN = "No", textPT = "Não")
val LAST_UNSHUFFLED_OPTIONS: MutableList<OptionData> = mutableListOf<OptionData>(
    NONE_OF_THE_ABOVE_OPTION,
    ALL_OF_THE_ABOVE_OPTION,
    YES_OPTION,
    NO_OPTION
)

//Supported Languages
val PORTUGUESE_LANGUAGE = "pt"
val ENGLISH_LANGUAGE = "en"
val DEFAULT_LANGUAGE = "pt"

fun getNearValuesAndNoneOfTheAbove(correctValue: Int):Map<OptionData, Boolean>{
    return mutableMapOf(
        SimpleTextOptionData(correctValue) to true,
        SimpleTextOptionData(correctValue + 1) to false,
        SimpleTextOptionData(if (correctValue == 0) 2 else correctValue - 1) to false,
        NONE_OF_THE_ABOVE_OPTION to false
    )
}

fun getTrueOrFalse(correctValue: Boolean):Map<OptionData, Boolean>{
    return mapOf(
        YES_OPTION to correctValue,
        NO_OPTION to !correctValue
    )
}

fun getMethod(source: String, methodName: String): MethodDeclaration {
        val unit = StaticJavaParser.parse(source)

        val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
        if (method == null)
            throw NoSuchMethodException("Method not found: $methodName")
        return method
    }