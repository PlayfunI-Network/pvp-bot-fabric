package org.stepan1411.pvp_bot.bot.pathfinding;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;

import java.util.*;


public class AStarPathfinder {
    
    private static final int MAX_ITERATIONS = 15000;
    private static final int MAX_PATH_LENGTH = 1000;
    
    
    public static List<Vec3> findPath(ServerPlayer bot, Vec3 targetPos) {
        Level world = bot.level();
        BlockPos start = bot.blockPosition();
        BlockPos goal = BlockPos.containing(targetPos);
        

        if (!isValidPosition(world, goal)) {
            goal = findNearestValid(world, goal, 5);
            if (goal == null) {
                return null;
            }
        }
        
        return calculatePath(world, start, goal);
    }
    
    
    private static List<Vec3> calculatePath(Level world, BlockPos start, BlockPos goal) {
        MovementCalculator movementCalc = new MovementCalculator(world);
        

        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<Long, PathNode> allNodes = new HashMap<>();
        Set<Long> closedSet = new HashSet<>();
        

        PathNode startNode = new PathNode(start, null, 0, heuristic(start, goal));
        openSet.add(startNode);
        allNodes.put(startNode.hash, startNode);
        
        int iterations = 0;
        PathNode bestNode = startNode;
        
        while (!openSet.isEmpty() && iterations < MAX_ITERATIONS) {
            iterations++;
            
            PathNode current = openSet.poll();
            

            if (current.estimatedCost < bestNode.estimatedCost) {
                bestNode = current;
            }
            

            if (isInGoal(current.pos, goal)) {
                return reconstructPath(current);
            }
            
            closedSet.add(current.hash);
            

            List<MovementCalculator.PossibleMovement> movements = movementCalc.getMovements(current.pos);
            
            for (MovementCalculator.PossibleMovement movement : movements) {
                BlockPos neighborPos = movement.destination;
                long neighborHash = neighborPos.asLong();
                
                if (closedSet.contains(neighborHash)) {
                    continue;
                }
                
                double tentativeCost = current.cost + movement.cost;
                PathNode neighborNode = allNodes.get(neighborHash);
                
                if (neighborNode == null) {

                    neighborNode = new PathNode(neighborPos, current, tentativeCost, heuristic(neighborPos, goal));
                    allNodes.put(neighborHash, neighborNode);
                    openSet.add(neighborNode);
                } else if (tentativeCost < neighborNode.cost) {

                    openSet.remove(neighborNode);
                    neighborNode.parent = current;
                    neighborNode.cost = tentativeCost;
                    neighborNode.combinedCost = tentativeCost + neighborNode.estimatedCost;
                    openSet.add(neighborNode);
                }
            }
        }
        

        if (bestNode != startNode && bestNode.pos.isWithinDistance(goal, 10.0)) {
            return reconstructPath(bestNode);
        }
        
        return null;
    }
    
    
    private static boolean isInGoal(BlockPos pos, BlockPos goal) {
        return pos.isWithinDistance(goal, 2.0);
    }
    
    
    private static double heuristic(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        int dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) * MovementCalculator.WALK_ONE_BLOCK_COST;
    }
    
    
    private static List<Vec3> reconstructPath(PathNode goal) {
        List<Vec3> path = new ArrayList<>();
        PathNode current = goal;
        
        while (current != null && path.size() < MAX_PATH_LENGTH) {
            path.add(new Vec3(
                current.pos.getX() + 0.5,
                current.pos.getY() + 0.1,
                current.pos.getZ() + 0.5
            ));
            current = current.parent;
        }
        
        Collections.reverse(path);
        return simplifyPath(path);
    }
    
    
    private static List<Vec3> simplifyPath(List<Vec3> path) {
        if (path.size() <= 2) {
            return path;
        }
        
        List<Vec3> simplified = new ArrayList<>();
        simplified.add(path.get(0));
        
        for (int i = 1; i < path.size() - 1; i++) {
            Vec3 prev = path.get(i - 1);
            Vec3 current = path.get(i);
            Vec3 next = path.get(i + 1);
            

            Vec3 dir1 = current.subtract(prev).normalize();
            Vec3 dir2 = next.subtract(current).normalize();
            double dot = dir1.dotProduct(dir2);
            
            if (dot < 0.95) {
                simplified.add(current);
            }
        }
        
        simplified.add(path.get(path.size() - 1));
        return simplified;
    }
    
    
    private static boolean isValidPosition(Level world, BlockPos pos) {
        return world.getBlockState(pos).isAir() || world.getBlockState(pos).isLiquid();
    }
    
    
    private static BlockPos findNearestValid(Level world, BlockPos target, int maxRadius) {
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = -2; dy <= 2; dy++) {
                        BlockPos candidate = target.add(dx, dy, dz);
                        if (isValidPosition(world, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }
}
