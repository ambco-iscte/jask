package pt.iscte.pt.iscte.pesca

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt
import java.io.File

data class HowManyParameters(val methodName: String, val source: String) :QuestionAboutAMethodInTheCode(methodName,source) ,IsStaticQuestion, IsMultipleChoiceQuestion {
    constructor(methodName: String, source: File)  : this(methodName, source = source.readText())

    override fun build(): QuestionData {

        val parameters = method.parameters.size

        return QuestionData(
            SimpleTextStatement("How many parameters does the $signature method take?"),
            getNearValuesAndNoneOfTheAbove(parameters)
            )
    }
}

data class IsRecursive(val methodName: String, val source: String) :QuestionAboutAMethodInTheCode(methodName,source) ,IsStaticQuestion, IsMultipleChoiceQuestion {
    constructor(methodName: String, source: File)  : this(methodName, source = source.readText())

    override fun build(): QuestionData {

        val isRecursive = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString == method.nameAsString
        }

        return QuestionData(
            SimpleTextStatement("Is the method $signature recursive?"),
            getTrueOrFalse(isRecursive)
        )
    }
}

data class HowManyVariables(val methodName: String, val source: String) :QuestionAboutAMethodInTheCode(methodName,source) ,IsStaticQuestion, IsMultipleChoiceQuestion {
    constructor(methodName: String, source: File)  : this(methodName, source = source.readText())

    override fun build(): QuestionData {

        val howManyVariables = method.body.get().findAll(VariableDeclarationExpr::class.java).size

        return QuestionData(
            SimpleTextStatement("How many variables (not including parameters) does the function $signature have?"),
            getNearValuesAndNoneOfTheAbove(howManyVariables)
        )
    }
}


data class HowManyLoops(val methodName: String, val source: String) :QuestionAboutAMethodInTheCode(methodName,source) ,IsStaticQuestion, IsMultipleChoiceQuestion {
    constructor(methodName: String, source: File)  : this(methodName, source = source.readText())

    override fun build(): QuestionData {

        val howManyLoops = listOf(
            method.body.get().findAll(ForStmt::class.java).size,
            method.body.get().findAll(DoStmt::class.java).size,
            method.body.get().findAll(WhileStmt::class.java).size,
            method.body.get().findAll(ForEachStmt::class.java).size
        ).sum()

        return QuestionData(
            SimpleTextStatement("How many loops does function $signature have?"),
            getNearValuesAndNoneOfTheAbove(howManyLoops)
        )
    }
}

data class CallsOtherFunctions(val methodName: String, val source: String) :QuestionAboutAMethodInTheCode(methodName,source) ,IsStaticQuestion, IsMultipleChoiceQuestion {
    constructor(methodName: String, source: File)  : this(methodName, source = source.readText())

    override fun build(): QuestionData {

        val callsOtherFunctions = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString != method.nameAsString
        }

        return QuestionData(
            SimpleTextStatement("Does function $signature depend on other functions?"),
            getTrueOrFalse(callsOtherFunctions)
        )
    }
}












