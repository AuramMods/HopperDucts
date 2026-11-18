package net.fybertech.hopperducts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiGratedHopper extends GuiContainer {
    ResourceLocation rl = new ResourceLocation("hopperducts:textures/gui/gratedhopper.png");
    private IInventory playerInventory;
    private IInventory gratedHopperInventory;

    public GuiGratedHopper(InventoryPlayer playerInv, IInventory hopperInv) {
        super(new ContainerGratedHopper(playerInv, hopperInv, Minecraft.getMinecraft().player));
        this.playerInventory = playerInv;
        this.gratedHopperInventory = hopperInv;
        this.allowUserInput = false;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int param1, int param2) {
        this.fontRenderer.drawString(this.gratedHopperInventory.getDisplayName().getUnformattedText(), 8, 6, 4210752);
        this.fontRenderer.drawString("=", 35, 55, 4210752);
        this.fontRenderer.drawString(this.playerInventory.getDisplayName().getUnformattedText(), 8, this.ySize - 128 + 33, 4210752);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(this.rl);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - this.xSize) / 2;
        int y = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(x, y, 0, 0, this.xSize, this.ySize);
    }
}
