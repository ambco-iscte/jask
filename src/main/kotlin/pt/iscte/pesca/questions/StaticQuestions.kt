package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.accepts
import pt.iscte.pesca.extensions.*
import kotlin.collections.ifEmpty
import kotlin.text.format

data class HowManyParameters(val methodName: String? = null): StaticQuestion() {

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration>() }.random()

        val signature = method.prettySignature
        val parameters = method.parameters.size

        return QuestionData(
            TextWithCodeStatement(language["HowManyParameters"].format(signature), method.toString()),
            parameters.multipleChoice(language),
            language = language
        )
    }
}

data class IsRecursive(val methodName: String? = null) : StaticQuestion() {

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration>() }.random()

        val signature = method.prettySignature

        val isRecursive = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString == method.nameAsString
        }

        return QuestionData(
            TextWithCodeStatement(language["IsRecursive"].format(signature), method.toString()),
            isRecursive.trueOrFalse(language),
            language = language
        )
    }

    override fun isApplicable(source: String): Boolean =
        source.findMethod(methodName).ifEmpty { source.getApplicable<MethodDeclaration>() }.any { it.hasMethodCalls() }
}

data class HowManyVariables(val methodName: String? = null): StaticQuestion() {

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration>() }.random()

        val signature = method.prettySignature

        val howManyVariables = method.body.get().findAll(VariableDeclarationExpr::class.java).size

        return QuestionData(
            TextWithCodeStatement(language["HowManyVariables"].format(signature), method.toString()),
            howManyVariables.multipleChoice(language),
            language = language
        )
    }
}

data class HowManyLoops(val methodName: String? = null): StaticQuestion() {

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration>() }.random()

        val signature = method.prettySignature

        val howManyLoops = method.body.get().getLoopControlStructures().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyLoops"].format(signature), method.toString()),
            howManyLoops.multipleChoice(language),
            language = language
        )
    }

    override fun isApplicable(source: String): Boolean =
        source.findMethod(methodName).ifEmpty { source.getApplicable<MethodDeclaration>() }.any {
            it.hasLoopControlStructures()
        }
}

data class CallsOtherFunctions(val methodName: String? = null) : StaticQuestion() {

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration>() }.random()

        val signature = method.prettySignature

        val callsOtherFunctions = method.findAll(MethodCallExpr::class.java).any { call ->
            call.nameAsString != method.nameAsString
        }

        return QuestionData(
            TextWithCodeStatement(language["CallsOtherFunctions"].format(signature), method.toString()),
            callsOtherFunctions.trueOrFalse(language),
            language = language
        )
    }
}

data class CanCallAMethodWithGivenArguments(val methodName: String? = null, val arguments: List<Any>): StaticQuestion() {

    constructor(methodName: String?, vararg arguments: Any) : this(methodName, arguments.toList())

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration>() }.random()

        val signature = method.prettySignature

        val canCallAMethodWithGivenArguments = method.accepts(arguments)

        val args = arguments.joinToString()

        return QuestionData(
            TextWithCodeStatement(language["CanCallAMethodWithGivenArguments"].format(signature, args), method.toString()),
            canCallAMethodWithGivenArguments.trueOrFalse(language),
            language = language
        )
    }
}

data class WhatIsTheReturnType(val methodName: String? = null) : StaticQuestion() {

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration> {
            it.usedTypes().size >= 2
        } }.random()

        val signature = method.prettySignature

        val methodReturnType = method.type

        val otherTypes = method.usedTypes().filter { it != methodReturnType }.map { it.asString() }

        val options: MutableMap<Option, Boolean> =
            (JAVA_PRIMITIVE_TYPES + otherTypes).shuffled().take(3).associate {
                SimpleTextOption(it) to false
            }.toMutableMap()

        val finalOption =
            if (method.returnsPrimitiveOrArrayOrString()) SimpleTextOption(methodReturnType)
            else SimpleTextOption.none(language)
        options[finalOption] = true

        return QuestionData(
            TextWithCodeStatement(language["WhatIsTheReturnType"].format(signature), method.toString()),
            options,
            language = language
        )
    }

    override fun isApplicable(source: String): Boolean {
        return super.isApplicable(source)
    }
}