package com.github._255_ping.rpg.core.abilities;

import com.github._255_ping.rpg.api.abilities.AbilityDsl;
import com.github._255_ping.rpg.api.abilities.AbilityInvocation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Scans {@code plugins/rpg-core/abilities/} for *.yml files. Each top-level key is a
 * custom ability id with an {@code AbilitySequence} list and optional fields:
 * <ul>
 *   <li>{@code Cooldown} — hard-floor ticks (items can't reduce below this)</li>
 *   <li>{@code Name} — display name shown in item lore</li>
 *   <li>{@code Description} — list of lore lines shown below the ability name</li>
 * </ul>
 * Each ability is registered as a {@link CompositeAbilityEffect} on the ability registry,
 * callable from item / mob {@code Abilities} lists by its id.
 */
public final class AbilityLoader {

    private final File folder;
    private final CoreAbilityRegistry registry;
    private final Logger logger;
    private final List<String> registered = new ArrayList<>();

    public AbilityLoader(File folder, CoreAbilityRegistry registry, Logger logger) {
        this.folder = folder;
        this.registry = registry;
        this.logger = logger;
    }

    public void loadAll() {
        // Unregister previously-loaded composites so reload doesn't leave stale ones.
        for (String id : registered) registry.unregister(id);
        registered.clear();

        if (!folder.isDirectory()) return;
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                loadFile(f);
            } catch (Exception ex) {
                logger.warning("Failed to parse abilities file " + f.getName() + ": " + ex.getMessage());
            }
        }
    }

    private void loadFile(File file) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                logger.warning("ability '" + id + "' in " + file.getName() + " is not a section, skipping");
                continue;
            }
            try {
                CompositeAbilityEffect composite = parse(id, section);
                registry.register(id, params -> composite);
                registry.registerMeta(id, composite.displayName(), composite.description());
                registered.add(id);
            } catch (Exception ex) {
                logger.warning("Skipping ability '" + id + "' in " + file.getName() + ": " + ex.getMessage());
            }
        }
    }

    /** Fields the loader actually reads. Anything else is silently ignored — warn admins. */
    private static final java.util.Set<String> KNOWN_KEYS = java.util.Set.of(
            "AbilitySequence", "Cooldown", "Name", "Description"
    );

    private CompositeAbilityEffect parse(String id, ConfigurationSection s) {
        // Warn on unrecognized top-level fields so admins catch docs-vs-loader drift early.
        // Common false positives: "ManaCost" (use mana_cost{} in AbilitySequence instead),
        // "CombatXpMultiplier" (not yet implemented).
        for (String key : s.getKeys(false)) {
            if (!KNOWN_KEYS.contains(key)) {
                logger.warning("ability '" + id + "' has unrecognized field '" + key
                        + "' — it will be ignored. Known fields: " + KNOWN_KEYS
                        + ". For mana cost use '- mana_cost{amount=N}' in AbilitySequence.");
            }
        }

        List<String> seq = s.getStringList("AbilitySequence");
        long cooldown = s.getLong("Cooldown", 0);
        String displayName = s.getString("Name");
        List<String> description = s.getStringList("Description");

        List<AbilityInvocation> invocations = new ArrayList<>();
        for (String line : seq) {
            invocations.addAll(AbilityDsl.parse(line));
        }
        return new CompositeAbilityEffect(id, invocations, cooldown, displayName, description);
    }
}
