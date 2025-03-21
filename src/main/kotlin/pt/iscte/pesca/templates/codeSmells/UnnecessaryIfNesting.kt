package pt.iscte.pesca.templates

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.InitializerDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.PrimitiveType
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*
import pt.iscte.strudel.model.INT

public class UnnecessaryIfNesting  : StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val list = element.findAll(IfStmt::class.java).filter { it->
                (!it.elseStmt.isPresent && it.thenStmt.isBlockStmt
                        && it.thenStmt.asBlockStmt().statements.size == 1
                        && it.thenStmt.asBlockStmt().statements.first().isIfStmt)
                        || ( !it.elseStmt.isPresent && !it.thenStmt.isBlockStmt && it.thenStmt.isIfStmt)
        }
        return list.isNotEmpty()
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        while (methodReplaced.findAll(IfStmt::class.java).filter { it->
                (!it.elseStmt.isPresent && it.thenStmt.isBlockStmt
                        && it.thenStmt.asBlockStmt().statements.size == 1
                        && it.thenStmt.asBlockStmt().statements.first().isIfStmt)
                        || ( !it.elseStmt.isPresent && !it.thenStmt.isBlockStmt && it.thenStmt.isIfStmt)
            }.randomOrNull() != null) {
            val ifStmtFather = methodReplaced.findAll(IfStmt::class.java).filter { it ->
                (!it.elseStmt.isPresent && it.thenStmt.isBlockStmt
                        && it.thenStmt.asBlockStmt().statements.size == 1
                        && it.thenStmt.asBlockStmt().statements.first().isIfStmt)
                        || (!it.elseStmt.isPresent && !it.thenStmt.isBlockStmt && it.thenStmt.isIfStmt)
            }.random()

            var ifStmtSon = IfStmt()
            if (ifStmtFather.thenStmt.isBlockStmt) {
                ifStmtSon = ifStmtFather.thenStmt.asBlockStmt().statements.first().asIfStmt()
            } else {
                ifStmtSon = ifStmtFather.thenStmt.asIfStmt()
            }

            ifStmtFather.thenStmt = ifStmtSon.thenStmt
            ifStmtFather.condition = BinaryExpr(
                wrapIfNeeded(ifStmtFather.condition),
                wrapIfNeeded(ifStmtSon.condition),
                BinaryExpr.Operator.AND
            )

        }
        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UnnecessaryIfNesting"].format(method.nameAsString),
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
                int a = 3;
                int var = 3;
                if(var > 2){
                    if(var > 1)
                        if(var > 0 || var > 0)
                            var = 3;
                        
                    
                    var = 4;
                }
            }
        
        }
    """.trimIndent()
    val qlc = UnnecessaryIfNesting()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
