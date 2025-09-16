import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite

import dynamic.*
import structural.*
import compiler.*

@Suite
@SelectClasses(
    // Dynamic Questions
    TestHowDeepCallStack::class,
    TestHowManyArrayAllocations::class,
    TestHowManyArrayReads::class,
    TestHowManyArrayWrites::class,
    TestHowManyFunctionCalls::class,
    TestHowManyLoopIterations::class,
    TestHowManyVariableAssignments::class,
    TestWhatArraySize::class,
    TestWhatIsResult::class,
    TestWhichLastVariableValues::class,
    TestWhichReturnExecuted::class,
    TestWhichVariableValues::class,

    // Structural Questions
    TestCallsOtherFunctions::class,
    TestHowManyFunctionDependencies::class,
    TestHowManyLoops::class,
    TestHowManyParams::class,
    TestHowManyVariables::class,
    TestIsRecursive::class,
    TestWhatVariables::class,
    TestWhichFixedVariables::class,
    TestWhichFunctionDependencies::class,
    TestWhichParameters::class,
    TestWhichParameterTypes::class,
    TestWhichReturnType::class,
    TestWhichVariableHoldsReturn::class,
    TestWhichVariableRole::class,

    // Compiler Errors
    TestAssignVarWithMethodWrongType::class,
    TestMethodWithWrongReturnStmt::class,
    TestReferencesUndefinedVariable::class,
    TestWrongMethodCall::class,
    TestFindUnknownMethod::class,
    TestFindUnknownType::class,
    TestFindUnknownVariable::class,
    TestWrongReturnStmtType::class,
    TestWrongTypeForVariableDeclaration::class
)
class AllTests