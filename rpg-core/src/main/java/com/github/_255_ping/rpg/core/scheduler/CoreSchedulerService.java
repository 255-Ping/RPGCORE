package com.github._255_ping.rpg.core.scheduler;

import com.github._255_ping.rpg.api.scheduler.SchedulerService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Bukkit-backed SchedulerService. Folia-readiness: the at-entity and at-location methods
 * exist so addons code against them today; on plain Paper they route to the main thread.
 * A Folia variant can be substituted later without touching downstream code.
 */
public final class CoreSchedulerService implements SchedulerService {

    @Override
    public void runGlobal(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runLater(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @Override
    public void runTimer(Plugin plugin, Runnable task, long delayTicks, long periodTicks) {
        Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks);
    }

    @Override
    public void runAtEntity(Plugin plugin, Entity entity, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAtLocation(Plugin plugin, Location loc, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Plugin plugin, Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
