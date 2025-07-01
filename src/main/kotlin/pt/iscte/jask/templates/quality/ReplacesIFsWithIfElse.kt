package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*

class ReplacesIFsWithIfElse : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val ifStmts = element.findAll(IfStmt::class.java)

        for (i in 0 until ifStmts.size - 1) {
            val firstIf = ifStmts[i]
            val secondIf = ifStmts[i + 1]


            if (firstIf.parentNode.orElse(null) == secondIf.parentNode.orElse(null)) {

                if (firstIf.condition.equals(secondIf.condition)
                    || firstIf.condition.equals(negateExpression( secondIf.condition))) {
                    return true
                }
            }
        }

        return false
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWrongAnswer = methodReplaced.clone()

        val ifStmts = methodReplaced.findAll(IfStmt::class.java)
        val ifStmtsWA = methodReplacedWrongAnswer.findAll(IfStmt::class.java)
        var matchingIfs: Pair<IfStmt, IfStmt>? = null
        var matchingIfsWA: Pair<IfStmt, IfStmt>? = null

        for (i in 0 until ifStmts.size - 1) {
            val firstIf = ifStmts[i]
            val secondIf = ifStmts[i + 1]
            if (firstIf.parentNode.orElse(null) == secondIf.parentNode.orElse(null)) {
                if (firstIf.condition.equals(secondIf.condition) || firstIf.condition.equals(negateExpression(secondIf.condition))) {
                    matchingIfs = Pair(firstIf, secondIf)
                    break
                }
            }
        }
        for (i in 0 until ifStmtsWA.size - 1) {
            val firstIf = ifStmtsWA[i]
            val secondIf = ifStmtsWA[i + 1]
            if (firstIf.parentNode.orElse(null) == secondIf.parentNode.orElse(null)) {
                if (firstIf.condition.equals(secondIf.condition) || firstIf.condition.equals(negateExpression(secondIf.condition))) {
                    matchingIfsWA = Pair(firstIf, secondIf)
                    break
                }
            }
        }

        val (firstIf, secondIf) = matchingIfs!!
        val (firstIfWA, secondIfWA) = matchingIfsWA!!

        val combinedBlock = BlockStmt()
        if (firstIf.thenStmt.isBlockStmt){
            firstIf.thenStmt.asBlockStmt().statements.forEach { stmt ->
                combinedBlock.addStatement(stmt.clone())
            }
        }else {
            combinedBlock.addStatement(firstIf.thenStmt.clone())
        }
        if (secondIf.thenStmt.isBlockStmt){
            secondIf.thenStmt.asBlockStmt().statements.forEach { stmt ->
                combinedBlock.addStatement(stmt.clone())
            }
        }else {
            combinedBlock.addStatement(secondIf.thenStmt.clone())
        }
        val mergedIf = IfStmt()
        mergedIf.setCondition(firstIf.condition.clone())
        mergedIf.setThenStmt(combinedBlock)
        firstIf.replace(mergedIf)
        secondIf.remove()

        val mergedIfElseWA = IfStmt()
        mergedIfElseWA.setCondition(firstIfWA.condition.clone())
        mergedIfElseWA.setThenStmt(firstIf.thenStmt.clone())
        mergedIfElseWA.setElseStmt(secondIf.thenStmt.clone())
        firstIfWA.replace(mergedIfElseWA)
        secondIfWA.remove()

        val methodReplacedAnswer = firstIf.condition.equals(secondIf.condition)




        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["ReplacesIFsWithIfElse"].format(method.nameAsString),
                listOf(method.toString())
            ),
            mapOf(
                SimpleTextOption(methodReplaced.toString()) to methodReplacedAnswer,
                SimpleTextOption(methodReplacedWrongAnswer.toString()) to !methodReplacedAnswer
            ),
            language = language
        )
    }
}


fun main() {
    val source = """
        class abc{
            
            public String test(){
                String i = null;
                if(product % 2 == 0)
                    i = "first";
                if(product % 2 != 0){
                    i = "second";
                }
                return i;
            }
        
        }
    """.trimIndent()

    val qlc = ReplacesIFsWithIfElse()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
