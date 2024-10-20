package pt.iscte.pt.iscte.pesca.questions

import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.pt.iscte.pesca.*

data class HowManyParameters(val methodName: String): StaticQuestion {

    override fun build(source: String, language: String): QuestionData {
        val method = getMethod(methodName = methodName, source = source)
        val signature = method.prettySignature

        val parameters = method.parameters.size

        return QuestionData(
            SimpleTextStatement(
                ENGLISH to "How many parameters does the $signature method take?",
                PORTUGUESE to "Quantos parâmetros tem o método $signature?"
            ),
            getNearValuesAndNoneOfTheAbove(parameters),
            language = language
        )
    }
}

data class IsRecursive(val methodName: String) : StaticQuestion {

    override fun build(source: String, language: String): QuestionData {
        val method = getMethod(methodName = methodName, source = source)
        val signature = method.prettySignature

        val isRecursive = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString == method.nameAsString
        }

        return QuestionData(
            SimpleTextStatement(
                ENGLISH to "Is the method $signature recursive?",
                PORTUGUESE to "O método $signature é recursivo?"
            ),
            getTrueOrFalse(isRecursive),
            language = language
        )
    }

    override fun isApplicable(source: String): Boolean =
        getMethod(methodName = methodName, source = source).findAll(MethodCallExpr::class.java).isNotEmpty()
}

data class HowManyVariables(val methodName: String): StaticQuestion {

    override fun build(source: String, language: String): QuestionData {
        val method = getMethod(methodName = methodName, source = source)
        val signature = method.prettySignature

        val howManyVariables = method.body.get().findAll(VariableDeclarationExpr::class.java).size

        return QuestionData(
            SimpleTextStatement(
                ENGLISH to "How many variables (not including parameters) does the method $signature have?",
                PORTUGUESE to "Quantas variáveis (excluindo os parâmetros) tem o método $signature?"
            ),
            getNearValuesAndNoneOfTheAbove(howManyVariables),
            language = language
        )
    }
}

data class HowManyLoops(val methodName: String): StaticQuestion {

    override fun build(source: String, language: String): QuestionData {
        val method = getMethod(methodName = methodName, source = source)
        val signature = method.prettySignature

        val howManyLoops = method.body.get().getLoopControlStructures().size

        return QuestionData(
            SimpleTextStatement(
                ENGLISH to "How many loops does method $signature have?",
                PORTUGUESE to "Quantos ciclos tem o método $signature?"
            ),
            getNearValuesAndNoneOfTheAbove(howManyLoops),
            language = language
        )
    }

    override fun isApplicable(source: String): Boolean =
        getMethod(methodName = methodName, source = source).getLoopControlStructures().isNotEmpty()
}

data class CallsOtherFunctions(val methodName: String) : StaticQuestion {

    override fun build(source: String, language: String): QuestionData {
        val method = getMethod(methodName = methodName, source = source)
        val signature = method.prettySignature

        val callsOtherFunctions = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString != method.nameAsString
        }

        return QuestionData(
            SimpleTextStatement(
                ENGLISH to "Does the method $signature depend on other methods?",
                PORTUGUESE to "O método $signature depende de outros métodos?"
            ),
            getTrueOrFalse(callsOtherFunctions),
            language = language
        )
    }
}

data class CanCallAMethodWithGivenArguments(val methodName: String, val arguments: List<Any>): StaticQuestion {

    constructor(methodName: String, vararg arguments: Any) : this(methodName, arguments.toList())

    override fun build(source: String, language: String): QuestionData {
        val method = getMethod(methodName = methodName, source = source)
        val signature = method.prettySignature

        val canCallAMethodWithGivenArguments = method.accepts(arguments)

        val args = arguments.joinToString()

        return QuestionData(
            SimpleTextStatement(
                ENGLISH to "Can the method $signature be called with the arguments ($args)?",
                PORTUGUESE to "O método $signature pode ser chamado com os argumentos ($args)?"
            ),
            getTrueOrFalse(canCallAMethodWithGivenArguments),
            language = language
        )
    }
}

data class WhatIsTheReturnType(val methodName: String) : StaticQuestion {

    override fun build(source: String, language: String): QuestionData {
        val method = getMethod(methodName = methodName, source = source)
        val signature = method.prettySignature

        val methodReturnType = method.type.asString()

        val otherTypes = JAVA_PRIMITIVE_TYPES.filter { it != methodReturnType }.shuffled().take(3)

        val options: MutableMap<OptionData, Boolean> =
            otherTypes.associate { SimpleTextOptionData(it) to false }.toMutableMap()

        val finalOption =
            if (method.returnsPrimitiveOrArrayOrString()) SimpleTextOptionData(methodReturnType)
            else NONE_OF_THE_ABOVE
        options[finalOption] = true

        return QuestionData(
            SimpleTextStatement(
                ENGLISH to "What is the return type of the method $signature?",
                PORTUGUESE to "Qual o tipo de retorno do método $signature?"
            ),
            options,
            language = language
        )
    }
}