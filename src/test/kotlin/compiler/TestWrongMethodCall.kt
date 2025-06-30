package compiler

import org.junit.jupiter.api.Test
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.compiler.templates.CallMethodWithWrongParameterNumber
import pt.iscte.jask.errors.compiler.templates.CallMethodWithWrongParameterTypes
import kotlin.test.assertEquals

class TestWrongMethodCall {

    @Test
    fun argumentCount() {
        val src = """
            class HelloWorld {
                int x = average(1, 2, 3);
            
                double average(int a, int b) {
                    return (a + b) / 2.0;
                }
            }
        """.trimIndent()

        val qlc = CallMethodWithWrongParameterNumber()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("2", data.solution.first().toString())
    }

    @Test
    fun argumentType() {
        val src = """
            class HelloWorld {
                int x = neg("hello");
            
                int neg(int a) {
                    return -a;
                }
            }
        """.trimIndent()

        val qlc = CallMethodWithWrongParameterTypes()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("int", data.solution.first().toString())
    }
}