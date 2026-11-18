package net.fybertech.hopperducts;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap.Builder;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = "hopperducts", name = "Hopper Ducts", version = "1.5", acceptedMinecraftVersions = "[1.12,1.13)")
public class HopperDuctsMod {
    @Instance("hopperducts")
    public static HopperDuctsMod instance;
    public static int cooldownTime = 8;
    public static BlockHopperDuct blockHopperDuct = null;
    public static BlockGratedHopper blockGratedHopper = null;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        Configuration cfg = new Configuration(event.getSuggestedConfigurationFile());

        try {
            cfg.load();
            cooldownTime = cfg.get("general", "cooldownTime", 8).getInt(8);
        } catch (Exception e) {
            FMLLog.info("Hopper Ducts had a problem loading its configuration");
        } finally {
            if (cfg.hasChanged()) {
                cfg.save();
            }
        }
    }

    public void createBlocks() {
        if (blockHopperDuct == null) {
            blockHopperDuct = new BlockHopperDuct();
            blockHopperDuct.setRegistryName("hopperducts:hopperduct");
        }

        if (blockGratedHopper == null) {
            blockGratedHopper = new BlockGratedHopper();
            blockGratedHopper.setRegistryName("hopperducts:gratedhopper");
        }
    }

    @SubscribeEvent
    public void registerBlocks(Register<Block> event) {
        this.createBlocks();
        event.getRegistry().register(blockHopperDuct);
        event.getRegistry().register(blockGratedHopper);
        GameRegistry.registerTileEntity(TileEntityHopperDuct.class, "tileentityhopperduct");
        GameRegistry.registerTileEntity(TileEntityGratedHopper.class, "tileentitygratedhopper");
    }

    @SubscribeEvent
    public void registerItems(Register<Item> event) {
        this.createBlocks();
        Item itemHopperDuct = new ItemHopperDuct(blockHopperDuct);
        itemHopperDuct.setRegistryName("hopperducts:hopperduct");
        event.getRegistry().register(itemHopperDuct);
        Item itemGratedHopper = new ItemBlock(blockGratedHopper);
        itemGratedHopper.setRegistryName("hopperducts:gratedhopper");
        event.getRegistry().register(itemGratedHopper);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void registerModels(ModelRegistryEvent event) {
        Item itemHopperDuct = Item.getItemFromBlock(blockHopperDuct);
        Item itemGratedHopper = Item.getItemFromBlock(blockGratedHopper);
        ModelLoader.setCustomStateMapper(blockGratedHopper, new Builder().ignore(BlockGratedHopper.ENABLED).build());
        ModelLoader.setCustomStateMapper(blockHopperDuct, new Builder().ignore(BlockHopperDuct.ENABLED).build());
        ModelLoader.setCustomModelResourceLocation(itemHopperDuct, 0, new ModelResourceLocation("hopperducts:hopperduct", "inventory"));
        ModelLoader.setCustomModelResourceLocation(itemGratedHopper, 0, new ModelResourceLocation("hopperducts:gratedhopper", "inventory"));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        GameRegistry.addShapedRecipe(
            new ResourceLocation("hopperducts:hopperduct"),
            new ResourceLocation("minecraft:redstone"),
            new ItemStack(blockHopperDuct, 4),
            "   ",
            "iwi",
            " i ",
            'i',
            Items.IRON_INGOT,
            'w',
            "plankWood"
        );
        GameRegistry.addShapelessRecipe(
            new ResourceLocation("hopperducts:gratedhopper"),
            new ResourceLocation("minecraft:redstone"),
            new ItemStack(blockGratedHopper, 1),
            Ingredient.fromStacks(new ItemStack(Blocks.IRON_BARS)),
            Ingredient.fromStacks(new ItemStack(Blocks.HOPPER))
        );
    }
}
