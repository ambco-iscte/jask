import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import staticquestions.*

@Suite
@SelectClasses(
    TestHowManyParameters::class,
    TestIsRecursive::class,
    TestHowManyVariables::class,
    TestHowManyLoops::class,
    TestCallsOtherFunctions::class,
    TestCanCallAMethodWithGivenArguments::class,
    TestWhatIsTheReturnType::class
)
class AllTests