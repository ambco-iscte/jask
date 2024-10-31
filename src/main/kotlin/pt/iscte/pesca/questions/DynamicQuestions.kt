package pt.iscte.pesca.questions

import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.VARIABLE_ROLES
import pt.iscte.pesca.extensions.generateRandomArguments
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.extensions.pascalCaseToSpaces
import pt.iscte.pesca.extensions.sample
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.roles.IVariableRole
import pt.iscte.strudel.parsing.java.Java2Strudel
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

        val arguments = procedure.generateRandomArguments(vm).toTypedArray()
        val call = "${procedure.id}(${arguments.joinToString()})"

        val result = vm.execute(procedure, *arguments)!! // TODO !!

        return QuestionData(
            TextWithCodeStatement(language["WhatIsResult"].format(call), procedure),
            result.multipleChoice(language),
            language = language
        )
    }
}

data class WhichVariableRole(val methodName: String? = null): DynamicQuestion<MethodDeclaration>() {

    private fun IProcedure.canDetermineVariableRoles(): Boolean =
        localVariables.any {
            IVariableRole.match(it) != IVariableRole.NONE
        }

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val type = element.findAncestor(ClassOrInterfaceDeclaration::class.java).get()
        type.removeModifier(Modifier.Keyword.PUBLIC)
        val module = Java2Strudel().load(type.toString())
        val method = module.getProcedure(element.nameAsString) as IProcedure
        return element.nameMatches(methodName) && method.canDetermineVariableRoles()
    }

    override fun build(sources: List<String>, language: Language): QuestionData {
        val source = getApplicableSources<MethodDeclaration>(sources).randomOrNull()
            ?: throw RuntimeException("Could not find an applicable source!")

        val module = Java2Strudel().load(source)
        val procedure = module.procedures.filterIsInstance<IProcedure>().filter {
            it.canDetermineVariableRoles()
        }.randomOrNull() ?:
            throw RuntimeException("Could not find procedure with in module ${module.id} where at least one variable role can be determined!")

        val variable = procedure.localVariables.filter { IVariableRole.match(it) != IVariableRole.NONE }.random()
        val role = IVariableRole.match(variable)
        val roleName = role::class.simpleName!!

        val options: MutableMap<Option, Boolean> = VARIABLE_ROLES.minus(roleName).sample(3).associate {
            SimpleTextOption(it.pascalCaseToSpaces()) to false
        }.toMutableMap()
        options[SimpleTextOption(roleName)] = true

        return QuestionData(
            TextWithCodeStatement(language["WhichVariableRole"].format(variable.id, procedure.id), procedure),
            options,
            language = language
        )
    }
}