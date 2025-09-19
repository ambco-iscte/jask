package dynamic

import pt.iscte.jask.Localisation
import pt.iscte.jask.templates.ProcedureCall
import pt.iscte.jask.templates.SimpleTextOption
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestWhichLastVariableValues {
    @Test
    fun test() {
        val src = """
            class Test {
                static void main(int i) {
                    int oneDiv2 = 1 / 2;
                    int oneMod2 = 1 % 2;
                    int sevenDiv2 = 7 / 2;
                    int sevenMod2 = 7 % 2;
                } 
                         
                static int factorial(int n) {
                      assert n >= 0;
                      if(n == 0) {
                          return 1;
                      }
                      int f = 1;
                      int i = n;
                      while(i > 1) {
                          f = f * i;
                          i = i - 1;
                      }
                      return f; 
                }
            }
        """.trimIndent()

        val qlc = WhichLastVariableValues().generate(src, ProcedureCall("main", listOf(1000)), Localisation.getLanguage("pt"))
        println(qlc)
        assertTrue(qlc.solution.size == 1)
        assertEquals("oneDiv2 = 0, oneMod2 = 1, sevenDiv2 = 3, sevenMod2 = 1", (qlc.solution.first() as SimpleTextOption).text)

        val qlc2 = WhichLastVariableValues().generate(src, ProcedureCall("factorial", listOf(4)), Localisation.getLanguage("pt"))
        println(qlc2)
        assertEquals("f = 24, i = 1", (qlc2.solution.first() as SimpleTextOption).text)

        val qlcNoHistory = WhichLastVariableValues().generate(src, ProcedureCall("factorial", listOf(0)), Localisation.getLanguage("pt"))
        assertEquals("f = indefinido, i = indefinido", (qlcNoHistory.solution.first() as SimpleTextOption).text)
        println(qlcNoHistory)
    }
}