package org.stepan1411.pvp_bot.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;


@FunctionalInterface
public interface BotDamageHandler {
    
    boolean onBotDamage(ServerPlayer bot, Entity attacker, float damage);
}
