plugins {
    id("java")
    application
}

group = "com.neverless"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

application {
    mainClass.set("com.neverless.Main")
}

dependencies {
    implementation("org.rapidoid:rapidoid-quick:5.5.5")
    implementation("org.agrona:agrona:1.21.2")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:3.7.7")
    testImplementation("org.mockito:mockito-junit-jupiter:3.7.7")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

tasks.test {
    useJUnitPlatform()
}