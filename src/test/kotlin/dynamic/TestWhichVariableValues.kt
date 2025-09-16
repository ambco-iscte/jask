package dynamic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.dynamic.WhichVariableValues
import pt.iscte.jask.templates.invoke

class TestWhichVariableValues {

    @Test
    fun testApplicable() {
        val src = """
            class Test {
                static int foo(int n) {
                    int m = n;
                    m = m + 1;
                    m = m + 2;
                    m = m + 3;
                    return m;
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = WhichVariableValues().generate(src, "foo"(5))
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("5, 6, 8, 11", qlc.solution[0].toString())
        }
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static int next(int n) {
                    return n + 1;
                }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            WhichVariableValues().generate(src, "next"(5))
        }
    }
}