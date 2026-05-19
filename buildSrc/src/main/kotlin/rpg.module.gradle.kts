plugins {
    `java-library`
}

// Per-plugin version is stored as "<short-name>Version" in gradle.properties,
// where short-name is the module name with the "rpg-" prefix stripped.
// e.g., rpg-core reads coreVersion. Falls back to 0.0.0 if the property is absent.
val moduleShortName = project.name.removePrefix("rpg-")
val moduleVersionKey = "${moduleShortName}Version"
val moduleVersion = if (project.hasProperty(moduleVersionKey)) {
    project.property(moduleVersionKey).toString()
} else {
    "0.0.0"
}
version = "$moduleVersion-${project.property("suiteVersion")}"

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
