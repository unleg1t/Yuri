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

    public final Property<Boolean> chests = new Property<>("Chests", true);
    public final Property<Boolean> enderChests = new Property<>("Ender Chests", true);
    public final Property<Boolean> beds = new Property<>("Beds", true);
    public final Property<Boolean> throughWalls = new Property<>("Through Walls", true);
    public final Property<Boolean> filled = new Property<>("Filled", true);
    public final Property<Boolean> outline = new Property<>("Outline", true);
    public final NumberProperty lineWidth = new NumberProperty("Line Width", 1.5, 0.5, 5.0, 0.5, outline::getValue);
    public final NumberProperty alpha = new NumberProperty("Alpha", 0.3, 0.05, 1.0, 0.05, filled::getValue);
    public final Property<Boolean> clientColor = new Property<>("Client Color", false);

    public final NumberProperty range = new NumberProperty("Bed Range", 16, 4, 32, 2, beds::getValue);
    public final NumberProperty rate = new NumberProperty("Bed Update Rate", 1.0D, 0.2D, 5.0D, 0.2D, beds::getValue);

    private final List<BlockPos[]> bedsList = new ArrayList<>();
    private long lastCheck = 0L;

    @EventHook
    public void onUpdate(PreUpdateEvent event) {
        if (!beds.getValue() || mc.thePlayer == null || mc.theWorld == null) return;

        if (System.currentTimeMillis() - lastCheck >= rate.getValue() * 1000.0) {
            lastCheck = System.currentTimeMillis();

            bedsList.removeIf(pair -> pair == null || mc.theWorld.getBlockState(pair[0]).getBlock() != Blocks.bed);

            int rangeValue = range.getValue().intValue();
            BlockPos playerPos = new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);

            for (int x = -rangeValue; x <= rangeValue; ++x) {
                for (int y = -rangeValue; y <= rangeValue; ++y) {
                    for (int z = -rangeValue; z <= rangeValue; ++z) {
                        BlockPos blockPos = playerPos.add(x, y, z);
                        IBlockState getBlockState = mc.theWorld.getBlockState(blockPos);
                        if (getBlockState.getBlock() == Blocks.bed && getBlockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                            boolean alreadyAdded = false;
                            for (BlockPos[] bedPair : bedsList) {
                                if (BlockPos.isSamePos(blockPos, bedPair[0])) {
                                    alreadyAdded = true;
                                    break;
                                }
                            }
                            if (!alreadyAdded) {
                                bedsList.add(new BlockPos[]{blockPos, blockPos.offset(getBlockState.getValue(BlockBed.FACING))});
                            }
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
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GlStateManager.disableCull();

        if (throughWalls.getValue()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
        }

        for (TileEntity te : mc.theWorld.loadedTileEntityList) {
            renderStorageBlock(te);
        }

        renderBeds();

        if (throughWalls.getValue()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
        }

        GlStateManager.enableCull();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();
    }

    private void drawESPBox(AxisAlignedBB bb, Color color) {
        float r = color.getRed() / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue() / 255.0f;
        float a = alpha.getValue().floatValue();

        if (filled.getValue()) {
            GlStateManager.color(r, g, b, a);
            RenderUtils.drawBoundingBox(bb);
        }
        if (outline.getValue()) {
            GL11.glLineWidth(lineWidth.getValue().floatValue());
            GlStateManager.color(r, g, b, 1.0f);
            RenderUtils.drawOutlinedBoundingBox(bb);
        }
    }

    private void renderStorageBlock(TileEntity tileEntity) {
        if (tileEntity == null) return;
        Color color;
        if (tileEntity instanceof TileEntityChest) {
            if (!chests.getValue()) return;
            TileEntityChest chest = (TileEntityChest) tileEntity;
            if (chest.adjacentChestXNeg != null || chest.adjacentChestZNeg != null) {
                return;
            }
            color = clientColor.getValue() ? ColorManager.getColor() : new Color(255, 170, 0);
        } else if (tileEntity instanceof TileEntityEnderChest) {
            if (!enderChests.getValue()) return;
            color = clientColor.getValue() ? ColorManager.getColor() : new Color(180, 50, 255);
        } else {
            return;
        }

        BlockPos pos = tileEntity.getPos();
        if (pos == null) return;

        double rx = mc.getRenderManager().viewerPosX;
        double ry = mc.getRenderManager().viewerPosY;
        double rz = mc.getRenderManager().viewerPosZ;

        AxisAlignedBB bb;
        if (tileEntity instanceof TileEntityChest) {
            TileEntityChest chest = (TileEntityChest) tileEntity;
            double minX = pos.getX() + 0.0625;
            double minY = pos.getY();
            double minZ = pos.getZ() + 0.0625;
            double maxX = pos.getX() + 0.9375;
            double maxY = pos.getY() + 0.875;
            double maxZ = pos.getZ() + 0.9375;

            if (chest.adjacentChestXPos != null) {
                maxX = pos.getX() + 1.9375;
            } else if (chest.adjacentChestZPos != null) {
                maxZ = pos.getZ() + 1.9375;
            }
            bb = new AxisAlignedBB(minX - rx, minY - ry, minZ - rz, maxX - rx, maxY - ry, maxZ - rz);
        } else {
            bb = new AxisAlignedBB(
                    pos.getX() + 0.0625 - rx, pos.getY() - ry, pos.getZ() + 0.0625 - rz,
                    pos.getX() + 0.9375 - rx, pos.getY() + 0.875 - ry, pos.getZ() + 0.9375 - rz
            );
        }

        drawESPBox(bb, color);
    }

    private void renderBeds() {
        if (!beds.getValue() || bedsList.isEmpty()) return;

        Color color = clientColor.getValue() ? ColorManager.getColor() : new Color(255, 50, 80);
        double rx = mc.getRenderManager().viewerPosX;
        double ry = mc.getRenderManager().viewerPosY;
        double rz = mc.getRenderManager().viewerPosZ;

        Iterator<BlockPos[]> iterator = bedsList.iterator();
        while (iterator.hasNext()) {
            BlockPos[] pair = iterator.next();
            if (pair == null || mc.theWorld.getBlockState(pair[0]).getBlock() != Blocks.bed) {
                iterator.remove();
                continue;
            }

            BlockPos foot = pair[0];
            BlockPos head = pair[1];

            double minX = Math.min(foot.getX(), head.getX());
            double maxX = Math.max(foot.getX(), head.getX()) + 1.0;
            double minY = foot.getY();
            double maxY = foot.getY() + 0.5625;
            double minZ = Math.min(foot.getZ(), head.getZ());
            double maxZ = Math.max(foot.getZ(), head.getZ()) + 1.0;

            AxisAlignedBB bb = new AxisAlignedBB(
                    minX - rx, minY - ry, minZ - rz,
                    maxX - rx, maxY - ry, maxZ - rz
            );

            drawESPBox(bb, color);
        }
    }

    @Override
    public void onDisable() {
        bedsList.clear();
    }
}