package compiler

import org.junit.jupiter.api.Test
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.compiler.templates.WhichWrongReturnStmtTypeMethodReturnType
import kotlin.test.assertEquals

class TestMethodWithWrongReturnStmt {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                int foo() {
                    return "bar";
                }
            }
        """.trimIndent()

        val qlc = WhichWrongReturnStmtTypeMethodReturnType()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("int", data.solution.first().toString())
    }
}