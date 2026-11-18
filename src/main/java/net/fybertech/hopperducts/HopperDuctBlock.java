package net.fybertech.hopperducts;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

final class HopperDuctBlock extends DuctBlock {
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Map<Direction, BooleanProperty> CONNECTORS = Map.of(
        Direction.DOWN, BlockStateProperties.DOWN, Direction.UP, BlockStateProperties.UP,
        Direction.NORTH, BlockStateProperties.NORTH, Direction.SOUTH, BlockStateProperties.SOUTH,
        Direction.WEST, BlockStateProperties.WEST, Direction.EAST, BlockStateProperties.EAST
    );
    private static final VoxelShape[] SHAPES = makeShapes();

    HopperDuctBlock(Properties properties) {
        super(properties, false);
        BlockState state = defaultBlockState();
        for (var property : CONNECTORS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    DirectionProperty facingProperty() {
        return BlockStateProperties.FACING;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(CONNECTORS.values().toArray(BooleanProperty[]::new));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction output = facing(state);
        if (!context.isSecondaryUseActive()) {
            output = selectOutput(level, pos, output);
        }
        state = state.setValue(facingProperty(), output);
        for (Direction side : DIRECTIONS) {
            state = updateConnector(state, side, level.getBlockState(pos.relative(side)));
        }
        return state;
    }

    private static Direction selectOutput(Level level, BlockPos pos, Direction clickedNeighbor) {
        Direction clickedOutput = neighborOutput(level.getBlockState(pos.relative(clickedNeighbor)));
        if (clickedOutput != null && clickedOutput != clickedNeighbor.getOpposite()) {
            return clickedNeighbor;
        }
        for (Direction side : DIRECTIONS) {
            Direction output = neighborOutput(level.getBlockState(pos.relative(side)));
            if (output != null) {
                return output.getAxis() == side.getAxis() ? output : side;
            }
        }
        return clickedNeighbor;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos from) {
        return updateConnector(state, side, neighbor);
    }

    private static BlockState updateConnector(BlockState state, Direction side, BlockState neighbor) {
        return state.setValue(CONNECTORS.get(side), neighborOutput(neighbor) == side.getOpposite());
    }

    private static Direction neighborOutput(BlockState neighbor) {
        if (neighbor.getBlock() instanceof DuctBlock duct) {
            return duct.facing(neighbor);
        }
        return neighbor.getBlock() instanceof HopperBlock ? neighbor.getValue(HopperBlock.FACING) : null;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = super.rotate(state, rotation);
        for (Direction side : DIRECTIONS) {
            rotated = rotated.setValue(CONNECTORS.get(rotation.rotate(side)), state.getValue(CONNECTORS.get(side)));
        }
        return rotated;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state.setValue(facingProperty(), mirror.mirror(facing(state)));
        for (Direction side : DIRECTIONS) {
            mirrored = mirrored.setValue(CONNECTORS.get(mirror.mirror(side)), state.getValue(CONNECTORS.get(side)));
        }
        return mirrored;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = bit(facing(state));
        for (Direction side : DIRECTIONS) {
            if (state.getValue(CONNECTORS.get(side))) {
                mask |= bit(side);
            }
        }
        return SHAPES[mask];
    }

    private static VoxelShape[] makeShapes() {
        VoxelShape[] shapes = new VoxelShape[1 << DIRECTIONS.length];
        for (int mask = 0; mask < shapes.length; mask++) {
            shapes[mask] = Block.box(
                (mask & bit(Direction.WEST)) != 0 ? 0 : 4,
                (mask & bit(Direction.DOWN)) != 0 ? 0 : 4,
                (mask & bit(Direction.NORTH)) != 0 ? 0 : 4,
                (mask & bit(Direction.EAST)) != 0 ? 16 : 12,
                (mask & bit(Direction.UP)) != 0 ? 16 : 12,
                (mask & bit(Direction.SOUTH)) != 0 ? 16 : 12
            );
        }
        return shapes;
    }

    private static int bit(Direction side) {
        return 1 << side.ordinal();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        if (stack.is(Items.STICK)) {
            if (!level.isClientSide) {
                cycle(state, level, pos, -1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(HopperDuctsMod.DUCT_ITEM.get()) || stack.is(HopperDuctsMod.GRATED_HOPPER_ITEM.get()) || stack.is(Items.HOPPER)) {
            return InteractionResult.PASS;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && !player.isCreative() && player.getMainHandItem().is(Items.STICK)) {
            cycle(state, level, pos, player.isShiftKeyDown() ? -1 : 1);
        }
    }

    private void cycle(BlockState state, Level level, BlockPos pos, int step) {
        int index = Math.floorMod(facing(state).ordinal() + step, DIRECTIONS.length);
        level.setBlock(pos, state.setValue(facingProperty(), DIRECTIONS[index]), UPDATE_ALL);
    }
}
