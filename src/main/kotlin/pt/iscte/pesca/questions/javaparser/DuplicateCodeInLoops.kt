package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.randomBy

class DuplicateCodeInLoops(val method: String) : StaticQuestion<MethodDeclaration>() {

    private fun IfStmt.hasDuplicateCode(): Boolean {
        if (!elseStmt.isPresent)
            return false
        TODO("How to check for duplicate code within if/else statements?")
    }

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.findAll(IfStmt::class.java).any { it.hasDuplicateCode() }

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val ifStmt = method.findAll(IfStmt::class.java).randomBy { it.hasDuplicateCode() }

        // ...
        TODO("Implement this question!")
    }
}