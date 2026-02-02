package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithMultipleCodeStatements

class IFReturnCondition : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val ifStmt = element.findAll(IfStmt::class.java).firstOrNull {
            ((it.thenStmt.isReturnStmt
                    && (it.thenStmt.asReturnStmt().expression.get().isBooleanLiteralExpr) &&
                    (it.thenStmt.asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            || it.thenStmt.asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                    ) ||
                    (it.thenStmt.isBlockStmt  &&
                            it.thenStmt.asBlockStmt().statements.first.get().isReturnStmt
                            && (it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                            && (it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            ||it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false)))
                            ))
                    &&
                    ((it.elseStmt.isPresent && it.elseStmt.get().isReturnStmt
                            && it.elseStmt.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                            &&(it.elseStmt.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            || it.elseStmt.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                            ) ||
                            (it.elseStmt.get().isBlockStmt &&
                                    it.elseStmt.get().asBlockStmt().statements.first.get().isReturnStmt
                                    && it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                                    &&(it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                                    || it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                                    ))
        }
        return ifStmt != null
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWrongAnswer = methodReplaced.clone()

        val ifStmt = methodReplaced.findAll(IfStmt::class.java).first {
            ((it.thenStmt.isReturnStmt
                    && (it.thenStmt.asReturnStmt().expression.get().isBooleanLiteralExpr) &&
                    (it.thenStmt.asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            || it.thenStmt.asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                    ) ||
                    (it.thenStmt.isBlockStmt  &&
                            it.thenStmt.asBlockStmt().statements.first.get().isReturnStmt
                            && (it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                            && (it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            ||it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false)))
                            ))
                    &&
                    ((it.elseStmt.isPresent && it.elseStmt.get().isReturnStmt
                            && it.elseStmt.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                            &&(it.elseStmt.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            || it.elseStmt.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                            ) ||
                            (it.elseStmt.get().isBlockStmt &&
                                    it.elseStmt.get().asBlockStmt().statements.first.get().isReturnStmt
                                    && it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                                    &&(it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                                    || it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                                    ))
        }
        val ifStmtWA = methodReplacedWrongAnswer.findAll(IfStmt::class.java).first {
            ((it.thenStmt.isReturnStmt
                    && (it.thenStmt.asReturnStmt().expression.get().isBooleanLiteralExpr) &&
                    (it.thenStmt.asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            || it.thenStmt.asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                    ) ||
                    (it.thenStmt.isBlockStmt  &&
                            it.thenStmt.asBlockStmt().statements.first.get().isReturnStmt
                            && (it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                            && (it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            ||it.thenStmt.asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false)))
                            ))
                    &&
                    ((it.elseStmt.isPresent && it.elseStmt.get().isReturnStmt
                            && it.elseStmt.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                            &&(it.elseStmt.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                            || it.elseStmt.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                            ) ||
                            (it.elseStmt.get().isBlockStmt &&
                                    it.elseStmt.get().asBlockStmt().statements.first.get().isReturnStmt
                                    && it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().isBooleanLiteralExpr
                                    &&(it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(true)
                                    || it.elseStmt.get().asBlockStmt().statements.first.get().asReturnStmt().expression.get().asBooleanLiteralExpr().value.equals(false))
                                    ))
        }


        val ifReturn = extractReturnExpr(ifStmt.thenStmt)
        val elseReturn = extractReturnExpr(
            ifStmt.elseStmt.orElseThrow { IllegalStateException("if não tem else") }
        )

        if (ifReturn!!.asBooleanLiteralExpr().value.equals(elseReturn!!.asBooleanLiteralExpr().value)){
                ifStmt.replace(ReturnStmt(ifReturn))
                ifStmtWA.replace(ReturnStmt(negateExpression(ifReturn)))
        }else{
            if (ifReturn.asBooleanLiteralExpr().value.equals(false)){
                ifStmt.replace( ReturnStmt(negateExpression(ifStmt.condition)))
                ifStmtWA.replace(ReturnStmt(ifStmt.condition))
            }else{
                ifStmt.replace(ReturnStmt(ifStmt.condition))
                ifStmtWA.replace(ReturnStmt(negateExpression(ifStmt.condition)))
            }
        }



        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["IFReturnCondition"].format(method.nameAsString),
                listOf(method.toString())
            ),
            mapOf(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWrongAnswer.toString()) to false
            ),
            language = language
        )
    }
}


fun main() {
    val source = """
        class abc{
            
            public boolean test(){
                if(product % 2 == 0){
                    return false;}
                else
                    return true;
                    
                    
                
            }
        
        }
    """.trimIndent()

    val qlc = IFReturnCondition()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
