package strudel

import org.junit.jupiter.api.Test
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.templates.HowManyArrayAllocations
import pt.iscte.pesca.templates.ProcedureCall
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
            ProcedureCall("drop", listOf(listOf(1, 2, 3, 4, 5), 3)),
            Localisation.getLanguage("en")
        )
        println(contains)
        assertEquals("1", contains.solution.first().toString())
    }
}