package org.stepan1411.pvp_bot.stats;

import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import org.stepan1411.pvp_bot.bot.BotManager;
import org.stepan1411.pvp_bot.bot.BotSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * РћС‚РїСЂР°РІР»СЏРµС‚ Р°РЅРѕРЅРёРјРЅСѓСЋ СЃС‚Р°С‚РёСЃС‚РёРєСѓ РЅР° СЃРµСЂРІРµСЂ
 * https://stepan1411.github.io/pvpbot-stats/
 */
public class StatsReporter {
    
    private static final String STATS_ENDPOINT = "https://stepan1411.pythonanywhere.com/api/stats";
    private static ScheduledExecutorService scheduler = null;
    private static String serverId = null;
    private static boolean enabled = true;
    private static net.minecraft.server.MinecraftServer currentServer = null;
    
    /**
     * Р—Р°РїСѓСЃРєР°РµС‚ РїРµСЂРёРѕРґРёС‡РµСЃРєСѓСЋ РѕС‚РїСЂР°РІРєСѓ СЃС‚Р°С‚РёСЃС‚РёРєРё
     */
    public static void start(net.minecraft.server.MinecraftServer server) {
        currentServer = server;
        
        // РћСЃС‚Р°РЅР°РІР»РёРІР°РµРј РїСЂРµРґС‹РґСѓС‰РёР№ scheduler РµСЃР»Рё РѕРЅ Р±С‹Р»
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        
        // РџСЂРѕРІРµСЂСЏРµРј РЅР°СЃС‚СЂРѕР№РєРё
        BotSettings settings = BotSettings.get();
        if (!settings.isSendStats()) {
            System.out.println("[PVP_BOT] Statistics reporting disabled in settings");
            enabled = false;
            return;
        }
        
        // Р—Р°РіСЂСѓР¶Р°РµРј РёР»Рё СЃРѕР·РґР°С‘Рј ID СЃРµСЂРІРµСЂР°
        serverId = loadOrCreateServerId();
        
        // РЎРѕР·РґР°С‘Рј РЅРѕРІС‹Р№ scheduler
        scheduler = Executors.newScheduledThreadPool(1);
        
        // РћС‚РїСЂР°РІР»СЏРµРј СЃС‚Р°С‚РёСЃС‚РёРєСѓ СЃСЂР°Р·Сѓ РїСЂРё СЃС‚Р°СЂС‚Рµ
        sendStats();
        
        // РћС‚РїСЂР°РІР»СЏРµРј РєР°Р¶РґС‹Рµ 5 СЃРµРєСѓРЅРґ (С‡С‚РѕР±С‹ Р±СЌРєРµРЅРґ Р·РЅР°Р» С‡С‚Рѕ СЃРµСЂРІРµСЂ РѕРЅР»Р°Р№РЅ)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendStats();
            } catch (Exception e) {
                // РўРёС…Рѕ РёРіРЅРѕСЂРёСЂСѓРµРј РѕС€РёР±РєРё С‡С‚РѕР±С‹ РЅРµ СЃРїР°РјРёС‚СЊ Р»РѕРіРё
            }
        }, 5, 5, TimeUnit.SECONDS);
        
        System.out.println("[PVP_BOT] Statistics reporter started (Server ID: " + serverId.substring(0, 8) + "...)");
    }
    
    /**
     * РћСЃС‚Р°РЅР°РІР»РёРІР°РµС‚ РѕС‚РїСЂР°РІРєСѓ СЃС‚Р°С‚РёСЃС‚РёРєРё
     */
    public static void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // Р–РґС‘Рј Р·Р°РІРµСЂС€РµРЅРёСЏ Р·Р°РґР°С‡ РјР°РєСЃРёРјСѓРј 2 СЃРµРєСѓРЅРґС‹
                if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
        // РћС‚РїСЂР°РІР»СЏРµРј С„РёРЅР°Р»СЊРЅСѓСЋ СЃС‚Р°С‚РёСЃС‚РёРєСѓ СЃ bots_count = 0
        if (enabled) {
            sendStats();
        }
    }
    
    /**
     * РћС‚РїСЂР°РІР»СЏРµС‚ СЃС‚Р°С‚РёСЃС‚РёРєСѓ РЅР° СЃРµСЂРІРµСЂ
     */
    public static void sendStats() {
        if (!enabled || serverId == null) {
            return;
        }
        
        try {
            JsonObject stats = new JsonObject();
            stats.addProperty("server_id", serverId);
            stats.addProperty("bots_count", BotManager.getAllBots().size());
            stats.addProperty("bots_spawned_total", BotManager.getBotsSpawnedTotal());
            stats.addProperty("bots_killed_total", BotManager.getBotsKilledTotal());
            stats.addProperty("mod_version", getModVersion());
            stats.addProperty("minecraft_version", "1.21.11");
            stats.addProperty("timestamp", System.currentTimeMillis());
            if (currentServer != null) {
                var playerManager = getPlayerManager();
                if (playerManager != null) {
                    int totalPlayers = playerManager.getPlayerList().size();
                    int realPlayers = totalPlayers - BotManager.getAllBots().size();
                    stats.addProperty("real_players_count", Math.max(0, realPlayers));
                    stats.addProperty("total_players_count", totalPlayers);
                    com.google.gson.JsonArray botsArray = new com.google.gson.JsonArray();
                    for (String botName : BotManager.getAllBots()) {
                        botsArray.add(botName);
                    }
                    stats.add("bots_list", botsArray);
                    
                    com.google.gson.JsonArray playersArray = new com.google.gson.JsonArray();
                    for (var player : playerManager.getPlayerList()) {
                        if (!BotManager.getAllBots().contains(player.getName().getString())) {
                            com.google.gson.JsonObject playerObj = new com.google.gson.JsonObject();
                            playerObj.addProperty("name", player.getName().getString());
                            // TODO: Р”РѕР±Р°РІРёС‚СЊ РїСЂРѕРІРµСЂРєСѓ OP СЃС‚Р°С‚СѓСЃР°
                            playerObj.addProperty("is_op", false);
                            playersArray.add(playerObj);
                        }
                    }
                    stats.add("players_list", playersArray);
                    stats.addProperty("server_core", getServerCore());
                }
            }
            
            // РћС‚РїСЂР°РІР»СЏРµРј POST Р·Р°РїСЂРѕСЃ
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STATS_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PVPBOT-Stats/1.0")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(stats.toString()))
                    .build();
            
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 200 || response.statusCode() == 201) {
                            // РЈСЃРїРµС€РЅРѕ РѕС‚РїСЂР°РІР»РµРЅРѕ
                        }
                    })
                    .exceptionally(e -> {
                        // РўРёС…Рѕ РёРіРЅРѕСЂРёСЂСѓРµРј РѕС€РёР±РєРё
                        return null;
                    });
            
        } catch (Exception e) {
            // РўРёС…Рѕ РёРіРЅРѕСЂРёСЂСѓРµРј РѕС€РёР±РєРё
        }
    }
    
    /**
     * РџРѕР»СѓС‡Р°РµС‚ PlayerManager РёР· СЃРµСЂРІРµСЂР°
     */
    private static net.minecraft.server.PlayerManager getPlayerManager() {
        try {
            if (currentServer != null) {
                return currentServer.getPlayerManager();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Р—Р°РіСЂСѓР¶Р°РµС‚ РёР»Рё СЃРѕР·РґР°С‘С‚ СѓРЅРёРєР°Р»СЊРЅС‹Р№ ID СЃРµСЂРІРµСЂР°
     */
    private static String loadOrCreateServerId() {
        try {
            Path configDir = org.stepan1411.pvp_bot.config.WorldConfigHelper.getGlobalConfigDir();
            Path serverIdFile = configDir.resolve("server_id.txt");
            
            if (Files.exists(serverIdFile)) {
                return Files.readString(serverIdFile).trim();
            } else {
                String newId = UUID.randomUUID().toString();
                Files.createDirectories(configDir);
                Files.writeString(serverIdFile, newId);
                return newId;
            }
        } catch (IOException e) {
            // Р•СЃР»Рё РЅРµ РјРѕР¶РµРј СЃРѕС…СЂР°РЅРёС‚СЊ - РіРµРЅРµСЂРёСЂСѓРµРј РІСЂРµРјРµРЅРЅС‹Р№ ID
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * РџРѕР»СѓС‡Р°РµС‚ РІРµСЂСЃРёСЋ РјРѕРґР°
     */
    private static String getModVersion() {
        return FabricLoader.getInstance()
                .getModContainer("pvp_bot")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
    
    /**
     * РћРїСЂРµРґРµР»СЏРµС‚ СЏРґСЂРѕ СЃРµСЂРІРµСЂР°
     */
    private static String getServerCore() {
        try {
            // РџСЂРѕРІРµСЂСЏРµРј РЅР°Р»РёС‡РёРµ РєР»Р°СЃСЃРѕРІ СЂР°Р·РЅС‹С… СЏРґРµСЂ
            if (classExists("org.spongepowered.api.Sponge")) {
                return "Sponge";
            } else if (classExists("org.bukkit.Bukkit")) {
                return "Bukkit/Spigot/Paper";
            } else if (classExists("net.minecraftforge.common.MinecraftForge")) {
                return "Forge";
            } else if (classExists("net.fabricmc.loader.api.FabricLoader")) {
                return "Fabric";
            }
            return "Vanilla";
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * РџСЂРѕРІРµСЂСЏРµС‚ СЃСѓС‰РµСЃС‚РІРѕРІР°РЅРёРµ РєР»Р°СЃСЃР°
     */
    private static boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
