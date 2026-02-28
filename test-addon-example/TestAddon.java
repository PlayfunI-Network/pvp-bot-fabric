package com.example.testaddon;

import net.fabricmc.api.ModInitializer;
import org.stepan1411.pvp_bot.api.PvpBotAPI;
import org.stepan1411.pvp_bot.api.event.BotEventManager;

/**
 * Simple test addon to verify PVP Bot API is working
 * 
 * To use:
 * 1. Create a new Fabric mod project
 * 2. Add PVP Bot as dependency in build.gradle
 * 3. Copy this file to your mod
 * 4. Build and test
 */
public class TestAddon implements ModInitializer {
    
    @Override
    public void onInitialize() {
        System.out.println("═══════════════════════════════════════");
        System.out.println("  PVP Bot API Test Addon Loaded!");
        System.out.println("  API Version: " + PvpBotAPI.getApiVersion());
        System.out.println("═══════════════════════════════════════");
        
        BotEventManager events = PvpBotAPI.getEventManager();
        
        // Test spawn event
        events.registerSpawnHandler(bot -> {
            System.out.println("✅ [TEST] Spawn event works!");
            System.out.println("   Bot: " + bot.getName().getString());
            System.out.println("   Position: " + bot.getPos());
            System.out.println("   Health: " + bot.getHealth());
        });
        
        // Test death event
        events.registerDeathHandler(bot -> {
            System.out.println("✅ [TEST] Death event works!");
            System.out.println("   Bot died: " + bot.getName().getString());
        });
        
        // Test attack event
        events.registerAttackHandler((bot, target) -> {
            System.out.println("✅ [TEST] Attack event works!");
            System.out.println("   " + bot.getName().getString() + " attacks " + target.getName().getString());
            
            // Example: Cancel attacks on villagers
            if (target.getType().toString().contains("villager")) {
                System.out.println("   ⚠️ Attack cancelled - protecting villager!");
                return true; // Cancel attack
            }
            
            return false; // Allow attack
        });
        
        // Test damage event
        events.registerDamageHandler((bot, attacker, damage) -> {
            System.out.println("✅ [TEST] Damage event works!");
            System.out.println("   " + bot.getName().getString() + " took " + damage + " damage");
            if (attacker != null) {
                System.out.println("   From: " + attacker.getName().getString());
            }
            
            // Example: Make bots immune to fall damage
            if (attacker == null) {
                System.out.println("   ⚠️ Damage cancelled - no attacker (fall damage?)");
                return true; // Cancel damage
            }
            
            return false; // Allow damage
        });
        
        // Test tick event (only log every 5 seconds to avoid spam)
        events.registerTickHandler(bot -> {
            if (bot.age % 100 == 0) { // Every 5 seconds
                System.out.println("✅ [TEST] Tick event works!");
                System.out.println("   Bot: " + bot.getName().getString());
                System.out.println("   Age: " + bot.age + " ticks");
                System.out.println("   Health: " + bot.getHealth() + "/" + bot.getMaxHealth());
            }
        });
        
        System.out.println("═══════════════════════════════════════");
        System.out.println("  All event handlers registered!");
        System.out.println("  Spawn a bot to test: /pvpbot spawn TestBot");
        System.out.println("═══════════════════════════════════════");
    }
}
