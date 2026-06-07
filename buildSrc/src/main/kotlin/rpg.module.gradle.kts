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
val pluginName = project.name
tasks.withType<ProcessResources>().configureEach {
    inputs.property("pluginVersion", pluginVersion)
    inputs.property("pluginName", pluginName)
    // plugin.yml uses ${version}; config.yml uses ${pluginName} and ${version}.
    // Both share the same expand map — extra keys are ignored by files that don't reference them.
    filesMatching(listOf("plugin.yml", "config.yml")) {
        expand(mapOf("version" to pluginVersion, "pluginName" to pluginName))
    }
}

dependencies {
    "compileOnly"("io.papermc.paper:paper-api:${project.property("paperApiVersion")}")
    // Tests need paper-api on the runtime classpath because many classes touch Adventure types
    // (Component, etc.) at static-init time. testImplementation puts it on compile + runtime.
    "testImplementation"("io.papermc.paper:paper-api:${project.property("paperApiVersion")}")
    "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.3")
    "testImplementation"("org.mockito:mockito-core:5.14.2")
    "testImplementation"("org.mockito:mockito-junit-jupiter:5.14.2")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher:1.11.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
