package org.stepan1411.pvp_bot.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.Vec3d;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class BotPath {
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    
    private static final Map<String, PathData> paths = new HashMap<>();
    private static final Map<String, PathFollower> followers = new HashMap<>();
    private static final Set<String> visiblePaths = new HashSet<>();
    
    public static class PathData {
        public String name;
        public List<Vec3d> points = new ArrayList<>();
        public boolean loop = false; // Зациклить путь (по умолчанию false - идти по кругу)
        public boolean attack = true; // Атаковать врагов (по умолчанию true)
        
        public PathData(String name) {
            this.name = name;
        }
    }
    
    public static class PathFollower {
        public String pathName;
        public int currentPoint = 0;
        public boolean reverse = false; // Идём в обратном направлении
        public Vec3d pausedAtPoint = null; // Точка где остановились для боя
        public boolean inCombat = false; // В бою
        
        public PathFollower(String pathName) {
            this.pathName = pathName;
        }
    }
    
    /**
     * Создать новый путь
     */
    public static boolean createPath(String name) {
        if (paths.containsKey(name)) {
            return false;
        }
        paths.put(name, new PathData(name));
        save();
        return true;
    }
    
    /**
     * Удалить путь
     */
    public static boolean deletePath(String name) {
        if (!paths.containsKey(name)) {
            return false;
        }
        paths.remove(name);
        // Останавливаем всех ботов на этом пути
        followers.entrySet().removeIf(entry -> entry.getValue().pathName.equals(name));
        save();
        return true;
    }
    
    /**
     * Добавить точку в путь
     */
    public static boolean addPoint(String pathName, Vec3d point) {
        PathData path = paths.get(pathName);
        if (path == null) {
            return false;
        }
        path.points.add(point);
        save();
        return true;
    }
    
    /**
     * Удалить последнюю точку из пути
     */
    public static boolean removeLastPoint(String pathName) {
        PathData path = paths.get(pathName);
        if (path == null || path.points.isEmpty()) {
            return false;
        }
        path.points.remove(path.points.size() - 1);
        save();
        return true;
    }
    
    /**
     * Удалить точку по индексу
     */
    public static boolean removePoint(String pathName, int index) {
        PathData path = paths.get(pathName);
        if (path == null || index < 0 || index >= path.points.size()) {
            return false;
        }
        path.points.remove(index);
        save();
        return true;
    }
    
    /**
     * Очистить все точки пути
     */
    public static boolean clearPath(String pathName) {
        PathData path = paths.get(pathName);
        if (path == null) {
            return false;
        }
        path.points.clear();
        save();
        return true;
    }
    
    /**
     * Установить зацикливание пути
     */
    public static boolean setLoop(String pathName, boolean loop) {
        PathData path = paths.get(pathName);
        if (path == null) {
            return false;
        }
        path.loop = loop;
        save();
        return true;
    }
    
    /**
     * Установить атаку врагов во время следования по пути
     */
    public static boolean setAttack(String pathName, boolean attack) {
        PathData path = paths.get(pathName);
        if (path == null) {
            return false;
        }
        path.attack = attack;
        save();
        return true;
    }
    
    /**
     * Начать следование бота по пути
     */
    public static boolean startFollowing(String botName, String pathName) {
        PathData path = paths.get(pathName);
        if (path == null || path.points.isEmpty()) {
            return false;
        }
        followers.put(botName, new PathFollower(pathName));
        return true;
    }
    
    /**
     * Остановить следование бота по пути
     */
    public static boolean stopFollowing(String botName) {
        return followers.remove(botName) != null;
    }
    
    /**
     * Получить следующую точку для бота
     */
    public static Vec3d getNextPoint(String botName) {
        PathFollower follower = followers.get(botName);
        if (follower == null) {
            return null;
        }
        
        PathData path = paths.get(follower.pathName);
        if (path == null || path.points.isEmpty()) {
            return null;
        }
        
        return path.points.get(follower.currentPoint);
    }
    
    /**
     * Перейти к следующей точке пути
     */
    public static void advanceToNextPoint(String botName) {
        PathFollower follower = followers.get(botName);
        if (follower == null) {
            return;
        }
        
        PathData path = paths.get(follower.pathName);
        if (path == null || path.points.isEmpty()) {
            return;
        }
        
        if (path.loop) {
            // Зацикленный путь
            if (follower.reverse) {
                follower.currentPoint--;
                if (follower.currentPoint < 0) {
                    follower.currentPoint = 1;
                    follower.reverse = false;
                }
            } else {
                follower.currentPoint++;
                if (follower.currentPoint >= path.points.size()) {
                    follower.currentPoint = path.points.size() - 2;
                    follower.reverse = true;
                }
            }
        } else {
            // Незацикленный путь - просто по кругу
            follower.currentPoint = (follower.currentPoint + 1) % path.points.size();
        }
    }
    
    /**
     * Проверить следует ли бот по пути
     */
    public static boolean isFollowing(String botName) {
        return followers.containsKey(botName);
    }
    
    /**
     * Получить путь по имени
     */
    public static PathData getPath(String name) {
        return paths.get(name);
    }
    
    /**
     * Получить все пути
     */
    public static Map<String, PathData> getAllPaths() {
        return paths;
    }
    
    /**
     * Получить информацию о следовании бота
     */
    public static PathFollower getFollower(String botName) {
        return followers.get(botName);
    }
    
    /**
     * Включить/выключить визуализацию пути
     */
    public static boolean setPathVisible(String pathName, boolean visible) {
        PathData path = paths.get(pathName);
        if (path == null) {
            return false;
        }
        
        if (visible) {
            visiblePaths.add(pathName);
        } else {
            visiblePaths.remove(pathName);
        }
        return true;
    }
    
    /**
     * Проверить видим ли путь
     */
    public static boolean isPathVisible(String pathName) {
        return visiblePaths.contains(pathName);
    }
    
    /**
     * Получить все видимые пути
     */
    public static Set<String> getVisiblePaths() {
        return visiblePaths;
    }
    
    /**
     * Начать бой - остановить следование и запомнить точку
     */
    public static void startCombat(String botName, Vec3d currentTarget) {
        PathFollower follower = followers.get(botName);
        if (follower != null && !follower.inCombat) {
            follower.inCombat = true;
            follower.pausedAtPoint = currentTarget;
        }
    }
    
    /**
     * Закончить бой - вернуться к следованию по пути
     */
    public static void endCombat(String botName) {
        PathFollower follower = followers.get(botName);
        if (follower != null) {
            follower.inCombat = false;
            // pausedAtPoint остаётся для возврата к точке
        }
    }
    
    /**
     * Проверить в бою ли бот
     */
    public static boolean isInCombat(String botName) {
        PathFollower follower = followers.get(botName);
        return follower != null && follower.inCombat;
    }
    
    /**
     * Получить точку где остановились для боя
     */
    public static Vec3d getPausedPoint(String botName) {
        PathFollower follower = followers.get(botName);
        return follower != null ? follower.pausedAtPoint : null;
    }
    
    /**
     * Очистить точку паузы (достигли её)
     */
    public static void clearPausedPoint(String botName) {
        PathFollower follower = followers.get(botName);
        if (follower != null) {
            follower.pausedAtPoint = null;
        }
    }
    
    /**
     * Проверить должен ли бот атаковать во время следования по пути
     */
    public static boolean shouldAttack(String botName) {
        PathFollower follower = followers.get(botName);
        if (follower == null) {
            return true; // По умолчанию атакуем
        }
        
        PathData path = paths.get(follower.pathName);
        return path != null && path.attack;
    }
    
    /**
     * Инициализация - создание папки и загрузка путей
     */
    public static void init() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("pvpbot");
        
        try {
            Files.createDirectories(configDir);
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Failed to create config directory: " + e.getMessage());
        }
        
        configPath = org.stepan1411.pvp_bot.config.WorldConfigHelper.getWorldConfigDir().resolve("paths.json");
        load();
    }
    
    /**
     * Сохранить пути в файл
     */
    public static void save() {
        if (configPath == null) return;
        
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(paths, writer);
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Failed to save paths: " + e.getMessage());
        }
    }
    
    /**
     * Загрузить пути из файла
     */
    private static void load() {
        if (configPath == null || !Files.exists(configPath)) {
            return;
        }
        
        try (Reader reader = Files.newBufferedReader(configPath)) {
            Map<String, PathData> loadedPaths = GSON.fromJson(reader, 
                new TypeToken<Map<String, PathData>>(){}.getType());
            
            if (loadedPaths != null) {
                paths.clear();
                paths.putAll(loadedPaths);
                System.out.println("[PVP_BOT] Loaded " + paths.size() + " paths");
            }
        } catch (Exception e) {
            System.err.println("[PVP_BOT] Failed to load paths: " + e.getMessage());
        }
    }
}
