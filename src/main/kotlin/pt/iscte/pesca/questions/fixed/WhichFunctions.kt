package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.WhileStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.asString
import pt.iscte.pesca.extensions.hasMethodCalls
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class WhichFunctions : JavaParserQuestionRandomMethod() {

    // Has at least one call statement to another function.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.findAll(MethodCallExpr::class.java).any {
            it.nameAsString != element.nameAsString
        }

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
        val calls = method.findAll(MethodCallExpr::class.java).map { it.asString()  }

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
            val choice = (calls + herrings).toSet().sample(null).toSet()
            if (choice != calls)
                others.add(choice)
        }

        val options: MutableMap<Option, Boolean> =
            others.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(calls)] = true
        options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(language["WhichFunctions"].format(method.nameAsString), method),
            options,
            language = language
        )
    }
}