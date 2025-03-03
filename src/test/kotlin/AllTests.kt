import compiler.TestFindUnknownMethod
import compiler.TestFindUnknownType
import compiler.TestFindUnknownVariable
import compiler.TestWrongReturnStmtType
import compiler.WrongTypeForVariableDeclaration
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import strudel.TestArraySize
import strudel.TestHowManyArrayAllocations
import javaparser.TestHowManyVariables
import strudel.TestWhichReturnExecuted

@Suite
@SelectClasses(
    // Static Questions
    TestHowManyVariables::class,
    // TODO

    // Dynamic Questions
    TestArraySize::class,
    TestHowManyArrayAllocations::class,
    TestWhichReturnExecuted::class,
    // TODO

    // Compiler Errors
    TestFindUnknownMethod::class,
    TestFindUnknownType::class,
    TestFindUnknownVariable::class,
    TestWrongReturnStmtType::class,
    WrongTypeForVariableDeclaration::class
)
class AllTests