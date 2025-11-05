package structural

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.dynamic.WhichReturnExecuted
import pt.iscte.jask.templates.invoke
import pt.iscte.jask.templates.structural.WhichVariableHoldsReturn
import kotlin.test.assertEquals

class TestWhichVariableHoldsReturn {

    @Test
    fun testApplicable() {
        val src = """
            class IRS {
                static double tax(int income) {
                    double result = 0;
                    if (income <= 8059)
                        result = 13;
                    else if (income <= 12160)
                        result = 16.5;
                    else if (income <= 17233)
                        result = 22;
                    else if (income <= 22306)
                        result = 25;
                    else if (income <= 28400)
                        result = 32;
                    else if (income <= 41629)
                        result = 25.5;
                    else if (income <= 44987)
                        result = 34.5;
                    else if (income <= 84696)
                        result = 45;
                    else
                        result = 48;
                    return result;
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow { WhichVariableHoldsReturn().generate(src) }
        println(qlc)
        assertEquals(1, qlc.solution.size)
        assertEquals("result", qlc.solution[0].toString())
    }

    @Test
    fun testPaddleIntDivision() {
        val src = """
            class Test {
                static int intDivision(int a, int b) {
                    int n = a;
                    int d = 0;
                    while(n >= b) {
                        n = n - b;
                        d = d + 1;
                    }
                    return d;
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            WhichVariableHoldsReturn().generate(SourceCode(src, listOf(
                "intDivision"(25, 5),
                "intDivision"(26, 5),
                "intDivision"(10, 3),
                "intDivision"(30, 7)
            )))
        }
        println(qlc)
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static void foo() { }
                
                static int bar(int n) { 
                    if (n == 0) return 1;
                    else return n;
                }
                
                static int baz() { 
                    return 42;
                }
                
                static int even(int n) {
                    boolean a = true;
                    boolean b = false;
                    if (n % 2 == 0) return a;
                    else return b;
                }
            }
        """.trimIndent()
        assertThrows<QuestionGenerationException> { WhichVariableHoldsReturn().generate(src) }
    }
}