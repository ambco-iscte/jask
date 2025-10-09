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
    fun testWeirdPaddleBugReplace() {
        val src = """
            class Test {
                static void replace(char[] letters, char find, char replace) {
                    for (int i = 0; i < letters.length; i++) {
                        if (letters[i] == find)
                            letters[i] = replace;
                    }
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            HowManyArrayWrites().generate(src, "replace"(listOf('j', 'a', 'v', 'a'), 'e', 'i'))
        }
        println(qlc)

        assertEquals(1, qlc.solution.size)
        assertEquals("0", qlc.solution[0].toString())
    }

    @Test
    fun testWeirdPaddleBugConstrain() {
        val src = """
            class Test {
                static void constrain(double[] array, double min, double max) {
                    for(int i = 0; i < array.length; i++)
                        if(array[i] < min)
                            array[i] = min;
                        else if(array[i] > max)
                            array[i] = max; 
                }
            }
        """.trimIndent()

        val qlc1 = HowManyArrayWrites().generate(
            src,
            "constrain"(listOf(-1.3, 0.4, 1.4, 0.5), 0.0, 1.0)
        )
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("2", qlc1.solution[0].toString())


        val qlc2 = HowManyArrayWrites().generate(
            src,
            "constrain"(listOf(0.0, 0.4, 1.0, 0.5), 0.0, 1.0)
        )
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("0", qlc2.solution[0].toString())
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