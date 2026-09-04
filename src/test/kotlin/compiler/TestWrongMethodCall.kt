package compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.junit.jupiter.api.Test
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.templates.CallMethodWithWrongParameterNumber
import pt.iscte.jask.errors.compiler.templates.CallMethodWithWrongParameterTypes
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestWrongMethodCall {

    @Test
    fun argumentCount() {
        val src = """
            class HelloWorld {
                int x = average(1, 2, 3);
            
                double average(int a, int b) {
                    return (a + b) / 2.0;
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findMethodCallsWithWrongArguments()
        assertEquals(1, errors.size)

        val error = errors.single()
        println(error)

        assertTrue(error.parameterNumberMismatch)
        assertEquals(2, error.expected.size)
        assertEquals(3, error.actual.size)
        assertEquals("average", error.method.nameAsString)

        // ---

        val qlc = CallMethodWithWrongParameterNumber()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("2", data.solution.first().toString())
    }

    @Test
    fun argumentType() {
        val src = """
            class HelloWorld {
                int x = neg("hello");
            
                int neg(int a) {
                    return -a;
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findMethodCallsWithWrongArguments()
        assertEquals(1, errors.size)

        val error = errors.single()
        println(error)

        assertTrue(error.parameterTypeMismatch)
        assertEquals(1, error.expected.size)
        assertEquals(1, error.actual.size)
        assertEquals("int", error.expected.single().asString())
        assertEquals("java.lang.String", error.actual.single().describe())
        assertEquals("neg", error.method.nameAsString)

        // ---

        val qlc = CallMethodWithWrongParameterTypes()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("int", data.solution.first().toString())
    }
}