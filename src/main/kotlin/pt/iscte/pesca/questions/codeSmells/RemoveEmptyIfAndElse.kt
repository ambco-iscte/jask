package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*

class RemoveEmptyIfAndElse : StaticQuestion<MethodDeclaration>() {

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

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        var ifStmt = methodReplaced.findAll(IfStmt::class.java).first {
            if (it.thenStmt.isBlockStmt) {
                it.thenStmt.asBlockStmt().isEmpty ||
                        (it.elseStmt.isPresent && it.elseStmt.get().isBlockStmt && it.elseStmt.get()
                            .asBlockStmt().isEmpty)
            } else {
                false
            }
        }

        do {
            if (ifStmt.elseStmt.isPresent) {
                if (ifStmt.elseStmt.get().isBlockStmt && ifStmt.elseStmt.get().asBlockStmt().isEmpty) {
                    ifStmt.removeElseStmt()
                } else {
                    ifStmt.setThenStmt(ifStmt.elseStmt.get())
                    ifStmt.removeElseStmt()
                    ifStmt.setCondition(negateExpression(ifStmt.condition))
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

        return QuestionData(
            source,
            TextWithMultipleCodeStatements(
                language["RemoveEmptyIfAndElse"].format(method.nameAsString),
                listOf(method.toString(), methodReplaced.toString())
            ),
            true.trueOrFalse(language),
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
                    y = 1;
                }
                if(a && y > 0)
                    if(a && y > 0){
                    
                    }else{
                        
                    }
                
                
            }
        
        }
    """.trimIndent()

    val qlc = RemoveEmptyIfAndElse()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
