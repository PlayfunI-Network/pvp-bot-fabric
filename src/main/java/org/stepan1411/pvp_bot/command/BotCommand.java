package org.stepan1411.pvp_bot.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.stepan1411.pvp_bot.bot.BotCombat;
import org.stepan1411.pvp_bot.bot.BotDebug;
import org.stepan1411.pvp_bot.bot.BotFaction;
import org.stepan1411.pvp_bot.bot.BotKits;
import org.stepan1411.pvp_bot.bot.BotManager;
import org.stepan1411.pvp_bot.bot.BotNameGenerator;
import org.stepan1411.pvp_bot.bot.BotPath;
import org.stepan1411.pvp_bot.bot.BotSettings;
import org.stepan1411.pvp_bot.gui.SettingsGui;
import org.stepan1411.pvp_bot.stats.StatsReporter;

import java.lang.reflect.Method;
import java.util.stream.Collectors;

public class BotCommand {
    
    // РџСЂРѕРІРµСЂРєР° РЅР°Р»РёС‡РёСЏ InvView
    private static final boolean HAS_INVVIEW = FabricLoader.getInstance().isModLoaded("invview");
    
    // РџРѕРґСЃРєР°Р·РєРё РґР»СЏ РёРјС‘РЅ Р±РѕС‚РѕРІ
    private static final SuggestionProvider<ServerCommandSource> BOT_SUGGESTIONS = (ctx, builder) -> 
        CommandSource.suggestMatching(BotManager.getAllBots(), builder);
    
    // РџРѕРґСЃРєР°Р·РєРё РґР»СЏ С†РµР»РµР№ (РІСЃРµ РёРіСЂРѕРєРё РЅР° СЃРµСЂРІРµСЂРµ)
    private static final SuggestionProvider<ServerCommandSource> TARGET_SUGGESTIONS = (ctx, builder) -> 
        CommandSource.suggestMatching(
            ctx.getSource().getServer().getPlayerManager().getPlayerList().stream()
                .map(p -> p.getName().getString())
                .collect(Collectors.toList()), 
            builder);
    
    // РџРѕРґСЃРєР°Р·РєРё РґР»СЏ РІСЃРµС… РёРіСЂРѕРєРѕРІ (Р±РѕС‚С‹ + РёРіСЂРѕРєРё)
    private static final SuggestionProvider<ServerCommandSource> PLAYER_SUGGESTIONS = TARGET_SUGGESTIONS;
    
    // РџРѕРґСЃРєР°Р·РєРё РґР»СЏ С„СЂР°РєС†РёР№
    private static final SuggestionProvider<ServerCommandSource> FACTION_SUGGESTIONS = (ctx, builder) -> 
        CommandSource.suggestMatching(BotFaction.getAllFactions(), builder);
    
    // РџРѕРґСЃРєР°Р·РєРё РґР»СЏ РєРёС‚РѕРІ
    private static final SuggestionProvider<ServerCommandSource> KIT_SUGGESTIONS = (ctx, builder) -> 
        CommandSource.suggestMatching(BotKits.getKitNames(), builder);
    
    // РџРѕРґСЃРєР°Р·РєРё РґР»СЏ РїСѓС‚РµР№
    private static final SuggestionProvider<ServerCommandSource> PATH_SUGGESTIONS = (ctx, builder) -> 
        CommandSource.suggestMatching(BotPath.getAllPaths().keySet(), builder);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("pvpbot")
                
                // /pvpbot spawn [name] - Р±РµР· РёРјРµРЅРё РіРµРЅРµСЂРёСЂСѓРµС‚ СЃР»СѓС‡Р°Р№РЅРѕРµ
                .then(CommandManager.literal("spawn")
                    .executes(ctx -> spawnBot(ctx.getSource(), BotNameGenerator.generateUniqueName()))
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> spawnBot(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                
                // /pvpbot massspawn <num> - СЃРїР°РІРЅРёС‚ РЅРµСЃРєРѕР»СЊРєРѕ Р±РѕС‚РѕРІ СЃ СЂР°РЅРґРѕРјРЅС‹РјРё РёРјРµРЅР°РјРё
                .then(CommandManager.literal("massspawn")
                    .then(CommandManager.argument("count", IntegerArgumentType.integer(1, 50))
                        .executes(ctx -> massSpawnBots(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "count")))
                    )
                )
                
                // /pvpbot remove <name> - СЃ РїРѕРґСЃРєР°Р·РєР°РјРё Р±РѕС‚РѕРІ
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> removeBot(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                
                // /pvpbot removeall
                .then(CommandManager.literal("removeall")
                    .executes(ctx -> removeAllBots(ctx.getSource()))
                )
                
                // /pvpbot list
                .then(CommandManager.literal("list")
                    .executes(ctx -> listBots(ctx.getSource()))
                )
                
                // /pvpbot sync [name] - СЃРёРЅС…СЂРѕРЅРёР·РёСЂРѕРІР°С‚СЊ СЃРїРёСЃРѕРє Р±РѕС‚РѕРІ СЃ СЃРµСЂРІРµСЂРѕРј
                .then(CommandManager.literal("sync")
                    .executes(ctx -> syncBots(ctx.getSource()))
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .executes(ctx -> syncBot(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                
                // /pvpbot debug - СЃРёСЃС‚РµРјР° РѕС‚Р»Р°РґРєРё
                .then(CommandManager.literal("debug")
                    .then(CommandManager.argument("bot", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        
                        // /pvpbot debug <bot> path [true/false]
                        .then(CommandManager.literal("path")
                            .executes(ctx -> toggleDebugPath(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugPath(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        
                        // /pvpbot debug <bot> target [true/false]
                        .then(CommandManager.literal("target")
                            .executes(ctx -> toggleDebugTarget(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugTarget(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        
                        // /pvpbot debug <bot> combat [true/false]
                        .then(CommandManager.literal("combat")
                            .executes(ctx -> toggleDebugCombat(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugCombat(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        
                        // /pvpbot debug <bot> navigation [true/false]
                        .then(CommandManager.literal("navigation")
                            .executes(ctx -> toggleDebugNavigation(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugNavigation(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        
                        // /pvpbot debug <bot> all [true/false]
                        .then(CommandManager.literal("all")
                            .executes(ctx -> toggleDebugAll(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                            .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> setDebugAll(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), BoolArgumentType.getBool(ctx, "enabled")))
                            )
                        )
                        
                        // /pvpbot debug <bot> status
                        .then(CommandManager.literal("status")
                            .executes(ctx -> showDebugStatus(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                        )
                    )
                )
                
                // /pvpbot menu - РѕС‚РєСЂС‹С‚СЊ С‚РµСЃС‚РѕРІРѕРµ РјРµРЅСЋ
                .then(CommandManager.literal("menu")
                    .executes(ctx -> openTestMenu(ctx.getSource()))
                )
                
                // /pvpbot settings
                .then(CommandManager.literal("settings")
                    .executes(ctx -> showSettings(ctx.getSource()))
                    
                    // /pvpbot settings gui - РѕС‚РєСЂС‹С‚СЊ GUI РЅР°СЃС‚СЂРѕРµРє
                    .then(CommandManager.literal("gui")
                        .executes(ctx -> openSettingsGui(ctx.getSource()))
                    )
                    
                    // /pvpbot settings autoarmor [true/false]
                    .then(CommandManager.literal("autoarmor")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("autoarmor: " + BotSettings.get().isAutoEquipArmor()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEquipArmor(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto equip armor: " + BotSettings.get().isAutoEquipArmor()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings autoweapon [true/false]
                    .then(CommandManager.literal("autoweapon")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("autoweapon: " + BotSettings.get().isAutoEquipWeapon()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEquipWeapon(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto equip weapon: " + BotSettings.get().isAutoEquipWeapon()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings droparmor [true/false]
                    .then(CommandManager.literal("droparmor")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("droparmor: " + BotSettings.get().isDropWorseArmor()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setDropWorseArmor(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Drop worse armor: " + BotSettings.get().isDropWorseArmor()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings dropweapon [true/false]
                    .then(CommandManager.literal("dropweapon")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("dropweapon: " + BotSettings.get().isDropWorseWeapons()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setDropWorseWeapons(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Drop worse weapons: " + BotSettings.get().isDropWorseWeapons()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings dropdistance [1-10]
                    .then(CommandManager.literal("dropdistance")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("dropdistance: " + BotSettings.get().getDropDistance()), false); return 1; })
                        .then(CommandManager.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                            .executes(ctx -> {
                                BotSettings.get().setDropDistance(DoubleArgumentType.getDouble(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Drop distance: " + BotSettings.get().getDropDistance()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings interval [1-100]
                    .then(CommandManager.literal("interval")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("interval: " + BotSettings.get().getCheckInterval() + " ticks"), false); return 1; })
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 100))
                            .executes(ctx -> {
                                BotSettings.get().setCheckInterval(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Check interval: " + BotSettings.get().getCheckInterval() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings minarmorlevel [0-100]
                    .then(CommandManager.literal("minarmorlevel")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("minarmorlevel: " + BotSettings.get().getMinArmorLevel()), false); return 1; })
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMinArmorLevel(IntegerArgumentType.getInteger(ctx, "level"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Min armor level: " + BotSettings.get().getMinArmorLevel() + " (0=any, 20=leather+, 40=gold+, 50=chain+, 60=iron+, 80=diamond+, 100=netherite)"), true);
                                return 1;
                            })
                        )
                    )
                    
                    // === Combat Settings (СЃ РїРѕРєР°Р·РѕРј С‚РµРєСѓС‰РµРіРѕ Р·РЅР°С‡РµРЅРёСЏ Р±РµР· Р°СЂРіСѓРјРµРЅС‚Р°) ===
                    .then(CommandManager.literal("combat")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("combat: " + BotSettings.get().isCombatEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setCombatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Combat enabled: " + BotSettings.get().isCombatEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("revenge")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("revenge: " + BotSettings.get().isRevengeEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRevengeEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Revenge mode: " + BotSettings.get().isRevengeEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("autotarget")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("autotarget: " + BotSettings.get().isAutoTargetEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoTargetEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto target: " + BotSettings.get().isAutoTargetEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("targetplayers")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("targetplayers: " + BotSettings.get().isTargetPlayers()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetPlayers(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Target players: " + BotSettings.get().isTargetPlayers()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("targetmobs")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("targetmobs: " + BotSettings.get().isTargetHostileMobs()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetHostileMobs(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Target hostile mobs: " + BotSettings.get().isTargetHostileMobs()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("targetbots")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("targetbots: " + BotSettings.get().isTargetOtherBots()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTargetOtherBots(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Target other bots: " + BotSettings.get().isTargetOtherBots()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("criticals")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("criticals: " + BotSettings.get().isCriticalsEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setCriticalsEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Criticals: " + BotSettings.get().isCriticalsEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("ranged")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("ranged: " + BotSettings.get().isRangedEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRangedEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Ranged weapons: " + BotSettings.get().isRangedEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("mace")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("mace: " + BotSettings.get().isMaceEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setMaceEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Mace combat: " + BotSettings.get().isMaceEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("attackcooldown")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("attackcooldown: " + BotSettings.get().getAttackCooldown() + " ticks"), false); return 1; })
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(1, 40))
                            .executes(ctx -> {
                                BotSettings.get().setAttackCooldown(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Attack cooldown: " + BotSettings.get().getAttackCooldown() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("meleerange")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("meleerange: " + BotSettings.get().getMeleeRange()), false); return 1; })
                        .then(CommandManager.argument("range", DoubleArgumentType.doubleArg(2.0, 6.0))
                            .executes(ctx -> {
                                BotSettings.get().setMeleeRange(DoubleArgumentType.getDouble(ctx, "range"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Melee range: " + BotSettings.get().getMeleeRange()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("movespeed")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("movespeed: " + BotSettings.get().getMoveSpeed()), false); return 1; })
                        .then(CommandManager.argument("speed", DoubleArgumentType.doubleArg(0.1, 2.0))
                            .executes(ctx -> {
                                BotSettings.get().setMoveSpeed(DoubleArgumentType.getDouble(ctx, "speed"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Move speed: " + BotSettings.get().getMoveSpeed()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // === Navigation Settings ===
                    .then(CommandManager.literal("bhop")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("bhop: " + BotSettings.get().isBhopEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setBhopEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Bhop enabled: " + BotSettings.get().isBhopEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("bhopcooldown")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("bhopcooldown: " + BotSettings.get().getBhopCooldown() + " ticks"), false); return 1; })
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(5, 30))
                            .executes(ctx -> {
                                BotSettings.get().setBhopCooldown(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Bhop cooldown: " + BotSettings.get().getBhopCooldown() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("jumpboost")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("jumpboost: " + BotSettings.get().getJumpBoost()), false); return 1; })
                        .then(CommandManager.argument("boost", DoubleArgumentType.doubleArg(0.0, 0.5))
                            .executes(ctx -> {
                                BotSettings.get().setJumpBoost(DoubleArgumentType.getDouble(ctx, "boost"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Jump boost: " + BotSettings.get().getJumpBoost()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("idle")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("idle: " + BotSettings.get().isIdleWanderEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setIdleWanderEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Idle wander: " + BotSettings.get().isIdleWanderEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("idleradius")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("idleradius: " + BotSettings.get().getIdleWanderRadius()), false); return 1; })
                        .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(3.0, 50.0))
                            .executes(ctx -> {
                                BotSettings.get().setIdleWanderRadius(DoubleArgumentType.getDouble(ctx, "radius"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Idle wander radius: " + BotSettings.get().getIdleWanderRadius()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // === Realism Settings ===
                    .then(CommandManager.literal("friendlyfire")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("friendlyfire: " + BotSettings.get().isFriendlyFireEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setFriendlyFireEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Friendly fire: " + BotSettings.get().isFriendlyFireEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("misschance")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("misschance: " + BotSettings.get().getMissChance() + "%"), false); return 1; })
                        .then(CommandManager.argument("percent", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMissChance(IntegerArgumentType.getInteger(ctx, "percent"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Miss chance: " + BotSettings.get().getMissChance() + "%"), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("mistakechance")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("mistakechance: " + BotSettings.get().getMistakeChance() + "%"), false); return 1; })
                        .then(CommandManager.argument("percent", IntegerArgumentType.integer(0, 100))
                            .executes(ctx -> {
                                BotSettings.get().setMistakeChance(IntegerArgumentType.getInteger(ctx, "percent"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Mistake chance: " + BotSettings.get().getMistakeChance() + "%"), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("reactiondelay")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("reactiondelay: " + BotSettings.get().getReactionDelay() + " ticks"), false); return 1; })
                        .then(CommandManager.argument("ticks", IntegerArgumentType.integer(0, 20))
                            .executes(ctx -> {
                                BotSettings.get().setReactionDelay(IntegerArgumentType.getInteger(ctx, "ticks"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Reaction delay: " + BotSettings.get().getReactionDelay() + " ticks"), true);
                                return 1;
                            })
                        )
                    )
                    
                    // === Weapon Settings ===
                    .then(CommandManager.literal("prefersword")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("prefersword: " + BotSettings.get().isPreferSword()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setPreferSword(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Prefer sword: " + BotSettings.get().isPreferSword()), true);
                                return 1;
                            })
                        )
                    )
                    .then(CommandManager.literal("shieldbreak")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("shieldbreak: " + BotSettings.get().isShieldBreakEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setShieldBreakEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Shield break: " + BotSettings.get().isShieldBreakEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings autoshield [true/false]
                    .then(CommandManager.literal("autoshield")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("autoshield: " + BotSettings.get().isAutoShieldEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoShieldEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto shield: " + BotSettings.get().isAutoShieldEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings autopotion [true/false]
                    .then(CommandManager.literal("autopotion")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("autopotion: " + BotSettings.get().isAutoPotionEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoPotionEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto potion: " + BotSettings.get().isAutoPotionEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings autototem [true/false]
                    .then(CommandManager.literal("autototem")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("autototem: " + BotSettings.get().isAutoTotemEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoTotemEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto totem: " + BotSettings.get().isAutoTotemEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings totempriority [true/false]
                    .then(CommandManager.literal("totempriority")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("totempriority: " + BotSettings.get().isTotemPriority()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setTotemPriority(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Totem priority (don't replace with shield): " + BotSettings.get().isTotemPriority()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings retreat [true/false]
                    .then(CommandManager.literal("retreat")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("retreat: " + BotSettings.get().isRetreatEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setRetreatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Retreat enabled: " + BotSettings.get().isRetreatEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings autoeat [true/false]
                    .then(CommandManager.literal("autoeat")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("autoeat: " + BotSettings.get().isAutoEatEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoEatEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto eat enabled: " + BotSettings.get().isAutoEatEnabled()), true);
                                return 1;
                            })
                        )
                    )
                    
                    // /pvpbot settings automend [true/false]
                    .then(CommandManager.literal("automend")
                        .executes(ctx -> { ctx.getSource().sendFeedback(() -> Text.literal("automend: " + BotSettings.get().isAutoMendEnabled()), false); return 1; })
                        .then(CommandManager.argument("value", BoolArgumentType.bool())
                            .executes(ctx -> {
                                BotSettings.get().setAutoMendEnabled(BoolArgumentType.getBool(ctx, "value"));
                                ctx.getSource().sendFeedback(() -> Text.literal("Auto mend enabled: " + BotSettings.get().isAutoMendEnabled()), true);
                                return 1;
                            })
                        )
                    )
                )
                
                // /pvpbot attack <botname> <target> - СЃ РїРѕРґСЃРєР°Р·РєР°РјРё
                .then(CommandManager.literal("attack")
                    .then(CommandManager.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .then(CommandManager.argument("target", StringArgumentType.word())
                            .suggests(TARGET_SUGGESTIONS)
                            .executes(ctx -> setAttackTarget(ctx.getSource(), 
                                StringArgumentType.getString(ctx, "botname"),
                                StringArgumentType.getString(ctx, "target")))
                        )
                    )
                )
                
                // /pvpbot stop <botname> - СЃ РїРѕРґСЃРєР°Р·РєР°РјРё
                .then(CommandManager.literal("stop")
                    .then(CommandManager.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> stopAttack(ctx.getSource(), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                
                // /pvpbot target <botname> - РїРѕРєР°Р·Р°С‚СЊ С‚РµРєСѓС‰СѓСЋ С†РµР»СЊ
                .then(CommandManager.literal("target")
                    .then(CommandManager.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> showTarget(ctx.getSource(), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                
                // /pvpbot inventory <botname> - РїРѕРєР°Р·Р°С‚СЊ РёРЅРІРµРЅС‚Р°СЂСЊ Р±РѕС‚Р°
                .then(CommandManager.literal("inventory")
                    .then(CommandManager.argument("botname", StringArgumentType.word())
                        .suggests(BOT_SUGGESTIONS)
                        .executes(ctx -> showInventory(ctx.getSource(), StringArgumentType.getString(ctx, "botname")))
                    )
                )
                
                // ============ РљРѕРјР°РЅРґС‹ С„СЂР°РєС†РёР№ ============
                .then(CommandManager.literal("faction")
                    // /pvpbot faction list
                    .then(CommandManager.literal("list")
                        .executes(ctx -> listFactions(ctx.getSource()))
                    )
                    // /pvpbot faction create <name>
                    .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> createFaction(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    // /pvpbot faction delete <name>
                    .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> deleteFaction(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    // /pvpbot faction add <faction> <player>
                    .then(CommandManager.literal("add")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> addToFaction(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "player")))
                            )
                        )
                    )
                    // /pvpbot faction remove <faction> <player>
                    .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(ctx -> removeFromFaction(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "player")))
                            )
                        )
                    )
                    // /pvpbot faction hostile <faction1> <faction2> [true/false]
                    .then(CommandManager.literal("hostile")
                        .then(CommandManager.argument("faction1", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("faction2", StringArgumentType.word())
                                .suggests(FACTION_SUGGESTIONS)
                                .executes(ctx -> setHostile(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction1"),
                                    StringArgumentType.getString(ctx, "faction2"),
                                    true))
                                .then(CommandManager.argument("hostile", BoolArgumentType.bool())
                                    .executes(ctx -> setHostile(ctx.getSource(), 
                                        StringArgumentType.getString(ctx, "faction1"),
                                        StringArgumentType.getString(ctx, "faction2"),
                                        BoolArgumentType.getBool(ctx, "hostile")))
                                )
                            )
                        )
                    )
                    // /pvpbot faction info <faction>
                    .then(CommandManager.literal("info")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> factionInfo(ctx.getSource(), StringArgumentType.getString(ctx, "faction")))
                        )
                    )
                    // /pvpbot faction addnear <faction> <radius> - РґРѕР±Р°РІРёС‚СЊ РІСЃРµС… Р±РѕС‚РѕРІ РІ СЂР°РґРёСѓСЃРµ
                    .then(CommandManager.literal("addnear")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(1.0, 10000.0))
                                .executes(ctx -> addNearbyBotsToFaction(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    DoubleArgumentType.getDouble(ctx, "radius")))
                            )
                        )
                    )
                    // /pvpbot faction addall <faction> - РґРѕР±Р°РІРёС‚СЊ Р’РЎР•РҐ Р±РѕС‚РѕРІ РІ С„СЂР°РєС†РёСЋ
                    .then(CommandManager.literal("addall")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> addAllBotsToFaction(ctx.getSource(), 
                                StringArgumentType.getString(ctx, "faction")))
                        )
                    )
                    // /pvpbot faction give <faction> <item> [count] - РІС‹РґР°С‚СЊ РїСЂРµРґРјРµС‚ РІСЃРµР№ С„СЂР°РєС†РёРё
                    .then(CommandManager.literal("give")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("item", StringArgumentType.greedyString())
                                .executes(ctx -> giveFactionItem(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "item")))
                            )
                        )
                    )
                    // /pvpbot faction attack <faction> <target> - РІСЃСЏ С„СЂР°РєС†РёСЏ Р°С‚Р°РєСѓРµС‚ С†РµР»СЊ
                    .then(CommandManager.literal("attack")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("target", StringArgumentType.word())
                                .suggests(TARGET_SUGGESTIONS)
                                .executes(ctx -> factionAttack(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "target")))
                            )
                        )
                    )
                    // /pvpbot faction startpath <faction> <path> - запустить путь для всей фракции
                    .then(CommandManager.literal("startpath")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("path", StringArgumentType.word())
                                .suggests(PATH_SUGGESTIONS)
                                .executes(ctx -> factionStartPath(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )
                    // /pvpbot faction stoppath <faction> - остановить путь для всей фракции
                    .then(CommandManager.literal("stoppath")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .executes(ctx -> factionStopPath(ctx.getSource(), 
                                StringArgumentType.getString(ctx, "faction")))
                        )
                    )
                )
                
                // /pvpbot settings viewdistance [5-128] - РґР°Р»СЊРЅРѕСЃС‚СЊ РІРёРґРёРјРѕСЃС‚Рё
                .then(CommandManager.literal("settings")
                    .then(CommandManager.literal("viewdistance")
                        .executes(ctx -> { 
                            ctx.getSource().sendFeedback(() -> Text.literal("viewdistance: " + BotSettings.get().getMaxTargetDistance()), false); 
                            return 1; 
                        })
                        .then(CommandManager.argument("distance", DoubleArgumentType.doubleArg(5.0, 128.0))
                            .executes(ctx -> {
                                BotSettings.get().setMaxTargetDistance(DoubleArgumentType.getDouble(ctx, "distance"));
                                ctx.getSource().sendFeedback(() -> Text.literal("View distance: " + BotSettings.get().getMaxTargetDistance()), true);
                                return 1;
                            })
                        )
                    )
                )
                
                // ============ РљРѕРјР°РЅРґС‹ РєРёС‚РѕРІ ============
                // /pvpbot createkit <name> - СЃРѕР·РґР°С‚СЊ РєРёС‚ РёР· РёРЅРІРµРЅС‚Р°СЂСЏ РёРіСЂРѕРєР°
                .then(CommandManager.literal("createkit")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .executes(ctx -> createKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                
                // /pvpbot deletekit <name> - СѓРґР°Р»РёС‚СЊ РєРёС‚
                .then(CommandManager.literal("deletekit")
                    .then(CommandManager.argument("name", StringArgumentType.word())
                        .suggests(KIT_SUGGESTIONS)
                        .executes(ctx -> deleteKit(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                    )
                )
                
                // /pvpbot kits - СЃРїРёСЃРѕРє РєРёС‚РѕРІ
                .then(CommandManager.literal("kits")
                    .executes(ctx -> listKits(ctx.getSource()))
                )
                
                // /pvpbot givekit <playername> <kitname> - РІС‹РґР°С‚СЊ РєРёС‚ РёРіСЂРѕРєСѓ РёР»Рё Р±РѕС‚Сѓ
                .then(CommandManager.literal("givekit")
                    .then(CommandManager.argument("playername", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS)
                        .then(CommandManager.argument("kitname", StringArgumentType.word())
                            .suggests(KIT_SUGGESTIONS)
                            .executes(ctx -> giveKitToPlayer(ctx.getSource(), 
                                StringArgumentType.getString(ctx, "playername"),
                                StringArgumentType.getString(ctx, "kitname")))
                        )
                    )
                )
                
                // /pvpbot faction givekit <faction> <kitname> - РІС‹РґР°С‚СЊ РєРёС‚ РІСЃРµР№ С„СЂР°РєС†РёРё
                .then(CommandManager.literal("faction")
                    .then(CommandManager.literal("givekit")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                            .suggests(FACTION_SUGGESTIONS)
                            .then(CommandManager.argument("kitname", StringArgumentType.word())
                                .suggests(KIT_SUGGESTIONS)
                                .executes(ctx -> giveKitToFaction(ctx.getSource(), 
                                    StringArgumentType.getString(ctx, "faction"),
                                    StringArgumentType.getString(ctx, "kitname")))
                            )
                        )
                    )
                )
                
                // /pvpbot updatestats - РѕС‚РїСЂР°РІРёС‚СЊ СЃС‚Р°С‚РёСЃС‚РёРєСѓ СЃРµР№С‡Р°СЃ (РґР»СЏ РѕС‚Р»Р°РґРєРё)
                
                // /pvpbot path - система путей для патрулирования
                .then(CommandManager.literal("path")
                    .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> createPath(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(CommandManager.literal("delete")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> deletePath(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(CommandManager.literal("addpoint")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> addPathPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(CommandManager.literal("removepoint")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> removeLastPathPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                            .then(CommandManager.argument("index", IntegerArgumentType.integer(0))
                                .executes(ctx -> removePathPoint(ctx.getSource(), StringArgumentType.getString(ctx, "name"), IntegerArgumentType.getInteger(ctx, "index")))
                            )
                        )
                    )
                    .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> clearPath(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    .then(CommandManager.literal("loop")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPathLoop(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "value")))
                            )
                        )
                    )
                    .then(CommandManager.literal("attack")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(CommandManager.argument("value", BoolArgumentType.bool())
                                .executes(ctx -> setPathAttack(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "value")))
                            )
                        )
                    )
                    .then(CommandManager.literal("start")
                        .then(CommandManager.argument("bot", StringArgumentType.word())
                            .suggests(BOT_SUGGESTIONS)
                            .then(CommandManager.argument("path", StringArgumentType.word())
                                .suggests(PATH_SUGGESTIONS)
                                .executes(ctx -> startPathFollowing(ctx.getSource(), StringArgumentType.getString(ctx, "bot"), StringArgumentType.getString(ctx, "path")))
                            )
                        )
                    )
                    .then(CommandManager.literal("stop")
                        .then(CommandManager.argument("bot", StringArgumentType.word())
                            .suggests(BOT_SUGGESTIONS)
                            .executes(ctx -> stopPathFollowing(ctx.getSource(), StringArgumentType.getString(ctx, "bot")))
                        )
                    )
                    .then(CommandManager.literal("list")
                        .executes(ctx -> listPaths(ctx.getSource()))
                    )
                    .then(CommandManager.literal("show")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(CommandManager.argument("visible", BoolArgumentType.bool())
                                .executes(ctx -> showPath(ctx.getSource(), StringArgumentType.getString(ctx, "name"), BoolArgumentType.getBool(ctx, "visible")))
                            )
                        )
                    )
                    .then(CommandManager.literal("info")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> pathInfo(ctx.getSource(), StringArgumentType.getString(ctx, "name")))
                        )
                    )
                    // /pvpbot path distribute <path> - распределить ботов по пути
                    .then(CommandManager.literal("distribute")
                        .then(CommandManager.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> distributeBotsOnPath(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                        )
                    )
                    // /pvpbot path startnear <path> <radius> - запустить путь для ботов в радиусе
                    .then(CommandManager.literal("startnear")
                        .then(CommandManager.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .then(CommandManager.argument("radius", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> startPathNear(ctx.getSource(), StringArgumentType.getString(ctx, "path"), DoubleArgumentType.getDouble(ctx, "radius")))
                            )
                        )
                    )
                    // /pvpbot path stopall <path> - остановить всех ботов на пути
                    .then(CommandManager.literal("stopall")
                        .then(CommandManager.argument("path", StringArgumentType.word())
                            .suggests(PATH_SUGGESTIONS)
                            .executes(ctx -> stopAllOnPath(ctx.getSource(), StringArgumentType.getString(ctx, "path")))
                        )
                    )
                )
                .then(CommandManager.literal("updatestats")
                    .executes(ctx -> updateStats(ctx.getSource()))
                )
        );
    }
    
    private static int setAttackTarget(ServerCommandSource source, String botName, String targetName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Text.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        BotCombat.setTarget(botName, targetName);
        source.sendFeedback(() -> Text.literal("Bot '" + botName + "' now attacking '" + targetName + "'"), true);
        return 1;
    }
    
    private static int stopAttack(ServerCommandSource source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Text.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        BotCombat.clearTarget(botName);
        source.sendFeedback(() -> Text.literal("Bot '" + botName + "' stopped attacking"), true);
        return 1;
    }
    
    private static int showTarget(ServerCommandSource source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Text.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        var target = BotCombat.getTarget(botName);
        if (target != null) {
            source.sendFeedback(() -> Text.literal("Bot '" + botName + "' target: " + target.getName().getString()), false);
        } else {
            source.sendFeedback(() -> Text.literal("Bot '" + botName + "' has no target"), false);
        }
        return 1;
    }


    private static int spawnBot(ServerCommandSource source, String name) {
        // РџСЂРѕРІРµСЂСЏРµРј СЃСѓС‰РµСЃС‚РІСѓРµС‚ Р»Рё СЂРµР°Р»СЊРЅС‹Р№ РёРіСЂРѕРє СЃ С‚Р°РєРёРј РЅРёРєРѕРј
        var server = source.getServer();
        var existingPlayer = server.getPlayerManager().getPlayer(name);
        if (existingPlayer != null && !BotManager.getAllBots().contains(name)) {
            source.sendError(Text.literal("Cannot create bot '" + name + "': a real player with this name is online!"));
            return 0;
        }
        
        if (BotManager.spawnBot(server, name, source)) {
            source.sendFeedback(() -> Text.literal("PvP Bot '" + name + "' spawned!"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to spawn bot '" + name + "' (bot already exists or name is taken)"));
            return 0;
        }
    }
    
    private static int massSpawnBots(ServerCommandSource source, int count) {
        var server = source.getServer();
        final int[] spawned = {0};
        final int[] current = {0};
        
        source.sendFeedback(() -> Text.literal("Spawning " + count + " bots "), false);
        
        // РЎРїР°РІРЅРёРј Р±РѕС‚РѕРІ СЃ Р·Р°РґРµСЂР¶РєРѕР№ С‡РµСЂРµР· scheduled tasks
        scheduleSpawn(server, source, count, spawned, current);
        
        return 1;
    }
    
    private static void scheduleSpawn(net.minecraft.server.MinecraftServer server, ServerCommandSource source, int total, int[] spawned, int[] current) {
        if (current[0] >= total) {
            source.sendFeedback(() -> Text.literal("Finished! Spawned " + spawned[0] + " bots."), true);
            return;
        }
        
        // РЎРїР°РІРЅРёРј РѕРґРЅРѕРіРѕ Р±РѕС‚Р°
        String name = BotNameGenerator.generateUniqueName();
        if (BotManager.spawnBot(server, name, source)) {
            spawned[0]++;
        }
        current[0]++;
        
        // РџР»Р°РЅРёСЂСѓРµРј СЃР»РµРґСѓСЋС‰РёР№ СЃРїР°РІРЅ С‡РµСЂРµР· 5 С‚РёРєРѕРІ
        server.execute(() -> {
            // РСЃРїРѕР»СЊР·СѓРµРј РїСЂРѕСЃС‚СѓСЋ Р·Р°РґРµСЂР¶РєСѓ С‡РµСЂРµР· С‚РёРєРё СЃРµСЂРІРµСЂР°
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

    private static int removeBot(ServerCommandSource source, String name) {
        if (BotManager.removeBot(source.getServer(), name, source)) {
            source.sendFeedback(() -> Text.literal("Bot '" + name + "' removed!"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Bot '" + name + "' not found!"));
            return 0;
        }
    }

    private static int removeAllBots(ServerCommandSource source) {
        int count = BotManager.getBotCount();
        BotManager.removeAllBots(source.getServer(), source);
        source.sendFeedback(() -> Text.literal("Removed " + count + " bots"), true);
        return count;
    }

    private static int listBots(ServerCommandSource source) {
        var bots = BotManager.getAllBots();
        
        if (bots.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No active PvP bots"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Active PvP bots (" + bots.size() + "):"), false);
            for (String botName : bots) {
                source.sendFeedback(() -> Text.literal(" - " + botName), false);
            }
        }
        return bots.size();
    }
    
    private static int syncBots(ServerCommandSource source) {
        var server = source.getServer();
        int beforeCount = BotManager.getAllBots().size();
        
        // РџРѕРєР°Р·С‹РІР°РµРј РІСЃРµС… РёРіСЂРѕРєРѕРІ Рё РёС… РєР»Р°СЃСЃС‹ РґР»СЏ РѕС‚Р»Р°РґРєРё
        source.sendFeedback(() -> Text.literal("=== Players on server ==="), false);
        for (var player : server.getPlayerManager().getPlayerList()) {
            String name = player.getName().getString();
            String className = player.getClass().getName();
            boolean inList = BotManager.getAllBots().contains(name);
            source.sendFeedback(() -> Text.literal(" - " + name + " [" + className + "] " + (inList ? "(in list)" : "(NOT in list)")), false);
        }
        
        // РЎРёРЅС…СЂРѕРЅРёР·РёСЂСѓРµРј
        BotManager.syncBots(server);
        
        int afterCount = BotManager.getAllBots().size();
        int added = afterCount - beforeCount;
        
        source.sendFeedback(() -> Text.literal("Synced! Added " + added + " bots. Total: " + afterCount), true);
        return added;
    }
    
    private static int syncBot(ServerCommandSource source, String name) {
        var server = source.getServer();
        
        // РџСЂРѕРІРµСЂСЏРµРј РµСЃС‚СЊ Р»Рё С‚Р°РєРѕР№ РёРіСЂРѕРє
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(name);
        if (player == null) {
            source.sendFeedback(() -> Text.literal("Player " + name + " not found on server!"), false);
            return 0;
        }
        
        // РџРѕРєР°Р·С‹РІР°РµРј РёРЅС„РѕСЂРјР°С†РёСЋ РѕР± РёРіСЂРѕРєРµ
        String className = player.getClass().getName();
        boolean inList = BotManager.getAllBots().contains(name);
        source.sendFeedback(() -> Text.literal("Player: " + name), false);
        source.sendFeedback(() -> Text.literal("Class: " + className), false);
        source.sendFeedback(() -> Text.literal("In bot list: " + inList), false);
        
        // РЎРёРЅС…СЂРѕРЅРёР·РёСЂСѓРµРј
        boolean added = BotManager.syncBot(server, name);
        
        if (added) {
            source.sendFeedback(() -> Text.literal("Successfully added " + name + " to bot list!"), true);
            return 1;
        } else if (inList) {
            source.sendFeedback(() -> Text.literal(name + " is already in bot list!"), false);
            return 0;
        } else {
            source.sendFeedback(() -> Text.literal(name + " is not a fake player (Carpet bot)!"), false);
            return 0;
        }
    }

    private static int openSettingsGui(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("This command must be run by a player!"));
                return 0;
            }
            
            SettingsGui gui = new SettingsGui(player);
            gui.open();
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to open settings GUI: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static int showSettings(ServerCommandSource source) {
        BotSettings s = BotSettings.get();
        source.sendFeedback(() -> Text.literal("=== Equipment Settings ==="), false);
        source.sendFeedback(() -> Text.literal("autoarmor: " + s.isAutoEquipArmor()), false);
        source.sendFeedback(() -> Text.literal("autoweapon: " + s.isAutoEquipWeapon()), false);
        source.sendFeedback(() -> Text.literal("droparmor: " + s.isDropWorseArmor()), false);
        source.sendFeedback(() -> Text.literal("dropweapon: " + s.isDropWorseWeapons()), false);
        source.sendFeedback(() -> Text.literal("dropdistance: " + s.getDropDistance()), false);
        source.sendFeedback(() -> Text.literal("interval: " + s.getCheckInterval() + " ticks"), false);
        source.sendFeedback(() -> Text.literal("minarmorlevel: " + s.getMinArmorLevel()), false);
        
        source.sendFeedback(() -> Text.literal("=== Combat Settings ==="), false);
        source.sendFeedback(() -> Text.literal("combat: " + s.isCombatEnabled()), false);
        source.sendFeedback(() -> Text.literal("revenge: " + s.isRevengeEnabled()), false);
        source.sendFeedback(() -> Text.literal("autotarget: " + s.isAutoTargetEnabled()), false);
        source.sendFeedback(() -> Text.literal("targetplayers: " + s.isTargetPlayers()), false);
        source.sendFeedback(() -> Text.literal("targetmobs: " + s.isTargetHostileMobs()), false);
        source.sendFeedback(() -> Text.literal("targetbots: " + s.isTargetOtherBots()), false);
        source.sendFeedback(() -> Text.literal("criticals: " + s.isCriticalsEnabled()), false);
        source.sendFeedback(() -> Text.literal("ranged: " + s.isRangedEnabled()), false);
        source.sendFeedback(() -> Text.literal("mace: " + s.isMaceEnabled()), false);
        source.sendFeedback(() -> Text.literal("attackcooldown: " + s.getAttackCooldown() + " ticks"), false);
        source.sendFeedback(() -> Text.literal("meleerange: " + s.getMeleeRange()), false);
        source.sendFeedback(() -> Text.literal("movespeed: " + s.getMoveSpeed()), false);
        
        source.sendFeedback(() -> Text.literal("=== Utilities ==="), false);
        source.sendFeedback(() -> Text.literal("autototem: " + s.isAutoTotemEnabled()), false);
        source.sendFeedback(() -> Text.literal("totempriority: " + s.isTotemPriority() + " (don't replace totem with shield)"), false);
        source.sendFeedback(() -> Text.literal("autoshield: " + s.isAutoShieldEnabled()), false);
        source.sendFeedback(() -> Text.literal("autopotion: " + s.isAutoPotionEnabled()), false);
        source.sendFeedback(() -> Text.literal("shieldbreak: " + s.isShieldBreakEnabled()), false);
        source.sendFeedback(() -> Text.literal("prefersword: " + s.isPreferSword()), false);
        
        source.sendFeedback(() -> Text.literal("=== Navigation Settings ==="), false);
        source.sendFeedback(() -> Text.literal("bhop: " + s.isBhopEnabled()), false);
        source.sendFeedback(() -> Text.literal("bhopcooldown: " + s.getBhopCooldown() + " ticks"), false);
        source.sendFeedback(() -> Text.literal("jumpboost: " + s.getJumpBoost()), false);
        source.sendFeedback(() -> Text.literal("idle: " + s.isIdleWanderEnabled()), false);
        source.sendFeedback(() -> Text.literal("idleradius: " + s.getIdleWanderRadius()), false);
        source.sendFeedback(() -> Text.literal("=== Factions & Mistakes ==="), false);
        source.sendFeedback(() -> Text.literal("factions: " + s.isFactionsEnabled()), false);
        source.sendFeedback(() -> Text.literal("friendlyfire: " + s.isFriendlyFireEnabled()), false);
        source.sendFeedback(() -> Text.literal("misschance: " + s.getMissChance() + "%"), false);
        source.sendFeedback(() -> Text.literal("mistakechance: " + s.getMistakeChance() + "%"), false);
        source.sendFeedback(() -> Text.literal("reactiondelay: " + s.getReactionDelay() + " ticks"), false);
        return 1;
    }
    
    // ============ РљРѕРјР°РЅРґС‹ С„СЂР°РєС†РёР№ ============
    
    private static int listFactions(ServerCommandSource source) {
        var factions = BotFaction.getAllFactions();
        if (factions.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No factions created"), false);
        } else {
            source.sendFeedback(() -> Text.literal("Factions (" + factions.size() + "):"), false);
            for (String faction : factions) {
                var members = BotFaction.getMembers(faction);
                var enemies = BotFaction.getHostileFactions(faction);
                source.sendFeedback(() -> Text.literal(" - " + faction + " (" + members.size() + " members, " + enemies.size() + " enemies)"), false);
            }
        }
        return factions.size();
    }
    
    private static int createFaction(ServerCommandSource source, String name) {
        if (BotFaction.createFaction(name)) {
            source.sendFeedback(() -> Text.literal("Faction '" + name + "' created!"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Faction '" + name + "' already exists!"));
            return 0;
        }
    }
    
    private static int deleteFaction(ServerCommandSource source, String name) {
        if (BotFaction.deleteFaction(name)) {
            source.sendFeedback(() -> Text.literal("Faction '" + name + "' deleted!"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Faction '" + name + "' not found!"));
            return 0;
        }
    }
    
    private static int addToFaction(ServerCommandSource source, String faction, String player) {
        if (BotFaction.addMember(faction, player)) {
            source.sendFeedback(() -> Text.literal("Added '" + player + "' to faction '" + faction + "'"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
    }
    
    private static int removeFromFaction(ServerCommandSource source, String faction, String player) {
        if (BotFaction.removeMember(faction, player)) {
            source.sendFeedback(() -> Text.literal("Removed '" + player + "' from faction '" + faction + "'"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to remove '" + player + "' from faction '" + faction + "'"));
            return 0;
        }
    }
    
    private static int setHostile(ServerCommandSource source, String faction1, String faction2, boolean hostile) {
        if (BotFaction.setHostile(faction1, faction2, hostile)) {
            if (hostile) {
                source.sendFeedback(() -> Text.literal("Factions '" + faction1 + "' and '" + faction2 + "' are now hostile!"), true);
            } else {
                source.sendFeedback(() -> Text.literal("Factions '" + faction1 + "' and '" + faction2 + "' are now neutral"), true);
            }
            return 1;
        } else {
            source.sendError(Text.literal("One or both factions not found, or same faction!"));
            return 0;
        }
    }
    
    private static int factionInfo(ServerCommandSource source, String faction) {
        var members = BotFaction.getMembers(faction);
        var enemies = BotFaction.getHostileFactions(faction);
        
        if (members.isEmpty() && enemies.isEmpty() && !BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Text.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("=== Faction: " + faction + " ==="), false);
        source.sendFeedback(() -> Text.literal("Members (" + members.size() + "): " + String.join(", ", members)), false);
        source.sendFeedback(() -> Text.literal("Hostile to (" + enemies.size() + "): " + String.join(", ", enemies)), false);
        return 1;
    }
    
    private static int addNearbyBotsToFaction(ServerCommandSource source, String faction, double radius) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Text.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var entity = source.getEntity();
        if (entity == null) {
            source.sendError(Text.literal("This command must be run by a player!"));
            return 0;
        }
        
        int count = 0;
        var allBots = BotManager.getAllBots();
        var server = source.getServer();
        
        for (String botName : allBots) {
            var bot = server.getPlayerManager().getPlayer(botName);
            if (bot != null && bot.distanceTo(entity) <= radius) {
                BotFaction.addMember(faction, botName);
                count++;
            }
        }
        
        final int added = count;
        source.sendFeedback(() -> Text.literal("Added " + added + " bots to faction '" + faction + "'"), true);
        return count;
    }
    
    private static int addAllBotsToFaction(ServerCommandSource source, String faction) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Text.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var allBots = BotManager.getAllBots();
        int count = 0;
        
        for (String botName : allBots) {
            BotFaction.addMember(faction, botName);
            count++;
        }
        
        final int added = count;
        source.sendFeedback(() -> Text.literal("Added " + added + " bots to faction '" + faction + "'"), true);
        return count;
    }
    
    private static int showInventory(ServerCommandSource source, String botName) {
        if (!BotManager.getAllBots().contains(botName)) {
            source.sendError(Text.literal("Bot '" + botName + "' not found!"));
            return 0;
        }
        
        var bot = source.getServer().getPlayerManager().getPlayer(botName);
        if (bot == null) {
            source.sendError(Text.literal("Bot '" + botName + "' not online!"));
            return 0;
        }
        
        // Р•СЃР»Рё InvView СѓСЃС‚Р°РЅРѕРІР»РµРЅ - РёСЃРїРѕР»СЊР·СѓРµРј РµРіРѕ GUI
        if (HAS_INVVIEW) {
            try {
                return openInvViewGui(source, bot);
            } catch (Exception e) {
                source.sendError(Text.literal("Failed to open InvView GUI: " + e.getMessage()));
                return 0;
            }
        }
        
        // Р•СЃР»Рё InvView РЅРµ СѓСЃС‚Р°РЅРѕРІР»РµРЅ - РїРѕРєР°Р·С‹РІР°РµРј СЃРѕРѕР±С‰РµРЅРёРµ
        source.sendError(Text.literal("InvView mod is not installed!"));
        source.sendFeedback(() -> Text.literal("Please install InvView to view bot inventories: https://modrinth.com/mod/invview"), false);
        return 0;
    }
    
    /**
     * РћС‚РєСЂС‹РІР°РµС‚ GUI InvView С‡РµСЂРµР· СЂРµС„Р»РµРєСЃРёСЋ (С‡С‚РѕР±С‹ РЅРµ Р±С‹Р»Рѕ Р¶РµСЃС‚РєРѕР№ Р·Р°РІРёСЃРёРјРѕСЃС‚Рё)
     */
    private static int openInvViewGui(ServerCommandSource source, ServerPlayerEntity targetPlayer) throws Exception {
        ServerPlayerEntity viewer = source.getPlayer();
        if (viewer == null) {
            source.sendError(Text.literal("This command must be run by a player!"));
            return 0;
        }
        
        // РСЃРїРѕР»СЊР·СѓРµРј СЂРµС„Р»РµРєСЃРёСЋ С‚РѕР»СЊРєРѕ РґР»СЏ РєР»Р°СЃСЃРѕРІ InvView
        Class<?> simpleGuiClass = Class.forName("eu.pb4.sgui.api.gui.SimpleGui");
        Class<?> savingGuiClass = Class.forName("us.potatoboy.invview.gui.SavingPlayerDataGui");
        
        // РџРѕР»СѓС‡Р°РµРј ScreenHandlerType.GENERIC_9X5 РЅР°РїСЂСЏРјСѓСЋ
        Object screenHandlerType = net.minecraft.screen.ScreenHandlerType.GENERIC_9X5;
        
        // new SavingPlayerDataGui(ScreenHandlerType.GENERIC_9X5, viewer, targetPlayer)
        Object gui = savingGuiClass.getConstructor(
                net.minecraft.screen.ScreenHandlerType.class, 
                ServerPlayerEntity.class, 
                ServerPlayerEntity.class)
                .newInstance(screenHandlerType, viewer, targetPlayer);
        
        // gui.setTitle(targetPlayer.getName())
        Method setTitleMethod = simpleGuiClass.getMethod("setTitle", Text.class);
        setTitleMethod.invoke(gui, targetPlayer.getName());
        
        // Р”РѕР±Р°РІР»СЏРµРј СЃР»РѕС‚С‹ РёРЅРІРµРЅС‚Р°СЂСЏ (СЃ РІРѕР·РјРѕР¶РЅРѕСЃС‚СЊСЋ СЂРµРґР°РєС‚РёСЂРѕРІР°РЅРёСЏ)
        Method setSlotRedirectMethod = simpleGuiClass.getMethod("setSlotRedirect", int.class, net.minecraft.screen.slot.Slot.class);
        var inventory = targetPlayer.getInventory();
        
        for (int i = 0; i < inventory.size(); i++) {
            // new Slot(inventory, i, 0, 0) - РѕР±С‹С‡РЅС‹Р№ СЃР»РѕС‚ СЃ РІРѕР·РјРѕР¶РЅРѕСЃС‚СЊСЋ СЂРµРґР°РєС‚РёСЂРѕРІР°РЅРёСЏ
            net.minecraft.screen.slot.Slot slot = new net.minecraft.screen.slot.Slot(inventory, i, 0, 0);
            setSlotRedirectMethod.invoke(gui, i, slot);
        }
        
        // gui.open()
        Method openMethod = simpleGuiClass.getMethod("open");
        openMethod.invoke(gui);
        
        return 1;
    }
    
    private static int giveFactionItem(ServerCommandSource source, String faction, String itemCommand) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Text.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var members = BotFaction.getMembers(faction);
        var server = source.getServer();
        int count = 0;
        
        for (String memberName : members) {
            // РСЃРїРѕР»СЊР·СѓРµРј РєРѕРјР°РЅРґСѓ give РґР»СЏ РєР°Р¶РґРѕРіРѕ С‡Р»РµРЅР° С„СЂР°РєС†РёРё
            try {
                server.getCommandManager().getDispatcher().execute(
                    "give " + memberName + " " + itemCommand,
                    server.getCommandSource()
                );
                count++;
            } catch (Exception e) {
                // РРіРЅРѕСЂРёСЂСѓРµРј РѕС€РёР±РєРё (РёРіСЂРѕРє РјРѕР¶РµС‚ Р±С‹С‚СЊ РѕС„С„Р»Р°Р№РЅ)
            }
        }
        
        final int given = count;
        source.sendFeedback(() -> Text.literal("Gave items to " + given + " members of faction '" + faction + "'"), true);
        return count;
    }
    
    private static int factionAttack(ServerCommandSource source, String faction, String targetName) {
        if (!BotFaction.getAllFactions().contains(faction)) {
            source.sendError(Text.literal("Faction '" + faction + "' not found!"));
            return 0;
        }
        
        var members = BotFaction.getMembers(faction);
        int count = 0;
        
        for (String memberName : members) {
            // РўРѕР»СЊРєРѕ Р±РѕС‚С‹ РјРѕРіСѓС‚ Р°С‚Р°РєРѕРІР°С‚СЊ
            if (BotManager.getAllBots().contains(memberName)) {
                BotCombat.setTarget(memberName, targetName);
                count++;
            }
        }
        
        final int attacking = count;
        source.sendFeedback(() -> Text.literal("Faction '" + faction + "' (" + attacking + " bots) attacking " + targetName + "!"), true);
        return count;
    }
    
    // ============ РљРѕРјР°РЅРґС‹ РєРёС‚РѕРІ ============
    
    private static int createKit(ServerCommandSource source, String kitName) {
        var player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("This command must be run by a player!"));
            return 0;
        }
        
        if (BotKits.kitExists(kitName)) {
            source.sendError(Text.literal("Kit '" + kitName + "' already exists!"));
            return 0;
        }
        
        if (BotKits.createKit(kitName, player)) {
            source.sendFeedback(() -> Text.literal("Kit '" + kitName + "' created from your inventory!"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to create kit (empty inventory?)"));
            return 0;
        }
    }
    
    private static int deleteKit(ServerCommandSource source, String kitName) {
        if (BotKits.deleteKit(kitName)) {
            source.sendFeedback(() -> Text.literal("Kit '" + kitName + "' deleted!"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Kit '" + kitName + "' not found!"));
            return 0;
        }
    }
    
    private static int listKits(ServerCommandSource source) {
        var kits = BotKits.getKitNames();
        if (kits.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No kits created. Use /pvpbot createkit <name> to create one."), false);
        } else {
            source.sendFeedback(() -> Text.literal("Kits (" + kits.size() + "): " + String.join(", ", kits)), false);
        }
        return 1;
    }
    
    private static int giveKitToPlayer(ServerCommandSource source, String playerName, String kitName) {
        if (!BotKits.kitExists(kitName)) {
            source.sendError(Text.literal("Kit '" + kitName + "' not found!"));
            return 0;
        }
        
        // РС‰РµРј РёРіСЂРѕРєР° РЅР° СЃРµСЂРІРµСЂРµ (Р±РѕС‚ РёР»Рё РѕР±С‹С‡РЅС‹Р№ РёРіСЂРѕРє)
        var player = source.getServer().getPlayerManager().getPlayer(playerName);
        if (player == null) {
            source.sendError(Text.literal("Player '" + playerName + "' not found!"));
            return 0;
        }
        
        if (BotKits.giveKit(kitName, player)) {
            source.sendFeedback(() -> Text.literal("Gave kit '" + kitName + "' to '" + playerName + "'"), true);
            return 1;
        } else {
            source.sendError(Text.literal("Failed to give kit!"));
            return 0;
        }
    }
    
    private static int giveKitToFaction(ServerCommandSource source, String factionName, String kitName) {
        if (!BotFaction.getAllFactions().contains(factionName)) {
            source.sendError(Text.literal("Faction '" + factionName + "' not found!"));
            return 0;
        }
        
        if (!BotKits.kitExists(kitName)) {
            source.sendError(Text.literal("Kit '" + kitName + "' not found!"));
            return 0;
        }
        
        var members = BotFaction.getMembers(factionName);
        if (members == null || members.isEmpty()) {
            source.sendError(Text.literal("Faction '" + factionName + "' has no members!"));
            return 0;
        }
        
        int count = 0;
        for (String memberName : members) {
            // РџСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ СЌС‚Рѕ Р±РѕС‚
            if (BotManager.getAllBots().contains(memberName)) {
                var bot = BotManager.getBot(source.getServer(), memberName);
                if (bot != null && BotKits.giveKit(kitName, bot)) {
                    count++;
                }
            }
        }
        
        final int given = count;
        source.sendFeedback(() -> Text.literal("Gave kit '" + kitName + "' to " + given + " bots in faction '" + factionName + "'"), true);
        return 1;
    }
    
    /**
     * РћС‚РєСЂС‹РІР°РµС‚ С‚РµСЃС‚РѕРІРѕРµ РјРµРЅСЋ СЃ РїСЂРёРјРµСЂР°РјРё sgui
     */
    private static int openTestMenu(ServerCommandSource source) {
        try {
            ServerPlayerEntity player = source.getPlayer();
            if (player == null) {
                source.sendError(Text.literal("This command must be run by a player!"));
                return 0;
            }
            
            // РћС‚РєСЂС‹РІР°РµРј РіР»Р°РІРЅРѕРµ РјРµРЅСЋ
            org.stepan1411.pvp_bot.gui.BotMenuGui.openMainMenu(player);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to open menu: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * РћС‚РїСЂР°РІР»СЏРµС‚ СЃС‚Р°С‚РёСЃС‚РёРєСѓ РЅР° СЃРµСЂРІРµСЂ (РґР»СЏ РѕС‚Р»Р°РґРєРё)
     */
    private static int updateStats(ServerCommandSource source) {
        try {
            StatsReporter.sendStats();
            source.sendFeedback(() -> Text.literal("Statistics sent to server!"), true);
            return 1;
        } catch (Exception e) {
            source.sendError(Text.literal("Failed to send statistics: " + e.getMessage()));
            return 0;
        }
    }
    
    // ============ Debug Commands ============
    
    private static int toggleDebugPath(ServerCommandSource source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.pathVisualization;
        BotDebug.setPathVisualization(botName, newValue);
        source.sendFeedback(() -> Text.literal("Path visualization for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugPath(ServerCommandSource source, String botName, boolean enabled) {
        BotDebug.setPathVisualization(botName, enabled);
        source.sendFeedback(() -> Text.literal("Path visualization for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugTarget(ServerCommandSource source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.targetVisualization;
        BotDebug.setTargetVisualization(botName, newValue);
        source.sendFeedback(() -> Text.literal("Target visualization for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugTarget(ServerCommandSource source, String botName, boolean enabled) {
        BotDebug.setTargetVisualization(botName, enabled);
        source.sendFeedback(() -> Text.literal("Target visualization for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugCombat(ServerCommandSource source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.combatInfo;
        BotDebug.setCombatInfo(botName, newValue);
        source.sendFeedback(() -> Text.literal("Combat info for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugCombat(ServerCommandSource source, String botName, boolean enabled) {
        BotDebug.setCombatInfo(botName, enabled);
        source.sendFeedback(() -> Text.literal("Combat info for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugNavigation(ServerCommandSource source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.navigationInfo;
        BotDebug.setNavigationInfo(botName, newValue);
        source.sendFeedback(() -> Text.literal("Navigation info for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugNavigation(ServerCommandSource source, String botName, boolean enabled) {
        BotDebug.setNavigationInfo(botName, enabled);
        source.sendFeedback(() -> Text.literal("Navigation info for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int toggleDebugAll(ServerCommandSource source, String botName) {
        var settings = BotDebug.getSettings(botName);
        boolean newValue = !settings.isAnyEnabled();
        if (newValue) {
            BotDebug.enableAll(botName);
        } else {
            BotDebug.disableAll(botName);
        }
        source.sendFeedback(() -> Text.literal("All debug modes for " + botName + ": " + newValue), true);
        return newValue ? 1 : 0;
    }
    
    private static int setDebugAll(ServerCommandSource source, String botName, boolean enabled) {
        if (enabled) {
            BotDebug.enableAll(botName);
        } else {
            BotDebug.disableAll(botName);
        }
        source.sendFeedback(() -> Text.literal("All debug modes for " + botName + ": " + enabled), true);
        return enabled ? 1 : 0;
    }
    
    private static int showDebugStatus(ServerCommandSource source, String botName) {
        var settings = BotDebug.getSettings(botName);
        source.sendFeedback(() -> Text.literal("=== Debug Status for " + botName + " ==="), false);
        source.sendFeedback(() -> Text.literal("Path visualization: " + settings.pathVisualization), false);
        source.sendFeedback(() -> Text.literal("Target visualization: " + settings.targetVisualization), false);
        source.sendFeedback(() -> Text.literal("Combat info: " + settings.combatInfo), false);
        source.sendFeedback(() -> Text.literal("Navigation info: " + settings.navigationInfo), false);
        return 1;
    }
    
    // ========== PATH COMMANDS ==========
    
    private static int createPath(ServerCommandSource source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.createPath(name)) {
            // Автоматически включаем визуализацию
            org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, true);
            source.sendFeedback(() -> Text.literal("§aPath '" + name + "' created"), true);
            source.sendFeedback(() -> Text.literal("§7Visualization enabled. To disable: §e/pvpbot path show " + name + " false"), false);
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' already exists"));
            return 0;
        }
    }
    
    private static int deletePath(ServerCommandSource source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.deletePath(name)) {
            source.sendFeedback(() -> Text.literal("§aPath '" + name + "' deleted"), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int addPathPoint(ServerCommandSource source, String name) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cOnly players can add path points"));
            return 0;
        }
        
        net.minecraft.util.math.Vec3d pos = new net.minecraft.util.math.Vec3d(player.getX(), player.getY(), player.getZ());
        if (org.stepan1411.pvp_bot.bot.BotPath.addPoint(name, pos)) {
            var path = org.stepan1411.pvp_bot.bot.BotPath.getPath(name);
            // Автоматически включаем визуализацию если ещё не включена
            if (!org.stepan1411.pvp_bot.bot.BotPath.isPathVisible(name)) {
                org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, true);
                source.sendFeedback(() -> Text.literal("§7Visualization enabled. To disable: §e/pvpbot path show " + name + " false"), false);
            }
            source.sendFeedback(() -> Text.literal(String.format("§aPoint #%d added to path '%s' at (%.1f, %.1f, %.1f)", 
                path.points.size(), name, pos.x, pos.y, pos.z)), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int removeLastPathPoint(ServerCommandSource source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.removeLastPoint(name)) {
            source.sendFeedback(() -> Text.literal("§aLast point removed from path '" + name + "'"), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' not found or empty"));
            return 0;
        }
    }
    
    private static int removePathPoint(ServerCommandSource source, String name, int index) {
        if (org.stepan1411.pvp_bot.bot.BotPath.removePoint(name, index)) {
            source.sendFeedback(() -> Text.literal("§aPoint #" + index + " removed from path '" + name + "'"), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cInvalid path or index"));
            return 0;
        }
    }
    
    private static int clearPath(ServerCommandSource source, String name) {
        if (org.stepan1411.pvp_bot.bot.BotPath.clearPath(name)) {
            source.sendFeedback(() -> Text.literal("§aAll points cleared from path '" + name + "'"), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int setPathLoop(ServerCommandSource source, String name, boolean loop) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setLoop(name, loop)) {
            source.sendFeedback(() -> Text.literal("§aPath '" + name + "' loop: " + loop), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int setPathAttack(ServerCommandSource source, String name, boolean attack) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setAttack(name, attack)) {
            if (attack) {
                source.sendFeedback(() -> Text.literal("§aPath '" + name + "' attack: enabled"), true);
            } else {
                source.sendFeedback(() -> Text.literal("§aPath '" + name + "' attack: disabled"), true);
                source.sendFeedback(() -> Text.literal("§7Bot will ignore attacks and continue following path"), false);
            }
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    private static int startPathFollowing(ServerCommandSource source, String botName, String pathName) {
        if (org.stepan1411.pvp_bot.bot.BotPath.startFollowing(botName, pathName)) {
            source.sendFeedback(() -> Text.literal("§aBot '" + botName + "' started following path '" + pathName + "'"), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + pathName + "' not found or empty"));
            return 0;
        }
    }
    
    private static int stopPathFollowing(ServerCommandSource source, String botName) {
        if (org.stepan1411.pvp_bot.bot.BotPath.stopFollowing(botName)) {
            source.sendFeedback(() -> Text.literal("§aBot '" + botName + "' stopped following path"), true);
            return 1;
        } else {
            source.sendError(Text.literal("§cBot '" + botName + "' is not following any path"));
            return 0;
        }
    }
    
    private static int listPaths(ServerCommandSource source) {
        var paths = org.stepan1411.pvp_bot.bot.BotPath.getAllPaths();
        if (paths.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§eNo paths created"), false);
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("§6=== Paths ==="), false);
        for (var entry : paths.entrySet()) {
            String name = entry.getKey();
            var path = entry.getValue();
            source.sendFeedback(() -> Text.literal(String.format("§e%s§7: %d points, loop: %s, attack: %s", 
                name, path.points.size(), path.loop, path.attack)), false);
        }
        return paths.size();
    }
    
    private static int pathInfo(ServerCommandSource source, String name) {
        var path = org.stepan1411.pvp_bot.bot.BotPath.getPath(name);
        if (path == null) {
            source.sendError(Text.literal("§cPath '" + name + "' not found"));
            return 0;
        }
        
        source.sendFeedback(() -> Text.literal("§6=== Path: " + name + " ==="), false);
        source.sendFeedback(() -> Text.literal("§7Points: " + path.points.size()), false);
        source.sendFeedback(() -> Text.literal("§7Loop: " + path.loop), false);
        source.sendFeedback(() -> Text.literal("§7Attack: " + path.attack), false);
        
        for (int i = 0; i < path.points.size(); i++) {
            var point = path.points.get(i);
            int index = i;
            source.sendFeedback(() -> Text.literal(String.format("§e#%d§7: (%.1f, %.1f, %.1f)", 
                index, point.x, point.y, point.z)), false);
        }
        
        return 1;
    }
    
    private static int showPath(ServerCommandSource source, String name, boolean visible) {
        if (org.stepan1411.pvp_bot.bot.BotPath.setPathVisible(name, visible)) {
            if (visible) {
                source.sendFeedback(() -> Text.literal("§aPath '" + name + "' visualization enabled"), true);
                source.sendFeedback(() -> Text.literal("§7To disable: §e/pvpbot path show " + name + " false"), false);
            } else {
                source.sendFeedback(() -> Text.literal("§aPath '" + name + "' visualization disabled"), true);
            }
            return 1;
        } else {
            source.sendError(Text.literal("§cPath '" + name + "' not found"));
            return 0;
        }
    }
    
    // ========== NEW PATH COMMANDS ==========
    
    private static int distributeBotsOnPath(ServerCommandSource source, String pathName) {
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Text.literal("§cPath '" + pathName + "' not found"));
            return 0;
        }
        
        if (path.points.isEmpty()) {
            source.sendError(Text.literal("§cPath '" + pathName + "' has no points"));
            return 0;
        }
        
        // Get all bots following this path
        var server = source.getServer();
        var botsOnPath = new java.util.ArrayList<String>();
        for (String botName : BotManager.getAllBots()) {
            if (BotPath.isFollowing(botName, pathName)) {
                botsOnPath.add(botName);
            }
        }
        
        if (botsOnPath.isEmpty()) {
            source.sendError(Text.literal("§cNo bots are following path '" + pathName + "'"));
            return 0;
        }
        
        // Distribute bots evenly along the path
        int totalPoints = path.points.size();
        int botCount = botsOnPath.size();
        
        for (int i = 0; i < botCount; i++) {
            String botName = botsOnPath.get(i);
            int pointIndex = (i * totalPoints) / botCount;
            
            // Set bot's current point on path
            BotPath.setBotPathIndex(botName, pointIndex);
            
            // Teleport bot to that point (1 block above to avoid getting stuck)
            var point = path.points.get(pointIndex);
            try {
                String tpCommand = String.format(java.util.Locale.US,
                    "tp %s %.2f %.2f %.2f",
                    botName, point.x, point.y + 1.0, point.z
                );
                server.getCommandManager().getDispatcher().execute(tpCommand, server.getCommandSource());
            } catch (Exception e) {
                // Ignore teleport errors
            }
        }
        
        source.sendFeedback(() -> Text.literal("§aDistributed " + botCount + " bots along path '" + pathName + "'"), true);
        return botCount;
    }
    
    private static int startPathNear(ServerCommandSource source, String pathName, double radius) {
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Text.literal("§cPath '" + pathName + "' not found"));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("§cThis command can only be used by a player"));
            return 0;
        }
        
        var server = source.getServer();
        int started = 0;
        
        for (String botName : BotManager.getAllBots()) {
            ServerPlayerEntity bot = server.getPlayerManager().getPlayer(botName);
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
            source.sendFeedback(() -> Text.literal("§aStarted path '" + pathName + "' for " + finalStarted + " bots within " + radius + " blocks"), true);
            return started;
        } else {
            source.sendError(Text.literal("§cNo bots found within " + radius + " blocks"));
            return 0;
        }
    }
    
    private static int stopAllOnPath(ServerCommandSource source, String pathName) {
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Text.literal("§cPath '" + pathName + "' not found"));
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
            source.sendFeedback(() -> Text.literal("§aStopped " + finalStopped + " bots on path '" + pathName + "'"), true);
            return stopped;
        } else {
            source.sendError(Text.literal("§cNo bots are following path '" + pathName + "'"));
            return 0;
        }
    }
    
    // ========== FACTION PATH COMMANDS ==========
    
    private static int factionStartPath(ServerCommandSource source, String factionName, String pathName) {
        var members = BotFaction.getMembers(factionName);
        if (members.isEmpty()) {
            source.sendError(Text.literal("§cFaction '" + factionName + "' not found or has no members"));
            return 0;
        }
        
        var path = BotPath.getPath(pathName);
        if (path == null) {
            source.sendError(Text.literal("§cPath '" + pathName + "' not found"));
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
            source.sendFeedback(() -> Text.literal("§aStarted path '" + pathName + "' for " + finalStarted + " bots in faction '" + factionName + "'"), true);
            return started;
        } else {
            source.sendError(Text.literal("§cNo bots in faction '" + factionName + "'"));
            return 0;
        }
    }
    
    private static int factionStopPath(ServerCommandSource source, String factionName) {
        var members = BotFaction.getMembers(factionName);
        if (members.isEmpty()) {
            source.sendError(Text.literal("§cFaction '" + factionName + "' not found or has no members"));
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
            source.sendFeedback(() -> Text.literal("§aStopped path for " + finalStopped + " bots in faction '" + factionName + "'"), true);
            return stopped;
        } else {
            source.sendError(Text.literal("§cNo bots in faction '" + factionName + "' were following a path"));
            return 0;
        }
    }
}
