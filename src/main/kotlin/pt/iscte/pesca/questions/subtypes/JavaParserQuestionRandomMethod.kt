package pt.iscte.pesca.questions.subtypes

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.QuestionGenerationException
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion

abstract class JavaParserQuestionRandomMethod : StaticQuestion<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val source = sources.filter { isApplicable(it, MethodDeclaration::class) }.randomOrNull() ?:
        throw QuestionGenerationException(this, null, "Could not find source with at least one applicable method.")

        val method = getApplicableElements<MethodDeclaration>(source).randomOrNull() ?:
        throw QuestionGenerationException(this, source, "Could not find applicable method within source.")

        return build(method, language).apply {
            this.type = this@JavaParserQuestionRandomMethod::class.simpleName
            this.source = source
        }
    }

    protected abstract fun build(method: MethodDeclaration, language: Language): QuestionData
}