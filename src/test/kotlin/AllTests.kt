import dynamic.TestWhatIsResult
import dynamic.TestWhichVariableRole
import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import fixed.*

@Suite
@SelectClasses(
    // Static
    TestHowManyParameters::class,
    TestIsRecursive::class,
    TestHowManyVariables::class,
    TestHowManyLoops::class,
    TestCallsOtherFunctions::class,
    TestCanCallAMethodWithGivenArguments::class,
    TestWhatIsTheReturnType::class,

    // Dynamic
    TestWhatIsResult::class,
    TestWhichVariableRole::class
)
class AllTests