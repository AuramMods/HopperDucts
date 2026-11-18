package net.fybertech.hopperducts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerHopperDuct extends Container {
    private final IInventory hopperInventory;

    public ContainerHopperDuct(InventoryPlayer playerInventory, IInventory hopperInventoryIn, EntityPlayer player) {
        this.hopperInventory = hopperInventoryIn;
        hopperInventoryIn.openInventory(player);
        byte b0 = 51;
        this.addSlotToContainer(new Slot(hopperInventoryIn, 0, 80, 20));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, i * 18 + b0));
            }
        }

        for (int var7 = 0; var7 < 9; var7++) {
            this.addSlotToContainer(new Slot(playerInventory, var7, 8 + var7 * 18, 58 + b0));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.hopperInventory.isUsableByPlayer(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);
        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();
            if (index < this.hopperInventory.getSizeInventory()) {
                if (!this.mergeItemStack(itemstack1, this.hopperInventory.getSizeInventory(), this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, this.hopperInventory.getSizeInventory(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void onContainerClosed(EntityPlayer playerIn) {
        super.onContainerClosed(playerIn);
        this.hopperInventory.closeInventory(playerIn);
    }
}
