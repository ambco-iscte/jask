package pt.iscte.pesca.compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.pesca.compiler.errors.VariableNotFound
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.getVariablesInScope
import pt.iscte.strudel.parsing.java.extensions.ClassLoader
import pt.iscte.strudel.parsing.java.extensions.getOrNull

fun main() {
    val src = """
        class VeryUsefulMath {
            private int k = 2;
            private int m = 2 * l;
            private int a = 2 * a;
        
            public static int doubleOf(int n) {
                foo("Hello world!", bar(n));
                return i * 2 * n;
            }
        }
    """.trimIndent()

    ErrorFinder(src).findUnknownVariables().forEach {
        println("Undefined variable '${it.symbol}' used at ${it.location.range.getOrNull}")
    }
}