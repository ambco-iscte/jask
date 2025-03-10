import compiler.TestAssignVarWithMethodWrongType
import compiler.TestMethodWithWrongReturnStmt
import compiler.TestReferencesUndefinedVariable
import compiler.TestWrongMethodCall
import errorfinder.TestFindUnknownMethod
import errorfinder.TestFindUnknownType
import errorfinder.TestFindUnknownVariable
import errorfinder.TestWrongReturnStmtType
import errorfinder.WrongTypeForVariableDeclaration
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

    // Compiler Questions
    TestAssignVarWithMethodWrongType::class,
    TestMethodWithWrongReturnStmt::class,
    TestReferencesUndefinedVariable::class,
    TestWrongMethodCall::class,

    // Compiler Errors
    TestFindUnknownMethod::class,
    TestFindUnknownType::class,
    TestFindUnknownVariable::class,
    TestWrongReturnStmtType::class,
    WrongTypeForVariableDeclaration::class
)
class AllTests