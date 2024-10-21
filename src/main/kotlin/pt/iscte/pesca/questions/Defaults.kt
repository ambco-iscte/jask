package pt.iscte.pt.iscte.pesca.questions

import pt.iscte.pt.iscte.pesca.Language

// Options that are always below the others
val NONE_OF_THE_ABOVE = SimpleTextOptionData(
    Language.ENGLISH to "None of the above.",
    Language.PORTUGUESE to  "Nenhuma das anteriores."
)

val ALL_OF_THE_ABOVE = SimpleTextOptionData(
    Language.ENGLISH to "All of the above.",
    Language.PORTUGUESE to "Todas as anteriores."
)

val YES = SimpleTextOptionData(
    Language.ENGLISH to "Yes",
    Language.PORTUGUESE to "Sim"
)

val NO = SimpleTextOptionData(
    Language.ENGLISH to "No",
    Language.PORTUGUESE to "Não"
)

val LAST_UNSHUFFLED_OPTIONS: MutableList<OptionData> =
    mutableListOf(NONE_OF_THE_ABOVE, ALL_OF_THE_ABOVE, YES, NO)