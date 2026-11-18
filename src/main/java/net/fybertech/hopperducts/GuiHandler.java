package net.fybertech.hopperducts;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
        if (tileEntity instanceof TileEntityDuctObject) {
            TileEntityDuctObject te = (TileEntityDuctObject)tileEntity;
            return !te.isGratedHopper
                ? new ContainerHopperDuct(player.inventory, te, player)
                : new ContainerGratedHopper(player.inventory, te, player);
        } else {
            return null;
        }
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tileEntity = world.getTileEntity(new BlockPos(x, y, z));
        if (tileEntity instanceof TileEntityDuctObject) {
            TileEntityDuctObject te = (TileEntityDuctObject)tileEntity;
            return !te.isGratedHopper ? new GuiHopperDuct(player.inventory, te) : new GuiGratedHopper(player.inventory, te);
        } else {
            return null;
        }
    }
}
