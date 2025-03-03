package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*
import pt.iscte.strudel.parsing.java.SourceLocation


class UselessDuplicationIfElse : StaticQuestion<MethodDeclaration>() {

    private fun IfStmt.hasDuplicateCode(): Boolean {
        if (elseStmt.isPresent && elseStmt.get().isBlockStmt) {
            val ifBody: BlockStmt = this.thenStmt.asBlockStmt()
            val elseBody: BlockStmt = elseStmt.get().asBlockStmt()
            if (ifBody == elseBody) {
                return true
            }
        }
        return false
    }

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.hasDuplicatedIfElse()

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        val ifStmt = methodReplaced.findAll(IfStmt::class.java).randomBy { it.hasDuplicateCode() }
        val body = ifStmt.thenStmt
        ifStmt.replace(body)


        return QuestionData(
            source,
            TextWithMultipleCodeStatements(
                language["UselessDuplicationIfElse"].format(method.nameAsString),listOf(method.toString(),methodReplaced.toString())),
            true.trueOrFalse(language),
            language = language,
            relevantSourceCode = listOf(SourceLocation(body)),
        )
}
}


fun main() {
    val source = """
        class abc{
            
            public void test(){
                if(a == true){
                    int y = 1+2;
                }else{
                    int y = 1+2;
                }
            }
        
        }
    """.trimIndent()

    val qlc = UselessDuplicationIfElse()
    val data = qlc.generate(source,Localisation.getLanguage("pt"))
    println(data)
}
