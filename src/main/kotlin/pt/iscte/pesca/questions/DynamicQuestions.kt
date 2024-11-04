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
import pt.iscte.pesca.extensions.toIValue
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.roles.IVariableRole
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

data class WhatIsResult(val methodName: String? = null): DynamicQuestion<IProcedure>() {

    // Return type is a value.
    override fun isApplicable(element: IProcedure): Boolean =
        element.returnType.isValueType

    override fun build(sources: List<SourceCodeWithInput>, language: Language): QuestionData {
        // Of the provided source code(s), find a random one which is applicable to this question type.
        val source = getApplicableSources<IProcedure>(sources).randomOrNull()
            ?: throw RuntimeException("Could not find an applicable source!")

        // Load the chosen source code.
        val module = Java2Strudel().load(source.source.code)

        // Of the chosen source code, find a random procedure which is applicable.
        // Can reuse the isApplicable(IProcedure) method which is already implemented as per the interface.
        val procedure = module.procedures.filterIsInstance<IProcedure>().filter { isApplicable(it) }.randomOrNull() ?:
            throw RuntimeException(
                "Could not find procedure with value type and value type parameters in module ${module.id}!"
            )

        val vm = IVirtualMachine.create()

        // Out of the provided sets of arguments for this source code, choose a random one which fits the
        // chosen procedure. If none can be found... well, the user should've specified it. Tell them to try again!
        val callsForProcedure = source.calls.filter { it.id == procedure.id }
        if (callsForProcedure.isEmpty())
            throw RuntimeException("Could not find procedure call specification for procedure ${procedure.id}.")
        val arguments = callsForProcedure.random().arguments.random().map { it.toIValue(vm) }.toTypedArray()

        val call = "${procedure.id}(${arguments.joinToString()})"

        // Execute the procedure with the chosen arguments.
        val result = vm.execute(procedure, *arguments)!! // TODO !!

        return QuestionData(
            TextWithCodeStatement(language["WhatIsResult"].format(call), procedure),
            result.multipleChoice(language),
            language = language
        )
    }
}

data class WhichVariableRole(val methodName: String? = null): DynamicQuestion<IProcedure>() {

    // There is at least one variable whose role can be determined.
    override fun isApplicable(element: IProcedure): Boolean =
        element.localVariables.any {
            IVariableRole.match(it) != IVariableRole.NONE
        }

    override fun build(sources: List<SourceCodeWithInput>, language: Language): QuestionData {
        // Of the provided source code(s), find a random one which is applicable to this question type.
        val source = getApplicableSources<IProcedure>(sources).randomOrNull()
            ?: throw RuntimeException("Could not find an applicable source!")

        // Load the chosen source code.
        val module = Java2Strudel().load(source.source.code)

        // Of the chosen source code, find a random procedure which is applicable.
        // Can reuse the isApplicable(IProcedure) method which is already implemented as per the interface.
        val procedure = module.procedures.filterIsInstance<IProcedure>().filter { isApplicable(it) }.randomOrNull() ?:
            throw RuntimeException(
                "Could not find procedure within module ${module.id} where at least one variable role can be determined!"
            )

        // Choose a random variable whose role can be determined.
        // As per the precondition, there is guaranteed to be at least one.
        val variable = procedure.localVariables.filter { IVariableRole.match(it) != IVariableRole.NONE }.random()

        // Determine that variable's role.
        val role = IVariableRole.match(variable)
        val roleName = VARIABLE_ROLES[role::class]!!

        // Generate fancy options. :)
        val options: MutableMap<Option, Boolean> = VARIABLE_ROLES.keys.minus(role::class).sample(3).associate {
            SimpleTextOption(VARIABLE_ROLES[it]!!) to false
        }.toMutableMap()
        options[SimpleTextOption(roleName)] = true

        return QuestionData(
            TextWithCodeStatement(language["WhichVariableRole"].format(variable.id, procedure.id), procedure),
            options,
            language = language
        )
    }
}