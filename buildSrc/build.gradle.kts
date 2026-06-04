plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Declaring paperweight here loads it in a single classloader shared by all subprojects.
    // Without this, two subprojects applying the same plugin version get separate classloaders
    // and Gradle throws a UserdevSetup classcast error.
    implementation("io.papermc.paperweight:paperweight-userdev:2.0.0-beta.21")
}
