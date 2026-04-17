package org.stepan1411.pvp_bot.api;

import org.stepan1411.pvp_bot.api.event.BotEventManager;


public class ExampleEventHandlers {
    
    
    public static void register() {
        BotEventManager eventManager = PvpBotAPI.getEventManager();
        

        eventManager.registerSpawnHandler(bot -> {
            System.out.println("[PVP_BOT_API] Example: Bot " + bot.getName().getString() + " spawned!");
            bot.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[API] Welcome! API is working correctly."));
        });
        

        eventManager.registerDeathHandler(bot -> {
            System.out.println("[PVP_BOT_API] Example: Bot " + bot.getName().getString() + " died!");
        });
        

        eventManager.registerAttackHandler((bot, target) -> {

            if (target.getType().toString().contains("villager")) {
                bot.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[API] Cannot attack villagers!"));
                return true;
            }
            

            System.out.println("[PVP_BOT_API] Example: " + bot.getName().getString() + 
                             " attacking " + target.getName().getString());
            return false;
        });
        

        eventManager.registerDamageHandler((bot, attacker, damage) -> {

            if (attacker == null && damage > 5.0f) {
                bot.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e[API] Fall damage reduced!"));


                System.out.println("[PVP_BOT_API] Example: Reduced fall damage for " + 
                                 bot.getName().getString() + " from " + damage + " to " + (damage * 0.5f));
            }
            
            return false;
        });
        

        eventManager.registerTickHandler(bot -> {

            if (bot.age % 20 == 0) {

                if (bot.getHealth() < bot.getMaxHealth() && bot.age % 100 == 0) {
                    float newHealth = Math.min(bot.getMaxHealth(), bot.getHealth() + 0.5f);
                    bot.setHealth(newHealth);
                    
                    if (bot.age % 200 == 0) {
                        System.out.println("[PVP_BOT_API] Example: Slow healing for " + 
                                         bot.getName().getString() + " (Health: " + newHealth + ")");
                    }
                }
            }
        });
        
        System.out.println("[PVP_BOT_API] Example event handlers registered successfully!");
    }
}
