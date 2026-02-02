package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithMultipleCodeStatements

class UnnecessaryParameter : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val body = element.body.orElse(null) ?: return false

        val nameUsages = body.findAll(com.github.javaparser.ast.expr.NameExpr::class.java)
            .map { it.nameAsString }
            .toSet()

        val unnecessaryParameters = element.parameters.filter { param ->
            param.nameAsString !in nameUsages
        }
        return unnecessaryParameters.isNotEmpty()
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWA = method.clone()
        val methodReplacedWA2 = method.clone()

        val body = methodReplaced.body.get()
        val nameUsages = body.findAll(com.github.javaparser.ast.expr.NameExpr::class.java)
            .map { it.nameAsString }
            .toSet()
        val unnecessaryParameters = methodReplaced.parameters.filter { param ->
            param.nameAsString !in nameUsages
        }
        methodReplaced.parameters.removeAll(unnecessaryParameters)

        val bodyWA = methodReplacedWA.body.get()
        val nameUsagesWA = bodyWA.findAll(com.github.javaparser.ast.expr.NameExpr::class.java)
            .map { it.nameAsString }
            .toSet()


        methodReplacedWA.setType(methodReplacedWA.parameters.first.get().type)
        val returnStatements = methodReplacedWA.findAll(com.github.javaparser.ast.stmt.ReturnStmt::class.java)
        val newReturn = ReturnStmt(methodReplacedWA.parameters.first.get().nameAsString)
        if (returnStatements.isEmpty()) {
            methodReplacedWA.body.get().addStatement(newReturn)
        }else{
            returnStatements.forEach { returnStatement -> returnStatement.replace(newReturn) }
        }


        var options: MutableMap<QuestionOption, Boolean> = mutableMapOf(
            SimpleTextOption(methodReplaced.toString()) to true,
            SimpleTextOption(methodReplacedWA.toString()) to false
        )

        val bodyWA2 = methodReplacedWA2.body.get()
        val nameUsagesWA2 = bodyWA2.findAll(com.github.javaparser.ast.expr.NameExpr::class.java)
            .map { it.nameAsString }
            .toSet()
        val necessaryParametersWA2 = methodReplacedWA2.parameters.filter { param ->
            param.nameAsString in nameUsagesWA2
        }
        if (necessaryParametersWA2.isNotEmpty()) {
            methodReplacedWA2.parameters.removeAll(necessaryParametersWA2.toSet())
            options[SimpleTextOption(methodReplacedWA2.toString())]=false
        }else{
            options[SimpleTextOption(language["NoneOfTheAbove"])]=false
        }



        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UnnecessaryEqualsTrueOrFalse"].format(method.nameAsString),
                listOf(method.toString())
            ),
            options,
            language = language
        )
    }
}


fun main() {
    val source = """
        class abc{
            
            public String test(int b){
                if(a == true || a != true){
                    return a;
                }
                return "None";
            }
        
        }
    """.trimIndent()

    val qlc = UnnecessaryParameter()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
