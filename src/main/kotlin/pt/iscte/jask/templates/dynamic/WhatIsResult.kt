package pt.iscte.jask.templates.dynamic
import com.github.javaparser.ast.expr.EnclosedExpr
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLiteralExpressions
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.model.BOOLEAN
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.ILiteral
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class WhatIsResult: DynamicQuestionTemplate<IProcedure>() {

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

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        var result = vm.execute(procedure, *arguments.toTypedArray())!!

        // Ugly edge case (works for now)
        if (result.type == DOUBLE && result.value == -0.0)
            result = vm.getValue(0.0)

        val returnLiterals: List<Pair<IValue, String?>> = procedure.findAll(IReturn::class).filter {
            it.expression != null && it.expression!! is ILiteral
        }.map {
            vm.getValue((it.expression!! as ILiteral).stringValue) to null
        }

        val returnExpressions: List<Pair<String, String?>> = procedure.findAll(IReturn::class).filter {
            it.expression != null
        }.map {
            val exp = it.expression.toString()
            exp to language["WhatIsResult_DistractorReturnExpression"].format(procedure.returnType.id, exp)
        }

        val literalExpressions: List<Pair<IValue, String?>> = procedure.getLiteralExpressions().map {
            vm.getValue(it.stringValue) to null
        }

        val lastVariableValues: List<Pair<IValue, String?>> = valuesPerVariable.map { (variable, values) ->
            values.last() to language["WhatIsResult_DistractorLastVariableValue"].format(variable.id)
        }

        val distractors: Set<Pair<Any?, String?>> = sampleSequentially(3,
            returnLiterals,
            literalExpressions,
            lastVariableValues,
            arguments.map { it to null },
            returnExpressions
        ) {
            if (it.first is IValue)
                (it.first as IValue).value != result.value && (it.first as IValue).type == procedure.returnType
            else
                it.first.toString() != result.toString()
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()
        options[SimpleTextOption(result)] = true

        /*
        if (options.size < 4) {
            returnExpressions.sample(4 - options.size).forEach {
                if (it.first != result.toString())
                    options[SimpleTextOption(it.first, it.second)] = false
            }
        }
         */

        if (options.size < 4 && result.type == BOOLEAN)
            options[SimpleTextOption(!result.toBoolean())] = false

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["WhatIsResult"].format(procedureCallAsString(procedure, args)),
                procedure
            ),
            options,
            language = language
        )
    }
}

fun main() {
    val source = """
        class Test {
            static double neg(double n) {
                return -n;
            }
        }
    """.trimIndent()

    val template = WhatIsResult()
    val qlc = template.generate(source, ProcedureCall("neg", listOf(0.0)))
    println(qlc)
}