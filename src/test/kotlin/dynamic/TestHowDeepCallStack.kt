package dynamic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.dynamic.HowDeepCallStack
import pt.iscte.jask.templates.invoke
import kotlin.test.assertEquals

class TestHowDeepCallStack {

    @Test
    fun testApplicable() {
        val src = """
            class Test {
                static int factorial(int n) {
                    if (n == 0) return 1;
                    else return n * factorial(n - 1);
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowDeepCallStack().generate(src, "factorial"(3))
            assertEquals(1, qlc.solution.size)
            assertEquals("4", qlc.solution[0].toString())
        }

        assertDoesNotThrow {
            val qlc = HowDeepCallStack().generate(src, "factorial"(0))
            assertEquals(1, qlc.solution.size)
            assertEquals("1", qlc.solution[0].toString())
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
            HowDeepCallStack().generate(src, "factorial"(3))
        }

        assertThrows<QuestionGenerationException> {
            HowDeepCallStack().generate(SourceCode(src))
        }
    }
}