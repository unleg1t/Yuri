package ddlc.yuri.utils.player;

import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;
import org.lwjgl.util.vector.Vector2f;

public class RotationUtils implements IMinecraft {
    public enum HitVecMode {
        RANDOMIZED("Randomized"),
        HEAD("Head"),
        BODY("Body"),
        FEET("Feet");

        public final String name;

        HitVecMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum NoiseMode {
        GAUSSIAN("Gaussian"),
        PERLIN("Perlin"),
        FRACTAL3D("Fractal3D"),
        MIXED("Mixed");

        public final String name;

        NoiseMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class FlickHandler {
        private boolean flicking = false;
        private int ticksLeft = 0;
        private int cooldownTicks = 0;
        private float offset = 0f;

        public void update(boolean enabled, boolean hasTarget, boolean inRange, double chance, double angle, int holdTicks, int cooldown) {
            if (!enabled || !hasTarget) {
                flicking = false;
                ticksLeft = 0;
                offset = 0f;
                return;
            }

            if (cooldownTicks > 0) {
                cooldownTicks--;
            }

            if (flicking) {
                ticksLeft--;
                if (ticksLeft <= 0) {
                    flicking = false;
                    offset = 0f;
                    cooldownTicks = cooldown;
                }
                return;
            }

            if (cooldownTicks <= 0 && inRange && MathUtils.getRandom(0.0, 100.0) < chance) {
                flicking = true;
                ticksLeft = holdTicks;
                offset = (MathUtils.getRandom(0.0, 1.0) < 0.5 ? -1f : 1f) * (float) angle;
            }
        }

        public boolean isFlicking() {
            return flicking;
        }

        public float getOffset() {
            return offset;
        }

        public void reset() {
            flicking = false;
            ticksLeft = 0;
            cooldownTicks = 0;
            offset = 0f;
        }
    }

    public static class OvershootHandler {
        private float remaining = 0f;
        private int decayTicks = 0;

        public void trigger(float amount, int ticks) {
            remaining = amount;
            decayTicks = Math.max(1, ticks);
        }

        public Vector2f apply(Vector2f rotation) {
            if (decayTicks <= 0 || remaining == 0f) return rotation;

            float step = remaining / decayTicks;
            Vector2f result = new Vector2f(rotation.x + remaining, rotation.y);
            remaining -= step;
            decayTicks--;
            if (decayTicks <= 0) remaining = 0f;
            return result;
        }

        public void reset() {
            remaining = 0f;
            decayTicks = 0;
        }
    }

    public static float[] getRotationsTo(Vec3 from, Vec3 to) {
        double dx = to.xCoord - from.xCoord;
        double dy = to.yCoord - from.yCoord;
        double dz = to.zCoord - from.zCoord;
        double distHorizontal = MathHelper.sqrt_double(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, distHorizontal));
        yaw = mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw);
        pitch = mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch);
        return new float[]{yaw, pitch};
    }

    public static float[] getRotationFromPosition(double x, double y, double z) {
        double xDiff = x - (Minecraft.getMinecraft()).thePlayer.posX;
        double zDiff = z - (Minecraft.getMinecraft()).thePlayer.posZ;
        double yDiff = y - (Minecraft.getMinecraft()).thePlayer.posY - 1.5f;
        double dist = MathHelper.sqrt_double(xDiff * xDiff + zDiff * zDiff);
        float yaw = (float) (Math.atan2(zDiff, xDiff) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(yDiff, dist) * 180.0D / Math.PI);
        return new float[]{yaw, pitch};
    }

    public static float[] getNormalRotationsFromPosition(double x, double y, double z, float currentYaw,
                                                         float currentPitch, float yawSpeed, float pitchSpeed) {
        if (yawSpeed < 0) {
            yawSpeed *= -1;
        }

        if (pitchSpeed < 0) {
            pitchSpeed *= -1;
        }

        float sYaw = (float) updateRotation((float) currentYaw, (float) getRotationFromPosition(x, y, z)[0], yawSpeed);
        float sPitch = (float) updateRotation((float) currentPitch, (float) getRotationFromPosition(x, y, z)[1],
                pitchSpeed);
        currentYaw = updateRotation(currentYaw, sYaw, 360);
        currentPitch = updateRotation(currentPitch, sPitch, 360);

        if (currentPitch > 90) {
            currentPitch = 90;
        } else if (currentPitch < -90) {
            currentPitch = -90;
        }

        return new float[]{currentYaw, currentPitch};
    }

    public static float updateRotation(float current, float intended, float factor) {
        float var4 = MathHelper.wrapAngleTo180_float(intended - current);

        if (var4 > factor) {
            var4 = factor;
        }

        if (var4 < -factor) {
            var4 = -factor;
        }

        return current + var4;
    }

    public static Vector2f move(final Vector2f targetRotation, final double speed) {
        return move(RotationManager.lastRotations, targetRotation, speed);
    }

    public static Vector2f move(final Vector2f lastRotation, final Vector2f targetRotation, double speed) {
        if (speed != 0) {

            double deltaYaw = MathHelper.wrapAngleTo180_float(targetRotation.x - lastRotation.x);
            final double deltaPitch = (targetRotation.y - lastRotation.y);

            final double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            final double distributionYaw = Math.abs(deltaYaw / distance);
            final double distributionPitch = Math.abs(deltaPitch / distance);

            final double maxYaw = speed * distributionYaw;
            final double maxPitch = speed * distributionPitch;

            final float moveYaw = (float) Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            final float movePitch = (float) Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);

            return new Vector2f(moveYaw, movePitch);
        }

        return new Vector2f(0, 0);
    }

    public static Vector2f applySensitivityPatch(final Vector2f rotation) {
        final Vector2f previousRotation = mc.thePlayer.getPreviousRotation();
        final float mouseSensitivity = (float) (mc.gameSettings.mouseSensitivity * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90, 90));
    }

    public static Vector2f applySensitivityPatch(final Vector2f rotation, final Vector2f previousRotation) {
        final float mouseSensitivity = (float) (mc.gameSettings.mouseSensitivity * (1 + Math.random() / 10000000) * 0.6F + 0.2F);
        final double multiplier = mouseSensitivity * mouseSensitivity * mouseSensitivity * 8.0F * 0.15D;
        final float yaw = previousRotation.x + (float) (Math.round((rotation.x - previousRotation.x) / multiplier) * multiplier);
        final float pitch = previousRotation.y + (float) (Math.round((rotation.y - previousRotation.y) / multiplier) * multiplier);
        return new Vector2f(yaw, MathHelper.clamp_float(pitch, -90, 90));
    }

    public static float[] faceTrajectory(Entity target, boolean predict, float predictSize, float gravity, float velocity) {
        EntityPlayerSP player = mc.thePlayer;

        double posX = target.posX + (predict ? (target.posX - target.prevPosX) * predictSize : 0.0) - (player.posX + (predict ? player.posX - player.prevPosX : 0.0));
        double posY = target.getEntityBoundingBox().minY + (predict ? (target.getEntityBoundingBox().minY - target.prevPosY) * predictSize : 0.0) + target.getEyeHeight() - 0.15 - (player.getEntityBoundingBox().minY + (predict ? player.posY - player.prevPosY : 0.0)) - player.getEyeHeight();
        double posZ = target.posZ + (predict ? (target.posZ - target.prevPosZ) * predictSize : 0.0) - (player.posZ + (predict ? player.posZ - player.prevPosZ : 0.0));
        double posSqrt = Math.sqrt(posX * posX + posZ * posZ);

        velocity = Math.min((velocity * velocity + velocity * 2) / 3, 1f);

        float gravityModifier = 0.12f * gravity;

        return new float[]{
                (float) Math.toDegrees(Math.atan2(posZ, posX)) - 90f,
                (float) -Math.toDegrees(Math.atan((velocity * velocity - Math.sqrt(
                        velocity * velocity * velocity * velocity - gravityModifier * (gravityModifier * posSqrt * posSqrt + 2 * posY * velocity * velocity)
                )) / (gravityModifier * posSqrt)))
        };
    }

    public static Vector2f resetRotation(final Vector2f rotation) {
        if (rotation == null) {
            return null;
        }

        final float yaw = rotation.x + MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - rotation.x);
        final float pitch = mc.thePlayer.rotationPitch;
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f smooth(final Vector2f targetRotation, final double speed) {
        return smooth(RotationManager.lastRotations, targetRotation, speed);
    }

    public static Vector2f smooth(final Vector2f lastRotation, final Vector2f targetRotation, final double speed) {
        float yaw = targetRotation.x;
        float pitch = targetRotation.y;
        final float lastYaw = lastRotation.x;
        final float lastPitch = lastRotation.y;

        if (speed != 0) {
            Vector2f move = move(targetRotation, speed);

            yaw = lastYaw + move.x;
            pitch = lastPitch + move.y;

            for (int i = 1; i <= (int) (Minecraft.getDebugFPS() / 20f + Math.random() * 10); ++i) {

                if (Math.abs(move.x) + Math.abs(move.y) > 0.0001) {
                    yaw += (Math.random() - 0.5) / 1000;
                    pitch -= Math.random() / 200;
                }

                final Vector2f rotations = new Vector2f(yaw, pitch);
                final Vector2f fixedRotations = RotationUtils.applySensitivityPatch(rotations);

                yaw = fixedRotations.x;
                pitch = Math.max(-90, Math.min(90, fixedRotations.y));
            }
        }

        return new Vector2f(yaw, pitch);
    }

    public static float getMovementYaw() {
        float yaw = 180.0f;
        KeyBinding forward = RotationUtils.mc.gameSettings.keyBindForward;
        KeyBinding back = RotationUtils.mc.gameSettings.keyBindBack;
        KeyBinding right = RotationUtils.mc.gameSettings.keyBindRight;
        KeyBinding left = RotationUtils.mc.gameSettings.keyBindLeft;
        if (back.isKeyDown()) {
            yaw -= 180.0f;
            if (right.isKeyDown()) {
                yaw -= 45.0f;
            }
            if (left.isKeyDown()) {
                yaw += 45.0f;
            }
        } else if (forward.isKeyDown()) {
            if (right.isKeyDown()) {
                yaw += 45.0f;
            }
            if (left.isKeyDown()) {
                yaw -= 45.0f;
            }
        } else {
            if (right.isKeyDown()) {
                yaw += 90.0f;
            }
            if (left.isKeyDown()) {
                yaw -= 90.0f;
            }
        }
        return (MathHelper.wrapAngleTo180_float(RotationUtils.mc.thePlayer.rotationYaw) + yaw % 360.0f + 360.0f) % 360.0f;
    }

    public static Vector2f calculate(final Vector3d from, final Vector3d to) {
        final Vector3d diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());
        final float yaw = (float) (MathHelper.atan2(diff.getZ(), diff.getX())
                * MathUtils.TO_DEGREES) - 90.0F;
        final float pitch = (float) (-(MathHelper.atan2(diff.getY(), distance)
                * MathUtils.TO_DEGREES));
        return new Vector2f(yaw, pitch);
    }

    public static Vector2f calculate(final Entity entity) {
        return calculate(entity.getCustomPositionVector().add(0, Math.max(0, Math.min(mc.thePlayer.posY - entity.posY +
                mc.thePlayer.getEyeHeight(), (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * 0.9)), 0));
    }

    public static Vector2f calculate(final Entity entity, final boolean adaptive, final double range) {
        Vector2f normalRotations = calculate(entity);
        if (!adaptive || RayCastUtils.rayCast(normalRotations, range).typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            return normalRotations;
        }

        for (double yPercent = 1; yPercent >= 0; yPercent -= 0.25 + Math.random() * 0.1) {
            for (double xPercent = 1; xPercent >= -0.5; xPercent -= 0.5) {
                for (double zPercent = 1; zPercent >= -0.5; zPercent -= 0.5) {
                    Vector2f adaptiveRotations = calculate(entity.getCustomPositionVector().add(
                            (entity.getEntityBoundingBox().maxX - entity.getEntityBoundingBox().minX) * xPercent,
                            (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) * yPercent,
                            (entity.getEntityBoundingBox().maxZ - entity.getEntityBoundingBox().minZ) * zPercent));

                    if (RayCastUtils.rayCast(adaptiveRotations, range).typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                        return adaptiveRotations;
                    }
                }
            }
        }

        return normalRotations;
    }

    public Vector2f calculate(final Vec3 to, final EnumFacing enumFacing) {
        return calculate(new Vector3d(to.xCoord, to.yCoord, to.zCoord), enumFacing);
    }

    public static Vector2f calculate(final Vec3 to) {
        return calculate(mc.thePlayer.getCustomPositionVector().add(0, mc.thePlayer.getEyeHeight(), 0), new Vector3d(to.xCoord, to.yCoord, to.zCoord));
    }

    public Vector2f calculate(final BlockPos to) {
        return calculate(mc.thePlayer.getCustomPositionVector().add(0, mc.thePlayer.getEyeHeight(), 0), new Vector3d(to.getX(), to.getY(), to.getZ()).add(0.5, 0.5, 0.5));
    }

    public static Vector2f calculate(final Vector3d to) {
        return calculate(mc.thePlayer.getCustomPositionVector().add(0, mc.thePlayer.getEyeHeight(), 0), to);
    }

    public static Vector2f calculate(final Vector3d position, final EnumFacing enumFacing) {
        double x = position.x + 0.5D;
        double y = position.y + 0.5D;
        double z = position.z + 0.5D;

        x += (double) enumFacing.getDirectionVec().getX() * 0.5D;
        y += (double) enumFacing.getDirectionVec().getY() * 0.5D;
        z += (double) enumFacing.getDirectionVec().getZ() * 0.5D;
        return calculate(new Vector3d(x, y, z));
    }

    public static Vec3 getHitVecPoint(EntityLivingBase entity, HitVecMode mode, double randomization) {
        AxisAlignedBB box = entity.getEntityBoundingBox();
        double targetX;
        double targetY;
        double targetZ;

        switch (mode) {
            case HEAD:
                targetX = box.minX + (box.maxX - box.minX) * 0.5;
                targetY = box.maxY - (box.maxY - box.minY) * 0.1;
                targetZ = box.minZ + (box.maxZ - box.minZ) * 0.5;
                break;
            case BODY:
                targetX = box.minX + (box.maxX - box.minX) * 0.5;
                targetY = box.minY + (box.maxY - box.minY) * 0.5;
                targetZ = box.minZ + (box.maxZ - box.minZ) * 0.5;
                break;
            case FEET:
                targetX = box.minX + (box.maxX - box.minX) * 0.5;
                targetY = box.minY + (box.maxY - box.minY) * 0.1;
                targetZ = box.minZ + (box.maxZ - box.minZ) * 0.5;
                break;
            case RANDOMIZED:
            default:
                targetX = box.minX + (box.maxX - box.minX) * MathUtils.getRandom(0.0, 1.0);
                targetY = box.minY + (box.maxY - box.minY) * MathUtils.getRandom(0.0, 1.0);
                targetZ = box.minZ + (box.maxZ - box.minZ) * MathUtils.getRandom(0.0, 1.0);
                break;
        }

        if (mode != HitVecMode.RANDOMIZED && randomization > 0) {
            double jitter = randomization / 10.0;
            targetX += (box.maxX - box.minX) * (MathUtils.getRandom(-0.5, 0.5) * jitter);
            targetY += (box.maxY - box.minY) * (MathUtils.getRandom(-0.5, 0.5) * jitter);
            targetZ += (box.maxZ - box.minZ) * (MathUtils.getRandom(-0.5, 0.5) * jitter);
        }

        return new Vec3(targetX, targetY, targetZ);
    }

    public static Vector2f getHitVecRotation(EntityLivingBase entity, HitVecMode mode, double randomization) {
        Vec3 point = getHitVecPoint(entity, mode, randomization);
        Vec3 eyePos = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        float[] rot = getRotationsTo(eyePos, point);
        return new Vector2f(rot[0], rot[1]);
    }

    public static Vector2f applyNoise(Vector2f rotation, NoiseMode mode, double frequency, long seedOffset) {
        double freq = Math.max(0.1, frequency);
        double t = (System.currentTimeMillis() + seedOffset) / 1000.0 * freq;
        double amplitude = 0.4 + freq * 0.15;
        double yawNoise;
        double pitchNoise;

        switch (mode) {
            case GAUSSIAN:
                yawNoise = MathUtils.getRandom(-1.0, 1.0);
                pitchNoise = MathUtils.getRandom(-1.0, 1.0);
                break;
            case PERLIN:
                yawNoise = Math.sin(t) * 0.6 + Math.sin(t * 2.13) * 0.3;
                pitchNoise = Math.cos(t * 1.7) * 0.6 + Math.sin(t * 3.1) * 0.3;
                break;
            case FRACTAL3D:
                yawNoise = Math.sin(t) + Math.sin(t * 2.0) * 0.5 + Math.sin(t * 4.0) * 0.25;
                pitchNoise = Math.cos(t) + Math.cos(t * 2.0) * 0.5 + Math.cos(t * 4.0) * 0.25;
                break;
            case MIXED:
            default:
                yawNoise = (Math.sin(t) + MathUtils.getRandom(-1.0, 1.0)) * 0.5;
                pitchNoise = (Math.cos(t) + MathUtils.getRandom(-1.0, 1.0)) * 0.5;
                break;
        }

        float yaw = rotation.x + (float) (yawNoise * amplitude);
        float pitch = MathHelper.clamp_float(rotation.y + (float) (pitchNoise * amplitude * 0.5), -90f, 90f);
        return new Vector2f(yaw, pitch);
    }
}