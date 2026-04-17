package org.stepan1411.pvp_bot.api.event;

import net.minecraft.server.level.ServerPlayer;


@FunctionalInterface
public interface BotDeathHandler {
    
    void onBotDeath(ServerPlayer bot);
}
