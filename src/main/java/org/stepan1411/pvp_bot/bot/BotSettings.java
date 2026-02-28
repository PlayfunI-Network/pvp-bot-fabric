package org.stepan1411.pvp_bot.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.stepan1411.pvp_bot.config.WorldConfigHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class BotSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static BotSettings INSTANCE;
    private static Path configPath;
    
    // РќР°СЃС‚СЂРѕР№РєРё СЌРєРёРїРёСЂРѕРІРєРё
    private boolean autoEquipArmor = true;
    private boolean autoEquipWeapon = true;
    
    // РќР°СЃС‚СЂРѕР№РєРё РІС‹Р±СЂР°СЃС‹РІР°РЅРёСЏ
    private boolean dropWorseArmor = false;
    private boolean dropWorseWeapons = false;
    private double dropDistance = 3.0;
    private int dropDelay = 20;
    
    // РРЅС‚РµСЂРІР°Р» РїСЂРѕРІРµСЂРєРё
    private int checkInterval = 20;
    
    // РњРёРЅРёРјР°Р»СЊРЅС‹Р№ СѓСЂРѕРІРµРЅСЊ Р±СЂРѕРЅРё
    private int minArmorLevel = 0;
    
    // ============ РќР°СЃС‚СЂРѕР№РєРё Р±РѕСЏ ============
    private boolean combatEnabled = true;
    private boolean revengeEnabled = true;        // РђС‚Р°РєРѕРІР°С‚СЊ С‚РѕРіРѕ РєС‚Рѕ Р°С‚Р°РєРѕРІР°Р» Р±РѕС‚Р°
    private boolean autoTargetEnabled = false;    // РђРІС‚РѕРјР°С‚РёС‡РµСЃРєРё РёСЃРєР°С‚СЊ РІСЂР°РіРѕРІ
    private boolean targetPlayers = true;         // РђС‚Р°РєРѕРІР°С‚СЊ РёРіСЂРѕРєРѕРІ
    private boolean targetHostileMobs = false;    // РђС‚Р°РєРѕРІР°С‚СЊ РІСЂР°Р¶РґРµР±РЅС‹С… РјРѕР±РѕРІ
    private boolean targetOtherBots = false;      // РђС‚Р°РєРѕРІР°С‚СЊ РґСЂСѓРіРёС… Р±РѕС‚РѕРІ
    
    // Р”РёСЃС‚Р°РЅС†РёРё
    private double maxTargetDistance = 64.0;      // РњР°РєСЃРёРјР°Р»СЊРЅР°СЏ РґРёСЃС‚Р°РЅС†РёСЏ РїРѕРёСЃРєР° С†РµР»Рё (СѓРІРµР»РёС‡РµРЅРѕ)
    private double meleeRange = 3.5;              // Р”РёСЃС‚Р°РЅС†РёСЏ Р±Р»РёР¶РЅРµРіРѕ Р±РѕСЏ
    private double rangedMinRange = 8.0;          // РњРёРЅРёРјР°Р»СЊРЅР°СЏ РґРёСЃС‚Р°РЅС†РёСЏ РґР»СЏ Р»СѓРєР°
    private double rangedOptimalRange = 20.0;     // РћРїС‚РёРјР°Р»СЊРЅР°СЏ РґРёСЃС‚Р°РЅС†РёСЏ РґР»СЏ Р»СѓРєР°
    private double maceRange = 6.0;               // Р”РёСЃС‚Р°РЅС†РёСЏ РґР»СЏ Р±СѓР»Р°РІС‹
    
    // РџР°СЂР°РјРµС‚СЂС‹ Р±РѕСЏ
    private int attackCooldown = 10;              // РљСѓР»РґР°СѓРЅ Р°С‚Р°РєРё РІ С‚РёРєР°С…
    private double moveSpeed = 1.0;               // РЎРєРѕСЂРѕСЃС‚СЊ РґРІРёР¶РµРЅРёСЏ
    private boolean criticalsEnabled = true;      // РљСЂРёС‚РёС‡РµСЃРєРёРµ СѓРґР°СЂС‹
    private int bowMinDrawTime = 15;              // РњРёРЅРёРјР°Р»СЊРЅРѕРµ РІСЂРµРјСЏ РЅР°С‚СЏР¶РµРЅРёСЏ Р»СѓРєР°
    
    // Р’РєР»СЋС‡РµРЅРёРµ С‚РёРїРѕРІ РѕСЂСѓР¶РёСЏ
    private boolean rangedEnabled = true;         // РСЃРїРѕР»СЊР·РѕРІР°С‚СЊ Р»СѓРє/Р°СЂР±Р°Р»РµС‚
    private boolean maceEnabled = true;           // РСЃРїРѕР»СЊР·РѕРІР°С‚СЊ Р±СѓР»Р°РІСѓ
    private boolean spearEnabled = false;         // РСЃРїРѕР»СЊР·РѕРІР°С‚СЊ РєРѕРїСЊС‘ (1.21.11) - Р’Р«РљР›Р®Р§Р•РќРћ РёР·-Р·Р° Р±Р°РіР° Carpet
    private boolean crystalPvpEnabled = true;     // РСЃРїРѕР»СЊР·РѕРІР°С‚СЊ Crystal PVP (РѕР±СЃРёРґРёР°РЅ + РєСЂРёСЃС‚Р°Р»Р»)
    private boolean anchorPvpEnabled = true;      // РСЃРїРѕР»СЊР·РѕРІР°С‚СЊ Anchor PVP (СЏРєРѕСЂСЊ + glowstone)
    
    // РќР°СЃС‚СЂРѕР№РєРё РєРѕРїСЊСЏ (Spear) - 1.21.11
    private double spearRange = 4.5;              // Р”РёСЃС‚Р°РЅС†РёСЏ РґР»СЏ jab Р°С‚Р°РєРё
    private double spearChargeRange = 12.0;       // Р”РёСЃС‚Р°РЅС†РёСЏ РґР»СЏ charge Р°С‚Р°РєРё
    private int spearMinChargeTime = 15;          // РњРёРЅРёРјР°Р»СЊРЅРѕРµ РІСЂРµРјСЏ Р·Р°СЂСЏРґР°
    private int spearMaxChargeTime = 40;          // РњР°РєСЃРёРјР°Р»СЊРЅРѕРµ РІСЂРµРјСЏ Р·Р°СЂСЏРґР°
    
    // ============ РЈС‚РёР»РёС‚С‹ ============
    private boolean autoTotemEnabled = true;      // РђРІС‚Рѕ-С‚РѕС‚РµРј РІ offhand
    private boolean totemPriority = true;         // РџСЂРёРѕСЂРёС‚РµС‚ С‚РѕС‚РµРјР° (РЅРµ Р·Р°РјРµРЅСЏС‚СЊ РЅР° С‰РёС‚)
    private boolean autoEatEnabled = true;        // РђРІС‚Рѕ-РµРґР°
    private boolean autoShieldEnabled = true;     // РђРІС‚Рѕ-С‰РёС‚
    private boolean autoMendEnabled = true;       // РђРІС‚Рѕ-СЂРµРјРѕРЅС‚ Р±СЂРѕРЅРё СЃ Mending С‡РµСЂРµР· XP Р±СѓС‚С‹Р»РєРё
    private double mendDurabilityThreshold = 0.25; // РџРѕСЂРѕРі РїСЂРѕС‡РЅРѕСЃС‚Рё РґР»СЏ СЂРµРјРѕРЅС‚Р° (0.25 = 25%)
    private double shieldHealthThreshold = 0.5;   // РџРѕСЂРѕРі HP РґР»СЏ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёСЏ С‰РёС‚Р° (0.5 = 50%)
    private boolean shieldBreakEnabled = true;    // РЎР±РёРІР°С‚СЊ С‰РёС‚ С‚РѕРїРѕСЂРѕРј
    private boolean preferSword = true;           // РџСЂРµРґРїРѕС‡РёС‚Р°С‚СЊ РјРµС‡ РІРјРµСЃС‚Рѕ С‚РѕРїРѕСЂР°
    private int minHungerToEat = 14;              // РњРёРЅРёРјР°Р»СЊРЅС‹Р№ РіРѕР»РѕРґ РґР»СЏ РµРґС‹
    private boolean autoPotionEnabled = true;     // РђРІС‚Рѕ-Р·РµР»СЊСЏ РёСЃС†РµР»РµРЅРёСЏ
    private boolean cobwebEnabled = true;         // РСЃРїРѕР»СЊР·РѕРІР°С‚СЊ РїР°СѓС‚РёРЅСѓ
    
    // ============ РќР°РІРёРіР°С†РёСЏ Рё РґРІРёР¶РµРЅРёРµ ============
    private boolean retreatEnabled = false;       // РћС‚СЃС‚СѓРїР°С‚СЊ РїСЂРё РЅРёР·РєРѕРј HP (Р’Р«РљР›Р®Р§Р•РќРћ РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ)
    private double retreatHealthPercent = 0.3;    // РџСЂРѕС†РµРЅС‚ HP РґР»СЏ РѕС‚СЃС‚СѓРїР»РµРЅРёСЏ (0.3 = 30%)
    private double criticalHealthPercent = 0.15;  // РљСЂРёС‚РёС‡РµСЃРєРёР№ HP РґР»СЏ РѕС‚СЃС‚СѓРїР»РµРЅРёСЏ РґР°Р¶Рµ СЃРѕ СЃР±РёС‚С‹Рј С‰РёС‚РѕРј (0.15 = 15%)
    private boolean bhopEnabled = true;           // Bunny hop (РїСЂС‹Р¶РєРё РїСЂРё Р±РµРіРµ)
    private int bhopCooldown = 12;                // РљСѓР»РґР°СѓРЅ РјРµР¶РґСѓ РїСЂС‹Р¶РєР°РјРё (С‚РёРєРё)
    private double jumpBoost = 0.0;               // Р”РѕРїРѕР»РЅРёС‚РµР»СЊРЅР°СЏ РІС‹СЃРѕС‚Р° РїСЂС‹Р¶РєР° (0.0 - 0.5)
    private boolean idleWanderEnabled = false;     // Р‘СЂРѕРґРёС‚СЊ РєРѕРіРґР° РЅРµС‚ С†РµР»Рё
    private double idleWanderRadius = 10.0;       // Р Р°РґРёСѓСЃ Р±Р»СѓР¶РґР°РЅРёСЏ
    
    // ============ Р¤СЂР°РєС†РёРё Рё РѕС€РёР±РєРё ============
    private boolean factionsEnabled = true;       // РСЃРїРѕР»СЊР·РѕРІР°С‚СЊ СЃРёСЃС‚РµРјСѓ С„СЂР°РєС†РёР№
    private boolean friendlyFireEnabled = false;  // РЈСЂРѕРЅ РїРѕ СЃРѕСЋР·РЅРёРєР°Рј (false = РЅРµР»СЊР·СЏ Р±РёС‚СЊ СЃРѕСЋР·РЅРёРєРѕРІ)
    private int missChance = 10;                  // РЁР°РЅСЃ РїСЂРѕРјР°С…Р° (0-100%)
    private int mistakeChance = 5;                // РЁР°РЅСЃ РѕС€РёР±РєРё (0-100%)
    private int reactionDelay = 0;                // Р—Р°РґРµСЂР¶РєР° СЂРµР°РєС†РёРё РІ С‚РёРєР°С… (0-20)
    private boolean botsRelogs = true;            // Р‘РѕС‚С‹ СЂРµСЃРїР°РІРЅСЏС‚СЃСЏ РїРѕСЃР»Рµ СЂРµСЃС‚Р°СЂС‚Р° СЃРµСЂРІРµСЂР°
    
    // ============ РЎС‚Р°С‚РёСЃС‚РёРєР° ============
    private boolean sendStats = true;             // РћС‚РїСЂР°РІР»СЏС‚СЊ Р°РЅРѕРЅРёРјРЅСѓСЋ СЃС‚Р°С‚РёСЃС‚РёРєСѓ
    
    private BotSettings() {}
    
    public static BotSettings get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }
    
    public static void load() {
        // РЎРѕР·РґР°С‘Рј РїР°РїРєСѓ config/pvpbot РµСЃР»Рё РЅРµ СЃСѓС‰РµСЃС‚РІСѓРµС‚
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("pvpbot");
        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            // РРіРЅРѕСЂРёСЂСѓРµРј
        }
        
        configPath = WorldConfigHelper.getWorldConfigDir().resolve("settings.json");
        
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                INSTANCE = GSON.fromJson(reader, BotSettings.class);
                if (INSTANCE == null) {
                    INSTANCE = new BotSettings();
                }
            } catch (Exception e) {
                INSTANCE = new BotSettings();
            }
        } else {
            INSTANCE = new BotSettings();
            save();
        }
    }

    
    public static void save() {
        if (INSTANCE == null || configPath == null) return;
        
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Getters - Equipment
    public boolean isAutoEquipArmor() { return autoEquipArmor; }
    public boolean isAutoEquipWeapon() { return autoEquipWeapon; }
    public boolean isDropWorseArmor() { return dropWorseArmor; }
    public boolean isDropWorseWeapons() { return dropWorseWeapons; }
    public double getDropDistance() { return dropDistance; }
    public int getDropDelay() { return dropDelay; }
    public int getCheckInterval() { return checkInterval; }
    public int getMinArmorLevel() { return minArmorLevel; }
    
    // Getters - Combat
    public boolean isCombatEnabled() { return combatEnabled; }
    public boolean isRevengeEnabled() { return revengeEnabled; }
    public boolean isAutoTargetEnabled() { return autoTargetEnabled; }
    public boolean isTargetPlayers() { return targetPlayers; }
    public boolean isTargetHostileMobs() { return targetHostileMobs; }
    public boolean isTargetOtherBots() { return targetOtherBots; }
    public double getMaxTargetDistance() { return maxTargetDistance; }
    public double getMeleeRange() { return meleeRange; }
    public double getRangedMinRange() { return rangedMinRange; }
    public double getRangedOptimalRange() { return rangedOptimalRange; }
    public double getMaceRange() { return maceRange; }
    public int getAttackCooldown() { return attackCooldown; }
    public double getMoveSpeed() { return moveSpeed; }
    public boolean isCriticalsEnabled() { return criticalsEnabled; }
    public int getBowMinDrawTime() { return bowMinDrawTime; }
    public boolean isRangedEnabled() { return rangedEnabled; }
    public boolean isMaceEnabled() { return maceEnabled; }
    public boolean isSpearEnabled() { return spearEnabled; }
    public boolean isCrystalPvpEnabled() { return crystalPvpEnabled; }
    public boolean isAnchorPvpEnabled() { return anchorPvpEnabled; }
    public double getSpearRange() { return spearRange; }
    public double getSpearChargeRange() { return spearChargeRange; }
    public int getSpearMinChargeTime() { return spearMinChargeTime; }
    public int getSpearMaxChargeTime() { return spearMaxChargeTime; }
    
    // Getters - Utils
    public boolean isAutoTotemEnabled() { return autoTotemEnabled; }
    public boolean isTotemPriority() { return totemPriority; }
    public boolean isAutoEatEnabled() { return autoEatEnabled; }
    public boolean isAutoShieldEnabled() { return autoShieldEnabled; }
    public boolean isAutoMendEnabled() { return autoMendEnabled; }
    public double getMendDurabilityThreshold() { return mendDurabilityThreshold; }
    public double getShieldHealthThreshold() { return shieldHealthThreshold; }
    public boolean isShieldBreakEnabled() { return shieldBreakEnabled; }
    public boolean isPreferSword() { return preferSword; }
    public int getMinHungerToEat() { return minHungerToEat; }
    public boolean isAutoPotionEnabled() { return autoPotionEnabled; }
    public boolean isCobwebEnabled() { return cobwebEnabled; }
    
    // Getters - Navigation
    public boolean isRetreatEnabled() { return retreatEnabled; }
    public double getRetreatHealthPercent() { return retreatHealthPercent; }
    public double getCriticalHealthPercent() { return criticalHealthPercent; }
    public boolean isBhopEnabled() { return bhopEnabled; }
    public int getBhopCooldown() { return bhopCooldown; }
    public double getJumpBoost() { return jumpBoost; }
    public boolean isIdleWanderEnabled() { return idleWanderEnabled; }
    public double getIdleWanderRadius() { return idleWanderRadius; }
    
    // Getters - Factions & Mistakes
    public boolean isFactionsEnabled() { return factionsEnabled; }
    public boolean isFriendlyFireEnabled() { return friendlyFireEnabled; }
    public int getMissChance() { return missChance; }
    public int getMistakeChance() { return mistakeChance; }
    public int getReactionDelay() { return reactionDelay; }
    public boolean isBotsRelogs() { return botsRelogs; }
    
    // Getters - Stats
    public boolean isSendStats() { return sendStats; }
    
    // Setters (СЃ Р°РІС‚РѕСЃРѕС…СЂР°РЅРµРЅРёРµРј)
    public void setAutoEquipArmor(boolean value) { 
        this.autoEquipArmor = value; 
        save();
    }
    public void setAutoEquipWeapon(boolean value) { 
        this.autoEquipWeapon = value; 
        save();
    }
    public void setDropWorseArmor(boolean value) { 
        this.dropWorseArmor = value; 
        save();
    }
    public void setDropWorseWeapons(boolean value) { 
        this.dropWorseWeapons = value; 
        save();
    }
    public void setDropDistance(double value) { 
        this.dropDistance = Math.max(1.0, Math.min(10.0, value)); 
        save();
    }
    public void setDropDelay(int value) { 
        this.dropDelay = Math.max(1, Math.min(200, value)); 
        save();
    }
    public void setCheckInterval(int value) { 
        this.checkInterval = Math.max(1, Math.min(100, value)); 
        save();
    }
    public void setMinArmorLevel(int value) { 
        this.minArmorLevel = Math.max(0, Math.min(100, value)); 
        save();
    }
    
    // Setters - Combat
    public void setCombatEnabled(boolean value) { this.combatEnabled = value; save(); }
    public void setRevengeEnabled(boolean value) { this.revengeEnabled = value; save(); }
    public void setAutoTargetEnabled(boolean value) { this.autoTargetEnabled = value; save(); }
    public void setTargetPlayers(boolean value) { this.targetPlayers = value; save(); }
    public void setTargetHostileMobs(boolean value) { this.targetHostileMobs = value; save(); }
    public void setTargetOtherBots(boolean value) { this.targetOtherBots = value; save(); }
    
    public void setMaxTargetDistance(double value) { 
        this.maxTargetDistance = Math.max(5.0, Math.min(128.0, value)); 
        save(); 
    }
    public void setMeleeRange(double value) { 
        this.meleeRange = Math.max(2.0, Math.min(6.0, value)); 
        save(); 
    }
    public void setRangedMinRange(double value) { 
        this.rangedMinRange = Math.max(3.0, Math.min(20.0, value)); 
        save(); 
    }
    public void setRangedOptimalRange(double value) { 
        this.rangedOptimalRange = Math.max(10.0, Math.min(50.0, value)); 
        save(); 
    }
    public void setMaceRange(double value) { 
        this.maceRange = Math.max(3.0, Math.min(10.0, value)); 
        save(); 
    }
    public void setAttackCooldown(int value) { 
        this.attackCooldown = Math.max(1, Math.min(40, value)); 
        save(); 
    }
    public void setMoveSpeed(double value) { 
        this.moveSpeed = Math.max(0.1, Math.min(2.0, value)); 
        save(); 
    }
    public void setCriticalsEnabled(boolean value) { this.criticalsEnabled = value; save(); }
    public void setBowMinDrawTime(int value) { 
        this.bowMinDrawTime = Math.max(5, Math.min(30, value)); 
        save(); 
    }
    public void setRangedEnabled(boolean value) { this.rangedEnabled = value; save(); }
    public void setMaceEnabled(boolean value) { this.maceEnabled = value; save(); }
    public void setSpearEnabled(boolean value) { this.spearEnabled = value; save(); }
    public void setCrystalPvpEnabled(boolean value) { this.crystalPvpEnabled = value; save(); }
    public void setAnchorPvpEnabled(boolean value) { this.anchorPvpEnabled = value; save(); }
    public void setSpearRange(double value) { 
        this.spearRange = Math.max(2.0, Math.min(8.0, value)); 
        save(); 
    }
    public void setSpearChargeRange(double value) { 
        this.spearChargeRange = Math.max(5.0, Math.min(20.0, value)); 
        save(); 
    }
    public void setSpearMinChargeTime(int value) { 
        this.spearMinChargeTime = Math.max(5, Math.min(30, value)); 
        save(); 
    }
    public void setSpearMaxChargeTime(int value) { 
        this.spearMaxChargeTime = Math.max(20, Math.min(60, value)); 
        save(); 
    }
    
    // Setters - Utils
    public void setAutoTotemEnabled(boolean value) { this.autoTotemEnabled = value; save(); }
    public void setTotemPriority(boolean value) { this.totemPriority = value; save(); }
    public void setAutoEatEnabled(boolean value) { this.autoEatEnabled = value; save(); }
    public void setAutoShieldEnabled(boolean value) { this.autoShieldEnabled = value; save(); }
    public void setAutoMendEnabled(boolean value) { this.autoMendEnabled = value; save(); }
    public void setMendDurabilityThreshold(double value) { 
        this.mendDurabilityThreshold = Math.max(0.1, Math.min(0.9, value)); 
        save(); 
    }
    public void setShieldHealthThreshold(double value) { 
        this.shieldHealthThreshold = Math.max(0.1, Math.min(1.0, value)); 
        save(); 
    }
    public void setShieldBreakEnabled(boolean value) { this.shieldBreakEnabled = value; save(); }
    public void setPreferSword(boolean value) { this.preferSword = value; save(); }
    public void setMinHungerToEat(int value) { 
        this.minHungerToEat = Math.max(1, Math.min(20, value)); 
        save(); 
    }
    public void setAutoPotionEnabled(boolean value) { this.autoPotionEnabled = value; save(); }
    public void setCobwebEnabled(boolean value) { this.cobwebEnabled = value; save(); }
    
    // Setters - Navigation
    public void setRetreatEnabled(boolean value) { this.retreatEnabled = value; save(); }
    public void setRetreatHealthPercent(double value) { 
        this.retreatHealthPercent = Math.max(0.1, Math.min(0.9, value)); 
        save(); 
    }
    public void setCriticalHealthPercent(double value) { 
        this.criticalHealthPercent = Math.max(0.05, Math.min(0.5, value)); 
        save(); 
    }
    public void setBhopEnabled(boolean value) { this.bhopEnabled = value; save(); }
    public void setBhopCooldown(int value) { 
        this.bhopCooldown = Math.max(5, Math.min(30, value)); 
        save(); 
    }
    public void setJumpBoost(double value) { 
        this.jumpBoost = Math.max(0.0, Math.min(0.5, value)); 
        save(); 
    }
    public void setIdleWanderEnabled(boolean value) { this.idleWanderEnabled = value; save(); }
    public void setIdleWanderRadius(double value) { 
        this.idleWanderRadius = Math.max(3.0, Math.min(50.0, value)); 
        save(); 
    }
    
    // Setters - Factions & Mistakes
    public void setFactionsEnabled(boolean value) { this.factionsEnabled = value; save(); }
    public void setFriendlyFireEnabled(boolean value) { this.friendlyFireEnabled = value; save(); }
    public void setMissChance(int value) { 
        this.missChance = Math.max(0, Math.min(100, value)); 
        save(); 
    }
    public void setMistakeChance(int value) { 
        this.mistakeChance = Math.max(0, Math.min(100, value)); 
        save(); 
    }
    public void setReactionDelay(int value) { 
        this.reactionDelay = Math.max(0, Math.min(20, value)); 
        save(); 
    }
    public void setBotsRelogs(boolean value) { this.botsRelogs = value; save(); }
}
