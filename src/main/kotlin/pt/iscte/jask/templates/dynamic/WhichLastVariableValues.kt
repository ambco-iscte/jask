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
            values: List<IValue>,
            variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
            arguments: List<IValue>,
            language: Language
        ): Map<Option, Boolean> {

            val options = mutableListOf(
                SimpleTextOption(variableHistory.map { "${it.key.id} = ${it.value.last()}" }.joinToString()) to true,
            )
            repeat(variableHistory.size-1) {
                options.add(SimpleTextOption(variableHistory.map { "${it.key.id} = ${variableHistory.values.random().last()}" }.joinToString()) to false)
            }
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

        valuesPerVariable.keys.forEach {
            if (it in procedure.parameters)
                valuesPerVariable[it] =
                    listOf(arguments[procedure.parameters.indexOf(it)]) + (valuesPerVariable[it] ?: emptyList())
        }
        val variable = valuesPerVariable.keys.random()
        val values = valuesPerVariable[variable]!!

        val questionTemplate = if(arguments.isEmpty() && procedure.id == "main") language["WhichLastVariableValuesAnonCall"] else language["WhichLastVariableValues"]
        return Question(
            source,
            TextWithCodeStatement(questionTemplate.format(procedureCallAsString(procedure, arguments)), procedure),
            options(values, valuesPerVariable, arguments, language),
            language = language,
            relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter { it.target == variable }.map { SourceLocation(it) }
        )
    }
}