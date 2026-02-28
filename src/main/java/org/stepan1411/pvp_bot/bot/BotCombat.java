package org.stepan1411.pvp_bot.bot;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BotCombat {
    
    // РҐСЂР°РЅРµРЅРёРµ СЃРѕСЃС‚РѕСЏРЅРёСЏ Р±РѕСЏ РґР»СЏ РєР°Р¶РґРѕРіРѕ Р±РѕС‚Р°
    private static final Map<String, CombatState> combatStates = new HashMap<>();
    
    public static class CombatState {
        public Entity target = null;
        public String forcedTargetName = null; // РџСЂРёРЅСѓРґРёС‚РµР»СЊРЅР°СЏ С†РµР»СЊ РїРѕ РєРѕРјР°РЅРґРµ
        public int attackCooldown = 0;
        public int bowDrawTicks = 0;
        public boolean isDrawingBow = false;
        public Entity lastAttacker = null;
        public long lastAttackTime = 0;
        public WeaponMode currentMode = WeaponMode.MELEE;
        public float lastHealth = 20.0f; // Р”Р»СЏ РѕС‚СЃР»РµР¶РёРІР°РЅРёСЏ СѓСЂРѕРЅР°
        public boolean isRetreating = false; // РћС‚СЃС‚СѓРїР°РµС‚ РґР»СЏ Р»РµС‡РµРЅРёСЏ
        
        // РЎРѕСЃС‚РѕСЏРЅРёРµ РєРѕРїСЊСЏ (Spear) - 1.21.11
        public boolean isChargingSpear = false;
        public int spearChargeTicks = 0;
        
        // РЎРѕСЃС‚РѕСЏРЅРёРµ РїР°СѓС‚РёРЅС‹
        public int cobwebCooldown = 0; // РљСѓР»РґР°СѓРЅ РЅР° СЂР°Р·РјРµС‰РµРЅРёРµ РїР°СѓС‚РёРЅС‹
        
        // РЎРѕСЃС‚РѕСЏРЅРёРµ С‰РёС‚Р°
        public boolean isUsingShield = false; // Р©РёС‚ Р°РєС‚РёРІРµРЅ С‡РµСЂРµР· Carpet РєРѕРјР°РЅРґСѓ
        public int shieldToggleCooldown = 0; // РљСѓР»РґР°СѓРЅ РЅР° РїРµСЂРµРєР»СЋС‡РµРЅРёРµ С‰РёС‚Р° (РїСЂРµРґРѕС‚РІСЂР°С‰Р°РµС‚ СЃРїР°Рј)
        public int airTimeTicks = 0; // РЎРєРѕР»СЊРєРѕ С‚РёРєРѕРІ Р±РѕС‚ РІ РІРѕР·РґСѓС…Рµ
        public boolean shieldBroken = false; // Р©РёС‚ Р±С‹Р» СЃР±РёС‚ С‚РѕРїРѕСЂРѕРј
        public long shieldBrokenTime = 0; // Р’СЂРµРјСЏ РєРѕРіРґР° С‰РёС‚ Р±С‹Р» СЃР±РёС‚
        public boolean isPlacingCobweb = false; // РџСЂРѕС†РµСЃСЃ СЂР°Р·РјРµС‰РµРЅРёСЏ РїР°СѓС‚РёРЅС‹
        public int cobwebPlaceTicks = 0; // РўРёРєРё СЂР°Р·РјРµС‰РµРЅРёСЏ
        
        // РЎРѕСЃС‚РѕСЏРЅРёРµ Crystal PVP
        public boolean isCrystalPvping = false; // Р РµР¶РёРј Crystal PVP Р°РєС‚РёРІРµРЅ
        public int crystalCooldown = 0; // РљСѓР»РґР°СѓРЅ РјРµР¶РґСѓ РєСЂРёСЃС‚Р°Р»Р»Р°РјРё
        public net.minecraft.util.math.BlockPos lastObsidianPos = null; // РџРѕСЃР»РµРґРЅРёР№ РїРѕСЃС‚Р°РІР»РµРЅРЅС‹Р№ РѕР±СЃРёРґРёР°РЅ
        public int crystalPvpStep = 0; // РўРµРєСѓС‰РёР№ С€Р°Рі: 0=СЃС‚Р°РІРёРј РѕР±СЃРёРґРёР°РЅ, 1=СЃС‚Р°РІРёРј РєСЂРёСЃС‚Р°Р»Р», 2=Р±СЊС‘Рј РєСЂРёСЃС‚Р°Р»Р»
        public int crystalPvpTicks = 0; // РўРёРєРё РЅР° С‚РµРєСѓС‰РµРј С€Р°РіРµ
        
        public enum WeaponMode {
            MELEE,      // Р‘Р»РёР¶РЅРёР№ Р±РѕР№ (РјРµС‡/С‚РѕРїРѕСЂ)
            RANGED,     // Р”Р°Р»СЊРЅРёР№ Р±РѕР№ (Р»СѓРє/Р°СЂР±Р°Р»РµС‚)
            MACE,       // Р‘СѓР»Р°РІР° (РїСЂС‹Р¶РѕРє + СѓРґР°СЂ)
            SPEAR,      // РљРѕРїСЊС‘ (charge + jab) - 1.21.11
            CRYSTAL,    // Crystal PVP (РѕР±СЃРёРґРёР°РЅ + РєСЂРёСЃС‚Р°Р»Р» + СѓРґР°СЂ)
            ANCHOR      // Anchor PVP (СЏРєРѕСЂСЊ + glowstone + РІР·СЂС‹РІ)
        }
    }
    
    public static CombatState getState(String botName) {
        return combatStates.computeIfAbsent(botName, k -> new CombatState());
    }
    
    public static void removeState(String botName) {
        combatStates.remove(botName);
    }
    
    /**
     * РћСЃРЅРѕРІРЅРѕР№ РјРµС‚РѕРґ РѕР±РЅРѕРІР»РµРЅРёСЏ Р±РѕСЏ - РІС‹Р·С‹РІР°РµС‚СЃСЏ РєР°Р¶РґС‹Р№ С‚РёРє
     */
    public static void update(ServerPlayerEntity bot, net.minecraft.server.MinecraftServer server) {
        BotSettings settings = BotSettings.get();
        if (!settings.isCombatEnabled()) return;
        
        CombatState state = getState(bot.getName().getString());
        
        // РџСЂРѕРІРµСЂСЏРµРј РїРѕР»СѓС‡РёР» Р»Рё Р±РѕС‚ СѓСЂРѕРЅ (Р°Р»СЊС‚РµСЂРЅР°С‚РёРІР° mixin)
        float currentHealth = bot.getHealth();
        if (currentHealth < state.lastHealth && settings.isRevengeEnabled()) {
            // Р‘РѕС‚ РїРѕР»СѓС‡РёР» СѓСЂРѕРЅ - РёС‰РµРј РєС‚Рѕ Р°С‚Р°РєРѕРІР°Р»
            Entity attacker = bot.getAttacker();
            if (attacker != null && attacker != bot && attacker instanceof LivingEntity) {
                state.lastAttacker = attacker;
                state.lastAttackTime = System.currentTimeMillis();
            }
        }
        state.lastHealth = currentHealth;
        
        // РЈРјРµРЅСЊС€Р°РµРј РєСѓР»РґР°СѓРЅ
        if (state.attackCooldown > 0) {
            state.attackCooldown--;
        }
        if (state.shieldToggleCooldown > 0) {
            state.shieldToggleCooldown--;
        }
        if (state.cobwebCooldown > 0) {
            state.cobwebCooldown--;
        }
        if (state.crystalCooldown > 0) {
            state.crystalCooldown--;
        }
        
        // РќР°С…РѕРґРёРј С†РµР»СЊ
        Entity target = findTarget(bot, state, settings, server);
        state.target = target;
        
        // === DEBUG: РџРѕРєР°Р·С‹РІР°РµРј С…РёС‚Р±РѕРєСЃ С†РµР»Рё ===
        if (target != null) {
            BotDebug.showTargetEntity(bot, target);
        }
        // ======================================
        
        // РћР±СЂР°Р±РѕС‚РєР° СЂР°Р·РјРµС‰РµРЅРёСЏ РїР°СѓС‚РёРЅС‹ (РїСЂРёРѕСЂРёС‚РµС‚ РЅР°Рґ РІСЃРµРј)
        if (state.isPlacingCobweb && target != null) {
            handleCobwebPlacement(bot, target, state, server);
            return; // РќРµ РґРµР»Р°РµРј РЅРёС‡РµРіРѕ РґСЂСѓРіРѕРіРѕ РїРѕРєР° СЃС‚Р°РІРёРј РїР°СѓС‚РёРЅСѓ
        }
        
        if (target == null) {
            // РќРµС‚ С†РµР»Рё - РїСЂРµРєСЂР°С‰Р°РµРј РЅР°С‚СЏРіРёРІР°С‚СЊ Р»СѓРє
            if (state.isDrawingBow) {
                stopUsingBow(bot, state);
            }
            // Idle Р±Р»СѓР¶РґР°РЅРёРµ РєРѕРіРґР° РЅРµС‚ С†РµР»Рё
            BotNavigation.idleWander(bot);
            return;
        }
        
        // Р•СЃС‚СЊ С†РµР»СЊ - СЃР±СЂР°СЃС‹РІР°РµРј idle
        BotNavigation.resetIdle(bot.getName().getString());
        
        // РћРїСЂРµРґРµР»СЏРµРј РґРёСЃС‚Р°РЅС†РёСЋ РґРѕ С†РµР»Рё
        double distance = bot.distanceTo(target);
        
        // РџСЂРѕРІРµСЂСЏРµРј РЅСѓР¶РЅРѕ Р»Рё РѕС‚СЃС‚СѓРїР°С‚СЊ (РЅРёР·РєРѕРµ HP)
        float health = bot.getHealth();
        float maxHealth = bot.getMaxHealth();
        float healthPercent = health / maxHealth;
        boolean lowHealth = healthPercent <= settings.getRetreatHealthPercent();
        boolean criticalHealth = healthPercent <= settings.getCriticalHealthPercent();
        
        // РџСЂРѕРІРµСЂСЏРµРј РµСЃС‚СЊ Р»Рё РµРґР° РґР»СЏ РѕС‚СЃС‚СѓРїР»РµРЅРёСЏ
        boolean hasFood = BotUtils.hasFood(bot);
        
        // РЎР±СЂР°СЃС‹РІР°РµРј С„Р»Р°Рі СЃР±РёС‚РѕРіРѕ С‰РёС‚Р° С‡РµСЂРµР· 5 СЃРµРєСѓРЅРґ
        if (state.shieldBroken && System.currentTimeMillis() - state.shieldBrokenTime > 5000) {
            state.shieldBroken = false;
        }
        
        // РџСЂРѕРІРµСЂСЏРµРј РµСЃС‚ Р»Рё Р±РѕС‚ - РќРРљРћР“Р”Рђ РЅРµ РїРµСЂРµРєР»СЋС‡Р°РµРј СЃР»РѕС‚С‹ РїРѕРєР° РµРґРёРј!
        var utilsState = BotUtils.getState(bot.getName().getString());
        boolean isEating = utilsState.isEating;
        
        if (isEating && settings.isRetreatEnabled()) {
            // Р‘РѕС‚ РµСЃС‚ Р retreat РІРєР»СЋС‡С‘РЅ - РѕС‚СЃС‚СѓРїР°РµРј
            state.isRetreating = true;
            if (state.isDrawingBow) {
                stopUsingBow(bot, state);
            }
            // РЈР±РµРіР°РµРј РѕС‚ РІСЂР°РіР° СЃ РЅР°РІРёРіР°С†РёРµР№ (СЃРєРѕСЂРѕСЃС‚СЊ 1.2 = Р±С…РѕРї РІРєР»СЋС‡С‘РЅ)
            BotNavigation.lookAway(bot, target);
            BotNavigation.moveAway(bot, target, 1.2);
            return;
        } else if (isEating) {
            // Р‘РѕС‚ РµСЃС‚ РќРћ retreat РІС‹РєР»СЋС‡РµРЅ - РїСЂРѕСЃС‚Рѕ РЅРµ РїРµСЂРµРєР»СЋС‡Р°РµРј СЃР»РѕС‚С‹, РїСЂРѕРґРѕР»Р¶Р°РµРј РґСЂР°С‚СЊСЃСЏ
            if (state.isDrawingBow) {
                stopUsingBow(bot, state);
            }
            // РќРµ РѕС‚СЃС‚СѓРїР°РµРј, РїСЂРѕРґРѕР»Р¶Р°РµРј Р±РѕР№
        }
        
        // РџСЂРѕР±СѓРµРј РёСЃРїРѕР»СЊР·РѕРІР°С‚СЊ Р·РµР»СЊРµ РёСЃС†РµР»РµРЅРёСЏ РµСЃР»Рё РЅРёР·РєРѕРµ HP
        if (lowHealth && settings.isAutoPotionEnabled()) {
            if (BotUtils.tryUseHealingPotion(bot, server)) {
                // РСЃРїРѕР»СЊР·СѓРµРј Р·РµР»СЊРµ - РЅРµ РѕС‚СЃС‚СѓРїР°РµРј РїРѕРєР° РїСЊС‘Рј
                return;
            }
        }
        
        // Р›РѕРіРёРєР° РѕС‚СЃС‚СѓРїР»РµРЅРёСЏ:
        // 1. Р•СЃР»Рё HP РєСЂРёС‚РёС‡РµСЃРєРѕРµ (< 15%) - Р’РЎР•Р“Р”Рђ РѕС‚СЃС‚СѓРїР°РµРј, РґР°Р¶Рµ РµСЃР»Рё С‰РёС‚ СЃР±РёС‚
        // 2. Р•СЃР»Рё HP РЅРёР·РєРѕРµ (< 30%) Р С‰РёС‚ РќР• СЃР±РёС‚ - РѕС‚СЃС‚СѓРїР°РµРј
        // 3. Р•СЃР»Рё С‰РёС‚ СЃР±РёС‚ - РїСЂРѕРґРѕР»Р¶Р°РµРј РґСЂР°С‚СЊСЃСЏ РїРѕРєР° HP РЅРµ СЃС‚Р°РЅРµС‚ РєСЂРёС‚РёС‡РµСЃРєРёРј
        boolean shouldRetreat = settings.isRetreatEnabled() && hasFood && 
                               (criticalHealth || (lowHealth && !state.shieldBroken));
        
        // РћС‚СЃС‚СѓРїР°РµРј РµСЃР»Рё РЅРёР·РєРѕРµ HP, РІРєР»СЋС‡РµРЅРѕ РѕС‚СЃС‚СѓРїР»РµРЅРёРµ Р РµСЃС‚СЊ РµРґР°
        // Р•СЃР»Рё РµРґС‹ РЅРµС‚ - РЅРµС‚ СЃРјС‹СЃР»Р° РѕС‚СЃС‚СѓРїР°С‚СЊ, Р»СѓС‡С€Рµ РґСЂР°С‚СЊСЃСЏ РґРѕ РєРѕРЅС†Р°
        if (shouldRetreat) {
            state.isRetreating = true;
            
            // РСЃРїРѕР»СЊР·СѓРµРј С‰РёС‚ РїСЂРё РѕС‚СЃС‚СѓРїР»РµРЅРёРё РґР»СЏ Р·Р°С‰РёС‚С‹
            if (settings.isAutoShieldEnabled()) {
                var inventory = bot.getInventory();
                // Р­РєРёРїРёСЂСѓРµРј С‰РёС‚ РІ offhand РµСЃР»Рё РµРіРѕ С‚Р°Рј РЅРµС‚
                ItemStack offhandItem = bot.getOffHandStack();
                if (offhandItem.isEmpty() || !offhandItem.getItem().toString().contains("shield")) {
                    int shieldSlot = findShield(inventory);
                    if (shieldSlot >= 0) {
                        // РџРµСЂРµРјРµС‰Р°РµРј С‰РёС‚ РІ offhand (СЃР»РѕС‚ 40)
                        ItemStack shield = inventory.getStack(shieldSlot);
                        inventory.setStack(40, shield);
                        inventory.setStack(shieldSlot, ItemStack.EMPTY);
                    }
                }
                
                // РџРѕРґРЅРёРјР°РµРј С‰РёС‚ РїСЂРё РѕС‚СЃС‚СѓРїР»РµРЅРёРё
                if (!state.isUsingShield && state.shieldToggleCooldown <= 0) {
                    startUsingShield(bot, server);
                    state.isUsingShield = true;
                }
            }
            
            // РЈР±РµРіР°РµРј РїРѕРєР° РІСЂР°Рі Р±Р»РёР¶Рµ 25 Р±Р»РѕРєРѕРІ (СЃРєРѕСЂРѕСЃС‚СЊ 1.5 = РјР°РєСЃРёРјР°Р»СЊРЅС‹Р№ Р±С…РѕРї)
            if (distance < 25.0) {
                // РџСЂРѕР±СѓРµРј РїРѕСЃС‚Р°РІРёС‚СЊ РїР°СѓС‚РёРЅСѓ РїРѕРґ РІСЂР°РіР° РїСЂРё РѕС‚СЃС‚СѓРїР»РµРЅРёРё
                if (settings.isCobwebEnabled() && distance < 8.0 && state.cobwebCooldown <= 0 && !state.isPlacingCobweb) {
                    tryPlaceCobweb(bot, target, server);
                }
                BotNavigation.lookAway(bot, target);
                BotNavigation.moveAway(bot, target, 1.5);
            }
            // РќРµ Р°С‚Р°РєСѓРµРј РїРѕРєР° HP РЅРёР·РєРѕРµ
            return;
        }
        state.isRetreating = false;
        
        // РџСЂРѕРІРµСЂСЏРµРј С‡РёРЅРёС‚СЃСЏ Р»Рё Р±РѕС‚ - РµСЃР»Рё РґР°, РЅРµ С‚СЂРѕРіР°РµРј РµРіРѕ
        if (utilsState.isMending) {
            return; // Р‘РѕС‚ С‡РёРЅРёС‚СЃСЏ - РЅРµ РјРµС€Р°РµРј РµРјСѓ
        }
        
        // Р’С‹Р±РёСЂР°РµРј СЂРµР¶РёРј Р±РѕСЏ
        selectWeaponMode(bot, state, distance, settings);
        
        // РџРѕРІРѕСЂР°С‡РёРІР°РµРјСЃСЏ Рє С†РµР»Рё (РµСЃР»Рё РЅРµ Р±СЂРѕСЃР°РµРј Р·РµР»СЊРµ)
        if (!utilsState.isThrowingPotion) {
            BotNavigation.lookAt(bot, target);
        }
        
        // Р•СЃР»Рё РІСЂР°Рі СЃР»РёС€РєРѕРј РґР°Р»РµРєРѕ РґР»СЏ С‚РµРєСѓС‰РµРіРѕ СЂРµР¶РёРјР° - РёРґС‘Рј Рє РЅРµРјСѓ
        double maxRange = switch (state.currentMode) {
            case MELEE -> settings.getMeleeRange() * 2;
            case RANGED -> settings.getRangedOptimalRange() + 15;
            case MACE -> settings.getMaceRange() * 2;
            case SPEAR -> settings.getSpearChargeRange();
            case CRYSTAL -> 10.0; // Crystal PVP СЌС„С„РµРєС‚РёРІРµРЅ РґРѕ 10 Р±Р»РѕРєРѕРІ
            case ANCHOR -> 10.0;  // Anchor PVP СЌС„С„РµРєС‚РёРІРµРЅ РґРѕ 10 Р±Р»РѕРєРѕРІ (increased)
        };
        
        if (distance > maxRange) {
            // Р’СЂР°Рі РґР°Р»РµРєРѕ - РёРґС‘Рј Рє РЅРµРјСѓ
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
            return;
        }
        
        // Р’С‹РїРѕР»РЅСЏРµРј РґРµР№СЃС‚РІРёРµ РІ Р·Р°РІРёСЃРёРјРѕСЃС‚Рё РѕС‚ СЂРµР¶РёРјР°
        switch (state.currentMode) {
            case MELEE -> handleMeleeCombat(bot, target, state, distance, settings, server);
            case RANGED -> handleRangedCombat(bot, target, state, distance, settings, server);
            case MACE -> handleMaceCombat(bot, target, state, distance, settings, server);
            case SPEAR -> handleSpearCombat(bot, target, state, distance, settings, server);
            case CRYSTAL -> {
                // РСЃРїРѕР»СЊР·СѓРµРј РѕС‚РґРµР»СЊРЅС‹Р№ РєР»Р°СЃСЃ РґР»СЏ Crystal PVP СЃ РїРѕР»РЅС‹Рј РєРѕРЅС‚СЂРѕР»РµРј
                boolean handled = BotCrystalPvp.doCrystalPvp(bot, target, settings, server);
                if (!handled) {
                    // Р•СЃР»Рё Crystal PVP РЅРµ РјРѕР¶РµС‚ СЂР°Р±РѕС‚Р°С‚СЊ - РїРµСЂРµРєР»СЋС‡Р°РµРјСЃСЏ РЅР° Р±Р»РёР¶РЅРёР№ Р±РѕР№
                    state.currentMode = CombatState.WeaponMode.MELEE;
                    handleMeleeCombat(bot, target, state, distance, settings, server);
                }
            }
            case ANCHOR -> {
                // РСЃРїРѕР»СЊР·СѓРµРј РѕС‚РґРµР»СЊРЅС‹Р№ РєР»Р°СЃСЃ РґР»СЏ Anchor PVP СЃ РїРѕР»РЅС‹Рј РєРѕРЅС‚СЂРѕР»РµРј
                boolean handled = BotAnchorPvp.doAnchorPvp(bot, target, settings, server);
                if (!handled) {
                    // Р•СЃР»Рё Anchor PVP РЅРµ РјРѕР¶РµС‚ СЂР°Р±РѕС‚Р°С‚СЊ - РїРµСЂРµРєР»СЋС‡Р°РµРјСЃСЏ РЅР° Р±Р»РёР¶РЅРёР№ Р±РѕР№
                    state.currentMode = CombatState.WeaponMode.MELEE;
                    handleMeleeCombat(bot, target, state, distance, settings, server);
                }
            }
        }
    }
    
    /**
     * РџРѕРёСЃРє С†РµР»Рё
     */
    private static Entity findTarget(ServerPlayerEntity bot, CombatState state, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        // РџСЂРёРѕСЂРёС‚РµС‚ 1: РџСЂРёРЅСѓРґРёС‚РµР»СЊРЅР°СЏ С†РµР»СЊ РїРѕ РєРѕРјР°РЅРґРµ (Р’РЎР•Р“Р”Рђ СЂР°Р±РѕС‚Р°РµС‚)
        if (state.forcedTargetName != null) {
            Entity forced = findEntityByName(bot, state.forcedTargetName, server);
            if (forced != null && forced.isAlive()) {
                double dist = bot.distanceTo(forced);
                if (dist <= settings.getMaxTargetDistance()) {
                    return forced;
                }
            }
            // РќР• СЃР±СЂР°СЃС‹РІР°РµРј С†РµР»СЊ РµСЃР»Рё РѕРЅР° РІСЂРµРјРµРЅРЅРѕ РґР°Р»РµРєРѕ - РїСѓСЃС‚СЊ Р±РѕС‚ РёРґС‘С‚ Рє РЅРµР№
            // state.forcedTargetName = null;
        }
        
        // РџСЂРёРѕСЂРёС‚РµС‚ 2: РўРѕС‚ РєС‚Рѕ РЅР°СЃ Р°С‚Р°РєРѕРІР°Р» (СЂРµРІР°РЅС€) - РґРµСЂР¶РёРј С†РµР»СЊ РїРѕРєР° РІСЂР°Рі Р¶РёРІ
        if (settings.isRevengeEnabled() && state.lastAttacker != null) {
            // РџСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ Р°С‚Р°РєСѓСЋС‰РёР№ РµС‰С‘ СЃСѓС‰РµСЃС‚РІСѓРµС‚ Рё Р¶РёРІ
            if (!state.lastAttacker.isRemoved() && state.lastAttacker.isAlive()) {
                // РџСЂРѕРІРµСЂСЏРµРј friendlyfire - РЅРµ Р°С‚Р°РєСѓРµРј СЃРѕСЋР·РЅРёРєРѕРІ РґР°Р¶Рµ РІ СЂРµРІР°РЅР¶Рµ
                if (!settings.isFriendlyFireEnabled() && state.lastAttacker instanceof PlayerEntity) {
                    String attackerName = state.lastAttacker.getName().getString();
                    if (BotFaction.areAllies(bot.getName().getString(), attackerName)) {
                        state.lastAttacker = null; // РЎР±СЂР°СЃС‹РІР°РµРј - СЌС‚Рѕ СЃРѕСЋР·РЅРёРє
                    }
                }
                
                if (state.lastAttacker != null) {
                    double dist = bot.distanceTo(state.lastAttacker);
                    if (dist <= settings.getMaxTargetDistance()) {
                        // РћР±РЅРѕРІР»СЏРµРј РІСЂРµРјСЏ РµСЃР»Рё РІСЂР°Рі Р±Р»РёР·РєРѕ (РЅРµ СЃР±СЂР°СЃС‹РІР°РµРј РїРѕРєР° РІСЂР°Рі СЂСЏРґРѕРј)
                        if (dist <= 10.0) {
                            state.lastAttackTime = System.currentTimeMillis();
                        }
                        return state.lastAttacker;
                    }
                }
            }
            // РЎР±СЂР°СЃС‹РІР°РµРј С‚РѕР»СЊРєРѕ РµСЃР»Рё С†РµР»СЊ РјРµСЂС‚РІР° РёР»Рё РґР°Р»РµРєРѕ Р±РѕР»СЊС€Рµ 30 СЃРµРєСѓРЅРґ
            if (state.lastAttacker == null || state.lastAttacker.isRemoved() || !state.lastAttacker.isAlive() || 
                System.currentTimeMillis() - state.lastAttackTime >= 30000) {
                state.lastAttacker = null;
            }
        }
        
        // РџСЂРёРѕСЂРёС‚РµС‚ 3: Р’СЂР°РіРё РїРѕ С„СЂР°РєС†РёСЏРј (РІСЃРµРіРґР° РµСЃР»Рё С„СЂР°РєС†РёРё РІРєР»СЋС‡РµРЅС‹)
        if (settings.isFactionsEnabled()) {
            Entity factionEnemy = findFactionEnemy(bot, settings, server);
            if (factionEnemy != null) {
                return factionEnemy;
            }
        }
        
        // РџСЂРёРѕСЂРёС‚РµС‚ 4: Р‘Р»РёР¶Р°Р№С€РёР№ РІСЂР°Рі (С‚РѕР»СЊРєРѕ РµСЃР»Рё autotarget РІРєР»СЋС‡С‘РЅ)
        if (settings.isAutoTargetEnabled()) {
            return findNearestEnemy(bot, settings, server);
        }
        
        return null;
    }
    
    /**
     * РџРѕРёСЃРє РІСЂР°РіР° РїРѕ С„СЂР°РєС†РёСЏРј
     */
    private static Entity findFactionEnemy(ServerPlayerEntity bot, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        String botName = bot.getName().getString();
        String botFaction = BotFaction.getFaction(botName);
        
        // Р•СЃР»Рё Р±РѕС‚ РЅРµ РІ С„СЂР°РєС†РёРё - РЅРµ РёС‰РµРј РІСЂР°РіРѕРІ РїРѕ С„СЂР°РєС†РёСЏРј
        if (botFaction == null) return null;
        
        double maxDist = settings.getMaxTargetDistance();
        Entity nearest = null;
        double nearestDist = maxDist + 1;
        
        if (server != null) {
            for (var player : server.getPlayerManager().getPlayerList()) {
                if (player == bot) continue;
                if (!player.isAlive()) continue;
                if (player.isSpectator() || player.isCreative()) continue;
                
                String targetName = player.getName().getString();
                
                // РџСЂРѕРІРµСЂСЏРµРј РІСЂР°Р¶РґРµР±РЅРѕСЃС‚СЊ РїРѕ С„СЂР°РєС†РёСЏРј
                if (BotFaction.areEnemies(botName, targetName)) {
                    double dist = bot.distanceTo(player);
                    if (dist < nearestDist && dist <= maxDist) {
                        nearestDist = dist;
                        nearest = player;
                    }
                }
            }
        }
        
        return nearest;
    }
    
    private static Entity findEntityByName(ServerPlayerEntity bot, String name, net.minecraft.server.MinecraftServer server) {
        // РС‰РµРј РёРіСЂРѕРєР°
        if (server != null) {
            var player = server.getPlayerManager().getPlayer(name);
            if (player != null && player != bot) return player;
        }
        
        // РС‰РµРј СЃСѓС‰РЅРѕСЃС‚СЊ РїРѕ РёРјРµРЅРё РІ РјРёСЂРµ Р±РѕС‚Р°
        if (server != null) {
            for (var world : server.getWorlds()) {
                for (Entity entity : world.iterateEntities()) {
                    if (entity.getName().getString().equalsIgnoreCase(name) && entity != bot) {
                        return entity;
                    }
                }
            }
        }
        return null;
    }
    
    private static Entity findNearestEnemy(ServerPlayerEntity bot, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        double maxDist = settings.getMaxTargetDistance();
        Box searchBox = bot.getBoundingBox().expand(maxDist);
        
        Entity nearest = null;
        double nearestDist = maxDist + 1;
        
        // РџРѕР»СѓС‡Р°РµРј РјРёСЂ С‡РµСЂРµР· СЃРµСЂРІРµСЂ
        if (server != null) {
            for (var world : server.getWorlds()) {
                for (Entity entity : world.getOtherEntities(bot, searchBox)) {
                    if (!isValidTarget(bot, entity, settings)) continue;
                    
                    double dist = bot.distanceTo(entity);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = entity;
                    }
                }
            }
        }
        
        return nearest;
    }
    
    private static boolean isValidTarget(ServerPlayerEntity bot, Entity entity, BotSettings settings) {
        if (entity == bot) return false;
        if (!entity.isAlive()) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        
        String botName = bot.getName().getString();
        
        // РРіСЂРѕРєРё Рё Р±РѕС‚С‹
        if (entity instanceof PlayerEntity player) {
            if (player.isSpectator() || player.isCreative()) return false;
            
            String targetName = player.getName().getString();
            
            // РџСЂРѕРІРµСЂСЏРµРј С„СЂР°РєС†РёРё
            if (settings.isFactionsEnabled()) {
                // РЎРѕСЋР·РЅРёРєРё - РЅРµ Р°С‚Р°РєСѓРµРј (РµСЃР»Рё friendlyfire РІС‹РєР»СЋС‡РµРЅ)
                if (!settings.isFriendlyFireEnabled() && BotFaction.areAllies(botName, targetName)) {
                    return false;
                }
                // Р’СЂР°РіРё РїРѕ С„СЂР°РєС†РёРё - Р°С‚Р°РєСѓРµРј
                if (BotFaction.areEnemies(botName, targetName)) {
                    return true;
                }
            }
            
            // РџСЂРѕРІРµСЂСЏРµРј РЅР°СЃС‚СЂРѕР№РєРё РґР»СЏ Р±РѕС‚РѕРІ
            if (BotManager.getAllBots().contains(targetName)) {
                if (!settings.isTargetOtherBots()) return false;
            } else {
                if (!settings.isTargetPlayers()) return false;
            }
            
            return true;
        }
        
        // Р’СЂР°Р¶РґРµР±РЅС‹Рµ РјРѕР±С‹
        if (entity instanceof HostileEntity) {
            return settings.isTargetHostileMobs();
        }
        
        // Р”СЂСѓРіРёРµ РјРѕР±С‹
        if (living instanceof net.minecraft.entity.mob.MobEntity) {
            return settings.isTargetHostileMobs();
        }
        
        return false;
    }

    
    /**
     * Р’С‹Р±РѕСЂ СЂРµР¶РёРјР° РѕСЂСѓР¶РёСЏ
     */
    private static void selectWeaponMode(ServerPlayerEntity bot, CombatState state, double distance, BotSettings settings) {
        var inventory = bot.getInventory();
        Entity target = state.target;
        
        boolean hasMelee = findMeleeWeapon(inventory) >= 0;
        boolean hasRanged = findRangedWeapon(inventory) >= 0;
        boolean hasMace = findMace(inventory) >= 0;
        boolean hasSpear = findSpear(inventory) >= 0;
        
        double meleeRange = settings.getMeleeRange();
        double rangedMinRange = settings.getRangedMinRange();
        double maceRange = settings.getMaceRange();
        double spearRange = settings.getSpearRange();
        
        // Р›РѕРіРёРєР° РІС‹Р±РѕСЂР° РѕСЂСѓР¶РёСЏ
        // РџСЂРёРѕСЂРёС‚РµС‚ 1: Crystal PVP (РµСЃР»Рё РґРѕСЃС‚СѓРїРµРЅ) - Р±С‹СЃС‚СЂРµРµ
        if (target != null && BotCrystalPvp.canUseCrystalPvp(bot, target, settings)) {
            state.currentMode = CombatState.WeaponMode.CRYSTAL;
        } else if (target != null && BotAnchorPvp.canUseAnchorPvp(bot, target, settings)) {
            // РџСЂРёРѕСЂРёС‚РµС‚ 2: Anchor PVP (РµСЃР»Рё РєСЂРёСЃС‚Р°Р»Р»С‹ РЅРµРґРѕСЃС‚СѓРїРЅС‹) - Р±РѕР»СЊС€Рµ СѓСЂРѕРЅР° РЅРѕ РјРµРґР»РµРЅРЅРµРµ
            state.currentMode = CombatState.WeaponMode.ANCHOR;
        } else if (hasMace && distance <= maceRange && settings.isMaceEnabled()) {
            // Р‘СѓР»Р°РІР° - РµСЃР»Рё РІСЂР°Рі Р±Р»РёР·РєРѕ Рё РјРѕР¶РЅРѕ РїСЂС‹РіРЅСѓС‚СЊ
            state.currentMode = CombatState.WeaponMode.MACE;
        } else if (hasSpear && distance <= spearRange && settings.isSpearEnabled()) {
            // РљРѕРїСЊС‘ - СЃСЂРµРґРЅСЏСЏ РґРёСЃС‚Р°РЅС†РёСЏ, charge Р°С‚Р°РєР° РїСЂРё РґРІРёР¶РµРЅРёРё
            state.currentMode = CombatState.WeaponMode.SPEAR;
        } else if (hasRanged && distance > rangedMinRange && settings.isRangedEnabled()) {
            // Р›СѓРє - РµСЃР»Рё РІСЂР°Рі РґР°Р»РµРєРѕ
            state.currentMode = CombatState.WeaponMode.RANGED;
        } else if (hasMelee && distance <= meleeRange * 2) {
            // РњРµС‡ - Р±Р»РёР¶РЅРёР№ Р±РѕР№
            state.currentMode = CombatState.WeaponMode.MELEE;
        } else if (hasSpear && settings.isSpearEnabled()) {
            // РљРѕРїСЊС‘ РєР°Рє Р·Р°РїР°СЃРЅРѕР№ РІР°СЂРёР°РЅС‚ РґР»СЏ СЃСЂРµРґРЅРµР№ РґРёСЃС‚Р°РЅС†РёРё
            state.currentMode = CombatState.WeaponMode.SPEAR;
        } else if (hasRanged && settings.isRangedEnabled()) {
            // Р›СѓРє РєР°Рє Р·Р°РїР°СЃРЅРѕР№ РІР°СЂРёР°РЅС‚
            state.currentMode = CombatState.WeaponMode.RANGED;
        } else {
            state.currentMode = CombatState.WeaponMode.MELEE;
        }
    }
    
    /**
     * Р‘Р»РёР¶РЅРёР№ Р±РѕР№
     */
    private static void handleMeleeCombat(ServerPlayerEntity bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        var inventory = bot.getInventory();
        
        // РџСЂРµРєСЂР°С‰Р°РµРј РЅР°С‚СЏРіРёРІР°С‚СЊ Р»СѓРє РµСЃР»Рё РЅР°С‚СЏРіРёРІР°Р»Рё
        if (state.isDrawingBow) {
            stopUsingBow(bot, state);
        }
        
        double meleeRange = settings.getMeleeRange();
        
        // РџСЂРѕР±СѓРµРј РїРѕСЃС‚Р°РІРёС‚СЊ РїР°СѓС‚РёРЅСѓ РїРѕРґ РІСЂР°РіР° РµСЃР»Рё РѕРЅ Р±Р»РёР·РєРѕ Рё Р±РµР¶РёС‚ РЅР° РЅР°СЃ
        if (settings.isCobwebEnabled() && distance < 6.0 && distance > 2.0 && state.cobwebCooldown <= 0 && !state.isPlacingCobweb) {
            // РџСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ РІСЂР°Рі РґРІРёР¶РµС‚СЃСЏ Рє РЅР°Рј
            if (target instanceof net.minecraft.entity.LivingEntity living) {
                Vec3d targetVel = living.getVelocity();
                double targetSpeed = Math.sqrt(targetVel.x * targetVel.x + targetVel.z * targetVel.z);
                if (targetSpeed > 0.08) { // Р’СЂР°Рі РґРІРёР¶РµС‚СЃСЏ
                    tryPlaceCobweb(bot, target, server);
                }
            }
        }
        
        // Р”РІРёР¶РµРЅРёРµ Рє С†РµР»Рё СЃ РЅР°РІРёРіР°С†РёРµР№
        if (distance > meleeRange) {
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
        } else if (distance < 1.5) {
            // РЎР»РёС€РєРѕРј Р±Р»РёР·РєРѕ - РѕС‚С…РѕРґРёРј РЅРµРјРЅРѕРіРѕ
            BotNavigation.moveAway(bot, target, 0.3);
        }
        
        // РџСЂРѕРІРµСЂСЏРµРј HP Рё РёСЃРїРѕР»СЊР·СѓРµРј С‰РёС‚ РµСЃР»Рё РЅСѓР¶РЅРѕ
        float healthPercent = bot.getHealth() / bot.getMaxHealth();
        boolean shouldUseShield = settings.isAutoShieldEnabled() && healthPercent < settings.getShieldHealthThreshold();
        
        // РћРїСЂРµРґРµР»СЏРµРј РЅСѓР¶РЅРѕ Р»Рё РґРµСЂР¶Р°С‚СЊ С‰РёС‚ РїРѕРґРЅСЏС‚С‹Рј
        // РћРїСѓСЃРєР°РµРј С‰РёС‚ РўРћР›Р¬РљРћ РєРѕРіРґР° attackCooldown == 1 (Р·Р° 1 С‚РёРє РґРѕ Р°С‚Р°РєРё)
        boolean willAttackSoon = distance <= meleeRange && state.attackCooldown == 1;
        boolean shouldHoldShield = shouldUseShield && !willAttackSoon;
        
        if (shouldUseShield) {
            // Р­РєРёРїРёСЂСѓРµРј С‰РёС‚ РІ offhand РµСЃР»Рё РµРіРѕ С‚Р°Рј РЅРµС‚
            ItemStack offhandItem = bot.getOffHandStack();
            if (offhandItem.isEmpty() || !offhandItem.getItem().toString().contains("shield")) {
                int shieldSlot = findShield(inventory);
                if (shieldSlot >= 0) {
                    // РџРµСЂРµРјРµС‰Р°РµРј С‰РёС‚ РІ offhand (СЃР»РѕС‚ 40)
                    ItemStack shield = inventory.getStack(shieldSlot);
                    inventory.setStack(40, shield);
                    inventory.setStack(shieldSlot, ItemStack.EMPTY);
                }
            }
            
            // РЈРїСЂР°РІР»СЏРµРј С‰РёС‚РѕРј С‡РµСЂРµР· Carpet РєРѕРјР°РЅРґС‹
            if (shouldHoldShield && !state.isUsingShield) {
                // РќСѓР¶РЅРѕ РїРѕРґРЅСЏС‚СЊ С‰РёС‚
                startUsingShield(bot, server);
                state.isUsingShield = true;
            } else if (!shouldHoldShield && state.isUsingShield) {
                // РќСѓР¶РЅРѕ РѕРїСѓСЃС‚РёС‚СЊ С‰РёС‚ (Р·Р° 1 С‚РёРє РґРѕ СѓРґР°СЂР°)
                stopUsingShield(bot, server);
                state.isUsingShield = false;
            }
        } else {
            // HP РЅРѕСЂРјР°Р»СЊРЅРѕРµ - РѕРїСѓСЃРєР°РµРј С‰РёС‚ РµСЃР»Рё РѕРЅ РїРѕРґРЅСЏС‚
            if (state.isUsingShield && state.shieldToggleCooldown <= 0) {
                stopUsingShield(bot, server);
                state.isUsingShield = false;
                state.shieldToggleCooldown = 20; // 1 СЃРµРєСѓРЅРґР° РєСѓР»РґР°СѓРЅ
            }
        }
        
        // РђС‚Р°РєР°
        if (distance <= meleeRange && state.attackCooldown <= 0) {
            // РџСЂРѕРІРµСЂСЏРµРј РєСѓР»РґР°СѓРЅ Р°С‚Р°РєРё РёРіСЂРѕРєР° (РІР°Р¶РЅРѕ РґР»СЏ 1.9+ Р±РѕСЏ)
            if (bot.getAttackCooldownProgress(0.5f) < 1.0f) {
                // РћСЂСѓР¶РёРµ РµС‰С‘ РЅРµ РіРѕС‚РѕРІРѕ Рє Р°С‚Р°РєРµ - Р¶РґС‘Рј
                return;
            }
            
            // РџСЂРѕРІРµСЂСЏРµРј РЅСѓР¶РЅРѕ Р»Рё СЃР±РёС‚СЊ С‰РёС‚
            if (settings.isShieldBreakEnabled() && target instanceof PlayerEntity player && player.isBlocking()) {
                // РџРµСЂРµРєР»СЋС‡Р°РµРјСЃСЏ РЅР° С‚РѕРїРѕСЂ РґР»СЏ СЃР±РёС‚РёСЏ С‰РёС‚Р°
                int axeSlot = findAxe(inventory);
                if (axeSlot >= 0) {
                    // РџРµСЂРµРјРµС‰Р°РµРј С‚РѕРїРѕСЂ РІ С…РѕС‚Р±Р°СЂ РµСЃР»Рё РЅСѓР¶РЅРѕ
                    if (axeSlot >= 9) {
                        ItemStack axe = inventory.getStack(axeSlot);
                        ItemStack current = inventory.getStack(0);
                        inventory.setStack(axeSlot, current);
                        inventory.setStack(0, axe);
                        axeSlot = 0;
                    }
                    org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, axeSlot);
                    
                    // РђС‚Р°РєСѓРµРј С‚РѕРїРѕСЂРѕРј С‡С‚РѕР±С‹ СЃР±РёС‚СЊ С‰РёС‚
                    attackWithCarpet(bot, target, server);
                    
                    // РћС‚РјРµС‡Р°РµРј С‡С‚Рѕ С‰РёС‚ СЃР±РёС‚ - Р±РѕС‚ РїСЂРѕРґРѕР»Р¶РёС‚ РґСЂР°С‚СЊСЃСЏ
                    state.shieldBroken = true;
                    state.shieldBrokenTime = System.currentTimeMillis();
                    
                    // РЈРІРµР»РёС‡РµРЅРЅС‹Р№ РєСѓР»РґР°СѓРЅ РµСЃР»Рё РёСЃРїРѕР»СЊР·СѓРµРј С‰РёС‚ (Р±РѕР»РµРµ РѕСЃС‚РѕСЂРѕР¶РЅР°СЏ Р°С‚Р°РєР°)
                    int cooldown = shouldUseShield ? (int)(settings.getAttackCooldown() * 1.5) : settings.getAttackCooldown();
                    state.attackCooldown = cooldown;
                    
                    // РќР• РїРµСЂРµРєР»СЋС‡Р°РµРјСЃСЏ РѕР±СЂР°С‚РЅРѕ РЅР° РјРµС‡ СЃСЂР°Р·Сѓ - СЌС‚Рѕ РїСЂРѕРёР·РѕР№РґС‘С‚ РІ СЃР»РµРґСѓСЋС‰РµРј С‚РёРєРµ
                    // РєРѕРіРґР° С‰РёС‚ СѓР¶Рµ Р±СѓРґРµС‚ СЃР±РёС‚ Рё Р±РѕС‚ РїСЂРѕРґРѕР»Р¶РёС‚ Р°С‚Р°РєРѕРІР°С‚СЊ
                    return;
                }
            }
            
            // РћР±С‹С‡РЅР°СЏ Р°С‚Р°РєР° - СЌРєРёРїРёСЂСѓРµРј РјРµС‡/С‚РѕРїРѕСЂ (С‚РѕР»СЊРєРѕ РµСЃР»Рё С‚РµРєСѓС‰РµРµ РѕСЂСѓР¶РёРµ РЅРµ РїРѕРґС…РѕРґРёС‚)
            int currentSlot = org.stepan1411.pvp_bot.utils.InventoryHelper.getSelectedSlot(inventory);
            ItemStack currentItem = inventory.getStack(currentSlot);
            double currentScore = getMeleeScore(currentItem.getItem(), settings.isPreferSword());
            
            int weaponSlot = findMeleeWeapon(inventory);
            if (weaponSlot >= 0 && weaponSlot < 9) {
                // РџРµСЂРµРєР»СЋС‡Р°РµРјСЃСЏ С‚РѕР»СЊРєРѕ РµСЃР»Рё РЅРѕРІРѕРµ РѕСЂСѓР¶РёРµ Р»СѓС‡С€Рµ С‚РµРєСѓС‰РµРіРѕ
                ItemStack newWeapon = inventory.getStack(weaponSlot);
                double newScore = getMeleeScore(newWeapon.getItem(), settings.isPreferSword());
                
                if (newScore > currentScore || currentScore == 0) {
                    org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, weaponSlot);
                }
            }
            
            // РљСЂРёС‚РёС‡РµСЃРєРёР№ СѓРґР°СЂ - РїСЂС‹Р¶РѕРє РїРµСЂРµРґ СѓРґР°СЂРѕРј, РЅРѕ Р°С‚Р°РєСѓРµРј С‚РѕР»СЊРєРѕ РєРѕРіРґР° РїР°РґР°РµРј
            if (settings.isCriticalsEnabled()) {
                if (bot.isOnGround()) {
                    // РџСЂС‹РіР°РµРј РґР»СЏ РєСЂРёС‚Р°
                    bot.jump();
                    return; // РќРµ Р°С‚Р°РєСѓРµРј СЃСЂР°Р·Сѓ, Р¶РґС‘Рј РїРѕРєР° РЅР°С‡РЅС‘Рј РїР°РґР°С‚СЊ
                } else {
                    // Р’ РІРѕР·РґСѓС…Рµ - РїСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ РјС‹ РїР°РґР°РµРј Р РїСЂРѕС€Р»Рё РґРѕСЃС‚Р°С‚РѕС‡РЅРѕ РІСЂРµРјРµРЅРё РїРѕСЃР»Рµ РїСЂС‹Р¶РєР°
                    double velocityY = bot.getVelocity().y;
                    double fallDistance = bot.fallDistance;
                    
                    // РђС‚Р°РєСѓРµРј С‚РѕР»СЊРєРѕ РµСЃР»Рё:
                    // 1. РџР°РґР°РµРј РІРЅРёР· (velocity.y < 0)
                    // 2. РџСЂРѕС€Р»Рё С…РѕС‚СЏ Р±С‹ РЅРµР±РѕР»СЊС€РѕРµ СЂР°СЃСЃС‚РѕСЏРЅРёРµ РїР°РґРµРЅРёСЏ (fallDistance > 0.1)
                    // Р­С‚Рѕ РіР°СЂР°РЅС‚РёСЂСѓРµС‚ С‡С‚Рѕ РјС‹ СѓР¶Рµ РїСЂРѕС€Р»Рё РїРёРє РїСЂС‹Р¶РєР°
                    if (velocityY < 0 && fallDistance > 0.1) {
                        // РџР°РґР°РµРј - РґРµР»Р°РµРј РєСЂРёС‚РёС‡РµСЃРєРёР№ СѓРґР°СЂ
                        attackWithCarpet(bot, target, server);
                        // РЈРІРµР»РёС‡РµРЅРЅС‹Р№ РєСѓР»РґР°СѓРЅ РµСЃР»Рё РёСЃРїРѕР»СЊР·СѓРµРј С‰РёС‚ (Р±РѕР»РµРµ РѕСЃС‚РѕСЂРѕР¶РЅР°СЏ Р°С‚Р°РєР°)
                        int cooldown = shouldUseShield ? (int)(settings.getAttackCooldown() * 1.5) : settings.getAttackCooldown();
                        state.attackCooldown = cooldown;
                    }
                    // РРЅР°С‡Рµ РїСЂРѕСЃС‚Рѕ Р¶РґС‘Рј РїРѕРєР° РЅРµ РЅР°С‡РЅС‘Рј РїР°РґР°С‚СЊ
                }
            } else {
                // РљСЂРёС‚РёС‡РµСЃРєРёРµ СѓРґР°СЂС‹ РІС‹РєР»СЋС‡РµРЅС‹ - Р°С‚Р°РєСѓРµРј СЃСЂР°Р·Сѓ
                attackWithCarpet(bot, target, server);
                // РЈРІРµР»РёС‡РµРЅРЅС‹Р№ РєСѓР»РґР°СѓРЅ РµСЃР»Рё РёСЃРїРѕР»СЊР·СѓРµРј С‰РёС‚ (Р±РѕР»РµРµ РѕСЃС‚РѕСЂРѕР¶РЅР°СЏ Р°С‚Р°РєР°)
                int cooldown = shouldUseShield ? (int)(settings.getAttackCooldown() * 1.5) : settings.getAttackCooldown();
                state.attackCooldown = cooldown;
            }
        } else {
            // РќРµ Р°С‚Р°РєСѓРµРј - РїСЂРѕСЃС‚Рѕ РґРµСЂР¶РёРј РѕСЂСѓР¶РёРµ РІ СЂСѓРєРµ (С‚РѕР»СЊРєРѕ РµСЃР»Рё С‚РµРєСѓС‰РµРµ РѕСЂСѓР¶РёРµ РЅРµ РїРѕРґС…РѕРґРёС‚)
            int currentSlot = org.stepan1411.pvp_bot.utils.InventoryHelper.getSelectedSlot(inventory);
            ItemStack currentItem = inventory.getStack(currentSlot);
            double currentScore = getMeleeScore(currentItem.getItem(), settings.isPreferSword());
            
            int weaponSlot = findMeleeWeapon(inventory);
            if (weaponSlot >= 0 && weaponSlot < 9) {
                // РџРµСЂРµРєР»СЋС‡Р°РµРјСЃСЏ С‚РѕР»СЊРєРѕ РµСЃР»Рё РЅРѕРІРѕРµ РѕСЂСѓР¶РёРµ Р»СѓС‡С€Рµ С‚РµРєСѓС‰РµРіРѕ
                ItemStack newWeapon = inventory.getStack(weaponSlot);
                double newScore = getMeleeScore(newWeapon.getItem(), settings.isPreferSword());
                
                if (newScore > currentScore || currentScore == 0) {
                    org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, weaponSlot);
                }
            }
        }
    }
    
    /**
     * Р”Р°Р»СЊРЅРёР№ Р±РѕР№ (Р»СѓРє/Р°СЂР±Р°Р»РµС‚)
     */
    private static void handleRangedCombat(ServerPlayerEntity bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        var inventory = bot.getInventory();
        
        // Р­РєРёРїРёСЂСѓРµРј Р»СѓРє
        int bowSlot = findRangedWeapon(inventory);
        if (bowSlot >= 0 && bowSlot < 9) {
            org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, bowSlot);
        }
        
        // РџСЂРѕРІРµСЂСЏРµРј РµСЃС‚СЊ Р»Рё СЃС‚СЂРµР»С‹
        if (!hasArrows(inventory)) {
            // РќРµС‚ СЃС‚СЂРµР» - РїРµСЂРµРєР»СЋС‡Р°РµРјСЃСЏ РЅР° Р±Р»РёР¶РЅРёР№ Р±РѕР№
            state.currentMode = CombatState.WeaponMode.MELEE;
            return;
        }
        
        ItemStack weapon = bot.getMainHandStack();
        boolean isCrossbow = weapon.getItem() instanceof CrossbowItem;
        
        if (isCrossbow) {
            handleCrossbowCombat(bot, target, state, distance, settings);
        } else {
            handleBowCombat(bot, target, state, distance, settings);
        }
        
        // Р”РµСЂР¶РёРј РґРёСЃС‚Р°РЅС†РёСЋ СЃ РЅР°РІРёРіР°С†РёРµР№
        double optimalRange = settings.getRangedOptimalRange();
        if (distance < optimalRange - 5) {
            BotNavigation.moveAway(bot, target, settings.getMoveSpeed());
        } else if (distance > optimalRange + 10) {
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
        }
    }
    
    private static void handleBowCombat(ServerPlayerEntity bot, Entity target, CombatState state, double distance, BotSettings settings) {
        if (!state.isDrawingBow) {
            // РќР°С‡РёРЅР°РµРј РЅР°С‚СЏРіРёРІР°С‚СЊ Р»СѓРє
            bot.setCurrentHand(Hand.MAIN_HAND);
            state.isDrawingBow = true;
            state.bowDrawTicks = 0;
        } else {
            state.bowDrawTicks++;
            
            // Р›СѓРє РїРѕР»РЅРѕСЃС‚СЊСЋ РЅР°С‚СЏРЅСѓС‚ РїРѕСЃР»Рµ 20 С‚РёРєРѕРІ (1 СЃРµРєСѓРЅРґР°)
            int minDrawTime = settings.getBowMinDrawTime();
            if (state.bowDrawTicks >= minDrawTime) {
                // РЎС‚СЂРµР»СЏРµРј
                bot.stopUsingItem();
                state.isDrawingBow = false;
                state.bowDrawTicks = 0;
                state.attackCooldown = 5; // РќРµР±РѕР»СЊС€Р°СЏ Р·Р°РґРµСЂР¶РєР° РјРµР¶РґСѓ РІС‹СЃС‚СЂРµР»Р°РјРё
            }
        }
    }
    
    private static void handleCrossbowCombat(ServerPlayerEntity bot, Entity target, CombatState state, double distance, BotSettings settings) {
        ItemStack crossbow = bot.getMainHandStack();
        
        if (CrossbowItem.isCharged(crossbow)) {
            // РђСЂР±Р°Р»РµС‚ Р·Р°СЂСЏР¶РµРЅ - СЃС‚СЂРµР»СЏРµРј С‡РµСЂРµР· stopUsingItem
            bot.stopUsingItem();
            state.attackCooldown = 5;
            state.isDrawingBow = false;
        } else if (!state.isDrawingBow) {
            // РќР°С‡РёРЅР°РµРј Р·Р°СЂСЏР¶Р°С‚СЊ
            bot.setCurrentHand(Hand.MAIN_HAND);
            state.isDrawingBow = true;
            state.bowDrawTicks = 0;
        } else {
            state.bowDrawTicks++;
            // РђСЂР±Р°Р»РµС‚ Р·Р°СЂСЏР¶Р°РµС‚СЃСЏ ~25 С‚РёРєРѕРІ
            if (state.bowDrawTicks >= 25) {
                bot.stopUsingItem();
                state.isDrawingBow = false;
            }
        }
    }
    
    private static void stopUsingBow(ServerPlayerEntity bot, CombatState state) {
        if (state.isDrawingBow) {
            bot.stopUsingItem();
            state.isDrawingBow = false;
            state.bowDrawTicks = 0;
        }
    }
    
    /**
     * Р‘РѕР№ Р±СѓР»Р°РІРѕР№ - РёСЃРїРѕР»СЊР·СѓРµС‚ wind charge РґР»СЏ РІС‹СЃРѕРєРѕРіРѕ РїСЂС‹Р¶РєР°
     */
    private static void handleMaceCombat(ServerPlayerEntity bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        var inventory = bot.getInventory();
        
        if (state.isDrawingBow) {
            stopUsingBow(bot, state);
        }
        
        // Р•СЃР»Рё РІ РІРѕР·РґСѓС…Рµ - СЌРєРёРїРёСЂСѓРµРј Р±СѓР»Р°РІСѓ Рё Р°С‚Р°РєСѓРµРј РїСЂРё РїР°РґРµРЅРёРё
        if (!bot.isOnGround()) {
            int maceSlot = findMace(inventory);
            if (maceSlot >= 0 && maceSlot < 9) {
                org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, maceSlot);
            }
            
            // РђС‚Р°РєСѓРµРј РїСЂРё РїР°РґРµРЅРёРё - СЂР°РЅСЊС€Рµ РґР»СЏ РјР°РєСЃРёРјР°Р»СЊРЅРѕРіРѕ СѓСЂРѕРЅР°
            // РђС‚Р°РєСѓРµРј РєРѕРіРґР° РЅР°С‡РёРЅР°РµРј РїР°РґР°С‚СЊ (velocity.y < 0) Рё Р±Р»РёР·РєРѕ Рє С†РµР»Рё
            double verticalSpeed = bot.getVelocity().y;
            if (verticalSpeed < 0 && distance <= 5.0 && state.attackCooldown <= 0) {
                // РђС‚Р°РєСѓРµРј СЃСЂР°Р·Сѓ РєР°Рє РЅР°С‡РёРЅР°РµРј РїР°РґР°С‚СЊ
                attackWithCarpet(bot, target, server);
                state.attackCooldown = 5; // РљРѕСЂРѕС‚РєРёР№ РєСѓР»РґР°СѓРЅ РґР»СЏ РїРѕРІС‚РѕСЂРЅРѕР№ Р°С‚Р°РєРё
            }
            return;
        }
        
        // РќР° Р·РµРјР»Рµ - РёСЃРїРѕР»СЊР·СѓРµРј wind charge РґР»СЏ РїСЂС‹Р¶РєР°
        if (bot.isOnGround() && distance <= settings.getMaceRange()) {
            // РС‰РµРј wind charge
            int windChargeSlot = findWindCharge(inventory);
            
            if (windChargeSlot >= 0) {
                // РСЃРїРѕР»СЊР·СѓРµРј wind charge С‡РµСЂРµР· BotUtils
                BotUtils.useWindCharge(bot, server);
                bot.jump();
            } else {
                // РќРµС‚ wind charge - РѕР±С‹С‡РЅС‹Р№ РїСЂС‹Р¶РѕРє СЃ Р±СѓР»Р°РІРѕР№
                int maceSlot = findMace(inventory);
                if (maceSlot >= 0 && maceSlot < 9) {
                    org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, maceSlot);
                }
                
                bot.jump();
                double dx = target.getX() - bot.getX();
                double dz = target.getZ() - bot.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    dx /= dist;
                    dz /= dist;
                }
                bot.addVelocity(dx * 0.3, 0.3, dz * 0.3);
            }
        }
        
        // РђС‚Р°РєСѓРµРј РЅР° Р·РµРјР»Рµ РµСЃР»Рё Р±Р»РёР·РєРѕ
        if (bot.isOnGround() && distance <= 3.5 && state.attackCooldown <= 0) {
            int maceSlot = findMace(inventory);
            if (maceSlot >= 0 && maceSlot < 9) {
                org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, maceSlot);
            }
            attackWithCarpet(bot, target, server);
            state.attackCooldown = settings.getAttackCooldown();
        }
        
        // Р”РІРёР¶РµРЅРёРµ Рє С†РµР»Рё СЃ РЅР°РІРёРіР°С†РёРµР№
        if (distance > settings.getMaceRange()) {
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
        }
    }
    
    /**
     * Р‘РѕР№ РєРѕРїСЊС‘Рј (Spear) - 1.21.11
     * Р”РІР° СЂРµР¶РёРјР° Р°С‚Р°РєРё:
     * - РЈРґР°СЂ СЃ СЂР°Р·Р±РµРіР° (charge): РґРµСЂР¶Р°С‚СЊ РџРљРњ Рё РІСЂРµР·Р°С‚СЊСЃСЏ РІ С†РµР»СЊ - СѓСЂРѕРЅ РЅР°РЅРѕСЃРёС‚СЃСЏ РїСЂРё СЃС‚РѕР»РєРЅРѕРІРµРЅРёРё
     * - РЈРєРѕР» (jab): РѕР±С‹С‡РЅР°СЏ Р°С‚Р°РєР° Р›РљРњ (С‚СЂРµР±СѓРµС‚ 100% Р·Р°СЂСЏРґР° РјРµР¶РґСѓ СѓРєРѕР»Р°РјРё)
     * 
     * Р’РђР–РќРћ: РќРµР»СЊР·СЏ РґРµР»Р°С‚СЊ РѕР±С‹С‡РЅСѓСЋ Р°С‚Р°РєСѓ РїРѕРєР° charge Р°РєС‚РёРІРµРЅ - СЌС‚Рѕ СЃР±СЂРѕСЃРёС‚ РµРіРѕ!
     * РЈРґР°СЂ СЃ СЂР°Р·Р±РµРіР° РЅР°РЅРѕСЃРёС‚ СѓСЂРѕРЅ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё РїСЂРё СЃС‚РѕР»РєРЅРѕРІРµРЅРёРё СЃ С†РµР»СЊСЋ.
     */
    private static void handleSpearCombat(ServerPlayerEntity bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        var inventory = bot.getInventory();
        
        // РџСЂРµРєСЂР°С‰Р°РµРј РЅР°С‚СЏРіРёРІР°С‚СЊ Р»СѓРє РµСЃР»Рё РЅР°С‚СЏРіРёРІР°Р»Рё
        if (state.isDrawingBow) {
            stopUsingBow(bot, state);
        }
        
        // Р­РєРёРїРёСЂСѓРµРј РєРѕРїСЊС‘
        int spearSlot = findSpear(inventory);
        if (spearSlot >= 0 && spearSlot < 9) {
            org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, spearSlot);
        }
        
        double chargeStartDistance = 10.0; // РќР°С‡РёРЅР°РµРј charge Р·Р° 5 Р±Р»РѕРєРѕРІ
        double chargeHitDistance = 0.1;   // Р”РёСЃС‚Р°РЅС†РёСЏ СЃС‚РѕР»РєРЅРѕРІРµРЅРёСЏ РґР»СЏ charge (РІРїР»РѕС‚РЅСѓСЋ)
        
        // Р›РѕРіРёРєР° Р±РѕСЏ РєРѕРїСЊС‘Рј:
        // 1. Р”Р°Р»РµРєРѕ (> 5 Р±Р»РѕРєРѕРІ) - Р±РµР¶РёРј Рє РІСЂР°РіСѓ Р‘Р•Р— charge
        // 2. Р—Р° 5 Р±Р»РѕРєРѕРІ - РЅР°С‡РёРЅР°РµРј charge (РґРµСЂР¶РёРј РџРљРњ) Рё Р±РµР¶РёРј Рє РІСЂР°РіСѓ
        // 3. РџСЂРё СЃС‚РѕР»РєРЅРѕРІРµРЅРёРё (< 1.5 Р±Р»РѕРєР°) - СѓСЂРѕРЅ РЅР°РЅРѕСЃРёС‚СЃСЏ Р°РІС‚РѕРјР°С‚РёС‡РµСЃРєРё, РѕС‚РїСѓСЃРєР°РµРј charge
        // 4. РџРѕСЃР»Рµ charge РјРѕР¶РЅРѕ СЃСЂР°Р·Сѓ РґРµР»Р°С‚СЊ jab, Рё РЅР°РѕР±РѕСЂРѕС‚
        
        if (distance > chargeStartDistance) {
            // Р”Р°Р»РµРєРѕ - Р±РµР¶РёРј Рє РІСЂР°РіСѓ Р‘Р•Р— charge
            if (state.isChargingSpear) {
                bot.stopUsingItem();
                state.isChargingSpear = false;
                state.spearChargeTicks = 0;
            }
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
            
        } else if (distance > chargeHitDistance) {
            // РЎСЂРµРґРЅСЏСЏ РґРёСЃС‚Р°РЅС†РёСЏ - charge Р°С‚Р°РєР° (РґРµСЂР¶РёРј РџРљРњ Рё Р±РµР¶РёРј)
            if (!state.isChargingSpear) {
                // РќР°С‡РёРЅР°РµРј charge - РІС‹СЃС‚Р°РІР»СЏРµРј РєРѕРїСЊС‘ РІРїРµСЂС‘Рґ
                bot.setCurrentHand(Hand.MAIN_HAND);
                state.isChargingSpear = true;
                state.spearChargeTicks = 0;
            }
            
            state.spearChargeTicks++;
            
            // Р‘РµР¶РёРј Рє РІСЂР°РіСѓ СЃ charge - СѓСЂРѕРЅ РЅР°РЅРµСЃС‘С‚СЃСЏ РїСЂРё СЃС‚РѕР»РєРЅРѕРІРµРЅРёРё
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed() * 1.3);
            
            // РџСЂРѕРІРµСЂСЏРµРј СЃС‚Р°РґРёРё charge (СѓСЃС‚Р°Р»РѕСЃС‚СЊ РїРѕСЃР»Рµ ~40 С‚РёРєРѕРІ, СЂР°Р·СЂСЏРґРєР° РїРѕСЃР»Рµ ~60)
            if (state.spearChargeTicks > 60) {
                // РЎС‚Р°РґРёСЏ СЂР°Р·СЂСЏРґРєРё - Р»СѓС‡С€Рµ РѕС‚РїСѓСЃС‚РёС‚СЊ Рё РЅР°С‡Р°С‚СЊ Р·Р°РЅРѕРІРѕ
                bot.stopUsingItem();
                state.isChargingSpear = false;
                state.spearChargeTicks = 0;
            }
            
        } else {
            // РћС‡РµРЅСЊ Р±Р»РёР·РєРѕ (СЃС‚РѕР»РєРЅРѕРІРµРЅРёРµ) - СѓСЂРѕРЅ РѕС‚ charge СѓР¶Рµ РЅР°РЅРµСЃС‘РЅ
            if (state.isChargingSpear) {
                // РћС‚РїСѓСЃРєР°РµРј charge РїРѕСЃР»Рµ СЃС‚РѕР»РєРЅРѕРІРµРЅРёСЏ
                bot.stopUsingItem();
                state.isChargingSpear = false;
                state.spearChargeTicks = 0;
                // РџРѕСЃР»Рµ charge РјРѕР¶РЅРѕ СЃСЂР°Р·Сѓ РґРµР»Р°С‚СЊ jab
                state.attackCooldown = 0;
            }
            
            // РћС‚С…РѕРґРёРј РЅР°Р·Р°Рґ С‡С‚РѕР±С‹ СЃРЅРѕРІР° СЂР°Р·Р±РµР¶Р°С‚СЊСЃСЏ РґР»СЏ charge
            BotNavigation.moveAway(bot, target, settings.getMoveSpeed());
        }
    }

    
    private static final java.util.Random random = new java.util.Random();
    
    /**
     * РђС‚Р°РєР° С†РµР»Рё
     */
    private static void attack(ServerPlayerEntity bot, Entity target) {
        BotSettings settings = BotSettings.get();
        
        // Fire attack event - allow addons to cancel attack
        try {
            boolean cancelled = org.stepan1411.pvp_bot.api.BotAPIIntegration.fireAttackEvent(bot, target);
            if (cancelled) {
                return; // Attack cancelled by addon
            }
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing attack event: " + e.getMessage());
        }
        
        // РЁР°РЅСЃ РїСЂРѕРјР°С…Р°
        if (random.nextInt(100) < settings.getMissChance()) {
            // РџСЂРѕРјР°С… - РїСЂРѕСЃС‚Рѕ РјР°С€РµРј СЂСѓРєРѕР№
            bot.swingHand(Hand.MAIN_HAND);
            return;
        }
        
        bot.attack(target);
        bot.swingHand(Hand.MAIN_HAND);
    }
    
    /**
     * РђС‚Р°РєР° С‡РµСЂРµР· РєРѕРјР°РЅРґСѓ Carpet (Р±РѕР»РµРµ РЅР°РґС‘Р¶РЅРѕ)
     */
    private static void attackWithCarpet(ServerPlayerEntity bot, Entity target, net.minecraft.server.MinecraftServer server) {
        BotSettings settings = BotSettings.get();
        
        // РџСЂРѕРІРµСЂРєР° friendlyfire - РЅРµ Р°С‚Р°РєСѓРµРј СЃРѕСЋР·РЅРёРєРѕРІ
        if (!settings.isFriendlyFireEnabled() && target instanceof PlayerEntity) {
            String botName = bot.getName().getString();
            String targetName = target.getName().getString();
            if (BotFaction.areAllies(botName, targetName)) {
                // РЎРѕСЋР·РЅРёРє - РЅРµ Р°С‚Р°РєСѓРµРј, РїСЂРѕСЃС‚Рѕ РјР°С€РµРј СЂСѓРєРѕР№
                bot.swingHand(Hand.MAIN_HAND);
                return;
            }
        }
        
        // РЁР°РЅСЃ РїСЂРѕРјР°С…Р°
        if (random.nextInt(100) < settings.getMissChance()) {
            // РџСЂРѕРјР°С… - РїСЂРѕСЃС‚Рѕ РјР°С€РµРј СЂСѓРєРѕР№
            try {
                server.getCommandManager().getDispatcher().execute(
                    "player " + bot.getName().getString() + " swinghand", 
                    server.getCommandSource()
                );
            } catch (Exception e) {
                bot.swingHand(Hand.MAIN_HAND);
            }
            return;
        }
        
        // РЁР°РЅСЃ РѕС€РёР±РєРё - Р°С‚Р°РєСѓРµРј РЅРµ С‚СѓРґР°
        if (random.nextInt(100) < settings.getMistakeChance()) {
            // РџРѕРІРѕСЂР°С‡РёРІР°РµРјСЃСЏ РЅРµРјРЅРѕРіРѕ РІ СЃС‚РѕСЂРѕРЅСѓ
            float yawOffset = (random.nextFloat() - 0.5f) * 60; // В±30 РіСЂР°РґСѓСЃРѕРІ
            bot.setYaw(bot.getYaw() + yawOffset);
        }
        
        try {
            server.getCommandManager().getDispatcher().execute(
                "player " + bot.getName().getString() + " attack once", 
                server.getCommandSource()
            );
        } catch (Exception e) {
            bot.swingHand(Hand.MAIN_HAND);
        }
    }
    
    /**
     * РќР°С‡Р°С‚СЊ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ С‰РёС‚Р° С‡РµСЂРµР· Carpet РєРѕРјР°РЅРґСѓ
     */
    private static void startUsingShield(ServerPlayerEntity bot, net.minecraft.server.MinecraftServer server) {
        try {
            server.getCommandManager().getDispatcher().execute(
                "player " + bot.getName().getString() + " use continuous", 
                server.getCommandSource()
            );
        } catch (Exception e) {
            // Fallback - РёСЃРїРѕР»СЊР·СѓРµРј РѕР±С‹С‡РЅС‹Р№ СЃРїРѕСЃРѕР±
            bot.setCurrentHand(Hand.OFF_HAND);
        }
    }
    
    /**
     * РџСЂРµРєСЂР°С‚РёС‚СЊ РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРµ С‰РёС‚Р° С‡РµСЂРµР· Carpet РєРѕРјР°РЅРґСѓ
     */
    private static void stopUsingShield(ServerPlayerEntity bot, net.minecraft.server.MinecraftServer server) {
        try {
            server.getCommandManager().getDispatcher().execute(
                "player " + bot.getName().getString() + " stop", 
                server.getCommandSource()
            );
        } catch (Exception e) {
            // Fallback - РёСЃРїРѕР»СЊР·СѓРµРј РѕР±С‹С‡РЅС‹Р№ СЃРїРѕСЃРѕР±
            bot.clearActiveItem();
        }
    }
    
    /**
     * РџРѕРІРѕСЂРѕС‚ Рє С†РµР»Рё
     */
    private static void lookAtTarget(ServerPlayerEntity bot, Entity target) {
        Vec3d targetPos = target.getEyePos();
        Vec3d botPos = bot.getEyePos();
        
        double dx = targetPos.x - botPos.x;
        double dy = targetPos.y - botPos.y;
        double dz = targetPos.z - botPos.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(MathHelper.atan2(dy, horizontalDist) * (180.0 / Math.PI));
        
        bot.setYaw(yaw);
        bot.setPitch(pitch);
        bot.setHeadYaw(yaw);
    }
    
    /**
     * РџРѕРІРѕСЂРѕС‚ РћРў С†РµР»Рё (РґР»СЏ СѓР±РµРіР°РЅРёСЏ)
     */
    private static void lookAwayFromTarget(ServerPlayerEntity bot, Entity target) {
        Vec3d targetPos = target.getEyePos();
        Vec3d botPos = bot.getEyePos();
        
        // РќР°РїСЂР°РІР»РµРЅРёРµ РћРў С†РµР»Рё (РїСЂРѕС‚РёРІРѕРїРѕР»РѕР¶РЅРѕРµ)
        double dx = botPos.x - targetPos.x;
        double dz = botPos.z - targetPos.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        
        bot.setYaw(yaw);
        bot.setPitch(0); // РЎРјРѕС‚СЂРёРј РїСЂСЏРјРѕ
        bot.setHeadYaw(yaw);
    }
    
    /**
     * Р”РІРёР¶РµРЅРёРµ Рє С†РµР»Рё
     */
    private static void moveToward(ServerPlayerEntity bot, Entity target, double speed) {
        double botX = bot.getX(), botY = bot.getY(), botZ = bot.getZ();
        double targetX = target.getX(), targetY = target.getY(), targetZ = target.getZ();
        double dx = targetX - botX;
        double dz = targetZ - botZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) {
            dx /= dist;
            dz /= dist;
        }
        
        bot.setSprinting(true);
        bot.forwardSpeed = (float) speed;
        bot.sidewaysSpeed = 0;
        
        // Р”РѕР±Р°РІР»СЏРµРј РёРјРїСѓР»СЊСЃ РґРІРёР¶РµРЅРёСЏ
        if (bot.isOnGround()) {
            bot.addVelocity(dx * speed * 0.1, 0, dz * speed * 0.1);
        }
    }
    
    /**
     * Р”РІРёР¶РµРЅРёРµ РѕС‚ С†РµР»Рё (СѓР±РµРіР°РЅРёРµ)
     */
    private static void moveAway(ServerPlayerEntity bot, Entity target, double speed) {
        double botX = bot.getX(), botY = bot.getY(), botZ = bot.getZ();
        double targetX = target.getX(), targetY = target.getY(), targetZ = target.getZ();
        double dx = botX - targetX;
        double dz = botZ - targetZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) {
            dx /= dist;
            dz /= dist;
        }
        
        // Р‘РµР¶РёРј Р’РџР•Р РЃР” (РІ РЅР°РїСЂР°РІР»РµРЅРёРё РѕС‚ РІСЂР°РіР°)
        bot.setSprinting(true);
        bot.forwardSpeed = (float) speed;
        
        if (bot.isOnGround()) {
            bot.addVelocity(dx * speed * 0.1, 0, dz * speed * 0.1);
        }
    }
    
    // ============ РџРѕРёСЃРє РѕСЂСѓР¶РёСЏ РІ РёРЅРІРµРЅС‚Р°СЂРµ ============
    
    private static int findMeleeWeapon(net.minecraft.entity.player.PlayerInventory inventory) {
        BotSettings settings = BotSettings.get();
        boolean preferSword = settings.isPreferSword();
        
        int bestSlot = -1;
        double bestScore = 0;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            
            Item item = stack.getItem();
            double score = getMeleeScore(item, preferSword);
            
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        
        return bestSlot;
    }
    
    /**
     * РџРѕРёСЃРє С‚РѕРїРѕСЂР° РґР»СЏ СЃР±РёС‚РёСЏ С‰РёС‚Р°
     */
    private static int findAxe(net.minecraft.entity.player.PlayerInventory inventory) {
        int bestSlot = -1;
        double bestDamage = 0;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            
            Item item = stack.getItem();
            if (item instanceof AxeItem) {
                double damage = getAxeDamage(item);
                if (damage > bestDamage) {
                    bestDamage = damage;
                    bestSlot = i;
                }
            }
        }
        
        return bestSlot;
    }
    
    /**
     * РџРѕРёСЃРє С‰РёС‚Р° РІ РёРЅРІРµРЅС‚Р°СЂРµ
     */
    private static int findShield(net.minecraft.entity.player.PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) continue;
            
            // РџСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ СЌС‚Рѕ С‰РёС‚
            if (stack.getItem().toString().contains("shield")) {
                return i;
            }
        }
        return -1;
    }
    
    private static double getAxeDamage(Item item) {
        if (item == Items.NETHERITE_AXE) return 10;
        if (item == Items.DIAMOND_AXE) return 9;
        if (item == Items.IRON_AXE) return 9;
        if (item == Items.STONE_AXE) return 9;
        if (item == Items.GOLDEN_AXE) return 7;
        if (item == Items.WOODEN_AXE) return 7;
        return 0;
    }
    
    /**
     * РџРѕР»СѓС‡РёС‚СЊ "РѕС‡РєРё" РѕСЂСѓР¶РёСЏ СЃ СѓС‡С‘С‚РѕРј preferSword
     * Р•СЃР»Рё preferSword = true, РјРµС‡Рё РїРѕР»СѓС‡Р°СЋС‚ Р±РѕРЅСѓСЃ +5 Рє РѕС‡РєР°Рј
     */
    private static double getMeleeScore(Item item, boolean preferSword) {
        double baseDamage = getMeleeDamage(item);
        if (baseDamage == 0) return 0;
        
        // Р•СЃР»Рё РїСЂРµРґРїРѕС‡РёС‚Р°РµРј РјРµС‡ - РґР°С‘Рј РјРµС‡Р°Рј Р±РѕРЅСѓСЃ
        if (preferSword && isSword(item)) {
            return baseDamage + 5; // РњРµС‡ РІСЃРµРіРґР° Р±СѓРґРµС‚ РІС‹Р±СЂР°РЅ РµСЃР»Рё РµСЃС‚СЊ
        }
        
        return baseDamage;
    }
    
    /**
     * РџСЂРѕРІРµСЂСЏРµС‚, СЏРІР»СЏРµС‚СЃСЏ Р»Рё РїСЂРµРґРјРµС‚ РјРµС‡РѕРј
     */
    private static boolean isSword(Item item) {
        return item == Items.NETHERITE_SWORD || 
               item == Items.DIAMOND_SWORD || 
               item == Items.IRON_SWORD || 
               item == Items.GOLDEN_SWORD || 
               item == Items.STONE_SWORD || 
               item == Items.WOODEN_SWORD;
    }
    
    private static double getMeleeDamage(Item item) {
        if (item == Items.NETHERITE_SWORD) return 8;
        if (item == Items.NETHERITE_AXE) return 10;
        if (item == Items.DIAMOND_SWORD) return 7;
        if (item == Items.DIAMOND_AXE) return 9;
        if (item == Items.IRON_SWORD) return 6;
        if (item == Items.IRON_AXE) return 9;
        if (item == Items.STONE_SWORD) return 5;
        if (item == Items.STONE_AXE) return 9;
        if (item == Items.GOLDEN_SWORD) return 4;
        if (item == Items.GOLDEN_AXE) return 7;
        if (item == Items.WOODEN_SWORD) return 4;
        if (item == Items.WOODEN_AXE) return 7;
        if (item == Items.TRIDENT) return 9;
        return 0;
    }
    
    private static int findRangedWeapon(net.minecraft.entity.player.PlayerInventory inventory) {
        // РџСЂРёРѕСЂРёС‚РµС‚: Р°СЂР±Р°Р»РµС‚ > Р»СѓРє
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof CrossbowItem) return i;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof BowItem) return i;
        }
        return -1;
    }
    
    private static int findMace(net.minecraft.entity.player.PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.MACE) return i;
        }
        return -1;
    }
    
    private static int findWindCharge(net.minecraft.entity.player.PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.WIND_CHARGE) return i;
        }
        return -1;
    }
    
    /**
     * РџРѕРёСЃРє РєРѕРїСЊСЏ (Spear) РІ РёРЅРІРµРЅС‚Р°СЂРµ - 1.21.11
     * РљРѕРїСЊС‘ - РЅРѕРІРѕРµ РѕСЂСѓР¶РёРµ СЃ charge Р°С‚Р°РєРѕР№
     */
    private static int findSpear(net.minecraft.entity.player.PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            // РџСЂРѕРІРµСЂСЏРµРј РїРѕ РёРјРµРЅРё РїСЂРµРґРјРµС‚Р°, С‚Р°Рє РєР°Рє Items.SPEAR РјРѕР¶РµС‚ РЅРµ СЃСѓС‰РµСЃС‚РІРѕРІР°С‚СЊ РІ С‚РµРєСѓС‰РµР№ РІРµСЂСЃРёРё
            String itemName = stack.getItem().toString().toLowerCase();
            if (itemName.contains("spear")) return i;
        }
        return -1;
    }
    
    /**
     * РџРѕРёСЃРє РїР°СѓС‚РёРЅС‹ РІ РёРЅРІРµРЅС‚Р°СЂРµ
     */
    private static int findCobweb(net.minecraft.entity.player.PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() == Items.COBWEB) return i;
        }
        return -1;
    }
    
    /**
     * РќР°С‡РёРЅР°РµС‚ РїСЂРѕС†РµСЃСЃ СЂР°Р·РјРµС‰РµРЅРёСЏ РїР°СѓС‚РёРЅС‹ РїРѕРґ РІСЂР°РіР°
     * Р‘РѕС‚ Р±РµСЂС‘С‚ РїР°СѓС‚РёРЅСѓ РІ СЂСѓРєСѓ, СЃРјРѕС‚СЂРёС‚ РЅР° РІСЂР°РіР° Рё РєР»РёРєР°РµС‚ РџРљРњ
     */
    private static boolean tryPlaceCobweb(ServerPlayerEntity bot, Entity target, net.minecraft.server.MinecraftServer server) {
        CombatState state = getState(bot.getName().getString());
        
        // РЈР¶Рµ СЂР°Р·РјРµС‰Р°РµРј РїР°СѓС‚РёРЅСѓ
        if (state.isPlacingCobweb) return false;
        
        var inventory = bot.getInventory();
        int cobwebSlot = findCobweb(inventory);
        if (cobwebSlot < 0) return false;
        
        // РџСЂРѕРІРµСЂСЏРµРј С‡С‚Рѕ РІСЂР°Рі РЅРµ РІ РїР°СѓС‚РёРЅРµ СѓР¶Рµ
        var world = bot.getEntityWorld();
        net.minecraft.util.math.BlockPos targetPos = target.getBlockPos();
        if (world.getBlockState(targetPos).getBlock() == net.minecraft.block.Blocks.COBWEB) {
            return false; // РЈР¶Рµ РІ РїР°СѓС‚РёРЅРµ
        }
        
        // РџРµСЂРµРјРµС‰Р°РµРј РїР°СѓС‚РёРЅСѓ РІ С…РѕС‚Р±Р°СЂ РµСЃР»Рё РЅСѓР¶РЅРѕ
        if (cobwebSlot >= 9) {
            ItemStack cobweb = inventory.getStack(cobwebSlot);
            ItemStack current = inventory.getStack(0);
            inventory.setStack(cobwebSlot, current);
            inventory.setStack(0, cobweb);
            cobwebSlot = 0;
        }
        
        // РџРµСЂРµРєР»СЋС‡Р°РµРј РЅР° РїР°СѓС‚РёРЅСѓ
        org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, cobwebSlot);
        
        // РќР°С‡РёРЅР°РµРј РїСЂРѕС†РµСЃСЃ СЂР°Р·РјРµС‰РµРЅРёСЏ
        state.isPlacingCobweb = true;
        state.cobwebPlaceTicks = 0;
        
        return true;
    }
    
    /**
     * РћР±СЂР°Р±РѕС‚РєР° РїСЂРѕС†РµСЃСЃР° СЂР°Р·РјРµС‰РµРЅРёСЏ РїР°СѓС‚РёРЅС‹
     * Р’С‹Р·С‹РІР°РµС‚СЃСЏ РєР°Р¶РґС‹Р№ С‚РёРє РєРѕРіРґР° isPlacingCobweb = true
     */
    private static void handleCobwebPlacement(ServerPlayerEntity bot, Entity target, CombatState state, net.minecraft.server.MinecraftServer server) {
        state.cobwebPlaceTicks++;
        
        // РЎРјРѕС‚СЂРёРј РЅР° РїРѕР·РёС†РёСЋ РІСЂР°РіР° (РїРѕРґ РЅРѕРіРё)
        Vec3d targetFeet = new Vec3d(target.getX(), target.getY() - 0.5, target.getZ());
        Vec3d botPos = bot.getEyePos();
        
        double dx = targetFeet.x - botPos.x;
        double dy = targetFeet.y - botPos.y;
        double dz = targetFeet.z - botPos.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI));
        
        bot.setYaw(yaw);
        bot.setPitch(pitch);
        bot.setHeadYaw(yaw);
        
        // РљР»РёРєР°РµРј РџРљРњ РЅРµСЃРєРѕР»СЊРєРѕ СЂР°Р· РґР»СЏ РЅР°РґС‘Р¶РЅРѕСЃС‚Рё
        if (state.cobwebPlaceTicks % 2 == 0 && state.cobwebPlaceTicks <= 6) {
            try {
                server.getCommandManager().getDispatcher().execute(
                    "player " + bot.getName().getString() + " use once", 
                    server.getCommandSource()
                );
            } catch (Exception e) {
                // РРіРЅРѕСЂРёСЂСѓРµРј
            }
        }
        
        // Р—Р°РєР°РЅС‡РёРІР°РµРј С‡РµСЂРµР· 8 С‚РёРєРѕРІ
        if (state.cobwebPlaceTicks >= 8) {
            state.isPlacingCobweb = false;
            state.cobwebPlaceTicks = 0;
            state.cobwebCooldown = 20; // 1 СЃРµРєСѓРЅРґР° РєСѓР»РґР°СѓРЅ
        }
    }
    
    private static boolean hasArrows(net.minecraft.entity.player.PlayerInventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof ArrowItem) return true;
        }
        return false;
    }
    
    // ============ РџСѓР±Р»РёС‡РЅС‹Рµ РјРµС‚РѕРґС‹ РґР»СЏ РєРѕРјР°РЅРґ ============
    
    /**
     * РЈСЃС‚Р°РЅРѕРІРёС‚СЊ РїСЂРёРЅСѓРґРёС‚РµР»СЊРЅСѓСЋ С†РµР»СЊ
     */
    public static void setTarget(String botName, String targetName) {
        CombatState state = getState(botName);
        state.forcedTargetName = targetName;
    }
    
    /**
     * РЎР±СЂРѕСЃРёС‚СЊ С†РµР»СЊ (РїРѕР»РЅРѕСЃС‚СЊСЋ РѕСЃС‚Р°РЅР°РІР»РёРІР°РµС‚ Р±РѕР№)
     */
    public static void clearTarget(String botName) {
        CombatState state = getState(botName);
        state.forcedTargetName = null;
        state.target = null;
        state.lastAttacker = null; // РЎР±СЂР°СЃС‹РІР°РµРј revenge
        state.lastAttackTime = 0;
        state.isRetreating = false;
    }
    
    /**
     * Р’С‹Р·С‹РІР°РµС‚СЃСЏ РєРѕРіРґР° Р±РѕС‚Р° Р°С‚Р°РєСѓСЋС‚
     */
    public static void onBotDamaged(ServerPlayerEntity bot, DamageSource source) {
        // РџСЂРѕР±СѓРµРј РїРѕР»СѓС‡РёС‚СЊ Р°С‚Р°РєСѓСЋС‰РµРіРѕ СЂР°Р·РЅС‹РјРё СЃРїРѕСЃРѕР±Р°РјРё
        Entity attacker = source.getAttacker();
        if (attacker == null) {
            attacker = source.getSource();
        }
        if (attacker == null || attacker == bot) return;
        
        // РќРµ СЂРµР°РіРёСЂСѓРµРј РЅР° СѓСЂРѕРЅ РѕС‚ СЃРµР±СЏ РёР»Рё РѕС‚ РѕРєСЂСѓР¶РµРЅРёСЏ
        if (!(attacker instanceof LivingEntity)) return;
        
        CombatState state = getState(bot.getName().getString());
        state.lastAttacker = attacker;
        state.lastAttackTime = System.currentTimeMillis();
    }
    
    /**
     * РџРѕР»СѓС‡РёС‚СЊ С‚РµРєСѓС‰СѓСЋ С†РµР»СЊ Р±РѕС‚Р°
     */
    public static Entity getTarget(String botName) {
        return getState(botName).target;
    }
}
