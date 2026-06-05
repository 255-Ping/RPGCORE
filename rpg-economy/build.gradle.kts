plugins {
    id("rpg.plugin-module")
}

dependencies {
    "compileOnly"(project(":rpg-api"))
    // Vault soft-dependency: economy bridge for third-party plugins.
    // JitPack repo is already declared in buildSrc/rpg.module.gradle.kts.
    // Exclude VaultAPI's bundled bukkit:1.13 to avoid a dependency conflict with Paper API.
    "compileOnly"("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
}
