package net.fybertech.hopperducts;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemHopperDuct extends ItemBlock {
    public ItemHopperDuct(Block block) {
        super(block);
    }

    public EnumFacing getDuctOrientation(EntityPlayer player, World par1World, int par2, int par3, int par4, EnumFacing par5) {
        EnumFacing j1 = par5.getOpposite();
        if (player != null && player.isSneaking()) {
            return j1;
        }

        int newx = par2 + j1.getXOffset();
        int newy = par3 + j1.getYOffset();
        int newz = par4 + j1.getZOffset();
        Block block = par1World.getBlockState(new BlockPos(newx, newy, newz)).getBlock();
        if (block == HopperDuctsMod.blockHopperDuct || block == HopperDuctsMod.blockGratedHopper || block == Blocks.HOPPER) {
            EnumFacing dir = null;
            IBlockState bs = par1World.getBlockState(new BlockPos(newx, newy, newz));
            if (block == HopperDuctsMod.blockHopperDuct) {
                dir = bs.getValue(BlockHopperDuct.FACING);
            } else if (block == HopperDuctsMod.blockGratedHopper) {
                dir = bs.getValue(BlockGratedHopper.FACING);
            } else if (block == Blocks.HOPPER) {
                dir = bs.getValue(BlockHopper.FACING);
            }

            if (dir != par5) {
                return par5.getOpposite();
            }
        }

        for (int n = 0; n < 6; n++) {
            newx = par2 + EnumFacing.values()[n].getXOffset();
            newy = par3 + EnumFacing.values()[n].getYOffset();
            newz = par4 + EnumFacing.values()[n].getZOffset();
            IBlockState bs = par1World.getBlockState(new BlockPos(newx, newy, newz));
            Block block2 = bs.getBlock();
            if (block2 == HopperDuctsMod.blockHopperDuct || block2 == HopperDuctsMod.blockGratedHopper || block2 == Blocks.HOPPER) {
                EnumFacing dir = null;
                if (block2 == HopperDuctsMod.blockHopperDuct) {
                    dir = bs.getValue(BlockHopperDuct.FACING);
                } else if (block2 == HopperDuctsMod.blockGratedHopper) {
                    dir = bs.getValue(BlockGratedHopper.FACING);
                } else if (block2 == Blocks.HOPPER) {
                    dir = bs.getValue(BlockHopper.FACING);
                }

                if (dir != null) {
                    if (n == dir.ordinal()) {
                        return dir;
                    }

                    if (n == dir.getOpposite().ordinal()) {
                        return dir;
                    }

                    return EnumFacing.values()[n];
                }
            }
        }

        return j1;
    }

    @Override
    public boolean placeBlockAt(
        ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, IBlockState newState
    ) {
        EnumFacing facing = this.getDuctOrientation(player, world, pos.getX(), pos.getY(), pos.getZ(), side);
        newState = HopperDuctsMod.blockHopperDuct.getStateFromMeta(facing.ordinal());
        return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
}
