package pt.iscte.pt.iscte.pesca

// Supported Languages
enum class Language(private val code: String) {
    PORTUGUESE("pt"),
    ENGLISH("en");

    companion object {
        val DEFAULT = ENGLISH
    }
}