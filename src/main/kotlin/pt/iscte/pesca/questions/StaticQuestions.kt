package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.accepts
import pt.iscte.pesca.extensions.*
import pt.iscte.pesca.sample
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.collections.ifEmpty
import kotlin.text.format

data class HowManyParams(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val signature = method.prettySignature
        val parameters = method.parameters.size

        return QuestionData(
            TextWithCodeStatement(language["HowManyParams"].format(signature), method.toString()),
            parameters.multipleChoice(language),
            language = language
        )
    }
}

data class IsRecursive(val methodName: String? = null) : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName) && element.body.getOrNull?.hasMethodCalls() == true

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val signature = method.prettySignature

        val isRecursive = method.findAll<MethodCallExpr>().any { call ->
            call.nameAsString == method.nameAsString
        }

        return QuestionData(
            TextWithCodeStatement(language["IsRecursive"].format(signature), method.toString()),
            isRecursive.trueOrFalse(language),
            language = language
        )
    }
}

data class HowManyVariables(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val signature = method.prettySignature

        val howManyVariables = method.body.get().findAll<VariableDeclarationExpr>().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyVariables"].format(signature), method.toString()),
            howManyVariables.multipleChoice(language),
            language = language
        )
    }
}

data class HowManyLoops(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName) && element.body.getOrNull?.hasLoopControlStructures() == true

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val howManyLoops = method.body.get().getLoopControlStructures().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyLoops"].format(method.nameAsString), method),
            howManyLoops.multipleChoice(language),
            language = language
        )
    }
}

data class CallsOtherFunctions(val methodName: String? = null) : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val callsOtherFunctions = method.findAll<MethodCallExpr>().any { call ->
            call.nameAsString != method.nameAsString
        }

        return QuestionData(
            TextWithCodeStatement(language["CallsOtherFunctions"].format(method.nameAsString), method),
            callsOtherFunctions.trueOrFalse(language),
            language = language
        )
    }
}

data class HowManyFunctions(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val otherFunctions = method.findAll<MethodCallExpr>().map { it.nameAsString }.toSet().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyFunctions"].format(method.nameAsString), method),
            otherFunctions.multipleChoice(language),
            language = language
        )
    }
}

data class CanCallAMethodWithGivenArguments(val methodName: String? = null, val arguments: List<Any>): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    constructor(methodName: String?, vararg arguments: Any) : this(methodName, arguments.toList())

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val canCallAMethodWithGivenArguments = method.accepts(arguments)

        val args = arguments.joinToString()

        return QuestionData(
            TextWithCodeStatement(language["CanCallAMethodWithGivenArguments"].format(method.nameAsString, args), method),
            canCallAMethodWithGivenArguments.trueOrFalse(language),
            language = language
        )
    }
}

data class WhichReturnType(val methodName: String? = null) : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName) && element.getUsedTypes().size >= 2

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val methodReturnType = method.type

        val otherTypes = method.getUsedTypes().filter { it != methodReturnType }.map { it.asString() }.toSet()

        val pool = otherTypes + JAVA_PRIMITIVE_TYPES.filter {
            it != methodReturnType.asString()
        }.sample(3 - otherTypes.size)

        val options: MutableMap<Option, Boolean> =
            pool.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(methodReturnType)] = true

        return QuestionData(
            TextWithCodeStatement(language["WhichReturnType"].format(method.nameAsString), method),
            options,
            language = language
        )
    }
}