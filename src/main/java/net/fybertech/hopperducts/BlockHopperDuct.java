package net.fybertech.hopperducts;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockHopperDuct extends BlockContainer {
    public static final PropertyDirection FACING = PropertyDirection.create("facing");
    public static final PropertyBool ENABLED = PropertyBool.create("enabled");
    public static final PropertyInteger CONNECTORS = PropertyInteger.create("connectors", 0, 63);

    protected BlockHopperDuct() {
        super(Material.IRON);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.DOWN).withProperty(ENABLED, true).withProperty(CONNECTORS, 0));
        this.setCreativeTab(CreativeTabs.REDSTONE);
        this.setSoundType(SoundType.METAL).setHardness(3.0F).setResistance(8.0F).setTranslationKey("hopperduct");
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        float x1 = 0.25F;
        float x2 = 1.0F - x1;
        float y1 = 0.25F;
        float y2 = 1.0F - y1;
        float z1 = 0.25F;
        float z2 = 1.0F - z1;
        float par2 = pos.getX();
        float par3 = pos.getY();
        float par4 = pos.getZ();
        if (isDuctConnectedTo(source, pos.west(), EnumFacing.EAST)) {
            x1 = 0.0F;
        }

        if (isDuctConnectedTo(source, pos.east(), EnumFacing.WEST)) {
            x2 = 1.0F;
        }

        if (isDuctConnectedTo(source, pos.north(), EnumFacing.SOUTH)) {
            z1 = 0.0F;
        }

        if (isDuctConnectedTo(source, pos.south(), EnumFacing.NORTH)) {
            z2 = 1.0F;
        }

        if (isDuctConnectedTo(source, pos.down(), EnumFacing.UP)) {
            y1 = 0.0F;
        }

        if (isDuctConnectedTo(source, pos.up(), EnumFacing.DOWN)) {
            y2 = 1.0F;
        }

        EnumFacing facing = state.getValue(FACING);
        switch (facing) {
            case DOWN:
                y1 = 0.0F;
                break;
            case UP:
                y2 = 1.0F;
                break;
            case NORTH:
                z1 = 0.0F;
                break;
            case SOUTH:
                z2 = 1.0F;
                break;
            case WEST:
                x1 = 0.0F;
                break;
            case EAST:
                x2 = 1.0F;
        }

        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }

    public static boolean isDuctConnectedTo(IBlockAccess blockaccess, BlockPos pos, EnumFacing dir) {
        Block block = blockaccess.getBlockState(pos).getBlock();
        if (block == HopperDuctsMod.blockHopperDuct) {
            if (blockaccess.getBlockState(pos).getValue(FACING) == dir) {
                return true;
            }
        } else if (block == HopperDuctsMod.blockGratedHopper) {
            if (blockaccess.getBlockState(pos).getValue(BlockGratedHopper.FACING) == dir) {
                return true;
            }
        } else if (block == Blocks.HOPPER && blockaccess.getBlockState(pos).getValue(BlockHopper.FACING) == dir) {
            return true;
        }

        return false;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        EnumFacing enumfacing1 = facing.getOpposite();
        return this.getDefaultState()
            .withProperty(FACING, enumfacing1)
            .withProperty(ENABLED, true)
            .withProperty(CONNECTORS, this.getNeighorConnectorsValue(worldIn, pos));
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityHopperDuct();
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        if (stack.hasDisplayName()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof TileEntityDuctObject) {
                ((TileEntityDuctObject)tileentity).setCustomName(stack.getDisplayName());
            }
        }
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        this.updateState(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(
        World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ
    ) {
        if (worldIn.isRemote) {
            return true;
        }

        ItemStack heldItem = playerIn.getHeldItem(hand);
        if (!heldItem.isEmpty()) {
            Item held = heldItem.getItem();
            if (held == Item.getItemFromBlock(this)) {
                return false;
            }

            if (held == Item.getItemFromBlock(HopperDuctsMod.blockGratedHopper)) {
                return false;
            }

            if (held == Item.getItemFromBlock(Blocks.HOPPER)) {
                return false;
            }

            if (held == Items.STICK) {
                int facingord = state.getValue(FACING).ordinal();
                if (--facingord >= 6) {
                    facingord = 0;
                }

                if (facingord < 0) {
                    facingord = 5;
                }

                worldIn.setBlockState(pos, state.withProperty(FACING, EnumFacing.values()[facingord]), 4);
                return true;
            }
        }

        if (worldIn.getTileEntity(pos) instanceof TileEntityDuctObject) {
            playerIn.openGui(HopperDuctsMod.instance, 0, worldIn, pos.getX(), pos.getY(), pos.getZ());
        }

        return true;
    }

    @Override
    public void onBlockClicked(World worldIn, BlockPos pos, EntityPlayer playerIn) {
        if (!worldIn.isRemote) {
            if (!playerIn.getHeldItem(EnumHand.MAIN_HAND).isEmpty()
                && playerIn.getHeldItem(EnumHand.MAIN_HAND).getItem() == Items.STICK) {
                IBlockState state = worldIn.getBlockState(pos);
                int facing = state.getValue(FACING).ordinal();
                if (playerIn.isSneaking()) {
                    facing--;
                } else {
                    facing++;
                }

                if (facing >= 6) {
                    facing = 0;
                }

                if (facing < 0) {
                    facing = 5;
                }

                worldIn.setBlockState(pos, state.withProperty(FACING, EnumFacing.values()[facing]), 6);
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
        this.updateState(worldIn, pos, state);
    }

    private void updateState(World worldIn, BlockPos pos, IBlockState state) {
        boolean ductEnabled = !worldIn.isBlockPowered(pos);
        int flags = 0;
        boolean doUpdate = false;
        if (ductEnabled != state.getValue(ENABLED)) {
            state = state.withProperty(ENABLED, ductEnabled);
            flags |= 4;
            doUpdate = true;
        }

        if (doUpdate) {
            worldIn.setBlockState(pos, state, flags);
        }
    }

    @Override
    public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
        TileEntity te = worldIn.getTileEntity(pos);
        if (te != null) {
            World world = te.getWorld();
            boolean ductEnabled = !world.isBlockPowered(pos);
            if (ductEnabled != state.getValue(ENABLED)) {
                state = state.withProperty(ENABLED, ductEnabled);
            }
        }

        int newConnectors = this.getNeighorConnectorsValue(worldIn, pos);
        return state.withProperty(CONNECTORS, newConnectors);
    }

    public int getNeighorConnectorsValue(IBlockAccess worldIn, BlockPos pos) {
        int connectors = 0;

        for (EnumFacing facing : EnumFacing.values()) {
            BlockPos neighborpos = pos.offset(facing);
            IBlockState bs = worldIn.getBlockState(neighborpos);
            if (bs.getBlock() instanceof BlockHopperDuct && neighborpos.offset(bs.getValue(FACING)).equals(pos)) {
                connectors |= 1 << facing.ordinal();
            }

            if (bs.getBlock() instanceof BlockGratedHopper && neighborpos.offset(bs.getValue(BlockGratedHopper.FACING)).equals(pos)) {
                connectors |= 1 << facing.ordinal();
            }

            if (bs.getBlock() instanceof BlockHopper && neighborpos.offset(bs.getValue(BlockHopper.FACING)).equals(pos)) {
                connectors |= 1 << facing.ordinal();
            }
        }

        return connectors;
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof TileEntityDuctObject) {
            InventoryHelper.dropInventoryItems(worldIn, pos, (TileEntityDuctObject)tileentity);
            worldIn.updateComparatorOutputLevel(pos, this);
        }

        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return true;
    }

    public static EnumFacing getFacing(int meta) {
        return EnumFacing.byIndex(meta & 7);
    }

    public static boolean isEnabled(int meta) {
        return (meta & 8) != 8;
    }

    @Override
    public boolean hasComparatorInputOverride(IBlockState state) {
        return true;
    }

    @Override
    public int getComparatorInputOverride(IBlockState state, World worldIn, BlockPos pos) {
        return Container.calcRedstone(worldIn.getTileEntity(pos));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(FACING, getFacing(meta)).withProperty(ENABLED, isEnabled(meta));
    }

    private int getConnectors(int meta) {
        return meta >> 8 & 63;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        byte b0 = 0;
        int i = b0 | state.getValue(FACING).getIndex();
        if (!state.getValue(ENABLED)) {
            i |= 8;
        }

        return i;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ENABLED, CONNECTORS);
    }
}
