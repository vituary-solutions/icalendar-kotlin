import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
