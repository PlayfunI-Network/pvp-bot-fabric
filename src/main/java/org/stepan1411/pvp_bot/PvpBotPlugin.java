package org.stepan1411.pvp_bot;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stepan1411.pvp_bot.bot.BotDamageHandler;
import org.stepan1411.pvp_bot.bot.BotKits;
import org.stepan1411.pvp_bot.bot.BotManager;
import org.stepan1411.pvp_bot.bot.BotPath;
import org.stepan1411.pvp_bot.bot.BotTicker;
import org.stepan1411.pvp_bot.command.BotCommand;
import org.stepan1411.pvp_bot.config.WorldConfigHelper;
import org.stepan1411.pvp_bot.stats.StatsReporter;

public class PvpBotPlugin extends JavaPlugin {

    public static final String MOD_ID = "pvp_bot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static PvpBotPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        LOGGER.info("PVP Bot plugin enabled!");

        try {
            MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
            nmsServer.getPlayerList().setMaxPlayers(99999);
        } catch (Exception e) {
            LOGGER.warn("Could not set max players: " + e.getMessage());
        }

        try {
            org.stepan1411.pvp_bot.api.BotAPIIntegration.initialize();
            LOGGER.info("PVP Bot API version: " + org.stepan1411.pvp_bot.api.PvpBotAPI.getApiVersion());
            org.stepan1411.pvp_bot.api.combat.CombatStrategyRegistry.getInstance()
                .register(new org.stepan1411.pvp_bot.api.combat.ExampleStrategy());
            org.stepan1411.pvp_bot.api.ExampleEventHandlers.register();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize PVP Bot API: " + e.getMessage());
        }

        LifecycleEventManager<Plugin> lifecycle = this.getLifecycleManager();
        lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            BotCommand.register(commands);
        });

        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        WorldConfigHelper.init(nmsServer);
        WorldConfigHelper.setOnWorldChangeCallback(() -> {
            BotManager.switchWorld(nmsServer);
            BotPath.init();
        });

        BotManager.init(nmsServer);
        BotKits.init(nmsServer);
        BotPath.init();
        StatsReporter.start(nmsServer);

        BotTicker.register(this);
        BotDamageHandler.register(this);
    }

    @Override
    public void onDisable() {
        StatsReporter.stop();
        MinecraftServer nmsServer = ((CraftServer) Bukkit.getServer()).getServer();
        BotManager.reset(nmsServer);
        try {
            org.stepan1411.pvp_bot.api.BotAPIIntegration.cleanup();
        } catch (Exception e) {
            LOGGER.error("Error during API cleanup: " + e.getMessage());
        }
    }

    public static PvpBotPlugin getInstance() {
        return instance;
    }
}
