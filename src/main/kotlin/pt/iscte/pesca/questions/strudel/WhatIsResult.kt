package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getLiteralExpressions
import pt.iscte.pesca.extensions.procedureCallAsString
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.extensions.toIValues
import pt.iscte.strudel.model.ILiteral
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class WhatIsResult: DynamicQuestion<IProcedure>() {

    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

    fun setup(vm: IVirtualMachine) {
        valuesPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                valuesPerVariable[a.target] = (valuesPerVariable[a.target] ?: emptyList()) + listOf(value)
            }
        })
    }

    // Return type is a value.
    override fun isApplicable(element: IProcedure): Boolean =
        element.returnType.isValueType

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        val result = vm.execute(procedure, *arguments.toTypedArray())!!

        val returnLiterals = procedure.findAll(IReturn::class).filter {
            it.expression != null && it.expression!! is ILiteral
        }.map {
            vm.getValue((it.expression!! as ILiteral).stringValue)
        }

        val returnExpressions = procedure.findAll(IReturn::class).filter {
            it.expression != null
        }.map { it.expression.toString() }

        val literalExpressions = procedure.getLiteralExpressions().map { vm.getValue(it.stringValue) }

        val lastVariableValues = valuesPerVariable.values.map { it.last() }

        val distractors = sampleSequentially(3, returnLiterals, literalExpressions, lastVariableValues, arguments) {
            it.value != result.value && it.type == procedure.returnType
        }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it) to false
        }.toMutableMap()
        options[SimpleTextOption(result)] = true
        if (options.size < 4) {
            returnExpressions.sample(4 - options.size).forEach {
                if (it.toString() != result.toString())
                    options[SimpleTextOption(it)] = false
            }
        }
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            source,
            TextWithCodeStatement(language["WhatIsResult"].format(procedureCallAsString(procedure, arguments)), procedure),
            options,
            language = language
        )
    }
}