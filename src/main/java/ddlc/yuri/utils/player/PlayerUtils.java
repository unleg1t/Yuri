package ddlc.yuri.utils.player;

import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerUtils implements IMinecraft {

    public static boolean canSeeEntity(Entity entity) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 targetPos = new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        MovingObjectPosition result = mc.theWorld.rayTraceBlocks(eyes, targetPos, false, true, false);
        return result == null;
    }

    public static double direction() {
        float rotationYaw = mc.thePlayer.rotationYaw;

        if (mc.thePlayer.moveForward < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (mc.thePlayer.moveForward < 0) {
            forward = -0.5F;
        } else if (mc.thePlayer.moveForward > 0) {
            forward = 0.5F;
        }

        if (mc.thePlayer.moveStrafing > 0) {
            rotationYaw -= 90 * forward;
        }

        if (mc.thePlayer.moveStrafing < 0) {
            rotationYaw += 90 * forward;
        }

        return Math.toRadians(rotationYaw);
    }

    public static Block blockRelativeToPlayer(final double offsetX, final double offsetY, final double offsetZ) {
        return block(mc.thePlayer.posX + offsetX, mc.thePlayer.posY + offsetY, mc.thePlayer.posZ + offsetZ);
    }

    public static Block block(final BlockPos blockPos) {
        return mc.theWorld.getBlockState(blockPos).getBlock();
    }

    public static Block block(final double x, final double y, final double z) {
        return mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
    }

    public static boolean inLiquid() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava();
    }

    public static boolean goodPotion(final int id) {
        return GOOD_POTIONS.containsKey(id);
    }

    public static void setSlot(int slot, boolean sync) {
        mc.thePlayer.inventory.currentItem = slot;

        if (sync) mc.playerController.syncCurrentPlayItem();
    }

    public static boolean checkDistance(double height) {
        if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, mc.thePlayer.getEntityBoundingBox().offset(0.0D, -height, 0.0D)).isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public static Block blockUnder() {
        return blockRelativeToPlayer(0, -1, 0);
    }

    private static final HashMap<Integer, Integer> GOOD_POTIONS = new HashMap<Integer, Integer>() {{
        put(6, 1); // Instant Health
        put(10, 2); // Regeneration
        put(11, 3); // Resistance
        put(21, 4); // Health Boost
        put(22, 5); // Absorption
        put(23, 6); // Saturation
        put(5, 7); // Strength
        put(1, 8); // Speed
        put(12, 9); // Fire Resistance
        put(14, 10); // Invisibility
        put(3, 11); // Haste
        put(13, 12); // Water Breathing
    }};

    public static Vec3 getPlacePossibility(double offsetX, double offsetY, double offsetZ) {
        return getPlacePossibility(offsetX, offsetY, offsetZ, null);
    }

    // This methods purpose is to get block placement possibilities, blocks are 1 unit thick so please don't change it to 0.5 it causes bugs.
    public static Vec3 getPlacePossibility(double offsetX, double offsetY, double offsetZ, Integer plane) {

        final List<Vec3> possibilities = new ArrayList<>();
        final int range = (int) (6.0 + (Math.abs(offsetX) + Math.abs(offsetZ)));

        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = BlockUtils.blockRelativeToPlayer(x, y, z);

                    if (!block.isReplaceable(mc.theWorld, new BlockPos(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z))) {
                        for (int x2 = -1; x2 <= 1; x2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x + x2, mc.thePlayer.posY + y, mc.thePlayer.posZ + z));

                        for (int y2 = -1; y2 <= 1; y2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y + y2, mc.thePlayer.posZ + z));

                        for (int z2 = -1; z2 <= 1; z2 += 2)
                            possibilities.add(new Vec3(mc.thePlayer.posX + x, mc.thePlayer.posY + y, mc.thePlayer.posZ + z + z2));
                    }
                }
            }
        }

        possibilities.removeIf(vec3 -> mc.thePlayer.getDistance(vec3.xCoord,
                vec3.yCoord, vec3.zCoord) > 5 || !(BlockUtils.block(vec3.xCoord, vec3.yCoord, vec3.zCoord).isReplaceable(mc.theWorld, new BlockPos(vec3.xCoord, vec3.yCoord, vec3.zCoord))));

        if (possibilities.isEmpty()) return null;

        if (plane != null) {
            possibilities.removeIf(vec3 -> Math.floor(vec3.yCoord + 1) != plane);
        }

        possibilities.sort(Comparator.comparingDouble(vec3 -> {

            final double d0 = (mc.thePlayer.posX + offsetX) - vec3.xCoord;
            final double d1 = (mc.thePlayer.posY - 1 + offsetY) - vec3.yCoord;
            final double d2 = (mc.thePlayer.posZ + offsetZ) - vec3.zCoord;
            return MathHelper.sqrt_double(d0 * d0 + d1 * d1 + d2 * d2);

        }));

        return possibilities.isEmpty() ? null : possibilities.get(0);
    }

    public static boolean isEntityTeamSameAsPlayer(EntityLivingBase target) {
        if (target.getTeam() != null && mc.thePlayer.getTeam() != null) {
            boolean ret0 = target.getDisplayName().getFormattedText().charAt(1)
                    == mc.thePlayer.getDisplayName().
                    getFormattedText().charAt(1);
            boolean ret1 = target.getTeam() == mc.thePlayer.getTeam();

            return ret0 || ret1;

        }
        return false;
    }

    public static boolean isBlockUnder(final double height) {
        return isBlockUnder(height, true);
    }

    public static boolean isBlockUnder(final double height, final boolean boundingBox) {
        if (boundingBox) {
            final AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, -height, 0);

            if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                return true;
            }
        } else {
            for (int offset = 0; offset < height; offset++) {
                if (PlayerUtils.blockRelativeToPlayer(0, -offset, 0).isFullBlock()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean insideBlock() {
        if (mc.thePlayer.ticksExisted < 5) {
            return false;
        }

        final EntityPlayerSP player = mc.thePlayer;
        final WorldClient world = mc.theWorld;
        final AxisAlignedBB bb = player.getEntityBoundingBox();
        for (int x = MathHelper.floor_double(bb.minX); x < MathHelper.floor_double(bb.maxX) + 1; ++x) {
            for (int y = MathHelper.floor_double(bb.minY); y < MathHelper.floor_double(bb.maxY) + 1; ++y) {
                for (int z = MathHelper.floor_double(bb.minZ); z < MathHelper.floor_double(bb.maxZ) + 1; ++z) {
                    final Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
                    final AxisAlignedBB boundingBox;
                    if (block != null && !(block instanceof BlockAir) && (boundingBox = block.getCollisionBoundingBox(world, new BlockPos(x, y, z), world.getBlockState(new BlockPos(x, y, z)))) != null && player.getEntityBoundingBox().intersectsWith(boundingBox)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean blockNear(final int range) {
        for (int x = -range; x <= range; ++x) {
            for (int y = -range; y <= range; ++y) {
                for (int z = -range; z <= range; ++z) {
                    final Block block = blockRelativeToPlayer(x, y, z);

                    if (!(block instanceof BlockAir)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean onLiquid() {
        boolean onLiquid = false;
        final AxisAlignedBB playerBB = mc.thePlayer.getEntityBoundingBox();
        final WorldClient world = mc.theWorld;
        final int y = (int) playerBB.offset(0.0, -0.01, 0.0).minY;
        for (int x = MathHelper.floor_double(playerBB.minX); x < MathHelper.floor_double(playerBB.maxX) + 1; ++x) {
            for (int z = MathHelper.floor_double(playerBB.minZ); z < MathHelper.floor_double(playerBB.maxZ) + 1; ++z) {
                final Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
                if (block != null && !(block instanceof BlockAir)) {
                    if (!(block instanceof BlockLiquid)) {
                        return false;
                    }
                    onLiquid = true;
                }
            }
        }
        return onLiquid;
    }

    public boolean isBlockOver(final double height, final boolean boundingBox) {
        final AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox().offset(0, height / 2f, 0).expand(0, height - mc.thePlayer.height, 0);

        if (!mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
            return true;
        }

        return false;
    }

    public static boolean isBlockUnder() {
        return isBlockUnder(10);
    }

    public double distanceToBlockUnder() {
        double distance = 0;

        for (int i = 0; i < 256; i++) {
            if (blockRelativeToPlayer(0, -i, 0).isFullBlock()) {
                distance = i;
                break;
            }
        }

        return distance;
    }

    public Vec3 getPlacePossibility() {
        return getPlacePossibility(0, 0, 0);
    }

    public static EnumFacingOffset getEnumFacing(final Vec3 position) {
        return getEnumFacing(position, false);
    }

    public static EnumFacingOffset getEnumFacing(final Vec3 position, boolean downwards) {
        List<EnumFacingOffset> possibleFacings = new ArrayList<>();

        int posX = (int) Math.floor(position.xCoord);
        int posY = (int) Math.floor(position.yCoord);
        int posZ = (int) Math.floor(position.zCoord);

        checkAndAddFacing(possibleFacings, posX, posY, posZ - 1, EnumFacing.SOUTH, 0, 0, -1);
        checkAndAddFacing(possibleFacings, posX, posY, posZ + 1, EnumFacing.NORTH, 0, 0, 1);
        checkAndAddFacing(possibleFacings, posX - 1, posY, posZ, EnumFacing.EAST,  -1, 0, 0);
        checkAndAddFacing(possibleFacings, posX + 1, posY, posZ, EnumFacing.WEST,   1, 0, 0);

        if (!possibleFacings.isEmpty()) {
            double jitter = (ThreadLocalRandom.current().nextDouble() - 0.5) * 30.0;
            double currentYaw = (RotationManager.rotations.x % 360 + 90) + jitter;

            possibleFacings.sort(Comparator.comparingDouble(enumFacing -> {
                double facingAngle = Math.toDegrees(Math.atan2(
                        enumFacing.getOffset().zCoord,
                        enumFacing.getOffset().xCoord
                )) % 360;

                return Math.abs(MathUtils.wrappedDifference(facingAngle, currentYaw));
            }));

            if (possibleFacings.size() > 1 && ThreadLocalRandom.current().nextDouble() < 0.25) {
                return possibleFacings.get(1);
            }

            return possibleFacings.get(0);
        }

        if (checkBlock(posX, posY - 1, posZ)) {
            return new EnumFacingOffset(EnumFacing.UP, new Vec3(0, -1, 0));
        } else if (downwards && checkBlock(posX, posY + 1, posZ)) {
            return new EnumFacingOffset(EnumFacing.DOWN, new Vec3(0, 1, 0));
        }

        return null;
    }

    private static void checkAndAddFacing(List<EnumFacingOffset> list, int x, int y, int z, EnumFacing facing, int offX, int offY, int offZ) {
        if (checkBlock(x, y, z)) {
            list.add(new EnumFacingOffset(facing, new Vec3(offX, offY, offZ)));
        }
    }

    private static boolean checkBlock(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return !BlockUtils.block(x, y, z).isReplaceable(mc.theWorld, pos);
    }
}
