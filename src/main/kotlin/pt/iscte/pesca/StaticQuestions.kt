package pt.iscte.pt.iscte.pesca

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt

data class HowManyParameters(val methodName: String): StaticQuestion {

    override fun build(source: String, language: String): QuestionData {

        val method = getMethod(methodName = methodName, source = source)

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"

        val parameters = method.parameters.size

        return QuestionData(
            SimpleTextStatement(
                textEN = "How many parameters does the $signature method take?",
                textPT = "Quantos parâmetros tem o método $signature?"
            ),
            getNearValuesAndNoneOfTheAbove(parameters),
            language = language
        )
    }

}

data class IsRecursive(val methodName: String) : StaticQuestion {

    override fun build(source: String, language: String): QuestionData {

        val method = getMethod(methodName = methodName, source = source)

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"

        val isRecursive = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString == method.nameAsString
        }


        return QuestionData(
            SimpleTextStatement(
                textEN = "Is the method $signature recursive?",
                textPT = "O método $signature é recursivo?"
                ),
            getTrueOrFalse(isRecursive),
            language = language
        )
    }
}

data class HowManyVariables(val methodName: String): StaticQuestion{
    override fun build(source: String, language: String): QuestionData {

        val method = getMethod(methodName = methodName, source = source)

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"

        val howManyVariables = method.body.get().findAll(VariableDeclarationExpr::class.java).size

        return QuestionData(
            SimpleTextStatement(
                textEN = "How many variables (not including parameters) does the function $signature have?",
                textPT = "Quantas variáveis (excluindo os parâmetros) têm a função $signature?"
            ),
            getNearValuesAndNoneOfTheAbove(howManyVariables),
            language = language
        )
    }
}


data class HowManyLoops(val methodName: String): StaticQuestion{
    override fun build(source: String, language: String): QuestionData {

        val method = getMethod(methodName = methodName, source = source)

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"

        val howManyLoops = listOf(
            method.body.get().findAll(ForStmt::class.java).size,
            method.body.get().findAll(DoStmt::class.java).size,
            method.body.get().findAll(WhileStmt::class.java).size,
            method.body.get().findAll(ForEachStmt::class.java).size
        ).sum()

        return QuestionData(
            SimpleTextStatement(
                textEN = "How many loops does function $signature have?",
                textPT = "Quantos loops tem a função $signature?"
            ),
            getNearValuesAndNoneOfTheAbove(howManyLoops),
            language = language
        )
    }
}

data class CallsOtherFunctions(val methodName: String) : StaticQuestion {

    override fun build(source: String, language: String): QuestionData {

        val method = getMethod(methodName = methodName, source = source)

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"

        val callsOtherFunctions = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString != method.nameAsString
        }

        return QuestionData(
            SimpleTextStatement(
                textEN = "Does function $signature depend on other functions?",
                textPT = "A função $signature depende de outras funções?"),
            getTrueOrFalse(callsOtherFunctions),
            language = language
        )
    }
}












