package pt.iscte.jask.templates

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.IModuleVisitor
import pt.iscte.jask.extensions.Quadruple
import pt.iscte.jask.extensions.accept
import pt.iscte.jask.extensions.configureStaticJavaParser
import pt.iscte.jask.extensions.randomByOrNull
import pt.iscte.jask.extensions.toIValue
import pt.iscte.strudel.model.*
import pt.iscte.strudel.parsing.java.CONSTRUCTOR_FLAG
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.cast

data class QuestionGenerationException(
    val template: QuestionTemplate<*>,
    val source: SourceCode?,
    override val message: String? = null,
    override val cause: Throwable? = null
) : Exception(message, cause) {

    override fun toString(): String =
        "Error generating question of type ${template::class.simpleName}: $message\n----------$source\n----------\nCause: $cause"
}

data class ProcedureCall(val id: String?, val arguments: List<Any?> = emptyList()) {
    override fun toString(): String =
        "$id(${arguments.joinToString()})"
}

data class SourceCode(val code: String, val calls: List<ProcedureCall> = emptyList()) {

    private var unit: CompilationUnit? = null

    constructor(file: File) : this(file.readText())

    constructor(unit: CompilationUnit, calls: List<ProcedureCall> = emptyList()): this(unit.toString(), calls) {
        this.unit = unit
    }

    fun load(): CompilationUnit = this.unit ?: runCatching { StaticJavaParser.parse(code) }.onFailure {
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
sealed class QuestionTemplate<T : Any>(val range: IntRange = 1 .. Int.MAX_VALUE) {

    companion object {
        init {
            configureStaticJavaParser()
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
abstract class StructuralQuestionTemplate<T : Node>(range: IntRange) : QuestionTemplate<T>(range) {

    constructor() : this(1..Int.MAX_VALUE)

    protected inline fun <reified R : T> List<SourceCode>.getRandom(): Pair<SourceCode, R> {
        val source = randomByOrNull { isApplicable<R>(it, R::class) } ?:
        throw QuestionGenerationException(this@StructuralQuestionTemplate, null, "Could not find a valid source.")

        val element = getApplicableElements<R>(source).randomOrNull() ?:
        throw QuestionGenerationException(this@StructuralQuestionTemplate, source, "Could not find applicable element of type ${R::class.simpleName} within source.")

        return Pair(source, element)
    }

    /**
     * Generates the [Question] for this question using a list of [sources].
     * @param sources A list of sources.
     * @param language The language to use for generating the question's textual elements.
     */
    fun generate(sources: List<SourceCode>, language: Language = Language.DEFAULT): Question {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language).apply {
            if (!this.hasQuestionType)
                questionType = this@StructuralQuestionTemplate::class.simpleName ?: this@StructuralQuestionTemplate::class.java.simpleName
        }
    }

    fun generate(unit: CompilationUnit, language: Language = Language.DEFAULT) =
        generate(listOf(SourceCode(unit)), language)

    fun generate(src: String, language: Language = Language.DEFAULT) =
        generate(listOf(SourceCode(src)), language)

    protected abstract fun build(sources: List<SourceCode>, language: Language = Language.DEFAULT): Question

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
abstract class DynamicQuestionTemplate<T : IProgramElement> : QuestionTemplate<T>() {

    private fun DynamicQuestionTemplate<IProcedure>.getRandomApplicableProcedureAndArguments(
        module: IModule,
        calls: List<ProcedureCall>
    ): Pair<IProcedure, List<Any?>>? {
        val vm = IVirtualMachine.create()
        val pairs = mutableListOf<Pair<IProcedure, List<Any?>>>()
        module.procedures.filterIsInstance<IProcedure>().filter { !it.hasFlag(CONSTRUCTOR_FLAG) }.forEach { p ->
            calls.forEach { call ->
                val args = call.arguments.map { it.toIValue(vm, module) }
                if (call.id == p.id && p.id != null) { // Specific test cases
                    if (isApplicable(p) && isApplicable(p, args))
                        pairs.add(p to call.arguments)
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

    protected fun DynamicQuestionTemplate<IProcedure>.getRandomProcedure(
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
     * Generates the [Question] for this question using a list of [sources].
     * @param sources A list of sources.
     * @param language The language to use for generating the question's textual elements.
     */
    fun generate(sources: List<SourceCode>, language: Language = Language.DEFAULT): Question {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language).apply {
            if (!this.hasQuestionType)
                questionType = this@DynamicQuestionTemplate::class.simpleName ?: this@DynamicQuestionTemplate::class.java.simpleName
        }
    }

    fun generate(src: String, call: ProcedureCall, language: Language = Language.DEFAULT) = generate(
        listOf(SourceCode(src, listOf(call))), language
    )

    fun generate(unit: CompilationUnit, call: ProcedureCall, language: Language = Language.DEFAULT) = generate(
        listOf(SourceCode(unit, listOf(call))), language
    )

    protected open fun isApplicable(element: T, args: List<IValue>): Boolean = isApplicable(element)

    protected abstract fun build(sources: List<SourceCode>, language: Language = Language.DEFAULT): Question

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