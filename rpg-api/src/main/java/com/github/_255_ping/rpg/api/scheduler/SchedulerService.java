package com.github._255_ping.rpg.api.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface SchedulerService {
    void runGlobal(Plugin plugin, Runnable task);
    void runLater(Plugin plugin, Runnable task, long delayTicks);
    void runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks);
    void runAtEntity(Plugin plugin, Entity entity, Runnable task);
    void runAtLocation(Plugin plugin, Location loc, Runnable task);
    void runAsync(Plugin plugin, Runnable task);
    <T> CompletableFuture<T> supplyAsync(Plugin plugin, Supplier<T> supplier);
}
