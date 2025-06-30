package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.WhileStmt
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.sample
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichFunctionDependencies : StaticQuestionTemplate<MethodDeclaration>() {

    // Has at least one call statement to another function.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.findAll(MethodCallExpr::class.java).any {
            it.nameAsString != element.nameAsString
        }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val calls = method.findAll(MethodCallExpr::class.java)
        val callsNames = calls.map { it.nameWithScope()  }

        val herrings = mutableListOf<String>(method.nameAsString)
        if (method.findAll(WhileStmt::class.java).isNotEmpty())
            herrings.add("while")
        if (method.findAll(ForStmt::class.java).isNotEmpty())
            herrings.add("for")
        if (method.findAll(IfStmt::class.java).isNotEmpty())
            herrings.add("if")
        if (method.findAll(ReturnStmt::class.java).isNotEmpty())
            herrings.add("return")

        val others = mutableListOf<Set<String>>()
        while (others.size < 3) {
            val choice = (callsNames + herrings).toSet().sample(null).toSet()
            if (choice != callsNames)
                others.add(choice)
        }

        val options: MutableMap<Option, Boolean> =
            others.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(callsNames)] = true
        options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(language[this::class.simpleName!!].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = calls.map { SourceLocation(it) }
        )
    }
}