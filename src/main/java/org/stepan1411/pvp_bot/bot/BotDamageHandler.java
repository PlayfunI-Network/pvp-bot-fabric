package org.stepan1411.pvp_bot.bot;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class BotDamageHandler {
    
    public static void register() {
        // Register damage handler via Fabric API
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // Check if this is a ServerPlayerEntity
            if (entity instanceof ServerPlayerEntity player) {
                String playerName = player.getName().getString();
                
                // Check if this player is our bot
                if (BotManager.getAllBots().contains(playerName)) {
                    // Fire damage event - allow addons to cancel damage
                    try {
                        Entity attacker = source.getAttacker();
                        boolean cancelled = org.stepan1411.pvp_bot.api.BotAPIIntegration.fireDamageEvent(player, attacker, amount);
                        if (cancelled) {
                            return false; // Cancel damage
                        }
                    } catch (Exception e) {
                        System.err.println("[PVP_BOT_API] Error firing damage event: " + e.getMessage());
                    }
                    
                    // Call combat handler
                    BotCombat.onBotDamaged(player, source);
                }
            }
            
            // Return true to allow damage
            return true;
        });
    }
}
