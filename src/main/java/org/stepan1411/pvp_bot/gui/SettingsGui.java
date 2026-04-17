package org.stepan1411.pvp_bot.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.stepan1411.pvp_bot.bot.BotSettings;

public class SettingsGui {
    public static void open(Player player) {
        BotSettings s = BotSettings.get();
        player.sendMessage(Component.text("=== PVP Bot Settings ==="));
        player.sendMessage(Component.text("Auto Armor: " + s.isAutoEquipArmor()));
        player.sendMessage(Component.text("Auto Weapon: " + s.isAutoEquipWeapon()));
        player.sendMessage(Component.text("Combat: " + s.isCombatEnabled()));
        player.sendMessage(Component.text("Use /pvpbot settings <key> <value> to change"));
    }
}
