package org.stepan1411.pvp_bot.api.event;

import net.minecraft.server.level.ServerPlayer;


@FunctionalInterface
public interface BotSpawnHandler {
    
    void onBotSpawn(ServerPlayer bot);
}
