package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.CallsOtherFunctions
import pt.iscte.jask.templates.structural.IsRecursive

class TestIsRecursive {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static int factorial(int n) {
                    if (n == 0) return 1;
                    else return n * factorial(n - 1);
                }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { IsRecursive().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("Yes.", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static int product(int n, int m) {
                    return n * m;
                }
                
                static int factorial(int n) {
                    int f = 1;
                    for (int i = 1; i <= n; i++) {
                        f = product(f, i);
                    }
                    return f;
                }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { IsRecursive().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("No.", qlc2.solution[0].toString())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static int factorial(int n) {
                    int f = 1;
                    for (int i = 1; i <= n; i++) {
                        f = f * i;
                    }
                    return f;
                }
            }
        """.trimIndent()
        assertThrows<QuestionGenerationException> {
            IsRecursive().generate(src)
        }
    }
}