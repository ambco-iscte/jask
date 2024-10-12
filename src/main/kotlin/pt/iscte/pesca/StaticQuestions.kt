package pt.iscte.pt.iscte.pesca

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt

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

data class HowManyVariables(val methodName: String): StaticQuestion{
    override fun build(source: String): QuestionData {
        val unit = StaticJavaParser.parse(source)

        val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
        if (method == null)
            throw NoSuchMethodException("Method not found: $methodName")

        val howManyVariables = method.body.get().findAll(VariableDeclarationExpr::class.java).size

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"
        val statement = SimpleTextStatement("How many variables (not including parameters) " +
                "does the function $signature have?")
        val options: Map<OptionData, Boolean> = mutableMapOf(
            SimpleTextOptionData(howManyVariables) to true,
            SimpleTextOptionData(howManyVariables + 1) to false,
            SimpleTextOptionData(if (howManyVariables == 0) 2 else howManyVariables - 1) to false,
            NoneOfTheAbove to false
        )

        return QuestionData(statement, options)
    }
}


data class HowManyLoops(val methodName: String): StaticQuestion{
    override fun build(source: String): QuestionData {
        val unit = StaticJavaParser.parse(source)
        val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
        if (method == null)
            throw NoSuchMethodException("Method not found: $methodName")
        val howManyFor = method.body.get().findAll(ForStmt::class.java).size
        val howManyDo = method.body.get().findAll(DoStmt::class.java).size
        val howManyWhile = method.body.get().findAll(WhileStmt::class.java).size
        val howManyForEach = method.body.get().findAll(ForEachStmt::class.java).size
        val howManyLoops = howManyFor + howManyDo + howManyWhile + howManyForEach



        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"


        val statement = SimpleTextStatement("How many loops does function $signature have?")
        val options: Map<OptionData, Boolean> = mutableMapOf(
            SimpleTextOptionData(howManyLoops) to true,
            SimpleTextOptionData(howManyLoops + 1) to false,
            SimpleTextOptionData(if (howManyLoops == 0) 2 else howManyLoops - 1) to false,
            NoneOfTheAbove to false
        )

        return QuestionData(statement, options)
    }
}

data class CallsOtherFunctions(val methodName: String) : StaticQuestion {

    override fun build(source: String): QuestionData {
        val unit = StaticJavaParser.parse(source)

        val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
        if (method == null)
            throw NoSuchMethodException("Method not found: $methodName")

        val callsOtherFunctionsisRecursive = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString != method.nameAsString
        }

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"
        return QuestionData(
            SimpleTextStatement("Does function $signature depend on other functions?"),
            mapOf(
                SimpleTextOptionData("Yes") to callsOtherFunctionsisRecursive,
                SimpleTextOptionData("No") to !callsOtherFunctionsisRecursive
            )
        )
    }
}












