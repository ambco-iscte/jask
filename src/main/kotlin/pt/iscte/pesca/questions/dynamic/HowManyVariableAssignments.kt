package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.getVariableAssignments
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyVariableAssignments : StrudelQuestionRandomProcedure() {

    val countPerVariable = mutableMapOf<IVariableDeclaration<*>, Int>()

    // There is at least one variable that is assigned multiple times.
    override fun isApplicable(element: IProcedure): Boolean =
        element.getVariableAssignments().any { it.value.size > 1 }

    override fun setup(vm: IVirtualMachine) {
        countPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                countPerVariable[a.target] = (countPerVariable[a.target] ?: 0) + 1
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

        val declarations = procedure.getVariableAssignments().keys.size
        val variable = countPerVariable.keys.random()
        val count = countPerVariable[variable]!!

        return QuestionData(
            TextWithCodeStatement(language["HowManyVariableAssignments"].format(variable.id, call), procedure),
            mapOf(
                SimpleTextOption(count) to true,
                SimpleTextOption(count+1) to false,
                SimpleTextOption(if(declarations != count) declarations else count-1) to false,
                (if (count != 0) SimpleTextOption(0) else SimpleTextOption.none(language)) to false,
            ),
            language = language
        )
    }
}