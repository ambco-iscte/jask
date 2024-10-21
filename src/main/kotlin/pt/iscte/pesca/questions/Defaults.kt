package pt.iscte.pt.iscte.pesca.questions

import pt.iscte.pt.iscte.pesca.Language.ENGLISH
import pt.iscte.pt.iscte.pesca.Language.PORTUGUESE

// Options that are always below the others
val NONE_OF_THE_ABOVE = SimpleTextOptionData(
    ENGLISH to "None of the above.",
    PORTUGUESE to  "Nenhuma das anteriores."
)

val ALL_OF_THE_ABOVE = SimpleTextOptionData(
    ENGLISH to "All of the above.",
    PORTUGUESE to "Todas as anteriores."
)

val YES = SimpleTextOptionData(
    ENGLISH to "Yes",
    PORTUGUESE to "Sim"
)

val NO = SimpleTextOptionData(
    ENGLISH to "No",
    PORTUGUESE to "Não"
)

val LAST_UNSHUFFLED_OPTIONS: MutableList<OptionData> =
    mutableListOf(NONE_OF_THE_ABOVE, ALL_OF_THE_ABOVE, YES, NO)