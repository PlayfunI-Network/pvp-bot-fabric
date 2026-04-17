package org.stepan1411.pvp_bot.bot;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.stepan1411.pvp_bot.api.BotAPIIntegration;

public class BotDamageHandler implements Listener {

    public static void register(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new BotDamageHandler(), plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof CraftPlayer craftPlayer)) return;
        ServerPlayer player = craftPlayer.getHandle();
        String playerName = player.getName().getString();

        if (!BotManager.getAllBots().contains(playerName)) return;

        try {
            net.minecraft.world.entity.Entity attacker = null;
            if (event instanceof org.bukkit.event.entity.EntityDamageByEntityEvent byEntityEvent) {
                if (byEntityEvent.getDamager() instanceof CraftLivingEntity craftAttacker) {
                    attacker = craftAttacker.getHandle();
                }
            }
            boolean cancelled = BotAPIIntegration.fireDamageEvent(player, attacker, (float) event.getDamage());
            if (cancelled) {
                event.setCancelled(true);
                return;
            }
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing damage event: " + e.getMessage());
        }

        BotCombat.onBotDamaged(player, null);
    }
}
