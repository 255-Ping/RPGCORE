plugins {
    id("rpg.plugin-module")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

dependencies {
    paperweight.paperDevBundle(project.property("paperDevBundle").toString())
    "compileOnly"(project(":rpg-api"))
}
