package org.stepan1411.pvp_bot.bot;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public class BotTicker {

    private static int tickCounter = 0;
    private static int autoSaveCounter = 0;
    private static final int AUTO_SAVE_INTERVAL = 1200; // Автосохранение каждые 60 секунд (1200 тиков)

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BotTicker::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        tickCounter++;
        autoSaveCounter++;
        
        int interval = BotSettings.get().getCheckInterval();
        
        // Автосохранение данных ботов каждые 60 секунд
        if (autoSaveCounter >= AUTO_SAVE_INTERVAL) {
            BotManager.updateBotData(server);
            BotManager.saveBots();
            autoSaveCounter = 0;
        }
        
        // Очищаем мёртвых ботов каждые 20 тиков (1 секунда)
        if (tickCounter % 20 == 0) {
            BotManager.cleanupDeadBots(server);
            // УБРАЛ автоматическую синхронизацию - теперь только по команде /pvpbot sync
        }
        
        // Визуализация путей
        BotPathVisualizer.update(server);
        
        for (String botName : BotManager.getAllBots()) {
            ServerPlayerEntity bot = BotManager.getBot(server, botName);
            if (bot != null && bot.isAlive()) {
                // Утилиты (тотем, еда, щит, плавание) - каждый тик
                BotUtils.update(bot, server);
                
                // Боевая система - каждый тик (но не если следует по пути с attack=false)
                boolean isFollowingWithoutAttack = BotPath.isFollowing(botName) && !BotPath.shouldAttack(botName);
                if (!isFollowingWithoutAttack) {
                    BotCombat.update(bot, server);
                }
                
                // Следование по пути - каждый тик
                if (BotPath.isFollowing(botName)) {
                    boolean shouldAttack = BotPath.shouldAttack(botName);
                    var target = BotCombat.getTarget(botName);
                    boolean hasTarget = target != null && target.isAlive();
                    
                    // Если должны атаковать и есть цель
                    if (shouldAttack && hasTarget) {
                        Vec3d nextPoint = BotPath.getNextPoint(botName);
                        
                        // Если не в бою - начинаем бой и запоминаем точку
                        if (!BotPath.isInCombat(botName)) {
                            BotPath.startCombat(botName, nextPoint);
                        }
                        
                        // В бою - боевая система управляет движением
                        // BotCombat.update() уже вызван выше
                        
                    } else {
                        // Нет цели или не должны атаковать
                        
                        // Если были в бою - заканчиваем бой
                        if (BotPath.isInCombat(botName)) {
                            BotPath.endCombat(botName);
                        }
                        
                        // Проверяем нужно ли вернуться к точке после боя
                        Vec3d pausedPoint = BotPath.getPausedPoint(botName);
                        if (pausedPoint != null) {
                            Vec3d botPos = new Vec3d(bot.getX(), bot.getY(), bot.getZ());
                            double distanceToPaused = botPos.distanceTo(pausedPoint);
                            
                            // Если достигли точки где остановились - продолжаем путь
                            if (distanceToPaused < 1.5) {
                                BotPath.clearPausedPoint(botName);
                            } else {
                                // Возвращаемся к точке
                                BotNavigation.lookAtPosition(bot, pausedPoint);
                                BotNavigation.moveTowardPosition(bot, pausedPoint, 1.0);
                            }
                        } else {
                            // Обычное следование по пути
                            Vec3d nextPoint = BotPath.getNextPoint(botName);
                            if (nextPoint != null) {
                                Vec3d botPos = new Vec3d(bot.getX(), bot.getY(), bot.getZ());
                                double distance = botPos.distanceTo(nextPoint);
                                
                                // Если достигли точки - переходим к следующей
                                if (distance < 1.5) {
                                    BotPath.advanceToNextPoint(botName);
                                } else {
                                    // Смотрим на точку и двигаемся к ней
                                    BotNavigation.lookAtPosition(bot, nextPoint);
                                    BotNavigation.moveTowardPosition(bot, nextPoint, 1.0);
                                }
                            }
                        }
                    }
                }
                
                // Экипировка - по интервалу (не во время еды!)
                if (tickCounter >= interval) {
                    var utilsState = BotUtils.getState(botName);
                    if (!utilsState.isEating) {
                        BotEquipment.autoEquip(bot);
                    }
                }
            }
        }
        
        if (tickCounter >= interval) {
            tickCounter = 0;
        }
    }
}
