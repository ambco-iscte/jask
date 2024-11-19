package pt.iscte.pesca.questions.subtypes

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion

abstract class JavaParserQuestionRandomMethod : StaticQuestion<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): QuestionData =
        build(getApplicableElements<MethodDeclaration>(sources).random(), language)

    protected abstract fun build(method: MethodDeclaration, language: Language): QuestionData
}