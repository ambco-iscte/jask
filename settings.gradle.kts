plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "jask"

include(":jask", ":strudel")
project(":strudel").projectDir = file("../strudel")