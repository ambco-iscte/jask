package compiler

import org.junit.jupiter.api.Test
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.compiler.templates.ReferencesUndefinedVariable
import kotlin.test.assertEquals

class TestReferencesUndefinedVariable {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                int a = 1;
                int b = 2;
                int c = d;
            }
        """.trimIndent()

        val qlc = ReferencesUndefinedVariable()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(3, data.solution.size)
        assertEquals(listOf("a", "b", "c"), data.solution.map { it.toString() })
    }
}