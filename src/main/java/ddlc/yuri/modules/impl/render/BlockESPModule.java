package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityEnderChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(label = "Block ESP", description = "Highlights storage blocks like chests, ender chests and beds", category = ModuleCategory.RENDER)
public final class BlockESPModule extends Module {

    private final Property<Boolean> chests = new Property<>("Chests", true);
    private final Property<Boolean> enderChests = new Property<>("Ender Chests", true);
    private final Property<Boolean> beds = new Property<>("Beds", true);
    private final Property<Boolean> throughWalls = new Property<>("Through Walls", true);
    private final Property<Boolean> filled = new Property<>("Filled", false);
    private final Property<Boolean> outline = new Property<>("Outline", true);
    private final NumberProperty lineWidth = new NumberProperty("Line Width", 2.0, 1.0, 5.0, 0.5, outline::getValue);
    private final NumberProperty alpha = new NumberProperty("Alpha", 0.3, 0.1, 1.0, 0.05);

    private final NumberProperty range = new NumberProperty("Range", 15, 2, 30, 1, beds::getValue);
    private final NumberProperty rate = new NumberProperty("Rate", 0.4D, 0.1D, 3D, 0.1D, beds::getValue);

    private final List<BlockPos[]> bedsList = new ArrayList<>();
    private long lastCheck = 0L;

    @EventHook
    public void onUpdate(PreUpdateEvent event) {
        if (!beds.getValue() || mc.thePlayer == null || mc.theWorld == null) return;

        if (System.currentTimeMillis() - lastCheck >= rate.getValue() * 1000.0) {
            lastCheck = System.currentTimeMillis();

            int rangeValue = range.getValue().intValue();
            for (int i = -rangeValue; i <= rangeValue; ++i) {
                for (int j = -rangeValue; j <= rangeValue; ++j) {
                    for (int k = -rangeValue; k <= rangeValue; ++k) {
                        BlockPos blockPos = new BlockPos(mc.thePlayer.posX + j, mc.thePlayer.posY + i, mc.thePlayer.posZ + k);
                        IBlockState getBlockState = mc.theWorld.getBlockState(blockPos);
                        if (getBlockState.getBlock() == Blocks.bed && getBlockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                            for (BlockPos[] bedPair : bedsList) {
                                if (BlockPos.isSamePos(blockPos, bedPair[0])) {
                                    continue;
                                }
                            }
                            bedsList.add(new BlockPos[]{blockPos, blockPos.offset(getBlockState.getValue(BlockBed.FACING))});
                        }
                    }
                }
            }
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.theWorld == null || mc.thePlayer == null) return;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();

        if (throughWalls.getValue()) GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (TileEntity te : mc.theWorld.loadedTileEntityList) {
            renderStorageBlock(te);
        }

        renderBeds();

        if (throughWalls.getValue()) GL11.glEnable(GL11.GL_DEPTH_TEST);

        GlStateManager.enableCull();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    private void renderStorageBlock(TileEntity tileEntity) {
        if (tileEntity instanceof TileEntityChest) {
            if (!chests.getValue()) return;
        } else if (tileEntity instanceof TileEntityEnderChest) {
            if (!enderChests.getValue()) return;
        } else {
            return;
        }

        BlockPos pos = tileEntity.getPos();
        if (pos == null) return;

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block == null) return;

        AxisAlignedBB boundingBox = block.getSelectedBoundingBox(mc.theWorld, pos);
        if (boundingBox == null) return;

        double rx = mc.getRenderManager().renderPosX;
        double ry = mc.getRenderManager().renderPosY;
        double rz = mc.getRenderManager().renderPosZ;
        boundingBox = new AxisAlignedBB(
                boundingBox.minX - rx + 0.002, boundingBox.minY - ry + 0.002, boundingBox.minZ - rz + 0.002,
                boundingBox.maxX - rx - 0.002, boundingBox.maxY - ry - 0.002, boundingBox.maxZ - rz - 0.002);

        Color c = ColorManager.getColor();
        float r = c.getRed() / 255f;
        float g = c.getGreen() / 255f;
        float b = c.getBlue() / 255f;
        float a = alpha.getValue().floatValue();

        if (filled.getValue()) {
            GL11.glDepthMask(false);
            GlStateManager.color(r, g, b, a);
            RenderUtils.drawBoundingBox(boundingBox);
            GL11.glDepthMask(true);
        }

        if (outline.getValue()) {
            GL11.glLineWidth(lineWidth.getValue().floatValue());
            GlStateManager.color(r, g, b, a);
            RenderUtils.drawOutlinedBoundingBox(boundingBox);
        }
    }

    private void renderBeds() {
        if (!beds.getValue() || BlockPos.nullCheck() || bedsList.isEmpty()) return;

        Iterator<BlockPos[]> iterator = bedsList.iterator();
        while (iterator.hasNext()) {
            BlockPos[] blockPos = iterator.next();
            if (mc.theWorld.getBlockState(blockPos[0]).getBlock() instanceof BlockBed) {
                RenderUtils.renderBed(blockPos);
            } else {
                iterator.remove();
            }
        }
    }

    @Override
    public void onDisable() {
        bedsList.clear();
    }
}