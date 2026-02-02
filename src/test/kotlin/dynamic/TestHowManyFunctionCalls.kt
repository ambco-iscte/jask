package dynamic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.dynamic.HowManyFunctionCalls
import pt.iscte.jask.common.invoke
import kotlin.test.assertEquals

class TestHowManyFunctionCalls {

    @Test
    fun testApplicable() {
        val src = """
            class Test {
                static void foo() {
                    for (int i = 0; i < 10; i++) {
                        bar();
                    }
                }
                
                static void bar() {
                
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyFunctionCalls().generate(src, "foo"())
            assertEquals(1, qlc.solution.size)
            assertEquals("10", qlc.solution[0].toString())
        }
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static int foo() {
                    int n = 0;
                    for (int i = 0; i < 10; i++) {
                        n = n + 1;
                    }
                    return n;
                }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            HowManyFunctionCalls().generate(src, "foo"())
        }
    }
}