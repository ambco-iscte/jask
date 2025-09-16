package dynamic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.dynamic.HowManyArrayReads
import pt.iscte.jask.templates.dynamic.HowManyArrayWrites
import pt.iscte.jask.templates.invoke
import kotlin.test.assertEquals

class TestHowManyArrayWrites {

    @Test
    fun testApplicable() {
        val src = """
            class Test {
                static void fill(int[] a, int e) {
                    for (int i = 0; i < a.length; i++) {
                        a[i] = e;
                    }
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyArrayWrites().generate(src, "fill"(listOf(1, 2, 3, 4, 5), 42))
            assertEquals(1, qlc.solution.size)
            assertEquals("5", qlc.solution[0].toString())
        }

        assertDoesNotThrow {
            val qlc = HowManyArrayWrites().generate(src, "fill"(listOf(5, 4, 3, 2, 1), 42))
            assertEquals(1, qlc.solution.size)
            assertEquals("5", qlc.solution[0].toString())
        }
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static void fill(int[] a, int e) {
                    
                }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            HowManyArrayWrites().generate(src, "fill"(listOf(1, 2, 3, 4, 5), 42))
        }
    }
}