plugins {
    id("rpg.plugin-module")
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    "implementation"(project(":rpg-api"))
}

tasks {
    jar {
        enabled = false
    }
    shadowJar {
        archiveClassifier.set("")
    }
    assemble {
        dependsOn(shadowJar)
    }
    runServer {
        minecraftVersion(project.property("minecraftVersion") as String)
        jvmArgs("-Xms2G", "-Xmx2G")
    }
}
