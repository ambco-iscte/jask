package compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.junit.jupiter.api.Test
import pt.iscte.pesca.compiler.ErrorFinder
import kotlin.test.assertEquals

class TestIncompatibleVariableType {

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
                private int i = "hello";
                private double j = 2;
                private String k = 3;
                private int x = 1;
            
                public static int foo(int n) {
                    int m = 2.0;
                    return n;
                }
            }
        """.trimIndent()

        val errors = ErrorFinder(StaticJavaParser.parse(src)).findIncompatibleVariableTypes()

        assertEquals(4, errors.size)

        val (e1, e2, e3, e4) = errors

        assertEquals("i", e1.variable.nameAsString)
        assertEquals("j", e2.variable.nameAsString)
        assertEquals("k", e3.variable.nameAsString)
        assertEquals("m", e4.variable.nameAsString)

        assertEquals("int", e1.expected.asString())
        assertEquals("double", e2.expected.asString())
        assertEquals("String", e3.expected.asString())
        assertEquals("int", e4.expected.asString())

        assertEquals("java.lang.String", e1.actual.describe())
        assertEquals("int", e2.actual.describe())
        assertEquals("int", e3.actual.describe())
        assertEquals("double", e4.actual.describe())
    }
}