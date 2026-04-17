package org.stepan1411.pvp_bot.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.stepan1411.pvp_bot.bot.BotFaction;
import org.stepan1411.pvp_bot.bot.BotKits;
import org.stepan1411.pvp_bot.bot.BotManager;

public class BotMenuGui {
    public static void openMainMenu(Player player) {
        player.sendMessage(Component.text("=== PVP Bot Menu ==="));
        player.sendMessage(Component.text("Bots: " + BotManager.getBotCount()));
        player.sendMessage(Component.text("Factions: " + BotFaction.getAllFactions().size()));
        player.sendMessage(Component.text("Kits: " + BotKits.getKitNames().size()));
        player.sendMessage(Component.text("Use /pvpbot <command> to manage bots"));
    }
}
