package compiler

import org.junit.jupiter.api.Test
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.questions.compiler.MethodWithWrongReturnStmt
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

        val qlc = MethodWithWrongReturnStmt()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("int", data.solution.first().toString())
    }
}