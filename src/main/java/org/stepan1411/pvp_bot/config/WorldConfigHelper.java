package org.stepan1411.pvp_bot.config;

import net.minecraft.server.MinecraftServer;
import org.stepan1411.pvp_bot.PvpBotPlugin;

import java.nio.file.Files;
import java.nio.file.Path;

public class WorldConfigHelper {

    private static MinecraftServer server;
    private static String currentWorldName = null;
    private static Runnable onWorldChangeCallback = null;

    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        currentWorldName = getWorldName();
        System.out.println("[PVP_BOT] WorldConfigHelper initialized with world: " + currentWorldName);
    }

    public static void setOnWorldChangeCallback(Runnable callback) {
        onWorldChangeCallback = callback;
    }

    public static Path getWorldConfigDir() {
        checkWorldChange();
        String worldName = getWorldName();
        return getWorldConfigDir(worldName);
    }

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

    public static Path getWorldConfigDir(String worldName) {
        Path dir = getDataPath()
            .resolve("worlds")
            .resolve(worldName);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Failed to create world config directory: " + e.getMessage());
        }
        return dir;
    }

    public static Path getGlobalConfigDir() {
        Path dir = getDataPath();
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Failed to create global config directory: " + e.getMessage());
        }
        return dir;
    }

    private static Path getDataPath() {
        return PvpBotPlugin.getInstance().getDataFolder().toPath();
    }

    private static String getWorldName() {
        if (server != null) {
            String name = server.getWorldData().getLevelName();
            System.out.println("[PVP_BOT] Getting world name: " + name);
            return name;
        }
        System.out.println("[PVP_BOT] Server is null, using default world name");
        return "world";
    }
}
