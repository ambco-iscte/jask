package pt.iscte.pesca.questions.subtypes

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.QuestionGenerationException
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion

abstract class JavaParserQuestionRandomMethod : StaticQuestion<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()
        return build(source, method, language)
    }

    protected abstract fun build(source: SourceCode, method: MethodDeclaration, language: Language): QuestionData
}