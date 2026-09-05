package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.player.*;
import ddlc.yuri.utils.player.MoveUtils;
import ddlc.yuri.utils.player.RotationUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector2f;

import java.util.function.Function;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class RotationManager {
    @Setter
    @Getter
    private static boolean active, smoothed;
    public static Vector2f rotations, lastRotations = new Vector2f(0, 0), targetRotations, lastServerRotations;
    private static double rotationSpeed;
    private static MovementFix correctMovement;
    private static Function<Vector2f, Boolean> raycast;
    private static float randomAngle;
    private static final Vector2f offset = new Vector2f(0, 0);
    private static final Vector2f identity = new Vector2f(0, 0);
    private static final Vector2f playerRotations = new Vector2f(0, 0);
    private static final Vector2f serverRotations = new Vector2f(0, 0);

    // added more call methods so it's easier to call. that's what I wanted with completely making my own rotation manager tbh, but I just pasted simp.

    // also quick note you can call any of these, and it sets rotations so it's up to you which is easier to call.

    // yours truly, unlegit :3

    public static void setRotations(final float yaw, final float pitch, final double rotationSpeed, final MovementFix correctMovement, final Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(yaw, pitch), rotationSpeed, correctMovement, raycast);
    }

    public static void setRotations(final float[] rotations, final double rotationSpeed, final MovementFix correctMovement,  final Function<Vector2f, Boolean> raycast) {
        setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, correctMovement, raycast);
    }

    public static void setRotations(final float yaw, final float pitch, final double rotationSpeed, final MovementFix correctMovement) {
        setRotations(new Vector2f(yaw, pitch), rotationSpeed, correctMovement, null);
    }

    public static void setRotations(final float[] rotations, final double rotationSpeed, final MovementFix correctMovement) {
        setRotations(new Vector2f(rotations[0], rotations[1]), rotationSpeed, correctMovement, null);
    }

    public static void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement) {
        setRotations(rotations, rotationSpeed, correctMovement, null);
    }

    public static void setRotations(final Vector2f rotations, final double rotationSpeed, final MovementFix correctMovement, final Function<Vector2f, Boolean> raycast) {
        RotationManager.targetRotations = rotations;
        RotationManager.rotationSpeed = rotationSpeed * 18;
        RotationManager.correctMovement = correctMovement;
        RotationManager.raycast = raycast;
        active = true;

        smooth();
    }

    @EventHook(value = EventPriority.HIGH)
    public void onPreUpdate(PreUpdateEvent event) {
        if (!active || rotations == null || lastRotations == null || targetRotations == null || lastServerRotations == null) {
            identity.set(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            rotations = lastRotations = targetRotations = lastServerRotations = identity;
        }

        if (active) {
            smooth();
        }

        if (correctMovement == MovementFix.BACKWARDS_SPRINT && active) {
            if (Math.abs(rotations.x % 360 - Math.toDegrees(MoveUtils.direction()) % 360) > 45) {
                mc.gameSettings.keyBindSprint.setPressed(false);
                mc.thePlayer.setSprinting(false);
            }
        }
    }

    @EventHook(value = EventPriority.VERY_HIGH)
    public void onMove(MoveEvent event) {
        if (active && correctMovement == MovementFix.NORMAL && rotations != null) {
            final float yaw = rotations.x;
            MoveUtils.fixMovement(event, yaw);
        }
    }


    @EventHook(value = EventPriority.HIGH)
    public void onLook(LookEvent event) {
        if (active && rotations != null) {
            event.setRotation(rotations);
        }
    }

    @EventHook(value = EventPriority.HIGH)
    public void onStrafe(StrafeEvent event) {
        if (active && (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL) && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @EventHook(value = EventPriority.HIGH)
    public void onJump(JumpEvent event) {
        if (active && (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL || correctMovement == MovementFix.BACKWARDS_SPRINT) && rotations != null) {
            event.setYaw(rotations.x);
        }
    }

    @EventHook(value = EventPriority.HIGH)
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;
        if (active && rotations != null) {
            final float yaw = rotations.x;
            final float pitch = rotations.y;

            event.setYaw(yaw);
            event.setPitch(pitch);

            mc.thePlayer.rotationYawHead = yaw;
            mc.thePlayer.renderPitchHead = pitch;

            serverRotations.set(yaw, pitch);
            lastServerRotations = serverRotations;

            if (Math.abs((rotations.x - mc.thePlayer.rotationYaw) % 360) < 1 && Math.abs((rotations.y - mc.thePlayer.rotationPitch)) < 1) {
                active = false;

                this.correctDisabledRotations();
            }

            lastRotations = rotations;
        } else {
            playerRotations.set(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            lastRotations = playerRotations;
        }

        playerRotations.set(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        targetRotations = playerRotations;
        smoothed = false;
    }

    private void correctDisabledRotations() {
        final Vector2f rotations = new Vector2f(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
        final Vector2f fixedRotations = RotationUtils.resetRotation(RotationUtils.applySensitivityPatch(rotations, lastRotations));

        float yawDelta = MathHelper.wrapAngleTo180_float(fixedRotations.x - mc.thePlayer.rotationYaw);
        mc.thePlayer.rotationYaw = mc.thePlayer.rotationYaw + yawDelta;
        mc.thePlayer.rotationPitch = fixedRotations.y;
    }

    public static void smooth() {
        if (!smoothed) {
            float targetYaw = targetRotations.x;
            float targetPitch = targetRotations.y;

            if (raycast != null && (Math.abs(targetYaw - rotations.x) > 5 || Math.abs(targetPitch - rotations.y) > 5)) {
                final Vector2f trueTargetRotations = new Vector2f(targetRotations.getX(), targetRotations.getY());

                double speed = (Math.random() * Math.random() * Math.random()) * 20;
                randomAngle += (float) ((20 + (float) (Math.random() - 0.5) * (Math.random() * Math.random() * Math.random() * 360)) * (mc.thePlayer.ticksExisted / 10 % 2 == 0 ? -1 : 1));

                offset.setX((float) (offset.getX() + -MathHelper.sin((float) Math.toRadians(randomAngle)) * speed));
                offset.setY((float) (offset.getY() + MathHelper.cos((float) Math.toRadians(randomAngle)) * speed));

                targetYaw += offset.getX();
                targetPitch += offset.getY();

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    randomAngle = (float) Math.toDegrees(Math.atan2(trueTargetRotations.getX() - targetYaw, targetPitch - trueTargetRotations.getY())) - 180;

                    targetYaw -= offset.getX();
                    targetPitch -= offset.getY();

                    offset.setX((float) (offset.getX() + -MathHelper.sin((float) Math.toRadians(randomAngle)) * speed));
                    offset.setY((float) (offset.getY() + MathHelper.cos((float) Math.toRadians(randomAngle)) * speed));

                    targetYaw = targetYaw + offset.getX();
                    targetPitch = targetPitch + offset.getY();
                }

                if (!raycast.apply(new Vector2f(targetYaw, targetPitch))) {
                    offset.setX(0);
                    offset.setY(0);

                    targetYaw = (float) (targetRotations.x + Math.random() * 2);
                    targetPitch = (float) (targetRotations.y + Math.random() * 2);
                }
            }

            // Normalize target yaw to prevent 360-degree jumps
            targetYaw = lastRotations.x + MathHelper.wrapAngleTo180_float(targetYaw - lastRotations.x);

            rotations = RotationUtils.smooth(new Vector2f(targetYaw, targetPitch),
                    rotationSpeed + Math.random());

            if (correctMovement == MovementFix.NORMAL || correctMovement == MovementFix.TRADITIONAL) {
                mc.thePlayer.movementYaw = rotations.x;
            }

            mc.thePlayer.velocityYaw = rotations.x;
        }

        smoothed = true;

        /*
         * Updating MouseOver
         */
        mc.entityRenderer.getMouseOver(1);
    }

    @AllArgsConstructor
    public enum MovementFix {
        OFF("Off"),
        NORMAL("Normal"),
        TRADITIONAL("Traditional"),
        BACKWARDS_SPRINT("Backwards Sprint");

        final String name;

        @Override
        public String toString() {
            return name;
        }
    }
}
