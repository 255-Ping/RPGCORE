pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "rpg-plugins"

include(":rpg-api")
include(":rpg-core")
include(":rpg-mining")
include(":rpg-quests")
include(":rpg-combat")
include(":rpg-economy")
include(":rpg-hud")
include(":rpg-chat")
include(":rpg-accessories")
include(":rpg-holograms")
include(":rpg-parties")
include(":rpg-foraging")
include(":rpg-fishing")
include(":rpg-regions")
include(":rpg-farming")
include(":rpg-guilds")
include(":rpg-enchanting")
include(":rpg-alchemy")
include(":rpg-npcs")
include(":rpg-dungeons")
include(":rpg-cooking")
include(":rpg-admin")
