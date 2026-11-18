package net.fybertech.hopperducts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerGratedHopper extends Container {
    private final IInventory hopperInventory;

    public ContainerGratedHopper(InventoryPlayer playerInventory, IInventory hopperInventoryIn, EntityPlayer player) {
        this.hopperInventory = hopperInventoryIn;
        hopperInventoryIn.openInventory(player);
        byte b0 = 82;

        for (int i = 0; i < 5; i++) {
            this.addSlotToContainer(new Slot(hopperInventoryIn, i, 44 + i * 18, 20));
        }

        for (int var7 = 5; var7 < 10; var7++) {
            this.addSlotToContainer(new Slot(hopperInventoryIn, var7, 44 + (var7 - 5) * 18, 51));
        }

        for (int var8 = 0; var8 < 3; var8++) {
            for (int j = 0; j < 9; j++) {
                this.addSlotToContainer(new Slot(playerInventory, j + var8 * 9 + 9, 8 + j * 18, var8 * 18 + b0));
            }
        }

        for (int var9 = 0; var9 < 9; var9++) {
            this.addSlotToContainer(new Slot(playerInventory, var9, 8 + var9 * 18, 58 + b0));
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
