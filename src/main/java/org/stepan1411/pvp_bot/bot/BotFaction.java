package org.stepan1411.pvp_bot.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * РЎРёСЃС‚РµРјР° С„СЂР°РєС†РёР№ РґР»СЏ Р±РѕС‚РѕРІ Рё РёРіСЂРѕРєРѕРІ
 */
public class BotFaction {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    
    // Р¤СЂР°РєС†РёРё: РёРјСЏ С„СЂР°РєС†РёРё -> СЃРїРёСЃРѕРє С‡Р»РµРЅРѕРІ (РЅРёРєРё)
    private static final Map<String, Set<String>> factions = new HashMap<>();
    
    // Р’СЂР°Р¶РґРµР±РЅС‹Рµ РѕС‚РЅРѕС€РµРЅРёСЏ: С„СЂР°РєС†РёСЏ -> СЃРїРёСЃРѕРє РІСЂР°Р¶РґРµР±РЅС‹С… С„СЂР°РєС†РёР№
    private static final Map<String, Set<String>> hostileRelations = new HashMap<>();
    
    // РљСЌС€: РёРіСЂРѕРє -> РµРіРѕ С„СЂР°РєС†РёСЏ
    private static final Map<String, String> playerFactionCache = new HashMap<>();
    
    static {
        load();
    }
    
    /**
     * РЎРѕР·РґР°С‚СЊ РЅРѕРІСѓСЋ С„СЂР°РєС†РёСЋ
     */
    public static boolean createFaction(String name) {
        if (factions.containsKey(name)) return false;
        factions.put(name, new HashSet<>());
        hostileRelations.put(name, new HashSet<>());
        save();
        return true;
    }
    
    /**
     * РЈРґР°Р»РёС‚СЊ С„СЂР°РєС†РёСЋ
     */
    public static boolean deleteFaction(String name) {
        if (!factions.containsKey(name)) return false;
        
        // РЈРґР°Р»СЏРµРј РІСЃРµС… С‡Р»РµРЅРѕРІ РёР· РєСЌС€Р°
        for (String member : factions.get(name)) {
            playerFactionCache.remove(member);
        }
        
        factions.remove(name);
        hostileRelations.remove(name);
        
        // РЈРґР°Р»СЏРµРј РёР· РІСЂР°Р¶РґРµР±РЅС‹С… РѕС‚РЅРѕС€РµРЅРёР№ РґСЂСѓРіРёС… С„СЂР°РєС†РёР№
        for (Set<String> enemies : hostileRelations.values()) {
            enemies.remove(name);
        }
        
        save();
        return true;
    }
    
    /**
     * Р”РѕР±Р°РІРёС‚СЊ РёРіСЂРѕРєР°/Р±РѕС‚Р° РІ С„СЂР°РєС†РёСЋ
     */
    public static boolean addMember(String faction, String playerName) {
        if (!factions.containsKey(faction)) return false;
        
        // РЈРґР°Р»СЏРµРј РёР· СЃС‚Р°СЂРѕР№ С„СЂР°РєС†РёРё
        String oldFaction = playerFactionCache.get(playerName);
        if (oldFaction != null && factions.containsKey(oldFaction)) {
            factions.get(oldFaction).remove(playerName);
        }
        
        factions.get(faction).add(playerName);
        playerFactionCache.put(playerName, faction);
        save();
        return true;
    }
    
    /**
     * РЈРґР°Р»РёС‚СЊ РёРіСЂРѕРєР°/Р±РѕС‚Р° РёР· С„СЂР°РєС†РёРё
     */
    public static boolean removeMember(String faction, String playerName) {
        if (!factions.containsKey(faction)) return false;
        boolean removed = factions.get(faction).remove(playerName);
        if (removed) {
            playerFactionCache.remove(playerName);
            save();
        }
        return removed;
    }
    
    /**
     * РЈСЃС‚Р°РЅРѕРІРёС‚СЊ РІСЂР°Р¶РґРµР±РЅС‹Рµ РѕС‚РЅРѕС€РµРЅРёСЏ РјРµР¶РґСѓ С„СЂР°РєС†РёСЏРјРё
     */
    public static boolean setHostile(String faction1, String faction2, boolean hostile) {
        if (!factions.containsKey(faction1) || !factions.containsKey(faction2)) return false;
        if (faction1.equals(faction2)) return false;
        
        // РЈР±РµРґРёРјСЃСЏ С‡С‚Рѕ Р·Р°РїРёСЃРё СЃСѓС‰РµСЃС‚РІСѓСЋС‚
        hostileRelations.computeIfAbsent(faction1, k -> new HashSet<>());
        hostileRelations.computeIfAbsent(faction2, k -> new HashSet<>());
        
        if (hostile) {
            hostileRelations.get(faction1).add(faction2);
            hostileRelations.get(faction2).add(faction1);
        } else {
            hostileRelations.get(faction1).remove(faction2);
            hostileRelations.get(faction2).remove(faction1);
        }
        save();
        return true;
    }
    
    /**
     * РџРѕР»СѓС‡РёС‚СЊ С„СЂР°РєС†РёСЋ РёРіСЂРѕРєР°
     */
    public static String getFaction(String playerName) {
        return playerFactionCache.get(playerName);
    }
    
    /**
     * РџСЂРѕРІРµСЂРёС‚СЊ СЏРІР»СЏСЋС‚СЃСЏ Р»Рё РґРІР° РёРіСЂРѕРєР° РІСЂР°РіР°РјРё
     */
    public static boolean areEnemies(String player1, String player2) {
        String faction1 = getFaction(player1);
        String faction2 = getFaction(player2);
        
        // Р•СЃР»Рё РєС‚Рѕ-С‚Рѕ Р±РµР· С„СЂР°РєС†РёРё - РЅРµ РІСЂР°РіРё РїРѕ С„СЂР°РєС†РёСЏРј
        if (faction1 == null || faction2 == null) return false;
        
        // Р•СЃР»Рё РІ РѕРґРЅРѕР№ С„СЂР°РєС†РёРё - СЃРѕСЋР·РЅРёРєРё
        if (faction1.equals(faction2)) return false;
        
        // РџСЂРѕРІРµСЂСЏРµРј РІСЂР°Р¶РґРµР±РЅРѕСЃС‚СЊ
        Set<String> enemies = hostileRelations.get(faction1);
        return enemies != null && enemies.contains(faction2);
    }
    
    /**
     * РџСЂРѕРІРµСЂРёС‚СЊ СЏРІР»СЏСЋС‚СЃСЏ Р»Рё РґРІР° РёРіСЂРѕРєР° СЃРѕСЋР·РЅРёРєР°РјРё (РІ РѕРґРЅРѕР№ С„СЂР°РєС†РёРё)
     */
    public static boolean areAllies(String player1, String player2) {
        String faction1 = getFaction(player1);
        String faction2 = getFaction(player2);
        
        if (faction1 == null || faction2 == null) return false;
        return faction1.equals(faction2);
    }
    
    /**
     * РџРѕР»СѓС‡РёС‚СЊ СЃРїРёСЃРѕРє РІСЃРµС… С„СЂР°РєС†РёР№
     */
    public static Set<String> getAllFactions() {
        return new HashSet<>(factions.keySet());
    }
    
    /**
     * РџРѕР»СѓС‡РёС‚СЊ С‡Р»РµРЅРѕРІ С„СЂР°РєС†РёРё
     */
    public static Set<String> getMembers(String faction) {
        Set<String> members = factions.get(faction);
        return members != null ? new HashSet<>(members) : new HashSet<>();
    }
    
    /**
     * РџРѕР»СѓС‡РёС‚СЊ РІСЂР°Р¶РґРµР±РЅС‹Рµ С„СЂР°РєС†РёРё
     */
    public static Set<String> getHostileFactions(String faction) {
        Set<String> enemies = hostileRelations.get(faction);
        return enemies != null ? new HashSet<>(enemies) : new HashSet<>();
    }
    
    // ============ РЎРѕС…СЂР°РЅРµРЅРёРµ/Р·Р°РіСЂСѓР·РєР° ============
    
    public static void load() {
        // РЎРѕР·РґР°С‘Рј РїР°РїРєСѓ config/pvpbot РµСЃР»Рё РЅРµ СЃСѓС‰РµСЃС‚РІСѓРµС‚
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("pvpbot");
        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            // РРіРЅРѕСЂРёСЂСѓРµРј
        }
        
        configPath = org.stepan1411.pvp_bot.config.WorldConfigHelper.getWorldConfigDir().resolve("factions.json");
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                FactionData data = GSON.fromJson(reader, FactionData.class);
                if (data != null) {
                    factions.clear();
                    hostileRelations.clear();
                    playerFactionCache.clear();
                    
                    if (data.factions != null) {
                        for (var entry : data.factions.entrySet()) {
                            factions.put(entry.getKey(), new HashSet<>(entry.getValue()));
                            for (String member : entry.getValue()) {
                                playerFactionCache.put(member, entry.getKey());
                            }
                        }
                    }
                    if (data.hostileRelations != null) {
                        for (var entry : data.hostileRelations.entrySet()) {
                            hostileRelations.put(entry.getKey(), new HashSet<>(entry.getValue()));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void save() {
        if (configPath == null) return;
        
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            FactionData data = new FactionData();
            data.factions = new HashMap<>();
            data.hostileRelations = new HashMap<>();
            
            for (var entry : factions.entrySet()) {
                data.factions.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            for (var entry : hostileRelations.entrySet()) {
                data.hostileRelations.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            
            GSON.toJson(data, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static class FactionData {
        Map<String, List<String>> factions;
        Map<String, List<String>> hostileRelations;
    }
}
