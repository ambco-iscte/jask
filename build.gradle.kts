plugins {
    kotlin("jvm") version "1.8.20"
}

group = "pt.iscte"
version = "0.4.15"

repositories {
    mavenCentral()
}

dependencies {
    testApi("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testApi("org.junit.platform:junit-platform-suite:1.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    api(project(":strudel"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}