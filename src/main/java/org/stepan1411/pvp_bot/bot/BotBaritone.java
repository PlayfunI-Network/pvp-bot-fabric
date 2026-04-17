package org.stepan1411.pvp_bot.bot;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.stepan1411.pvp_bot.bot.pathfinding.AStarPathfinder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BotBaritone {
    
    private static final Map<String, PathState> pathStates = new HashMap<>();
    private static final Map<String, GroupPathCache> groupPathCache = new HashMap<>();
    private static final Map<String, Long> failedPathCache = new HashMap<>();
    private static final long FAILED_PATH_CACHE_TIME = 3000;
    private static final double GROUP_RADIUS = 5.0;
    
    private static class PathState {
        List<Vec3> currentPath;
        int currentIndex;
        Vec3 targetPos;
        long lastPathTime;
        PathState(List<Vec3> path, Vec3 target) {
            this.currentPath = path;
            this.currentIndex = 0;
            this.targetPos = target;
            this.lastPathTime = System.currentTimeMillis();
        }
    }
    
    private static class GroupPathCache {
        List<Vec3> path;
        Vec3 groupStartPos;
        Vec3 targetPos;
        long cacheTime;
        GroupPathCache(List<Vec3> path, Vec3 groupStartPos, Vec3 targetPos) {
            this.path = path;
            this.groupStartPos = groupStartPos;
            this.targetPos = targetPos;
            this.cacheTime = System.currentTimeMillis();
        }
        boolean isValid(Vec3 botPos, Vec3 currentTarget) {
            if (System.currentTimeMillis() - cacheTime > 5000) return false;
            if (botPos.distanceTo(groupStartPos) > GROUP_RADIUS) return false;
            if (targetPos.distanceTo(currentTarget) > 2.0) return false;
            return true;
        }
    }
    
    public static boolean isBaritoneAvailable(ServerPlayer bot) { return true; }
    
    public static boolean goToPosition(ServerPlayer bot, Vec3 targetPos) {
        String botName = bot.getName().getString();
        PathState state = pathStates.get(botName);
        Vec3 botPos = new Vec3(bot.getX(), bot.getY(), bot.getZ());
        
        boolean needNewPath = state == null ||
                              state.currentPath == null ||
                              state.currentIndex >= state.currentPath.size() ||
                              state.targetPos.distanceTo(targetPos) > 2.0 ||
                              System.currentTimeMillis() - state.lastPathTime > 5000;
        
        if (needNewPath) {
            List<Vec3> path = null;
            String failKey = generateGroupKey(botPos, targetPos);
            Long lastFailTime = failedPathCache.get(failKey);
            if (lastFailTime != null && System.currentTimeMillis() - lastFailTime < FAILED_PATH_CACHE_TIME) {
                path = new java.util.ArrayList<>();
                path.add(targetPos);
            } else {
                String groupKey = generateGroupKey(botPos, targetPos);
                GroupPathCache cachedGroup = groupPathCache.get(groupKey);
                if (cachedGroup != null && cachedGroup.isValid(botPos, targetPos)) {
                    path = cachedGroup.path;
                    System.out.println("[BotBaritone] " + botName + " using cached group path with " + path.size() + " waypoints");
                } else {
                    path = AStarPathfinder.findPath(bot, targetPos);
                    if (path == null || path.isEmpty()) {
                        failedPathCache.put(failKey, System.currentTimeMillis());
                        path = new java.util.ArrayList<>();
                        path.add(targetPos);
                        if (lastFailTime == null) {
                            System.out.println("[BotBaritone] No path found for " + botName + ", using direct path (cached for 3s)");
                        }
                    } else {
                        System.out.println("[BotBaritone] Calculated new path for " + botName + " with " + path.size() + " waypoints");
                        groupPathCache.put(groupKey, new GroupPathCache(path, botPos, targetPos));
                        if (groupPathCache.size() > 20) cleanOldGroupCache();
                    }
                }
            }
            state = new PathState(path, targetPos);
            pathStates.put(botName, state);
        }
        
        if (state.currentIndex < state.currentPath.size()) {
            Vec3 nextPoint = state.currentPath.get(state.currentIndex);
            if (BotDebug.isEnabled(botName)) {
                BotDebug.showPath(bot, targetPos, new java.util.LinkedList<>(), state.currentPath);
            }
            double horizontalDist = Math.sqrt(
                Math.pow(nextPoint.x - botPos.x, 2) +
                Math.pow(nextPoint.z - botPos.z, 2)
            );
            double verticalDist = Math.abs(nextPoint.y - botPos.y);
            if (horizontalDist < 0.5 && verticalDist < 1.0) {
                state.currentIndex++;
                if (state.currentIndex >= state.currentPath.size()) return true;
                nextPoint = state.currentPath.get(state.currentIndex);
            }
            BotNavigation.NavigationState navState = BotNavigation.getState(bot.getName().getString());
            if (navState.jumpCooldown > 0) navState.jumpCooldown--;
            if (navState.avoidTicks > 0) navState.avoidTicks--;
            double dx = nextPoint.x - botPos.x;
            double dz = nextPoint.z - botPos.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > 0.1) {
                double dy = nextPoint.y - botPos.y;
                dx /= dist; dz /= dist;
                double yaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
                float yawF = (float) yaw;
                float currentYaw = bot.getYRot();
                float yawDiff = yawF - currentYaw;
                while (yawDiff > 180) yawDiff -= 360;
                while (yawDiff < -180) yawDiff += 360;
                if (Math.abs(yawDiff) > 30) yawF = currentYaw + Math.signum(yawDiff) * 30;
                bot.setYRot(yawF);
                bot.setYHeadRot(yawF);
                if (HerobotMovement.isHerobotAvailable()) {
                    MinecraftServer server = bot.createCommandSourceStack().getServer();
                    HerobotMovement.executeCommand(server, String.format("player %s move forward", botName));
                    if (dist > 3.0) HerobotMovement.executeCommand(server, String.format("player %s sprint", botName));
                    BotSettings settings = BotSettings.get();
                    if (bot.onGround() && navState.jumpCooldown <= 0) {
                        if (dy > 0.5) {
                            HerobotMovement.executeCommand(server, String.format("player %s jump", botName));
                            navState.jumpCooldown = 10;
                        } else if (settings.isBhopEnabled() && dist > 2.0) {
                            HerobotMovement.executeCommand(server, String.format("player %s jump", botName));
                            navState.jumpCooldown = settings.getBhopCooldown();
                        }
                    }
                } else {
                    double speed = 0.1;
                    double currentVelX = bot.getDeltaMovement().x;
                    double currentVelZ = bot.getDeltaMovement().z;
                    double targetVelX = dx * speed;
                    double targetVelZ = dz * speed;
                    double newVelX = currentVelX * 0.8 + targetVelX * 0.2;
                    double newVelZ = currentVelZ * 0.8 + targetVelZ * 0.2;
                    bot.setDeltaMovement(newVelX, bot.getDeltaMovement().y, newVelZ);
                    bot.setSprinting(true);
                    BotSettings settings = BotSettings.get();
                    if (bot.onGround() && navState.jumpCooldown <= 0) {
                        if (dy > 0.5) { bot.jump(); navState.jumpCooldown = 10; }
                        else if (settings.isBhopEnabled() && dist > 2.0) { bot.jump(); navState.jumpCooldown = settings.getBhopCooldown(); }
                    }
                }
            }
            return true;
        }
        return false;
    }
    
    public static boolean goToEntity(ServerPlayer bot, Entity target, double distance) {
        Vec3 targetPos = new Vec3(target.getX(), target.getY(), target.getZ());
        return goToPosition(bot, targetPos);
    }
    
    public static boolean moveAwayFrom(ServerPlayer bot, Entity target, double minDistance) { return false; }
    
    public static void stop(ServerPlayer bot) { pathStates.remove(bot.getName().getString()); }
    
    public static boolean isPathing(ServerPlayer bot) {
        PathState state = pathStates.get(bot.getName().getString());
        return state != null && state.currentPath != null && state.currentIndex < state.currentPath.size();
    }
    
    public static double getDistanceToGoal(ServerPlayer bot) {
        PathState state = pathStates.get(bot.getName().getString());
        if (state == null || state.targetPos == null) return -1;
        Vec3 botPos = new Vec3(bot.getX(), bot.getY(), bot.getZ());
        return botPos.distanceTo(state.targetPos);
    }
    
    public static void removeBaritone(String botName) { pathStates.remove(botName); }
    
    private static String generateGroupKey(Vec3 botPos, Vec3 targetPos) {
        int gridX = (int) Math.floor(botPos.x / GROUP_RADIUS);
        int gridY = (int) Math.floor(botPos.y / GROUP_RADIUS);
        int gridZ = (int) Math.floor(botPos.z / GROUP_RADIUS);
        int targetX = (int) Math.floor(targetPos.x);
        int targetY = (int) Math.floor(targetPos.y);
        int targetZ = (int) Math.floor(targetPos.z);
        return String.format("%d_%d_%d_to_%d_%d_%d", gridX, gridY, gridZ, targetX, targetY, targetZ);
    }
    
    private static void cleanOldGroupCache() {
        long currentTime = System.currentTimeMillis();
        groupPathCache.entrySet().removeIf(entry -> currentTime - entry.getValue().cacheTime > 5000);
        failedPathCache.entrySet().removeIf(entry -> currentTime - entry.getValue() > FAILED_PATH_CACHE_TIME);
    }
}
