package org.stepan1411.pvp_bot.bot.pathfinding;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;


public class MovementCalculator {
    

    public static final double WALK_ONE_BLOCK_COST = 20.0 / 4.317;
    public static final double WALK_ONE_IN_WATER_COST = 20.0 / 2.2;
    public static final double JUMP_ONE_BLOCK_COST = 20.0 / 4.0;
    public static final double FALL_N_BLOCKS_COST = 20.0 / 10.0;
    public static final double DIAGONAL_COST = WALK_ONE_BLOCK_COST * 1.414;
    public static final double CLIMB_COST = 20.0 / 2.35;
    
    private final Level world;
    
    public MovementCalculator(Level world) {
        this.world = world;
    }
    
    
    public List<PossibleMovement> getMovements(BlockPos from) {
        List<PossibleMovement> movements = new ArrayList<>();
        

        addTraverseMovement(movements, from, 1, 0);
        addTraverseMovement(movements, from, -1, 0);
        addTraverseMovement(movements, from, 0, 1);
        addTraverseMovement(movements, from, 0, -1);
        

        addDiagonalMovement(movements, from, 1, 1);
        addDiagonalMovement(movements, from, 1, -1);
        addDiagonalMovement(movements, from, -1, 1);
        addDiagonalMovement(movements, from, -1, -1);
        

        addPillarMovement(movements, from);
        addFallMovement(movements, from);
        

        if (isClimbable(from) || isClimbable(from.above())) {
            addClimbMovement(movements, from);
        }
        
        return movements;
    }
    
    
    private void addTraverseMovement(List<PossibleMovement> movements, BlockPos from, int dx, int dz) {
        BlockPos to = from.add(dx, 0, dz);
        

        if (isStairs(to)) {

            if (canPassThrough(to.above()) && canPassThrough(to.above(2))) {
                movements.add(new PossibleMovement(to, WALK_ONE_BLOCK_COST * 1.2, MovementType.ASCEND));
            }
            return;
        }
        

        if (canWalkOn(to)) {
            movements.add(new PossibleMovement(to, WALK_ONE_BLOCK_COST, MovementType.TRAVERSE));
            return;
        }
        

        BlockPos ascend = to.above();
        if (canWalkOn(ascend) && canPassThrough(from.above()) && canPassThrough(from.above(2))) {
            movements.add(new PossibleMovement(ascend, JUMP_ONE_BLOCK_COST, MovementType.ASCEND));
            return;
        }
        

        BlockPos descend = to.below();
        if (canWalkOn(descend)) {
            movements.add(new PossibleMovement(descend, WALK_ONE_BLOCK_COST * 1.2, MovementType.DESCEND));
        }
    }
    
    
    private void addDiagonalMovement(List<PossibleMovement> movements, BlockPos from, int dx, int dz) {
        BlockPos to = from.add(dx, 0, dz);
        

        BlockPos side1 = from.add(dx, 0, 0);
        BlockPos side2 = from.add(0, 0, dz);
        
        if (!canPassThrough(side1) && !canPassThrough(side2)) {
            return;
        }
        
        if (canWalkOn(to)) {
            movements.add(new PossibleMovement(to, DIAGONAL_COST, MovementType.DIAGONAL));
        }
    }
    
    
    private void addPillarMovement(List<PossibleMovement> movements, BlockPos from) {
        BlockPos up = from.above();
        if (canWalkOn(up) && canPassThrough(from.above()) && canPassThrough(from.above(2))) {
            movements.add(new PossibleMovement(up, JUMP_ONE_BLOCK_COST * 1.5, MovementType.PILLAR));
        }
    }
    
    
    private void addFallMovement(List<PossibleMovement> movements, BlockPos from) {

        for (int fallDist = 1; fallDist <= 4; fallDist++) {
            BlockPos down = from.below(fallDist);
            

            boolean pathClear = true;
            for (int i = 1; i < fallDist; i++) {
                if (!canPassThrough(from.below(i))) {
                    pathClear = false;
                    break;
                }
            }
            
            if (pathClear && canWalkOn(down)) {
                double cost = FALL_N_BLOCKS_COST * fallDist;
                movements.add(new PossibleMovement(down, cost, MovementType.FALL));
                break;
            }
        }
    }
    
    
    private void addClimbMovement(List<PossibleMovement> movements, BlockPos from) {
        BlockPos up = from.above();
        if (isClimbable(up) && canPassThrough(up.above())) {
            movements.add(new PossibleMovement(up, CLIMB_COST, MovementType.CLIMB));
        }
    }
    
    
    private boolean canWalkOn(BlockPos pos) {
        BlockState belowState = world.getBlockState(pos.below());
        

        if (isStairs(pos.below())) {
            return canPassThrough(pos) && canPassThrough(pos.above());
        }
        

        if (!isSolid(pos.below())) {
            return false;
        }
        

        if (!canPassThrough(pos) || !canPassThrough(pos.above())) {
            return false;
        }
        
        return true;
    }
    
    
    private boolean canPassThrough(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || state.isLiquid() || !state.isSolidBlock(world, pos);
    }
    
    
    private boolean isSolid(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.isAir() && state.isSolidBlock(world, pos);
    }
    
    
    private boolean isClimbable(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof net.minecraft.world.level.block.LadderBlock ||
               state.getBlock() instanceof net.minecraft.world.level.block.VineBlock ||
               state.is(net.minecraft.world.level.block.Blocks.SCAFFOLDING) ||
               state.is(net.minecraft.world.level.block.Blocks.TWISTING_VINES) ||
               state.is(net.minecraft.world.level.block.Blocks.WEEPING_VINES);
    }
    
    
    private boolean isStairs(BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof net.minecraft.world.level.block.StairsBlock;
    }
    
    
    public static class PossibleMovement {
        public final BlockPos destination;
        public final double cost;
        public final MovementType type;
        
        public PossibleMovement(BlockPos destination, double cost, MovementType type) {
            this.destination = destination;
            this.cost = cost;
            this.type = type;
        }
    }
}
