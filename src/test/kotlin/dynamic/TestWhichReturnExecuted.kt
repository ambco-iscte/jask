package dynamic

import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.jask.Localisation
import pt.iscte.jask.templates.ProcedureCall
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.invoke
import pt.iscte.jask.templates.quality.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TestWhichReturnExecuted {
    @Test
    fun test() {
        val src = """
            class Test {
                static double abs(double n) {
                    if(n < 0) {
                        return -n;
                    }
                    else {
                        return n;
                    }
                }
            }
        """.trimIndent()

        val qlc1 = WhichReturnExecuted().generate(src, ProcedureCall("abs", listOf(-2.0)), Localisation.getLanguage("pt"))
        assertEquals("return -n;", qlc1.solution.first().toString())

        val qlc2 = WhichReturnExecuted().generate(src, ProcedureCall("abs", listOf(2.0)), Localisation.getLanguage("pt"))
        assertEquals("return n;", qlc2.solution.first().toString())
    }

    @Test
    fun testPaddleAbsIf() {
        val src = """
            class Test {
                static double abs(double n) {
                  if(n >= 0) return n;
                  else return -n;
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            WhichReturnExecuted().generate(SourceCode(src, listOf(
                "abs"(2.8),
                "abs"(-1.3)
            )))
        }
        println(qlc)
    }

    @Test
    fun testPaddleIrsTax() {
        val src = """
            class Test {
                static double irsTax(int group) {
                    if(group == 1) {
                        return .12;
                    } else {
                        if (group == 2) {
                            return .18;
                        } else {
                            if (group == 3) {
                                return .23;
                            } else { 
                                if (group == 4) {
                                    return .29;
                                } else {
                                    return 0.0;
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            WhichReturnExecuted().generate(SourceCode(src, listOf(
                "irsTax"(0),
                "irsTax"(-3),
                "irsTax"(5),
                "irsTax"(1),
                "irsTax"(2),
                "irsTax"(3),
                "irsTax"(4),
            )))
        }
        println(qlc)
    }
}