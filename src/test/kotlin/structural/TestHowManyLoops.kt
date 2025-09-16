package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.HowManyLoops

class TestHowManyLoops {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static int foo() {
                    int n = 0;
                    for (int i = 0; i <= 10; i++) {
                        n = n + i;
                    }
                    return n;
                }
            }
        """.trimIndent()

        val qlc1 = assertDoesNotThrow { HowManyLoops().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("1", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static int foo() {
                    int n = 0;
                    for (int i = 0; i <= 10; i++) {
                        for (int j = 0; j <= 10; j++) {
                            n = n + j;
                        }
                    }
                    return n;
                }
            }
        """.trimIndent()

        val qlc2 = assertDoesNotThrow { HowManyLoops().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("2", qlc2.solution[0].toString())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static int foo() { }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            HowManyLoops().generate(src)
        }
    }
}