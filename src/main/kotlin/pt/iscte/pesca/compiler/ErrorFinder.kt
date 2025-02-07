package pt.iscte.pesca.compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.pesca.compiler.errors.VariableNotFound
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.getVariablesInScope

interface ICompilerError { }

data class ErrorFinder(private val unit: CompilationUnit) {

    constructor(source: String): this(StaticJavaParser.parse(source))

    init {
        StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
        StaticJavaParser.getParserConfiguration().setSymbolResolver(
            JavaSymbolSolver(CombinedTypeSolver().apply { add(ReflectionTypeSolver()) })
        )
    }

    fun findUnknownVariables(): List<VariableNotFound> =
        unit.findAll<NameExpr>().filter { name ->
            // Variable name is not in scope
            name.getVariablesInScope().none { it == name.nameAsString }

            // JP cannot resolve the type (to exclude NameExpr like "System" in "System.out.println")
            && runCatching { name.calculateResolvedType() }.isFailure
        }.map { VariableNotFound(it.nameAsString, it) }

    fun findUnknownMethods(): List<ICompilerError> {
        TODO()
    }
}