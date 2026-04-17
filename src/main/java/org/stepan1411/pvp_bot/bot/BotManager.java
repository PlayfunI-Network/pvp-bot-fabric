package org.stepan1411.pvp_bot.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.stepan1411.pvp_bot.config.WorldConfigHelper;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BotManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> bots = new HashSet<>();
    private static final Map<String, BotData> botDataMap = new HashMap<>();
    private static Path savePath;
    private static boolean initialized = false;
    private static int botsSpawnedTotal = 0;
    private static int botsKilledTotal = 0;
    public static class BotData {
        public String name;
        public double x, y, z;
        public float yaw, pitch;
        public String dimension;
        public String gamemode;
        
        public BotData() {}
        
        public BotData(ServerPlayer bot) {
            this.name = bot.getName().getString();
            this.x = bot.getX();
            this.y = bot.getY();
            this.z = bot.getZ();
            this.yaw = bot.getYRot();
            this.pitch = bot.getXRot();
            this.dimension = bot.level().dimension().location().toString();
            this.gamemode = bot.gameMode.getGameModeForPlayer().getName();
        }
    }
    
    public static class StatsData {
        public int botsSpawnedTotal = 0;
        public int botsKilledTotal = 0;
    }
    
    public static void init(MinecraftServer server) {
        if (initialized) return;
        

        bots.clear();
        botDataMap.clear();
        System.out.println("[PVP_BOT] Initializing BotManager for new world...");
        
        Path configDir = org.stepan1411.pvp_bot.PvpBotPlugin.getInstance().getDataFolder().toPath().getParent().resolve("pvpbot");
        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            System.out.println("[PVP_BOT] Failed to create config directory: " + e.getMessage());
        }
        
        savePath = org.stepan1411.pvp_bot.config.WorldConfigHelper.getWorldConfigDir().resolve("bots.json");
        System.out.println("[PVP_BOT] Bot config path: " + savePath);
        loadBots();
        loadStats();
        BotSettings settings = BotSettings.get();
        if (settings.isBotsRelogs() && !botDataMap.isEmpty()) {
            System.out.println("[PVP_BOT] Restoring " + botDataMap.size() + " bots...");
            Map<String, BotData> botsToRestore = new HashMap<>(botDataMap);
            bots.clear();
            botDataMap.clear();
            server.execute(() -> restoreBotsDelayed(server, botsToRestore, 0));
        } else if (!settings.isBotsRelogs()) {
            bots.clear();
            botDataMap.clear();
            saveBots();
        }
        
        initialized = true;
    }
    
    private static void restoreBotsDelayed(MinecraftServer server, Map<String, BotData> botsToRestore, int index) {
        restoreBotsDelayedWithRetry(server, botsToRestore, index, 0);
    }
    
    private static void restoreBotsDelayedWithRetry(MinecraftServer server, Map<String, BotData> botsToRestore, int index, int retryCount) {
        final int MAX_RETRIES = 2;
        
        if (index >= botsToRestore.size()) {
            System.out.println("[PVP_BOT] Restored " + bots.size() + " bots");
            return;
        }
        
        String[] names = botsToRestore.keySet().toArray(new String[0]);
        if (index < names.length) {
            String name = names[index];
            BotData data = botsToRestore.get(name);
            ServerPlayer existingPlayer = server.getPlayerList().getPlayerByName(name);
            if (existingPlayer != null && !bots.contains(name)) {
                System.out.println("[PVP_BOT] Skipping bot '" + name + "': real player with this name is online");
                final int nextIndex = index + 1;
                server.execute(() -> restoreBotsDelayedWithRetry(server, botsToRestore, nextIndex, 0));
                return;
            }
            var dispatcher = server.getCommands().getDispatcher();
            boolean success = false;
            
            try {
                String command = String.format(java.util.Locale.US,
                    "player %s spawn at %.2f %.2f %.2f facing %.2f %.2f in %s in %s",
                    name, data.x, data.y, data.z, data.yaw, data.pitch, data.dimension, data.gamemode
                );
                if (retryCount == 0) {
                    System.out.println("[PVP_BOT] Restoring bot: " + name);
                } else {
                    System.out.println("[PVP_BOT] Retry #" + retryCount + " for bot: " + name);
                }
                dispatcher.execute(command, server.getSharedSuggestionProvider());
                bots.add(name);
                botDataMap.put(name, data);
                success = true;
                System.out.println("[PVP_BOT] вњ“ Successfully restored bot: " + name);
            } catch (Exception e) {

                try {
                    String simpleCommand = String.format(java.util.Locale.US,
                        "player %s spawn at %.2f %.2f %.2f",
                        name, data.x, data.y, data.z
                    );
                    dispatcher.execute(simpleCommand, server.getSharedSuggestionProvider());
                    bots.add(name);
                    botDataMap.put(name, data);
                    success = true;
                    System.out.println("[PVP_BOT] вњ“ Restored bot with simple command: " + name);
                } catch (Exception e2) {
                    if (retryCount < MAX_RETRIES) {
                        System.out.println("[PVP_BOT] вљ  Failed to restore bot '" + name + "', will retry... (" + (retryCount + 1) + "/" + MAX_RETRIES + ")");
                    } else {
                        System.out.println("[PVP_BOT] вњ— Failed to restore bot '" + name + "' after " + (MAX_RETRIES + 1) + " attempts: " + e2.getMessage());
                    }
                }
            }
            

            if (!success && retryCount < MAX_RETRIES) {
                final int currentRetry = retryCount + 1;
                server.execute(() -> {
                    final int[] delay = {0};
                    server.execute(new Runnable() {
                        @Override
                        public void run() {
                            delay[0]++;
                            if (delay[0] < 20) {
                                server.execute(this);
                            } else {
                                restoreBotsDelayedWithRetry(server, botsToRestore, index, currentRetry);
                            }
                        }
                    });
                });
            } else {

                final int nextIndex = index + 1;
                server.execute(() -> {
                    final int[] delay = {0};
                    server.execute(new Runnable() {
                        @Override
                        public void run() {
                            delay[0]++;
                            if (delay[0] < 10) {
                                server.execute(this);
                            } else {
                                restoreBotsDelayedWithRetry(server, botsToRestore, nextIndex, 0);
                            }
                        }
                    });
                });
            }
        }
    }
    
    
    public static void updateBotData(MinecraftServer server) {
        int updated = 0;
        int skipped = 0;
        int missing = 0;
        for (String name : bots) {
            ServerPlayer bot = server.getPlayerList().getPlayerByName(name);
            if (bot != null && bot.isAlive()) {

                botDataMap.put(name, new BotData(bot));
                updated++;
            } else if (!botDataMap.containsKey(name)) {


                missing++;
                System.out.println("[PVP_BOT] WARNING: Bot " + name + " in list but has no data!");
            } else {

                skipped++;
            }
        }
        System.out.println("[PVP_BOT] Updated bot data: " + updated + " updated, " + skipped + " skipped, " + missing + " missing, " + bots.size() + " total in list, " + botDataMap.size() + " in data map");
    }
    
    
    public static void saveBots() {
        if (savePath == null) return;
        
        try (Writer writer = Files.newBufferedWriter(savePath)) {
            GSON.toJson(botDataMap, writer);
        } catch (Exception e) {
            System.out.println("[PVP_BOT] Failed to save bots: " + e.getMessage());
        }
    }
    
    
    private static void loadBots() {
        System.out.println("[PVP_BOT] Loading bots from: " + savePath);
        if (savePath == null || !Files.exists(savePath)) {
            System.out.println("[PVP_BOT] No bots file found, starting fresh");
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(savePath)) {
            Map<String, BotData> loaded = GSON.fromJson(reader, new TypeToken<Map<String, BotData>>(){}.getType());
            if (loaded != null) {
                botDataMap.putAll(loaded);
                bots.addAll(loaded.keySet());
                System.out.println("[PVP_BOT] Loaded " + loaded.size() + " bots from file");
            }
        } catch (Exception e) {
            System.out.println("[PVP_BOT] Failed to load bots: " + e.getMessage());
        }
    }
    
    
    public static void reset(MinecraftServer server) {
        updateBotData(server);
        saveBots();
        initialized = false;
    }
    
    public static void switchWorld(MinecraftServer server) {

        updateBotData(server);
        saveBots();
        saveStats();


        bots.clear();
        botDataMap.clear();


        savePath = org.stepan1411.pvp_bot.config.WorldConfigHelper.getWorldConfigDir().resolve("bots.json");


        loadBots();


        BotSettings settings = BotSettings.get();
        if (settings.isBotsRelogs() && !botDataMap.isEmpty()) {
            System.out.println("[PVP_BOT] Switching world, restoring " + botDataMap.size() + " bots...");
            Map<String, BotData> botsToRestore = new HashMap<>(botDataMap);
            bots.clear();
            botDataMap.clear();

            server.execute(() -> restoreBotsDelayed(server, botsToRestore, 0));
        }
    }


    public static boolean spawnBot(MinecraftServer server, String name, CommandSourceStack source) {

        ServerPlayer existingPlayer = server.getPlayerList().getPlayerByName(name);
        if (existingPlayer != null && existingPlayer.isAlive()) {

            if (!bots.contains(name)) {
                bots.add(name);
                botDataMap.put(name, new BotData(existingPlayer));
                saveBots();
                System.out.println("[PVP_BOT] Added existing bot to list: " + name);
            }
            return false;
        }


        var dispatcher = server.getCommands().getDispatcher();
        try {

            dispatcher.execute("playerspawn " + name + " at ~ ~ ~ facing 0 0 in survival", source);
        } catch (Exception e) {

            try {
                dispatcher.execute("playerspawn " + name, source);

                dispatcher.execute("gamemode survival " + name, server.getSharedSuggestionProvider());
            } catch (Exception e2) {

            }
        }
        


        server.execute(() -> {
            ServerPlayer newBot = server.getPlayerList().getPlayerByName(name);
            if (newBot != null && !bots.contains(name)) {
                bots.add(name);
                botDataMap.put(name, new BotData(newBot));
                incrementBotsSpawned();
                saveBots();
                System.out.println("[PVP_BOT] Added bot to list (delayed): " + name);
                

                try {
                    org.stepan1411.pvp_bot.api.PvpBotAPI.getEventManager().fireSpawnEvent(newBot);
                } catch (Exception e) {
                    System.err.println("[PVP_BOT_API] Error firing spawn event: " + e.getMessage());
                }
            } else if (newBot != null && bots.contains(name)) {

                botDataMap.put(name, new BotData(newBot));
                saveBots();
                System.out.println("[PVP_BOT] Updated bot data (delayed): " + name);
            }
        });
        

        ServerPlayer newBot = server.getPlayerList().getPlayerByName(name);
        if (newBot != null) {
            if (!bots.contains(name)) {
                bots.add(name);
                botDataMap.put(name, new BotData(newBot));
                incrementBotsSpawned();
                saveBots();
                System.out.println("[PVP_BOT] Added bot to list (immediate): " + name);

                try {
                    org.stepan1411.pvp_bot.api.BotAPIIntegration.fireSpawnEvent(newBot);
                } catch (Exception e) {
                    System.err.println("[PVP_BOT_API] Error firing spawn event: " + e.getMessage());
                }
            }
            return true;
        }
        

        if (!bots.contains(name)) {
            bots.add(name);

            BotData defaultData = new BotData();
            defaultData.name = name;
            defaultData.x = source.getPosition().x;
            defaultData.y = source.getPosition().y;
            defaultData.z = source.getPosition().z;
            defaultData.yaw = source.getRotation().y;
            defaultData.pitch = source.getRotation().x;
            defaultData.dimension = source.getLevel().dimension().location().toString();
            defaultData.gamemode = "survival";
            botDataMap.put(name, defaultData);
            incrementBotsSpawned();
            saveBots();
            System.out.println("[PVP_BOT] Added bot to list (default data): " + name);
        }
        
        return true;
    }

    public static boolean removeBot(MinecraftServer server, String name, CommandSourceStack source) {

        boolean wasInList = bots.remove(name);
        botDataMap.remove(name);
        saveBots();
        

        BotCombat.removeState(name);
        BotUtils.removeState(name);
        BotNavigation.resetIdle(name);
        BotBaritone.removeBaritone(name);
        BotMovement.clearState(name);
        

        try {
            org.stepan1411.pvp_bot.api.combat.CombatStrategyRegistry.getInstance().clearCooldowns(name);
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error clearing strategy cooldowns: " + e.getMessage());
        }

        String command = "player " + name + " kill";
        var dispatcher = server.getCommands().getDispatcher();
        try {
            dispatcher.execute(command, source);
        } catch (Exception e) {

        }
        

        
        return wasInList;
    }

    public static ServerPlayer getBot(MinecraftServer server, String name) {
        if (server == null) return null;
        return server.getPlayerList().getPlayerByName(name);
    }

    public static void removeAllBots(MinecraftServer server, CommandSourceStack source) {
        var dispatcher = server.getCommands().getDispatcher();
        for (String name : new HashSet<>(bots)) {

            BotCombat.removeState(name);
            BotUtils.removeState(name);
            BotNavigation.removeState(name);
            BotNavigation.resetIdle(name);
            BotBaritone.removeBaritone(name);
            BotMovement.clearState(name);
            

            try {
                org.stepan1411.pvp_bot.api.combat.CombatStrategyRegistry.getInstance().clearCooldowns(name);
            } catch (Exception e) {
                System.err.println("[PVP_BOT_API] Error clearing strategy cooldowns: " + e.getMessage());
            }
            
            String command = "player " + name + " kill";
            try {
                dispatcher.execute(command, source);
            } catch (Exception e) {

            }
        }
        bots.clear();
        botDataMap.clear();
        saveBots();
        

    }

    public static int getBotCount() {
        return bots.size();
    }

    public static Set<String> getAllBots() {
        return new HashSet<>(bots);
    }
    
    
    public static void cleanupDeadBots(MinecraftServer server) {
        boolean changed = false;
        for (String name : new HashSet<>(bots)) {
            ServerPlayer bot = server.getPlayerList().getPlayerByName(name);
            


            if (bot == null) {
                continue;
            }
            

            boolean isDead = !bot.isAlive() || bot.getHealth() <= 0 || bot.isDead();
            if (isDead) {


                try {
                    org.stepan1411.pvp_bot.api.BotAPIIntegration.fireDeathEvent(bot);
                } catch (Exception e) {
                    System.err.println("[PVP_BOT_API] Error firing death event: " + e.getMessage());
                }
                
                bots.remove(name);
                botDataMap.remove(name);
                BotCombat.removeState(name);
                BotUtils.removeState(name);
                BotNavigation.removeState(name);
                BotNavigation.resetIdle(name);
                BotBaritone.removeBaritone(name);
                BotMovement.clearState(name);
                

                try {
                    org.stepan1411.pvp_bot.api.combat.CombatStrategyRegistry.getInstance().clearCooldowns(name);
                } catch (Exception e) {
                    System.err.println("[PVP_BOT_API] Error clearing strategy cooldowns: " + e.getMessage());
                }
                
                incrementBotsKilled();
                changed = true;
                System.out.println("[PVP_BOT] Removed dead bot: " + name);
            }
        }
        if (changed) {
            saveBots();
        }
    }
    
    
    public static void syncBots(MinecraftServer server) {
        boolean changed = false;

        for (var player : server.getPlayerList().getPlayers()) {
            String name = player.getName().getString();
            

            if (bots.contains(name)) continue;
            

            String className = player.getClass().getName();
            boolean isFakePlayer = className.contains("EntityPlayerMPFake") || 
                                   className.contains("FakePlayer") ||
                                   className.contains("fake") ||
                                   className.contains("Fake");
            
            if (isFakePlayer) {
                bots.add(name);
                botDataMap.put(name, new BotData(player));
                changed = true;
                System.out.println("[PVP_BOT] Synced Carpet bot: " + name);
            }
        }
        if (changed) {
            saveBots();
        }
    }
    
    
    public static boolean syncBot(MinecraftServer server, String name) {

        if (bots.contains(name)) {
            return false;
        }
        

        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player == null) {
            return false;
        }
        

        String className = player.getClass().getName();
        boolean isFakePlayer = className.contains("EntityPlayerMPFake") || 
                               className.contains("FakePlayer") ||
                               className.contains("fake") ||
                               className.contains("Fake");
        
        if (isFakePlayer) {
            bots.add(name);
            botDataMap.put(name, new BotData(player));
            incrementBotsSpawned();
            saveBots();
            System.out.println("[PVP_BOT] Synced bot: " + name);
            return true;
        }
        
        return false;
    }
    
    
    public static void incrementBotsSpawned() {
        botsSpawnedTotal++;
        saveStats();

    }
    
    
    public static void incrementBotsKilled() {
        botsKilledTotal++;
        saveStats();

    }
    
    
    public static int getBotsSpawnedTotal() {
        return botsSpawnedTotal;
    }
    
    
    public static int getBotsKilledTotal() {
        return botsKilledTotal;
    }
    
    
    private static void saveStats() {
        Path statsPath = WorldConfigHelper.getGlobalConfigDir().resolve("stats.json");
        try (Writer writer = Files.newBufferedWriter(statsPath)) {
            StatsData stats = new StatsData();
            stats.botsSpawnedTotal = botsSpawnedTotal;
            stats.botsKilledTotal = botsKilledTotal;
            GSON.toJson(stats, writer);
        } catch (Exception e) {
            System.out.println("[PVP_BOT] Failed to save stats: " + e.getMessage());
        }
    }
    
    
    private static void loadStats() {
        Path statsPath = WorldConfigHelper.getGlobalConfigDir().resolve("stats.json");
        if (!Files.exists(statsPath)) return;
        
        try (Reader reader = Files.newBufferedReader(statsPath)) {
            StatsData stats = GSON.fromJson(reader, StatsData.class);
            if (stats != null) {
                botsSpawnedTotal = stats.botsSpawnedTotal;
                botsKilledTotal = stats.botsKilledTotal;
            }
        } catch (Exception e) {
            System.out.println("[PVP_BOT] Failed to load stats: " + e.getMessage());
        }
    }
}
