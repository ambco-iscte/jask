package dynamic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.jask.Localisation
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*
import pt.iscte.jask.templates.ProcedureCall
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.invoke
import kotlin.test.assertEquals

class TestHowManyArrayAllocations {

    @Test
    fun test() {
        val src = """
            class Test {
                static double[] drop(double[] array, int n) {
	                int diff = Math.abs(n); 
                    if(diff > array.length)
                        return new double[0];
                
                    double[] drop = new double[array.length - diff];
                    if(n > 0)
                        for(int i = 0; i < drop.length; i++)
                            drop[i] = array[i+n];
                    else
                        for(int i = 0; i < drop.length; i++)
                            drop[i] = array[i];
                    return drop;
                }
            }
        """.trimIndent()
        val qlc = HowManyArrayAllocations()
        val contains = qlc.generate(
            src,
            "drop"(listOf(1, 2, 3, 4, 5), 3),
            Localisation.getLanguage("en")
        )
        println(contains)
        assertEquals("1", contains.solution.first().toString())
    }

    @Test
    fun testPaddleBugDoubleArray() {
        val src = """
            class DoubleArray {
                static void doubleArray(int[] array) {
                    for(int i = 0; i < array.length; i++) {
                        array[i] = array[i] * 2;
                    }
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            HowManyArrayAllocations().generate(SourceCode(src, listOf(
                null(listOf(1, 2, 3, 4)),
                null(listOf(-1, 1, 3, 0)),

            )))
        }
        println(qlc)

        assertEquals(1, qlc.solution.size)
        assertEquals("0", qlc.solution.first().toString())
    }
}