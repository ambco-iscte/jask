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
                mutableMapOf(
                    ENGLISH_LANGUAGE to "How many parameters does the $signature method take?",
                    PORTUGUESE_LANGUAGE to "Quantos parâmetros tem o método $signature?"
                )
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
                mutableMapOf(
                    ENGLISH_LANGUAGE to "Is the method $signature recursive?",
                    PORTUGUESE_LANGUAGE to "O método $signature é recursivo?"
                )
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
                mutableMapOf(
                    ENGLISH_LANGUAGE to "How many variables (not including parameters) does the function $signature have?",
                    PORTUGUESE_LANGUAGE to "Quantas variáveis (excluindo os parâmetros) têm a função $signature?"
                )
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
                mutableMapOf(
                    ENGLISH_LANGUAGE to "How many loops does function $signature have?",
                    PORTUGUESE_LANGUAGE to "Quantos loops tem a função $signature?"
                )
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
                mutableMapOf(
                    ENGLISH_LANGUAGE to "Does function $signature depend on other functions?",
                    PORTUGUESE_LANGUAGE to "A função $signature depende de outras funções?"
                )
            ),
            getTrueOrFalse(callsOtherFunctions),
            language = language
        )
    }
}

data class CanCallAMethodWithGivenArguments(val methodName: String, val args: List<Any>): StaticQuestion {

    override fun build(source: String, language: String): QuestionData {

        val method = getMethod(methodName = methodName, source = source)

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"

        val canCallAMethodWithGivenArguments = canCallJavaMethodWithArgs(method,args)

        val arguments = args.withIndex().joinToString(separator = ", ") { (index, arg) ->
            "\n${method.parameters.get(index).name}: " + formattedArg(arg)
        }

        return QuestionData(
            SimpleTextStatement(
                mutableMapOf(
                    ENGLISH_LANGUAGE to "Can the method $signature be called with these arguments: $arguments?",
                    PORTUGUESE_LANGUAGE to "O método $signature pode ser chamado com os seguintes argumentos?$arguments"
                )
            ),
            getTrueOrFalse(canCallAMethodWithGivenArguments),
            language = language
        )
    }

}

data class WhatsTheReturnType(val methodName: String) : StaticQuestion {

    override fun build(source: String, language: String): QuestionData {

        val method = getMethod(methodName = methodName, source = source)

        val signature = "${method.nameAsString}(${method.parameters.joinToString()})"

        val whatsTheReturnType = method.type.asString()

        val otherTypes = PRIMITIVE_JAVA_TYPES_EXCLUDING_VOID.filter { it != whatsTheReturnType }.shuffled().take(3)

        val options = mutableMapOf<OptionData,Boolean>(
            SimpleTextOptionData(otherTypes[0]) to false,
            SimpleTextOptionData(otherTypes[1]) to false,
            SimpleTextOptionData(otherTypes[2]) to false,
        )

        if (!isMethodReturningObject(method)) {
            options[NONE_OF_THE_ABOVE_OPTION] = true
        } else {
            options[SimpleTextOptionData(whatsTheReturnType)] = true
        }

        return QuestionData(
            SimpleTextStatement(
                mutableMapOf(
                    ENGLISH_LANGUAGE to "What is the return type of the function $signature?",
                    PORTUGUESE_LANGUAGE to "Qual o tipo devolvido pela função $signature?"
                )
            ),
            options,
            language = language
        )
    }
}










