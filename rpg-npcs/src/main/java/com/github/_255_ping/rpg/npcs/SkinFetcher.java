package com.github._255_ping.rpg.npcs;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;

/** Async Mojang API skin lookup with in-memory cache per session. */
public final class SkinFetcher {

    private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";

    private final JavaPlugin plugin;
    private final HttpClient http;
    private final Map<String, NpcDef.SkinDef> cache = new ConcurrentHashMap<>();

    public SkinFetcher(JavaPlugin plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newHttpClient();
    }

    /**
     * Fetch skin by player name, call callback on the main thread.
     * Calls back with null if fetch fails.
     */
    public void fetchByName(String playerName, Consumer<NpcDef.SkinDef> callback) {
        if (cache.containsKey(playerName.toLowerCase())) {
            callback.accept(cache.get(playerName.toLowerCase()));
            return;
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            NpcDef.SkinDef skin = null;
            try {
                // Step 1: resolve name → UUID
                HttpResponse<String> uuidResp = http.send(
                    HttpRequest.newBuilder(URI.create(UUID_URL + playerName)).build(),
                    HttpResponse.BodyHandlers.ofString());
                if (uuidResp.statusCode() != 200) throw new RuntimeException("UUID lookup failed: " + uuidResp.statusCode());
                String uuidJson = uuidResp.body();
                String rawUuid = extractJsonString(uuidJson, "id");
                if (rawUuid == null) throw new RuntimeException("No id in response");

                // Step 2: UUID → profile with textures
                String formattedUuid = rawUuid.replaceAll(
                    "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                HttpResponse<String> profileResp = http.send(
                    HttpRequest.newBuilder(URI.create(PROFILE_URL + formattedUuid + "?unsigned=false")).build(),
                    HttpResponse.BodyHandlers.ofString());
                if (profileResp.statusCode() != 200) throw new RuntimeException("Profile lookup failed");
                String profileJson = profileResp.body();
                String value = extractJsonString(profileJson, "value");
                String signature = extractJsonString(profileJson, "signature");
                if (value == null) throw new RuntimeException("No texture value");
                skin = new NpcDef.SkinDef(playerName, value, signature);
                cache.put(playerName.toLowerCase(), skin);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to fetch skin for '" + playerName + "': " + ex.getMessage());
            }
            final NpcDef.SkinDef result = skin;
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(result));
        });
    }

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        if (end < 0) return null;
        return json.substring(start, end);
    }
}
