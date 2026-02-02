package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithMultipleCodeStatements

class RemoveEmptyIfAndElse : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val ifStmt = element.findAll(IfStmt::class.java).firstOrNull {
            if (it.thenStmt.isBlockStmt) {
                it.thenStmt.asBlockStmt().isEmpty ||
                        (it.elseStmt.isPresent && it.elseStmt.get().isBlockStmt && it.elseStmt.get()
                            .asBlockStmt().isEmpty)
            } else {
                false
            }
        }
        return ifStmt != null
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWA = method.clone()

        var ifStmt = methodReplaced.findAll(IfStmt::class.java).first {
            if (it.thenStmt.isBlockStmt) {
                it.thenStmt.asBlockStmt().isEmpty ||
                        (it.elseStmt.isPresent && it.elseStmt.get().isBlockStmt && it.elseStmt.get()
                            .asBlockStmt().isEmpty)
            }else
            false
        }
        var ifStmtWA = methodReplacedWA.findAll(IfStmt::class.java).first {
            if (it.thenStmt.isBlockStmt) {
                it.thenStmt.asBlockStmt().isEmpty ||
                        (it.elseStmt.isPresent && it.elseStmt.get().isBlockStmt && it.elseStmt.get()
                            .asBlockStmt().isEmpty)
            } else
            false
        }

        do {
            if (ifStmt.elseStmt.isPresent) {
                if (ifStmt.elseStmt.get().isBlockStmt && ifStmt.elseStmt.get().asBlockStmt().isEmpty) {
                    ifStmt.removeElseStmt()

                    ifStmtWA.setCondition(negateExpression(ifStmtWA.condition))
                    ifStmtWA.removeElseStmt()
                } else {
                    ifStmt.setThenStmt(ifStmt.elseStmt.get())
                    ifStmt.removeElseStmt()
                    ifStmt.setCondition(negateExpression(ifStmt.condition))

                    ifStmtWA.setThenStmt(ifStmtWA.elseStmt.get())
                    ifStmtWA.removeElseStmt()
                }
            } else {
                val thenStmt = ifStmt.thenStmt
                val parent = ifStmt.parentNode.get()
                if (parent is BlockStmt) {
                    val parentStatements = parent.asBlockStmt().statements
                    parentStatements.remove(ifStmt)
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

                val lastreturn = method.findAll(ReturnStmt::class.java).lastOrNull()
                if (lastreturn != null &&
                    !lastreturn.expression.isEmpty &&
                    lastreturn.expression.get().isBooleanLiteralExpr &&
                    lastreturn.expression.get().asBooleanLiteralExpr().value.equals(false)){
                        val newReturn = ReturnStmt("true")
                        ifStmtWA.setThenStmt(newReturn)
                }else{
                    val newReturn = ReturnStmt("false")
                    ifStmtWA.setThenStmt(newReturn)
                }

            }
            ifStmt = methodReplaced.findAll(IfStmt::class.java).firstOrNull {
                if (it.thenStmt.isBlockStmt) {
                    it.thenStmt.asBlockStmt().isEmpty ||
                            (it.elseStmt.isPresent && it.elseStmt.get().isBlockStmt && it.elseStmt.get()
                                .asBlockStmt().isEmpty)
                } else {
                    false
                }
            }
        } while (ifStmt != null)

        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["RemoveEmptyIfAndElse"].format(method.nameAsString),
                listOf(method.toString())
            ),
            mapOf(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWA.toString()) to false
            ),
            language = language
        )
    }
}


fun main() {
    val source = """
        class abc{
            
            public void test(){
                int y = 0;
                if(a && y > 0){
                }else{
                }
                return;
            }
        
        }
    """.trimIndent()

    val qlc = RemoveEmptyIfAndElse()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}

/*
if(a && y > 0)
    if(a && y > 0){

    }else{

    }
 */