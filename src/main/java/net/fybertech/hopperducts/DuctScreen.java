package net.fybertech.hopperducts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = HopperDuctsMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
final class DuctScreen extends AbstractContainerScreen<DuctMenu> {
    private static final ResourceLocation DUCT_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(HopperDuctsMod.ID, "textures/gui/hopperduct.png");
    private static final ResourceLocation GRATED_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(HopperDuctsMod.ID, "textures/gui/gratedhopper.png");

    private DuctScreen(DuctMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageHeight = menu.grated ? 164 : 133;
        inventoryLabelY = menu.grated ? 71 : 40;
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(HopperDuctsMod.DUCT_MENU.get(), DuctScreen::new);
            MenuScreens.register(HopperDuctsMod.GRATED_MENU.get(), DuctScreen::new);
        });
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float tickDelta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, tickDelta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float tickDelta, int mouseX, int mouseY) {
        graphics.blit(menu.grated ? GRATED_TEXTURE : DUCT_TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.grated) {
            graphics.drawString(font, "=", 35, 55, 0x404040, false);
        }
    }
}
