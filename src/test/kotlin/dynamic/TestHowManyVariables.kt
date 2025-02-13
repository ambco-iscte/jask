package dynamic

import assertUniqueSolution
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.questions.HowManyVariables
import kotlin.test.Test

class TestHowManyVariables {
    @Test
    fun test() {
        val src = """
            class Test {
                static double average(int a, int b) {
                    double n = 2.0;
                    return (a + b) / n;
                }           
            }
        """.trimIndent()

        val qlc = HowManyVariables().generate(src, Localisation.getLanguage("pt"))
        qlc.assertUniqueSolution("1")
    }
}