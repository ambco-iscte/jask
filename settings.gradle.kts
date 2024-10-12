plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "pesca"

include(":pesca", ":strudel")
project(":strudel").projectDir = file("../strudel")