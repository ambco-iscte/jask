package pt.iscte.jask.common

enum class Mistake(val description: String) {

    ConfuseParameterNamesWithTypes(
        "Fails to differentiate between a method's parameter names and types"
    ),

    ConfuseParameterTypesWithNames(
        "Fails to differentiate between a method's parameter types and names"
    ),

    ConsiderMethodItsOwnDependency(
        "Considers a non-recursive method to be its own dependency"
    ),

    ConsiderNativeInstructionsAsFunctions(
        "Considers instructions such as 'return' to be functions"
    ),

    MissLastLoopIteratorAssignment(
        "Misses last iterator variable increment which causes loop guard to fail"
    ),

    MissLastVariableValue(
        "Misses the last value assigned to a variable"
    ),

    NonSpecificOffByOne(
        "Is off-by-one (non-specific)"
    ),

    NotCountInitialisationAsAssignment(
        "Does not count variable initialisation as a value assignment"
    );
}