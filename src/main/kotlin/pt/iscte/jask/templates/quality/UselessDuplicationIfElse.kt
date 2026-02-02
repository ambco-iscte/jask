package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithMultipleCodeStatements

class UselessDuplicationIfElse : StructuralQuestionTemplate<MethodDeclaration>() {


    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.hasDuplicatedIfElse()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWA = method.clone()
        val methodReplacedWA2 = method.clone()
        val methodReplacedRA = method.clone()


        val ifStmt = methodReplaced.findAll(IfStmt::class.java)
            .filter { it.hasDuplicateCode() }
            .first()

        val ifStmtWA = methodReplacedWA.findAll(IfStmt::class.java)
            .filter { it.hasDuplicateCode() }
            .first()

        val ifStmtWA2 = methodReplacedWA2.findAll(IfStmt::class.java)
            .filter { it.hasDuplicateCode() }
            .first()

        val ifStmtRA = methodReplacedRA.findAll(IfStmt::class.java)
            .filter { it.hasDuplicateCode() }
            .first()

        ifStmtWA.removeElseStmt()
        ifStmtWA2.removeElseStmt()
        ifStmtWA2.setCondition(negateExpression(ifStmtWA2.condition))
        ifStmtRA.removeElseStmt()
        ifStmtRA.setCondition(BooleanLiteralExpr(true))


        val thenStmt = ifStmt.thenStmt
        val parent = ifStmt.parentNode.get()
        val statements = if (thenStmt.isBlockStmt) {
            thenStmt.asBlockStmt().statements
        } else {
            listOf(thenStmt)  // Wrap single statement in a list
        }
        if (parent is BlockStmt) {
            val parentStatements = parent.asBlockStmt().statements
            val index = parentStatements.indexOf(ifStmt)
            parentStatements.addAll(index + 1, statements)
            parentStatements.removeAt(index)
        } else {
            if (parent is NodeWithBody<*>) {
                parent.body = thenStmt
            }
            if (parent is IfStmt)
                if (parent.thenStmt.equals(ifStmt)) {
                    parent.thenStmt.replace(thenStmt)
                } else {
                    parent.elseStmt.get().replace(thenStmt)
                }
        }

        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UselessDuplicationIfElse"].format(method.nameAsString),
                listOf(method.toString())
            ),
            mapOf(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWA.toString()) to false,
                SimpleTextOption(methodReplacedWA2.toString()) to false,
                SimpleTextOption(methodReplacedRA.toString()) to true
            ),
            language = language

        )
    }
}

fun main() {
    val source = """
        class abc{
            
            public void test(){
                if(a == true){
                    y = 1+2;
                }
                else {
                    y = 1+2;
                }
            }
        
        }
    """.trimIndent()

    val qlc = UselessDuplicationIfElse()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
