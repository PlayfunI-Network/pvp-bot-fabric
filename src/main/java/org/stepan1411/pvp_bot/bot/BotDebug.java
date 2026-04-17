package org.stepan1411.pvp_bot.bot;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;


public class BotDebug {
    
    private static final Map<String, DebugSettings> debugSettings = new HashMap<>();
    
    public static class DebugSettings {
        public boolean pathVisualization = false;
        public boolean targetVisualization = false;
        public boolean combatInfo = false;
        public boolean navigationInfo = false;
        public int pathTickCounter = 0;
        public int targetTickCounter = 0;
        public int navigationTickCounter = 0;
        public boolean isAnyEnabled() {
            return pathVisualization || targetVisualization || combatInfo || navigationInfo;
        }
    }
    
    public static DebugSettings getSettings(String botName) {
        return debugSettings.computeIfAbsent(botName, k -> new DebugSettings());
    }
    public static void setPathVisualization(String botName, boolean enabled) { getSettings(botName).pathVisualization = enabled; }
    public static void setTargetVisualization(String botName, boolean enabled) { getSettings(botName).targetVisualization = enabled; }
    public static void setCombatInfo(String botName, boolean enabled) { getSettings(botName).combatInfo = enabled; }
    public static void setNavigationInfo(String botName, boolean enabled) { getSettings(botName).navigationInfo = enabled; }
    public static void enableAll(String botName) {
        DebugSettings settings = getSettings(botName);
        settings.pathVisualization = true; settings.targetVisualization = true;
        settings.combatInfo = true; settings.navigationInfo = true;
    }
    public static void disableAll(String botName) { debugSettings.remove(botName); }
    public static boolean isEnabled(String botName) {
        DebugSettings settings = debugSettings.get(botName);
        return settings != null && settings.isAnyEnabled();
    }
    
    public static void showPath(ServerPlayer bot, Vec3 targetPos, java.util.LinkedList<Vec3> pathHistory) {
        showPath(bot, targetPos, pathHistory, null);
    }
    
    public static void showPath(ServerPlayer bot, Vec3 targetPos, java.util.LinkedList<Vec3> pathHistory, List<Vec3> fullPath) {
        DebugSettings settings = getSettings(bot.getName().getString());
        if (!settings.pathVisualization) return;
        settings.pathTickCounter++;
        if (settings.pathTickCounter < 5) return;
        settings.pathTickCounter = 0;
        ServerLevel world = (ServerLevel) bot.level();
        DustParticleOptions greenDust = new DustParticleOptions(new org.joml.Vector3f(0f, 1f, 0f), 1.0f);
        DustParticleOptions grayDust = new DustParticleOptions(new org.joml.Vector3f(0.502f, 0.502f, 0.502f), 0.7f);
        DustParticleOptions blueDust = new DustParticleOptions(new org.joml.Vector3f(0f, 0.502f, 1f), 1.0f);
        if (fullPath != null && !fullPath.isEmpty()) {
            for (Vec3 waypoint : fullPath) {
                world.sendParticles(null, blueDust, false, waypoint.x, waypoint.y + 0.5, waypoint.z, 3, 0.2, 0.2, 0.2, 0);
            }
            for (int i = 0; i < fullPath.size() - 1; i++) {
                Vec3 pos1 = fullPath.get(i); Vec3 pos2 = fullPath.get(i + 1);
                drawLine(world, blueDust, pos1.x, pos1.y + 0.3, pos1.z, pos2.x, pos2.y + 0.3, pos2.z, 0.3);
            }
            return;
        }
        List<Vec3> allPositions = new ArrayList<>(pathHistory);
        Vec3 currentPos = new Vec3(bot.getX(), bot.getY(), bot.getZ());
        if (allPositions.isEmpty() || currentPos.distanceTo(allPositions.get(allPositions.size() - 1)) > 0.3) {
            allPositions.add(currentPos);
        }
        allPositions.add(targetPos);
        if (allPositions.isEmpty()) return;
        Set<BlockPos> uniqueBlocks = new LinkedHashSet<>();
        for (Vec3 pos : allPositions) {
            uniqueBlocks.add(new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z)));
        }
        for (BlockPos blockPos : uniqueBlocks) {
            int bx = blockPos.getX(), by = blockPos.getY(), bz = blockPos.getZ();
            world.sendParticles(null, greenDust, false, bx, by + 1, bz, 1, 0, 0, 0, 0);
            world.sendParticles(null, greenDust, false, bx + 1, by + 1, bz + 1, 1, 0, 0, 0, 0);
        }
        if (allPositions.size() > 1) {
            for (int i = 0; i < allPositions.size() - 1; i++) {
                Vec3 pos1 = allPositions.get(i); Vec3 pos2 = allPositions.get(i + 1);
                drawLine(world, grayDust, pos1.x, pos1.y + 0.1, pos1.z, pos2.x, pos2.y + 0.1, pos2.z, 0.3);
            }
        }
    }
    
    public static void showTargetBlock(ServerPlayer bot, Vec3 targetPos) {
        DebugSettings settings = getSettings(bot.getName().getString());
        if (!settings.navigationInfo) return;
        settings.navigationTickCounter++;
        if (settings.navigationTickCounter < 5) return;
        settings.navigationTickCounter = 0;
        ServerLevel world = (ServerLevel) bot.level();
        int blockX = (int) Math.floor(targetPos.x);
        int blockY = (int) Math.floor(targetPos.y);
        int blockZ = (int) Math.floor(targetPos.z);
        DustParticleOptions redDust = new DustParticleOptions(new org.joml.Vector3f(1f, 0f, 0f), 1.0f);
        double step = 0.2;
        drawLine(world, redDust, blockX, blockY, blockZ, blockX + 1, blockY, blockZ, step);
        drawLine(world, redDust, blockX, blockY, blockZ + 1, blockX + 1, blockY, blockZ + 1, step);
        drawLine(world, redDust, blockX, blockY, blockZ, blockX, blockY, blockZ + 1, step);
        drawLine(world, redDust, blockX + 1, blockY, blockZ, blockX + 1, blockY, blockZ + 1, step);
        drawLine(world, redDust, blockX, blockY + 1, blockZ, blockX + 1, blockY + 1, blockZ, step);
        drawLine(world, redDust, blockX, blockY + 1, blockZ + 1, blockX + 1, blockY + 1, blockZ + 1, step);
        drawLine(world, redDust, blockX, blockY + 1, blockZ, blockX, blockY + 1, blockZ + 1, step);
        drawLine(world, redDust, blockX + 1, blockY + 1, blockZ, blockX + 1, blockY + 1, blockZ + 1, step);
        drawLine(world, redDust, blockX, blockY, blockZ, blockX, blockY + 1, blockZ, step);
        drawLine(world, redDust, blockX + 1, blockY, blockZ, blockX + 1, blockY + 1, blockZ, step);
        drawLine(world, redDust, blockX, blockY, blockZ + 1, blockX, blockY + 1, blockZ + 1, step);
        drawLine(world, redDust, blockX + 1, blockY, blockZ + 1, blockX + 1, blockY + 1, blockZ + 1, step);
    }
    
    public static void showTargetEntity(ServerPlayer bot, net.minecraft.world.entity.Entity target) {
        DebugSettings settings = getSettings(bot.getName().getString());
        if (!settings.targetVisualization) return;
        settings.targetTickCounter++;
        if (settings.targetTickCounter < 3) return;
        settings.targetTickCounter = 0;
        ServerLevel world = (ServerLevel) bot.level();
        var box = target.getBoundingBox();
        DustParticleOptions purpleDust = new DustParticleOptions(new org.joml.Vector3f(1f, 0f, 1f), 1.0f);
        double step = 0.2;
        drawLine(world, purpleDust, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, step);
        drawLine(world, purpleDust, box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, step);
        drawLine(world, purpleDust, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, step);
        drawLine(world, purpleDust, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, step);
        drawLine(world, purpleDust, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, step);
        drawLine(world, purpleDust, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, step);
        drawLine(world, purpleDust, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, step);
        drawLine(world, purpleDust, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, step);
        drawLine(world, purpleDust, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, step);
        drawLine(world, purpleDust, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, step);
        drawLine(world, purpleDust, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, step);
        drawLine(world, purpleDust, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, step);
    }
    
    private static void drawLine(ServerLevel world, DustParticleOptions dust, double x1, double y1, double z1, double x2, double y2, double z2, double step) {
        double dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) Math.ceil(length / step);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            world.sendParticles(null, dust, false, x1 + dx * t, y1 + dy * t, z1 + dz * t, 1, 0, 0, 0, 0);
        }
    }
    
    public static void clearAll() { debugSettings.clear(); }
}
