package pt.iscte.jask.templates.dynamic
import jdk.jfr.Description
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getVariableAssignments
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
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
        ): Map<QuestionOption, Boolean> {

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

            val options = mutableListOf<Pair<SimpleTextOption, Boolean>>()
            if (varHist.isNotEmpty()) {
                options.add(SimpleTextOption(varHist.map {
                    "${it.key.id} = ${it.value.lastOrNull() ?: "indefinido"}"
                }.joinToString()) to true)
            }

            val values = varHist.map { it.value.lastOrNull() }

            val shuffled = mutableSetOf<List<IValue?>>()
            repeat(4) {
                val s = values.shuffled()
                if (s != values && s.isNotEmpty())
                    shuffled.add(s)
            }

            shuffled.forEach {
                if (it.isNotEmpty()) {
                    val o = varHist.keys.zip(it).joinToString { "${it.first.id} = ${it.second ?: "indefinido"}" }
                    options.add(SimpleTextOption(o) to false)
                }
            }

            repeat(2) {
                if (options.size < 4) {
                    val text = varHist.map { "${it.key.id} = ${argumentsAndLiterals.randomOrNull() ?: "indefinido"}" }.joinToString()
                    if(options.none { it.first.text == text })
                         options.add(SimpleTextOption(text) to false)
                }
            }

            if(options.size < 4)
                options.add(SimpleTextOption.none(language) to varHist.isEmpty())

            return options.toMap()
        }
    }

    @Description("Procedure must contain at least 1 local variable assignment")
    override fun isApplicable(element: IProcedure): Boolean =
        element.localVariables.isNotEmpty() && element.getVariableAssignments().isNotEmpty()

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