package org.stepan1411.pvp_bot.api.combat;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.stepan1411.pvp_bot.bot.BotSettings;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class CombatStrategyRegistry {
    
    private static final CombatStrategyRegistry INSTANCE = new CombatStrategyRegistry();
    private final List<CombatStrategy> strategies = new ArrayList<>();
    private final Map<String, Map<String, Long>> strategyCooldowns = new ConcurrentHashMap<>();
    
    private CombatStrategyRegistry() {}
    
    
    public static CombatStrategyRegistry getInstance() {
        return INSTANCE;
    }
    
    
    public synchronized void register(CombatStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy cannot be null");
        }
        

        for (CombatStrategy existing : strategies) {
            if (existing.getName().equals(strategy.getName())) {
                System.err.println("[PVP_BOT_API] Warning: Strategy with name '" + strategy.getName() + "' already exists. Replacing...");
                strategies.remove(existing);
                break;
            }
        }
        
        strategies.add(strategy);
        strategies.sort(Comparator.comparingInt(CombatStrategy::getPriority).reversed());
        System.out.println("[PVP_BOT_API] Registered combat strategy: " + strategy.getName() + " (priority: " + strategy.getPriority() + ")");
    }
    
    
    public synchronized boolean unregister(CombatStrategy strategy) {
        boolean removed = strategies.remove(strategy);
        if (removed) {
            System.out.println("[PVP_BOT_API] Unregistered combat strategy: " + strategy.getName());
        }
        return removed;
    }
    
    
    public synchronized boolean unregister(String strategyName) {
        for (Iterator<CombatStrategy> it = strategies.iterator(); it.hasNext(); ) {
            CombatStrategy strategy = it.next();
            if (strategy.getName().equals(strategyName)) {
                it.remove();
                System.out.println("[PVP_BOT_API] Unregistered combat strategy: " + strategyName);
                return true;
            }
        }
        return false;
    }
    
    
    public List<CombatStrategy> getStrategies() {
        return new ArrayList<>(strategies);
    }
    
    
    public CombatStrategy getStrategy(String name) {
        for (CombatStrategy strategy : strategies) {
            if (strategy.getName().equals(name)) {
                return strategy;
            }
        }
        return null;
    }
    
    
    public boolean executeStrategy(ServerPlayer bot, Entity target, BotSettings settings, MinecraftServer server) {
        String botName = bot.getName().getString();
        long currentTime = System.currentTimeMillis();
        
        for (CombatStrategy strategy : strategies) {
            try {

                if (isOnCooldown(botName, strategy.getName(), currentTime)) {
                    continue;
                }
                

                if (!strategy.canUse(bot, target, settings)) {
                    continue;
                }
                

                if (strategy.execute(bot, target, settings, server)) {

                    setCooldown(botName, strategy.getName(), currentTime);
                    return true;
                }
            } catch (Exception e) {
                System.err.println("[PVP_BOT_API] Error executing strategy '" + strategy.getName() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        return false;
    }
    
    
    public boolean isOnCooldown(String botName, String strategyName) {
        return isOnCooldown(botName, strategyName, System.currentTimeMillis());
    }
    
    private boolean isOnCooldown(String botName, String strategyName, long currentTime) {
        Map<String, Long> botCooldowns = strategyCooldowns.get(botName);
        if (botCooldowns == null) {
            return false;
        }
        
        Long lastUsed = botCooldowns.get(strategyName);
        if (lastUsed == null) {
            return false;
        }
        
        CombatStrategy strategy = getStrategy(strategyName);
        if (strategy == null) {
            return false;
        }
        
        long cooldownMs = strategy.getCooldown() * 50L;
        return (currentTime - lastUsed) < cooldownMs;
    }
    
    private void setCooldown(String botName, String strategyName, long currentTime) {
        strategyCooldowns.computeIfAbsent(botName, k -> new ConcurrentHashMap<>()).put(strategyName, currentTime);
    }
    
    
    public void clearCooldowns(String botName) {
        strategyCooldowns.remove(botName);
    }
    
    
    public void clearAllCooldowns() {
        strategyCooldowns.clear();
    }
    
    
    public int getStrategyCount() {
        return strategies.size();
    }
    
    
    public boolean isRegistered(String strategyName) {
        return getStrategy(strategyName) != null;
    }
    
    
    public List<CombatStrategy> getStrategiesByPriority(int minPriority, int maxPriority) {
        List<CombatStrategy> result = new ArrayList<>();
        for (CombatStrategy strategy : strategies) {
            int priority = strategy.getPriority();
            if (priority >= minPriority && priority <= maxPriority) {
                result.add(strategy);
            }
        }
        return result;
    }
    
    
    public synchronized void clear() {
        strategies.clear();
        strategyCooldowns.clear();
        System.out.println("[PVP_BOT_API] Cleared all combat strategies");
    }
    
    
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Combat Strategies (").append(strategies.size()).append(" registered):\n");
        for (int i = 0; i < strategies.size(); i++) {
            CombatStrategy strategy = strategies.get(i);
            sb.append("  ").append(i + 1).append(". ")
              .append(strategy.getName())
              .append(" (priority: ").append(strategy.getPriority())
              .append(", cooldown: ").append(strategy.getCooldown()).append(" ticks)\n");
        }
        return sb.toString();
    }
}
