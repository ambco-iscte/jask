package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.CallsOtherFunctions

class TestCallsOtherFunctions {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static void foo() { bar(); }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = CallsOtherFunctions().generate(src1)
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("Yes.", qlc.solution[0].toString())
        }

        val src2 = """
            class Test {
                static void foo() { int n = 3; }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = CallsOtherFunctions().generate(src2)
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("No.", qlc.solution[0].toString())
        }

        val src3 = """
            import java.lang.Math;
            
            class Test {
                static int random() {
                    return (int) (Math.random() * 1000000);
                }
            }
        """.trimIndent()
        assertDoesNotThrow {
            val qlc = CallsOtherFunctions().generate(src3)
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("Yes.", qlc.solution[0].toString())
        }
    }

    @Test
    fun testNotApplicable() {
        val src2 = """
            class Test {
                
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            CallsOtherFunctions().generate(src2)
        }
    }
}