package pt.iscte.jask.errors.compiler

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.NameExpr
import pt.iscte.jask.Language
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextStatement
import pt.iscte.jask.errors.ICompilerError
import pt.iscte.jask.errors.VariableScoping
import pt.iscte.jask.errors.compiler.templates.WhichVariablesUsableAtLine
import pt.iscte.jask.templates.structural.WhatVariables
import pt.iscte.jask.templates.structural.WhichParametersSingleChoice
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.jvm.optionals.getOrNull

data class UnknownVariable(val expr: NameExpr, val scope: VariableScoping.Scope<*>): ICompilerError {

    override fun toQLC(unit: CompilationUnit, language: Language): QuestionSequenceWithContext =
        QuestionSequenceWithContext(
            SimpleTextStatement(language["ReferencesUndefinedVariable"].format(
                expr.nameAsString,
                expr.range.get().begin.line
            )),
            listOf(
                WhatVariables().generate(unit, language),
                WhichParametersSingleChoice().generate(unit, language),
                WhichVariablesUsableAtLine(this).generate(unit, language)
            )
        )

    override fun message(): String =
        "Unknown variable $expr at ${
            expr.begin.getOrNull()?.line?.let {
                "line $it"
            } ?: ""
        }:\t${expr.parentNode.getOrNull ?: expr}"
}