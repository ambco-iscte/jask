package pt.iscte.pesca.questions

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.IModuleVisitor
import pt.iscte.pesca.extensions.accept
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IBlockElement
import pt.iscte.strudel.model.IConstantDeclaration
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IPolymorphicProcedure
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.impl.IForeignProcedure
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.cast

sealed interface ISource

data class SourceCode(val code: String): ISource {

    constructor(file: File) : this(file.readText())

    fun load(): CompilationUnit = runCatching { StaticJavaParser.parse(code) }.onFailure {
        throw IllegalArgumentException("Source code does not compile: ${it.message}", it.cause)
    }.getOrThrow()

    override fun toString(): String = code
}

data class ProcedureCall(val id: String, val arguments: List<List<Any?>>) {

    constructor(id: String, vararg arguments: Any?): this(id, listOf(arguments.toList()))
}

data class SourceCodeWithInput(val source: SourceCode, val calls: List<ProcedureCall>): ISource

sealed class Question<T : Any, S : ISource>(val range: IntRange = 1 .. Int.MAX_VALUE) {

    companion object {
        init {
            StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
        }
    }

    fun <R : T> isApplicable(source: S, type: KClass<R>): Boolean =
        getApplicableElements(source, type).isNotEmpty()

    protected abstract fun <R : T> getApplicableElements(source: S, type: KClass<R>): List<R>

    protected inline fun <reified R : T> getApplicableElements(source: S): List<R> =
        getApplicableElements(source, R::class)

    protected open fun isApplicable(element: T): Boolean = true

    protected inline fun <reified R : T> getApplicableElements(sources: List<S>): List<R> =
        sources.filter { isApplicable(it, R::class) }.flatMap {
            source -> getApplicableElements(source, R::class)
        }

    protected inline fun <reified R : T> getApplicableSources(sources: List<S>): List<S> =
        sources.filter { isApplicable(it, R::class) }
}

abstract class StaticQuestion<T : Node> : Question<T, SourceCode>() {

    fun generate(sources: List<SourceCode>, language: Language = Language.DEFAULT): QuestionData {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language)
    }

    protected abstract fun build(sources: List<SourceCode>, language: Language = Language.DEFAULT): QuestionData

    override fun <R : T> getApplicableElements(source: SourceCode, type: KClass<R>): List<R> =
        source.load().findAll(type.java) { isApplicable(it) }
}

abstract class DynamicQuestion<T : IProgramElement> : Question<T, SourceCodeWithInput>() {

    fun generate(sources: List<SourceCodeWithInput>, language: Language = Language.DEFAULT): QuestionData {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language)
    }

    protected abstract fun build(sources: List<SourceCodeWithInput>, language: Language = Language.DEFAULT): QuestionData

    private inner class ApplicableCollector<R : T>(private val type: KClass<R>): IModuleVisitor {

        private val collectedElements = mutableSetOf<R>()

        fun getCollectedElements(): Set<R> = collectedElements

        inner class ExpressionVisitorCollector(val set: MutableSet<R>): IExpression.IVisitor {
            override fun visitAny(exp: IExpression) {
                if (type.java.isAssignableFrom(exp::class.java))
                    collectedElements.add(type.cast(exp))
            }
        }

        inner class BlockVisitorCollector(val set: MutableSet<R>): IBlock.IVisitor {
            override fun visitAny(element: IBlockElement) {
                if (type.java.isAssignableFrom(element::class.java))
                    collectedElements.add(type.cast(element))
            }
        }

        override fun visit(constant: IConstantDeclaration): Boolean {
            if (type.java.isAssignableFrom(constant::class.java))
                collectedElements.add(type.cast(constant))
            constant.value.accept(ExpressionVisitorCollector(collectedElements))
            return true
        }

        override fun visit(type: IType): Boolean {
            if (this.type.java.isAssignableFrom(type::class.java))
                collectedElements.add(this.type.cast(type))
            return true
        }

        override fun visit(procedure: IProcedureDeclaration): Boolean {
            if (type.java.isAssignableFrom(procedure::class.java))
                collectedElements.add(type.cast(procedure))
            when (procedure) {
                is IProcedure -> procedure.block.accept(BlockVisitorCollector(collectedElements))
                else -> return false
            }
            return true
        }
    }

    override fun <R : T> getApplicableElements(source: SourceCodeWithInput, type: KClass<R>): List<R> {
        val visitor = ApplicableCollector(type)
        Java2Strudel().load(source.source.code).accept(visitor)
        return visitor.getCollectedElements().toList()
    }
}