package com.schematica.client.renderer;

import com.schematica.Settings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RegionRenderCache;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

/**
 * RenderCache for schematics.
 */
public class SchematicRenderCache extends RegionRenderCache
{
    private final Minecraft minecraft = Minecraft.getMinecraft();

    /**
     * Create a RenderCache for a schematic.
     *
     * @param world    passed to super.
     * @param from     passed to super.
     * @param to       passed to super.
     * @param subtract passed to super.
     */
    public SchematicRenderCache(final World world, final BlockPos from, final BlockPos to, final int subtract)
    {
        super(world, from, to, subtract);
    }

    /**
     * Return the cached block state.
     *
     * @param pos position of block in SchematicWorld
     */
    @Override
    public IBlockState getBlockState(final BlockPos pos)
    {
        final BlockPos realPos = pos.add(Settings.instance.getSchematicWorld().position);
        final World world = this.minecraft.theWorld;

        if (!world.isAirBlock(realPos))
        {
            return Blocks.air.getDefaultState();
        }

        return super.getBlockState(pos);
    }
}
