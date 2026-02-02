package structural

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.invoke
import pt.iscte.jask.templates.structural.WhichVariableRole
import kotlin.test.assertEquals

class TestWhichVariableRole {

    @Test
    fun testPaddlePowerOfTwoSolution() {
        val src = """
            class Test {
                static int powerOfTwo(int e) {
                    assert e >= 0;
                    int p = 1;
                    int n = e;
                    while (n > 0) {
                        p = p * 2;
                        n = n - 1;
                    }
                    return p;
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            WhichVariableRole().generate(SourceCode(src, listOf(
                "powerOfTwo"(4),
                "powerOfTwo"(8),
            )))
        }
        assertEquals(1, qlc.solution.size)
        assertEquals("Stepper", qlc.solution.first().toString())
        println(qlc)
    }

    @Test
    fun testPaddleSumEvenBetween() {
        val src = """
            class Test {
                static int sumEvenBetween(int min, int max) {
                    assert min <= max;
                    int s = 0;
                    int n = min;
                    if(n % 2 != 0) {
                        n = n + 1;
                    }
                    while(n <= max) {
                        s = s + n;
                        n = n + 2;
                    }
                    return s;
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            WhichVariableRole().generate(SourceCode(src, listOf(
                "sumEvenBetween"(3, 3),
                "sumEvenBetween"(3, 4),
                "sumEvenBetween"(3, 8),
                "sumEvenBetween"(4, 8),
                "sumEvenBetween"(4, 9),
            )))
        }
        println(qlc)
    }

    @Test
    fun testPaddleSquareMatrixNaturals() {
        val src = """
            class SquareMatrix {
                static int[][] squareMatrixNaturals(int n) {
                    assert n >= 0;
                    
                    int[][] m = new int[n][n];
                    int num = 1;
                    
                    for(int i = 0; i < n; i++) {
                        for(int j = 0; j < n; j++) {
                            m[i][j] = num;
                            num++;
                        }
                    }
                    
                    return m;
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            WhichVariableRole().generate(SourceCode(src,
                calls = listOf(
                    "squareMatrixNaturals"(0),
                    "squareMatrixNaturals"(1),
                    "squareMatrixNaturals"(2),
                    "squareMatrixNaturals"(3)
                )
            ))
        }
        println(qlc)
    }
}