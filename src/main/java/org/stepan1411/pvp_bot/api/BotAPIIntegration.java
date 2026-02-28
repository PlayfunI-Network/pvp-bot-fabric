package org.stepan1411.pvp_bot.api;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Helper class for integrating API events with the mod's internal systems.
 * This class should be called from BotManager, BotCombat, etc.
 */
public class BotAPIIntegration {
    
    /**
     * Fire spawn event when a bot spawns
     * Call this from BotManager.spawnBot() after adding bot to list
     */
    public static void fireSpawnEvent(ServerPlayerEntity bot) {
        try {
            PvpBotAPI.getEventManager().fireSpawnEvent(bot);
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing spawn event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fire death event when a bot dies
     * Call this from BotManager.cleanupDeadBots() before removing bot from list
     */
    public static void fireDeathEvent(ServerPlayerEntity bot) {
        try {
            PvpBotAPI.getEventManager().fireDeathEvent(bot);
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing death event: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Fire attack event when a bot attacks
     * Call this from BotCombat before executing attack
     * @return true if attack should be cancelled
     */
    public static boolean fireAttackEvent(ServerPlayerEntity bot, Entity target) {
        try {
            return PvpBotAPI.getEventManager().fireAttackEvent(bot, target);
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing attack event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Fire damage event when a bot takes damage
     * Call this from damage handler/mixin
     * @return true if damage should be cancelled
     */
    public static boolean fireDamageEvent(ServerPlayerEntity bot, Entity attacker, float damage) {
        try {
            return PvpBotAPI.getEventManager().fireDamageEvent(bot, attacker, damage);
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing damage event: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Fire tick event for a bot
     * Call this from BotTicker every tick
     */
    public static void fireTickEvent(ServerPlayerEntity bot) {
        try {
            PvpBotAPI.getEventManager().fireTickEvent(bot);
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing tick event: " + e.getMessage());
            // Don't print stack trace for tick events to avoid spam
        }
    }
}
