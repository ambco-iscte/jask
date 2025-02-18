package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.roles.IVariableRole
import pt.iscte.strudel.model.roles.impl.ArrayIndexIterator
import pt.iscte.strudel.model.roles.impl.FixedValue
import pt.iscte.strudel.model.roles.impl.Gatherer
import pt.iscte.strudel.model.roles.impl.MostWantedHolder
import pt.iscte.strudel.model.roles.impl.OneWayFlag
import pt.iscte.strudel.model.roles.impl.Stepper
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.reflect.KClass

class WhichVariableRole : StrudelQuestionRandomProcedure() {

    // There is at least one variable whose role can be determined.
    override fun isApplicable(element: IProcedure): Boolean =
        element.localVariables.any { IVariableRole.Companion.match(it) != IVariableRole.Companion.NONE }

    private fun variableRoles(language: Language): Map<KClass<out IVariableRole>, String> = mapOf(
        FixedValue::class to language["FixedValue"].toString(),
        Gatherer::class to language["Gatherer"].toString(),
        ArrayIndexIterator::class to language["ArrayIndexIterator"].toString(),
        Stepper::class to language["Stepper"].toString(),
        MostWantedHolder::class to language["MostWantedHolder"].toString(),
        // OneWayFlag::class to language["OneWayFlag"].toString()
    )

    override fun build(
        source: SourceCode,
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        // Choose a random variable whose role can be determined.
        // As per the precondition, there is guaranteed to be at least one.
        val variable = procedure.localVariables.filter {
            IVariableRole.Companion.match(it) != IVariableRole.Companion.NONE
        }.random()

        // Determine that variable's role.
        val varRoles = variableRoles(language)
        val role = IVariableRole.Companion.match(variable)
        val roleName = varRoles[role::class]!!

        // Generate fancy options. :)
        val options: MutableMap<Option, Boolean> = varRoles.keys.filter { varRoles[it]!! != roleName }.sample(4).associate {
            SimpleTextOption(varRoles[it]!!) to false
        }.toMutableMap()
        options[SimpleTextOption(roleName)] = true

        return QuestionData(
            source,
            TextWithCodeStatement(language["WhichVariableRole"].format(variable.id, procedure.id), procedure),
            options,
            language = language
        )
    }
}