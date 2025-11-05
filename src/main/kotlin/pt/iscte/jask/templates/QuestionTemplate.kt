package pt.iscte.jask.templates

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import jdk.jfr.Description
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.IModuleVisitor
import pt.iscte.jask.extensions.Quadruple
import pt.iscte.jask.extensions.accept
import pt.iscte.jask.extensions.configureStaticJavaParser
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.extensions.randomKeyBy
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.*
import pt.iscte.strudel.parsing.java.CONSTRUCTOR_FLAG
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

abstract class QuestionGenerationException(
    open val template: QuestionTemplate<*>
) : Exception()

class ApplicableSourceNotFoundException(
    override val template: QuestionTemplate<*>,
    val errors: Map<SourceCode, Throwable?>
): QuestionGenerationException(template) {

    override val message: String
        get() {
            val messages = errors.mapNotNull { it.value?.message?.ifEmpty { null } }.toSet()
            var message = "Could not find any applicable sources for QLC of type ${template::class.simpleName}."
            if (messages.isNotEmpty())
                message += " ${messages.joinToString("; ")}"
            return message
        }
}

class ApplicableElementNotFoundException(
    override val template: QuestionTemplate<*>,
    val errors: Map<SourceCode, Throwable?>,
    val elementType: KClass<*>
): QuestionGenerationException(template) {

    override val message: String
        get() {
            val messages = errors.mapNotNull { it.value?.message?.ifEmpty { null } }.toSet()
            var message = "Could not find any applicable elements of type ${elementType.simpleName} for QLC of type ${template::class.simpleName}."
            if (messages.isNotEmpty())
                message += " ${messages.joinToString("; ")}"
            return message
        }
}

class ApplicableProcedureCallNotFoundException(
    override val template: QuestionTemplate<*>,
    val errors: Map<SourceCode, List<Throwable>>,
): QuestionGenerationException(template) {

    override val message: String
        get() {
            val messages = errors.flatMap { it.value.mapNotNull { e -> e.message?.ifEmpty { null } } }.toSet()
            var message = "Could not find any applicable procedure calls for QLC of type ${template::class.simpleName}."
            if (messages.isNotEmpty())
                message += " ${messages.joinToString("; ")}"
            return message
        }
}

operator fun String?.invoke(vararg arguments: Any?): ProcedureCall =
    ProcedureCall(this, arguments.toList())

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

    fun load(): Result<CompilationUnit> =
        this.unit?.let { Result.success(it) } ?: runCatching { StaticJavaParser.parse(code) }

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

    protected fun QuestionTemplate<*>.getFailedRequirementException(): AssertionError? =
        this::class.members.firstOrNull {
            it.isOpen && it.name == "isApplicable" && it.hasAnnotation<Description>()
        }?.findAnnotation<Description>()?.value?.let { AssertionError("Failed requirement: $it") }

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
    open fun isApplicable(element: T): Boolean = true

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
        require(this.isNotEmpty()) {
            "List of sources passed to StructuralQuestionTemplate.getRandom must not be empty!"
        }

        val elements: Map<SourceCode, Pair<List<R>, Throwable?>> = this.associateWith { source ->
            val result = source.load()
            val applicable = result.getOrNull()?.findAll(R::class.java) {
                isApplicable(it)
            } ?: emptyList()
            val error = result.exceptionOrNull() ?: (if (applicable.isEmpty()) getFailedRequirementException() else null)
            Pair(applicable, error)
        }

        val errors = elements.mapValues { it.value.second }
        if (errors.none { it.value == null })
            throw ApplicableSourceNotFoundException(this@StructuralQuestionTemplate, errors)

        if (elements.none { it.value.first.isNotEmpty() })
            throw ApplicableElementNotFoundException(this@StructuralQuestionTemplate, errors, R::class)

        val source = elements.randomKeyBy { errors[it.key] == null && it.value.first.isNotEmpty() }
        val element = elements[source]!!.first.random()
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

    fun generate(source: SourceCode, language: Language = Language.DEFAULT): Question =
        generate(listOf(source), language)

    fun generate(unit: CompilationUnit, language: Language = Language.DEFAULT) =
        generate(listOf(SourceCode(unit)), language)

    fun generate(src: String, language: Language = Language.DEFAULT) =
        generate(listOf(SourceCode(src)), language)

    protected abstract fun build(sources: List<SourceCode>, language: Language = Language.DEFAULT): Question

    override fun <R : T> getApplicableElements(source: SourceCode, type: KClass<R>): List<R> =
        source.load().getOrNull()?.findAll(type.java) { isApplicable(it) } ?: emptyList()
}

/**
 * This class provides a representation of a QLC targeting *dynamic* code elements, i.e. elements which depend
 * on the code's execution and depend on the methods' provided input.
 *
 * @param T The type of Strudel - [pt.iscte.strudel] - element that the question targets, e.g.
 * [IProcedure] if the question targets concrete methods.
 */
abstract class DynamicQuestionTemplate<T : IProgramElement> : QuestionTemplate<T>() {

    private data class ApplicableProcedureAndArguments(
        val procedure: IProcedure,
        val arguments: List<Any?>,
        val exception: Throwable?,
        private val applicable: Boolean
    ) {
        val isApplicable: Boolean
            get() = exception == null && applicable
    }

    private fun List<ApplicableProcedureAndArguments>.anyApplicable(): Boolean =
        any { it.isApplicable }

    private fun DynamicQuestionTemplate<IProcedure>.getProceduresAndArguments(
        module: IModule,
        calls: List<ProcedureCall>
    ): List<ApplicableProcedureAndArguments> {
        val pairs = mutableListOf<ApplicableProcedureAndArguments>()
        module.procedures.filterIsInstance<IProcedure>().filter { !it.hasFlag(CONSTRUCTOR_FLAG) }.forEach { p ->
            calls.forEach { call ->
                val vm = IVirtualMachine.create()
                val args = call.arguments.toIValues(vm, module)
                val applicableResult = runCatching { isApplicable(p) && isApplicable(p, args) }
                val isApplicable = applicableResult.getOrDefault(false)

                val failedRequirement =
                    if (!isApplicable && applicableResult.isSuccess) this.getFailedRequirementException()
                    else null

                if (call.id == null) {
                    // Wildcard test cases
                    val result = runCatching { vm.execute(p, *args.toTypedArray()) }
                    pairs.add(ApplicableProcedureAndArguments(
                        p,
                        call.arguments,
                        result.exceptionOrNull() ?: failedRequirement,
                        result.isSuccess && isApplicable
                    ))
                } else if (call.id == p.id && p.id?.startsWith("$") == false) {
                    // Specific test cases
                    pairs.add(ApplicableProcedureAndArguments(
                        p,
                        call.arguments,
                        applicableResult.exceptionOrNull() ?: failedRequirement,
                        isApplicable
                    ))
                }
            }
        }
        return pairs
    }

    protected fun DynamicQuestionTemplate<IProcedure>.getRandomProcedure(
        sources: List<SourceCode>
    ): Quadruple<SourceCode, IModule, IProcedure, List<Any?>> {
        require(sources.isNotEmpty()) {
            "List of sources passed to DynamicQuestionTemplate.getRandomProcedure must not be empty!"
        }

        val loadingErrors = mutableMapOf<SourceCode, Throwable?>()
        val calls: Map<SourceCode, List<ApplicableProcedureAndArguments>> = sources.associateWith { source ->
            runCatching {
                val module = Java2Strudel(checkJavaCompilation = false).load(source.code)
                getProceduresAndArguments(module, source.calls)
            }.onFailure {
                loadingErrors[source] = it
            }.getOrDefault(emptyList())
        }

        if (calls.none { loadingErrors[it.key] == null })
            throw ApplicableSourceNotFoundException(this, loadingErrors)

        if (calls.none { it.value.anyApplicable() }) {
            val errors = calls.mapValues { (source, applicable) ->
                applicable.mapNotNull { it.exception } + (
                    if (loadingErrors[source] != null) listOf(loadingErrors[source]!!) else emptyList()
                )
            }.filterValues { it.isNotEmpty() }
            throw ApplicableProcedureCallNotFoundException(this, errors)
        }

        val source = calls.randomKeyBy { loadingErrors[it.key] == null && it.value.anyApplicable() }
        val (procedure, args, _) = calls[source]!!.randomBy { it.isApplicable }
        val module = Java2Strudel(checkJavaCompilation = false).load(source.code)
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

    fun generate(source: SourceCode, language: Language = Language.DEFAULT): Question =
        generate(listOf(source), language)

    fun generate(src: String, call: ProcedureCall, language: Language = Language.DEFAULT) =
        generate(listOf(SourceCode(src, listOf(call))), language)

    fun generate(unit: CompilationUnit, call: ProcedureCall, language: Language = Language.DEFAULT) =
        generate(listOf(SourceCode(unit, listOf(call))), language)

    protected open fun isApplicable(element: T, args: List<IValue>): Boolean = true // isApplicable(element)

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