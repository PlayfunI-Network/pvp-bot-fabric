package org.stepan1411.pvp_bot.config;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper class for managing per-world configuration paths
 */
public class WorldConfigHelper {
    
    private static MinecraftServer server;
    private static String currentWorldName = null;
    private static Runnable onWorldChangeCallback = null;
    
    /**
     * Initialize with server instance
     */
    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        currentWorldName = getWorldName();
        System.out.println("[PVP_BOT] WorldConfigHelper initialized with world: " + currentWorldName);
    }
    
    /**
     * Set callback to be called when world changes
     */
    public static void setOnWorldChangeCallback(Runnable callback) {
        onWorldChangeCallback = callback;
    }
    
    /**
     * Get the config directory for the current world
     * Structure: config/pvpbot/worlds/{worldName}/
     */
    public static Path getWorldConfigDir() {
        checkWorldChange();
        String worldName = getWorldName();
        return getWorldConfigDir(worldName);
    }
    
    /**
     * Check if world has changed and trigger callback
     */
    private static void checkWorldChange() {
        String newWorldName = getWorldName();
        if (currentWorldName != null && !currentWorldName.equals(newWorldName)) {
            System.out.println("[PVP_BOT] World changed from '" + currentWorldName + "' to '" + newWorldName + "'");
            currentWorldName = newWorldName;
            if (onWorldChangeCallback != null) {
                onWorldChangeCallback.run();
            }
        } else if (currentWorldName == null) {
            currentWorldName = newWorldName;
        }
    }
    
    /**
     * Get the config directory for a specific world
     */
    public static Path getWorldConfigDir(String worldName) {
        Path dir = FabricLoader.getInstance().getConfigDir()
            .resolve("pvpbot")
            .resolve("worlds")
            .resolve(worldName);
        
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Failed to create world config directory: " + e.getMessage());
        }
        
        return dir;
    }
    
    /**
     * Get the global config directory (for stats, kits, server_id)
     * Structure: config/pvpbot/
     */
    public static Path getGlobalConfigDir() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("pvpbot");
        
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Failed to create global config directory: " + e.getMessage());
        }
        
        return dir;
    }
    
    /**
     * Get the current world name from server
     */
    private static String getWorldName() {
        if (server != null) {
            String name = server.getSaveProperties().getLevelName();
            System.out.println("[PVP_BOT] Getting world name: " + name);
            return name;
        }
        System.out.println("[PVP_BOT] Server is null, using default world name");
        return "world";
    }
}
