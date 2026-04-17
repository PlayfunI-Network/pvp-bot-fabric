package org.stepan1411.pvp_bot.command;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
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
    

    private static final SuggestionProvider<CommandSourceStack> BOT_SUGGESTIONS = (ctx, builder) -> {
        var server = ctx.getSource().getServer();
        var aliveBots = BotManager.getAllBots().stream()
            .filter(name -> {
                var bot = server.getPlayerList().getPlayerByName(name);
                return bot != null && bot.isAlive();
            })
            .collect(Collectors.toList());
        return SharedSuggestionProvider.suggest(aliveBots, builder);
    };
    

    private static final SuggestionProvider<CommandSourceStack> TARGET_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(
            ctx.getSource().getServer().getPlayerList().getPlayers().stream()
                .map(p -> p.getName().getString())
                .collect(Collectors.toList()), 
            builder);
    

    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = TARGET_SUGGESTIONS;
    

    private static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(BotFaction.getAllFactions(), builder);
    

    private static final SuggestionProvider<CommandSourceStack> KIT_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(BotKits.getKitNames(), builder);
    

    private static final SuggestionProvider<CommandSourceStack> PATH_SUGGESTIONS = (ctx, builder) -> 
        SharedSuggestionProvider.suggest(BotPath.getAllPaths().keySet(), builder);

    public static void register(io.papermc.paper.command.brigadier.Commands commands) {
        commands.register(
            Commands.literal("pvpbot")
                

                .then(Commands.literal("spawn")
                    .executes(ctx -> spawnBot(ctx.getSource(), BotNameGenerator.generateUniqueName()))
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> spawnBot(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("massspawn")
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
                        .executes(ctx -> massSpawnBots(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))
                    )
                )
                

                .then(Commands.literal("remove")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> removeBot(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("removeall")
                    .executes(ctx -> removeAllBots(ctx.getSource()))
                )
                

                .then(Commands.literal("list")
                    .executes(ctx -> listBots(ctx.getSource()))
                )
                

                .then(Commands.literal("sync")
                    .executes(ctx -> syncBots(ctx.getSource()))
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(ctx -> syncBot(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("debug")
                    .then(Commands.argument("bot", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        

                        .then(Commands.literal("path")
                            .executes(ctx -> toggleDebugPath(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugPath(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("target")
                            .executes(ctx -> toggleDebugTarget(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugTarget(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("combat")
                            .executes(ctx -> toggleDebugCombat(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugCombat(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("navigation")
                            .executes(ctx -> toggleDebugNavigation(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugNavigation(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("all")
                            .executes(ctx -> toggleDebugAll(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugAll(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        

                        .then(Commands.literal("status")
                            .executes(ctx -> showDebugStatus(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                        )
                    )
                    

                    .then(Commands.literal("api")
                        .executes(ctx -> showApiDebugInfo(ctx.getSource()))
                    )
                )
                

                .then(Commands.literal("menu")
                    .executes(ctx -> openTestMenu(ctx.getSource()))
                )
                

                .then(Commands.literal("settings")
                    .executes(ctx -> showSettings(ctx.getSource()))
                    

                    .then(Commands.literal("gui")
                        .executes(ctx -> openSettingsGui(ctx.getSource()))
                    )
                    

                    .then(Commands.literal("autoarmor")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("autoarmor: " + BotSettings.get().isAutoEquipArmor()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEquipArmor(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto equip armor: " + BotSettings.get().isAutoEquipArmor()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autoweapon")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("autoweapon: " + BotSettings.get().isAutoEquipWeapon()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEquipWeapon(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto equip weapon: " + BotSettings.get().isAutoEquipWeapon()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("droparmor")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("droparmor: " + BotSettings.get().isDropWorseArmor()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setDropWorseArmor(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Drop worse armor: " + BotSettings.get().isDropWorseArmor()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("dropweapon")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("dropweapon: " + BotSettings.get().isDropWorseWeapons()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setDropWorseWeapons(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Drop worse weapons: " + BotSettings.get().isDropWorseWeapons()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("dropdistance")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("dropdistance: " + BotSettings.get().getDropDistance()), false); return 1; })
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                            .executes(ctx -> {
                                BotSettings.get().setDropDistance(DoubleArgumentType.getDouble(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Drop distance: " + BotSettings.get().getDropDistance()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("interval")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("interval: " + BotSettings.get().getCheckInterval() + " ticks"), false); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> {
                                BotSettings.get().setCheckInterval(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Check interval: " + BotSettings.get().getCheckInterval() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("minarmorlevel")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("minarmorlevel: " + BotSettings.get().getMinArmorLevel()), false); return 1; })
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMinArmorLevel(IntegerArgumentType.getInteger(ctx, "level"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Min armor level: " + BotSettings.get().getMinArmorLevel() + " (0=any, 20=leather+, 40=gold+, 50=chain+, 60=iron+, 80=diamond+, 100=netherite)"), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("combat")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("combat: " + BotSettings.get().isCombatEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setCombatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Combat enabled: " + BotSettings.get().isCombatEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("revenge")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("revenge: " + BotSettings.get().isRevengeEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRevengeEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Revenge mode: " + BotSettings.get().isRevengeEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("autotarget")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("autotarget: " + BotSettings.get().isAutoTargetEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoTargetEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto target: " + BotSettings.get().isAutoTargetEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("targetplayers")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("targetplayers: " + BotSettings.get().isTargetPlayers()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetPlayers(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Target players: " + BotSettings.get().isTargetPlayers()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("targetmobs")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("targetmobs: " + BotSettings.get().isTargetHostileMobs()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetHostileMobs(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Target hostile mobs: " + BotSettings.get().isTargetHostileMobs()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("targetbots")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("targetbots: " + BotSettings.get().isTargetOtherBots()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetOtherBots(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Target other bots: " + BotSettings.get().isTargetOtherBots()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("criticals")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("criticals: " + BotSettings.get().isCriticalsEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setCriticalsEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Criticals: " + BotSettings.get().isCriticalsEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("ranged")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("ranged: " + BotSettings.get().isRangedEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRangedEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Ranged weapons: " + BotSettings.get().isRangedEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("mace")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("mace: " + BotSettings.get().isMaceEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setMaceEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Mace combat: " + BotSettings.get().isMaceEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramace")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("elytramace: " + BotSettings.get().isElytraMaceEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("ElytraMace trick: " + BotSettings.get().isElytraMaceEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("specialnames")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("specialnames: " + BotSettings.get().isUseSpecialNames()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setUseSpecialNames(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Use special names: " + BotSettings.get().isUseSpecialNames()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramaceretries")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("elytramaceretries: " + BotSettings.get().getElytraMaceMaxRetries()), false); return 1; })
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceMaxRetries(IntegerArgumentType.getInteger(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("ElytraMace max retries: " + BotSettings.get().getElytraMaceMaxRetries()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramacealtitude")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("elytramacealtitude: " + BotSettings.get().getElytraMaceMinAltitude()), false); return 1; })
                        .then(Commands.argument("value", IntegerArgumentType.integer(5, 50))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceMinAltitude(IntegerArgumentType.getInteger(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("ElytraMace min altitude: " + BotSettings.get().getElytraMaceMinAltitude()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramacedistance")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("elytramacedistance: " + BotSettings.get().getElytraMaceAttackDistance()), false); return 1; })
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(3.0, 15.0))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceAttackDistance(DoubleArgumentType.getDouble(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("ElytraMace attack distance: " + BotSettings.get().getElytraMaceAttackDistance()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("elytramacefireworks")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("elytramacefireworks: " + BotSettings.get().getElytraMaceFireworkCount()), false); return 1; })
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 10))
                            .executes(ctx -> {
                                BotSettings.get().setElytraMaceFireworkCount(IntegerArgumentType.getInteger(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("ElytraMace firework count: " + BotSettings.get().getElytraMaceFireworkCount()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("gotousebaritone")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("gotousebaritone: " + BotSettings.get().isGotoUseBaritone()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setGotoUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Goto use Baritone: " + BotSettings.get().isGotoUseBaritone()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("escortusebaritone")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("escortusebaritone: " + BotSettings.get().isEscortUseBaritone()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setEscortUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Escort use Baritone: " + BotSettings.get().isEscortUseBaritone()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("followusebaritone")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("followusebaritone: " + BotSettings.get().isFollowUseBaritone()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setFollowUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Follow use Baritone: " + BotSettings.get().isFollowUseBaritone()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("shieldmace")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("shieldmace: " + BotSettings.get().isShieldMace()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setShieldMace(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Shield mace defense: " + BotSettings.get().isShieldMace()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("prefershieldmace")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("prefershieldmace: " + BotSettings.get().isPreferShieldMace()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setPreferShieldMace(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Prefer shield over totem for mace: " + BotSettings.get().isPreferShieldMace()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("shieldmainhand")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("shieldmainhand: " + BotSettings.get().isShieldMainHand()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setShieldMainHand(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Shield in main hand when having totem: " + BotSettings.get().isShieldMainHand()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("attackcooldown")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("attackcooldown: " + BotSettings.get().getAttackCooldown() + " ticks"), false); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 40))
                            .executes(ctx -> {
                                BotSettings.get().setAttackCooldown(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Attack cooldown: " + BotSettings.get().getAttackCooldown() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("meleerange")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("meleerange: " + BotSettings.get().getMeleeRange()), false); return 1; })
                        .then(Commands.argument("range", DoubleArgumentType.doubleArg(2.0, 6.0))
                            .executes(ctx -> {
                                BotSettings.get().setMeleeRange(DoubleArgumentType.getDouble(ctx, "range"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Melee range: " + BotSettings.get().getMeleeRange()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("movespeed")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("movespeed: " + BotSettings.get().getMoveSpeed()), false); return 1; })
                        .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.1, 2.0))
                            .executes(ctx -> {
                                BotSettings.get().setMoveSpeed(DoubleArgumentType.getDouble(ctx, "speed"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Move speed: " + BotSettings.get().getMoveSpeed()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("bhop")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("bhop: " + BotSettings.get().isBhopEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setBhopEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Bhop enabled: " + BotSettings.get().isBhopEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("bhopcooldown")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("bhopcooldown: " + BotSettings.get().getBhopCooldown() + " ticks"), false); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(5, 30))
                            .executes(ctx -> {
                                BotSettings.get().setBhopCooldown(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Bhop cooldown: " + BotSettings.get().getBhopCooldown() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("jumpboost")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("jumpboost: " + BotSettings.get().getJumpBoost()), false); return 1; })
                        .then(Commands.argument("boost", DoubleArgumentType.doubleArg(0.0, 0.5))
                            .executes(ctx -> {
                                BotSettings.get().setJumpBoost(DoubleArgumentType.getDouble(ctx, "boost"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Jump boost: " + BotSettings.get().getJumpBoost()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("idle")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("idle: " + BotSettings.get().isIdleWanderEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setIdleWanderEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Idle wander: " + BotSettings.get().isIdleWanderEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("usebaritone")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("usebaritone: " + BotSettings.get().isUseBaritone()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setUseBaritone(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Use Baritone: " + BotSettings.get().isUseBaritone()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("idleradius")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("idleradius: " + BotSettings.get().getIdleWanderRadius()), false); return 1; })
                        .then(Commands.argument("radius", DoubleArgumentType.doubleArg(3.0, 50.0))
                            .executes(ctx -> {
                                BotSettings.get().setIdleWanderRadius(DoubleArgumentType.getDouble(ctx, "radius"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Idle wander radius: " + BotSettings.get().getIdleWanderRadius()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("friendlyfire")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("friendlyfire: " + BotSettings.get().isFriendlyFireEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setFriendlyFireEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Friendly fire: " + BotSettings.get().isFriendlyFireEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("misschance")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("misschance: " + BotSettings.get().getMissChance() + "%"), false); return 1; })
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMissChance(IntegerArgumentType.getInteger(ctx, "percent"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Miss chance: " + BotSettings.get().getMissChance() + "%"), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("mistakechance")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("mistakechance: " + BotSettings.get().getMistakeChance() + "%"), false); return 1; })
                        .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMistakeChance(IntegerArgumentType.getInteger(ctx, "percent"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Mistake chance: " + BotSettings.get().getMistakeChance() + "%"), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("reactiondelay")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("reactiondelay: " + BotSettings.get().getReactionDelay() + " ticks"), false); return 1; })
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 20))
                            .executes(ctx -> {
                                BotSettings.get().setReactionDelay(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Reaction delay: " + BotSettings.get().getReactionDelay() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("prefersword")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("prefersword: " + BotSettings.get().isPreferSword()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setPreferSword(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Prefer sword: " + BotSettings.get().isPreferSword()), true);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("shieldbreak")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("shieldbreak: " + BotSettings.get().isShieldBreakEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setShieldBreakEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Shield break: " + BotSettings.get().isShieldBreakEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autoshield")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("autoshield: " + BotSettings.get().isAutoShieldEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoShieldEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto shield: " + BotSettings.get().isAutoShieldEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autopotion")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("autopotion: " + BotSettings.get().isAutoPotionEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoPotionEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto potion: " + BotSettings.get().isAutoPotionEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autototem")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("autototem: " + BotSettings.get().isAutoTotemEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoTotemEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto totem: " + BotSettings.get().isAutoTotemEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("totempriority")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("totempriority: " + BotSettings.get().isTotemPriority()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTotemPriority(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Totem priority (don't replace with shield): " + BotSettings.get().isTotemPriority()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("retreat")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("retreat: " + BotSettings.get().isRetreatEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRetreatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Retreat enabled: " + BotSettings.get().isRetreatEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("autoeat")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("autoeat: " + BotSettings.get().isAutoEatEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto eat enabled: " + BotSettings.get().isAutoEatEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    

                    .then(Commands.literal("automend")
                        .executes(ctx -> { ctx.getSource().sendSuccess(() -> Component.literal("automend: " + BotSettings.get().isAutoMendEnabled()), false); return 1; })
                        .then(Commands.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoMendEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendSuccess(() -> Component.literal("Auto mend enabled: " + BotSettings.get().isAutoMendEnabled()), true);
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
                            .executes(ctx -> setAttackTarget(ctx.getSource(), 
                                StringArgumentType.getString(ctx, "botname"),
                                StringArgumentType.getString(ctx, "target")))
                        )
                    )
                )
                

                .then(Commands.literal("stop")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> stopAttack(ctx.getSource(), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("stopmovement")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> stopBotMovement(ctx.getSource(), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("follow")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .then(Commands.argument("target", StringArgumentType.word())
                            .suggests(TARGET_SUGGESTIONS)
                            .executes(ctx -> setBotFollow(ctx.getSource(), 
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
                            .executes(ctx -> setBotFollow(ctx.getSource(), 
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
                                    .executes(ctx -> setBotGoto(ctx.getSource(), 
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
                        .executes(ctx -> showTarget(ctx.getSource(), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("inventory")
                    .then(Commands.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> showInventory(ctx.getSource(), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                

                .then(Commands.literal("faction")

                    .then(Commands.literal("list")
                        .executes(ctx -> listFactions(ctx.getSource()))
                    )

                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> createFaction(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )

                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> deleteFaction(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )

                    .then(Commands.literal("add")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> addToFaction(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "player")))
                            )
                        )
                    )

                    .then(Commands.literal("remove")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.word())
                                .executes(ctx -> removeFromFaction(ctx.getSource(), 
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
                                .executes(ctx -> setHostile(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction1"),
                                    StringArgumentType.getString(ctx, "faction2"),
                                    true))
                                .then(Commands.argument("hostile", BoolArgumentType.bool())
                                    .executes(ctx -> setHostile(ctx.getSource(), 
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
                            .executes(ctx -> factionInfo(ctx.getSource(), StringArgumentType.getString(ctx, "faction")))
                        )
                    )

                    .then(Commands.literal("addnear")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0, 10000.0))
                                .executes(ctx -> addNearbyBotsToFaction(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    DoubleArgumentType.getDouble(ctx, "radius")))
                            )
                        )
                    )

                    .then(Commands.literal("addall")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> addAllBotsToFaction(ctx.getSource(), 
                                StringArgumentType.getString(ctx, "faction")))
                        )
                    )

                    .then(Commands.literal("give")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("item", StringArgumentType.greedyString())
                                .executes(ctx -> giveFactionItem(ctx.getSource(), 
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
                                .executes(ctx -> factionAttack(ctx.getSource(), 
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
                                .executes(ctx -> factionStartPath(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )

                    .then(Commands.literal("stoppath")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> factionStopPath(ctx.getSource(), 
                                StringArgumentType.getString(ctx, "faction")))
                        )
                    )
                    
                    .then(Commands.literal("follow")
                        .then(Commands.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(Commands.argument("target", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> factionFollow(ctx.getSource(), 
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
                                .executes(ctx -> factionFollow(ctx.getSource(), 
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
                                        .executes(ctx -> factionGoto(ctx.getSource(), 
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
                            ctx.getSource().sendSuccess(() -> Component.literal("viewdistance: " + BotSettings.get().getMaxTargetDistance()), false); 
                            return 1; 
                        })
                        .then(Commands.argument("distance", DoubleArgumentType.doubleArg(5.0, 128.0))
                            .executes(ctx -> {
                                BotSettings.get().setMaxTargetDistance(DoubleArgumentType.getDouble(ctx, "distance"));
                                ctx.getSource().sendSuccess(() -> Component.literal("View distance: " + BotSettings.get().getMaxTargetDistance()), true);
                                return 1;
                            })
                        )
                    )
                )
                


                .then(Commands.literal("createkit")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> createKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("deletekit")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .suggests(KIT_SUGGESTIONS)
                        .executes(ctx -> deleteKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                

                .then(Commands.literal("kits")
                    .executes(ctx -> listKits(ctx.getSource()))
                )
                

                .then(Commands.literal("givekit")
                    .then(Commands.argument("playername", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .then(Commands.argument("kitname", StringArgumentType.word())
                            .suggests(KIT_SUGGESTIONS)
                            .executes(ctx -> giveKitToPlayer(ctx.getSource(), 
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
                                .executes(ctx -> giveKitToFaction(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "kitname")))
                            )
                        )
                    )
                )
                

                

                .then(Commands.literal("path")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .executes(ctx -> createPath(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> deletePath(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("addpoint")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> addPathPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("removepoint")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> removeLastPathPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                            .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> removePathPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "index")))
                            )
                        )
                    )
                    .then(Commands.literal("clear")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> clearPath(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(Commands.literal("loop")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPathLoop(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "value")))
                            )
                        )
                    )
                    .then(Commands.literal("attack")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPathAttack(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "value")))
                            )
                        )
                    )
                    .then(Commands.literal("start")
                        .then(Commands.argument("bot", StringArgumentType.word())
                            .suggests(BOT_SUGGESTIONS)
                            .then(Commands.argument("path", StringArgumentType.word())
                                .suggests(PATH_SUGGESTIONS)
                                .executes(ctx -> startPathFollowing(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )
                    .then(Commands.literal("stop")
                        .then(Commands.argument("bot", StringArgumentType.word())
                            .suggests(BOT_SUGGESTIONS)
                            .executes(ctx -> stopPathFollowing(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                        )
                    )
                    .then(Commands.literal("list")
                        .executes(ctx -> listPaths(ctx.getSource()))
                    )
                    .then(Commands.literal("show")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("visible", BoolArgumentType.bool())
                                .executes(ctx -> showPath(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "visible")))
                            )
                        )
                    )
                    .then(Commands.literal("info")
                        .then(Commands.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> pathInfo(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )

                    .then(Commands.literal("distribute")
                        .then(Commands.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> distributeBotsOnPath(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                        )
                    )

                    .then(Commands.literal("startnear")
                        .then(Commands.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> startPathNear(ctx.getSource(), StringArgumentType.getString(ctx, "path"), DoubleArgumentType.getDouble(ctx, "radius")))
                            )
                        )
                    )

                    .then(Commands.literal("stopall")
                        .then(Commands.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> stopAllOnPath(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                        )
                    )
                )
                .then(Commands.literal("updatestats")
                    .executes(ctx -> updateStats(ctx.getSource()))
                )
        );
    }
    
    private static int setAttackTarget(CommandSourceStack source, String botName, String targetName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        BotCombat.setTarget(botName, targetName);
        source.sendSuccess(() -> Component.literal("Bot '" + botName + "' now attacking '" + targetName + "'"), true);
        return 1;
    }
    
    private static int stopAttack(CommandSourceStack source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        BotCombat.clearTarget(botName);
        source.sendSuccess(() -> Component.literal("Bot '" + botName + "' stopped attacking"), true);
        return 1;
    }
    
    private static int showTarget(CommandSourceStack source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        var target = BotCombat.getTarget(botName);
        if (target != null) {
            source.sendSuccess(() -> Component.literal("Bot '" + botName + "' target: " + target.getName().getString()), false);
        } else {
            source.sendSuccess(() -> Component.literal("Bot '" + botName + "' has no target"), false);
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
            source.sendSuccess(() -> Component.literal("PvP Bot '" + name + "' spawned!"), true);
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
        
        source.sendSuccess(() -> Component.literal("Spawning " + count + " bots "), false);
        

        scheduleSpawn(server, source, count, spawned, current);
        
        return 1;
    }
    
    private static void scheduleSpawn(net.minecraft.server.MinecraftServer server, CommandSourceStack source, int total, int[] spawned, int[] current) {
        if (current[0] >= total) {
            source.sendSuccess(() -> Component.literal("Finished! Spawned " + spawned[0] + " bots."), true);
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
            source.sendSuccess(() -> Component.literal("Bot '" + name + "' removed!"), true);
            return 1;
        } else {
            source.sendError(Component.literal("Bot '" + name + "' not found!"));
            return 0;
        }
    }

    private static int removeAllBots(CommandSourceStack source) {
        int count = BotManager.getBotCount();
        BotManager.removeAllBots(source.getServer(), source);
        source.sendSuccess(() -> Component.literal("Removed " + count + " bots"), true);
        return count;
    }

    private static int listBots(CommandSourceStack source) {
        var bots = BotManager.getAllBots();
        
        if (bots.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No active PvP bots"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Active PvP bots (" + bots.size() + "):"), false);
            for (String botName : bots) {
                source.sendSuccess(() -> Component.literal(" - " + botName), false);
            }
        }
        return bots.size();
    }
    
    private static int syncBots(CommandSourceStack source) {
        var server = source.getServer();
        int beforeCount = BotManager.getAllBots().size();
        

        source.sendSuccess(() -> Component.literal("=== Players on server ==="), false);
        for (var player : server.getPlayerList().getPlayers()) {
            String name = player.getName().getString();
            String className = player.getClass().getName();
            boolean inList = BotManager.getAllBots().contains(name);
            source.sendSuccess(() -> Component.literal(" - " + name + " [" + className + "] " + (inList ? "(in list)" : "(NOT in list)")), false);
        }
        

        BotManager.syncBots(server);
        
        int afterCount = BotManager.getAllBots().size();
        int added = afterCount - beforeCount;
        
        source.sendSuccess(() -> Component.literal("Synced! Added " + added + " bots. Total: " + afterCount), true);
        return added;
    }
    
    private static int syncBot(CommandSourceStack source, String name) {
        var server = source.getServer();
        

        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player == null) {
            source.sendSuccess(() -> Component.literal("Player " + name + " not found on server!"), false);
            return 0;
        }
        

        String className = player.getClass().getName();
        boolean inList = BotManager.getAllBots().contains(name);
        source.sendSuccess(() -> Component.literal("Player: " + name), false);
        source.sendSuccess(() -> Component.literal("Class: " + className), false);
        source.sendSuccess(() -> Component.literal("In bot list: " + inList), false);
        

        boolean added = BotManager.syncBot(server, name);
        
        if (added) {
            source.sendSuccess(() -> Component.literal("Successfully added " + name + " to bot list!"), true);
            return 1;
        } else if (inList) {
            source.sendSuccess(() -> Component.literal(name + " is already in bot list!"), false);
            return 0;
        } else {
            source.sendSuccess(() -> Component.literal(name + " is not a fake player (HeroBot bot)!"), false);
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
            
            SettingsGui gui = new SettingsGui(player);
            gui.open();
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("Failed to open settings GUI: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int showSettings(CommandSourceStack source) {
        BotSettings s = BotSettings.get();
        source.sendSuccess(() -> Component.literal("=== Equipment Settings ==="), false);
        source.sendSuccess(() -> Component.literal("autoarmor: " + s.isAutoEquipArmor()), false);
        source.sendSuccess(() -> Component.literal("autoweapon: " + s.isAutoEquipWeapon()), false);
        source.sendSuccess(() -> Component.literal("droparmor: " + s.isDropWorseArmor()), false);
        source.sendSuccess(() -> Component.literal("dropweapon: " + s.isDropWorseWeapons()), false);
        source.sendSuccess(() -> Component.literal("dropdistance: " + s.getDropDistance()), false);
        source.sendSuccess(() -> Component.literal("interval: " + s.getCheckInterval() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("minarmorlevel: " + s.getMinArmorLevel()), false);
        
        source.sendSuccess(() -> Component.literal("=== Combat Settings ==="), false);
        source.sendSuccess(() -> Component.literal("combat: " + s.isCombatEnabled()), false);
        source.sendSuccess(() -> Component.literal("revenge: " + s.isRevengeEnabled()), false);
        source.sendSuccess(() -> Component.literal("autotarget: " + s.isAutoTargetEnabled()), false);
        source.sendSuccess(() -> Component.literal("targetplayers: " + s.isTargetPlayers()), false);
        source.sendSuccess(() -> Component.literal("targetmobs: " + s.isTargetHostileMobs()), false);
        source.sendSuccess(() -> Component.literal("targetbots: " + s.isTargetOtherBots()), false);
        source.sendSuccess(() -> Component.literal("criticals: " + s.isCriticalsEnabled()), false);
        source.sendSuccess(() -> Component.literal("ranged: " + s.isRangedEnabled()), false);
        source.sendSuccess(() -> Component.literal("mace: " + s.isMaceEnabled()), false);
        source.sendSuccess(() -> Component.literal("elytramace: " + s.isElytraMaceEnabled()), false);
        source.sendSuccess(() -> Component.literal("specialnames: " + s.isUseSpecialNames()), false);
        source.sendSuccess(() -> Component.literal("elytramaceretries: " + s.getElytraMaceMaxRetries()), false);
        source.sendSuccess(() -> Component.literal("elytramacealtitude: " + s.getElytraMaceMinAltitude()), false);
        source.sendSuccess(() -> Component.literal("elytramacedistance: " + s.getElytraMaceAttackDistance()), false);
        source.sendSuccess(() -> Component.literal("elytramacefireworks: " + s.getElytraMaceFireworkCount()), false);
        source.sendSuccess(() -> Component.literal("gotousebaritone: " + s.isGotoUseBaritone()), false);
        source.sendSuccess(() -> Component.literal("escortusebaritone: " + s.isEscortUseBaritone()), false);
        source.sendSuccess(() -> Component.literal("followusebaritone: " + s.isFollowUseBaritone()), false);
        source.sendSuccess(() -> Component.literal("shieldmace: " + s.isShieldMace()), false);
        source.sendSuccess(() -> Component.literal("prefershieldmace: " + s.isPreferShieldMace()), false);
        source.sendSuccess(() -> Component.literal("shieldmainhand: " + s.isShieldMainHand()), false);
        source.sendSuccess(() -> Component.literal("attackcooldown: " + s.getAttackCooldown() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("meleerange: " + s.getMeleeRange()), false);
        source.sendSuccess(() -> Component.literal("movespeed: " + s.getMoveSpeed()), false);
        
        source.sendSuccess(() -> Component.literal("=== Utilities ==="), false);
        source.sendSuccess(() -> Component.literal("autototem: " + s.isAutoTotemEnabled()), false);
        source.sendSuccess(() -> Component.literal("totempriority: " + s.isTotemPriority() + " (don't replace totem with shield)"), false);
        source.sendSuccess(() -> Component.literal("autoshield: " + s.isAutoShieldEnabled()), false);
        source.sendSuccess(() -> Component.literal("autopotion: " + s.isAutoPotionEnabled()), false);
        source.sendSuccess(() -> Component.literal("shieldbreak: " + s.isShieldBreakEnabled()), false);
        source.sendSuccess(() -> Component.literal("prefersword: " + s.isPreferSword()), false);
        
        source.sendSuccess(() -> Component.literal("=== Navigation Settings ==="), false);
        source.sendSuccess(() -> Component.literal("bhop: " + s.isBhopEnabled()), false);
        source.sendSuccess(() -> Component.literal("bhopcooldown: " + s.getBhopCooldown() + " ticks"), false);
        source.sendSuccess(() -> Component.literal("jumpboost: " + s.getJumpBoost()), false);
        source.sendSuccess(() -> Component.literal("idle: " + s.isIdleWanderEnabled()), false);
        source.sendSuccess(() -> Component.literal("idleradius: " + s.getIdleWanderRadius()), false);
        source.sendSuccess(() -> Component.literal("=== Factions & Mistakes ==="), false);
        source.sendSuccess(() -> Component.literal("factions: " + s.isFactionsEnabled()), false);
        source.sendSuccess(() -> Component.literal("friendlyfire: " + s.isFriendlyFireEnabled()), false);
        source.sendSuccess(() -> Component.literal("misschance: " + s.getMissChance() + "%"), false);
        source.sendSuccess(() -> Component.literal("mistakechance: " + s.getMistakeChance() + "%"), false);
        source.sendSuccess(() -> Component.literal("reactiondelay: " + s.getReactionDelay() + " ticks"), false);
        return 1;
    }
    

    
    private static int listFactions(CommandSourceStack source) {
        var factions = BotFaction.getAllFactions();
        if (factions.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No factions created"), false);
        } else {
            source.sendSuccess(() -> Component.literal("Factions (" + factions.size() + "):"), false);
            for (String faction : factions) {
                var members = BotFaction.getMembers(faction);
                var enemies = BotFaction.getHostileFactions(faction);
                source.sendSuccess(() -> Component.literal(" - " + faction + " (" + members.size() + " members, " + enemies.size() + " enemies)"), false);
            }
        }
        return factions.size();
    }
    
    private static int createFaction(CommandSourceStack source, String name) {
        if (BotFaction.createFaction(name)) {
            source.sendSuccess(() -> Component.literal("Faction '" + name + "' created!"), true);
            return 1;
        } else {
            source.sendError(Component.literal("Faction '" + name + "' already exists!"));
            return 0;
        }
    }
    
    private static int deleteFaction(CommandSourceStack source, String name) {
        if (BotFaction.deleteFaction(name)) {
            source.sendSuccess(() -> Component.literal("Faction '" + name + "' deleted!"), true);
            return 1;
        } else {
            source.sendError(Component.literal("Faction '" + name + "' not found!"));
            return 0;
        }
    }
    
    private static int addToFaction(CommandSourceStack source, String faction, String player) {
        if (BotFaction.addMember(faction, player)) {
            source.sendSuccess(() -> Component.literal("Added '" + player + "' to faction '" + faction + "'"), true);
            return 1;
        } else {
            source.sendError(Component.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
    }
    
    private static int removeFromFaction(CommandSourceStack source, String faction, String player) {
        if (BotFaction.removeMember(faction, player)) {
            source.sendSuccess(() -> Component.literal("Removed '" + player + "' from faction '" + faction + "'"), true);
            return 1;
        } else {
            source.sendError(Component.literal("Failed to remove '" + player + "' from faction '" + faction + "'"));
            return 0;
        }
    }
    
    private static int setHostile(CommandSourceStack source, String faction1, String faction2, boolean hostile) {
        if (BotFaction.setHostile(faction1, faction2, hostile)) {
            if (hostile) {
                source.sendSuccess(() -> Component.literal("Factions '" + faction1 + "' and '" + faction2 + "' are now hostile!"), true);
            } else {
                source.sendSuccess(() -> Component.literal("Factions '" + faction1 + "' and '" + faction2 + "' are now neutral"), true);
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
        
        source.sendSuccess(() -> Component.literal("=== Faction: " + faction + " ==="), false);
        source.sendSuccess(() -> Component.literal("Members (" + members.size() + "): " + String.join(", ", members)), false);
        source.sendSuccess(() -> Component.literal("Hostile to (" + enemies.size() + "): " + String.join(", ", enemies)), false);
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
        source.sendSuccess(() -> Component.literal("Added " + added + " bots to faction '" + faction + "'"), true);
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
        source.sendSuccess(() -> Component.literal("Added " + added + " bots to faction '" + faction + "'"), true);
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
        source.sendSuccess(() -> Component.literal("Please install InvView to view bot inventories: https://modrinth.com/mod/invview"), false);
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
        source.sendSuccess(() -> Component.literal("Gave items to " + given + " members of faction '" + faction + "'"), true);
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
        source.sendSuccess(() -> Component.literal("Faction '" + faction + "' (" + attacking + " bots) attacking " + targetName + "!"), true);
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
            source.sendSuccess(() -> Component.literal("Kit '" + kitName + "' created from your inventory!"), true);
            return 1;
        } else {
            source.sendError(Component.literal("Failed to create kit (empty inventory?)"));
            return 0;
        }
    }
    
    private static int deleteKit(CommandSourceStack source, String kitName) {
        if (BotKits.deleteKit(kitName)) {
            source.sendSuccess(() -> Component.literal("Kit '" + kitName + "' deleted!"), true);
            return 1;
        } else {
            source.sendError(Component.literal("Kit '" + kitName + "' not found!"));
            return 0;
        }
    }
    
    private static int listKits(CommandSourceStack source) {
        var kits = BotKits.getKitNames();
        if (kits.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No kits created. Use /pvpbot createkit <name> to create one."), false);
        } else {
            source.sendSuccess(() -> Component.literal("Kits (" + kits.size() + "): " + String.join(", ", kits)), false);
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
            source.sendSuccess(() -> Component.literal("Gave kit '" + kitName + "' to '" + playerName + "'"), true);
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
        source.sendSuccess(() -> Component.literal("Gave kit '" + kitName + "' to " + given + " bots in faction '" + factionName + "'"), true);
        return 1;
    }
    
    
    private static int openTestMenu(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayer();
            if (player == null) {
                source.sendError(Component.literal("This command must be run by a player!"));
                return 0;
            }
            

            org.stepan1411.pvp_bot.gui.BotMenuGui.openMainMenu(player);
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
            source.sendSuccess(() -> Component.literal("Statistics sent to server!"), true);
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
        source.sendSuccess(() -> Component.literal("Path visualization for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugPath(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setPathVisualization(botName, enabled);
        source.sendSuccess(() -> Component.literal("Path visualization for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugTarget(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.targetVisualization;
        BotDebug.setTargetVisualization(botName, newValue);
        source.sendSuccess(() -> Component.literal("Target visualization for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugTarget(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setTargetVisualization(botName, enabled);
        source.sendSuccess(() -> Component.literal("Target visualization for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugCombat(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.combatInfo;
        BotDebug.setCombatInfo(botName, newValue);
        source.sendSuccess(() -> Component.literal("Combat info for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugCombat(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setCombatInfo(botName, enabled);
        source.sendSuccess(() -> Component.literal("Combat info for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugNavigation(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.navigationInfo;
        BotDebug.setNavigationInfo(botName, newValue);
        source.sendSuccess(() -> Component.literal("Navigation info for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugNavigation(CommandSourceStack source, String botName, boolean enabled) {
        BotDebug.setNavigationInfo(botName, enabled);
        source.sendSuccess(() -> Component.literal("Navigation info for " + botName + ": " + enabled), true);
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
        source.sendSuccess(() -> Component.literal("All debug modes for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugAll(CommandSourceStack source, String botName, boolean enabled) {
        if (enabled) {
            BotDebug.enableAll(botName);
        } else {
            BotDebug.disableAll(botName);
        }
        source.sendSuccess(() -> Component.literal("All debug modes for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int showDebugStatus(CommandSourceStack source, String botName) {
        var settings = BotDebug.getSettings(botName);
        source.sendSuccess(() -> Component.literal("=== Debug Status for " + botName + " ==="), false);
        source.sendSuccess(() -> Component.literal("Path visualization: " + settings.pathVisualization), false);
        source.sendSuccess(() -> Component.literal("Target visualization: " + settings.targetVisualization), false);
        source.sendSuccess(() -> Component.literal("Combat info: " + settings.combatInfo), false);
        source.sendSuccess(() -> Component.literal("Navigation info: " + settings.navigationInfo), false);
        return 1;
    }
    

    
    private static int createPath(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.createPath(name)) {

            org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, true);
            source.sendSuccess(() -> Component.literal("§aPath '" + name + "' created"), true);
            source.sendSuccess(() -> Component.literal("§7Visualization enabled. To disable: §e/pvpbot path show " + name + " false"), false);
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' already exists"));
            return 0;
        }
    }
    
    private static int deletePath(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.deletePath(name)) {
            source.sendSuccess(() -> Component.literal("§aPath '" + name + "' deleted"), true);
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
                source.sendSuccess(() -> Component.literal("§7Visualization enabled. To disable: §e/pvpbot path show " + name + " false"), false);
            }
            source.sendSuccess(() -> Component.literal(String.format("§aPoint #%d added to path '%s' at (%.1f, %.1f, %.1f)", 
                path.points.size(), name, pos.x, pos.y, pos.z)), true);
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int removeLastPathPoint(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.removeLastPoint(name)) {
            source.sendSuccess(() -> Component.literal("§aLast point removed from path '" + name + "'"), true);
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found or empty"));
            return 0;
        }
    }
    
    private static int removePathPoint(CommandSourceStack source, String name, int index) {
        if (org.stepan1411.pvp_bot.bot.BotPath.removePoint(name, index)) {
            source.sendSuccess(() -> Component.literal("§aPoint #" + index + " removed from path '" + name + "'"), true);
            return 1;
        } else {
            source.sendError(Component.literal("§cInvalid path or index"));
            return 0;
        }
    }
    
    private static int clearPath(CommandSourceStack source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.clearPath(name)) {
            source.sendSuccess(() -> Component.literal("§aAll points cleared from path '" + name + "'"), true);
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int setPathLoop(CommandSourceStack source, String name, boolean loop) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setLoop(name, loop)) {
            source.sendSuccess(() -> Component.literal("§aPath '" + name + "' loop: " + loop), true);
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int setPathAttack(CommandSourceStack source, String name, boolean attack) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setAttack(name, attack)) {
            if (attack) {
                source.sendSuccess(() -> Component.literal("§aPath '" + name + "' attack: enabled"), true);
            } else {
                source.sendSuccess(() -> Component.literal("§aPath '" + name + "' attack: disabled"), true);
                source.sendSuccess(() -> Component.literal("§7Bot will ignore attacks and continue following path"), false);
            }
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int startPathFollowing(CommandSourceStack source, String botName, String pathName) {
        if (org.stepan1411.pvp_bot.bot.BotPath.startFollowing(botName, pathName)) {
            source.sendSuccess(() -> Component.literal("§aBot '" + botName + "' started following path '" + pathName + "'"), true);
            return 1;
        } else {
            source.sendError(Component.literal("§cPath '" + pathName + "' not found or empty"));
            return 0;
        }
    }
    
    private static int stopPathFollowing(CommandSourceStack source, String botName) {
        if (org.stepan1411.pvp_bot.bot.BotPath.stopFollowing(botName)) {
            source.sendSuccess(() -> Component.literal("§aBot '" + botName + "' stopped following path"), true);
            return 1;
        } else {
            source.sendError(Component.literal("§cBot '" + botName + "' is not following any path"));
            return 0;
        }
    }
    
    private static int listPaths(CommandSourceStack source) {
        var paths = org.stepan1411.pvp_bot.bot.BotPath.getAllPaths();
        if (paths.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§eNo paths created"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Paths ==="), false);
        for (var entry : paths.entrySet()) {
            String name = entry.getKey();
            var path = entry.getValue();
            source.sendSuccess(() -> Component.literal(String.format("§e%s§7: %d points, loop: %s, attack: %s", 
                name, path.points.size(), path.loop, path.attack)), false);
        }
        return paths.size();
    }
    
    private static int pathInfo(CommandSourceStack source, String name) {
        var path = org.stepan1411.pvp_bot.bot.BotPath.getPath(name);
        if (path == null) {
            source.sendError(Component.literal("§cPath '" + name + "' not found"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Path: " + name + " ==="), false);
        source.sendSuccess(() -> Component.literal("§7Points: " + path.points.size()), false);
        source.sendSuccess(() -> Component.literal("§7Loop: " + path.loop), false);
        source.sendSuccess(() -> Component.literal("§7Attack: " + path.attack), false);
        
        for (int i = 0; i < path.points.size(); i++) {
            var point = path.points.get(i);
            int index = i;
            source.sendSuccess(() -> Component.literal(String.format("§e#%d§7: (%.1f, %.1f, %.1f)", 
                index, point.x, point.y, point.z)), false);
        }
        
        return 1;
    }
    
    private static int showPath(CommandSourceStack source, String name, boolean visible) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, visible)) {
            if (visible) {
                source.sendSuccess(() -> Component.literal("§aPath '" + name + "' visualization enabled"), true);
                source.sendSuccess(() -> Component.literal("§7To disable: §e/pvpbot path show " + name + " false"), false);
            } else {
                source.sendSuccess(() -> Component.literal("§aPath '" + name + "' visualization disabled"), true);
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
        
        source.sendSuccess(() -> Component.literal("§aDistributed " + botCount + " bots along path '" + pathName + "'"), true);
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
            source.sendSuccess(() -> Component.literal("§aStarted path '" + pathName + "' for " + finalStarted + " bots within " + radius + " blocks"), true);
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
            source.sendSuccess(() -> Component.literal("§aStopped " + finalStopped + " bots on path '" + pathName + "'"), true);
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
            source.sendSuccess(() -> Component.literal("§aStarted path '" + pathName + "' for " + finalStarted + " bots in faction '" + factionName + "'"), true);
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
            source.sendSuccess(() -> Component.literal("§aStopped path for " + finalStopped + " bots in faction '" + factionName + "'"), true);
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
        source.sendSuccess(() -> Component.literal("§aBot '" + botName + "' is now " + mode + " '" + targetName + "'"), true);
        return 1;
    }
    

    private static int stopBotMovement(CommandSourceStack source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("§cBot '" + botName + "' not found"));
            return 0;
        }
        
        BotMovement.stop(botName);
        source.sendSuccess(() -> Component.literal("§aBot '" + botName + "' movement stopped"), true);
        return 1;
    }
    

    private static int setBotGoto(CommandSourceStack source, String botName, double x, double y, double z) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Component.literal("§cBot '" + botName + "' not found"));
            return 0;
        }
        

        Vec3 targetPos = new Vec3(x, y, z);
        BotMovement.setGoto(botName, targetPos);
        
        source.sendSuccess(() -> Component.literal("§aBot '" + botName + "' is moving to " + 
            String.format("%.1f %.1f %.1f", x, y, z)), true);
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
            source.sendSuccess(() -> Component.literal("§a" + finalCount + " bots in faction '" + factionName + "' are now " + mode + " '" + targetName + "'"), true);
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
            source.sendSuccess(() -> Component.literal("§a" + finalCount + " bots in faction '" + factionName + "' are moving to " + 
                String.format("%.1f %.1f %.1f", x, y, z)), true);
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
            
            source.sendSuccess(() -> Component.literal("=== PVP Bot API Debug Info ==="), false);
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    source.sendSuccess(() -> Component.literal(line), false);
                }
            }
            

            var eventManager = PvpBotAPI.getEventManager();
            var strategyRegistry = PvpBotAPI.getCombatStrategyRegistry();
            
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("=== Detailed Event Handler Info ==="), false);
            source.sendSuccess(() -> Component.literal("Spawn handlers: " + eventManager.getSpawnHandlerCount()), false);
            source.sendSuccess(() -> Component.literal("Death handlers: " + eventManager.getDeathHandlerCount()), false);
            source.sendSuccess(() -> Component.literal("Attack handlers: " + eventManager.getAttackHandlerCount()), false);
            source.sendSuccess(() -> Component.literal("Damage handlers: " + eventManager.getDamageHandlerCount()), false);
            source.sendSuccess(() -> Component.literal("Tick handlers: " + eventManager.getTickHandlerCount()), false);
            
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("=== Combat Strategy Details ==="), false);
            String strategyDebugInfo = strategyRegistry.getDebugInfo();
            String[] strategyLines = strategyDebugInfo.split("\n");
            for (String line : strategyLines) {
                if (!line.trim().isEmpty()) {
                    source.sendSuccess(() -> Component.literal(line), false);
                }
            }
            
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("API Status: " + (PvpBotAPI.isInitialized() ? "Initialized" : "Not Initialized")), false);
            
            return 1;
        } catch (Exception e) {
            source.sendError(Component.literal("Failed to get API debug info: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
