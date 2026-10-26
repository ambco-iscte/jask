package compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.junit.jupiter.api.Test
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.extensions.simpleName
import kotlin.test.assertEquals

class TestWrongReturnStmtType {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                public static int foo(int n) {
                    return n * 2.0;
                }
                
                public static double bar(int n) {
                    return ":)";
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findReturnStmtsWithWrongType()

        assertEquals(2, errors.size)

        val (e1, e2) = errors

        assertEquals("int", e1.expected.simpleName)
        assertEquals("double", e1.actual.simpleName)

        assertEquals("double", e2.expected.simpleName)
        assertEquals("String", e2.actual.simpleName)
    }
}