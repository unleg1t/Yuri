package ddlc.yuri.utils.player;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public final class ScaffoldUtils {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int HOTBAR_SIZE = 9;

    private ScaffoldUtils() {}

    private static Vector2f serverRotations() {
        return RotationManager.rotations != null ? RotationManager.rotations : new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
    }

    private static void computeJPSRotations(BlockPos blockFace, EnumFacingOffset enumFacing, float[] target, boolean strict) {
        Vector2f server = serverRotations();
        float serverYaw = server.x;
        float serverPitch = server.y;

        double deltaX = blockFace.getX() + 0.5 - mc.thePlayer.posX;
        double deltaY = blockFace.getY() + 0.5 - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double deltaZ = blockFace.getZ() + 0.5 - mc.thePlayer.posZ;
        double horizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float baseYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
        float basePitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontal));

        float[] angularJumps = {0f, -5f, 5f, -12f, 12f, -25f, 25f, -45f, 45f};

        float bestYaw = baseYaw;
        float bestPitch = basePitch;
        float closestRotationDelta = Float.MAX_VALUE;
        boolean foundStrict = false;

        for (float yawOffset : angularJumps) {
            for (float pitchOffset : angularJumps) {
                float testYaw = MathHelper.wrapAngleTo180_float(baseYaw + yawOffset);
                float testPitch = MathHelper.clamp_float(basePitch + pitchOffset, -90, 90);
                Vector2f testRot = new Vector2f(testYaw, testPitch);

                float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(testYaw - serverYaw));
                float pitchDiff = Math.abs(testPitch - serverPitch);
                float totalDelta = yawDiff + pitchDiff;

                if (totalDelta >= closestRotationDelta) continue;

                if (RayCastUtils.overBlock(testRot, enumFacing.getEnumFacing(), blockFace, strict)) {
                    closestRotationDelta = totalDelta;
                    bestYaw = testYaw;
                    bestPitch = testPitch;
                    foundStrict = true;
                } else if (!strict && !foundStrict && RayCastUtils.overBlock(testRot, enumFacing.getEnumFacing(), blockFace, false)) {
                    closestRotationDelta = totalDelta;
                    bestYaw = testYaw;
                    bestPitch = testPitch;
                }
            }
            if (foundStrict && closestRotationDelta < 15f) break;
        }

        if (closestRotationDelta != Float.MAX_VALUE) {
            target[0] = bestYaw;
            target[1] = bestPitch;
            return;
        }

        for (EnumFacing face : EnumFacing.VALUES) {
            if (face == enumFacing.getEnumFacing()) continue;
            Vector2f fallback = RotationUtils.calculate(
                    new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()), face);
            if (RayCastUtils.overBlock(fallback, face, blockFace, false)) {
                target[0] = fallback.x;
                target[1] = fallback.y;
                return;
            }
        }

        final Vector2f fallback = RotationUtils.calculate(
                new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()),
                enumFacing.getEnumFacing());
        target[0] = fallback.x;
        target[1] = fallback.y;
    }

    public static void computeNormalRotations(BlockPos blockFace, EnumFacingOffset enumFacing, float[] target, ScaffoldModule.SearchAlgorithm algorithm, boolean strict) {
        if (ScaffoldModule.rotations.getValue() == ScaffoldModule.Rotations.OLD) {
            computeOldRotations(blockFace, enumFacing, target);
            return;
        }

        switch (algorithm) {
            case NORMAL: {
                double difference = mc.thePlayer.posY + mc.thePlayer.getEyeHeight()
                        - blockFace.getY() - 0.5 - (Math.random() - 0.5) * 0.1;

                for (int offset = -180; offset <= 180; offset += 45) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - difference, mc.thePlayer.posZ);
                    MovingObjectPosition mop = RayCastUtils.rayCast(
                            new Vector2f((float) (mc.thePlayer.rotationYaw + (offset * 3)), 0), 4.5);
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + difference, mc.thePlayer.posZ);

                    if (mop == null || mop.hitVec == null) return;

                    Vector2f rotations = RotationUtils.calculate(mop.hitVec);
                    if (RayCastUtils.overBlock(rotations, blockFace, enumFacing.getEnumFacing())) {
                        target[0] = rotations.x;
                        target[1] = rotations.y;
                        return;
                    }
                }

                final Vector2f rotations = RotationUtils.calculate(
                        new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()),
                        enumFacing.getEnumFacing());

                if (!RayCastUtils.overBlock(new Vector2f(target[0], target[1]), blockFace, enumFacing.getEnumFacing())) {
                    target[0] = rotations.x;
                    target[1] = rotations.y;
                }
                break;
            }
            case SECONDARY: {
                double deltaX = blockFace.getX() - mc.thePlayer.posX + 0.5;
                double deltaY = blockFace.getY() - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight()) + 0.5;
                double deltaZ = blockFace.getZ() - mc.thePlayer.posZ + 0.5;
                double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                float baseYaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0f;
                float basePitch = (float) -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

                float bestYaw = baseYaw;
                float bestPitch = basePitch;
                double bestDistance = Double.MAX_VALUE;

                Vector2f server = serverRotations();

                for (float yawOff = -15; yawOff <= 15; yawOff += 3) {
                    for (float pitchOff = -15; pitchOff <= 15; pitchOff += 3) {
                        float testYaw = baseYaw + yawOff;
                        float testPitch = MathHelper.clamp_float(basePitch + pitchOff, -90, 90);
                        Vector2f testRot = new Vector2f(testYaw, testPitch);

                        if (RayCastUtils.overBlock(testRot, enumFacing.getEnumFacing(), blockFace, strict)) {
                            double yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(testYaw - server.x));
                            double pitchDiff = Math.abs(testPitch - server.y);
                            double totalDist = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

                            if (totalDist < bestDistance) {
                                bestDistance = totalDist;
                                bestYaw = testYaw;
                                bestPitch = testPitch;
                            }
                        }
                    }
                }

                if (bestDistance != Double.MAX_VALUE) {
                    target[0] = bestYaw;
                    target[1] = bestPitch;
                } else {
                    final Vector2f rotations = RotationUtils.calculate(
                            new Vector3d(blockFace.getX(), blockFace.getY(), blockFace.getZ()),
                            enumFacing.getEnumFacing());
                    target[0] = rotations.x;
                    target[1] = rotations.y;
                }
                break;
            }
            case ULTRA_SAFE: {
                computeJPSRotations(blockFace, enumFacing, target, true);
                break;
            }
        }
    }

    public static void computeOldRotations(BlockPos blockFace, EnumFacingOffset enumFacing, float[] target) {
        double x = blockFace.getX() + 0.5;
        double y = blockFace.getY() + 0.5;
        double z = blockFace.getZ() + 0.5;

        switch (enumFacing.getEnumFacing()) {
            case DOWN:  y = blockFace.getY(); break;
            case UP:    y = blockFace.getY() + 1.0; break;
            case NORTH: z = blockFace.getZ(); break;
            case EAST:  x = blockFace.getX() + 1.0; break;
            case SOUTH: z = blockFace.getZ() + 1.0; break;
            case WEST:  x = blockFace.getX(); break;
        }

        final double xDif = x - mc.thePlayer.posX;
        final double zDif = z - mc.thePlayer.posZ;
        final double yDif = y - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        final double xzDist = StrictMath.sqrt(xDif * xDif + zDif * zDif);

        target[0] = (float) (StrictMath.atan2(zDif, xDif) * 180.0D / StrictMath.PI) - 90.0F;
        target[1] = (float) (-(StrictMath.atan2(yDif, xzDist) * 180.0D / StrictMath.PI));
    }

    public static Vec3 computeHitVec(BlockPos blockFace, EnumFacingOffset enumFacing) {
        EnumFacing facing = enumFacing.getEnumFacing();

        double minMargin = 0.1;
        double maxMargin = 0.9;
        double randX = minMargin + Math.random() * (maxMargin - minMargin);
        double randY = minMargin + Math.random() * (maxMargin - minMargin);
        double randZ = minMargin + Math.random() * (maxMargin - minMargin);
        double x = (facing.getAxis() == EnumFacing.Axis.X) ? (facing == EnumFacing.EAST ? blockFace.getX() + 1.0 : blockFace.getX()) : blockFace.getX() + randX;
        double y = (facing.getAxis() == EnumFacing.Axis.Y) ? (facing == EnumFacing.UP   ? blockFace.getY() + 1.0 : blockFace.getY()) : blockFace.getY() + randY;
        double z = (facing.getAxis() == EnumFacing.Axis.Z) ? (facing == EnumFacing.SOUTH ? blockFace.getZ() + 1.0 : blockFace.getZ()) : blockFace.getZ() + randZ;
        Vec3 hitVec = new Vec3(x, y, z);
        final MovingObjectPosition mop = RayCastUtils.rayCast(serverRotations(),mc.playerController.getBlockReachDistance());
        if (mop != null && mop.getBlockPos() != null && mop.hitVec != null&& mop.getBlockPos().equals(blockFace)&& mop.sideHit == facing) {
            double jitter = 0.03;
            double jX = (Math.random() - 0.5) * (jitter * 2);
            double jY = (Math.random() - 0.5) * (jitter * 2);
            double jZ = (Math.random() - 0.5) * (jitter * 2);
            double mopX = facing.getAxis() == EnumFacing.Axis.X ? mop.hitVec.xCoord: Math.max(blockFace.getX() + minMargin, Math.min(blockFace.getX() + maxMargin, mop.hitVec.xCoord + jX));
            double mopY = facing.getAxis() == EnumFacing.Axis.Y ? mop.hitVec.yCoord: Math.max(blockFace.getY() + minMargin, Math.min(blockFace.getY() + maxMargin, mop.hitVec.yCoord + jY));
            double mopZ = facing.getAxis() == EnumFacing.Axis.Z ? mop.hitVec.zCoord: Math.max(blockFace.getZ() + minMargin, Math.min(blockFace.getZ() + maxMargin, mop.hitVec.zCoord + jZ));
            hitVec = new Vec3(mopX, mopY, mopZ);
        }
        return hitVec;
    }

    public static boolean doesNotContainBlock(Vec3i offset, int down) {
        return BlockUtils.blockRelativeToPlayer(offset.getX(), -down + offset.getY(), offset.getZ()).isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer).down(down));
    }

    public static int findPreferredBlockSlot() {
        int fallbackSingle = -1;
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[slot];
            if (stack == null || !(stack.getItem() instanceof ItemBlock)) continue;
            if (BlockUtils.blacklist.contains(((ItemBlock) stack.getItem()).getBlock())) continue;
            if (stack.stackSize > 1) {
                return slot;
            }
            if (fallbackSingle == -1) {
                fallbackSingle = slot;
            }
        }
        return fallbackSingle;
    }

    public static int countBlocks() {
        int count = 0;
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack stack = mc.thePlayer.inventory.mainInventory[slot];
            if (stack != null && stack.getItem() instanceof ItemBlock&& !BlockUtils.blacklist.contains(((ItemBlock) stack.getItem()).getBlock())) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    public static void renderBlockCounter(Enum<?> counterMode, float alpha, int blockCount) {
        ScaledResolution sr = new ScaledResolution(mc);
        int centerX = sr.getScaledWidth() / 2;
        int textWidth = FontUtils.getFont("sf", 14).getStringWidth(String.valueOf(blockCount) + EnumChatFormatting.WHITE + " blocks");
        int textX = centerX - textWidth / 2;
        int textY = sr.getScaledHeight() / 2 + 13;
        Color accent = ColorManager.getColor();
        int accentAlpha = RenderUtils.applyOpacity(accent.getRGB(), alpha);
        if (counterMode == ScaffoldModule.BlockCounter.SIMPLE) {
            FontUtils.getFont("sf", 14).drawStringWithShadow(String.valueOf(blockCount) + EnumChatFormatting.WHITE + " blocks", textX, textY, accentAlpha);
        }
    }
}