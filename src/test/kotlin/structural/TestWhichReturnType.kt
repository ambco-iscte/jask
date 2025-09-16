package structural

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.WhichReturnType
import kotlin.test.assertEquals

class TestWhichReturnType {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static double div(int a, int b) {
                    return (a + b) / 2.0;
                }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { WhichReturnType().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("double", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static boolean equal(int a, int b) {
                    return a == b;
                }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { WhichReturnType().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("boolean", qlc2.solution[0].toString())

        val src3 = """
            class Test {
                static String hello(int times) {
                    String x = "";
                    for (int i = 0; i < times; i++) {
                        x = x + "hello";
                    }
                    return x;
                }
            }
        """.trimIndent()
        val qlc3 = assertDoesNotThrow { WhichReturnType().generate(src3) }
        println(qlc3)
        assertEquals(1, qlc3.solution.size)
        assertEquals("String", qlc3.solution[0].toString())
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
        assertThrows<QuestionGenerationException> { WhichReturnType().generate(src) }
    }
}