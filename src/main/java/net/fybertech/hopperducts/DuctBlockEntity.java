package net.fybertech.hopperducts;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

final class DuctBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, Hopper {
    private static final Direction[] SIDES = Direction.values();
    private static final int[] DUCT_SLOTS = {0};
    private static final int[] GRATED_SLOTS = {0, 1, 2, 3, 4};

    private final boolean grated;
    private final NonNullList<ItemStack> items;
    private final SimpleContainer filters;
    private int cooldown;
    private LazyOptional<IItemHandlerModifiable>[] sideHandlers = SidedInvWrapper.create(this, SIDES);

    private DuctBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean grated) {
        super(type, pos, state);
        this.grated = grated;
        items = NonNullList.withSize(grated ? GRATED_SLOTS.length : DUCT_SLOTS.length, ItemStack.EMPTY);
        filters = new SimpleContainer(grated ? GRATED_SLOTS.length : 0);
        filters.addListener(container -> setChanged());
    }

    static DuctBlockEntity duct(BlockPos pos, BlockState state) {
        return new DuctBlockEntity(HopperDuctsMod.DUCT_ENTITY.get(), pos, state, false);
    }

    static DuctBlockEntity grated(BlockPos pos, BlockState state) {
        return new DuctBlockEntity(HopperDuctsMod.GRATED_ENTITY.get(), pos, state, true);
    }

    static void tick(Level level, BlockPos pos, BlockState state, DuctBlockEntity duct) {
        if (--duct.cooldown > 0) {
            return;
        }

        duct.cooldown = HopperDuctsMod.COOLDOWN_TICKS.get();
        if (!state.getValue(DuctBlock.ENABLED)) {
            return;
        }

        if (!duct.isEmpty()) {
            DuctTransfer.push(level, pos, ((DuctBlock) state.getBlock()).facing(state), duct);
        }
        if (duct.grated) {
            DuctTransfer.pull(level, pos, duct);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.clear();
        ContainerHelper.loadAllItems(tag, items);
        filters.clearContent();
        ListTag savedFilters = tag.getList("FilterItems", Tag.TAG_COMPOUND);
        for (int index = 0; index < savedFilters.size(); index++) {
            CompoundTag entry = savedFilters.getCompound(index);
            int slot = entry.getByte("Slot") & 255;
            if (slot < filters.getContainerSize()) {
                filters.setItem(slot, ItemStack.of(entry));
            }
        }
        cooldown = tag.getInt("TransferCooldown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        ListTag savedFilters = new ListTag();
        for (int slot = 0; slot < filters.getContainerSize(); slot++) {
            ItemStack stack = filters.getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = stack.save(new CompoundTag());
                entry.putByte("Slot", (byte) slot);
                savedFilters.add(entry);
            }
        }
        tag.put("FilterItems", savedFilters);
        tag.putInt("TransferCooldown", cooldown);
    }

    Container filters() {
        return filters;
    }

    // Filter matching follows the original item and damage comparison. Stacking uses the full item tags.
    boolean matchesFilter(ItemStack stack) {
        if (!grated) {
            return true;
        }
        for (int slot = 0; slot < filters.getContainerSize(); slot++) {
            ItemStack filter = filters.getItem(slot);
            if (!filter.isEmpty() && stack.is(filter.getItem()) && stack.getDamageValue() == filter.getDamageValue()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        int limit = Math.min(getMaxStackSize(), stack.getMaxStackSize());
        if (stack.getCount() > limit) {
            stack.setCount(limit);
        }
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(grated ? "container.hopperducts.gratedhopper" : "container.hopperducts.hopperduct");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new DuctMenu(id, inventory, this, filters, grated);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return grated ? GRATED_SLOTS : DUCT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return (side != Direction.UP && side != null) || matchesFilter(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !grated || !matchesFilter(stack);
    }

    @Override
    public double getLevelX() {
        return worldPosition.getX() + 0.5;
    }

    @Override
    public double getLevelY() {
        return worldPosition.getY() + 0.5;
    }

    @Override
    public double getLevelZ() {
        return worldPosition.getZ() + 0.5;
    }

    @Override
    protected IItemHandler createUnSidedHandler() {
        return new SidedInvWrapper(this, null);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && side != null && !isRemoved()) {
            return sideHandlers[side.ordinal()].cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (var handler : sideHandlers) {
            handler.invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        sideHandlers = SidedInvWrapper.create(this, SIDES);
    }
}
