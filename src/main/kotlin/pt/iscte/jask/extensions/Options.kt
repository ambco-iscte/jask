package pt.iscte.jask.extensions

import pt.iscte.jask.Language
import pt.iscte.jask.templates.Option
import pt.iscte.jask.templates.SimpleTextOption

fun Int.multipleChoice(language: Language): Map<Option, Boolean> = mapOf(
    SimpleTextOption(this) to true,
    SimpleTextOption(this + 1) to false,
    SimpleTextOption(if (this == 0) 2 else (this - 1)) to false,
    SimpleTextOption.none(language) to false
)

fun Double.multipleChoice(language: Language): Map<Option, Boolean> = mapOf(
    SimpleTextOption(this) to true,
    SimpleTextOption(this + 1) to false,
    SimpleTextOption(if (this == 0.0) 2 else (this - 1)) to false,
    SimpleTextOption.none(language) to false
)

fun Char.multipleChoice(language: Language): Map<Option, Boolean> = mapOf(
    SimpleTextOption(this) to true,
    SimpleTextOption(this + 1) to false,
    SimpleTextOption(this - 1) to false,
    SimpleTextOption.none(language) to false
)

fun Boolean.trueOrFalse(language: Language, literal: Boolean = false): Map<Option, Boolean> =
    if (!literal)
        mapOf(
            SimpleTextOption.yes(language) to this,
            SimpleTextOption.no(language) to !this
        )
    else
        mapOf(
            SimpleTextOption(this.toString()) to this,
            SimpleTextOption((!this).toString()) to !this
        )