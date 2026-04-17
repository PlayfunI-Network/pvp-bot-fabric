package org.stepan1411.pvp_bot.bot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BotCombat {
    

    private static final Map<String, CombatState> combatStates = new HashMap<>();
    
    public static class CombatState {
        public Entity target = null;
        public String forcedTargetName = null;
        public int attackCooldown = 0;
        public int bowDrawTicks = 0;
        public boolean isDrawingBow = false;
        public Entity lastAttacker = null;
        public long lastAttackTime = 0;
        public WeaponMode currentMode = WeaponMode.MELEE;
        public float lastHealth = 20.0f;
        public boolean isRetreating = false;
        

        public boolean isChargingSpear = false;
        public int spearChargeTicks = 0;
        

        public int cobwebCooldown = 0;
        

        public boolean isUsingShield = false;
        public int shieldToggleCooldown = 0;
        public int airTimeTicks = 0;
        public boolean shieldBroken = false;
        public long shieldBrokenTime = 0;
        public boolean isPlacingCobweb = false;
        public int cobwebPlaceTicks = 0;
        

        public boolean isCrystalPvping = false;
        public int crystalCooldown = 0;
        public net.minecraft.core.BlockPos lastObsidianPos = null;
        public int crystalPvpStep = 0;
        public int crystalPvpTicks = 0;
        

        public boolean isMaceDefending = false;
        public int maceDefenseCooldown = 0;
        
        public enum WeaponMode {
            MELEE,
            RANGED,
            MACE,
            SPEAR,
            CRYSTAL,
            ANCHOR,
            ELYTRA_MACE
        }
    }
    
    public static CombatState getState(String botName) {
        return combatStates.computeIfAbsent(botName, k -> new CombatState());
    }
    
    public static void removeState(String botName) {
        combatStates.remove(botName);
    }
    
    
    public static void update(ServerPlayer bot, net.minecraft.server.MinecraftServer server) {
        BotSettings settings = BotSettings.get();
        if (!settings.isCombatEnabled()) return;
        
        CombatState state = getState(bot.getName().getString());
        

        float currentHealth = bot.getHealth();
        if (currentHealth < state.lastHealth && settings.isRevengeEnabled()) {

            Entity attacker = bot.getLastHurtByMob();
            if (attacker != null && attacker != bot && attacker instanceof LivingEntity) {
                state.lastAttacker = attacker;
                state.lastAttackTime = System.currentTimeMillis();
            }
        }
        state.lastHealth = currentHealth;
        

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
        

        Entity target = findTarget(bot, state, settings, server);
        state.target = target;
        

        if (target != null) {
            BotDebug.showTargetEntity(bot, target);
        }

        

        if (state.isPlacingCobweb && target != null) {
            handleCobwebPlacement(bot, target, state, server);
            return;
        }
        
        if (target == null) {

            if (state.isDrawingBow) {
                stopUsingBow(bot, state);
            }

            BotNavigation.idleWander(bot);
            return;
        }
        

        BotNavigation.resetIdle(bot.getName().getString());
        

        double distance = bot.distanceTo(target);
        

        float health = bot.getHealth();
        float maxHealth = bot.getMaxHealth();
        float healthPercent = health / maxHealth;
        boolean lowHealth = healthPercent <= settings.getRetreatHealthPercent();
        boolean criticalHealth = healthPercent <= settings.getCriticalHealthPercent();
        

        boolean hasFood = BotUtils.hasFood(bot);
        

        if (state.shieldBroken && System.currentTimeMillis() - state.shieldBrokenTime > 5000) {
            state.shieldBroken = false;
        }
        

        var utilsState = BotUtils.getState(bot.getName().getString());
        
        if (utilsState.isEscapingCobweb) {
            return;
        }
        
        if (utilsState.needsToCollectWater) {
            return;
        }
        
        boolean isEating = utilsState.isEating;
        
        if (isEating && settings.isRetreatEnabled()) {

            state.isRetreating = true;
            if (state.isDrawingBow) {
                stopUsingBow(bot, state);
            }

            BotNavigation.lookAway(bot, target);
            BotNavigation.moveAway(bot, target, 1.2);
            return;
        } else if (isEating) {

            if (state.isDrawingBow) {
                stopUsingBow(bot, state);
            }

        }
        

        if (lowHealth && settings.isAutoPotionEnabled()) {
            if (BotUtils.tryUseHealingPotion(bot, server)) {

                return;
            }
        }
        




        boolean shouldRetreat = settings.isRetreatEnabled() && hasFood && 
                               (criticalHealth || (lowHealth && !state.shieldBroken));
        


        if (shouldRetreat) {
            state.isRetreating = true;
            

            if (settings.isAutoShieldEnabled()) {
                var inventory = bot.getInventory();

                ItemStack offhandItem = bot.getOffhandItem();
                if (offhandItem.isEmpty() || !offhandItem.getItem().toString().contains("shield")) {
                    int shieldSlot = findShield(inventory);
                    if (shieldSlot >= 0) {

                        ItemStack shield = inventory.getItem(shieldSlot);
                        inventory.setItem(40, shield);
                        inventory.setItem(shieldSlot, ItemStack.EMPTY);
                    }
                }
                

                if (!state.isUsingShield && state.shieldToggleCooldown <= 0) {
                    startUsingShield(bot, server);
                    state.isUsingShield = true;
                }
            }
            

            if (distance < 25.0) {

                if (settings.isCobwebEnabled() && distance < 8.0 && state.cobwebCooldown <= 0 && !state.isPlacingCobweb) {
                    tryPlaceCobweb(bot, target, server);
                }
                BotNavigation.lookAway(bot, target);
                BotNavigation.moveAway(bot, target, 1.5);
            }

            return;
        }
        state.isRetreating = false;
        

        if (utilsState.isMending) {
            return;
        }
        


        try {
            var strategyRegistry = org.stepan1411.pvp_bot.api.combat.CombatStrategyRegistry.getInstance();
            boolean strategyExecuted = strategyRegistry.executeStrategy(bot, target, settings, server);
            if (strategyExecuted) {

                return;
            }
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Error executing combat strategy: " + e.getMessage());
            e.printStackTrace();
        }



        selectWeaponMode(bot, state, distance, settings);
        


        boolean shouldLookAt = !utilsState.isThrowingPotion;
        
        if (shouldLookAt && settings.isUseBaritone()) {

            var mainHandStack = bot.getMainHandItem();
            boolean usingMace = mainHandStack.getItem().toString().toLowerCase().contains("mace");
            
            if (usingMace) {

                shouldLookAt = !bot.isOnGround() || distance <= 5.0;
            } else {

                shouldLookAt = distance <= 3.5;
            }
        }
        
        if (shouldLookAt) {
            BotNavigation.lookAt(bot, target);
        }
        

        double maxRange = switch (state.currentMode) {
            case MELEE -> settings.getMeleeRange() * 2;
            case RANGED -> settings.getRangedOptimalRange() + 15;
            case MACE -> settings.getMaceRange() * 2;
            case SPEAR -> settings.getSpearChargeRange();
            case CRYSTAL -> 10.0;
            case ANCHOR -> 10.0;
            case ELYTRA_MACE -> 15.0;
        };
        
        if (distance > maxRange) {

            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
            return;
        }
        

        handleMaceDefense(bot, target, settings, server);
        

        switch (state.currentMode) {
            case MELEE -> handleMeleeCombat(bot, target, state, distance, settings, server);
            case RANGED -> handleRangedCombat(bot, target, state, distance, settings, server);
            case MACE -> handleMaceCombat(bot, target, state, distance, settings, server);
            case SPEAR -> handleSpearCombat(bot, target, state, distance, settings, server);
            case CRYSTAL -> {
                boolean handled = BotCrystalPvp.doCrystalPvp(bot, target, settings, server);
                if (!handled) {
                    state.currentMode = CombatState.WeaponMode.MELEE;
                    handleMeleeCombat(bot, target, state, distance, settings, server);
                }
            }
            case ANCHOR -> {
                boolean handled = BotAnchorPvp.doAnchorPvp(bot, target, settings, server);
                if (!handled) {
                    state.currentMode = CombatState.WeaponMode.MELEE;
                    handleMeleeCombat(bot, target, state, distance, settings, server);
                }
            }
            case ELYTRA_MACE -> {

                boolean handled = BotElytraMace.doElytraMace(bot, target, settings, server);
                if (!handled) {
                    state.currentMode = CombatState.WeaponMode.MELEE;
                    handleMeleeCombat(bot, target, state, distance, settings, server);
                }
            }
        }
    }
    
    
    private static Entity findTarget(ServerPlayer bot, CombatState state, BotSettings settings, net.minecraft.server.MinecraftServer server) {

        if (state.forcedTargetName != null) {
            Entity forced = findEntityByName(bot, state.forcedTargetName, server);
            if (forced != null && forced.isAlive()) {
                double dist = bot.distanceTo(forced);
                if (dist <= settings.getMaxTargetDistance()) {
                    return forced;
                }
            }


        }
        

        if (settings.isRevengeEnabled() && state.lastAttacker != null) {

            if (!state.lastAttacker.isRemoved() && state.lastAttacker.isAlive()) {

                if (!settings.isFriendlyFireEnabled() && state.lastAttacker instanceof Player) {
                    String attackerName = state.lastAttacker.getName().getString();
                    if (BotFaction.areAllies(bot.getName().getString(), attackerName)) {
                        state.lastAttacker = null;
                    }
                }
                
                if (state.lastAttacker != null) {
                    double dist = bot.distanceTo(state.lastAttacker);
                    if (dist <= settings.getMaxTargetDistance()) {

                        if (dist <= 10.0) {
                            state.lastAttackTime = System.currentTimeMillis();
                        }
                        return state.lastAttacker;
                    }
                }
            }

            if (state.lastAttacker == null || state.lastAttacker.isRemoved() || !state.lastAttacker.isAlive() || 
                System.currentTimeMillis() - state.lastAttackTime >= 30000) {
                state.lastAttacker = null;
            }
        }
        

        if (settings.isFactionsEnabled()) {
            Entity factionEnemy = findFactionEnemy(bot, settings, server);
            if (factionEnemy != null) {
                return factionEnemy;
            }
        }
        

        if (settings.isAutoTargetEnabled()) {
            return findNearestEnemy(bot, settings, server);
        }
        
        return null;
    }
    
    
    private static Entity findFactionEnemy(ServerPlayer bot, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        String botName = bot.getName().getString();
        String botFaction = BotFaction.getFaction(botName);
        

        if (botFaction == null) return null;
        
        double maxDist = settings.getMaxTargetDistance();
        Entity nearest = null;
        double nearestDist = maxDist + 1;
        
        if (server != null) {
            for (var player : server.getPlayerList().getPlayers()) {
                if (player == bot) continue;
                if (!player.isAlive()) continue;
                if (player.isSpectator() || player.isCreative()) continue;
                
                String targetName = player.getName().getString();
                

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
    
    private static Entity findEntityByName(ServerPlayer bot, String name, net.minecraft.server.MinecraftServer server) {

        if (server != null) {
            var player = server.getPlayerList().getPlayerByName(name);
            if (player != null && player != bot) return player;
        }
        

        if (server != null) {
            for (var world : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity.getName().getString().equalsIgnoreCase(name) && entity != bot) {
                        return entity;
                    }
                }
            }
        }
        return null;
    }
    
    private static Entity findNearestEnemy(ServerPlayer bot, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        double maxDist = settings.getMaxTargetDistance();
        Box searchBox = bot.getBoundingAABB().expand(maxDist);
        
        Entity nearest = null;
        double nearestDist = maxDist + 1;
        

        if (server != null) {
            for (var world : server.getAllLevels()) {
                for (Entity entity : level.getEntities(bot, searchBox)) {
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
    
    private static boolean isValidTarget(ServerPlayer bot, Entity entity, BotSettings settings) {
        if (entity == bot) return false;
        if (!entity.isAlive()) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        
        String botName = bot.getName().getString();
        

        if (entity instanceof Player player) {
            if (player.isSpectator() || player.isCreative()) return false;
            
            String targetName = player.getName().getString();
            

            if (settings.isFactionsEnabled()) {

                if (!settings.isFriendlyFireEnabled() && BotFaction.areAllies(botName, targetName)) {
                    return false;
                }

                if (BotFaction.areEnemies(botName, targetName)) {
                    return true;
                }
            }
            

            if (BotManager.getAllBots().contains(targetName)) {
                if (!settings.isTargetOtherBots()) return false;
            } else {
                if (!settings.isTargetPlayers()) return false;
            }
            
            return true;
        }
        

        if (entity instanceof Monster) {
            return settings.isTargetHostileMobs();
        }
        

        if (living instanceof net.minecraft.world.entity.Mob) {
            return settings.isTargetHostileMobs();
        }
        
        return false;
    }

    
    
    private static void selectWeaponMode(ServerPlayer bot, CombatState state, double distance, BotSettings settings) {
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
        

        if (target != null && BotElytraMace.canUseElytraMace(bot, target, settings)) {
            state.currentMode = CombatState.WeaponMode.ELYTRA_MACE;
        } else if (target != null && BotCrystalPvp.canUseCrystalPvp(bot, target, settings)) {
            state.currentMode = CombatState.WeaponMode.CRYSTAL;
        } else if (target != null && BotAnchorPvp.canUseAnchorPvp(bot, target, settings)) {
            state.currentMode = CombatState.WeaponMode.ANCHOR;
        } else if (hasMace && distance <= maceRange && settings.isMaceEnabled()) {
            state.currentMode = CombatState.WeaponMode.MACE;
        } else if (hasSpear && distance <= spearRange && settings.isSpearEnabled()) {
            state.currentMode = CombatState.WeaponMode.SPEAR;
        } else if (hasRanged && distance > rangedMinRange && settings.isRangedEnabled()) {
            state.currentMode = CombatState.WeaponMode.RANGED;
        } else if (hasMelee && distance <= meleeRange * 2) {
            state.currentMode = CombatState.WeaponMode.MELEE;
        } else if (hasSpear && settings.isSpearEnabled()) {
            state.currentMode = CombatState.WeaponMode.SPEAR;
        } else if (hasRanged && settings.isRangedEnabled()) {
            state.currentMode = CombatState.WeaponMode.RANGED;
        } else {
            state.currentMode = CombatState.WeaponMode.MELEE;
        }
    }
    
    
    private static boolean isTargetMaceAttacking(Entity target) {
        if (!(target instanceof Player player)) return false;
        

        ItemStack mainHand = player.getMainHandItem();
        boolean hasMace = mainInteractionHand.getItem() == Items.MACE;
        
        if (!hasMace) return false;
        

        boolean inAir = !player.isOnGround();
        if (!inAir) return false;
        

        double velocityY = player.getDeltaMovement().y;
        



        boolean willAttackSoon = velocityY < -0.08;
        
        if (willAttackSoon) {
            System.out.println("[MACE_DETECTION] " + player.getName().getString() + " attacking with mace! (vel: " + String.format("%.2f", velocityY) + ")");
        }
        
        return willAttackSoon;
    }
    
    
    private static void handleMaceDefense(ServerPlayer bot, Entity target, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        if (!settings.isShieldMace()) {
            return;
        }
        
        var inventory = bot.getInventory();
        var utilsState = BotUtils.getState(bot.getName().getString());
        var combatState = getState(bot.getName().getString());
        

        if (combatState.maceDefenseCooldown > 0) {
            combatState.maceDefenseCooldown--;
        }
        
        boolean isMaceAttacking = isTargetMaceAttacking(target);
        
        if (isMaceAttacking) {

            combatState.maceDefenseCooldown = 20;
            combatState.isMaceDefending = true;
            System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " detected mace attack from " + target.getName().getString() + "! Settings: shieldmace=" + settings.isShieldMace());
        }
        

        boolean shouldDefend = combatState.isMaceDefending && combatState.maceDefenseCooldown > 0;
        
        if (!shouldDefend) {

            if (combatState.isUsingShield && combatState.isMaceDefending) {
                try {
                    server.getCommands().getDispatcher().execute(
                        "player " + bot.getName().getString() + " stop", 
                        server.getSharedSuggestionProvider()
                    );
                    System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " stopped defending (cooldown expired)");
                } catch (Exception e) {
                    bot.clearActiveItem();
                }
                combatState.isUsingShield = false;
                combatState.isMaceDefending = false;
            }
            return;
        }
        

        int shieldSlot = findShield(inventory);
        if (shieldSlot < 0) {
            System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " has no shield!");
            return;
        }
        

        ItemStack offhand = inventory.getItem(40);
        boolean hasTotem = offhand.getItem() == Items.TOTEM_OF_UNDYING;
        
        if (hasTotem && !settings.isPreferShieldMace()) {
            System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " has totem and preferShieldMace is false, skipping");
            return;
        }
        

        if (settings.isShieldMainHand() && hasTotem) {

            if (shieldSlot != 0) {
                ItemStack shield = inventory.getItem(shieldSlot);
                ItemStack current = inventory.getItem(0);
                inventory.setItem(shieldSlot, current);
                inventory.setItem(0, shield);
                shieldSlot = 0;
                System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " moved shield to main hand (slot 0)");
            }
            

            org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, 0);
            

            if (!combatState.isUsingShield) {
                try {
                    server.getCommands().getDispatcher().execute(
                        "player " + bot.getName().getString() + " use continuous", 
                        server.getSharedSuggestionProvider()
                    );
                    System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " activating shield in main hand");
                } catch (Exception e) {
                    bot.setCurrentHand(InteractionInteractionHand.MAIN_HAND);
                }
                combatState.isUsingShield = true;
            }
        } else {

            if (shieldSlot != 40) {
                ItemStack shield = inventory.getItem(shieldSlot);
                ItemStack current = inventory.getItem(40);
                inventory.setItem(shieldSlot, current);
                inventory.setItem(40, shield);
                System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " moved shield to offhand");
            }
            

            if (!combatState.isUsingShield) {
                try {
                    server.getCommands().getDispatcher().execute(
                        "player " + bot.getName().getString() + " use continuous", 
                        server.getSharedSuggestionProvider()
                    );
                    System.out.println("[MACE_DEFENSE] " + bot.getName().getString() + " activating shield in offhand");
                } catch (Exception e) {
                    bot.setCurrentHand(InteractionInteractionHand.OFF_HAND);
                }
                combatState.isUsingShield = true;
            }
        }
    }
    
    
    private static void handleMeleeCombat(ServerPlayer bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {
        var utilsState = BotUtils.getState(bot.getName().getString());
        

        if (state.isMaceDefending && state.maceDefenseCooldown > 0) {

            lookAtTarget(bot, target);
            return;
        }
        
        if (utilsState.isEating) {
            System.out.println("[COMBAT] " + bot.getName().getString() + " is eating, skipping combat");
            return;
        }
        
        var inventory = bot.getInventory();
        

        if (state.isDrawingBow) {
            stopUsingBow(bot, state);
        }
        
        double meleeRange = settings.getMeleeRange();
        

        if (settings.isCobwebEnabled() && distance < 6.0 && distance > 2.0 && state.cobwebCooldown <= 0 && !state.isPlacingCobweb) {

            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                Vec3 targetVel = living.getDeltaMovement();
                double targetSpeed = Math.sqrt(targetVel.x * targetVel.x + targetVel.z * targetVel.z);
                if (targetSpeed > 0.08) {
                    tryPlaceCobweb(bot, target, server);
                }
            }
        }
        

        if (distance > meleeRange) {
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
        } else if (distance < 1.5) {

            BotNavigation.moveAway(bot, target, 0.3);
        }
        

        float healthPercent = bot.getHealth() / bot.getMaxHealth();
        boolean shouldUseShield = settings.isAutoShieldEnabled() && healthPercent < settings.getShieldHealthThreshold();
        


        boolean willAttackSoon = distance <= meleeRange && state.attackCooldown == 1;
        boolean shouldHoldShield = shouldUseShield && !willAttackSoon;
        
        if (shouldUseShield) {

            ItemStack offhandItem = bot.getOffhandItem();
            if (offhandItem.isEmpty() || !offhandItem.getItem().toString().contains("shield")) {
                int shieldSlot = findShield(inventory);
                if (shieldSlot >= 0) {

                    ItemStack shield = inventory.getItem(shieldSlot);
                    inventory.setItem(40, shield);
                    inventory.setItem(shieldSlot, ItemStack.EMPTY);
                }
            }
            

            if (shouldHoldShield && !state.isUsingShield) {

                startUsingShield(bot, server);
                state.isUsingShield = true;
            } else if (!shouldHoldShield && state.isUsingShield) {

                stopUsingShield(bot, server);
                state.isUsingShield = false;
            }
        } else {

            if (state.isUsingShield && state.shieldToggleCooldown <= 0) {
                stopUsingShield(bot, server);
                state.isUsingShield = false;
                state.shieldToggleCooldown = 20;
            }
        }

        if (distance <= meleeRange && state.attackCooldown <= 0) {
            

            if (bot.getAttackStrengthScale(0.5f) < 1.0f) {
                return;
            }
            

            if (settings.isShieldBreakEnabled() && target instanceof Player player && player.isBlocking()) {

                int axeSlot = findAxe(inventory);
                if (axeSlot >= 0) {

                    if (axeSlot >= 9) {
                        ItemStack axe = inventory.getItem(axeSlot);
                        ItemStack current = inventory.getItem(0);
                        inventory.setItem(axeSlot, current);
                        inventory.setItem(0, axe);
                        axeSlot = 0;
                    }
                    org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, axeSlot);
                    

                    attackWithCarpet(bot, target, server);
                    

                    state.shieldBroken = true;
                    state.shieldBrokenTime = System.currentTimeMillis();
                    

                    int cooldown = shouldUseShield ? (int)(settings.getAttackCooldown() * 1.5) : settings.getAttackCooldown();
                    state.attackCooldown = cooldown;
                    


                    return;
                }
            }
            

            int currentSlot = org.stepan1411.pvp_bot.utils.InventoryHelper.getSelectedSlot(inventory);
            ItemStack currentItem = inventory.getItem(currentSlot);
            double currentScore = getMeleeScore(currentItem.getItem(), settings.isPreferSword());
            
            int weaponSlot = findMeleeWeapon(inventory);
            if (weaponSlot >= 0 && weaponSlot < 9) {

                ItemStack newWeapon = inventory.getItem(weaponSlot);
                double newScore = getMeleeScore(newWeapon.getItem(), settings.isPreferSword());
                
                if (newScore > currentScore || currentScore == 0) {
                    org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, weaponSlot);
                }
            }
            

            if (settings.isCriticalsEnabled()) {
                if (bot.isOnGround()) {
                    bot.jump();
                    return;
                } else {
                    double velocityY = bot.getDeltaMovement().y;
                    

                    if (velocityY < 0) {
                        attackWithCarpet(bot, target, server);
                        
                        int cooldown = shouldUseShield ? (int)(settings.getAttackCooldown() * 1.5) : settings.getAttackCooldown();
                        state.attackCooldown = cooldown;
                    }
                }
            } else {
                attackWithCarpet(bot, target, server);
                
                int cooldown = shouldUseShield ? (int)(settings.getAttackCooldown() * 1.5) : settings.getAttackCooldown();
                state.attackCooldown = cooldown;
            }
        } else {

            int currentSlot = org.stepan1411.pvp_bot.utils.InventoryHelper.getSelectedSlot(inventory);
            ItemStack currentItem = inventory.getItem(currentSlot);
            double currentScore = getMeleeScore(currentItem.getItem(), settings.isPreferSword());
            
            int weaponSlot = findMeleeWeapon(inventory);
            if (weaponSlot >= 0 && weaponSlot < 9) {

                ItemStack newWeapon = inventory.getItem(weaponSlot);
                double newScore = getMeleeScore(newWeapon.getItem(), settings.isPreferSword());
                
                if (newScore > currentScore || currentScore == 0) {
                    org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, weaponSlot);
                }
            }
        }
    }
    
    
    private static void handleRangedCombat(ServerPlayer bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {


        if (state.isMaceDefending && state.maceDefenseCooldown > 0) {
            lookAtTarget(bot, target);
            return;
        }

        var utilsState = BotUtils.getState(bot.getName().getString());
        if (utilsState.isEating) {
            return;
        }
        
        var inventory = bot.getInventory();
        

        int bowSlot = findRangedWeapon(inventory);
        if (bowSlot >= 0 && bowSlot < 9) {
            org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, bowSlot);
        }
        

        if (!hasArrows(inventory)) {

            state.currentMode = CombatState.WeaponMode.MELEE;
            return;
        }
        
        ItemStack weapon = bot.getMainHandItem();
        boolean isCrossbow = weapon.getItem() instanceof CrossbowItem;
        
        if (isCrossbow) {
            handleCrossbowCombat(bot, target, state, distance, settings);
        } else {
            handleBowCombat(bot, target, state, distance, settings);
        }
        

        double optimalRange = settings.getRangedOptimalRange();
        if (distance < optimalRange - 5) {

            double dx = bot.getX() - target.getX();
            double dz = bot.getZ() - target.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0) {
                dx /= dist;
                dz /= dist;
                bot.push(dx * settings.getMoveSpeed() * 0.05, 0, dz * settings.getMoveSpeed() * 0.05);

            }
        } else if (distance > optimalRange + 10) {
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
        }
    }
    
    private static void handleBowCombat(ServerPlayer bot, Entity target, CombatState state, double distance, BotSettings settings) {
        if (!state.isDrawingBow) {

            bot.setCurrentHand(InteractionInteractionHand.MAIN_HAND);
            state.isDrawingBow = true;
            state.bowDrawTicks = 0;
        } else {
            state.bowDrawTicks++;
            

            int minDrawTime = settings.getBowMinDrawTime();
            if (state.bowDrawTicks >= minDrawTime) {

                bot.stopUsingItem();
                state.isDrawingBow = false;
                state.bowDrawTicks = 0;
                state.attackCooldown = 5;
            }
        }
    }
    
    private static void handleCrossbowCombat(ServerPlayer bot, Entity target, CombatState state, double distance, BotSettings settings) {
        ItemStack crossbow = bot.getMainHandItem();
        
        if (CrossbowItem.isCharged(crossbow)) {

            bot.stopUsingItem();
            state.attackCooldown = 5;
            state.isDrawingBow = false;
        } else if (!state.isDrawingBow) {

            bot.setCurrentHand(InteractionInteractionHand.MAIN_HAND);
            state.isDrawingBow = true;
            state.bowDrawTicks = 0;
        } else {
            state.bowDrawTicks++;

            if (state.bowDrawTicks >= 25) {
                bot.stopUsingItem();
                state.isDrawingBow = false;
            }
        }
    }
    
    private static void stopUsingBow(ServerPlayer bot, CombatState state) {
        if (state.isDrawingBow) {
            bot.stopUsingItem();
            state.isDrawingBow = false;
            state.bowDrawTicks = 0;
        }
    }
    
    
    private static void handleMaceCombat(ServerPlayer bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {


        if (state.isMaceDefending && state.maceDefenseCooldown > 0) {
            lookAtTarget(bot, target);
            return;
        }

        var utilsState = BotUtils.getState(bot.getName().getString());
        if (utilsState.isEating) {
            return;
        }
        
        var inventory = bot.getInventory();
        
        if (state.isDrawingBow) {
            stopUsingBow(bot, state);
        }
        

        if (!bot.isOnGround()) {
            int maceSlot = findMace(inventory);
            if (maceSlot >= 0 && maceSlot < 9) {
                org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, maceSlot);
            }
            


            double verticalSpeed = bot.getDeltaMovement().y;
            if (verticalSpeed < 0 && distance <= 5.0 && state.attackCooldown <= 0) {

                attackWithCarpet(bot, target, server);
                state.attackCooldown = 5;
            }
            return;
        }
        

        if (bot.isOnGround() && distance <= settings.getMaceRange()) {

            int windChargeSlot = findWindCharge(inventory);
            
            if (windChargeSlot >= 0) {

                BotUtils.useWindCharge(bot, server);
                bot.jump();
            } else {

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
                bot.push(dx * 0.3, 0.3, dz * 0.3);
            }
        }
        

        if (bot.isOnGround() && distance <= 3.5 && state.attackCooldown <= 0) {
            int maceSlot = findMace(inventory);
            if (maceSlot >= 0 && maceSlot < 9) {
                org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, maceSlot);
            }
            attackWithCarpet(bot, target, server);
            state.attackCooldown = settings.getAttackCooldown();
        }
        

        if (distance > settings.getMaceRange()) {
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
        }
    }
    
    
    private static void handleSpearCombat(ServerPlayer bot, Entity target, CombatState state, double distance, BotSettings settings, net.minecraft.server.MinecraftServer server) {


        if (state.isMaceDefending && state.maceDefenseCooldown > 0) {
            lookAtTarget(bot, target);
            return;
        }

        var utilsState = BotUtils.getState(bot.getName().getString());
        if (utilsState.isEating) {
            return;
        }
        
        var inventory = bot.getInventory();
        

        if (state.isDrawingBow) {
            stopUsingBow(bot, state);
        }
        

        int spearSlot = findSpear(inventory);
        if (spearSlot >= 0 && spearSlot < 9) {
            org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, spearSlot);
        }
        
        double chargeStartDistance = 10.0;
        double chargeHitDistance = 0.1;
        





        
        if (distance > chargeStartDistance) {

            if (state.isChargingSpear) {
                bot.stopUsingItem();
                state.isChargingSpear = false;
                state.spearChargeTicks = 0;
            }
            BotNavigation.moveToward(bot, target, settings.getMoveSpeed());
            
        } else if (distance > chargeHitDistance) {

            if (!state.isChargingSpear) {

                bot.setCurrentHand(InteractionInteractionHand.MAIN_HAND);
                state.isChargingSpear = true;
                state.spearChargeTicks = 0;
            }
            
            state.spearChargeTicks++;
            

            BotNavigation.moveToward(bot, target, settings.getMoveSpeed() * 1.3);
            

            if (state.spearChargeTicks > 60) {

                bot.stopUsingItem();
                state.isChargingSpear = false;
                state.spearChargeTicks = 0;
            }
            
        } else {

            if (state.isChargingSpear) {

                bot.stopUsingItem();
                state.isChargingSpear = false;
                state.spearChargeTicks = 0;

                state.attackCooldown = 0;
            }
            

            BotNavigation.moveAway(bot, target, settings.getMoveSpeed());
        }
    }

    
    private static final java.util.Random random = new java.util.Random();
    
    
    private static void attack(ServerPlayer bot, Entity target) {
        BotSettings settings = BotSettings.get();
        

        try {
            boolean cancelled = org.stepan1411.pvp_bot.api.BotAPIIntegration.fireAttackEvent(bot, target);
            if (cancelled) {
                return;
            }
        } catch (Exception e) {
            System.err.println("[PVP_BOT_API] Error firing attack event: " + e.getMessage());
        }
        

        if (random.nextInt(100) < settings.getMissChance()) {

            bot.swing(InteractionInteractionHand.MAIN_HAND);
            return;
        }
        
        bot.attack(target);
        bot.swing(InteractionInteractionHand.MAIN_HAND);
    }
    
    
    private static void attackWithCarpet(ServerPlayer bot, Entity target, net.minecraft.server.MinecraftServer server) {
        BotSettings settings = BotSettings.get();
        
        var utilsState = BotUtils.getState(bot.getName().getString());
        if (!BotUtils.canAttack(bot, utilsState)) {
            System.out.println("[COMBAT] " + bot.getName().getString() + " cannot attack - cobweb escape active");
            bot.swing(InteractionInteractionHand.MAIN_HAND);
            return;
        }

        boolean cancelled = org.stepan1411.pvp_bot.api.BotAPIIntegration.fireAttackEvent(bot, target);
        if (cancelled) {
            System.out.println("[COMBAT] " + bot.getName().getString() + " attack cancelled by API");
            bot.swing(InteractionInteractionHand.MAIN_HAND);
            return;
        }


        if (!settings.isFriendlyFireEnabled() && target instanceof Player) {
            String botName = bot.getName().getString();
            String targetName = target.getName().getString();
            if (BotFaction.areAllies(botName, targetName)) {
                System.out.println("[COMBAT] " + bot.getName().getString() + " skipping ally " + targetName);
                bot.swing(InteractionInteractionHand.MAIN_HAND);
                return;
            }
        }
        

        if (random.nextInt(100) < settings.getMissChance()) {
            try {
                server.getCommands().getDispatcher().execute(
                    "player " + bot.getName().getString() + " swinghand", 
                    server.getSharedSuggestionProvider()
                );
            } catch (Exception e) {
                bot.swing(InteractionInteractionHand.MAIN_HAND);
            }
            return;
        }
        

        if (random.nextInt(100) < settings.getMistakeChance()) {
            float yawOffset = (random.nextFloat() - 0.5f) * 60;
            bot.setYRot(bot.getYaw() + yawOffset);
        }
        

        try {
            server.getCommands().getDispatcher().execute(
                "player " + bot.getName().getString() + " attack once", 
                server.getSharedSuggestionProvider()
            );
        } catch (Exception e) {
            bot.swing(InteractionInteractionHand.MAIN_HAND);
        }
    }
    
    
    private static void startUsingShield(ServerPlayer bot, net.minecraft.server.MinecraftServer server) {
        try {
            server.getCommands().getDispatcher().execute(
                "player " + bot.getName().getString() + " use continuous", 
                server.getSharedSuggestionProvider()
            );
        } catch (Exception e) {

            bot.setCurrentHand(InteractionInteractionHand.OFF_HAND);
        }
    }
    
    
    private static void stopUsingShield(ServerPlayer bot, net.minecraft.server.MinecraftServer server) {
        try {
            server.getCommands().getDispatcher().execute(
                "player " + bot.getName().getString() + " stop", 
                server.getSharedSuggestionProvider()
            );
        } catch (Exception e) {

            bot.clearActiveItem();
        }
    }
    
    
    private static void lookAtTarget(ServerPlayer bot, Entity target) {
        Vec3 targetPos = target.getEyePosition();
        Vec3 botPos = bot.getEyePosition();
        
        double dx = targetPos.x - botPos.x;
        double dy = targetPos.y - botPos.y;
        double dz = targetPos.z - botPos.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Mth.atan2(dy, horizontalDist) * (180.0 / Math.PI));
        
        bot.setYRot(yaw);
        bot.setXRot(pitch);
        bot.setYHeadRot(yaw);
    }
    
    
    private static void lookAwayFromTarget(ServerPlayer bot, Entity target) {
        Vec3 targetPos = target.getEyePosition();
        Vec3 botPos = bot.getEyePosition();
        

        double dx = botPos.x - targetPos.x;
        double dz = botPos.z - targetPos.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        
        bot.setYRot(yaw);
        bot.setXRot(0);
        bot.setYHeadRot(yaw);
    }
    
    
    private static void moveToward(ServerPlayer bot, Entity target, double speed) {
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
        

        if (bot.isOnGround()) {
            bot.push(dx * speed * 0.1, 0, dz * speed * 0.1);
        }
    }
    
    
    private static void moveAway(ServerPlayer bot, Entity target, double speed) {
        double botX = bot.getX(), botY = bot.getY(), botZ = bot.getZ();
        double targetX = target.getX(), targetY = target.getY(), targetZ = target.getZ();
        double dx = botX - targetX;
        double dz = botZ - targetZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > 0) {
            dx /= dist;
            dz /= dist;
        }
        

        bot.setSprinting(true);
        bot.forwardSpeed = (float) speed;
        
        if (bot.isOnGround()) {
            bot.push(dx * speed * 0.1, 0, dz * speed * 0.1);
        }
    }
    

    
    private static int findMeleeWeapon(net.minecraft.world.entity.player.Inventory inventory) {
        BotSettings settings = BotSettings.get();
        boolean preferSword = settings.isPreferSword();
        
        int bestSlot = -1;
        double bestScore = 0;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
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
    
    
    private static int findAxe(net.minecraft.world.entity.player.Inventory inventory) {
        int bestSlot = -1;
        double bestDamage = 0;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
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
    
    
    private static int findShield(net.minecraft.world.entity.player.Inventory inventory) {

        if (inventory.getItem(40).getItem() == Items.SHIELD) return 40;
        

        for (int i = 0; i < 36; i++) {
            if (inventory.getItem(i).getItem() == Items.SHIELD) return i;
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
    
    
    private static double getMeleeScore(Item item, boolean preferSword) {
        double baseDamage = getMeleeDamage(item);
        if (baseDamage == 0) return 0;
        

        if (preferSword && isSword(item)) {
            return baseDamage + 5;
        }
        
        return baseDamage;
    }
    
    
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
    
    private static int findRangedWeapon(net.minecraft.world.entity.player.Inventory inventory) {

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof CrossbowItem) return i;
        }
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof BowItem) return i;
        }
        return -1;
    }
    
    private static int findMace(net.minecraft.world.entity.player.Inventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == Items.MACE) return i;
        }
        return -1;
    }
    
    private static int findWindCharge(net.minecraft.world.entity.player.Inventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == Items.WIND_CHARGE) return i;
        }
        return -1;
    }
    
    
    private static int findSpear(net.minecraft.world.entity.player.Inventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);

            String itemName = stack.getItem().toString().toLowerCase();
            if (itemName.contains("spear")) return i;
        }
        return -1;
    }
    
    
    private static int findCobweb(net.minecraft.world.entity.player.Inventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == Items.COBWEB) return i;
        }
        return -1;
    }
    
    
    private static boolean tryPlaceCobweb(ServerPlayer bot, Entity target, net.minecraft.server.MinecraftServer server) {
        CombatState state = getState(bot.getName().getString());
        

        if (state.isPlacingCobweb) return false;
        
        var inventory = bot.getInventory();
        int cobwebSlot = findCobweb(inventory);
        if (cobwebSlot < 0) return false;
        

        var world = bot.level();
        net.minecraft.core.BlockPos targetPos = target.blockPosition();
        if (world.getBlockState(targetPos).getBlock() == net.minecraft.world.level.block.Blocks.COBWEB) {
            return false;
        }
        

        if (cobwebSlot >= 9) {
            ItemStack cobweb = inventory.getItem(cobwebSlot);
            ItemStack current = inventory.getItem(0);
            inventory.setItem(cobwebSlot, current);
            inventory.setItem(0, cobweb);
            cobwebSlot = 0;
        }
        

        org.stepan1411.pvp_bot.utils.InventoryHelper.setSelectedSlot(inventory, cobwebSlot);
        

        state.isPlacingCobweb = true;
        state.cobwebPlaceTicks = 0;
        
        return true;
    }
    
    
    private static void handleCobwebPlacement(ServerPlayer bot, Entity target, CombatState state, net.minecraft.server.MinecraftServer server) {
        state.cobwebPlaceTicks++;
        

        Vec3 targetFeet = new Vec3(target.getX(), target.getY() - 0.5, target.getZ());
        Vec3 botPos = bot.getEyePosition();
        
        double dx = targetFeet.x - botPos.x;
        double dy = targetFeet.y - botPos.y;
        double dz = targetFeet.z - botPos.z;
        
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) -(Math.atan2(dy, horizontalDist) * (180.0 / Math.PI));
        
        bot.setYRot(yaw);
        bot.setXRot(pitch);
        bot.setYHeadRot(yaw);
        

        if (state.cobwebPlaceTicks % 2 == 0 && state.cobwebPlaceTicks <= 6) {
            try {
                server.getCommands().getDispatcher().execute(
                    "player " + bot.getName().getString() + " use once", 
                    server.getSharedSuggestionProvider()
                );
            } catch (Exception e) {

            }
        }
        

        if (state.cobwebPlaceTicks >= 8) {
            state.isPlacingCobweb = false;
            state.cobwebPlaceTicks = 0;
            state.cobwebCooldown = 20;
        }
    }
    
    private static boolean hasArrows(net.minecraft.world.entity.player.Inventory inventory) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof ArrowItem) return true;
        }
        return false;
    }
    

    
    
    public static void setTarget(String botName, String targetName) {
        CombatState state = getState(botName);
        state.forcedTargetName = targetName;
    }
    
    
    public static void clearTarget(String botName) {
        CombatState state = getState(botName);
        state.forcedTargetName = null;
        state.target = null;
        state.lastAttacker = null;
        state.lastAttackTime = 0;
        state.isRetreating = false;
    }
    
    
    public static void onBotDamaged(ServerPlayer bot, DamageSource source) {

        Entity attacker = source.getEntity();
        if (attacker == null) {
            attacker = source.getSource();
        }
        if (attacker == null || attacker == bot) return;
        

        if (!(attacker instanceof LivingEntity)) return;
        
        CombatState state = getState(bot.getName().getString());
        state.lastAttacker = attacker;
        state.lastAttackTime = System.currentTimeMillis();
    }
    
    
    public static Entity getTarget(String botName) {
        return getState(botName).target;
    }
}
