package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getVariableAssignments
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.collections.set

class WhichLastVariableValues() : DynamicQuestionTemplate<IProcedure>() {

    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

    companion object {
        fun options(
            variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
            arguments: List<IValue>,
            language: Language
        ): Map<Option, Boolean> {

            val options = mutableListOf(
                SimpleTextOption(variableHistory.map { "${it.key.id} = ${it.value.last()}" }.joinToString()) to true,
            )

            val values = variableHistory.map { it.value.last() }
            val shuffled = mutableSetOf<List<IValue>>()
            repeat(4) {
                val s = values.shuffled()
                if (s != values)
                    shuffled.add(s)
            }

            shuffled.forEach {
                val o = variableHistory.keys.zip(it).joinToString { "${it.first.id} = ${it.second}" }
                options.add(SimpleTextOption(o) to false)
            }

            if(options.size < 4)
                options.add(SimpleTextOption.none(language) to false)

            return options.toMap()
        }
    }

    override fun isApplicable(element: IProcedure): Boolean =
        element.localVariables.size > 1 && element.getVariableAssignments().isNotEmpty()

    fun setup(vm: IVirtualMachine) {
        valuesPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                valuesPerVariable[a.target] = (valuesPerVariable[a.target] ?: emptyList()) + listOf(value)
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)
        vm.execute(procedure, *arguments.toTypedArray())

        val statement = language["WhichLastVariableValues"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, arguments)),
                procedure
            ),
            options(valuesPerVariable, arguments, language),
            language = language
        )
    }
}