plugins {
    id("rpg.plugin-module")
}

repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    "compileOnly"(project(":rpg-api"))
    "compileOnly"("me.clip:placeholderapi:2.11.6")
}
