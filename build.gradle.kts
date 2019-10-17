group = "com.vituary"
version = "0.1.0"

plugins {
    kotlin("jvm") version "1.3.50"
    maven
}


repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
