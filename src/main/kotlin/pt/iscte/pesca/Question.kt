package pt.iscte.pt.iscte.pesca

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import java.io.File

interface IsQuestionStatement{
}

data class SimpleTextStatement(val text: String): IsQuestionStatement {
    override fun toString() = text
}


interface IsOptionData{
}

data class SimpleTextOptionData(val text: String): IsOptionData {
    constructor(value: Any): this(value.toString())

    override fun toString() = text
}

//Options that are always below the others
val NoneOfTheAbove = SimpleTextOptionData("None of the above.")
val AllOfTheAbove = SimpleTextOptionData("All of the above.")
val lastOptions = mutableListOf<IsOptionData>(NoneOfTheAbove, AllOfTheAbove)

data class QuestionData(
    val statement: IsQuestionStatement,
    private val options: Map<IsOptionData, Boolean>,
) {
    private val shuffledOptions: Map<IsOptionData, Boolean>
        get() {
            val shuffled = options.keys.filter { option ->  !(lastOptions.contains(option))  }.shuffled().associateWith {
                option -> options[option]!!
            }.toMutableMap()
            lastOptions.forEach { lastOption ->
                if (options.containsKey(lastOption))
                    shuffled[lastOption] = options[lastOption]!!
            }
            return shuffled
        }

    init {
        require(options.size >= 2) { "Question must have at least two options!" }
        require(options.any { option -> option.value }) { "Question must have at least one correct option!" }
    }

    override fun toString(): String = "$statement\n${shuffledOptions.toList().joinToString(System.lineSeparator()) { 
        option -> "[${if (option.second) "x" else " "}] ${option.first}"
    }}"
}

interface IsQuestion {
}

open class QuestionAboutTheCode(val sourceOfCode: String):IsQuestion {
    constructor(file: File) : this(sourceOfCode = file.readText())
    /**
     * To be overridden
     * Builds the question from the source code.
     */
    open fun build(): QuestionData?{
        return null
    }
}

open class QuestionAboutAMethodInTheCode(private val methodInCode: String, source: String): QuestionAboutTheCode(source) {
    constructor(methodName:String, file: File) : this(methodName,source = file.readText())

    /**
     * Gets the method with the given name from a Java source code.
     * @param methodName Method's name.
     * @param source Source code of a Java class.
     * @return The method with the given name
     */
    val method: MethodDeclaration
        get() {
            val unit = StaticJavaParser.parse(this.sourceOfCode)

            val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodInCode }
            if (method == null)
                throw NoSuchMethodException("Method not found: $methodInCode")
            return method
        }

    val signature: String
        get() = "${method.nameAsString}(${method.parameters.joinToString()})"

}

sealed interface IsMultipleChoiceQuestion : IsQuestion {

    fun getNearValuesAndNoneOfTheAbove(correctValue: Int):Map<IsOptionData, Boolean>{
        return mutableMapOf(
            SimpleTextOptionData(correctValue) to true,
            SimpleTextOptionData(correctValue + 1) to false,
            SimpleTextOptionData(if (correctValue == 0) 2 else correctValue - 1) to false,
            NoneOfTheAbove to false
        )
    }

    fun getTrueOrFalse(correctValue: Boolean):Map<IsOptionData, Boolean>{
        return mapOf(
            SimpleTextOptionData("Yes") to correctValue,
            SimpleTextOptionData("No") to !correctValue
        )
    }
}

interface IsStaticQuestion : IsQuestion{
}

interface IsDynamicQuestion : IsQuestion{
}
