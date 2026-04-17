package org.stepan1411.pvp_bot.bot.pathfinding;

import net.minecraft.core.BlockPos;


public class PathNode implements Comparable<PathNode> {
    public final BlockPos pos;
    public final long hash;
    
    public PathNode parent;
    public double cost;
    public double estimatedCost;
    public double combinedCost;
    
    public PathNode(BlockPos pos, PathNode parent, double cost, double estimatedCost) {
        this.pos = pos;
        this.hash = pos.asLong();
        this.parent = parent;
        this.cost = cost;
        this.estimatedCost = estimatedCost;
        this.combinedCost = cost + estimatedCost;
    }
    
    @Override
    public int compareTo(PathNode other) {
        return Double.compare(this.combinedCost, other.combinedCost);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PathNode)) return false;
        return this.hash == ((PathNode) obj).hash;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(hash);
    }
}
