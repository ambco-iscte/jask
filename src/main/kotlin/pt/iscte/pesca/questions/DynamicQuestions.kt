package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.type.VoidType
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.prettySignature

data class WhatIsTheCallResult(val methodName: String? = null): DynamicQuestion() {

    override fun build(sources: List<String>, language: Language): QuestionData {
        val method = sources.findMethod(methodName).ifEmpty { sources.getApplicable<MethodDeclaration>() {
            it.typeAsString != "void"
        } }.random()

        // TODO lol

        val parameters = method.parameters.size

        return QuestionData(
            TextWithCodeStatement(language["WhatIsTheCallResult"].format(), method.toString()),
            parameters.multipleChoice(language),
            language = language
        )
    }
}