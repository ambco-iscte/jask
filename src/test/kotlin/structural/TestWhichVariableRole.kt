package structural

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.invoke
import pt.iscte.jask.templates.structural.WhichVariableRole

class TestWhichVariableRole {

    @Test
    fun testPaddleSquareMatrixNaturals() {
        val src = """
            class SquareMatrix {
                int[][] squareMatrixNaturals(int n) {
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