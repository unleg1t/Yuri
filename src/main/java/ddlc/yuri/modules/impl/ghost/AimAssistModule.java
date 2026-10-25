package ddlc.yuri.modules.impl.ghost;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.RayCastUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSword;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@ModuleInfo(label = "Aim Assist", description = "Smoothly aims at the closest valid target", category = ModuleCategory.GHOST)
public final class AimAssistModule extends Module {

    private static final float SNAP_ENTER = 5.0f;
    private static final float SNAP_EXIT = 7.0f;
    private static final long MIN_ADAPTIVE_NANOS = 8333333L;
    private static final long SIMPLE_STEP_NANOS = 1000000L;
    private static final int MAX_SIMPLE_STEPS = 30;

    public enum AimMode {
        SIMPLE("Simple"), ADAPTIVE("Adaptive");
        public final String name;

        AimMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum TargetMode {
        YAW("Yaw"), DISTANCE("Distance"), ARMOR("Armor"), THREAT("Threat"), HEALTH("Health");
        public final String name;

        TargetMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum TargetArea {
        CENTER("Center"), CLOSEST("Closest");
        public final String name;

        TargetArea(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<AimMode> mode = new ModeProperty<>("Mode", AimMode.SIMPLE);
    private final Property<Boolean> players = new Property<>("Players", true);
    private final Property<Boolean> mobs = new Property<>("Mobs", false);
    private final Property<Boolean> invisibles = new Property<>("Invisibles", false);
    private final Property<Boolean> ignoreTeams = new Property<>("Ignore Teams", true);
    private final Property<Boolean> requireMouseDown = new Property<>("Require Mouse Down", true);
    private final Property<Boolean> aimVertically = new Property<>("Aim Vertically", false);
    private final Property<Boolean> strafeIncrease = new Property<>("Strafe Increase", false);
    private final Property<Boolean> checkBlockBreak = new Property<>("Check Block Break", false);
    private final Property<Boolean> swordsOnly = new Property<>("Swords Only", false);
    private final NumberProperty verticalSpeed = new NumberProperty("Vertical Speed", 5.0, 1.0, 10.0, 0.1, aimVertically::getValue);
    private final NumberProperty horizontalSpeed = new NumberProperty("Horizontal Speed", 5.0, 1.0, 10.0, 0.1);
    private final Property<Boolean> reduceOnTarget = new Property<>("Reduce On Target", false);
    private final NumberProperty reduceFactor = new NumberProperty("Reduce Factor", 0.5, 0, 1, 0.01, reduceOnTarget::getValue);
    private final NumberProperty maxAngle = new NumberProperty("Max Angle", 180.0, 1.0, 360.0, 1.0);
    private final NumberProperty distance = new NumberProperty("Distance", 5.0, 1.0, 8.0, 0.1);
    private final ModeProperty<TargetMode> targetMode = new ModeProperty<>("Target Mode", TargetMode.YAW);
    private final ModeProperty<TargetArea> targetArea = new ModeProperty<>("Target Area", TargetArea.CENTER);

    private final Random random = new Random();
    private final Random sharedRandom = new Random();
    private EntityLivingBase target;
    private AimMode lastMode = null;
    private int blockBreakCooldown = 0;
    private int retargetCounter = 0;
    private long lastSimpleNanos = 0L;

    private int randomOffsetX, randomOffsetY, driftX, driftY, swapTickCounter, sampleCounter;
    private float hMouseAcc, vMouseAcc, hVel, vVel, hVelBuf, vVelBuf, yawBoost, pitchBoost;
    private double driftTimer, prevTargetX = Double.NaN, prevTargetZ = Double.NaN, targetX, targetY, targetZ, lastAngleDiff;
    private boolean prevOnLeft, prevAbove;

    private float pitchVelocity, lastYawDiff, pendingPitch, pendingYaw, airFactor, lastPlayerPitch, pitchBias, overshoot;
    private float driftPos, driftVelocity, aimStrength, yawVelocity, yawBias, lastTargetPitch, driftTarget, driftNoise;
    private float lastYawSign, lastPitchSign, yawFlickTicks, pitchFlickTicks, yawAccel, pitchAccel, lastTargetYaw, lastPlayerYaw;
    private float lastPitchDiff, adVerticalVelocity;
    private double predictedX, predictedY, predictedZ, leadX, leadY, leadZ, aimX, aimY, aimZ, lastGroundEyeY, lastEyeY;
    private long lastFrameNanos, driftNextNanos, noiseStartNanos;
    private boolean aimPointInitialized, predictionInitialized, yawSnapped, pitchSnapped, initialized;

    private boolean reduceThisTick;

    @Override
    public void onEnable() {
        fullReset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        fullReset();
        super.onDisable();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        fullReset();
    }

    private void fullReset() {
        target = null;
        resetSimple();
        resetAdaptive();
        lastMode = null;
        lastSimpleNanos = 0L;
        retargetCounter = 0;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null) return;

        int steps = 1;
        if (mode.getValue() == AimMode.SIMPLE) {
            long now = System.nanoTime();
            if (lastSimpleNanos == 0L) {
                lastSimpleNanos = now;
            } else {
                long elapsed = now - lastSimpleNanos;
                steps = (int) Math.max(1L, Math.min(MAX_SIMPLE_STEPS, elapsed / SIMPLE_STEP_NANOS));
                if (elapsed > MAX_SIMPLE_STEPS * SIMPLE_STEP_NANOS) {
                    lastSimpleNanos = now;
                } else {
                    lastSimpleNanos += steps * SIMPLE_STEP_NANOS;
                }
            }
        } else {
            lastSimpleNanos = 0L;
        }

        for (int i = 0; i < steps; i++) {
            tick();
            if (target == null) break;
        }

        if (target == null) return;

        float[] delta = mode.getValue() == AimMode.SIMPLE ? drainSimple() : drainAdaptive();
        if (delta[0] != 0.0f || delta[1] != 0.0f) {
            mc.thePlayer.setAngles(delta[0], delta[1]);
        }
    }

    private float[] drainSimple() {
        hMouseAcc += driftX;
        vMouseAcc += driftY;
        int hs = (int) hMouseAcc;
        int vs = (int) vMouseAcc;
        hMouseAcc -= hs;
        vMouseAcc -= vs;
        driftX = 0;
        driftY = 0;
        float scale = sensitivityScale();
        return new float[]{hs * scale, vs * scale};
    }

    private float[] drainAdaptive() {
        EntityLivingBase t = target;
        boolean yawSnapEnabled = getHSpeed() > 20.0f;
        boolean pitchSnapEnabled = aimVertically.getValue() && getVSpeed() > 20.0f;
        boolean snapYaw = yawSnapEnabled && yawSnapped;
        boolean snapPitch = pitchSnapEnabled && pitchSnapped;
        int yawSteps = snapYaw ? Math.round(pendingYaw) : (int) pendingYaw;
        int pitchSteps = snapPitch ? Math.round(pendingPitch) : (int) pendingPitch;
        float remainingYaw = snapYaw ? 0.0f : pendingYaw - yawSteps;
        float remainingPitch = snapPitch ? 0.0f : pendingPitch - pitchSteps;
        float scale = sensitivityScale();
        float dx = yawSteps * scale;
        float dy = -(float) pitchSteps * scale;
        if ((snapYaw || snapPitch) && t != null) {
            EntityPlayerSP player = mc.thePlayer;
            double[] p = resolveAimPoint(player, t, false);
            double ddx = p[0] - player.posX;
            double ddz = p[2] - player.posZ;
            double ddy = p[1] - (player.posY + player.getEyeHeight());
            double flat = Math.sqrt(ddx * ddx + ddz * ddz);
            float ty = (float) (Math.toDegrees(Math.atan2(ddz, ddx)) - 90.0);
            float tp = (float) (-Math.toDegrees(Math.atan2(ddy, Math.max(flat, 1.0E-4))));
            if (snapYaw) dx = MathHelper.wrapAngleTo180_float(ty - player.rotationYaw) / 0.15f;
            if (snapPitch) dy = -MathHelper.wrapAngleTo180_float(tp - player.rotationPitch) / 0.15f;
        }
        pendingYaw = remainingYaw;
        pendingPitch = remainingPitch;
        return new float[]{dx, dy};
    }

    private void tick() {
        AimMode m = mode.getValue();
        if (m != lastMode) {
            lastMode = m;
            clearState();
        }

        if (!canAim()) {
            clearState();
            return;
        }

        EntityLivingBase cur = target;
        if (cur != null && (cur.isDead || cur.getHealth() <= 0.0f)) {
            clearState();
            cur = null;
        }

        boolean mouse = requireMouseDown.getValue();
        boolean down = mc.gameSettings.keyBindAttack.isKeyDown();
        if (mouse && !down) {
            clearState();
            return;
        }

        if (cur != null && mc.thePlayer.getDistanceToEntity(cur) > distance.getValue().doubleValue()) {
            clearState();
            cur = null;
        }

        boolean need = mouse ? (down && cur == null) : (++retargetCounter > 700 || cur == null || !isValidTarget(cur));
        if (need) {
            EntityLivingBase candidate = findBestTarget();
            if (cur == null || !cur.equals(candidate)) {
                resetCurrentMode();
            }
            target = candidate;
            cur = candidate;
            retargetCounter = 0;
        }

        if (cur == null || mc.currentScreen != null) {
            clearState();
            return;
        }

        if (m == AimMode.SIMPLE) {
            updateVelocityBuffers();
            applySimple(cur);
        } else {
            updateAdaptive(cur);
        }
    }

    private void clearState() {
        target = null;
        resetCurrentMode();
    }

    private void resetCurrentMode() {
        if (mode.getValue() == AimMode.SIMPLE) resetSimple();
        else resetAdaptive();
    }

    private boolean canAim() {
        if (mc.thePlayer == null || mc.playerController == null) return false;
        if (checkBlockBreak.getValue()) {
            MovingObjectPosition over = mc.objectMouseOver;
            if (over != null && over.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                blockBreakCooldown = 250;
                return false;
            }
            if (blockBreakCooldown > 0) blockBreakCooldown--;
            if (blockBreakCooldown > 0) return false;
        }
        return passesItemFilter();
    }

    private boolean passesItemFilter() {
        if (!swordsOnly.getValue()) return true;
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    private boolean isValidTarget(EntityLivingBase e) {
        EntityPlayerSP p = mc.thePlayer;
        if (e == null || e == p || e.isDead || e.getHealth() <= 0.0f || e instanceof EntityArmorStand) return false;
        if (p.getDistanceToEntity(e) >= distance.getValue().floatValue()) return false;
        if (yawAngleTo(e) > maxAngle.getValue().floatValue() / 2.0f) return false;
        if (e == p.ridingEntity) return false;
        if (e.isInvisible() && !invisibles.getValue()) return false;
        if (e instanceof EntityPlayer) {
            if (!players.getValue()) return false;
            if (ignoreTeams.getValue() && e.isOnSameTeam(p)) return false;
        } else if (!mobs.getValue() || !(e instanceof EntityLiving)) {
            return false;
        }
        return true;
    }

    private EntityLivingBase findBestTarget() {
        List<EntityLivingBase> list = new ArrayList<>();
        for (Entity o : new ArrayList<>(mc.theWorld.loadedEntityList)) {
            if (o instanceof EntityLivingBase && isValidTarget((EntityLivingBase) o)) {
                list.add((EntityLivingBase) o);
            }
        }
        if (list.isEmpty()) return null;
        list.sort(Comparator.comparingDouble(this::sortKey));
        return list.get(0);
    }

    private double sortKey(EntityLivingBase e) {
        double dist = mc.thePlayer.getDistanceToEntity(e);
        switch (targetMode.getValue()) {
            case YAW:
                return yawAngleTo(e);
            case DISTANCE:
                return dist;
            case THREAT:
                return e instanceof EntityPlayer ? -e.getTotalArmorValue() : 1000.0 + dist;
            case ARMOR:
                return e instanceof EntityPlayer ? e.getTotalArmorValue() : 1000.0 + dist;
            default:
                return e.getHealth();
        }
    }

    private float sensitivityScale() {
        float s = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f;
        return s * s * s * 8.0f;
    }

    private float yawTo(double fromX, double fromZ, double toX, double toZ) {
        return (float) (Math.atan2(toZ - fromZ, toX - fromX) * 180.0 / Math.PI) - 90.0f;
    }

    private float yawAngleTo(EntityLivingBase e) {
        EntityPlayerSP p = mc.thePlayer;
        return Math.abs(MathHelper.wrapAngleTo180_float(yawTo(p.posX, p.posZ, e.posX, e.posZ) - p.rotationYaw));
    }

    private int verticalDiff(EntityPlayerSP p, double x, double y, double z) {
        double dx = x - p.posX;
        double dz = z - p.posZ;
        double dy = y - (p.posY + p.getEyeHeight());
        double flat = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(flat, 1.0E-4))));
        return (int) (pitch - p.rotationPitch);
    }

    private double[] closestPoint(EntityPlayerSP p, EntityLivingBase t) {
        Vec3 eyes = p.getPositionEyes(1.0f);
        AxisAlignedBB bb = t.getEntityBoundingBox();
        return new double[]{
                MathHelper.clamp_double(eyes.xCoord, bb.minX, bb.maxX),
                MathHelper.clamp_double(eyes.yCoord, bb.minY, bb.maxY),
                MathHelper.clamp_double(eyes.zCoord, bb.minZ, bb.maxZ)
        };
    }

    private double randomRange(double lo, double hi) {
        return lo + sharedRandom.nextDouble() * (hi - lo);
    }

    private void resetSimple() {
        hMouseAcc = 0.0f;
        vMouseAcc = 0.0f;
        randomOffsetX = 0;
        randomOffsetY = 0;
        driftX = 0;
        driftY = 0;
        prevTargetX = Double.NaN;
        prevTargetZ = Double.NaN;
    }

    private void updateVelocityBuffers() {
        swapTickCounter++;
        if (swapTickCounter > 10) {
            vVelBuf = vVel;
            hVel = hVelBuf;
            hVelBuf = 0.0f;
            vVel = 0.0f;
            swapTickCounter = 0;
        }
    }

    private void updateDrift() {
        driftTimer += 1.0;
        if (driftTimer >= 250 + random.nextInt(50)) {
            driftTimer = -100 + random.nextInt(50);
            randomOffsetX = -1 + random.nextInt(3);
            randomOffsetY = -1 + random.nextInt(3);
        }
        int hStep = randomOffsetX;
        int vStep = randomOffsetY;
        if (random.nextInt(10) < 2) hStep = 0;
        if (random.nextInt(10) < 2) vStep = 0;
        if (driftTimer < 0.0) {
            hStep = 0;
            vStep = 0;
        }
        if (random.nextInt(20) == 1) {
            driftX += hStep;
            driftY += vStep;
        }
        if ((hMouseAcc > 0.0f && driftX < 0) || (hMouseAcc < 0.0f && driftX > 0)) {
            driftX = 0;
        }
    }

    private void queueHorizontal(EntityLivingBase t, float adjustment) {
        if (adjustment != 0.0f) {
            adjustment *= 5.0f;
            float speed = getHSpeed();
            float angleDifference = yawAngleTo(t);
            if (angleDifference <= 10.0f) pitchBoost = speed;
            if (pitchBoost > 0.0f) {
                speed -= pitchBoost / 3.0f;
                pitchBoost -= angleDifference / 200.0f;
            }
            hMouseAcc += speed * adjustment;
        } else {
            hMouseAcc = 0.0f;
        }
    }

    private void queueVertical(float adjustment, float angleDifference) {
        if (adjustment != 0.0f) {
            adjustment *= 5.0f;
            float speed = getVSpeed();
            float abs = Math.abs(angleDifference);
            if (abs <= 10.0f) yawBoost = speed;
            if (yawBoost > 0.0f) {
                speed -= yawBoost / 3.0f;
                yawBoost -= abs / 200.0f;
            }
            vMouseAcc += speed * adjustment;
        } else {
            vMouseAcc = 0.0f;
        }
    }

    private void applySimple(EntityLivingBase t) {
        EntityPlayerSP player = mc.thePlayer;
        updateDrift();
        targetX = t.posX;
        targetY = t.posY + t.height * 0.65;
        targetZ = t.posZ;
        if (targetArea.getValue() == TargetArea.CLOSEST) {
            double[] closest = closestPoint(player, t);
            double mx = t.posX - t.lastTickPosX;
            double mz = t.posZ - t.lastTickPosZ;
            double px = closest[0] - mx;
            double pz = closest[2] - mz;
            float partial = mc.timer.renderPartialTicks;
            targetX = px + (closest[0] - px) * partial;
            targetZ = pz + (closest[2] - pz) * partial;
        }

        double motionX = Double.isNaN(prevTargetX) ? 0.0 : targetX - prevTargetX;
        double motionZ = Double.isNaN(prevTargetZ) ? 0.0 : targetZ - prevTargetZ;
        prevTargetX = targetX;
        prevTargetZ = targetZ;

        float viewYaw = player.rotationYaw;
        double predX = targetX + motionX * 1.7;
        double predZ = targetZ + motionZ * 1.7;
        float yawDiff = MathHelper.wrapAngleTo180_float(yawTo(player.posX, player.posZ, predX, predZ) - viewYaw);
        double hDiff = Math.abs(yawDiff);
        boolean onLeft = yawDiff < 0.0f;
        int vDiff = verticalDiff(player, targetX, targetY, targetZ);
        boolean above = vDiff < 0;
        int vDead = Math.abs(vDiff) - 10;

        float hForce = 1.0f;
        float vForce = 1.0f;
        hForce = (float) (hForce + randomRange(0.0, 2.0));
        hForce = (float) (hForce + hDiff / 50.0);
        vForce = (float) (vForce + randomRange(0.0, 2.0));
        vForce += Math.abs(vDead) / 50.0f;
        if (Math.abs(hDiff - lastAngleDiff) > 6.0) {
            hForce = (float) (hForce + hDiff / 35.0);
        }
        double proximity = Math.max(0.0, (9.0f - player.getDistanceToEntity(t)) / 2.5f - 2.0f);
        hForce = (float) (hForce + proximity);

        float strafe = player.movementInput.moveStrafe;
        boolean strafingAway = onLeft ? strafe < 0.0f : strafe > 0.0f;
        if (strafeIncrease.getValue() && strafingAway) {
            hForce = (float) (hForce * 1.6);
        }
        if (player.getDistanceToEntity(t) < 0.5f) {
            hForce /= 5.0f;
        }

        float hAccel = hForce / 90.0f * (onLeft ? -1.0f : 1.0f);
        float vAccel = vForce / 90.0f * (above ? 1.0f : -1.0f);
        if (hDiff < 5.0) {
            hAccel = 0.0f;
            hVel *= 0.7f;
            boolean strafingToward = onLeft ? strafe > 0.0f : strafe < 0.0f;
            if (strafingToward) hVel *= 0.5f;
        }
        if (onLeft != prevOnLeft) {
            hVel = -hVel;
            hVelBuf = -hVelBuf;
            hMouseAcc = 0.0f;
        }
        if (above != prevAbove) {
            vVelBuf = -vVelBuf;
            vVel = -vVel;
            vMouseAcc = 0.0f;
        }
        if (vDead < 5) {
            vAccel = 0.0f;
            vVelBuf *= 0.7f;
        }

        hVelBuf += hAccel;
        vVel += vAccel;
        float smoothedH = hVel;
        float smoothedV = vVelBuf;
        if (Math.abs(smoothedH) > 10.0f) {
            hVelBuf = 0.0f;
            hVel = 0.0f;
            return;
        }

        float hAdjust = smoothedH * 0.15f;
        if (hDiff <= 9.0) {
            hAdjust = (float) (hAdjust / (10.0 - hDiff));
        }
        if (Float.isNaN(hAdjust)) {
            hVelBuf = 0.0f;
            hVel = 0.0f;
            return;
        }
        queueHorizontal(t, hAdjust);

        if (aimVertically.getValue()) {
            float vAdjust = (float) (smoothedV * 0.15);
            if (Float.isNaN(vAdjust)) {
                vVel = 0.0f;
                vVelBuf = 0.0f;
                return;
            }
            queueVertical(vAdjust, vDiff);
        }
        prevAbove = above;
        prevOnLeft = onLeft;
        sampleCounter++;
        if (sampleCounter > 10) {
            lastAngleDiff = hDiff;
            sampleCounter = 0;
        }
    }

    private void resetAdaptive() {
        pendingYaw = 0.0f;
        pendingPitch = 0.0f;
        yawSnapped = false;
        pitchSnapped = false;
        yawBias = 0.0f;
        pitchBias = 0.0f;
        yawVelocity = 0.0f;
        pitchVelocity = 0.0f;
        lastYawDiff = 0.0f;
        lastPitchDiff = 0.0f;
        lastTargetYaw = 0.0f;
        lastTargetPitch = 0.0f;
        yawAccel = 0.0f;
        pitchAccel = 0.0f;
        lastPlayerYaw = 0.0f;
        lastPlayerPitch = 0.0f;
        initialized = false;
        lastFrameNanos = 0L;
        aimPointInitialized = false;
        aimX = 0.0;
        aimY = 0.0;
        aimZ = 0.0;
        leadX = 0.0;
        leadY = 0.0;
        leadZ = 0.0;
        aimStrength = 0.0f;
        yawFlickTicks = 0.0f;
        pitchFlickTicks = 0.0f;
        lastYawSign = 0.0f;
        lastPitchSign = 0.0f;
        overshoot = 0.0f;
        lastEyeY = 0.0;
        lastGroundEyeY = 0.0;
        adVerticalVelocity = 0.0f;
        airFactor = 0.0f;
        predictionInitialized = false;
        predictedX = 0.0;
        predictedY = 0.0;
        predictedZ = 0.0;
        noiseStartNanos = System.nanoTime();
        driftPos = 0.0f;
        driftVelocity = 0.0f;
        driftTarget = 0.0f;
        driftNoise = 0.0f;
        driftNextNanos = 0L;
    }

    private double[] resolveAimPoint(EntityPlayerSP player, EntityLivingBase t, boolean smooth) {
        double eyeY = player.posY + player.getEyeHeight();
        AxisAlignedBB bb = t.getEntityBoundingBox();
        double minY = bb.minY;
        double maxY = bb.maxY;
        double height = maxY - minY;
        double centerY = minY + height * 0.65;
        double airborne = Math.min(0.85, airFactor);
        double eyeOffset = 0.1 + airborne;
        double eyeAligned = Math.max(minY + 0.01, Math.min(maxY - 0.01, eyeY - eyeOffset));
        double centerBlend = Math.max(0.0, Math.min(1.0, airborne / 0.55));
        double ty = eyeAligned + (centerY - eyeAligned) * centerBlend;
        ty = Math.max(minY + 0.01, Math.min(maxY - 0.01, ty));
        if (targetArea.getValue() == TargetArea.CLOSEST) {
            double[] closest = closestPoint(player, t);
            double cy = Math.max(minY + 0.01, Math.min(maxY - 0.01, closest[1] - eyeOffset));
            cy += (centerY - cy) * centerBlend;
            cy = Math.max(minY + 0.01, Math.min(maxY - 0.01, cy));
            double rx = closest[0];
            double ry = cy;
            double rz = closest[2];
            if (smooth) {
                if (predictionInitialized) {
                    rx = predictedX + (rx - predictedX) * 0.35;
                    ry = predictedY + (ry - predictedY) * 0.35;
                    rz = predictedZ + (rz - predictedZ) * 0.35;
                }
                predictedX = rx;
                predictedY = ry;
                predictedZ = rz;
                predictionInitialized = true;
            }
            return new double[]{rx, ry, rz};
        }
        return new double[]{t.posX, ty, t.posZ};
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float n = Math.max(0.0f, Math.min(1.0f, (value - edge0) / (edge1 - edge0)));
        return n * n * (3.0f - 2.0f * n);
    }

    private static float lerp(float factor, float start, float end) {
        return start + factor * (end - start);
    }

    private static boolean shouldSnap(boolean enabled, boolean snapped, float angleDifference) {
        if (!enabled) return false;
        return snapped ? angleDifference <= SNAP_EXIT : angleDifference <= SNAP_ENTER;
    }

    private float computePitchDrift(float playerPitch, float deltaTime, long nowNanos) {
        float strength = 0.65f + 0.35f * aimStrength;
        if (driftNextNanos == 0L || nowNanos >= driftNextNanos) {
            float amplitude = lerp(strength, 0.05f, 0.15f);
            float correction = playerPitch > 22.0f ? -0.18f : (playerPitch < -22.0f ? 0.18f : 0.0f);
            driftTarget = (random.nextFloat() * 2.0f - 1.0f) * amplitude + correction;
            driftTarget = Math.max(-0.5f, Math.min(0.5f, driftTarget));
            long intervalMillis = 300L + random.nextInt(420);
            driftNextNanos = nowNanos + intervalMillis * 1000000L;
        }
        float spring = lerp(strength, 2.0f, 5.0f);
        float damping = (float) Math.pow(0.04f, deltaTime);
        driftVelocity += (driftTarget - driftPos) * spring * deltaTime;
        driftVelocity *= damping;
        driftPos += driftVelocity * deltaTime;
        float limit = lerp(strength, 0.45f, 0.8f);
        if (driftPos > limit) {
            driftPos = limit;
            driftVelocity = Math.min(0.0f, driftVelocity);
        } else if (driftPos < -limit) {
            driftPos = -limit;
            driftVelocity = Math.max(0.0f, driftVelocity);
        }
        driftNoise += (random.nextFloat() * 2.0f - 1.0f - driftNoise) * Math.max(0.02f, Math.min(0.18f, deltaTime * 5.0f));
        float elapsed = (float) (nowNanos - noiseStartNanos) / 1.0E9f;
        float noise = (float) (Math.sin(elapsed * 8.7 + 0.4) * 0.35f + Math.sin(elapsed * 13.1 + 2.2) * 0.22f + Math.sin(elapsed * 19.6 + 1.1) * 0.12f + driftNoise * 0.3f);
        float restoring = -driftPos * lerp(strength, 1.4f, 3.0f);
        float output = driftVelocity * 0.25f + restoring + noise * lerp(strength, 0.35f, 0.9f);
        float pitchLimitScale = 1.0f - 0.85f * smoothStep(72.0f, 88.0f, Math.abs(playerPitch));
        float outputLimit = lerp(strength, 0.25f, 0.55f) * pitchLimitScale;
        return Math.max(-outputLimit, Math.min(outputLimit, output));
    }

    private void updateAdaptive(EntityLivingBase t) {
        long nowNanos = System.nanoTime();
        if (initialized && lastFrameNanos != 0L && nowNanos - lastFrameNanos < MIN_ADAPTIVE_NANOS) return;

        boolean yawSnapEnabled = getHSpeed() > 20.0f;
        boolean pitchSnapEnabled = aimVertically.getValue() && getVSpeed() > 20.0f;
        boolean snapYaw = yawSnapEnabled && yawSnapped;
        boolean snapPitch = pitchSnapEnabled && pitchSnapped;
        boolean closest = targetArea.getValue() == TargetArea.CLOSEST;
        EntityPlayerSP player = mc.thePlayer;

        float deltaTime;
        if (!initialized || lastFrameNanos == 0L) {
            deltaTime = 0.016666668f;
        } else {
            deltaTime = (float) (nowNanos - lastFrameNanos) / 1.0E9f;
            deltaTime = Math.max(0.008333334f, Math.min(0.12f, deltaTime));
        }
        lastFrameNanos = nowNanos;

        double[] resolved = resolveAimPoint(player, t, true);
        double resolvedX = resolved[0];
        double resolvedY = resolved[1];
        double resolvedZ = resolved[2];
        double targetMotionX = t.posX - t.lastTickPosX;
        double targetMotionY = t.posY - t.lastTickPosY;
        double targetMotionZ = t.posZ - t.lastTickPosZ;
        double relMotionX = targetMotionX - player.motionX;
        double relMotionY = targetMotionY - player.motionY;
        double relMotionZ = targetMotionZ - player.motionZ;
        double targetLeadX = targetMotionX + relMotionX * 0.08;
        double targetLeadY = targetMotionY + relMotionY * 0.1;
        double targetLeadZ = targetMotionZ + relMotionZ * 0.08;
        if (!aimPointInitialized) {
            aimX = resolvedX;
            aimY = resolvedY;
            aimZ = resolvedZ;
            leadX = targetLeadX;
            leadY = targetLeadY;
            leadZ = targetLeadZ;
            aimPointInitialized = true;
        }
        double targetHSpeed = Math.sqrt(targetMotionX * targetMotionX + targetMotionZ * targetMotionZ);
        double relHSpeed = Math.sqrt(relMotionX * relMotionX + relMotionZ * relMotionZ);
        double targetDistance = player.getDistanceToEntity(t);
        double baseSmoothing = closest ? 0.3 : 0.28;
        double aimSmoothing = Math.max(0.08, Math.min(0.75, baseSmoothing + Math.min(0.35, relHSpeed * 0.5)));
        double leadMotionScale = targetHSpeed + relHSpeed * 0.25;
        double leadSmoothing = Math.max(0.12, Math.min(0.68, 0.18 + Math.min(0.42, leadMotionScale * 0.72)));
        double frameScale = Math.max(0.45, Math.min(2.4, deltaTime * 60.0));
        double aimBlend = 1.0 - Math.pow(1.0 - aimSmoothing, frameScale);
        double leadBlend = 1.0 - Math.pow(1.0 - leadSmoothing, frameScale);
        aimX += (resolvedX - aimX) * aimBlend;
        aimY += (resolvedY - aimY) * aimBlend;
        aimZ += (resolvedZ - aimZ) * aimBlend;
        leadX += (targetLeadX - leadX) * leadBlend;
        leadY += (targetLeadY - leadY) * leadBlend;
        leadZ += (targetLeadZ - leadZ) * leadBlend;

        double lagX = resolvedX - aimX;
        double lagZ = resolvedZ - aimZ;
        double hLag = Math.sqrt(lagX * lagX + lagZ * lagZ);
        double maxLag = 0.3 + targetHSpeed * 1.3 + relHSpeed * 0.4;
        if (hLag > maxLag) {
            aimX = resolvedX;
            aimY = resolvedY;
            aimZ = resolvedZ;
            leadX = targetLeadX;
            leadY = targetLeadY;
            leadZ = targetLeadZ;
        }

        double leadAmount = 0.35 + targetDistance * 0.045 + targetHSpeed * 0.95 + Math.min(0.18, relHSpeed * 0.12);
        leadAmount = Math.max(0.1, Math.min(1.05, leadAmount));
        double distanceLeadScale = Math.max(0.0, Math.min(1.0, (targetDistance - 0.8) / 2.5));
        leadAmount *= 0.3 + 0.7 * distanceLeadScale;
        leadAmount = Math.max(0.05, leadAmount);
        double playerX = player.posX;
        double playerZ = player.posZ;
        double playerEyeY = player.posY + player.getEyeHeight();
        float measuredVV = 0.0f;
        if (initialized && lastEyeY != 0.0) {
            measuredVV = (float) ((playerEyeY - lastEyeY) / deltaTime);
        }
        adVerticalVelocity += (measuredVV - adVerticalVelocity) * 0.65f;
        lastEyeY = playerEyeY;
        if (player.onGround) {
            lastGroundEyeY = playerEyeY;
            airFactor *= 0.35f;
        } else {
            if (lastGroundEyeY == 0.0) lastGroundEyeY = playerEyeY;
            double airborneHeight = Math.max(0.0, playerEyeY - lastGroundEyeY);
            float velocityAir = (float) Math.min(0.7, Math.max(0.0, adVerticalVelocity) * 0.34);
            float heightAir = (float) Math.min(0.82, airborneHeight * 0.58);
            airFactor = Math.max(airFactor * 0.92f, Math.max(velocityAir, heightAir));
        }

        double adjustedAimX = aimX;
        double adjustedAimZ = aimZ;
        double verticalAimDelta = aimY - playerEyeY;
        double verticalOffsetScale = Math.max(0.0, Math.min(1.0, Math.abs(verticalAimDelta) / 0.7));
        double verticalLeadAmount = Math.min(leadAmount, 0.7 + targetDistance * 0.04);
        double verticalLeadScale = 0.28 + 0.52 * verticalOffsetScale;
        double verticalLead = leadY * verticalLeadAmount * verticalLeadScale;
        double verticalLeadLimit = 0.16 + targetDistance * 0.055;
        verticalLead = Math.max(-verticalLeadLimit, Math.min(verticalLeadLimit, verticalLead));
        double adjustedAimY = aimY + verticalLead;
        if (snapYaw) {
            adjustedAimX = resolvedX;
            adjustedAimZ = resolvedZ;
        }
        if (snapPitch) {
            adjustedAimY = resolvedY;
        }
        double aimDeltaX = adjustedAimX - playerX;
        double aimDeltaZ = adjustedAimZ - playerZ;
        double aimDeltaY = adjustedAimY - playerEyeY;
        double hAimDistance = Math.sqrt(aimDeltaX * aimDeltaX + aimDeltaZ * aimDeltaZ);
        float targetYaw = (float) (Math.toDegrees(Math.atan2(aimDeltaZ, aimDeltaX)) - 90.0);
        float targetPitch = (float) (-Math.toDegrees(Math.atan2(aimDeltaY, Math.max(hAimDistance, 1.0E-4))));
        float viewYaw = player.rotationYaw;
        if (initialized) {
            float yawTargetChange = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - lastTargetYaw));
            float pitchTargetChange = Math.abs(MathHelper.wrapAngleTo180_float(targetPitch - lastTargetPitch));
            float abruptThreshold = closest ? 20.0f : 12.0f;
            float maxTargetChange = Math.max(yawTargetChange, pitchTargetChange);
            float abruptFactor = Math.max(0.0f, Math.min(1.0f, (maxTargetChange - abruptThreshold) / 50.0f));
            overshoot = Math.max(overshoot, abruptFactor);
            if (abruptFactor > 0.3f) {
                aimStrength *= 0.3f;
                aimX = resolvedX;
                aimY = resolvedY;
                aimZ = resolvedZ;
            }
            float yawRateScale = 1.0f + Math.min(2.0f, yawTargetChange / 60.0f);
            float pitchRateScale = 1.0f + Math.min(2.0f, pitchTargetChange / 60.0f);
            float maxYawRate = (120.0f + (float) (relHSpeed * 700.0)) * yawRateScale;
            float maxPitchRate = (95.0f + (float) (Math.abs(leadY) * 550.0)) * pitchRateScale;
            float viewYawDifference = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - viewYaw));
            float wideTurn = smoothStep(10.0f, 35.0f, viewYawDifference);
            maxYawRate *= 1.0f + wideTurn * 1.5f;
            maxYawRate = Math.max(90.0f, Math.min(1080.0f, maxYawRate));
            maxPitchRate = Math.max(70.0f, Math.min(500.0f, maxPitchRate));
            float yawStep = MathHelper.wrapAngleTo180_float(targetYaw - lastTargetYaw);
            float pitchStep = MathHelper.wrapAngleTo180_float(targetPitch - lastTargetPitch);
            if (!snapYaw) {
                yawStep = Math.max(-maxYawRate * deltaTime, Math.min(maxYawRate * deltaTime, yawStep));
            }
            if (!snapPitch) {
                pitchStep = Math.max(-maxPitchRate * deltaTime, Math.min(maxPitchRate * deltaTime, pitchStep));
            }
            targetYaw = lastTargetYaw + yawStep;
            targetPitch = lastTargetPitch + pitchStep;
        }
        float overshootDecay = (float) Math.pow(0.02, deltaTime);
        overshoot *= overshootDecay;
        if (overshoot < 0.01f) overshoot = 0.0f;
        float overshootMultiplier = 1.0f + 3.0f * overshoot;
        float playerYaw = viewYaw;
        float playerPitch = player.rotationPitch;
        float yawError = MathHelper.wrapAngleTo180_float(targetYaw - playerYaw);
        float pitchError = MathHelper.wrapAngleTo180_float(targetPitch - playerPitch);
        if (!aimVertically.getValue()) pitchError = 0.0f;
        float absYawError = Math.abs(yawError);
        float absPitchError = Math.abs(pitchError);
        float combinedError = (float) Math.sqrt(absYawError * absYawError + absPitchError * absPitchError);
        yawSnapped = shouldSnap(yawSnapEnabled, yawSnapped, absYawError);
        pitchSnapped = shouldSnap(pitchSnapEnabled, pitchSnapped, absPitchError);
        snapYaw = yawSnapEnabled && yawSnapped;
        snapPitch = pitchSnapEnabled && pitchSnapped;
        float desiredStrength = 1.0f - smoothStep(1.5f, 8.0f, combinedError);
        if (initialized) {
            float closingSpeed = -(absYawError - Math.abs(lastYawDiff)) / deltaTime;
            float closingAdjustment = Math.max(-0.3f, Math.min(0.3f, closingSpeed / 20.0f));
            desiredStrength += closingAdjustment;
            desiredStrength = Math.max(0.0f, Math.min(1.0f, desiredStrength));
        }
        float strengthBlend = desiredStrength > aimStrength
                ? Math.max(0.01f, Math.min(0.25f, deltaTime * 3.0f))
                : Math.max(0.05f, Math.min(0.8f, deltaTime * 20.0f));
        aimStrength += (desiredStrength - aimStrength) * strengthBlend;
        float currentStrength = aimStrength;

        float smoothedTargetYawRate = 0.0f;
        float smoothedTargetPitchRate = 0.0f;
        float playerYawRate = 0.0f;
        float playerPitchRate = 0.0f;
        if (initialized) {
            float targetYawRate = MathHelper.wrapAngleTo180_float(targetYaw - lastTargetYaw) / deltaTime;
            float targetPitchRate = MathHelper.wrapAngleTo180_float(targetPitch - lastTargetPitch) / deltaTime;
            playerYawRate = MathHelper.wrapAngleTo180_float(playerYaw - lastPlayerYaw) / deltaTime;
            playerPitchRate = MathHelper.wrapAngleTo180_float(playerPitch - lastPlayerPitch) / deltaTime;
            float accelBlend = Math.max(0.05f, Math.min(0.45f, deltaTime * 12.0f));
            yawAccel += (targetYawRate - yawAccel) * accelBlend;
            pitchAccel += (targetPitchRate - pitchAccel) * accelBlend;
            smoothedTargetYawRate = yawAccel;
            smoothedTargetPitchRate = pitchAccel;
        }
        float hSpeed = getHSpeed() * 0.75f;
        float vSpeed = getVSpeed() * 0.75f;
        float hSpeedFactor = Math.max(0.0f, Math.min(1.0f, (hSpeed - 10.0f) / 90.0f));
        float vSpeedFactor = Math.max(0.0f, Math.min(1.0f, (vSpeed - 10.0f) / 90.0f));
        if (Math.signum(yawError) != Math.signum(lastYawDiff) && Math.abs(yawError) > 0.1f && Math.abs(lastYawDiff) > 0.1f) {
            yawBias *= 0.3f;
        }
        if (Math.signum(pitchError) != Math.signum(lastPitchDiff) && Math.abs(pitchError) > 0.1f && Math.abs(lastPitchDiff) > 0.1f) {
            pitchBias *= 0.3f;
        }
        float squaredStrength = currentStrength * currentStrength;
        yawBias += yawError * deltaTime * squaredStrength * (1.0f - hSpeedFactor);
        pitchBias += pitchError * deltaTime * squaredStrength * (1.0f - vSpeedFactor);
        float biasRetention = 1.0f - (1.0f - currentStrength) * Math.max(0.0f, Math.min(0.5f, deltaTime * 5.0f));
        yawBias *= biasRetention;
        pitchBias *= biasRetention;
        float yawBiasLimit = 15.0f * (1.0f - hSpeedFactor * 0.9f);
        float pitchBiasLimit = 10.0f * (1.0f - vSpeedFactor * 0.9f);
        yawBias = Math.max(-yawBiasLimit, Math.min(yawBiasLimit, yawBias));
        pitchBias = Math.max(-pitchBiasLimit, Math.min(pitchBiasLimit, pitchBias));

        float yawErrorVelocity = initialized ? (yawError - lastYawDiff) / deltaTime : 0.0f;
        float pitchErrorVelocity = initialized ? (pitchError - lastPitchDiff) / deltaTime : 0.0f;
        float velocityBlend = 0.15f;
        yawVelocity = yawVelocity * (1.0f - velocityBlend) + yawErrorVelocity * velocityBlend;
        pitchVelocity = pitchVelocity * (1.0f - velocityBlend) + pitchErrorVelocity * velocityBlend;

        float flickThreshold = closest ? 1.5f : 0.5f;
        float yawSign = Math.signum(yawError);
        yawFlickTicks = yawSign != lastYawSign && Math.abs(yawError) > flickThreshold
                ? Math.min(yawFlickTicks + 1.0f, 8.0f)
                : Math.max(0.0f, yawFlickTicks - deltaTime * 3.0f);
        lastYawSign = yawSign;
        float pitchSign = Math.signum(pitchError);
        pitchFlickTicks = pitchSign != lastPitchSign && Math.abs(pitchError) > flickThreshold
                ? Math.min(pitchFlickTicks + 1.0f, 8.0f)
                : Math.max(0.0f, pitchFlickTicks - deltaTime * 3.0f);
        lastPitchSign = pitchSign;
        float yawFlickFactor = Math.max(0.0f, Math.min(1.0f, yawFlickTicks / 5.0f));
        float pitchFlickFactor = Math.max(0.0f, Math.min(1.0f, pitchFlickTicks / 5.0f));

        float limitedH = Math.min(hSpeed, 10.0f);
        float limitedV = Math.min(vSpeed, 10.0f);
        float hGainInput = (limitedH - 1.0f) / 9.0f;
        float vGainInput = (limitedV - 1.0f) / 9.0f;
        hGainInput *= hGainInput;
        vGainInput *= vGainInput;
        float hGainScale = 0.15f + 0.85f * Math.max(0.0f, Math.min(1.0f, hGainInput));
        float vGainScale = 0.15f + 0.85f * Math.max(0.0f, Math.min(1.0f, vGainInput));
        float yawErrorGain = lerp(currentStrength, 8.0f, 2.5f + limitedH * 0.15f) * hGainScale;
        float pitchErrorGain = lerp(currentStrength, 7.0f, 2.2f + limitedV * 0.13f) * vGainScale;
        float yawBiasGain = lerp(currentStrength, 0.15f, 0.8f + limitedH * 0.04f) * hGainScale;
        float pitchBiasGain = lerp(currentStrength, 0.12f, 0.65f + limitedV * 0.035f) * vGainScale;
        float yawVelocityGain = lerp(currentStrength, 0.08f, 0.25f) * hGainScale;
        float pitchVelocityGain = lerp(currentStrength, 0.06f, 0.2f) * vGainScale;
        float targetYawRateGain = (0.85f + limitedH * 0.015f) * hGainScale;
        float targetPitchRateGain = (0.82f + limitedV * 0.013f) * vGainScale;
        float playerYawCompensation = lerp(currentStrength, 0.1f, 0.3f);
        float playerPitchCompensation = lerp(currentStrength, 0.08f, 0.25f);
        yawErrorGain *= 1.0f - 0.6f * yawFlickFactor;
        yawVelocityGain *= 1.0f + 2.0f * yawFlickFactor;
        pitchErrorGain *= 1.0f - 0.6f * pitchFlickFactor;
        pitchVelocityGain *= 1.0f + 2.0f * pitchFlickFactor;

        float yawRate = yawErrorGain * yawError + yawBiasGain * yawBias + yawVelocityGain * yawVelocity
                + targetYawRateGain * smoothedTargetYawRate - playerYawCompensation * playerYawRate;
        float pitchRate = 0.0f;
        if (aimVertically.getValue()) {
            pitchRate = pitchErrorGain * pitchError + pitchBiasGain * pitchBias + pitchVelocityGain * pitchVelocity
                    + targetPitchRateGain * smoothedTargetPitchRate - playerPitchCompensation * playerPitchRate;
        } else {
            pitchBias = 0.0f;
            pitchVelocity = 0.0f;
        }
        if (!snapPitch) {
            pitchRate += computePitchDrift(playerPitch, deltaTime, nowNanos);
        }
        float strafeMultiplier = 1.0f;
        float strafeInput = player.movementInput.moveStrafe;
        if (strafeIncrease.getValue() && Math.abs(strafeInput) > 0.01f) {
            boolean targetToRight = yawError > 0.0f;
            boolean strafingAway = (targetToRight && strafeInput < 0.0f) || (!targetToRight && strafeInput > 0.0f);
            if (strafingAway) strafeMultiplier = 1.15f;
        }
        yawRate *= strafeMultiplier;

        float wideYawScale = smoothStep(8.0f, 40.0f, absYawError);
        float wideTurnMultiplier = 1.0f + wideYawScale * 2.5f;
        float yawBaseLimit = (22.0f + limitedH * 15.0f) * hGainScale * overshootMultiplier * wideTurnMultiplier;
        float pitchBaseLimit = (18.0f + limitedV * 13.0f) * vGainScale * overshootMultiplier;
        float yawMotionLimit = (Math.abs(smoothedTargetYawRate) * 0.4f + 18.0f) * (0.35f + hGainScale * 0.65f) * wideTurnMultiplier;
        float pitchMotionLimit = (Math.abs(smoothedTargetPitchRate) * 0.38f + 14.0f) * (0.35f + vGainScale * 0.65f);
        float maxYawOutput = Math.min(400.0f * overshootMultiplier * wideTurnMultiplier, Math.max(yawBaseLimit, yawMotionLimit));
        float maxPitchOutput = Math.min(300.0f * overshootMultiplier, Math.max(pitchBaseLimit, pitchMotionLimit));
        yawRate = Math.max(-maxYawOutput, Math.min(maxYawOutput, yawRate));
        pitchRate = Math.max(-maxPitchOutput, Math.min(maxPitchOutput, pitchRate));

        float distanceScale = smoothStep(0.5f, 3.0f, (float) targetDistance);
        float wideTurnBase = lerp(wideYawScale, 0.15f, 0.65f);
        float yawDistanceMultiplier = lerp(currentStrength, wideTurnBase + (1.0f - wideTurnBase) * distanceScale, 0.4f + 0.6f * distanceScale);
        float pitchDistanceMultiplier = lerp(currentStrength, 0.2f + 0.8f * distanceScale, 0.45f + 0.55f * distanceScale);
        yawRate *= yawDistanceMultiplier;
        pitchRate *= pitchDistanceMultiplier;
        float noiseT = (float) (nowNanos - noiseStartNanos) / 1.0E9f;
        float hfYaw = (float) (Math.sin(noiseT * 62.83) * 0.4 + Math.sin(noiseT * 47.12) * 0.25 + Math.sin(noiseT * 78.54) * 0.15);
        float hfPitch = (float) (Math.sin(noiseT * 56.55 + 1.3) * 0.35 + Math.sin(noiseT * 43.98 + 0.7) * 0.2 + Math.sin(noiseT * 72.26 + 2.1) * 0.12);
        float mfYaw = (float) (Math.sin(noiseT * 12.57) * 0.8 + Math.sin(noiseT * 7.85) * 0.5);
        float mfPitch = (float) (Math.sin(noiseT * 10.47 + 0.9) * 0.6 + Math.sin(noiseT * 5.65 + 1.8) * 0.4);
        float lfYaw = (float) (Math.sin(noiseT * 1.26) * 0.3);
        float lfPitch = (float) (Math.sin(noiseT * 0.94 + 0.5) * 0.2);
        float noiseStrength = 0.15f + 0.85f * currentStrength;
        float speedNoiseScale = 0.5f + 0.5f * (1.0f - hGainInput);
        float yawNoise = (hfYaw + mfYaw + lfYaw) * noiseStrength * speedNoiseScale * 1.5f;
        float pitchNoise = (hfPitch + mfPitch + lfPitch) * noiseStrength * speedNoiseScale;
        if (!snapYaw) yawRate += yawNoise;
        if (aimVertically.getValue() && !snapPitch) pitchRate += pitchNoise;

        float yawFrameDelta = yawRate * deltaTime;
        float pitchFrameDelta = pitchRate * deltaTime;
        float mouseAngleUnit = sensitivityScale() * 0.15f;
        if (mouseAngleUnit > 1.0E-5f) {
            float yawUnits = yawFrameDelta / mouseAngleUnit;
            float pitchUnits = pitchFrameDelta / mouseAngleUnit;
            float pitchBlend = aimVertically.getValue() ? vSpeedFactor : 0.0f;
            if (snapYaw) pendingYaw = 0.0f;
            if (snapPitch) pendingPitch = 0.0f;
            if (!snapYaw) {
                if (hSpeedFactor > 0.0f) {
                    float targetYawUnits = yawError / mouseAngleUnit;
                    float yawCorrection = targetYawUnits - pendingYaw;
                    float maxYawCorrection = hSpeed * 2.0f;
                    yawCorrection = Math.max(-maxYawCorrection, Math.min(maxYawCorrection, yawCorrection));
                    pendingYaw += yawUnits * (1.0f - hSpeedFactor) + yawCorrection * hSpeedFactor;
                } else {
                    pendingYaw += yawUnits;
                }
            }
            if (!snapPitch) {
                if (pitchBlend > 0.0f) {
                    float targetPitchUnits = aimVertically.getValue() ? pitchError / mouseAngleUnit : 0.0f;
                    float pitchCorrection = targetPitchUnits - pendingPitch;
                    float maxPitchCorrection = vSpeed * 2.0f;
                    pitchCorrection = Math.max(-maxPitchCorrection, Math.min(maxPitchCorrection, pitchCorrection));
                    pendingPitch += pitchUnits * (1.0f - pitchBlend) + pitchCorrection * pitchBlend;
                } else {
                    pendingPitch += pitchUnits;
                }
            }
        }
        if (!initialized) initialized = true;
        lastYawDiff = yawError;
        lastPitchDiff = pitchError;
        lastTargetYaw = targetYaw;
        lastTargetPitch = targetPitch;
        lastPlayerYaw = playerYaw;
        lastPlayerPitch = playerPitch;
    }

    @EventHook
    public void onTick(ClientTickEvent e) {
        if (reduceOnTarget.getValue()) {
            reduceThisTick = RayCastUtils.rayCast(new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch), distance.getValue()).entityHit == target;
        }
    }

    private float getHSpeed() {
        if (reduceOnTarget.getValue() && reduceThisTick) {
            return horizontalSpeed.getValue().floatValue() * (1 - reduceFactor.getValue().floatValue());
        }
        return horizontalSpeed.getValue().floatValue();
    }

    private float getVSpeed() {
        if (reduceOnTarget.getValue() && reduceThisTick) {
            return verticalSpeed.getValue().floatValue() * (1 - reduceFactor.getValue().floatValue());
        }
        return verticalSpeed.getValue().floatValue();
    }
}
