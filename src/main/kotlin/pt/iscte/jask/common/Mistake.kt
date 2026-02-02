package pt.iscte.jask.common

enum class Mistake(val description: String) {

    NotCountInitialisationAsAssignment(
        "Does not count variable initialisation as a value assignment"
    ),

    MissLastVariableValue(
        "Misses the last value assigned to a variable"
    ),

    MissLastLoopIteratorAssignment(
        "Misses last iterator variable increment which causes loop guard to fail"
    ),

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

    NonSpecificOffByOne(
        "Is off-by-one (non-specific)"
    );
}