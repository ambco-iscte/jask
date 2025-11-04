package dynamic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.dynamic.HowManyArrayReads
import pt.iscte.jask.templates.invoke
import kotlin.test.assertEquals

class TestHowManyArrayReads {

    @Test
    fun testApplicable() {
        val src = """
            class Test {
                static boolean isSorted(int[] a) {
                    for (int i = 0; i < a.length - 1; i++) {
                        if (a[i] > a[i + 1])
                            return false;
                    }
                    return true;
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyArrayReads().generate(src, "isSorted"(listOf(1, 2, 3, 4, 5)))
            assertEquals(1, qlc.solution.size)
            assertEquals("8", qlc.solution[0].toString())
        }

        assertDoesNotThrow {
            val qlc = HowManyArrayReads().generate(src, "isSorted"(listOf(5, 4, 3, 2, 1)))
            assertEquals(1, qlc.solution.size)
            assertEquals("2", qlc.solution[0].toString())
        }
    }

    @Test
    fun testPaddleBug() {
        val src = """
            class Test {
                static void replaceLast(char[] letters, char find, char replace) {
                    for (int i = letters.length - 1; i >= 0; i--) {
                        if (letters[i] == find) {
                            letters[i] = replace;
                            return;
                        }
                    }
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            HowManyArrayReads().generate(SourceCode(src, listOf(
                null(listOf('j', 'a', 'v', 'a'), 'j', 'c'),
            )))
        }
        println(qlc)

        assertEquals(1, qlc.solution.size)
        assertEquals("4", qlc.solution[0].toString())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static boolean isSorted(int[] a) {
                    return true;
                }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            HowManyArrayReads().generate(src, "isSorted"(listOf(1, 2, 3, 4, 5)))
        }
    }
}