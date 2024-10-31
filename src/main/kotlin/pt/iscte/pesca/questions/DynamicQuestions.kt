package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.generateRandomValue
import pt.iscte.pesca.extensions.hasLoopControlStructures
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.strudel.model.VOID
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import pt.iscte.strudel.vm.IVirtualMachine

data class WhatIsResult(val methodName: String? = null): DynamicQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName) &&
        element.type.isPrimitiveType && element.parameters.all { it.type.isPrimitiveType }

    override fun build(sources: List<String>, language: Language): QuestionData {
        val source = getApplicableSources<MethodDeclaration>(sources).randomOrNull()
            ?: throw RuntimeException("Could not find an applicable source!")

        val module = Java2Strudel().load(source)
        val procedure = module.procedures.filter { p ->
            p.returnType.isValueType && p.parameters.all { it.type.isValueType }
        }.randomOrNull() ?:
            throw RuntimeException("Could not find procedure with value type and value type parameters in module ${module.id}!")

        val vm = IVirtualMachine.create()
        val parameterTypes = procedure.parameters.map { it.type }

        val arguments = parameterTypes.map { it.generateRandomValue(vm) }.toTypedArray()
        val call = "${procedure.id}(${arguments.joinToString()})"

        val result = vm.execute(procedure, *arguments)!! // TODO !!

        return QuestionData(
            TextWithCodeStatement(language["WhatIsResult"].format(call), procedure),
            result.multipleChoice(language),
            language = language
        )
    }
}