package org.stepan1411.pvp_bot.api.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.stepan1411.pvp_bot.bot.BotSettings;


public class ExampleStrategy implements CombatStrategy {
    
    @Override
    public String getName() {
        return "ExampleLowHealthBoost";
    }
    
    @Override
    public int getPriority() {
        return 150;
    }
    
    @Override
    public String getDescription() {
        return "Applies strength boost when bot health is low";
    }
    
    @Override
    public boolean canUse(ServerPlayer bot, Entity target, BotSettings settings) {
        float healthPercent = bot.getHealth() / bot.getMaxHealth();
        if (bot.hasEffect(net.minecraft.world.effect.MobEffects.STRENGTH)) {
            return false;
        }
        return healthPercent < 0.5f && bot.distanceTo(target) < 8.0;
    }
    
    @Override
    public boolean execute(ServerPlayer bot, Entity target, BotSettings settings, MinecraftServer server) {
        try {
            bot.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.STRENGTH, 
                200,
                1
            ));
            bot.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[Strategy] Low health boost activated!"));
            System.out.println("[PVP_BOT_API] ExampleStrategy executed for " + bot.getName().getString());
            return true;
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error in ExampleStrategy: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public int getCooldown() {
        return 300;
    }
}
