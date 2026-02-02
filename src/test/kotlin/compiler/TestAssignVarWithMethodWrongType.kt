package compiler

import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.compiler.templates.WhichMethodCallReturnType
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAssignVarWithMethodWrongType {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                int x = foo();
                
                String foo() {
                    return "bar";
                }
            }
        """.trimIndent()

        val qlc = WhichMethodCallReturnType()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("String", data.solution.first().toString())
    }
}