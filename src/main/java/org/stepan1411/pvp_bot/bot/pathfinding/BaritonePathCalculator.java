package org.stepan1411.pvp_bot.bot.pathfinding;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;


public class BaritonePathCalculator {
    
    private static boolean baritoneAvailable = false;
    
    static {
        try {

            Class.forName("baritone.api.BaritoneAPI");
            baritoneAvailable = true;
        } catch (ClassNotFoundException e) {
            baritoneAvailable = false;
        }
    }
    
    
    public static List<Vec3> calculatePath(ServerPlayer bot, Vec3 targetPos) {



        return AStarPathfinder.findPath(bot, targetPos);
    }
    
    
    public static boolean isBaritoneAvailable() {
        return baritoneAvailable;
    }
    
    
    private static List<Vec3> tryBaritonePathfinding(ServerPlayer bot, Vec3 targetPos) {





        

        return null;
    }
}
