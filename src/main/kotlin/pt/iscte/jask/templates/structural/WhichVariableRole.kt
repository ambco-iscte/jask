package pt.iscte.jask.templates.structural

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.templates.DynamicQuestionTemplate
import pt.iscte.jask.templates.Option
import pt.iscte.jask.templates.Question
import pt.iscte.jask.templates.SimpleTextOption
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.TextWithCodeStatement
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.roles.IVariableRole
import pt.iscte.strudel.model.roles.impl.ArrayIndexIterator
import pt.iscte.strudel.model.roles.impl.FixedValue
import pt.iscte.strudel.model.roles.impl.Gatherer
import pt.iscte.strudel.model.roles.impl.MostWantedHolder
import pt.iscte.strudel.model.roles.impl.Stepper
import kotlin.reflect.KClass

class WhichVariableRole : DynamicQuestionTemplate<IProcedure>() {

    // There is at least one variable whose role can be determined.
    override fun isApplicable(element: IProcedure): Boolean =
        element.localVariables.any { IVariableRole.match(it) != IVariableRole.NONE }

    private fun variableRoles(language: Language): Map<KClass<out IVariableRole>, String> = mapOf(
        FixedValue::class to language["FixedValue"].toString(),
        Gatherer::class to language["Gatherer"].toString(),
        ArrayIndexIterator::class to language["ArrayIndexIterator"].toString(),
        Stepper::class to language["Stepper"].toString(),
        MostWantedHolder::class to language["MostWantedHolder"].toString(),
        // OneWayFlag::class to language["OneWayFlag"].toString()
    )

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        // Choose a random variable whose role can be determined.
        // Per the precondition, there is guaranteed to be at least one.
        val variable = procedure.localVariables.filter {
            IVariableRole.match(it) != IVariableRole.NONE
        }.random()

        // Determine that variable's role.
        val varRoles = variableRoles(language)
        val role = IVariableRole.match(variable)
        val roleName = varRoles[role::class]!!

        // Generate fancy options. :)
        val options: MutableMap<Option, Boolean> = varRoles.keys.filter { varRoles[it]!! != roleName }.sample(4).associate {
            SimpleTextOption(varRoles[it]!!) to false
        }.toMutableMap()
        options[SimpleTextOption(roleName)] = true

        val statement = language["WhichVariableRole"].orAnonymous(emptyList(), procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(variable.id, procedure.id),
                procedure
            ),
            options,
            language = language
        )
    }
}