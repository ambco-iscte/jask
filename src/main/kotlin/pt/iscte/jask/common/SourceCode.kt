package pt.iscte.jask.common

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import java.io.File

operator fun String?.invoke(vararg arguments: Any?): ProcedureCall =
    ProcedureCall(this, arguments.toList())

data class ProcedureCall(val id: String?, val arguments: List<Any?> = emptyList()) {
    override fun toString(): String =
        "$id(${arguments.joinToString()})"
}

data class RecordTypeData(val name: String, val fields: List<Any?>) {
    override fun toString(): String = "$name[${fields.joinToString()}]"
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