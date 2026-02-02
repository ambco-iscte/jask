package pt.iscte.jask.common

import pt.iscte.jask.Language

sealed interface QuestionOption {
    val feedback: String?
        get() = null
}

data class SimpleTextOption(val text: String, override val feedback: String? = null): QuestionOption {

    companion object {
        fun none(language: Language = Language.DEFAULT, feedback: String? = null): SimpleTextOption =
            SimpleTextOption(language["NoneOfTheAbove"], feedback)

        fun all(language: Language = Language.DEFAULT, feedback: String? = null): SimpleTextOption =
            SimpleTextOption(language["AllOfTheAbove"], feedback)

        fun yes(language: Language = Language.DEFAULT, feedback: String? = null): SimpleTextOption =
            SimpleTextOption(language["Yes"], feedback)

        fun no(language: Language = Language.DEFAULT, feedback: String? = null): SimpleTextOption =
            SimpleTextOption(language["No"], feedback)
    }

    constructor(value: Any?, feedback: String? = null): this(when (value) {
        is Collection<*> -> value.joinToString()
        else -> value.toString()
    }, feedback)

    override fun toString() = text
}

enum class QuestionChoiceType {
    SINGLE,
    MULTIPLE
}