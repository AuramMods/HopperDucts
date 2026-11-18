package net.fybertech.hopperducts;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(HopperDuctsMod.ID)
public final class HopperDuctsMod {
    public static final String ID = "hopperducts";
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ID);

    static final RegistryObject<HopperDuctBlock> DUCT =
        BLOCKS.register("hopperduct", () -> new HopperDuctBlock(blockProperties()));
    static final RegistryObject<DuctBlock> GRATED_HOPPER =
        BLOCKS.register("gratedhopper", () -> new DuctBlock(blockProperties(), true));
    static final RegistryObject<Item> DUCT_ITEM =
        ITEMS.register("hopperduct", () -> new BlockItem(DUCT.get(), new Item.Properties()));
    static final RegistryObject<Item> GRATED_HOPPER_ITEM =
        ITEMS.register("gratedhopper", () -> new BlockItem(GRATED_HOPPER.get(), new Item.Properties()));
    static final RegistryObject<BlockEntityType<DuctBlockEntity>> DUCT_ENTITY = BLOCK_ENTITIES.register(
        "tileentityhopperduct", () -> BlockEntityType.Builder.of(DuctBlockEntity::duct, DUCT.get()).build(null)
    );
    static final RegistryObject<BlockEntityType<DuctBlockEntity>> GRATED_ENTITY = BLOCK_ENTITIES.register(
        "tileentitygratedhopper", () -> BlockEntityType.Builder.of(DuctBlockEntity::grated, GRATED_HOPPER.get()).build(null)
    );
    static final RegistryObject<MenuType<DuctMenu>> DUCT_MENU = MENUS.register("hopperduct",
        () -> new MenuType<>((id, inventory) -> DuctMenu.client(id, inventory, false), FeatureFlags.DEFAULT_FLAGS));
    static final RegistryObject<MenuType<DuctMenu>> GRATED_MENU = MENUS.register("gratedhopper",
        () -> new MenuType<>((id, inventory) -> DuctMenu.client(id, inventory, true), FeatureFlags.DEFAULT_FLAGS));

    private static final ForgeConfigSpec CONFIG;
    static final ForgeConfigSpec.IntValue COOLDOWN_TICKS;

    static {
        ForgeConfigSpec.Builder config = new ForgeConfigSpec.Builder();
        COOLDOWN_TICKS = config.comment("Ticks between Hopper Duct and Grated Hopper transfer attempts.")
            .defineInRange("cooldownTime", 8, 1, Integer.MAX_VALUE);
        CONFIG = config.build();
    }

    public HopperDuctsMod(FMLJavaModLoadingContext context) {
        var bus = context.getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(HopperDuctsMod::addCreativeItems);
        context.registerConfig(ModConfig.Type.SERVER, CONFIG);
    }

    private static BlockBehaviour.Properties blockProperties() {
        return BlockBehaviour.Properties.of().strength(3.0F, 8.0F).sound(SoundType.METAL)
            .requiresCorrectToolForDrops().noOcclusion();
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(DUCT_ITEM);
            event.accept(GRATED_HOPPER_ITEM);
        }
    }
}
