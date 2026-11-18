package net.fybertech.hopperducts;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

final class DuctTransfer {
    private DuctTransfer() {}

    static void push(Level level, BlockPos pos, Direction output, DuctBlockEntity duct) {
        IItemHandler destination = inventoryAt(level, pos.relative(output), output.getOpposite());
        if (destination == null) {
            return;
        }

        for (int slot = 0; slot < duct.getContainerSize(); slot++) {
            ItemStack stack = duct.getItem(slot);
            if (!stack.isEmpty() && duct.matchesFilter(stack)
                && ItemHandlerHelper.insertItemStacked(destination, stack.copyWithCount(1), false).isEmpty()) {
                duct.removeItem(slot, 1);
                return;
            }
        }
    }

    static void pull(Level level, BlockPos pos, DuctBlockEntity duct) {
        var intake = duct.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElseThrow(IllegalStateException::new);
        BlockPos above = pos.above();
        IItemHandler source = inventoryAt(level, above, Direction.DOWN);
        if (source == null) {
            pickUpItems(level, duct, intake);
            return;
        }

        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack candidate = source.extractItem(slot, 1, true);
            if (candidate.isEmpty() || !ItemHandlerHelper.insertItemStacked(intake, candidate, true).isEmpty()) {
                continue;
            }

            ItemStack taken = source.extractItem(slot, 1, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(intake, taken, false);
            if (!remainder.isEmpty()) {
                // A handler can change its answer between simulation and extraction. Preserve the item.
                remainder = source.insertItem(slot, remainder, false);
                if (!remainder.isEmpty()) {
                    Block.popResource(level, above, remainder);
                }
            }
            return;
        }
    }

    private static void pickUpItems(Level level, DuctBlockEntity duct, IItemHandler intake) {
        for (var entity : HopperBlockEntity.getItemsAtAndAbove(level, duct)) {
            ItemStack stack = entity.getItem();
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(intake, stack, false);
            if (remainder.isEmpty()) {
                entity.discard();
                return;
            }
            entity.setItem(remainder);
        }
    }

    @Nullable
    private static IItemHandler inventoryAt(Level level, BlockPos pos, Direction side) {
        if (!level.hasChunkAt(pos)) {
            return null;
        }

        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
            if (handler != null) {
                return handler;
            }
        }

        // Vanilla lookup also handles joined chests, inventory minecarts, and containers without a block entity.
        var container = HopperBlockEntity.getContainerAt(level, pos);
        if (container instanceof WorldlyContainer sided) {
            return new SidedInvWrapper(sided, side);
        }
        return container == null ? null : new InvWrapper(container);
    }
}
