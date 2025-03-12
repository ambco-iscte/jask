package compiler

import pt.iscte.pesca.Localisation
import pt.iscte.pesca.templates.AssignVarWithMethodWrongType
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

        val qlc = AssignVarWithMethodWrongType()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("String", data.solution.first().toString())
    }
}