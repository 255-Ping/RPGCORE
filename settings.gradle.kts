plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "rpg-plugins"

include(":rpg-api")
include(":rpg-core")
include(":rpg-mining")
include(":rpg-quests")
include(":rpg-combat")
include(":rpg-economy")
include(":rpg-hud")
