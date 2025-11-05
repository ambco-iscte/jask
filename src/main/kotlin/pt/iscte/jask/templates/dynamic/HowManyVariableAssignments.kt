package pt.iscte.jask.templates.dynamic
import jdk.jfr.Description
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
import kotlin.collections.get
import kotlin.inc

class HowManyVariableAssignments : DynamicQuestionTemplate<IProcedure>() {

    private class HowManyVariableAssignmentsListener(val vm: IVirtualMachine): IVirtualMachine.IListener {
        val countPerVariable = mutableMapOf<IVariableDeclaration<*>, Int>()
        var iterations = 0

        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if (!vm.callStack.isEmpty)
                countPerVariable[a.target] = (countPerVariable[a.target] ?: 0) + 1
        }

        override fun loopIteration(loop: ILoop) {
            if (!vm.callStack.isEmpty)
                iterations++
        }
    }

    @Description("Procedure must contain at least 1 variable which takes 2 or more values")
    override fun isApplicable(element: IProcedure): Boolean =
        element.getVariableAssignments().any { it.value.isNotEmpty() }

    // There is at least one variable that is assigned multiple times.
    override fun isApplicable(element: IProcedure, args: List<IValue>): Boolean {
        val vm = IVirtualMachine.create()
        val listener = HowManyVariableAssignmentsListener(vm)
        vm.addListener(listener)

        vm.execute(element, *args.toTypedArray())

        return listener.countPerVariable.any { it.value > 1 }
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        val listener = HowManyVariableAssignmentsListener(vm)
        vm.addListener(listener)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())

        val variable = listener.countPerVariable.keys.random()
        val count = listener.countPerVariable[variable]!!

        val variablesPerCount = mutableMapOf<Int, List<IVariableDeclaration<*>>>()
        listener.countPerVariable.forEach { (variable, count) ->
            variablesPerCount[count] = (variablesPerCount[count] ?: emptyList()).plus(variable)
        }

        val options = correctAndRandomDistractors(
    count to language["HowManyVariableAssignments_Correct"].format("a", "a = ...;"), (
            setOf(count + 1 to null, count - 1 to null, listener.iterations to null) +
                    variablesPerCount.map { (count, variables) ->
                        count to language["HowManyVariableAssignments_DistractorWrongVariable"].format(
                            variables.joinToString { it.id.toString() },
                            variable.id
                        )
                    }
            ).toMap()
        ).toMutableMap()

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        val statement = language["HowManyVariableAssignments"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(variable.id, procedureCallAsString(procedure, args)),
                procedure
            ),
            options,
            language
        )
    }
}