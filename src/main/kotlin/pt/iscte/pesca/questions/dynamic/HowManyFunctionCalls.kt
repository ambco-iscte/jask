package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.extensions.getProcedureCalls
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.isSelfContained
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureCall
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyFunctionCalls : StrudelQuestionRandomProcedure() {
    var count: MutableMap<String, Int> = mutableMapOf()

    override fun isApplicable(element: IProcedure): Boolean =
        element.getProcedureCalls().isNotEmpty()

    override fun setup(vm: IVirtualMachine) {
        count.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                val p = procedure.id!!
                if (!vm.callStack.isEmpty) {
                    count[p] = (count[p] ?: 0) + 1
                }
            }
        })
    }

    override fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        vm.execute(procedure, *arguments.toTypedArray())

        val depProcedures = procedure.getProcedureCalls().map { it.procedure }.toSet()

        val randomProcedure = depProcedures.random()
        val correct = count[randomProcedure.id!!] ?: 0

        val distractors = sampleSequentially(3,
            count.values,
            listOf(
                count.values.sum(),
                count.values.sum() + 1,
                count.values.sum() - 1,
                correct + 1,
                correct - 1
            )
        ) { it != correct && it >= 0 }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(correct)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(
                language["HowManyFunctionCalls"].format(randomProcedure.id, call),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language
        )
    }
}