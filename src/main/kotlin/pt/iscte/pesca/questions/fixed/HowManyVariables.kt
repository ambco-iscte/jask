package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.getLocalVariables
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.text.format

class HowManyVariables : StaticQuestion<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()
        val localVariables = method.getLocalVariables()
        val howManyVariables = localVariables.size

        return QuestionData(
            TextWithCodeStatement(language["HowManyVariables"].format(method.nameAsString), method.toString()),
            howManyVariables.multipleChoice(language),
            language = language,
            relevantSourceCode = localVariables.map { SourceLocation(it) }
        )
    }
}