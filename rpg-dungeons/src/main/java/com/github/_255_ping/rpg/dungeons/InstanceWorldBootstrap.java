package com.github._255_ping.rpg.dungeons;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;
import java.util.logging.Logger;

/** Bootstraps a void world used to house dungeon instances. */
public final class InstanceWorldBootstrap {

    public static World ensure(String name, Logger logger) {
        World existing = Bukkit.getWorld(name);
        if (existing != null) return existing;
        try {
            WorldCreator c = new WorldCreator(name);
            c.type(WorldType.FLAT);
            c.generator(new VoidGenerator());
            c.generateStructures(false);
            World w = c.createWorld();
            if (w != null) {
                w.setSpawnFlags(false, false);
                w.setKeepSpawnInMemory(false);
                w.setAutoSave(false);
                w.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
                w.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                w.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                w.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
            }
            return w;
        } catch (Exception ex) {
            logger.warning("Failed to create dungeon instance world '" + name + "': " + ex.getMessage());
            return null;
        }
    }

    private static final class VoidGenerator extends ChunkGenerator {
        @Override
        public boolean shouldGenerateNoise() { return false; }
        @Override
        public boolean shouldGenerateSurface() { return false; }
        @Override
        public boolean shouldGenerateCaves() { return false; }
        @Override
        public boolean shouldGenerateDecorations() { return false; }
        @Override
        public boolean shouldGenerateMobs() { return false; }
        @Override
        public boolean shouldGenerateStructures() { return false; }
    }
}
