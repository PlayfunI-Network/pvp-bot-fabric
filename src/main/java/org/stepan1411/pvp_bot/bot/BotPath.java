package org.stepan1411.pvp_bot.bot;

import net.minecraft.util.math.Vec3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BotPath {
    
    private static final Map<String, PathData> paths = new HashMap<>();
    private static final Map<String, PathFollower> followers = new HashMap<>();
    
    public static class PathData {
        public String name;
        public List<Vec3d> points = new ArrayList<>();
        public boolean loop = true; // Зациклить путь
        
        public PathData(String name) {
            this.name = name;
        }
    }
    
    public static class PathFollower {
        public String pathName;
        public int currentPoint = 0;
        public boolean reverse = false; // Идём в обратном направлении
        
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
}
