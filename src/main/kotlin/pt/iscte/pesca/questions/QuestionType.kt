package pt.iscte.pesca.questions

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.IModuleVisitor
import pt.iscte.pesca.extensions.Quadruple
import pt.iscte.pesca.extensions.accept
import pt.iscte.pesca.extensions.randomByOrNull
import pt.iscte.pesca.extensions.toIValue
import pt.iscte.strudel.model.*
import pt.iscte.strudel.parsing.java.CONSTRUCTOR_FLAG
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.cast

data class QuestionGenerationException(
    val question: Question<*>,
    val source: SourceCode?,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    override fun toString(): String =
        "Error generating question of type ${question::class.simpleName}: $message\n----------$source\n----------\nCause: $cause"
}

data class ProcedureCall(val id: String?, val arguments: List<Any?>)

data class SourceCode(val code: String, val calls: List<ProcedureCall> = emptyList()) {

    constructor(file: File) : this(file.readText())

    fun load(): CompilationUnit = runCatching { StaticJavaParser.parse(code) }.onFailure {
        throw IllegalArgumentException("Source code does not compile: ${it.message}\n$code", it.cause)
    }.getOrThrow()

    override fun toString(): String = code
}

data class RecordTypeData(val name: String, val fields: List<Any?>) {
    override fun toString(): String = "$name[${fields.joinToString()}]"
}

/**
 * This class provides a generic representation of a QLC.
 * @param T Type of targeted elements within student code.
 * @param range Number of source code(s) which are needed to produce this question.
 */
sealed class Question<T : Any>(val range: IntRange = 1 .. Int.MAX_VALUE) {

    companion object {
        init {
            StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
        }
    }

    /**
     * Given the [type] of targeted elements, is the question applicable to this [source] code?
     * @param source Source code.
     * @param type Type of targeted elements. Must be [T] or a valid subclass.
     */
    fun <R : T> isApplicable(source: SourceCode, type: KClass<R>): Boolean =
        getApplicableElements(source, type).isNotEmpty()

    /**
     * Is the question applicable to this [element]?
     * @param element An element of the question's target type.
     */
    protected open fun isApplicable(element: T): Boolean = true

    protected abstract fun <R : T> getApplicableElements(source: SourceCode, type: KClass<R>): List<R>

    protected inline fun <reified R : T> getApplicableElements(source: SourceCode): List<R> =
        getApplicableElements(source, R::class)

    protected inline fun <reified R : T> getApplicableElements(sources: List<SourceCode>): List<R> =
        sources.filter { isApplicable(it, R::class) }.flatMap {
            source -> getApplicableElements(source, R::class)
        }

    protected inline fun <reified R : T> getApplicableSources(sources: List<SourceCode>): List<SourceCode> =
        sources.filter { isApplicable(it, R::class) }
}

/**
 * This class provides a representation of a QLC targeting *static* code elements, i.e. static syntactic elements
 * which do not change during execution. This question type takes pure [SourceCode] as input.
 *
 * @param T The type of JavaParser - [com.github.javaparser] - node the question targets, e.g.
 * [com.github.javaparser.ast.body.MethodDeclaration] if the question targets methods.
 */
abstract class StaticQuestion<T : Node>(range: IntRange) : Question<T>(range) {

    constructor() : this(1..Int.MAX_VALUE)

    protected inline fun <reified R : T> List<SourceCode>.getRandom(): Pair<SourceCode, R> {
        val source = randomByOrNull { isApplicable<R>(it, R::class) } ?:
        throw QuestionGenerationException(this@StaticQuestion, null, "Could not find a valid source.")

        val element = getApplicableElements<R>(source).randomOrNull() ?:
        throw QuestionGenerationException(this@StaticQuestion, source, "Could not find applicable element of type ${R::class.simpleName} within source.")

        return Pair(source, element)
    }

    /**
     * Generates the [QuestionData] for this question using a list of [sources].
     * @param sources A list of sources.
     * @param language The language to use for generating the question's textual elements.
     */
    fun generate(sources: List<SourceCode>, language: Language = Language.DEFAULT): QuestionData {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language).apply {
            if (!this.hasQuestionType)
                questionType = this@StaticQuestion::class.simpleName ?: this@StaticQuestion::class.java.simpleName
        }
    }

    fun generate(src: String, language: Language = Language.DEFAULT) =
        generate(listOf(SourceCode(src)), language)

    protected abstract fun build(sources: List<SourceCode>, language: Language = Language.DEFAULT): QuestionData

    override fun <R : T> getApplicableElements(source: SourceCode, type: KClass<R>): List<R> =
        source.load().findAll(type.java) { isApplicable(it) }
}

/**
 * This class provides a representation of a QLC targeting *dynamic* code elements, i.e. elements which depend
 * on the code's execution and depend on the methods' provided input.
 *
 * @param T The type of Strudel - [pt.iscte.strudel] - element that the question targets, e.g.
 * [IProcedure] if the question targets concrete methods.
 */
abstract class DynamicQuestion<T : IProgramElement> : Question<T>() {

    private fun DynamicQuestion<IProcedure>.getRandomApplicableProcedureAndArguments(
        module: IModule,
        calls: List<ProcedureCall>
    ): Pair<IProcedure, List<Any?>>? {
        val vm = IVirtualMachine.create()
        val pairs = mutableListOf<Pair<IProcedure, List<Any?>>>()
        module.procedures.filterIsInstance<IProcedure>().filter { !it.hasFlag(CONSTRUCTOR_FLAG) }.forEach { p ->
            calls.forEach { call ->
                val args = call.arguments.map { it.toIValue(vm, module) }
                if (call.id == p.id && p.id != null) { // Specific test cases
                    call.arguments.forEach {
                        if (isApplicable(p) && isApplicable(p, args))
                            pairs.add(p to call.arguments)
                    }
                }
                else if (call.id == null) { // Wildcard test cases
                    runCatching { vm.execute(p, *args.toTypedArray()) }.onSuccess {
                        pairs.add(p to call.arguments)
                    }
                }
            }
        }
        return pairs.randomOrNull()
    }

    protected fun DynamicQuestion<IProcedure>.getRandomProcedure(
        sources: List<SourceCode>
    ): Quadruple<SourceCode, IModule, IProcedure, List<Any?>> {
        val source: SourceCode? = sources.filter { s ->
            runCatching {
                val module = Java2Strudel(checkJavaCompilation = false).load(s.code)
                getRandomApplicableProcedureAndArguments(module, s.calls) != null
            }.getOrDefault(false)
        }.randomOrNull()

        if (source == null)
            throw QuestionGenerationException(this, null, "Could not find source with at least one applicable procedure.")

        val module = Java2Strudel(checkJavaCompilation = false).load(source.code)

        val (procedure, args) = getRandomApplicableProcedureAndArguments(module, source.calls) ?:
        throw QuestionGenerationException(this, source, "Could not find applicable procedure within source.")

        return Quadruple(source, module, procedure, args)
    }

    /**
     * Generates the [QuestionData] for this question using a list of [sources].
     * @param sources A list of sources.
     * @param language The language to use for generating the question's textual elements.
     */
    fun generate(sources: List<SourceCode>, language: Language = Language.DEFAULT): QuestionData {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language).apply {
            if (!this.hasQuestionType)
                questionType = this@DynamicQuestion::class.simpleName ?: this@DynamicQuestion::class.java.simpleName
        }
    }

    fun generate(src: String, call: ProcedureCall, language: Language = Language.DEFAULT) = generate(
        listOf(SourceCode(src, listOf(call))), language
    )

    protected open fun isApplicable(element: T, args: List<IValue>): Boolean = isApplicable(element)

    protected abstract fun build(sources: List<SourceCode>, language: Language = Language.DEFAULT): QuestionData

    private inner class ApplicableCollector<R : T>(private val type: KClass<R>): IModuleVisitor {

        private val collectedElements = mutableSetOf<R>()

        fun getCollectedElements(): Set<R> = collectedElements

        inner class ExpressionVisitorCollector(): IExpression.IVisitor {
            override fun visitAny(exp: IExpression) {
                if (type.java.isAssignableFrom(exp::class.java))
                    collectedElements.add(type.cast(exp))
            }
        }

        inner class BlockVisitorCollector(): IBlock.IVisitor {
            override fun visitAny(element: IBlockElement) {
                if (type.java.isAssignableFrom(element::class.java))
                    collectedElements.add(type.cast(element))
            }
        }

        override fun visit(constant: IConstantDeclaration): Boolean {
            if (type.java.isAssignableFrom(constant::class.java))
                collectedElements.add(type.cast(constant))
            constant.value.accept(ExpressionVisitorCollector())
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
                is IProcedure -> procedure.block.accept(BlockVisitorCollector())
                else -> return false
            }
            return true
        }
    }

    override fun <R : T> getApplicableElements(source: SourceCode, type: KClass<R>): List<R> {
        val visitor = ApplicableCollector(type)
        Java2Strudel().load(source.code).accept(visitor)
        return visitor.getCollectedElements().toList()
    }
}