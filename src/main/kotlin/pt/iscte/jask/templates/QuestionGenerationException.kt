package pt.iscte.jask.templates

import pt.iscte.jask.common.SourceCode
import kotlin.reflect.KClass

abstract class QuestionGenerationException(
    open val template: QuestionTemplate<*>
) : Exception()

/**
 * Thrown when a QLC [template] could not find any applicable sources from the given list.
 * @property errors For each source code, the error that prevented the source from being used (null if no error occurred for that [pt.iscte.jask.common.SourceCode]).
 */
class ApplicableSourceNotFoundException(
    override val template: QuestionTemplate<*>,
    val errors: Map<SourceCode, Throwable?>
): QuestionGenerationException(template) {

    override val message: String
        get() {
            val messages = errors.mapNotNull { it.value?.message?.ifEmpty { null } }.toSet()
            var message = "Could not find any applicable sources for QLC of type ${template::class.simpleName}."
            if (messages.isNotEmpty())
                message += " ${messages.joinToString("; ")}"
            return message
        }
}

/**
 * Thrown when a QLC [template] could not find, within any of the given sources, an applicable element.
 * @property errors For each source code, the error which caused no applicable elements to be found (null if no error occurred for that [SourceCode]).
 * @property elementType Expected element type, i.e. element typed targeted by the [template].
 */
class ApplicableElementNotFoundException(
    override val template: QuestionTemplate<*>,
    val errors: Map<SourceCode, Throwable?>,
    val elementType: KClass<*>
): QuestionGenerationException(template) {

    override val message: String
        get() {
            val messages = errors.mapNotNull { it.value?.message?.ifEmpty { null } }.toSet()
            var message = "Could not find any applicable elements of type ${elementType.simpleName} for QLC of type ${template::class.simpleName}."
            if (messages.isNotEmpty())
                message += " ${messages.joinToString("; ")}"
            return message
        }
}

/**
 * Thrown when a dynamic QLC [template] could not find, for any source or test case, an applicable procedure call.
 * @property errors For each source code, the list of errors which caused each procedure call to not be applicable (empty if no errors occurred for that [SourceCode]).
 */
class ApplicableProcedureCallNotFoundException(
    override val template: QuestionTemplate<*>,
    val errors: Map<SourceCode, List<Throwable>>,
): QuestionGenerationException(template) {

    override val message: String
        get() {
            val messages = errors.flatMap { it.value.mapNotNull { e -> e.message?.ifEmpty { null } } }.toSet()
            var message = "Could not find any applicable procedure calls for QLC of type ${template::class.simpleName}."
            if (messages.isNotEmpty())
                message += " ${messages.joinToString("; ")}"
            return message
        }
}