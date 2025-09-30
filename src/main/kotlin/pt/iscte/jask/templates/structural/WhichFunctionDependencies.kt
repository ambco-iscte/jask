package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.WhileStmt
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.getLoopControlStructures
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class WhichFunctionDependencies : StructuralQuestionTemplate<MethodDeclaration>() {

    // Has at least one call statement to another function.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.findAll(MethodCallExpr::class.java).any {
            it.nameAsString != element.nameAsString
        }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val calls = method.findAll(MethodCallExpr::class.java)
        val callsNames = calls.map { it.nameAsString }.toSet()

        val controlStructures = method.getLoopControlStructures().map { (control, condition) ->
            when (control) {
                is IfStmt -> "if"
                is WhileStmt, is DoStmt -> "while"
                is ForStmt, is ForEachStmt -> "for"
                else -> toString()
            }
        }.toSet()

        val classes = (
            method.findCompilationUnit().getOrNull?.findAll(TypeDeclaration::class.java) ?: emptyList()
        ).map { it.nameAsString!! }.toSet()

        val otherFunctions = (
            method.findCompilationUnit().getOrNull?.findAll(MethodDeclaration::class.java) ?: emptyList()
        ).minus(method).map { it.nameAsString!! }.toSet()

        val returns =
            if (method.findFirst(ReturnStmt::class.java).isPresent) setOf("return")
            else emptySet()

        val types = listOf(method.type.toString()).plus(method.getLocalVariables().map { it.typeAsString }).plus(method.parameters.map { it.typeAsString }).toSet()

        val modifiers = method.modifiers.map { it.keyword.asString() }.toSet()

        val distractors = sampleSequentially(3, listOf(
            callsNames.plus(method.nameAsString) to null,
            otherFunctions to null,
            // classes to null,
            otherFunctions.plus(method.nameAsString) to null,
            // classes.plus(method.nameAsString) to null,
            listOf(method.nameAsString) to null,
        ), listOf(
            controlStructures to language["WhichFunctionDependencies_DistractorControlStructures"].format(controlStructures.joinToString()),
            callsNames.plus(controlStructures) to language["WhichFunctionDependencies_DistractorNamesAndControlStructures"].format(controlStructures.joinToString()),
            returns to language["WhichFunctionDependencies_DistractorReturnStmts"].format("return"),
            controlStructures.plus(returns) to language["WhichFunctionDependencies_DistractorControlsAndReturns"].format(controlStructures.joinToString(), "return"),
            callsNames.plus(returns) to null,
            callsNames.plus(controlStructures).plus(returns) to null,
            types to language["WhichFunctionDependencies_DistractorTypes"].format(),
            modifiers to language["WhichFunctionDependencies_DistractorModifiers"].format("public", "static"),
            types.plus(modifiers) to null,
            callsNames.plus(types) to null,
            callsNames.plus(modifiers) to null
        )) {
            it.first != callsNames && it.first.isNotEmpty()
        }.toMutableList()

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(
            callsNames.minus(method.nameAsString),
            language["WhichFunctionDependencies_Correct"].format()
        )] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(language["WhichFunctionDependencies"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = calls.map { SourceLocation(it) }
        )
    }
}