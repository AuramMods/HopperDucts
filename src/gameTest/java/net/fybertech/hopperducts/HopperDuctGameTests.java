package net.fybertech.hopperducts;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder(HopperDuctsMod.ID)
@PrefixGameTestTemplate(false)
public final class HopperDuctGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void transfersInEveryDirection(GameTestHelper test) {
        List<Direction> directions = List.of(Direction.values());
        for (int i = 0; i < directions.size(); i++) {
            BlockPos pos = testDuctPos(i);
            Direction output = directions.get(i);
            test.setBlock(pos.relative(output), Blocks.BARREL);
            placeDuct(test, pos, output).setItem(0, new ItemStack(Items.IRON_INGOT));
        }
        BlockPos passive = new BlockPos(2, 2, 6);
        DuctBlockEntity duct = placeDuct(test, passive, Direction.EAST);
        test.setBlock(passive.above(), Blocks.BARREL);
        var above = (Container) test.getBlockEntity(passive.above());
        above.setItem(0, new ItemStack(Items.GOLD_INGOT));

        test.succeedWhen(() -> {
            for (int i = 0; i < directions.size(); i++) {
                BlockPos pos = testDuctPos(i);
                test.assertContainerContains(pos.relative(directions.get(i)), Items.IRON_INGOT);
                test.assertContainerEmpty(pos);
            }
            test.assertTrue(duct.isEmpty() && above.getItem(0).is(Items.GOLD_INGOT), "A duct pulled from above");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 65)
    public static void filtersVanillaHoppersAndCapabilities(GameTestHelper test) {
        BlockPos pos = new BlockPos(4, 2, 4);
        test.setBlock(pos, HopperDuctsMod.GRATED_HOPPER.get().defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        test.setBlock(pos.east(), Blocks.BARREL);
        test.setBlock(pos.above(), Blocks.HOPPER);
        test.setBlock(pos.below(), Blocks.HOPPER);

        var grated = (DuctBlockEntity) test.getBlockEntity(pos);
        var above = (Container) test.getBlockEntity(pos.above());
        var below = (Container) test.getBlockEntity(pos.below());
        grated.filters().setItem(4, new ItemStack(Items.IRON_INGOT));
        above.setItem(0, new ItemStack(Items.GOLD_INGOT));
        above.setItem(1, new ItemStack(Items.IRON_INGOT));

        IItemHandler top = itemHandler(grated, Direction.UP);
        IItemHandler side = itemHandler(grated, Direction.NORTH);
        IItemHandler bottom = itemHandler(grated, Direction.DOWN);
        var gold = new ItemStack(Items.GOLD_INGOT);
        test.assertTrue(top.getSlots() == 5 && side.getSlots() == 5, "Automation can see filter slots");
        test.assertTrue(top.insertItem(0, gold, true).getCount() == 1, "Top accepted a nonmatching item");
        test.assertTrue(bottom.insertItem(0, gold, true).isEmpty(), "Bottom refused a nonmatching item");
        test.assertTrue(side.insertItem(0, gold, false).isEmpty(), "Side refused a nonmatching item");
        test.assertFalse(bottom.extractItem(0, 1, true).isEmpty(), "Bottom refused nonmatching extraction");
        grated.setItem(1, new ItemStack(Items.IRON_INGOT));
        test.assertTrue(bottom.extractItem(1, 1, true).isEmpty(), "Bottom extracted a matching item");

        test.succeedWhen(() -> {
            test.assertTrue(((Container) test.getBlockEntity(pos.east())).getItem(0).getCount() == 2, "Both matching items should reach the output");
            test.assertTrue(below.getItem(0).is(Items.GOLD_INGOT), "Vanilla hopper did not extract the nonmatching item");
            test.assertTrue(above.getItem(0).is(Items.GOLD_INGOT), "Top took a nonmatching item");
            test.assertTrue(grated.filters().getItem(4).is(Items.IRON_INGOT), "The real filter item moved");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60)
    public static void redstoneAndFullDestination(GameTestHelper test) {
        BlockPos pos = new BlockPos(4, 2, 4);
        test.setBlock(pos.east(), Blocks.BARREL);
        test.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
        var duct = placeDuct(test, pos, Direction.EAST);
        duct.setItem(0, new ItemStack(Items.IRON_INGOT, 64));
        var destination = (Container) test.getBlockEntity(pos.east());

        test.runAfterDelay(12, () -> {
            test.assertBlockProperty(pos, DuctBlock.ENABLED, false);
            test.assertTrue(destination.isEmpty() && duct.getItem(0).getCount() == 64, "Powered duct moved items");
            var state = test.getBlockState(pos);
            test.assertTrue(state.getAnalogOutputSignal(test.getLevel(), test.absolutePos(pos)) == 15, "Full duct comparator strength");
            for (int slot = 0; slot < destination.getContainerSize(); slot++) {
                destination.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            test.setBlock(pos.west(), Blocks.AIR);
        });
        test.runAfterDelay(26, () -> {
            test.assertBlockProperty(pos, DuctBlock.ENABLED, true);
            test.assertTrue(duct.getItem(0).getCount() == 64, "Full destination consumed an item");
            destination.setItem(0, ItemStack.EMPTY);
        });
        test.succeedWhen(() -> {
            test.assertTrue(destination.getItem(0).is(Items.IRON_INGOT), "Unblocked transfer did not resume");
            test.assertTrue(duct.getItem(0).getCount() + destination.getItem(0).getCount() == 64, "Items were duplicated or lost");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void picksUpMatchingDroppedItems(GameTestHelper test) {
        BlockPos pos = new BlockPos(3, 2, 3);
        test.setBlock(pos, HopperDuctsMod.GRATED_HOPPER.get());
        var grated = (DuctBlockEntity) test.getBlockEntity(pos);
        grated.filters().setItem(0, new ItemStack(Items.IRON_INGOT));

        BlockPos above = test.absolutePos(pos.above());
        var iron = new ItemEntity(test.getLevel(), above.getX() + 0.5, above.getY() + 0.1, above.getZ() + 0.5, new ItemStack(Items.IRON_INGOT, 3));
        var gold = new ItemEntity(test.getLevel(), above.getX() + 0.4, above.getY() + 0.1, above.getZ() + 0.5, new ItemStack(Items.GOLD_INGOT));
        iron.setNoGravity(true);
        gold.setNoGravity(true);
        test.getLevel().addFreshEntity(gold);
        test.getLevel().addFreshEntity(iron);

        test.succeedWhen(() -> {
            test.assertTrue(grated.getItem(0).is(Items.IRON_INGOT) && grated.getItem(0).getCount() == 3, "Matching dropped stack was not collected");
            test.assertTrue(iron.isRemoved() && !gold.isRemoved(), "Filter incorrectly handled dropped items");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void savesFilterPositionsAndDropsContents(GameTestHelper test) {
        BlockPos pos = new BlockPos(4, 2, 4);
        BlockPos worldPos = test.absolutePos(pos);
        test.setBlock(pos, HopperDuctsMod.GRATED_HOPPER.get());
        var grated = (DuctBlockEntity) test.getBlockEntity(pos);
        var state = test.getBlockState(pos);
        grated.setItem(0, new ItemStack(Items.GOLD_INGOT, 3));
        grated.filters().setItem(4, new ItemStack(Items.IRON_INGOT, 2));
        grated.setCustomName(Component.literal("QA Filter"));
        var saved = grated.saveWithFullMetadata();

        test.getLevel().removeBlockEntity(worldPos);
        var loaded = (DuctBlockEntity) BlockEntity.loadStatic(worldPos, state, saved);
        test.assertTrue(loaded != null, "Saved block entity type was not registered");
        test.getLevel().setBlockEntity(loaded);
        test.assertTrue(loaded.getItem(0).getCount() == 3, "Storage did not persist");
        test.assertTrue(loaded.filters().getItem(0).isEmpty(), "Sparse filter slots shifted on load");
        test.assertTrue(loaded.filters().getItem(4).getCount() == 2, "Filter did not persist");
        test.assertTrue(loaded.getName().getString().equals("QA Filter"), "Custom name did not persist");
        test.getLevel().destroyBlock(worldPos, true);
        test.assertItemEntityCountIs(Items.GOLD_INGOT, pos, 2.0, 3);
        test.assertItemEntityCountIs(Items.IRON_INGOT, pos, 2.0, 2);
        test.succeed();
    }

    private static BlockPos testDuctPos(int i) {
        return new BlockPos(2 + i * 2, 2, 2);
    }

    private static DuctBlockEntity placeDuct(GameTestHelper test, BlockPos pos, Direction output) {
        var duct = HopperDuctsMod.DUCT.get();
        test.setBlock(pos, duct.defaultBlockState().setValue(duct.facingProperty(), output));
        return (DuctBlockEntity) test.getBlockEntity(pos);
    }

    private static IItemHandler itemHandler(DuctBlockEntity duct, Direction side) {
        return duct.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElseThrow(IllegalStateException::new);
    }
}
