package compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import org.junit.jupiter.api.Test
import pt.iscte.jask.errors.CompilerErrorFinder
import kotlin.test.assertEquals

class TestFindUnknownVariable {

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
                private int i = 0;
                private int j = k;
            
                public static int foo(int n) {
                    return n * m;
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findUnknownVariables()

        errors.forEach { println(it) }

        assertEquals(2, errors.size)

        val (e1, e2) = errors

        assertEquals("k", e1.expr.nameAsString)
        assertEquals("m", e2.expr.nameAsString)

        assertEquals(setOf("i", "j"), e1.scope.getUsableVariables().map { it.nameAsString }.toSet())
        assertEquals(setOf("i", "j", "n"), e2.scope.getUsableVariables().map { it.nameAsString }.toSet())
    }
}