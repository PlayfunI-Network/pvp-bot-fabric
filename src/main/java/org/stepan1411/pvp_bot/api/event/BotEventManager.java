package org.stepan1411.pvp_bot.api.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;


public class BotEventManager {
    
    private static final BotEventManager INSTANCE = new BotEventManager();
    
    private final List<BotSpawnHandler> spawnHandlers = new ArrayList<>();
    private final List<BotDeathHandler> deathHandlers = new ArrayList<>();
    private final List<BotAttackHandler> attackHandlers = new ArrayList<>();
    private final List<BotDamageHandler> damageHandlers = new ArrayList<>();
    private final List<BotTickHandler> tickHandlers = new ArrayList<>();
    
    private BotEventManager() {}
    
    
    public static BotEventManager getInstance() {
        return INSTANCE;
    }
    
    
    public void registerSpawnHandler(BotSpawnHandler handler) {
        if (handler != null) {
            spawnHandlers.add(handler);
        }
    }
    
    
    public void registerDeathHandler(BotDeathHandler handler) {
        if (handler != null) {
            deathHandlers.add(handler);
        }
    }
    
    
    public void registerAttackHandler(BotAttackHandler handler) {
        if (handler != null) {
            attackHandlers.add(handler);
        }
    }
    
    
    public void registerDamageHandler(BotDamageHandler handler) {
        if (handler != null) {
            damageHandlers.add(handler);
        }
    }
    
    
    public void registerTickHandler(BotTickHandler handler) {
        if (handler != null) {
            tickHandlers.add(handler);
        }
    }
    
    
    public void fireSpawnEvent(ServerPlayer bot) {
        for (BotSpawnHandler handler : spawnHandlers) {
            try {
                handler.onBotSpawn(bot);
            } catch (Exception e) {
                System.err.println("[PVP_BOT_API] Error in spawn handler: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    
    public void fireDeathEvent(ServerPlayer bot) {
        for (BotDeathHandler handler : deathHandlers) {
            try {
                handler.onBotDeath(bot);
            } catch (Exception e) {
                System.err.println("[PVP_BOT_API] Error in death handler: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    
    public boolean fireAttackEvent(ServerPlayer bot, Entity target) {
        boolean cancelled = false;
        for (BotAttackHandler handler : attackHandlers) {
            try {
                if (handler.onBotAttack(bot, target)) {
                    cancelled = true;
                }
            } catch (Exception e) {
                System.err.println("[PVP_BOT_API] Error in attack handler: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return cancelled;
    }
    
    
    public boolean fireDamageEvent(ServerPlayer bot, Entity attacker, float damage) {
        boolean cancelled = false;
        for (BotDamageHandler handler : damageHandlers) {
            try {
                if (handler.onBotDamage(bot, attacker, damage)) {
                    cancelled = true;
                }
            } catch (Exception e) {
                System.err.println("[PVP_BOT_API] Error in damage handler: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return cancelled;
    }
    
    
    public void fireTickEvent(ServerPlayer bot) {
        for (BotTickHandler handler : tickHandlers) {
            try {
                handler.onBotTick(bot);
            } catch (Exception e) {
                System.err.println("[PVP_BOT_API] Error in tick handler: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    
    public int getSpawnHandlerCount() {
        return spawnHandlers.size();
    }
    
    
    public int getDeathHandlerCount() {
        return deathHandlers.size();
    }
    
    
    public int getAttackHandlerCount() {
        return attackHandlers.size();
    }
    
    
    public int getDamageHandlerCount() {
        return damageHandlers.size();
    }
    
    
    public int getTickHandlerCount() {
        return tickHandlers.size();
    }
    
    
    public void clearAllHandlers() {
        spawnHandlers.clear();
        deathHandlers.clear();
        attackHandlers.clear();
        damageHandlers.clear();
        tickHandlers.clear();
    }
}
