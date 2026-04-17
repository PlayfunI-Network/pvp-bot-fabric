package org.stepan1411.pvp_bot.command;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.stepan1411.pvp_bot.bot.BotCombat;
import org.stepan1411.pvp_bot.bot.BotDebug;
import org.stepan1411.pvp_bot.bot.BotFaction;
import org.stepan1411.pvp_bot.bot.BotKits;
import org.stepan1411.pvp_bot.bot.BotManager;
import org.stepan1411.pvp_bot.bot.BotMovement;
import org.stepan1411.pvp_bot.bot.BotNameGenerator;
import org.stepan1411.pvp_bot.bot.BotPath;
import org.stepan1411.pvp_bot.bot.BotSettings;
import org.stepan1411.pvp_bot.api.PvpBotAPI;
import org.stepan1411.pvp_bot.gui.SettingsGui;
import org.stepan1411.pvp_bot.stats.StatsReporter;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

public class BotCommand {
    

    private static final boolean HAS_INVVIEW = false;
    

    private static final SuggestionProvider<io.papermc.paper.command.brigadier.CommandSourceStack> BOT_SUGGESTIONS = (ctx, builder) -> {
        var server = toVanilla(ctx.getSource()).getServer();
        var aliveBots = BotManager.getAllBots().stream()
            .filter(name -> {
                var bot = server.getPlayerList().getPlayerByName(name);
                return bot != null && bot.isAlive();
            })
            .collect(Collectors.toList());
        return SharedSuggestionProvider.suggest(aliveBots, builder);
    };
    

    private static final SuggestionProvider<io.papermc.paper.command.brigadier.CommandSourceStack> TARGET_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(
            toVanilla(ctx.getSource()).getServer().getPlayerList().getPlayers().stream()
                .map(p -> p.getName().getString())
                .collect(Collectors.toList()), 
            builder);
    

    private static final SuggestionProvider<io.papermc.paper.command.brigadier.CommandSourceStack> PLAYER_SUGGESTIONS = TARGET_SUGGESTIONS;
    

    private static final SuggestionProvider<io.papermc.paper.command.brigadier.CommandSourceStack> FACTION_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(BotFaction.getAllFactions(), builder);
    

    private static final SuggestionProvider<io.papermc.paper.command.brigadier.CommandSourceStack> KIT_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(BotKits.getKitNames(), builder);
    

    private static final SuggestionProvider<io.papermc.paper.command.brigadier.CommandSourceStack> PATH_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(BotPath.getAllPaths().keySet(), builder);

    public static void register(io.papermc.paper.command.brigadier.Commands commands) {
        commands.register(
            Commands.literal("pvpbot")
                

                .then(Commands.literal("spawn")
                    .executes(ctx -> spawnBot(toVanilla(ctx.getSource()), BotNameGenerator.generateUniqueName()))
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> spawnBot(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("massspawn")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                        .executes(ctx -> massSpawnBots(toVanilla(ctx.getSource()), IntegerArgumentType.getInteger(ctx, "count")))
                    )
                )
                

                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> removeBot(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("removeall")
                    .executes(ctx -> removeAllBots(toVanilla(ctx.getSource())))
                )
                

                .then(Commands.literal("list")
                    .executes(ctx -> listBots(toVanilla(ctx.getSource())))
                )
                

                .then(Commands.literal("sync")
                    .executes(ctx -> syncBots(toVanilla(ctx.getSource())))
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(ctx -> syncBot(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("debug")
                    .then(Commands.argument("bot", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        

                        .then(Commands.literal("path")
                            .executes(ctx -> toggleDebugPath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugPath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("target")
                            .executes(ctx -> toggleDebugTarget(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugTarget(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("combat")
                            .executes(ctx -> toggleDebugCombat(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugCombat(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("navigation")
                            .executes(ctx -> toggleDebugNavigation(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugNavigation(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("all")
                            .executes(ctx -> toggleDebugAll(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugAll(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("status")
                            .executes(ctx -> showDebugStatus(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot")))
                        )
                    )
                    

                    .then(Commands.literal("api")
                        .executes(ctx -> showApiDebugInfo(toVanilla(ctx.getSource())))
                    )
                )
                

                .then(Commands.literal("menu")
                    .executes(ctx -> openTestMenu(toVanilla(ctx.getSource())))
                )
                

                .then(Commands.literal("settings")
                    .executes(ctx -> showSettings(toVanilla(ctx.getSource())))
                    

                    .then(Commands.literal("gui")
                        .executes(ctx -> openSettingsGui(toVanilla(ctx.getSource())))
                    )
                    

                    .then(Commands.literal("autoarmor")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("autoarmor: " + BotSettings.get().isAutoEquipArmor())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEquipArmor(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto equip armor: " + BotSettings.get().isAutoEquipArmor()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autoweapon")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("autoweapon: " + BotSettings.get().isAutoEquipWeapon())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEquipWeapon(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto equip weapon: " + BotSettings.get().isAutoEquipWeapon()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("droparmor")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("droparmor: " + BotSettings.get().isDropWorseArmor())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setDropWorseArmor(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Drop worse armor: " + BotSettings.get().isDropWorseArmor()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("dropweapon")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("dropweapon: " + BotSettings.get().isDropWorseWeapons())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setDropWorseWeapons(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Drop worse weapons: " + BotSettings.get().isDropWorseWeapons()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("dropdistance")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("dropdistance: " + BotSettings.get().getDropDistance())); return 1; })
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                            .executes(ctx -> {
                                BotSettings.get().setDropDistance(DoubleArgumentType.getDouble(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Drop distance: " + BotSettings.get().getDropDistance()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("interval")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("interval: " + BotSettings.get().getCheckInterval() + " ticks")); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> {
                                BotSettings.get().setCheckInterval(IntegerArgumentType.getInteger(ctx, "ticks"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Check interval: " + BotSettings.get().getCheckInterval() + " ticks"));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("minarmorlevel")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("minarmorlevel: " + BotSettings.get().getMinArmorLevel())); return 1; })
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMinArmorLevel(IntegerArgumentType.getInteger(ctx, "level"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Min armor level: " + BotSettings.get().getMinArmorLevel() + " (0=any, 20=leather+, 40=gold+, 50=chain+, 60=iron+, 80=diamond+, 100=netherite)"));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("combat")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("combat: " + BotSettings.get().isCombatEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setCombatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Combat enabled: " + BotSettings.get().isCombatEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("revenge")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("revenge: " + BotSettings.get().isRevengeEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRevengeEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Revenge mode: " + BotSettings.get().isRevengeEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("autotarget")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("autotarget: " + BotSettings.get().isAutoTargetEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoTargetEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto target: " + BotSettings.get().isAutoTargetEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("targetplayers")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("targetplayers: " + BotSettings.get().isTargetPlayers())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetPlayers(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Target players: " + BotSettings.get().isTargetPlayers()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("targetmobs")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("targetmobs: " + BotSettings.get().isTargetHostileMobs())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetHostileMobs(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Target hostile mobs: " + BotSettings.get().isTargetHostileMobs()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("targetbots")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("targetbots: " + BotSettings.get().isTargetOtherBots())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetOtherBots(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Target other bots: " + BotSettings.get().isTargetOtherBots()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("criticals")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("criticals: " + BotSettings.get().isCriticalsEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setCriticalsEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Criticals: " + BotSettings.get().isCriticalsEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("ranged")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("ranged: " + BotSettings.get().isRangedEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRangedEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Ranged weapons: " + BotSettings.get().isRangedEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("mace")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("mace: " + BotSettings.get().isMaceEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setMaceEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Mace combat: " + BotSettings.get().isMaceEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramace")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("elytramace: " + BotSettings.get().isElytraMaceEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("ElytraMace trick: " + BotSettings.get().isElytraMaceEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("specialnames")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("specialnames: " + BotSettings.get().isUseSpecialNames())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setUseSpecialNames(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Use special names: " + BotSettings.get().isUseSpecialNames()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramaceretries")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("elytramaceretries: " + BotSettings.get().getElytraMaceMaxRetries())); return 1; })
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceMaxRetries(IntegerArgumentType.getInteger(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("ElytraMace max retries: " + BotSettings.get().getElytraMaceMaxRetries()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramacealtitude")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("elytramacealtitude: " + BotSettings.get().getElytraMaceMinAltitude())); return 1; })
                        .then(Commands.argument("value", IntegerArgumentType.integer(5, 50))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceMinAltitude(IntegerArgumentType.getInteger(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("ElytraMace min altitude: " + BotSettings.get().getElytraMaceMinAltitude()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramacedistance")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("elytramacedistance: " + BotSettings.get().getElytraMaceAttackDistance())); return 1; })
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(3.0, 15.0))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceAttackDistance(DoubleArgumentType.getDouble(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("ElytraMace attack distance: " + BotSettings.get().getElytraMaceAttackDistance()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramacefireworks")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("elytramacefireworks: " + BotSettings.get().getElytraMaceFireworkCount())); return 1; })
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceFireworkCount(IntegerArgumentType.getInteger(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("ElytraMace firework count: " + BotSettings.get().getElytraMaceFireworkCount()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("gotousebaritone")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("gotousebaritone: " + BotSettings.get().isGotoUseBaritone())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setGotoUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Goto use Baritone: " + BotSettings.get().isGotoUseBaritone()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("escortusebaritone")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("escortusebaritone: " + BotSettings.get().isEscortUseBaritone())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setEscortUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Escort use Baritone: " + BotSettings.get().isEscortUseBaritone()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("followusebaritone")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("followusebaritone: " + BotSettings.get().isFollowUseBaritone())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setFollowUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Follow use Baritone: " + BotSettings.get().isFollowUseBaritone()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("shieldmace")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("shieldmace: " + BotSettings.get().isShieldMace())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setShieldMace(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Shield mace defense: " + BotSettings.get().isShieldMace()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("prefershieldmace")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("prefershieldmace: " + BotSettings.get().isPreferShieldMace())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setPreferShieldMace(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Prefer shield over totem for mace: " + BotSettings.get().isPreferShieldMace()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("shieldmainhand")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("shieldmainhand: " + BotSettings.get().isShieldMainHand())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setShieldMainHand(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Shield in main hand when having totem: " + BotSettings.get().isShieldMainHand()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("attackcooldown")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("attackcooldown: " + BotSettings.get().getAttackCooldown() + " ticks")); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 40))
                            .executes(ctx -> {
                                BotSettings.get().setAttackCooldown(IntegerArgumentType.getInteger(ctx, "ticks"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Attack cooldown: " + BotSettings.get().getAttackCooldown() + " ticks"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("meleerange")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("meleerange: " + BotSettings.get().getMeleeRange())); return 1; })
                        .then(Commands.argument("range", DoubleArgumentType.doubleArg(2.0, 6.0))
                            .executes(ctx -> {
                                BotSettings.get().setMeleeRange(DoubleArgumentType.getDouble(ctx, "range"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Melee range: " + BotSettings.get().getMeleeRange()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("movespeed")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("movespeed: " + BotSettings.get().getMoveSpeed())); return 1; })
                        .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.1, 2.0))
                            .executes(ctx -> {
                                BotSettings.get().setMoveSpeed(DoubleArgumentType.getDouble(ctx, "speed"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Move speed: " + BotSettings.get().getMoveSpeed()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("bhop")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("bhop: " + BotSettings.get().isBhopEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setBhopEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Bhop enabled: " + BotSettings.get().isBhopEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("bhopcooldown")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("bhopcooldown: " + BotSettings.get().getBhopCooldown() + " ticks")); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(5, 30))
                            .executes(ctx -> {
                                BotSettings.get().setBhopCooldown(IntegerArgumentType.getInteger(ctx, "ticks"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Bhop cooldown: " + BotSettings.get().getBhopCooldown() + " ticks"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("jumpboost")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("jumpboost: " + BotSettings.get().getJumpBoost())); return 1; })
                        .then(Commands.argument("boost", DoubleArgumentType.doubleArg(0.0, 0.5))
                            .executes(ctx -> {
                                BotSettings.get().setJumpBoost(DoubleArgumentType.getDouble(ctx, "boost"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Jump boost: " + BotSettings.get().getJumpBoost()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("idle")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("idle: " + BotSettings.get().isIdleWanderEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setIdleWanderEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Idle wander: " + BotSettings.get().isIdleWanderEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("usebaritone")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("usebaritone: " + BotSettings.get().isUseBaritone())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Use Baritone: " + BotSettings.get().isUseBaritone()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("idleradius")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("idleradius: " + BotSettings.get().getIdleWanderRadius())); return 1; })
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(3.0, 50.0))
                            .executes(ctx -> {
                                BotSettings.get().setIdleWanderRadius(DoubleArgumentType.getDouble(ctx, "radius"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Idle wander radius: " + BotSettings.get().getIdleWanderRadius()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("friendlyfire")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("friendlyfire: " + BotSettings.get().isFriendlyFireEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setFriendlyFireEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Friendly fire: " + BotSettings.get().isFriendlyFireEnabled()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("misschance")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("misschance: " + BotSettings.get().getMissChance() + "%")); return 1; })
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMissChance(IntegerArgumentType.getInteger(ctx, "percent"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Miss chance: " + BotSettings.get().getMissChance() + "%"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("mistakechance")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("mistakechance: " + BotSettings.get().getMistakeChance() + "%")); return 1; })
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMistakeChance(IntegerArgumentType.getInteger(ctx, "percent"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Mistake chance: " + BotSettings.get().getMistakeChance() + "%"));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("reactiondelay")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("reactiondelay: " + BotSettings.get().getReactionDelay() + " ticks")); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 20))
                            .executes(ctx -> {
                                BotSettings.get().setReactionDelay(IntegerArgumentType.getInteger(ctx, "ticks"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Reaction delay: " + BotSettings.get().getReactionDelay() + " ticks"));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("prefersword")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("prefersword: " + BotSettings.get().isPreferSword())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setPreferSword(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Prefer sword: " + BotSettings.get().isPreferSword()));
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("shieldbreak")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("shieldbreak: " + BotSettings.get().isShieldBreakEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setShieldBreakEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Shield break: " + BotSettings.get().isShieldBreakEnabled()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autoshield")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("autoshield: " + BotSettings.get().isAutoShieldEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoShieldEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto shield: " + BotSettings.get().isAutoShieldEnabled()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autopotion")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("autopotion: " + BotSettings.get().isAutoPotionEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoPotionEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto potion: " + BotSettings.get().isAutoPotionEnabled()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autototem")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("autototem: " + BotSettings.get().isAutoTotemEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoTotemEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto totem: " + BotSettings.get().isAutoTotemEnabled()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("totempriority")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("totempriority: " + BotSettings.get().isTotemPriority())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTotemPriority(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Totem priority (don't replace with shield): " + BotSettings.get().isTotemPriority()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("retreat")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("retreat: " + BotSettings.get().isRetreatEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRetreatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Retreat enabled: " + BotSettings.get().isRetreatEnabled()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autoeat")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("autoeat: " + BotSettings.get().isAutoEatEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto eat enabled: " + BotSettings.get().isAutoEatEnabled()));
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("automend")
                        .executes(ctx -> { sendSuccess(toVanilla(ctx.getSource()), Component.literal("automend: " + BotSettings.get().isAutoMendEnabled())); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoMendEnabled(BoolArgumentType.getBool(ctx, "value"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("Auto mend enabled: " + BotSettings.get().isAutoMendEnabled()));
                                return 1;
                            })
                        )
                    )
                )
                

                .then(Commands.literal("attack")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(TARGET_SUGGESTIONS)
                            .executes(ctx -> setAttackTarget(toVanilla(ctx.getSource()), 
                                StringArgumentType.getString(ctx, "botname"),
                                StringArgumentType.getString(ctx, "target")))
                        )
                    )
                )
                

                .then(Commands.literal("stop")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> stopAttack(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("stopmovement")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> stopBotMovement(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("follow")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(TARGET_SUGGESTIONS)
                            .executes(ctx -> setBotFollow(toVanilla(ctx.getSource()), 
                                StringArgumentType.getString(ctx, "botname"),
                                StringArgumentType.getString(ctx, "target"), false))
                        )
                    )
                )
                

                .then(Commands.literal("escort")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(TARGET_SUGGESTIONS)
                            .executes(ctx -> setBotFollow(toVanilla(ctx.getSource()), 
                                StringArgumentType.getString(ctx, "botname"),
                                StringArgumentType.getString(ctx, "target"), true))
                        )
                    )
                )
                

                .then(Commands.literal("goto")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                    .executes(ctx -> setBotGoto(toVanilla(ctx.getSource()), 
                                        StringArgumentType.getString(ctx, "botname"),
                                        DoubleArgumentType.getDouble(ctx, "x"),
                                        DoubleArgumentType.getDouble(ctx, "y"),
                                        DoubleArgumentType.getDouble(ctx, "z")))
                                )
                            )
                        )
                    )
                )
                

                .then(Commands.literal("target")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> showTarget(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("inventory")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> showInventory(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("faction")

                    .then(Commands.literal("list")
                        .executes(ctx -> listFactions(toVanilla(ctx.getSource())))
                    )

                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> createFaction(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                        )
                    )

                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> deleteFaction(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                        )
                    )

                    .then(Commands.literal("add")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> addToFaction(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "player")))
                            )
                        )
                    )

                    .then(Commands.literal("remove")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> removeFromFaction(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "player")))
                            )
                        )
                    )

                    .then(Commands.literal("hostile")
                        .then(Commands.argument("faction1", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("faction2", StringArgumentType.word())
                                .suggests(FACTION_SUGGESTIONS)
                                .executes(ctx -> setHostile(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction1"),
                                    StringArgumentType.getString(ctx, "faction2"),
                                    true))
                                .then(Commands.argument("hostile", BoolArgumentType.bool())
                                    .executes(ctx -> setHostile(toVanilla(ctx.getSource()), 
                                        StringArgumentType.getString(ctx, "faction1"),
                                        StringArgumentType.getString(ctx, "faction2"),
                                        BoolArgumentType.getBool(ctx, "hostile")))
                                )
                            )
                        )
                    )

                    .then(Commands.literal("info")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> factionInfo(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "faction")))
                        )
                    )

                    .then(Commands.literal("addnear")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0, 10000.0))
                                .executes(ctx -> addNearbyBotsToFaction(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    DoubleArgumentType.getDouble(ctx, "radius")))
                            )
                        )
                    )

                    .then(Commands.literal("addall")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> addAllBotsToFaction(toVanilla(ctx.getSource()), 
                                StringArgumentType.getString(ctx, "faction")))
                        )
                    )

                    .then(Commands.literal("give")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("item", StringArgumentType.greedyString())
                                .executes(ctx -> giveFactionItem(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "item")))
                            )
                        )
                    )

                    .then(Commands.literal("attack")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> factionAttack(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "target")))
                            )
                        )
                    )

                    .then(Commands.literal("startpath")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("path", StringArgumentType.word())
                                .suggests(PATH_SUGGESTIONS)
                                .executes(ctx -> factionStartPath(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )

                    .then(Commands.literal("stoppath")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> factionStopPath(toVanilla(ctx.getSource()), 
                                StringArgumentType.getString(ctx, "faction")))
                        )
                    )
                    
                    .then(Commands.literal("follow")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> factionFollow(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "target"), false))
                            )
                        )
                    )
                    
                    .then(Commands.literal("escort")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> factionFollow(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "target"), true))
                            )
                        )
                    )
                    
                    .then(Commands.literal("goto")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                    .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(ctx -> factionGoto(toVanilla(ctx.getSource()), 
                                            StringArgumentType.getString(ctx, "faction"),
                                            DoubleArgumentType.getDouble(ctx, "x"),
                                            DoubleArgumentType.getDouble(ctx, "y"),
                                            DoubleArgumentType.getDouble(ctx, "z")))
                                    )
                                )
                            )
                        )
                    )
                )
                

                .then(Commands.literal("settings")
                    .then(Commands.literal("viewdistance")
                        .executes(ctx -> { 
                            sendSuccess(toVanilla(ctx.getSource()), Component.literal("viewdistance: " + BotSettings.get().getMaxTargetDistance())); 
                            return 1; 
                        })
                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(5.0, 128.0))
                            .executes(ctx -> {
                                BotSettings.get().setMaxTargetDistance(DoubleArgumentType.getDouble(ctx, "distance"));
                                sendSuccess(toVanilla(ctx.getSource()), Component.literal("View distance: " + BotSettings.get().getMaxTargetDistance()));
                                return 1;
                            })
                        )
                    )
                )
                


                .then(Commands.literal("createkit")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> createKit(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("deletekit")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(KIT_SUGGESTIONS)
                        .executes(ctx -> deleteKit(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("kits")
                    .executes(ctx -> listKits(toVanilla(ctx.getSource())))
                )
                

                .then(Commands.literal("givekit")
                    .then(Commands.argument("playername", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .then(Commands.argument("kitname", StringArgumentType.word())
                            .suggests(KIT_SUGGESTIONS)
                            .executes(ctx -> giveKitToPlayer(toVanilla(ctx.getSource()), 
                                StringArgumentType.getString(ctx, "playername"),
                                StringArgumentType.getString(ctx, "kitname")))
                        )
                    )
                )
                

                .then(Commands.literal("faction")
                    .then(Commands.literal("givekit")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("kitname", StringArgumentType.word())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(ctx -> giveKitToFaction(toVanilla(ctx.getSource()), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "kitname")))
                            )
                        )
                    )
                )
                

                

                .then(Commands.literal("path")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> createPath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> deletePath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("addpoint")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> addPathPoint(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("removepoint")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> removeLastPathPoint(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                            .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> removePathPoint(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "index")))
                            )
                        )
                    )
                    .then(Commands.literal("clear")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> clearPath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("loop")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPathLoop(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "value")))
                            )
                        )
                    )
                    .then(Commands.literal("attack")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPathAttack(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "value")))
                            )
                        )
                    )
                    .then(Commands.literal("start")
                        .then(Commands.argument("bot", StringArgumentType.word())
                            .suggests(BOT_SUGGESTIONS)
                            .then(Commands.argument("path", StringArgumentType.word())
                                .suggests(PATH_SUGGESTIONS)
                                .executes(ctx -> startPathFollowing(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot"), StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )
                    .then(Commands.literal("stop")
                        .then(Commands.argument("bot", StringArgumentType.word())
                            .suggests(BOT_SUGGESTIONS)
                            .executes(ctx -> stopPathFollowing(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "bot")))
                        )
                    )
                    .then(Commands.literal("list")
                        .executes(ctx -> listPaths(toVanilla(ctx.getSource())))
                    )
                    .then(Commands.literal("show")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("visible", BoolArgumentType.bool())
                                .executes(ctx -> showPath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "visible")))
                            )
                        )
                    )
                    .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> pathInfo(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "name")))
                        )
                    )

                    .then(Commands.literal("distribute")
                        .then(Commands.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> distributeBotsOnPath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "path")))
                        )
                    )

                    .then(Commands.literal("startnear")
                        .then(Commands.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> startPathNear(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "path"), DoubleArgumentType.getDouble(ctx, "radius")))
                            )
                        )
                    )

                    .then(Commands.literal("stopall")
                        .then(Commands.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> stopAllOnPath(toVanilla(ctx.getSource()), StringArgumentType.getString(ctx, "path")))
                        )
                    )
                )
                .then(Commands.literal("updatestats")
                    .executes(ctx -> updateStats(toVanilla(ctx.getSource())))
                )
        );
    }

    private static CommandSourceStack toVanilla(io.papermc.paper.command.brigadier.CommandSourceStack source) {
        if (source instanceof CommandSourceStack vanillaSource) {
            return vanillaSource;
        }

        try {
            Method getHandleMethod = source.getClass().getMethod("getHandle");
            Object handle = getHandleMethod.invoke(source);
            if (handle instanceof CommandSourceStack vanillaSource) {
                return vanillaSource;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        throw new IllegalStateException("Unsupported command source stack implementation: " + source.getClass().getName());
    }

    private static void sendSuccess(CommandSourceStack source, Component message) {
        try {
            Method sendSuccessMethod = source.getClass().getMethod("sendSuccess", java.util.function.Supplier.class, boolean.class);
            sendSuccessMethod.invoke(source, (java.util.function.Supplier<?>) () -> message, false);
            return;
        } catch (ReflectiveOperationException ignored) {
        }

        var entity = source.getEntity();
        if (entity != null) {
            entity.sendSystemMessage(message);
        } else {
            source.sendError(message);
        }
    }
    
    private static int setAttackTarget(CommandSourceStack source, String botName, String targetName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        BotCombat.setTarget(botName, targetName);
        sendSuccess(source, Component.literal("Bot '" + botName + "' now attacking '" + targetName + "'"));
        return 1;
    }
    
    private static int stopAttack(CommandSourceStack source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        BotCombat.clearTarget(botName);
        sendSuccess(source, Component.literal("Bot '" + botName + "' stopped attacking"));
        return 1;
    }
    
    private static int showTarget(CommandSourceStack source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        var target = BotCombat.getTarget(botName);
        if (target != null) {
            sendSuccess(source, Component.literal("Bot '" + botName + "' target: " + target.getName().getString()));
        } else {
            sendSuccess(source, Component.literal("Bot '" + botName + "' has no target"));
        }
        return 1;
    }


    private static int spawnBot(CommandSourceStack source, String name) {

        var server = source.getServer();
        var existingPlayer = server.getPlayerList().getPlayerByName(name);
        if (existingPlayer != null && !BotManager.getAllBots().contains(name)) {
            source.sendError(Component.literal("Cannot create bot '" + name + "': a real player with this name is online!"));
            return 0;
        }
        
        if (BotManager.spawnBot(server, name, source)) {
            sendSuccess(source, Component.literal("PvP Bot '" + name + "' spawned!"));
            return 1;
        } else {
            source.sendError(Component.literal("Failed to spawn bot '" + name + "' (bot already exists or name is taken)"));
            return 0;
        }
    }
    
    private static int massSpawnBots(CommandSourceStack source, int count) {
        var server = source.getServer();
        final int[] spawned = {0};
        final int[] current = {0};
        
        sendSuccess(source, Component.literal("Spawning " + count + " bots "));
        

        scheduleSpawn(server, source, count, spawned, current);
        
        return 1;
    }
    
    private static void scheduleSpawn(net.minecraft.server.MinecraftServer server, CommandSourceStack source, int total, int[] spawned, int[] current) {
        if (current[0] >= total) {
            sendSuccess(source, Component.literal("Finished! Spawned " + spawned[0] + " bots."));
            return;
        }
        

        String name = BotNameGenerator.generateUniqueName();
        if (BotManager.spawnBot(server, name, source)) {
            spawned[0]++;
        }
        current[0]++;
        

        server.execute(() -> {

            final int[] delay = {0};
            server.execute(new Runnable() {
                @Override
                public void run() {
                    delay[0]++;
                    if (delay[0] < 5) {
                        server.execute(this);
                    } else {
                        scheduleSpawn(server, source, total, spawned, current);
                    }
                }
            });
        });
    }

    private static int removeBot(CommandSourceStack source, String name) {
        if (BotManager.removeBot(source.getServer(), name, source)) {
            sendSuccess(source, Component.literal("Bot '" + name + "' removed!"));
            return 1;
        } else {
            source.sendError(Component.literal("Bot '" + name + "' not found!"));
            return 0;
        }
    }

    private static int removeAllBots(CommandSourceStack source) {
        int count = BotManager.getBotCount();
        BotManager.removeAllBots(source.getServer(), source);
        sendSuccess(source, Component.literal("Removed " + count + " bots"));
        return count;
    }

    private static int listBots(CommandSourceStack source) {
        var bots = BotManager.getAllBots();
        
        if (bots.isEmpty()) {
            sendSuccess(source, Component.literal("No active PvP bots"));
        } else {
            sendSuccess(source, Component.literal("Active PvP bots (" + bots.size() + "):"));
            for (String botName : bots) {
                sendSuccess(source, Component.literal(" - " + botName));
            }
        }
        return bots.size();
    }
    
    private static int syncBots(CommandSourceStack source) {
        var server = source.getServer();
        int beforeCount = BotManager.getAllBots().size();
        

        sendSuccess(source, Component.literal("=== Players on server ==="));
        for (var player : server.getPlayerList().getPlayers()) {
            String name = player.getName().getString();
            String className = player.getClass().getName();
            boolean inList = BotManager.getAllBots().contains(name);
            sendSuccess(source, Component.literal(" - " + name + " [" + className + "] " + (inList ? "(in list)" : "(NOT in list)")));
        }
        

        BotManager.syncBots(server);
        
        int afterCount = BotManager.getAllBots().size();
        int added = afterCount - beforeCount;
        
        sendSuccess(source, Component.literal("Synced! Added " + added + " bots. Total: " + afterCount));
        return added;
    }
    
    private static int syncBot(CommandSourceStack source, String name) {
        var server = source.getServer();
        

        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player == null) {
            sendSuccess(source, Component.literal("Player " + name + " not found on server!"));
            return 0;
        }
        

        String className = player.getClass().getName();
        boolean inList = BotManager.getAllBots().contains(name);
        sendSuccess(source, Component.literal("Player: " + name));
        sendSuccess(source, Component.literal("Class: " + className));
        sendSuccess(source, Component.literal("In bot list: " + inList));
        

        boolean added = BotManager.syncBot(server, name);
        
        if (added) {
            sendSuccess(source, Component.literal("Successfully added " + name + " to bot list!"));
            return 1;
        } else if (inList) {
            sendSuccess(source, Component.literal(name + " is already in bot list!"));
            return 0;
        } else {
            sendSuccess(source, Component.literal(name + " is not a fake player (HeroBot bot)!"));
            return 0;
        }
    }

    private static int openSettingsGui(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendError(Component.literal("This command must be run by a player!"));
                return 0;
            }
            
            SettingsGui.open(player.getBukkitEntity());
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("Failed to open settings GUI: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int showSettings(CommandSourceStack source) {
        BotSettings s = BotSettings.get();
        sendSuccess(source, Component.literal("=== Equipment Settings ==="));
        sendSuccess(source, Component.literal("autoarmor: " + s.isAutoEquipArmor()));
        sendSuccess(source, Component.literal("autoweapon: " + s.isAutoEquipWeapon()));
        sendSuccess(source, Component.literal("droparmor: " + s.isDropWorseArmor()));
        sendSuccess(source, Component.literal("dropweapon: " + s.isDropWorseWeapons()));
        sendSuccess(source, Component.literal("dropdistance: " + s.getDropDistance()));
        sendSuccess(source, Component.literal("interval: " + s.getCheckInterval() + " ticks"));
        sendSuccess(source, Component.literal("minarmorlevel: " + s.getMinArmorLevel()));
        
        sendSuccess(source, Component.literal("=== Combat Settings ==="));
        sendSuccess(source, Component.literal("combat: " + s.isCombatEnabled()));
        sendSuccess(source, Component.literal("revenge: " + s.isRevengeEnabled()));
        sendSuccess(source, Component.literal("autotarget: " + s.isAutoTargetEnabled()));
        sendSuccess(source, Component.literal("targetplayers: " + s.isTargetPlayers()));
        sendSuccess(source, Component.literal("targetmobs: " + s.isTargetHostileMobs()));
        sendSuccess(source, Component.literal("targetbots: " + s.isTargetOtherBots()));
        sendSuccess(source, Component.literal("criticals: " + s.isCriticalsEnabled()));
        sendSuccess(source, Component.literal("ranged: " + s.isRangedEnabled()));
        sendSuccess(source, Component.literal("mace: " + s.isMaceEnabled()));
        sendSuccess(source, Component.literal("elytramace: " + s.isElytraMaceEnabled()));
        sendSuccess(source, Component.literal("specialnames: " + s.isUseSpecialNames()));
        sendSuccess(source, Component.literal("elytramaceretries: " + s.getElytraMaceMaxRetries()));
        sendSuccess(source, Component.literal("elytramacealtitude: " + s.getElytraMaceMinAltitude()));
        sendSuccess(source, Component.literal("elytramacedistance: " + s.getElytraMaceAttackDistance()));
        sendSuccess(source, Component.literal("elytramacefireworks: " + s.getElytraMaceFireworkCount()));
        sendSuccess(source, Component.literal("gotousebaritone: " + s.isGotoUseBaritone()));
        sendSuccess(source, Component.literal("escortusebaritone: " + s.isEscortUseBaritone()));
        sendSuccess(source, Component.literal("followusebaritone: " + s.isFollowUseBaritone()));
        sendSuccess(source, Component.literal("shieldmace: " + s.isShieldMace()));
        sendSuccess(source, Component.literal("prefershieldmace: " + s.isPreferShieldMace()));
        sendSuccess(source, Component.literal("shieldmainhand: " + s.isShieldMainHand()));
        sendSuccess(source, Component.literal("attackcooldown: " + s.getAttackCooldown() + " ticks"));
        sendSuccess(source, Component.literal("meleerange: " + s.getMeleeRange()));
        sendSuccess(source, Component.literal("movespeed: " + s.getMoveSpeed()));
        
        sendSuccess(source, Component.literal("=== Utilities ==="));
        sendSuccess(source, Component.literal("autototem: " + s.isAutoTotemEnabled()));
        sendSuccess(source, Component.literal("totempriority: " + s.isTotemPriority() + " (don't replace totem with shield)"));
        sendSuccess(source, Component.literal("autoshield: " + s.isAutoShieldEnabled()));
        sendSuccess(source, Component.literal("autopotion: " + s.isAutoPotionEnabled()));
        sendSuccess(source, Component.literal("shieldbreak: " + s.isShieldBreakEnabled()));
        sendSuccess(source, Component.literal("prefersword: " + s.isPreferSword()));
        
        sendSuccess(source, Component.literal("=== Navigation Settings ==="));
        sendSuccess(source, Component.literal("bhop: " + s.isBhopEnabled()));
        sendSuccess(source, Component.literal("bhopcooldown: " + s.getBhopCooldown() + " ticks"));
        sendSuccess(source, Component.literal("jumpboost: " + s.getJumpBoost()));
        sendSuccess(source, Component.literal("idle: " + s.isIdleWanderEnabled()));
        sendSuccess(source, Component.literal("idleradius: " + s.getIdleWanderRadius()));
        sendSuccess(source, Component.literal("=== Factions & Mistakes ==="));
        sendSuccess(source, Component.literal("factions: " + s.isFactionsEnabled()));
        sendSuccess(source, Component.literal("friendlyfire: " + s.isFriendlyFireEnabled()));
        sendSuccess(source, Component.literal("misschance: " + s.getMissChance() + "%"));
        sendSuccess(source, Component.literal("mistakechance: " + s.getMistakeChance() + "%"));
        sendSuccess(source, Component.literal("reactiondelay: " + s.getReactionDelay() + " ticks"));
        return 1;
    }
    

    
    private static int listFactions(CommandSourceStack source) {
        var factions = BotFaction.getAllFactions();
        if (factions.isEmpty()) {
            sendSuccess(source, Component.literal("No factions created"));
        } else {
            sendSuccess(source, Component.literal("Factions (" + factions.size() + "):"));
            for (String faction : factions) {
                var members = BotFaction.getMembers(faction);
                var enemies = BotFaction.getHostileFactions(faction);
                sendSuccess(source, Component.literal(" - " + faction + " (" + members.size() + " members, " + enemies.size() + " enemies)"));
            }
        }
        return factions.size();
    }
    
    private static int createFaction(CommandSourceStack source, String name) {
        if (BotFaction.createFaction(name)) {
            sendSuccess(source, Component.literal("Faction '" + name + "' created!"));
            return 1;
        } else {
            source.sendError(Component.literal("Faction '" + name + "' already exists!"));
            return 0;
        }
    }
    
    private static int deleteFaction(CommandSourceStack source, String name) {
        if (BotFaction.deleteFaction(name)) {
            sendSuccess(source, Component.literal("Faction '" + name + "' deleted!"));
            return 1;
        } else {
            source.sendError(Component.literal("Faction '" + name + "' not found!"));
            return 0;
        }
    }
    
    private static int addToFaction(CommandSourceStack source, String faction, String player) {
        if (BotFaction.addMember(faction, player)) {
            sendSuccess(source, Component.literal("Added '" + player + "' to faction '" + faction + "'"));
            return 1;
        } else {
            source.sendError(Component.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
    }
    
    private static int removeFromFaction(CommandSourceStack source, String faction, String player) {
        if (BotFaction.removeMember(faction, player)) {
            sendSuccess(source, Component.literal("Removed '" + player + "' from faction '" + faction + "'"));
            return 1;
        } else {
            source.sendError(Component.literal("Failed to remove '" + player + "' from faction '" + faction + "'"));
            return 0;
        }
    }
    
    private static int setHostile(CommandSourceStack source, String faction1, String faction2, boolean hostile) {
        if (BotFaction.setHostile(faction1, faction2, hostile)) {
            if (hostile) {
                sendSuccess(source, Component.literal("Factions '" + faction1 + "' and '" + faction2 + "' are now hostile!"));
            } else {
                sendSuccess(source, Component.literal("Factions '" + faction1 + "' and '" + faction2 + "' are now neutral"));
            }
            return 1;
        } else {
            source.sendError(Component.literal("One or both factions not found, or same faction!"));
            return 0;
        }
    }
    
    private static int factionInfo(CommandSourceStack source, String faction) {
        var members = BotFaction.getMembers(faction);
        var enemies = BotFaction.getHostileFactions(faction);
        
        if (members.isEmpty() && enemies.isEmpty() && !BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Component.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        sendSuccess(source, Component.literal("=== Faction: " + faction + " ==="));
        sendSuccess(source, Component.literal("Members (" + members.size() + "): " + String.join(", ", members)));
        sendSuccess(source, Component.literal("Hostile to (" + enemies.size() + "): " + String.join(", ", enemies)));
        return 1;
    }
    
    private static int addNearbyBotsToFaction(CommandSourceStack source, String faction, double radius) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Component.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var entity = source.getEntity();
        if (entity == null) {
            source.sendError(Component.literal("This command must be run by a player!"));
            return 0;
        }
        
        int count = 0;
        var allBots = BotManager.getAllBots();
        var server = source.getServer();
        
        for (String botName : allBots) {
            var bot = server.getPlayerList().getPlayerByName(botName);
            if (bot != null && bot.distanceTo(entity) <= radius) {
                BotFaction.addMember(faction, botName);
                count++;
            }
        }
        
        final int added = count;
        sendSuccess(source, Component.literal("Added " + added + " bots to faction '" + faction + "'"));
        return count;
    }
    
    private static int addAllBotsToFaction(CommandSourceStack source, String faction) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Component.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var allBots = BotManager.getAllBots();
        int count = 0;
        
        for (String botName : allBots) {
            BotFaction.addMember(faction, botName);
            count++;
        }
        
        final int added = count;
        sendSuccess(source, Component.literal("Added " + added + " bots to faction '" + faction + "'"));
        return count;
    }
    
    private static int showInventory(CommandSourceStack source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        var bot = source.getServer().getPlayerList().getPlayerByName(botName);
        if (bot == null) {
            source.sendError(Component.literal("Bot '" + botName + "' not online!"));
            return 0;
        }
        

        if (HAS_INVVIEW) {
            try {
                return openInvViewGui(source, bot);
            } catch (Exception e) {
                source.sendError(Component.literal("Failed to open InvView GUI: " + e.getMessage()));
                return 0;
            }
        }
        

        source.sendError(Component.literal("InvView mod is not installed!"));
        sendSuccess(source, Component.literal("Please install InvView to view bot inventories: https://modrinth.com/mod/invview"));
        return 0;
    }
    
    
    private static int openInvViewGui(CommandSourceStack source, ServerPlayer targetPlayer) throws Exception {
        ServerPlayer viewer = source.getPlayer();
        if (viewer == null) {
            source.sendError(Component.literal("This command must be run by a player!"));
            return 0;
        }
        

        Class<?> simpleGuiClass = Class.forName("eu.pb4.sgui.api.gui.SimpleGui");
        Class<?> savingGuiClass = Class.forName("us.potatoboy.invview.gui.SavingPlayerDataGui");
        

        Object screenHandlerType = net.minecraft.screen.ScreenHandlerType.GENERIC_9X5;
        

        Object gui = savingGuiClass.getConstructor(
                net.minecraft.screen.ScreenHandlerType.class, 
                ServerPlayer.class, 
                ServerPlayer.class)
                .newInstance(screenHandlerType, viewer, targetPlayer);
        

        Method setTitleMethod = simpleGuiClass.getMethod("setTitle", Text.class);
        setTitleMethod.invoke(gui, targetPlayer.getName());
        

        Method setSlotRedirectMethod = simpleGuiClass.getMethod("setSlotRedirect", int.class, net.minecraft.screen.slot.Slot.class);
        var inventory = targetPlayer.getInventory();
        
        for (int i = 0; i < inventory.size(); i++) {

            net.minecraft.screen.slot.Slot slot = new net.minecraft.screen.slot.Slot(inventory, i, 0, 0);
            setSlotRedirectMethod.invoke(gui, i, slot);
        }
        

        Method openMethod = simpleGuiClass.getMethod("open");
        openMethod.invoke(gui);
        
        return 1;
    }
    
    private static int giveFactionItem(CommandSourceStack source, String faction, String itemCommand) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Component.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var members = BotFaction.getMembers(faction);
        var server = source.getServer();
        int count = 0;
        
        for (String memberName : members) {

            try {
                server.getCommands().getDispatcher().execute(
                    "give " + memberName + " " + itemCommand,
                    server.getSharedSuggestionProvider()
                );
                count++;
            } catch (Exception e) {

            }
        }
        
        final int given = count;
        sendSuccess(source, Component.literal("Gave items to " + given + " members of faction '" + faction + "'"));
        return count;
    }
    
    private static int factionAttack(CommandSourceStack source, String faction, String targetName) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Component.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var members = BotFaction.getMembers(faction);
        int count = 0;
        
        for (String memberName : members) {

            if (BotManager.getAllBots().contains(memberName)) {
                BotCombat.setTarget(memberName, targetName);
                count++;
            }
        }
        
        final int attacking = count;
        sendSuccess(source, Component.literal("Faction '" + faction + "' (" + attacking + " bots) attacking " + targetName + "!"));
        return count;
    }
    

    
    private static int createKit(CommandSourceStack source, String kitName) {
        var player = source.getPlayer();
        if (player == null) {
            source.sendError(Component.literal("This command must be run by a player!"));
            return 0;
        }
        
        if (BotKits.kitExists(kitName)) {
            source.sendError(Component.literal("Kit '" + kitName + "' already exists!"));
            return 0;
        }
        
        if (BotKits.createKit(kitName, player)) {
            sendSuccess(source, Component.literal("Kit '" + kitName + "' created from your inventory!"));
            return 1;
        } else {
            source.sendError(Component.literal("Failed to create kit (empty inventory?)"));
            return 0;
        }
    }
    
    private static int deleteKit(CommandSourceStack source, String kitName) {
        if (BotKits.deleteKit(kitName)) {
            sendSuccess(source, Component.literal("Kit '" + kitName + "' deleted!"));
            return 1;
        } else {
            source.sendError(Component.literal("Kit '" + kitName + "' not found!"));
            return 0;
        }
    }
    
    private static int listKits(CommandSourceStack source) {
        var kits = BotKits.getKitNames();
        if (kits.isEmpty()) {
            sendSuccess(source, Component.literal("No kits created. Use /pvpbot createkit <name> to create one."));
        } else {
            sendSuccess(source, Component.literal("Kits (" + kits.size() + "): " + String.join(", ", kits)));
        }
        return 1;
    }
    
    private static int giveKitToPlayer(CommandSourceStack source, String playerName, String kitName) {
        if (!BotKits.kitExists(kitName)) {
            source.sendError(Component.literal("Kit '" + kitName + "' not found!"));
            return 0;
        }
        

        var player = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            source.sendError(Component.literal("Player '" + playerName + "' not found!"));
            return 0;
        }
        
        if (BotKits.giveKit(kitName, player)) {
            sendSuccess(source, Component.literal("Gave kit '" + kitName + "' to '" + playerName + "'"));
            return 1;
        } else {
            source.sendError(Component.literal("Failed to give kit!"));
            return 0;
        }
    }
    
    private static int giveKitToFaction(CommandSourceStack source, String factionName, String kitName) {
        if (!BotFaction.getAllFactions().contains(factionName)) {
            source.sendError(Component.literal("Faction '" + factionName + "' not found!"));
            return 0;
        }
        
        if (!BotKits.kitExists(kitName)) {
            source.sendError(Component.literal("Kit '" + kitName + "' not found!"));
            return 0;
        }
        
        var members = BotFaction.getMembers(factionName);
        if (members == null || members.isEmpty()) {
            source.sendError(Component.literal("Faction '" + factionName + "' has no members!"));
            return 0;
        }
        
        int count = 0;
        for (String memberName : members) {

            if (BotManager.getAllBots().contains(memberName)) {
                var bot = BotManager.getBot(source.getServer(), memberName);
                if (bot != null && BotKits.giveKit(kitName, bot)) {
                    count++;
                }
            }
        }
        
        final int given = count;
        sendSuccess(source, Component.literal("Gave kit '" + kitName + "' to " + given + " bots in faction '" + factionName + "'"));
        return 1;
    }
    
    
    private static int openTestMenu(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendError(Component.literal("This command must be run by a player!"));
                return 0;
            }
            

            org.stepan1411.pvp_bot.gui.BotMenuGui.openMainMenu(player.getBukkitEntity());
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("Failed to open menu: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    
    private static int updateStats(CommandSourceStack source) {
        try {
            StatsReporter.sendStats();
            sendSuccess(source, Component.literal("Statistics sent to server!"));
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("Failed to send statistics: " + e.getMessage()));
            return 0;
        }
    }
    

    
    private static int toggleDebugPath(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.pathVisualization;
        BotDebug.setPathVisualization(botName, newValue);
        sendSuccess(source, Component.literal("Path visualization for " + botName + ": " + newValue));
        return newValue ? 1 : 0;
    }
    
    private static int setDebugPath(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setPathVisualization(botName, enabled);
        sendSuccess(source, Component.literal("Path visualization for " + botName + ": " + enabled));
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugTarget(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.targetVisualization;
        BotDebug.setTargetVisualization(botName, newValue);
        sendSuccess(source, Component.literal("Target visualization for " + botName + ": " + newValue));
        return newValue ? 1 : 0;
    }
    
    private static int setDebugTarget(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setTargetVisualization(botName, enabled);
        sendSuccess(source, Component.literal("Target visualization for " + botName + ": " + enabled));
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugCombat(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.combatInfo;
        BotDebug.setCombatInfo(botName, newValue);
        sendSuccess(source, Component.literal("Combat info for " + botName + ": " + newValue));
        return newValue ? 1 : 0;
    }
    
    private static int setDebugCombat(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setCombatInfo(botName, enabled);
        sendSuccess(source, Component.literal("Combat info for " + botName + ": " + enabled));
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugNavigation(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.navigationInfo;
        BotDebug.setNavigationInfo(botName, newValue);
        sendSuccess(source, Component.literal("Navigation info for " + botName + ": " + newValue));
        return newValue ? 1 : 0;
    }
    
    private static int setDebugNavigation(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setNavigationInfo(botName, enabled);
        sendSuccess(source, Component.literal("Navigation info for " + botName + ": " + enabled));
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugAll(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.isAnyEnabled();
        if (newValue) {
            BotDebug.enableAll(botName);
        } else {
            BotDebug.disableAll(botName);
        }
        sendSuccess(source, Component.literal("All debug modes for " + botName + ": " + newValue));
        return newValue ? 1 : 0;
    }
    
    private static int setDebugAll(CommandSourceStack source, String botName, boolean enabled) {
        if (enabled) {
            BotDebug.enableAll(botName);
        } else {
            BotDebug.disableAll(botName);
        }
        sendSuccess(source, Component.literal("All debug modes for " + botName + ": " + enabled));
        return enabled ? 1 : 0;
    }
    
    private static int showDebugStatus(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        sendSuccess(source, Component.literal("=== Debug Status for " + botName + " ==="));
        sendSuccess(source, Component.literal("Path visualization: " + settings.pathVisualization));
        sendSuccess(source, Component.literal("Target visualization: " + settings.targetVisualization));
        sendSuccess(source, Component.literal("Combat info: " + settings.combatInfo));
        sendSuccess(source, Component.literal("Navigation info: " + settings.navigationInfo));
        return 1;
    }
    

    
    private static int createPath(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.createPath(name)) {

            org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, true);
            sendSuccess(source, Component.literal("§aPath '" + name + "' created"));
            sendSuccess(source, Component.literal("§7Visualization enabled. To disable: §e/pvpbot path show " + name + " false"));
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' already exists"));
            return 0;
        }
    }
    
    private static int deletePath(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.deletePath(name)) {
            sendSuccess(source, Component.literal("§aPath '" + name + "' deleted"));
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int addPathPoint(CommandSourceStack source, String name) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendError(Component.literal("§cOnly players can add path points"));
            return 0;
        }
        
        net.minecraft.world.phys.Vec3 pos = new net.minecraft.world.phys.Vec3(player.getX(), player.getY(), player.getZ());
        if (org.stepan1411.pvp_bot.bot.BotPath.addPoint(name, pos)) {
            var path = org.stepan1411.pvp_bot.bot.BotPath.getPath(name);

            if (!org.stepan1411.pvp_bot.bot.BotPath.isPathVisible(name)) {
                org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, true);
                sendSuccess(source, Component.literal("§7Visualization enabled. To disable: §e/pvpbot path show " + name + " false"));
            }
            sendSuccess(source, Component.literal(String.format("§aPoint #%d added to path '%s' at (%.1f, %.1f, %.1f)",
                path.points.size(), name, pos.x, pos.y, pos.z)));
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int removeLastPathPoint(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.removeLastPoint(name)) {
            sendSuccess(source, Component.literal("§aLast point removed from path '" + name + "'"));
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found or empty"));
            return 0;
        }
    }
    
    private static int removePathPoint(CommandSourceStack source, String name, int index) {
        if (org.stepan1411.pvp_bot.bot.BotPath.removePoint(name, index)) {
            sendSuccess(source, Component.literal("§aPoint #" + index + " removed from path '" + name + "'"));
            return 1;
        } else {
            source.sendError(Component.literal("§cInvalid path or index"));
            return 0;
        }
    }
    
    private static int clearPath(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.clearPath(name)) {
            sendSuccess(source, Component.literal("§aAll points cleared from path '" + name + "'"));
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int setPathLoop(CommandSourceStack source, String name, boolean loop) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setLoop(name, loop)) {
            sendSuccess(source, Component.literal("§aPath '" + name + "' loop: " + loop));
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int setPathAttack(CommandSourceStack source, String name, boolean attack) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setAttack(name, attack)) {
            if (attack) {
                sendSuccess(source, Component.literal("§aPath '" + name + "' attack: enabled"));
            } else {
                sendSuccess(source, Component.literal("§aPath '" + name + "' attack: disabled"));
                sendSuccess(source, Component.literal("§7Bot will ignore attacks and continue following path"));
            }
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int startPathFollowing(CommandSourceStack source, String botName, String pathName) {
        if (org.stepan1411.pvp_bot.bot.BotPath.startFollowing(botName, pathName)) {
            sendSuccess(source, Component.literal("§aBot '" + botName + "' started following path '" + pathName + "'"));
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + pathName + "' not found or empty"));
            return 0;
        }
    }
    
    private static int stopPathFollowing(CommandSourceStack source, String botName) {
        if (org.stepan1411.pvp_bot.bot.BotPath.stopFollowing(botName)) {
            sendSuccess(source, Component.literal("§aBot '" + botName + "' stopped following path"));
            return 1;
        } else {
            source.sendError(Component.literal("§cBot '" + botName + "' is not following any path"));
            return 0;
        }
    }
    
    private static int listPaths(CommandSourceStack source) {
        var paths = org.stepan1411.pvp_bot.bot.BotPath.getAllPaths();
        if (paths.isEmpty()) {
            sendSuccess(source, Component.literal("§eNo paths created"));
            return 0;
        }
        
        sendSuccess(source, Component.literal("§6=== Paths ==="));
        for (var entry : paths.entrySet()) {
            String name = entry.getKey();
            var path = entry.getValue();
            sendSuccess(source, Component.literal(String.format("§e%s§7: %d points, loop: %s, attack: %s",
                name, path.points.size(), path.loop, path.attack)));
        }
        return paths.size();
    }
    
    private static int pathInfo(CommandSourceStack source, String name) {
        var path = org.stepan1411.pvp_bot.bot.BotPath.getPath(name);
        if (path == null) {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
        
        sendSuccess(source, Component.literal("§6=== Path: " + name + " ==="));
        sendSuccess(source, Component.literal("§7Points: " + path.points.size()));
        sendSuccess(source, Component.literal("§7Loop: " + path.loop));
        sendSuccess(source, Component.literal("§7Attack: " + path.attack));
        
        for (int i = 0; i < path.points.size(); i++) {
            var point = path.points.get(i);
            int index = i;
            sendSuccess(source, Component.literal(String.format("§e#%d§7: (%.1f, %.1f, %.1f)",
                index, point.x, point.y, point.z)));
        }
        
        return 1;
    }
    
    private static int showPath(CommandSourceStack source, String name, boolean visible) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, visible)) {
            if (visible) {
                sendSuccess(source, Component.literal("§aPath '" + name + "' visualization enabled"));
                sendSuccess(source, Component.literal("§7To disable: §e/pvpbot path show " + name + " false"));
            } else {
                sendSuccess(source, Component.literal("§aPath '" + name + "' visualization disabled"));
            }
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    

    
    private static int distributeBotsOnPath(CommandSourceStack source, String pathName) {
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Component.literal("§cPath '" + pathName + "' not found"));
            return 0;
        }
        
        if (path.points.isEmpty()) {
            source.sendError(Component.literal("§cPath '" + pathName + "' has no points"));
            return 0;
        }
        

        var server = source.getServer();
        var botsOnPath = new java.util.ArrayList<String>();
        for (String botName : BotManager.getAllBots()) {
            if (BotPath.isFollowing(botName, pathName)) {
                botsOnPath.add(botName);
            }
        }
        
        if (botsOnPath.isEmpty()) {
            source.sendError(Component.literal("§cNo bots are following path '" + pathName + "'"));
            return 0;
        }
        

        int totalPoints = path.points.size();
        int botCount = botsOnPath.size();
        
        for (int i = 0; i < botCount; i++) {
            String botName = botsOnPath.get(i);
            int pointIndex = (i * totalPoints) / botCount;
            

            BotPath.setBotPathIndex(botName, pointIndex);
            

            var point = path.points.get(pointIndex);
            try {
                String tpCommand = String.format(java.util.Locale.US,
                    "tp %s %.2f %.2f %.2f",
                    botName, point.x, point.y + 1.0, point.z
                );
                server.getCommands().getDispatcher().execute(tpCommand, server.getSharedSuggestionProvider());
            } catch (Exception e) {

            }
        }
        
        sendSuccess(source, Component.literal("§aDistributed " + botCount + " bots along path '" + pathName + "'"));
        return botCount;
    }
    
    private static int startPathNear(CommandSourceStack source, String pathName, double radius) {
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Component.literal("§cPath '" + pathName + "' not found"));
            return 0;
        }
        
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendError(Component.literal("§cThis command can only be used by a player"));
            return 0;
        }
        
        var server = source.getServer();
        int started = 0;
        
        for (String botName : BotManager.getAllBots()) {
            ServerPlayer bot = server.getPlayerList().getPlayerByName(botName);
            if (bot != null) {
                double distance = bot.distanceTo(player);
                if (distance <= radius) {
                    if (BotPath.startFollowing(botName, pathName)) {
                        started++;
                    }
                }
            }
        }
        
        if (started > 0) {
            int finalStarted = started;
            sendSuccess(source, Component.literal("§aStarted path '" + pathName + "' for " + finalStarted + " bots within " + radius + " blocks"));
            return started;
        } else {
            source.sendError(Component.literal("§cNo bots found within " + radius + " blocks"));
            return 0;
        }
    }
    
    private static int stopAllOnPath(CommandSourceStack source, String pathName) {
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Component.literal("§cPath '" + pathName + "' not found"));
            return 0;
        }
        
        int stopped = 0;
        for (String botName : BotManager.getAllBots()) {
            if (BotPath.isFollowing(botName, pathName)) {
                if (BotPath.stopFollowing(botName)) {
                    stopped++;
                }
            }
        }
        
        if (stopped > 0) {
            int finalStopped = stopped;
            sendSuccess(source, Component.literal("§aStopped " + finalStopped + " bots on path '" + pathName + "'"));
            return stopped;
        } else {
            source.sendError(Component.literal("§cNo bots are following path '" + pathName + "'"));
            return 0;
        }
    }
    

    
    private static int factionStartPath(CommandSourceStack source, String factionName, String pathName) {
        var members = BotFaction.getMembers(factionName);
        if (members.isEmpty()) {
            source.sendError(Component.literal("§cFaction '" + factionName + "' not found or has no members"));
            return 0;
        }
        
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Component.literal("§cPath '" + pathName + "' not found"));
            return 0;
        }
        
        int started = 0;
        for (String member : members) {
            if (BotManager.getAllBots().contains(member)) {
                if (BotPath.startFollowing(member, pathName)) {
                    started++;
                }
            }
        }
        
        if (started > 0) {
            int finalStarted = started;
            sendSuccess(source, Component.literal("§aStarted path '" + pathName + "' for " + finalStarted + " bots in faction '" + factionName + "'"));
            return started;
        } else {
            source.sendError(Component.literal("§cNo bots in faction '" + factionName + "'"));
            return 0;
        }
    }
    
    private static int factionStopPath(CommandSourceStack source, String factionName) {
        var members = BotFaction.getMembers(factionName);
        if (members.isEmpty()) {
            source.sendError(Component.literal("§cFaction '" + factionName + "' not found or has no members"));
            return 0;
        }
        
        int stopped = 0;
        for (String member : members) {
            if (BotManager.getAllBots().contains(member)) {
                if (BotPath.stopFollowing(member)) {
                    stopped++;
                }
            }
        }
        
        if (stopped > 0) {
            int finalStopped = stopped;
            sendSuccess(source, Component.literal("§aStopped path for " + finalStopped + " bots in faction '" + factionName + "'"));
            return stopped;
        } else {
            source.sendError(Component.literal("§cNo bots in faction '" + factionName + "' were following a path"));
            return 0;
        }
    }
    

    private static int setBotFollow(CommandSourceStack source, String botName, String targetName, boolean escort) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("§cBot '" + botName + "' not found"));
            return 0;
        }
        

        BotMovement.setFollow(botName, targetName, escort);
        
        String mode = escort ? "escorting" : "following";
        sendSuccess(source, Component.literal("§aBot '" + botName + "' is now " + mode + " '" + targetName + "'"));
        return 1;
    }
    

    private static int stopBotMovement(CommandSourceStack source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("§cBot '" + botName + "' not found"));
            return 0;
        }
        
        BotMovement.stop(botName);
        sendSuccess(source, Component.literal("§aBot '" + botName + "' movement stopped"));
        return 1;
    }
    

    private static int setBotGoto(CommandSourceStack source, String botName, double x, double y, double z) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("§cBot '" + botName + "' not found"));
            return 0;
        }
        

        Vec3 targetPos = new Vec3(x, y, z);
        BotMovement.setGoto(botName, targetPos);
        
        sendSuccess(source, Component.literal("§aBot '" + botName + "' is moving to " +
            String.format("%.1f %.1f %.1f", x, y, z)));
        return 1;
    }
    

    private static int factionFollow(CommandSourceStack source, String factionName, String targetName, boolean escort) {
        var members = BotFaction.getMembers(factionName);
        if (members.isEmpty()) {
            source.sendError(Component.literal("§cFaction '" + factionName + "' not found or has no members"));
            return 0;
        }
        
        int count = 0;
        String mode = escort ? "escorting" : "following";
        for (String member : members) {
            if (BotManager.getAllBots().contains(member)) {
                BotMovement.setFollow(member, targetName, escort);
                count++;
            }
        }
        
        if (count > 0) {
            int finalCount = count;
            sendSuccess(source, Component.literal("§a" + finalCount + " bots in faction '" + factionName + "' are now " + mode + " '" + targetName + "'"));
            return count;
        } else {
            source.sendError(Component.literal("§cNo bots found in faction '" + factionName + "'"));
            return 0;
        }
    }
    
    private static int factionGoto(CommandSourceStack source, String factionName, double x, double y, double z) {
        var members = BotFaction.getMembers(factionName);
        if (members.isEmpty()) {
            source.sendError(Component.literal("§cFaction '" + factionName + "' not found or has no members"));
            return 0;
        }
        
        int count = 0;
        Vec3 targetPos = new Vec3(x, y, z);
        for (String member : members) {
            if (BotManager.getAllBots().contains(member)) {
                BotMovement.setGoto(member, targetPos);
                count++;
            }
        }
        
        if (count > 0) {
            int finalCount = count;
            sendSuccess(source, Component.literal("§a" + finalCount + " bots in faction '" + factionName + "' are moving to " +
                String.format("%.1f %.1f %.1f", x, y, z)));
            return count;
        } else {
            source.sendError(Component.literal("§cNo bots found in faction '" + factionName + "'"));
            return 0;
        }
    }
    
    
    private static int showApiDebugInfo(CommandSourceStack source) {
        try {
            String debugInfo = PvpBotAPI.getDebugInfo();
            String[] lines = debugInfo.split("\n");
            
            sendSuccess(source, Component.literal("=== PVP Bot API Debug Info ==="));
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sendSuccess(source, Component.literal(line));
                }
            }
            

            var eventManager = PvpBotAPI.getEventManager();
            var strategyRegistry = PvpBotAPI.getCombatStrategyRegistry();
            
            sendSuccess(source, Component.literal(""));
            sendSuccess(source, Component.literal("=== Detailed Event Handler Info ==="));
            sendSuccess(source, Component.literal("Spawn handlers: " + eventManager.getSpawnHandlerCount()));
            sendSuccess(source, Component.literal("Death handlers: " + eventManager.getDeathHandlerCount()));
            sendSuccess(source, Component.literal("Attack handlers: " + eventManager.getAttackHandlerCount()));
            sendSuccess(source, Component.literal("Damage handlers: " + eventManager.getDamageHandlerCount()));
            sendSuccess(source, Component.literal("Tick handlers: " + eventManager.getTickHandlerCount()));
            
            sendSuccess(source, Component.literal(""));
            sendSuccess(source, Component.literal("=== Combat Strategy Details ==="));
            String strategyDebugInfo = strategyRegistry.getDebugInfo();
            String[] strategyLines = strategyDebugInfo.split("\n");
            for (String line : strategyLines) {
                if (!line.trim().isEmpty()) {
                    sendSuccess(source, Component.literal(line));
                }
            }
            
            sendSuccess(source, Component.literal(""));
            sendSuccess(source, Component.literal("API Status: " + (PvpBotAPI.isInitialized() ? "Initialized" : "Not Initialized")));
            
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("Failed to get API debug info: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
