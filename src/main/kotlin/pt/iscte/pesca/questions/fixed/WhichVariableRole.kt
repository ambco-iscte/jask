package pt.iscte.pesca.questions.fixed

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.VARIABLE_ROLES
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.roles.IVariableRole
import pt.iscte.strudel.parsing.java.Java2Strudel
import kotlin.text.format

data class WhichVariableRole(val methodName: String? = null): DynamicQuestion<IProcedure>() {

    // There is at least one variable whose role can be determined.
    override fun isApplicable(element: IProcedure): Boolean =
        element.nameMatches(methodName) && element.localVariables.any {
            IVariableRole.match(it) != IVariableRole.NONE
        }

    override fun build(sources: List<SourceCodeWithInput>, language: Language): QuestionData {
        // Of the provided source code(s), find a random one which is applicable to this question type.
        val source = getApplicableSources<IProcedure>(sources).randomOrNull()
            ?: throw RuntimeException("Could not find an applicable source!")

        // Load the chosen source code.
        val module = Java2Strudel().load(source.source.code)

        // Of the chosen source code, find a random procedure which is applicable.
        // Can reuse the isApplicable(IProcedure) method which is already implemented as per the interface.
        val procedure = module.procedures.filterIsInstance<IProcedure>().filter { isApplicable(it) }.randomOrNull() ?:
        throw RuntimeException(
            "Could not find procedure within module ${module.id} where at least one variable role can be determined!"
        )

        // Choose a random variable whose role can be determined.
        // As per the precondition, there is guaranteed to be at least one.
        val variable = procedure.localVariables.filter { IVariableRole.match(it) != IVariableRole.NONE }.random()

        // Determine that variable's role.
        val role = IVariableRole.match(variable)
        val roleName = VARIABLE_ROLES[role::class]!!

        // Generate fancy options. :)
        val options: MutableMap<Option, Boolean> = VARIABLE_ROLES.keys.minus(role::class).sample(3).associate {
            SimpleTextOption(VARIABLE_ROLES[it]!!) to false
        }.toMutableMap()
        options[SimpleTextOption(roleName)] = true

        return QuestionData(
            TextWithCodeStatement(language["WhichVariableRole"].format(variable.id, procedure.id), procedure),
            options,
            language = language
        )
    }
}