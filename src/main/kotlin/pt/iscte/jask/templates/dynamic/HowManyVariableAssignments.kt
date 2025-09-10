package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.correctAndRandomDistractors
import pt.iscte.jask.extensions.getVariableAssignments
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyVariableAssignments : DynamicQuestionTemplate<IProcedure>() {

    val countPerVariable = mutableMapOf<IVariableDeclaration<*>, Int>()
    var iterations = 0

    // There is at least one variable that is assigned multiple times.
    override fun isApplicable(element: IProcedure): Boolean =
        element.getVariableAssignments().any { it.value.size > 1 }

    fun setup(vm: IVirtualMachine) {
        countPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                countPerVariable[a.target] = (countPerVariable[a.target] ?: 0) + 1
            }

            override fun loopIteration(loop: ILoop) {
                iterations++
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())

        val variable = countPerVariable.keys.random()
        val count = countPerVariable[variable]!!

        val statement = language["HowManyVariableAssignments"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(variable.id, procedureCallAsString(procedure, arguments)),
                procedure
            ),
            correctAndRandomDistractors(
                count to language["HowManyVariableAssignments_Correct"].format("x", "x = ...;"),
                (
                    setOf(count + 1 to null, count - 1 to null, iterations to null) +
                    countPerVariable.map {
                        it.value to language["HowManyVariableAssignments_DistractorWrongVariable"].format(
                            it.key.id,
                            variable.id
                        )
                    }
                ).toMap()
            ),
            language
        )
    }
}