package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*

class UnnecessaryCodeAfterReturn : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val body = element.body.orElse(null) ?: return false
        return findReturnWithDeadCode(body)!=null
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWrongAnswer = method.clone()
        val methodReplacedWrongAnswer2 = method.clone()

        val returnStmt = findReturnWithDeadCode(methodReplaced.body.get())
        val returnStmtWA = findReturnWithDeadCode(methodReplacedWrongAnswer.body.get())
        val returnStmtWA2 = findReturnWithDeadCode(methodReplacedWrongAnswer2.body.get())


        val parentBlock = returnStmt!!.parentNode.get() as BlockStmt
        val stmts = parentBlock.statements
        val returnIndex = stmts.indexOf(returnStmt)
        if (returnIndex != -1 && returnIndex < stmts.size - 1) {
            val toRemove = stmts.subList(returnIndex + 1, stmts.size)
            toRemove.clear()
        }
        val parentBlockWA = returnStmtWA!!.parentNode.get() as BlockStmt
        val stmtsWA = parentBlockWA.statements
        val returnIndexWA = stmtsWA.indexOf(returnStmtWA)
        if (returnIndexWA != -1 && returnIndexWA < stmtsWA.size - 1) {
            val toRemoveWA = stmtsWA.subList(returnIndexWA + 1, stmtsWA.size)
            toRemoveWA.clear()
        }
        val parentBlockWA2 = returnStmtWA2!!.parentNode.get() as BlockStmt
        val stmtsWA2 = parentBlockWA2.statements
        val returnIndexWA2 = stmtsWA2.indexOf(returnStmtWA2)
        val toRemoveWA2 = stmtsWA2.subList(returnIndexWA2,returnIndexWA2+1)
        toRemoveWA2.clear()

        if (returnStmtWA.expression.isPresent && returnStmtWA.expression.get() is NullLiteralExpr) {
            val newReturn = ReturnStmt(StringLiteralExpr(""))
            returnStmtWA.replace(newReturn)
        }else{
            val returnStmtWAnew = ReturnStmt("null")
            returnStmtWA.replace(returnStmtWAnew)
        }



        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UnnecessaryCodeAfterReturn"].format(method.nameAsString),
                listOf(method.toString())
            ),
            mapOf(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWrongAnswer.toString() ) to false,
                SimpleTextOption(methodReplacedWrongAnswer2.toString() ) to false
            ),
            language = language
        )
    }
}


fun main() {
    val source = """
        class abc{
            
            public Int test(){
                if(i=="test"){
                    return;
                    i = "unnecessary";
                }
                return "needed";
            }
        
        }
    """.trimIndent()

    val qlc = UnnecessaryCodeAfterReturn()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
