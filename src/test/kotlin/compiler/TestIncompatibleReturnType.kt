package compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.junit.jupiter.api.Test
import pt.iscte.pesca.compiler.ErrorFinder
import kotlin.test.assertEquals

class TestIncompatibleReturnType {

    init {
        StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
        StaticJavaParser.getParserConfiguration().setSymbolResolver(
            JavaSymbolSolver(CombinedTypeSolver().apply { add(ReflectionTypeSolver()) })
        )
    }

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

        val errors = ErrorFinder(StaticJavaParser.parse(src)).findIncompatibleReturnTypes()

        assertEquals(2, errors.size)

        val (e1, e2) = errors

        assertEquals("int", e1.expected.asString())
        assertEquals("double", e1.actual.describe())

        assertEquals("double", e2.expected.asString())
        assertEquals("java.lang.String", e2.actual.describe())
    }
}