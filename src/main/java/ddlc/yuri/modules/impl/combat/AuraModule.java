package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.*;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.player.ScaffoldModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.InventoryUtils;
import ddlc.yuri.utils.player.PlayerUtils;
import ddlc.yuri.utils.player.RayCastUtils;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.security.SecureRandom;
import java.util.Arrays;

@ModuleInfo(label = "Aura", description = "Automatically attacks entities around you", category = ModuleCategory.COMBAT)
public class AuraModule extends Module {

    /*
        for anyone curious, attack range is when the attack is processed, swing range is when you start pre-attacking which uses real left-clicking.
        simulate mouse clicks is just fully legit REAL left-clicking, this helps in hvh so you can get start to attack before 3 blocks
        (which is the limit for prediction based anti-cheats when using mc.playerController.attackEntity).

        therefore, using 6.0 on all ranges with simulate mouse clicks is the most optimal settings for prediction anti-cheats.

        now if on NCP or a less strict anti-cheat DON'T use simulate mouse clicks. instead use swing range 6.0, attack
        range 4.2, and block range at 6.0

        this is honestly the only client that uses these attack methods to date, and it's kinda sad.
        this all results in beating every other Hypixel client (paid ones and clients with auto blocks included)
        in a hvh with even using fake auto block on Yuri.

        -unlegit
    */

    private final MultiModeProperty<TargetManager.Targets> targets = new MultiModeProperty<>("Targets", TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.TEAMMATES, TargetManager.Targets.INVISIBLES);
    private static final ModeProperty<TargetManager.Mode> mode = new ModeProperty<>("Mode", TargetManager.Mode.SINGLE);
    public static NumberProperty seekRange = new NumberProperty("Seek Range", 6.0, 3, 6, 0.1);
    public static final Property<Boolean> useOnlyMouse = new Property<>("Simulate Mouse Clicks", true);
    public static NumberProperty attackRange = new NumberProperty("Attack Range", 3.0, 3, 6, 0.1, () -> !useOnlyMouse.getValue());
    public static NumberProperty swingRange = new NumberProperty("Swing Range", 6.0, 3, 6, 0.1);
    public static NumberProperty blockRange = new NumberProperty("Block Range", 6.0, 3, 6, 0.1);
    private static final NumberProperty min = new NumberProperty("Min CPS", 9.0, 1, 20.0, 0.1);
    private static final NumberProperty max = new NumberProperty("Max CPS", 13.0, 1, 20.0, 0.1);
    public static ModeProperty<AutoBlock> ab = new ModeProperty<>("Auto Block", AutoBlock.FAKE);
    private static final NumberProperty blockCps = new NumberProperty("Auto Block CPS", 8.0, 1, 10, 0.1, () -> ab.getValue() == AutoBlock.HYPIXEL);
    public static Property<Boolean> onlyBlockIfHurt = new Property<>("Only Block If Hurt", false);
    private final NumberProperty blockOnHurtTicks = new NumberProperty("Block On Hurt Ticks", 4, 0, 10, 1, onlyBlockIfHurt::getValue);
    public static final Property<Boolean> throughWalls = new Property<>("Through Walls", false);
    public static ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.NORMAL);
    private final NumberProperty minRotSpeed = new NumberProperty("Min Rotation Speed", 3, 0.1, 10, 0.1f);
    private final NumberProperty maxRotSpeed = new NumberProperty("Max Rotation Speed", 7, 0.1, 10, 0.1f);
    private final NumberProperty bodyEase = new NumberProperty("Body Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    private final NumberProperty mlEase = new NumberProperty("ML Ease", 0.2, 0.01, 1.0, 0.01, () -> rotations.getValue() == Rotations.ML);
    public static final Property<Boolean> rayCast = new Property<>("Ray Cast", true);
    public static final ModeProperty<MoveFix> fix = new ModeProperty<>("Move Fix", MoveFix.SILENT);
    public static final Property<Boolean> sprint = new Property<>("Keep Sprint", false);
    public static final Property<Boolean> hypixelSprint = new Property<>("Hypixel Keep Sprint", false, sprint::getValue);
    public static final Property<Boolean> autoDisable = new Property<>("Auto Disable", true);

    public enum MoveFix {
        NONE("None"),
        STRICT("Strict"),
        SILENT("Silent");

        public final String name;

        MoveFix(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Rotations {
        NORMAL("Normal"),
        ML("ML"),
        NONE("None");

        public final String name;

        Rotations(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum AutoBlock {
        FAKE("Fake"),
        VANILLA("Vanilla"),
        NCP("NCP"),
        LEGIT("Legit"),
        HYPIXEL("Hypixel"),
        NONE("None");

        public final String name;

        AutoBlock(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static EntityLivingBase target;
    public static boolean autoBlocking = false;
    public static boolean canAttack = true;
    public static boolean rotationOverride = false;
    private static final TimerUtils attackTimer = new TimerUtils();
    private int blockTicks = 0;
    private static long delay = 0;
    private static long lastAttackStamp = 0;
    public int hitTicks;
    private EntityLivingBase lastTarget;
    private Vec3 smoothedBodyPoint;
    private static final TimerUtils blockTimer = new TimerUtils();
    private boolean hypixelBlocking = false;
    private int hypixelTick = 0;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null || Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class).isEnabled()) {
            if (target != null || autoBlocking) {
                resetCombatState();
            }
            return;
        }

        TargetManager.setTargets(targets.getValue());
        target = TargetManager.getTarget();

        if (target != null && !throughWalls.getValue() && !PlayerUtils.canSeeEntity(target)) {
            target = null;
        }

        if (target == null) {
            unblock();
            canAttack = true;
            return;
        }

        calculateRotations();

        if (ab.getValue() != AutoBlock.NONE && ab.getValue() != AutoBlock.NCP) {
            if (mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue() && InventoryUtils.isHoldingSword()) {
                autoblock();
            }
        }

        if (ab.getValue() == AutoBlock.LEGIT && mc.gameSettings.keyBindAttack.isPressed()) {
            mc.gameSettings.keyBindAttack.setPressed(false);
        }

        attack();
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (event.isPre()) {
            this.hitTicks++;
            return;
        }

        if (ab.getValue() == AutoBlock.HYPIXEL && hypixelBlocking && mc.thePlayer != null && !mc.thePlayer.isBlocking()) {
            ItemStack held = mc.thePlayer.getHeldItem();
            if (held != null) {
                mc.thePlayer.setItemInUse(held, held.getMaxItemUseDuration());
            }
        }

        if (target == null) return;

        if (ab.getValue() == AutoBlock.NCP) {
            if (!autoBlocking && InventoryUtils.isHoldingSword() && mc.thePlayer.getDistanceToEntity(target) <= blockRange.getValue()) {
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
            }
        }
    }

    @EventHook
    public void onHitSlowDown(HitSlowDownEvent e) {
        if (sprint.getValue() && !hypixelSprint.getValue()) {
            e.setSprint(true);
            e.setSlowDown(1.0);
        }

        if (hypixelSprint.getValue() && sprint.getValue()) {
            if (!mc.thePlayer.isCollidedHorizontally && mc.thePlayer.isSprinting() && mc.thePlayer.moveForward > 0 && mc.thePlayer.hurtTime <= 4) {
                e.setSprint(true);
            }
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent e) {
        resetCombatState();
        if (autoDisable.getValue()) {
            toggle();
        }
    }

    private void calculateRotations() {
        if (mc.thePlayer == null || target == null || rotations.getValue() == Rotations.NONE) return;
        if (rotationOverride) return;

        if (target != lastTarget) {
            smoothedBodyPoint = null;
            RotationLearnerManager.resetSmoothing();
            lastTarget = target;
        }

        float rotSpeed = (float) MathUtils.getRandom(minRotSpeed.getValue(), maxRotSpeed.getValue());
        Vector2f rotation = RotationUtils.calculate(target, false, seekRange.getValue());

        if (rotations.getValue() == Rotations.ML && RotationLearnerManager.hasModelLoaded()) {
            rotation = RotationLearnerManager.humanize(RotationUtils.getWholeBodyRotation(target, smoothedBodyPoint, bodyEase.getValue()), 1.0f, mlEase.getValue().floatValue());
        }

        RotationManager.setRotations(rotation, rotSpeed, fix.getValue() != MoveFix.NONE ? fix.getValue() == MoveFix.SILENT ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.TRADITIONAL : RotationManager.MovementFix.OFF);
    }

    private void releaseBlock() {
        hypixelBlocking = false;
        if (mc.thePlayer == null) return;
        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        mc.thePlayer.stopUsingItem();
    }

    private void sendBlock() {
        if (mc.thePlayer == null) return;
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return;
        PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(held));
        mc.thePlayer.setItemInUse(held, held.getMaxItemUseDuration());
        hypixelBlocking = true;
    }

    private void autoblock() {
        if (mc.thePlayer == null || mc.playerController == null) return;

        if (target == null || mc.thePlayer.getDistanceToEntity(target) > blockRange.getValue() || !InventoryUtils.isHoldingSword()) {
            if (autoBlocking) unblock();
            blockTimer.reset();
            return;
        }

        if (onlyBlockIfHurt.getValue() && mc.thePlayer.hurtTime < blockOnHurtTicks.getValue().intValue()) {
            if (autoBlocking) unblock();
            return;
        }

        switch (ab.getValue()) {
            case FAKE:
                autoBlocking = true;
                break;
            case LEGIT:
                mc.gameSettings.keyBindUseItem.setPressed(mc.thePlayer.hurtTime <= 10 && mc.thePlayer.hurtTime >= 6 && mc.thePlayer.getDistanceToEntity(target) <= 3.0f);
                autoBlocking = true;
                blockTicks++;
                if (mc.gameSettings.keyBindUseItem.isPressed() || mc.thePlayer.isUsingItem()) {
                    blockTicks = 0;
                }
                canAttack = !BadPacketsManager.bad(false, false, false, true, false) && blockTicks >= 1;
                break;
            case VANILLA:
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
                autoBlocking = true;
                break;
            case HYPIXEL:
                canAttack = true;
                if (hypixelBlocking) {
                    releaseBlock();
                }
                long remaining = delay - (System.currentTimeMillis() - lastAttackStamp);
                if (remaining <= 50L) {
                    hypixelTick = 1;
                } else if (hypixelTick == 1) {
                    sendBlock();
                    hypixelTick = 0;
                }
                autoBlocking = true;
                break;
            case NCP:
                canAttack = true;
                if (autoBlocking) {
                    PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
                    autoBlocking = false;
                }
                break;
        }
    }

    private void unblock() {
        if (!autoBlocking) {
            canAttack = true;
            return;
        }

        blockTimer.reset();
        blockTicks = -1;

        if (ab.getValue() == AutoBlock.FAKE) {
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.LEGIT) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (ab.getValue() == AutoBlock.HYPIXEL) {
            if (hypixelBlocking) {
                releaseBlock();
            }
            hypixelTick = 0;
            autoBlocking = false;
            canAttack = true;
            return;
        }

        if (InventoryUtils.isHoldingSword() && ab.getValue() != AutoBlock.LEGIT) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN));
        }

        autoBlocking = false;
        canAttack = true;
    }

    private void attack() {
        if (mc.thePlayer == null || mc.playerController == null || target == null || !canAttack)
            return;
        if (!hitTimerDone()) return;

        double dist = mc.thePlayer.getDistanceToEntity(target);

        if (dist <= attackRange.getValue() && !useOnlyMouse.getValue()) {
            if (rayCast.getValue() && !(RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()) != null
                    && RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()).entityHit != null
                    && RayCastUtils.rayCast(RotationManager.rotations, blockRange.getValue().floatValue()).entityHit == target))
                return;
            mc.thePlayer.swingItem();
            mc.playerController.attackEntity(mc.thePlayer, target);
            this.hitTicks = 0;
        } else if (dist <= swingRange.getValue()) {
            mc.clickMouse();
            this.hitTicks = 0;
        }
    }

    private static boolean hitTimerDone() {
        boolean returnVal = false;
        if (attackTimer.hasTimeElapsed(delay, false)) {
            returnVal = true;
            attackTimer.reset();
            lastAttackStamp = System.currentTimeMillis();
            if (ab.getValue() == AutoBlock.LEGIT) {
                delay = (long) (1000.0 / 5.0);
            } else if (ab.getValue() == AutoBlock.HYPIXEL) {
                delay = (long) (1000.0 / blockCps.getValue());
            } else {
                delay = (long) (1000.0 / getCPS());
            }
        }
        return returnVal;
    }

    private void resetCombatState() {
        if (autoBlocking) {
            unblock();
        } else {
            canAttack = true;
        }
        if (hypixelBlocking) {
            releaseBlock();
        }
        hypixelTick = 0;
        if (SlotManager.isActive()) {
            SlotManager.swapBack();
        }
        target = null;
        lastTarget = null;
        smoothedBodyPoint = null;
        RotationLearnerManager.resetSmoothing();
        delay = 0;
        blockTimer.reset();
        blockTicks = -1;
        attackTimer.reset();
    }

    @Override
    public void onEnable() {
        delay = (long) (1000.0 / getCPS());
        lastAttackStamp = 0;
        canAttack = true;
        autoBlocking = false;
        hypixelBlocking = false;
        hypixelTick = 0;
        blockTicks = -1;
        TargetManager.configure(Arrays.asList(targets.getValues()));
        attackTimer.reset();
        if (rotations.getValue() == Rotations.ML) {
            if (!RotationLearnerManager.hasModelLoaded()) {
                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Use .rot load <name> to load a rotation model!");
            }
        }
        super.onEnable();
    }

    private static double getCPS() {
        double minVal = min.getValue();
        double maxVal = max.getValue();
        if (maxVal <= 0) maxVal = 1.0;
        if (minVal < 0) minVal = 0.0;
        if (minVal > maxVal) {
            double t = minVal;
            minVal = maxVal;
            maxVal = t;
        }
        double cps = MathHelper.clamp_double(minVal + ((maxVal - minVal) * new SecureRandom().nextDouble()), minVal, maxVal);
        return Math.max(1.0, cps);
    }

    @Override
    public void onDisable() {
        resetCombatState();
        super.onDisable();
    }
}