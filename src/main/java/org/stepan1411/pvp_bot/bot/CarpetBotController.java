package org.stepan1411.pvp_bot.bot;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class CarpetBotController {

    public static boolean isCarpetAvailable() {
        return false;
    }

    public static ServerPlayer spawnBot(String name, MinecraftServer server, ServerLevel level,
                                        double x, double y, double z, float yaw, float pitch, String gamemode) {
        return PaperBotSpawner.spawnBot(name, server, level, x, y, z, yaw, pitch, gamemode);
    }

    public static void removeBot(ServerPlayer player) {
        PaperBotSpawner.removeBot(player);
    }

    public static void killBot(ServerPlayer player) {
        PaperBotSpawner.removeBot(player);
    }
}
