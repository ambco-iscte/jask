package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.extensions.toIValue
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.text.format

data class WhatIsResult(val methodName: String? = null): DynamicQuestion<IProcedure>() {

    // Return type is a value.
    override fun isApplicable(element: IProcedure): Boolean =
        element.nameMatches(methodName) && element.returnType.isValueType

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
        val arguments = callsForProcedure.random().arguments.random().map { it.toIValue(vm, module) }.toTypedArray()

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