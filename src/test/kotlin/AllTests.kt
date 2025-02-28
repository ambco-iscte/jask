import compiler.TestFindUnknownMethod
import compiler.TestFindUnknownType
import compiler.TestFindUnknownVariable
import compiler.TestIncompatibleReturnType
import compiler.TestIncompatibleVariableType
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

    // Dynamic Questions
    TestArraySize::class,
    TestHowManyArrayAllocations::class,
    TestWhichReturnExecuted::class,

    // Compiler Errors
    TestFindUnknownMethod::class,
    TestFindUnknownType::class,
    TestFindUnknownVariable::class,
    TestIncompatibleReturnType::class,
    TestIncompatibleVariableType::class
)
class AllTests