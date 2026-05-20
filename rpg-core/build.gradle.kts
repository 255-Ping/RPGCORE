plugins {
    id("rpg.plugin-module")
    id("com.gradleup.shadow") version "8.3.5"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    "implementation"(project(":rpg-api"))
    "compileOnly"("net.luckperms:api:5.4")
    // Paper resolves these at runtime via plugin.yml `libraries`. compileOnly here for sources.
    "compileOnly"("com.mysql:mysql-connector-j:9.0.0")
    "compileOnly"("com.zaxxer:HikariCP:6.0.0")
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
