package dynamic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.dynamic.HowManyVariableAssignments
import pt.iscte.jask.templates.invoke

class TestHowManyVariableAssignments {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static int foo() {
                    int c = 0;
                    c = c + 1;
                    c = c + 2;
                    return c;
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyVariableAssignments().generate(src1, "foo"())
            println(qlc)

            assertEquals(1, qlc.solution.size)
            // assertEquals("3", qlc.solution[0].toString()) -- Depends on which variable is chosen
        }

        val src2 = """
            class Test {
                static int foo() {
                    int c = 0;
                    c = c + 2;
                    int x = c + 1;
                    int y = x + 2;
                    return y;
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyVariableAssignments().generate(src2, "foo"())
            println(qlc)

            assertEquals(1, qlc.solution.size)
            // assertEquals("2", qlc.solution[0].toString()) -- Depends on which variable is chosen
        }
    }

    @Test
    fun testPaddleSumNaturals() {
        val src = """
            class Test {
                static int sumNaturals(int max){
                    int s = 0;
                    int n = 1;
                    while (n <= max) {
                        s = s + n;
                        n = n + 1;
                    }
                    return s;
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            HowManyVariableAssignments().generate(SourceCode(src, listOf(
                "sumNaturals"(5),
                "sumNaturals"(8),
            )))
        }
        println(qlc)
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static int foo() {
                    int c = 42;
                    return c;
                }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            HowManyVariableAssignments().generate(src, "foo"())
        }
    }
}