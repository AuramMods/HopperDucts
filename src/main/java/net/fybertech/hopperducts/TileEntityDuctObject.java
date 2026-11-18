package net.fybertech.hopperducts;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.IHopper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class TileEntityDuctObject extends TileEntityLockable implements IHopper, ITickable, ISidedInventory {
    private NonNullList<ItemStack> inventory = NonNullList.withSize(10, ItemStack.EMPTY);
    private String customName;
    private int transferCooldown = -1;
    private int totalCounter = 0;
    private final int visibleSlotCount;
    public final boolean isGratedHopper;
    private static final int[] slots_GratedHopper = new int[]{0, 1, 2, 3, 4};
    private static final int[] slots_filter = new int[]{5, 6, 7, 8, 9};
    private static final int[] slots_HopperDuct = new int[]{0};
    private IItemHandler itemHandler;

    public TileEntityDuctObject(boolean isGratedHopper) {
        this.isGratedHopper = isGratedHopper;
        if (isGratedHopper) {
            this.visibleSlotCount = 5;
        } else {
            this.visibleSlotCount = 1;
        }
    }

    @Override
    public void updateContainingBlockInfo() {
        super.updateContainingBlockInfo();
    }

    @Override
    public boolean isInvalid() {
        return super.isInvalid();
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
        return !(newSate.getBlock() instanceof BlockHopperDuct) && !(newSate.getBlock() instanceof BlockGratedHopper);
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.inventory = NonNullList.withSize(this.isGratedHopper ? this.visibleSlotCount * 2 : this.visibleSlotCount, ItemStack.EMPTY);
        if (compound.hasKey("CustomName", 8)) {
            this.customName = compound.getString("CustomName");
        }

        this.transferCooldown = compound.getInteger("TransferCooldown");
        NBTTagList nbttaglist = compound.getTagList("Items", 10);

        for (int i = 0; i < nbttaglist.tagCount(); i++) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(i);
            byte b0 = nbttagcompound1.getByte("Slot");
            if (b0 >= 0 && b0 < this.inventory.size()) {
                this.inventory.set(b0, new ItemStack(nbttagcompound1));
            }
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList nbttaglist = new NBTTagList();

        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.get(i);
            if (stack != null && !stack.isEmpty()) {
                NBTTagCompound nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Slot", (byte)i);
                stack.writeToNBT(nbttagcompound1);
                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        compound.setTag("Items", nbttaglist);
        compound.setInteger("TransferCooldown", this.transferCooldown);
        if (this.hasCustomName()) {
            compound.setString("CustomName", this.customName);
        }

        return compound;
    }

    @Override
    public int getSizeInventory() {
        return this.visibleSlotCount;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.inventory.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(this.inventory, index, count);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.inventory.set(index, stack);
        if (stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }
    }

    @Override
    public String getName() {
        if (this.hasCustomName()) {
            return this.customName;
        } else {
            return this.isGratedHopper ? "container.gratedhopper" : "container.hopperduct";
        }
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null && this.customName.length() > 0;
    }

    public void setCustomName(String customNameIn) {
        this.customName = customNameIn;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.getWorld().getTileEntity(this.pos) != this
            ? false
            : player.getDistanceSq(
                    this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5
                )
                <= 64.0;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public void update() {
        if (this.getWorld() != null && !this.getWorld().isRemote) {
            this.transferCooldown--;
            if (this.transferCooldown <= 0) {
                this.transferCooldown = HopperDuctsMod.cooldownTime;
                if (!BlockHopperDuct.isEnabled(this.getBlockMetadata())) {
                    return;
                }

                boolean contentChanged = false;
                if (!this.isDuctObjectEmpty()) {
                    contentChanged = this.pushOutItems();
                }

                if (!this.isDuctObjectFull() && this.isGratedHopper) {
                    contentChanged = this.pullInItems(this) || contentChanged;
                }

                if (contentChanged) {
                    this.markDirty();
                }
            }
        }
    }

    private boolean isDuctObjectEmpty() {
        return this.isEmpty();
    }

    private boolean isDuctObjectFull() {
        for (int n = 0; n < this.visibleSlotCount; n++) {
            ItemStack itemstack = this.inventory.get(n);
            if (itemstack.isEmpty() || itemstack.getCount() < itemstack.getMaxStackSize()) {
                return false;
            }
        }

        return true;
    }

    private boolean pushOutItems() {
        IInventory iinventory = this.getInventoryPointedTo();
        if (iinventory == null) {
            return false;
        }

        EnumFacing enumfacing = BlockHopperDuct.getFacing(this.getBlockMetadata()).getOpposite();
        if (this.isInventoryFull(iinventory, enumfacing)) {
            return false;
        }

        int isize = this.getSizeInventory();

        for (int i = 0; i < isize; i++) {
            ItemStack stackInSlot = this.getStackInSlot(i);
            if (!stackInSlot.isEmpty() && this.isValidFilterItem(stackInSlot)) {
                ItemStack itemstack = stackInSlot.copy();
                ItemStack itemstack1 = putStackInInventoryAllSlots(this, iinventory, this.decrStackSize(i, 1), enumfacing);
                if (itemstack1.isEmpty()) {
                    iinventory.markDirty();
                    return true;
                }

                this.setInventorySlotContents(i, itemstack);
            }
        }

        return false;
    }

    private boolean isInventoryFull(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory) {
            ISidedInventory isidedinventory = (ISidedInventory)inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int k : aint) {
                ItemStack itemstack1 = isidedinventory.getStackInSlot(k);
                if (itemstack1.isEmpty() || itemstack1.getCount() != itemstack1.getMaxStackSize()) {
                    return false;
                }
            }
        } else {
            int i = inventoryIn.getSizeInventory();

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = inventoryIn.getStackInSlot(j);
                if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isInventoryEmpty(IInventory inventoryIn, EnumFacing side) {
        if (inventoryIn instanceof ISidedInventory) {
            ISidedInventory isidedinventory = (ISidedInventory)inventoryIn;
            int[] aint = isidedinventory.getSlotsForFace(side);

            for (int i : aint) {
                if (!isidedinventory.getStackInSlot(i).isEmpty()) {
                    return false;
                }
            }
        } else {
            int j = inventoryIn.getSizeInventory();

            for (int k = 0; k < j; k++) {
                if (!inventoryIn.getStackInSlot(k).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean pullInItems(IHopper hopper) {
        IInventory iinventory = TileEntityHopper.getInventoryAtPosition(
            hopper.getWorld(), hopper.getXPos(), hopper.getYPos() + 1.0, hopper.getZPos()
        );
        if (iinventory != null) {
            EnumFacing enumfacing = EnumFacing.DOWN;
            if (isInventoryEmpty(iinventory, enumfacing)) {
                return false;
            }

            if (iinventory instanceof ISidedInventory) {
                ISidedInventory isidedinventory = (ISidedInventory)iinventory;
                int[] aint = isidedinventory.getSlotsForFace(enumfacing);

                for (int i : aint) {
                    if (this.pullItemFromSlot(hopper, iinventory, i, enumfacing)) {
                        return true;
                    }
                }
            } else {
                int j = iinventory.getSizeInventory();

                for (int k = 0; k < j; k++) {
                    if (this.pullItemFromSlot(hopper, iinventory, k, enumfacing)) {
                        return true;
                    }
                }
            }
        } else {
            for (EntityItem entityitem : TileEntityHopper.getCaptureItems(
                hopper.getWorld(), hopper.getXPos(), hopper.getYPos(), hopper.getZPos()
            )) {
                if (putDropInInventoryAllSlots((IInventory)null, hopper, entityitem)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean pullItemFromSlot(IHopper hopper, IInventory inventoryIn, int index, EnumFacing direction) {
        ItemStack itemstack = inventoryIn.getStackInSlot(index);
        if (!itemstack.isEmpty() && canExtractItemFromSlot(inventoryIn, itemstack, index, direction)) {
            ItemStack itemstack1 = itemstack.copy();
            ItemStack itemstack2 = putStackInInventoryAllSlots(inventoryIn, hopper, inventoryIn.decrStackSize(index, 1), (EnumFacing)null);
            if (itemstack2.isEmpty()) {
                inventoryIn.markDirty();
                return true;
            }

            inventoryIn.setInventorySlotContents(index, itemstack1);
        }

        return false;
    }

    public static boolean putDropInInventoryAllSlots(IInventory inventoryIn, IInventory itemIn, EntityItem itemEntityIn) {
        boolean flag = false;
        if (itemEntityIn == null) {
            return false;
        }

        ItemStack itemstack = itemEntityIn.getItem().copy();
        ItemStack itemstack1 = putStackInInventoryAllSlots(inventoryIn, itemIn, itemstack, (EnumFacing)null);
        if (itemstack1.isEmpty()) {
            flag = true;
            itemEntityIn.setDead();
        } else {
            itemEntityIn.setItem(itemstack1);
        }

        return flag;
    }

    public static ItemStack putStackInInventoryAllSlots(IInventory inventoryIn, IInventory stack, ItemStack side, @Nullable EnumFacing facingIn) {
        if (stack instanceof ISidedInventory && facingIn != null) {
            ISidedInventory isidedinventory = (ISidedInventory)stack;
            int[] aint = isidedinventory.getSlotsForFace(facingIn);

            for (int k = 0; k < aint.length && !side.isEmpty(); k++) {
                side = insertStack(inventoryIn, stack, side, aint[k], facingIn);
            }
        } else {
            int i = stack.getSizeInventory();

            for (int j = 0; j < i && !side.isEmpty(); j++) {
                side = insertStack(inventoryIn, stack, side, j, facingIn);
            }
        }

        return side;
    }

    private static boolean canInsertItemInSlot(IInventory inventoryIn, ItemStack stackIn, int countIn, EnumFacing facingIn) {
        return !inventoryIn.isItemValidForSlot(countIn, stackIn)
            ? false
            : !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory)inventoryIn).canInsertItem(countIn, stackIn, facingIn);
    }

    private static boolean canExtractItemFromSlot(IInventory inventoryIn, ItemStack stackIn, int countIn, EnumFacing facingIn) {
        return !(inventoryIn instanceof ISidedInventory) || ((ISidedInventory)inventoryIn).canExtractItem(countIn, stackIn, facingIn);
    }

    private static ItemStack insertStack(IInventory inventoryIn, IInventory stack, ItemStack index, int side, EnumFacing facingIn) {
        ItemStack itemstack = stack.getStackInSlot(side);
        if (canInsertItemInSlot(stack, index, side, facingIn)) {
            boolean flag = false;
            boolean flag1 = stack.isEmpty();
            if (itemstack.isEmpty()) {
                stack.setInventorySlotContents(side, index);
                index = ItemStack.EMPTY;
                flag = true;
            } else if (canCombine(itemstack, index)) {
                int i = index.getMaxStackSize() - itemstack.getCount();
                int j = Math.min(index.getCount(), i);
                index.shrink(j);
                itemstack.grow(j);
                flag = j > 0;
            }

            if (flag && inventoryIn != null) {
                if (inventoryIn instanceof TileEntityDuctObject) {
                    TileEntityDuctObject tileentityhopper = (TileEntityDuctObject)inventoryIn;
                    if (tileentityhopper.mayTransfer()) {
                        tileentityhopper.setTransferCooldown(8);
                    }

                    inventoryIn.markDirty();
                }

                inventoryIn.markDirty();
            }
        }

        return index;
    }

    private IInventory getInventoryPointedTo() {
        EnumFacing enumfacing = BlockHopperDuct.getFacing(this.getBlockMetadata());
        return getInventoryAtLocation(
            this.getWorld(),
            this.pos.getX() + enumfacing.getXOffset(),
            this.pos.getY() + enumfacing.getYOffset(),
            this.pos.getZ() + enumfacing.getZOffset()
        );
    }

    public static IInventory getInventoryAtLocation(World worldIn, double x, double y, double z) {
        Object object = null;
        int floorX = MathHelper.floor(x);
        int floorY = MathHelper.floor(y);
        int floorZ = MathHelper.floor(z);
        BlockPos blockpos = new BlockPos(floorX, floorY, floorZ);
        TileEntity tileentity = worldIn.getTileEntity(new BlockPos(floorX, floorY, floorZ));
        if (tileentity instanceof IInventory) {
            object = (IInventory)tileentity;
            if (object instanceof TileEntityChest) {
                Block block = worldIn.getBlockState(new BlockPos(floorX, floorY, floorZ)).getBlock();
                if (block instanceof BlockChest) {
                    object = ((BlockChest)block).getLockableContainer(worldIn, blockpos);
                }
            }
        }

        if (object == null) {
            List list = worldIn.getEntitiesInAABBexcluding((Entity)null, new AxisAlignedBB(x, y, z, x + 1.0, y + 1.0, z + 1.0), EntitySelectors.HAS_INVENTORY);
            if (list.size() > 0) {
                object = (IInventory)list.get(worldIn.rand.nextInt(list.size()));
            }
        }

        return (IInventory)object;
    }

    private static boolean canCombine(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() != stack2.getItem()
            ? false
            : (
                stack1.getMetadata() != stack2.getMetadata()
                    ? false
                    : (stack1.getCount() > stack1.getMaxStackSize() ? false : ItemStack.areItemStackTagsEqual(stack1, stack2))
            );
    }

    @Override
    public double getXPos() {
        return this.pos.getX();
    }

    @Override
    public double getYPos() {
        return this.pos.getY();
    }

    @Override
    public double getZPos() {
        return this.pos.getZ();
    }

    public void setTransferCooldown(int ticks) {
        this.transferCooldown = ticks;
    }

    public boolean isOnTransferCooldown() {
        return this.transferCooldown > 0;
    }

    public boolean mayTransfer() {
        return this.transferCooldown <= 1;
    }

    @Override
    public String getGuiID() {
        return "hopperducts:hopperduct";
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new ContainerHopperDuct(playerInventory, this, playerIn);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        for (int n = 0; n < this.visibleSlotCount; n++) {
            if (!this.inventory.get(n).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (!this.inventory.get(index).isEmpty()) {
            ItemStack itemstack = this.inventory.get(index);
            this.inventory.set(index, ItemStack.EMPTY);
            return itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.visibleSlotCount; i++) {
            this.inventory.set(i, ItemStack.EMPTY);
        }
    }

    private boolean isValidFilterItem(ItemStack itemstack) {
        if (!this.isGratedHopper) {
            return true;
        }

        for (int n = this.visibleSlotCount; n < this.visibleSlotCount * 2; n++) {
            ItemStack currentStack = this.inventory.get(n);
            if (!currentStack.isEmpty()
                && currentStack.getItem() == itemstack.getItem()
                && currentStack.getItemDamage() == itemstack.getItemDamage()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return this.isGratedHopper ? slots_GratedHopper : slots_HopperDuct;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        if (!this.isGratedHopper) {
            return true;
        }

        int j = -1;
        if (direction != null) {
            j = direction.ordinal();
        }

        return j != -1 && j != 1 ? true : this.isValidFilterItem(itemStackIn);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return !this.isGratedHopper ? true : !this.isValidFilterItem(stack);
    }

    protected IItemHandler createInvHandler() {
        return new InvWrapper(this) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                ItemStack stack = this.getStackInSlot(slot);
                return stack != null
                        && !stack.isEmpty()
                        && TileEntityDuctObject.this.isGratedHopper
                        && TileEntityDuctObject.this.isValidFilterItem(stack)
                    ? ItemStack.EMPTY
                    : super.extractItem(slot, amount, simulate);
            }
        };
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T)(this.itemHandler == null ? (this.itemHandler = this.createInvHandler()) : this.itemHandler);
        } else {
            return super.getCapability(capability, facing);
        }
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? true : super.hasCapability(capability, facing);
    }
}
