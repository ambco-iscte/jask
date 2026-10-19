package compiler

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import pt.iscte.jask.errors.CompilerErrorFinder
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestMissingReturnInBranch {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                static boolean largestIsEven(int[] a) {
                    if (a.length == 0) {
                        return false;
                    }
                    
                    int i = 1;
                    int largest = a[0];
                    
                    while (i < a.length) {
                        if (a[i] > largest) {
                            largest = a[i];
                        }
                        i = i + 1;
                    }
                    
                    if (largest % 2 == 0) {
                        return true;
                    }
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findMissingReturnInBranch()

        errors.forEach { println(it) }

        assertEquals(1, errors.size)

        val e = errors.single().condition()

        assertEquals("a.length != 0 && i >= a.length && largest % 2 != 0", e.toString())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class HelloWorld {
                static boolean largestIsEven(int[] a) {
                    if (a.length == 0) {
                        return false;
                    }
                    
                    int i = 1;
                    int largest = a[0];
                    
                    while (i < a.length) {
                        if (a[i] > largest) {
                            largest = a[i];
                        }
                        i = i + 1;
                    }
                    
                    if (largest % 2 == 0) {
                        return true;
                    }
                    
                    return false;
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findMissingReturnInBranch()

        assertEquals(0, errors.size)
    }
}