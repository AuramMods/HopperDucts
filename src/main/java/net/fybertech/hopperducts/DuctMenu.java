package net.fybertech.hopperducts;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class DuctMenu extends AbstractContainerMenu {
    private final Container storage;
    private final int storageSize;
    private final int blockSlots;
    final boolean grated;

    DuctMenu(int id, Inventory playerInventory, Container storage, Container filters, boolean grated) {
        super(grated ? HopperDuctsMod.GRATED_MENU.get() : HopperDuctsMod.DUCT_MENU.get(), id);
        this.storage = storage;
        this.grated = grated;
        storageSize = storage.getContainerSize();
        blockSlots = storageSize + filters.getContainerSize();

        for (int slot = 0; slot < storageSize; slot++) {
            addSlot(new Slot(storage, slot, grated ? 44 + slot * 18 : 80, 20));
        }
        for (int slot = 0; slot < filters.getContainerSize(); slot++) {
            addSlot(new Slot(filters, slot, 44 + slot * 18, 51));
        }

        int playerY = grated ? 82 : 51;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + column, 8 + column * 18, playerY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, playerY + 58));
        }
    }

    static DuctMenu client(int id, Inventory inventory, boolean grated) {
        return new DuctMenu(id, inventory, new SimpleContainer(grated ? 5 : 1), new SimpleContainer(grated ? 5 : 0), grated);
    }

    @Override
    public boolean stillValid(Player player) {
        return storage.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < blockSlots
            ? moveItemStackTo(stack, blockSlots, slots.size(), true)
            : moveItemStackTo(stack, 0, storageSize, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
