package pt.iscte.pt.iscte.pesca

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr

data class HowManyParameters(val methodName: String): StaticQuestion {

    override fun build(source: String): QuestionData {
        val unit = StaticJavaParser.parse(source)

        val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
        if (method == null)
            throw NoSuchMethodException("Method not found: $methodName")
        val parameters = method.parameters.size

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"
        val statement = SimpleTextStatement("How many parameters does the $signature method take?")
        val options: Map<OptionData, Boolean> = mutableMapOf(
            SimpleTextOptionData(parameters) to true,
            SimpleTextOptionData(parameters + 1) to false,
            SimpleTextOptionData(if (parameters == 0) 2 else parameters - 1) to false,
            NoneOfTheAbove to false
        )

        return QuestionData(statement, options)
    }
}

data class IsRecursive(val methodName: String) : StaticQuestion {

    override fun build(source: String): QuestionData {
        val unit = StaticJavaParser.parse(source)

        val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
        if (method == null)
            throw NoSuchMethodException("Method not found: $methodName")

        val isRecursive = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString == method.nameAsString
        }

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"
        return QuestionData(
            SimpleTextStatement("Is the method $signature recursive?"),
            mapOf(
                SimpleTextOptionData("Yes") to isRecursive,
                SimpleTextOptionData("No") to !isRecursive
            )
        )
    }
}