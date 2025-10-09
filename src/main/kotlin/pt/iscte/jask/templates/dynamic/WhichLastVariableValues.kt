package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getVariableAssignments
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.*
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.collections.set

class WhichLastVariableValues() : DynamicQuestionTemplate<IProcedure>() {

    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

    companion object {
        fun options(
            vm: IVirtualMachine,
            variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
            procedure: IProcedure,
            arguments: List<Any?>,
            language: Language
        ): Map<Option, Boolean> {

            val argumentsAndLiterals = arguments.toMutableSet()
            procedure.block.accept(object : IBlock.IVisitor {
                override fun visit(exp: ILiteral) {
                    argumentsAndLiterals.add(vm.getValue(exp.stringValue))
                }
            })

            val varHist = variableHistory.toMutableMap()

            procedure.localVariables.forEach {
                if(!varHist.containsKey(it))
                    varHist[it] = listOf()
            }

            val options = mutableListOf(
                SimpleTextOption(varHist.map { "${it.key.id} = ${it.value.lastOrNull() ?: "indefinido" }" }.joinToString()) to true,
            )

            val values = varHist.map { it.value.lastOrNull() }

            val shuffled = mutableSetOf<List<IValue?>>()
            repeat(4) {
                val s = values.shuffled()
                if (s != values)
                    shuffled.add(s)
            }

            shuffled.forEach {
                val o = varHist.keys.zip(it).joinToString { "${it.first.id} = ${it.second ?: "indefinido"}" }
                options.add(SimpleTextOption(o) to false)
            }

            repeat(2) {
                if (options.size < 4) {
                    val text = varHist.map { "${it.key.id} = ${argumentsAndLiterals.randomOrNull() ?: "indefinido"}" }.joinToString()
                    if(options.none { it.first.text == text })
                         options.add(SimpleTextOption(text) to false)
                }
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
                statement.format(procedureCallAsString(procedure, args)),
                procedure
            ),
            options(vm, valuesPerVariable, procedure, args, language),
            language = language
        )
    }
}