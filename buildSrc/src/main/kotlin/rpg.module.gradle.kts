plugins {
    `java-library`
}

version = "0.0.0-${project.property("suiteVersion")}"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-parameters"))
}

val pluginVersion = version.toString()
tasks.withType<ProcessResources>().configureEach {
    inputs.property("pluginVersion", pluginVersion)
    filesMatching("plugin.yml") {
        expand(mapOf("version" to pluginVersion))
    }
}

dependencies {
    "compileOnly"("io.papermc.paper:paper-api:${project.property("paperApiVersion")}")
}
