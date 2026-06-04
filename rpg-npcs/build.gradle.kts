plugins {
    id("rpg.plugin-module")
    id("io.papermc.paperweight.userdev")
}

dependencies {
    paperweight.paperDevBundle(project.property("paperDevBundle").toString())
    "compileOnly"(project(":rpg-api"))
}
