package dynamic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.dynamic.HowManyFunctionCalls
import pt.iscte.jask.templates.dynamic.HowManyLoopIterations
import pt.iscte.jask.common.invoke
import kotlin.test.assertEquals

class TestHowManyLoopIterations {

    @Test
    fun testApplicable() {
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

        assertDoesNotThrow {
            val qlc = HowManyLoopIterations().generate(src, "foo"())
            assertEquals(1, qlc.solution.size)
            assertEquals("10", qlc.solution[0].toString())
        }
    }


    @Test
    fun testMatrix() {
        val src = """
            class totalMatrixElements {
                static int totalElements(int[][] m) {
                    int c = 0;
                    for (int i = 0; i < m.length; i++)
                        c += m[i].length;
                    return c; 
                }
            }
        """.trimIndent()
        assertDoesNotThrow {
            val qlc = HowManyLoopIterations().generate(src, "totalElements"(
                listOf(listOf(1,2,3), listOf(4,5,6,7))
            ))
            assertEquals(1, qlc.solution.size)
            assertEquals("2", qlc.solution[0].toString())

            val qlcEmptyArg = HowManyLoopIterations().generate(src, "totalElements"(
                listOf(listOf<Int>(), listOf())
            ))
            assertEquals(1, qlcEmptyArg.solution.size)
            assertEquals("2", qlcEmptyArg.solution[0].toString())
        }
    }

    @Test
    fun testNotApplicable() {
        assertThrows<QuestionGenerationException> {
            HowManyFunctionCalls().generate("""
                class Test {
                static int foo() {
                    int n = 0;
                    for (int i = 0; i < 10; i++) {
                        n = n + 1;
                    }
                    for (int i = 0; i < 5; i++) {
                        n = n * 2;
                    }
                    return n;
                }
            }
            """.trimIndent(), "foo"())
        }

        assertThrows<QuestionGenerationException> {
            HowManyFunctionCalls().generate("""
                class Test {
                static int foo() {
                    return 42;
                }
            }
            """.trimIndent(), "foo"())
        }
    }
}