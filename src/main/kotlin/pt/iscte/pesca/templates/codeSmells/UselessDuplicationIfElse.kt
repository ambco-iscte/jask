package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*

class UselessDuplicationIfElse : StaticQuestionTemplate<MethodDeclaration>() {


    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.hasDuplicatedIfElse()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        // Find a random IfStmt that has duplicate code
        val ifStmt = methodReplaced.findAll(IfStmt::class.java)
            .filter { it.hasDuplicateCode() }
            .random()

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
