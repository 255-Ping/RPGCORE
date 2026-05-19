import java.nio.file.Files
import java.nio.file.LinkOption

plugins {
    id("rpg.module")
}

val testServerPluginsDir = file(project.property("testServerPluginsDir") as String)
val moduleName = name
val libsDir = layout.buildDirectory.dir("libs")

val syncTestServerSymlinks = tasks.register("syncTestServerSymlinks") {
    notCompatibleWithConfigurationCache("Manipulates symlinks in TestServer/plugins at execution time")
    doLast {
        val pv = project.version.toString()
        val newJarName = "$moduleName-$pv.jar"
        val newSourcesName = "$moduleName-$pv-sources.jar"
        val libsFile = libsDir.get().asFile
        val newJar = libsFile.resolve(newJarName)

        libsFile.listFiles { f ->
            f.name.startsWith("$moduleName-")
                    && f.name.endsWith(".jar")
                    && f.name != newJarName
                    && f.name != newSourcesName
        }?.forEach {
            println("Deleting stale jar: ${it.name}")
            it.delete()
        }

        if (testServerPluginsDir.isDirectory) {
            testServerPluginsDir.listFiles { f ->
                f.name.startsWith("$moduleName-")
                        && f.name.endsWith(".jar")
                        && f.name != newJarName
            }?.forEach {
                println("Deleting stale plugin: ${it.name}")
                it.delete()
            }

            if (newJar.exists()) {
                val linkPath = testServerPluginsDir.toPath().resolve(newJarName)
                if (!Files.exists(linkPath, LinkOption.NOFOLLOW_LINKS)) {
                    Files.createSymbolicLink(linkPath, newJar.toPath())
                    println("Symlinked: $newJarName -> ${newJar.absolutePath}")
                }
            }
        }
    }
}

tasks.named("assemble") {
    finalizedBy(syncTestServerSymlinks)
}
