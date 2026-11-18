package net.fybertech.hopperducts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

class DuctBlock extends BaseEntityBlock {
    static final BooleanProperty ENABLED = HopperBlock.ENABLED;
    private final boolean grated;

    DuctBlock(Properties properties, boolean grated) {
        super(properties);
        this.grated = grated;
        registerDefaultState(stateDefinition.any().setValue(facingProperty(), Direction.DOWN).setValue(ENABLED, true));
    }

    DirectionProperty facingProperty() {
        return HopperBlock.FACING;
    }

    Direction facing(BlockState state) {
        return state.getValue(facingProperty());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(facingProperty(), ENABLED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace().getOpposite();
        return defaultBlockState()
            .setValue(facingProperty(), facing == Direction.UP && grated ? Direction.DOWN : facing)
            .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(facingProperty(), rotation.rotate(facing(state)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(facing(state)));
    }

    @Override
    public DuctBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return grated ? DuctBlockEntity.grated(pos, state) : DuctBlockEntity.duct(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        var ductType = grated ? HopperDuctsMod.GRATED_ENTITY.get() : HopperDuctsMod.DUCT_ENTITY.get();
        return createTickerHelper(type, ductType, DuctBlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        if (!state.is(previous.getBlock())) {
            updateEnabled(state, level, pos);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos from, boolean moving) {
        updateEnabled(state, level, pos);
    }

    private void updateEnabled(BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide) {
            boolean enabled = !level.hasNeighborSignal(pos);
            if (state.getValue(ENABLED) != enabled) {
                level.setBlock(pos, state.setValue(ENABLED, enabled), UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DuctBlockEntity duct) {
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, duct);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (stack.hasCustomHoverName() && level.getBlockEntity(pos) instanceof DuctBlockEntity duct) {
            duct.setCustomName(stack.getHoverName());
            duct.setChanged();
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide
            && level.getBlockEntity(pos) instanceof DuctBlockEntity duct) {
            Containers.dropContents(level, pos, duct);
            Containers.dropContents(level, pos, duct.filters());
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
