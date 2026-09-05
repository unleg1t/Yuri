package ddlc.yuri.modules.impl.player;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.MoveEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ProgressBarManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.managers.impl.SlotManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.movement.SpeedModule;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.*;
import ddlc.yuri.utils.player.packet.PacketUtils;
import ddlc.yuri.utils.render.progress.ProgressBarEntry;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockAir;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

@ModuleInfo(label = "Scaffold", description = "Automatically builds bridges for you", category = ModuleCategory.PLAYER)
public final class ScaffoldModule extends Module {

    public static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.NORMAL);
    public final Property<Boolean> hypixelTelly = new Property<>("Hypixel Telly", false, () -> mode.getValue() == Mode.TELLY);
    private final NumberProperty tellyStraightTicks = new NumberProperty("Telly Straight Ticks", 6, 0, 8, 1, () -> mode.getValue() == Mode.TELLY && !hypixelTelly.getValue());
    private final NumberProperty tellyDiagonalTicks = new NumberProperty("Telly Diagonal Ticks", 4, 0, 8, 1, () -> mode.getValue() == Mode.TELLY && !hypixelTelly.getValue());
    private final NumberProperty tellyJumpDownTicks = new NumberProperty("Telly Jump Down Ticks", 1, 0, 8, 1, () -> mode.getValue() == Mode.TELLY && !hypixelTelly.getValue());
    public static final ModeProperty<Rotations> rotations = new ModeProperty<>("Rotations", Rotations.NORMAL);
    private final NumberProperty randomizedSpeedMin = new NumberProperty("Randomized Speed Min", 3, 0, 10, 0.5f, () -> rotations.getValue() == Rotations.RANDOMIZED);
    private final NumberProperty randomizedSpeedMax = new NumberProperty("Randomized Speed Max", 7, 0, 10, 0.5f, () -> rotations.getValue() == Rotations.RANDOMIZED);
    public final ModeProperty<SearchAlgorithm> searchAlgorithm = new ModeProperty<>("Search Algorithm", SearchAlgorithm.NORMAL, () -> rotations.getValue() != Rotations.OLD);
    private final NumberProperty minRotationSpeed = new NumberProperty("Min Rotation Speed", 3, 0, 10, 0.5f);
    private final NumberProperty maxRotationSpeed = new NumberProperty("Max Rotation Speed", 7, 0, 10, 0.5f);
    private final NumberProperty placeDelay = new NumberProperty("Place Delay", 0, 0, 10, 1);
    private final ModeProperty<RayCast> rayCast = new ModeProperty<>("Ray Cast", RayCast.NORMAL);
    private final ModeProperty<SwapMode> swapMode = new ModeProperty<>("Swap Mode", SwapMode.CLIENT);
    private final ModeProperty<SprintMode> sprintMode = new ModeProperty<>("Sprint Mode", SprintMode.NONE);
    private final ModeProperty<TowerMode> towerMode = new ModeProperty<>("Tower Mode", TowerMode.NONE);
    private final Property<Boolean> towerMove = new Property<>("Tower Move", true, () -> towerMode.getValue() != TowerMode.NONE);
    public final Property<Boolean> moveFix = new Property<>("Move Fix", true);
    public final Property<Boolean> autoJump = new Property<>("Auto Jump", false);
    public final Property<Boolean> keepY = new Property<>("Keep Y", false);
    private final Property<Boolean> sneak = new Property<>("Sneak", false);
    private final NumberProperty sneakEvery = new NumberProperty("Sneak Every", 1, 0, 10, 1, sneak::getValue);
    private final Property<Boolean> safeWalk = new Property<>("Safe Walk", false);
    private final NumberProperty expand = new NumberProperty("Expand", 0, 0, 4, 1);
    private final ModeProperty<BlockCounter> blockCounter = new ModeProperty<>("Block Counter", BlockCounter.NONE);
    private final Property<Boolean> autoDisable = new Property<>("Auto Disable", false);

    public enum Mode {
        NORMAL("Normal"),
        TELLY("Telly"),
        BREEZILY("Breezily"),
        GOD_BRIDGE("God Bridge");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum Rotations {
        NORMAL("Normal"), // POLAR("Polar"),
        RANDOMIZED("Randomized"), OLD("Old");
        public final String name;

        Rotations(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum SprintMode {
        VANILLA("Vanilla"), UNIVERSAL("Universal"), NCP("NCP"), LEGIT("Legit"), NONE("None");
        public final String name;

        SprintMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum TowerMode {
        VANILLA("Vanilla"), POLAR("Polar"), HYPIXEL("Hypixel"), NCP("NCP"), NONE("None");
        public final String name;

        TowerMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum RayCast {
        NONE("None"), NORMAL("Normal"), STRICT("Strict");
        public final String name;

        RayCast(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum SwapMode {
        CLIENT("Client"), SERVER("Server");
        public final String name;

        SwapMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum SearchAlgorithm {
        NORMAL("Normal"), ULTRA_SAFE("Ultra Safe"), SECONDARY("Secondary");
        public final String name;

        SearchAlgorithm(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum BlockCounter {
        BAR("Bar"), SIMPLE("Simple"), NONE("None");
        public final String name;

        BlockCounter(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final TimerUtils delayTimer = new TimerUtils();
    private final TimerUtils tellySafeTimer = new TimerUtils();
    private Vec3 targetBlock;
    private EnumFacingOffset enumFacing;
    public Vec3i offset = new Vec3i(0, 0, 0);
    private BlockPos blockFace;
    private float targetYaw, targetPitch;
    @Getter
    @Setter
    private int ticksOnAir;
    public int recursions, recursion;
    public double startY;
    private boolean canPlace;
    private int directionalChange;
    private int blockCount;
    private float rotSpeed;
    private boolean stop;
    private int blocksPlaced;
    private float counterAlpha = 0f;
    private long lastRenderTime = -1L;
    private int initialBlockCount;
    private ProgressBarEntry barEntry;
    private boolean tellyNoPlace;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!isEnabled()) return;
        resetBinds(false, false, true, true, false, false);

        if (autoDisable.getValue()) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityFireball && entity.getDistanceToEntity(mc.thePlayer) < 6) {
                    RotationManager.setRotations(RotationUtils.calculate(entity), 10, RotationManager.MovementFix.NORMAL);
                    if (entity.getDistanceToEntity(mc.thePlayer) <= 5) {
                        Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Disabled, fireball detected.");
                        this.toggle();
                        break;
                    }
                    break;
                }
            }
        }

        if (mc.gameSettings.keyBindAttack.isPressed()) {
            mc.gameSettings.keyBindAttack.setPressed(false);
        }

        if (mode.getValue() == Mode.TELLY) {
            if (tellySafeTimer.hasTimeElapsed(500)) {
                mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
            } else {
                mc.gameSettings.keyBindJump.setPressed(true);
            }
        }

        setSuffix(mode.getValue().toString());

        if (safeWalk.getValue()) {
            mc.thePlayer.safeWalk = true;
        }

        sprint();
        sneak();
        tower();

        if (mode.getValue() == Mode.TELLY) {
            tellyLogic();
        }

        if (mc.gameSettings.keyBindJump.isKeyDown()
                || (mc.thePlayer.onGround && mc.thePlayer.posY < startY)
                || (mc.thePlayer.onGround && mc.thePlayer.posY > startY && mc.thePlayer.onGroundTicks <= 1)
                || (mc.thePlayer.onGround && Math.abs(mc.thePlayer.posY - startY) > 0.5 && !MoveUtils.isMoving())) {
            startY = Math.floor(mc.thePlayer.posY);
        }

        for (recursion = 0; recursion <= recursions; recursion++) {

            this.rotSpeed = (float) MathUtils.getRandom(this.minRotationSpeed.getValue(), this.maxRotationSpeed.getValue());

            if (expand.getValue().intValue() != 0) {
                double direction = MoveUtils.direction(mc.thePlayer.rotationYaw,
                        mc.gameSettings.keyBindForward.isKeyDown() ? 1 : mc.gameSettings.keyBindBack.isKeyDown() ? -1 : 0,
                        mc.gameSettings.keyBindRight.isKeyDown() ? -1 : mc.gameSettings.keyBindLeft.isKeyDown() ? 1 : 0);

                for (int range = 0; range <= expand.getValue().intValue(); range++) {
                    if (BlockUtils.blockAheadOfPlayer(range, this.offset.getY() - 0.5) instanceof BlockAir) {
                        this.offset = this.offset.add(new Vec3i(
                                (int) (-Math.sin(direction) * (range + 1)), 0,
                                (int) (Math.cos(direction) * (range + 1))));
                        break;
                    }
                }
            }

            final boolean sameY = (((keepY.getValue() && !mc.gameSettings.keyBindJump.isKeyDown()) ||
                    Yuri.INSTANCE.getModuleManager().getModule(SpeedModule.class).isEnabled()
                            && !mc.gameSettings.keyBindJump.isKeyDown()) && MoveUtils.isMoving()) && Math.abs(mc.thePlayer.posY - startY) <= 3.0;

            final int blockSlot = ScaffoldUtils.findPreferredBlockSlot();
            if (blockSlot == -1) {
                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Disabled, no blocks found.");
                this.toggle();
                return;
            }

            final ItemStack blockStack = mc.thePlayer.inventory.getStackInSlot(blockSlot);
            if (blockStack == null || !(blockStack.getItem() instanceof ItemBlock)) {
                return;
            }

            if (swapMode.getValue() == SwapMode.CLIENT || swapMode.getValue() == SwapMode.SERVER) {
                if (rayCast.getValue() == RayCast.STRICT) {
                    SlotManager.swap(blockSlot, false);
                } else {
                    SlotManager.swap(blockSlot, swapMode.getValue() == SwapMode.SERVER);
                }
            }

            if (ScaffoldUtils.doesNotContainBlock(offset, 1) && (!sameY ||
                    (ScaffoldUtils.doesNotContainBlock(offset, 2)
                            && ScaffoldUtils.doesNotContainBlock(offset, 3)
                            && ScaffoldUtils.doesNotContainBlock(offset, 4)))) {
                ticksOnAir++;
            } else {
                ticksOnAir = 0;
            }

            canPlace = ticksOnAir > 0;

            if (mode.getValue() == Mode.TELLY && tellyNoPlace) {
                canPlace = false;
            }

            targetBlock = PlayerUtils.getPlacePossibility(offset.getX(), offset.getY(), offset.getZ(), sameY ? (int) Math.floor(startY) : null);
            if (targetBlock == null && sameY) {
                targetBlock = PlayerUtils.getPlacePossibility(offset.getX(), offset.getY(), offset.getZ(), null);
            }
            if (targetBlock == null) return;

            enumFacing = PlayerUtils.getEnumFacing(targetBlock, offset.getY() < 0);
            if (enumFacing == null) return;

            final BlockPos position = new BlockPos(targetBlock.xCoord, targetBlock.yCoord, targetBlock.zCoord);
            blockFace = position.add(enumFacing.getOffset().xCoord, enumFacing.getOffset().yCoord, enumFacing.getOffset().zCoord);

            if (blockFace == null || enumFacing.getEnumFacing() == null) return;

            this.doRotations();

            if (targetBlock == null || enumFacing == null || blockFace == null) return;

            if (startY - 1 != Math.floor(targetBlock.yCoord) && sameY) return;

            if (blockStack == null || !(blockStack.getItem() instanceof ItemBlock))
                return;

            if (canPlace && (RayCastUtils.overBlock(enumFacing.getEnumFacing(), blockFace,
                    rayCast.getValue() == RayCast.STRICT) || rayCast.getValue() == RayCast.NONE)) {
                this.place(blockStack);
                ticksOnAir = 0;
            }
        }
    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!isEnabled()) return;
        if (!event.isPre()) return;
        this.offset = new Vec3i(0, 0, 0);
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {
        if (!isEnabled()) return;
        if (!mc.gameSettings.keyBindJump.isPressed()) {
            this.jump();
            if (mode.getValue() == Mode.GOD_BRIDGE && !autoJump.getValue()) {
                if (blocksPlaced >= 8) {
                    if (mc.thePlayer.onGround) mc.thePlayer.jump();
                    blocksPlaced = 0;
                }
            }
        }
    }

    @EventHook
    public void onMove(MoveEvent event) {
        if (!isEnabled()) return;
        if (stop) {
            event.setForward(0);
            event.setStrafe(0);
            event.setJump(false);
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        renderBlockCounter();
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (autoJump.getValue()) {
            if (event.getPacket() instanceof S02PacketChat) {
                S02PacketChat packet = (S02PacketChat) event.getPacket();
                if (packet.getChatComponent().getUnformattedText().contains("You are not allowed to place blocks here!")) {
                    Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Disabled, block placement denied by server!");
                    this.toggle();
                }
            }
        }
    }

    private void tellyLogic() {
        if (mc.thePlayer.offGroundTicks == 0) {
            if (mc.thePlayer.onGroundTicks == 0) {
                tellyNoPlace = true;
            }
        } else if (!hypixelTelly.getValue()) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                if (mc.thePlayer.offGroundTicks >= tellyJumpDownTicks.getValue().intValue()) {
                    tellyNoPlace = false;
                }
            } else if (isDiagonal()) {
                if (mc.thePlayer.offGroundTicks == tellyDiagonalTicks.getValue().intValue()) {
                    tellyNoPlace = false;
                }
            } else {
                if (mc.thePlayer.offGroundTicks == tellyStraightTicks.getValue().intValue()) {
                    tellyNoPlace = false;
                }
            }
        } else {
            if (mc.thePlayer.offGroundTicks <= (isDiagonal() || mc.gameSettings.keyBindJump.isKeyDown() ? 3 : 3)) {
                tellyNoPlace = false;
            }
        }
    }

    private boolean isDiagonal() {
        float delta = mc.thePlayer.rotationYaw % 90;
        if (delta < 0) delta += 90;
        return delta > 20 && delta < 70;
    }

    public void resetBinds() {
        resetBinds(true, true, true, true, true, true);
    }

    public void resetBinds(boolean sneak, boolean jump, boolean right, boolean left, boolean forward, boolean back) {
        if (sneak)
            mc.gameSettings.keyBindSneak.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode()));
        if (jump) mc.gameSettings.keyBindJump.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode()));
        if (right)
            mc.gameSettings.keyBindRight.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindRight.getKeyCode()));
        if (left) mc.gameSettings.keyBindLeft.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.getKeyCode()));
        if (forward)
            mc.gameSettings.keyBindForward.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindForward.getKeyCode()));
        if (back) mc.gameSettings.keyBindBack.setPressed(Keyboard.isKeyDown(mc.gameSettings.keyBindBack.getKeyCode()));
    }

    public void doRotations() {
        RotationManager.MovementFix movementFix = moveFix.getValue()
                ? RotationManager.MovementFix.NORMAL
                : RotationManager.MovementFix.OFF;

        float[] target = {targetYaw, targetPitch};

        switch (mode.getValue()) {
            case NORMAL:
                mc.entityRenderer.getMouseOver(1);
                if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                    if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                        ScaffoldUtils.computeNormalRotations(blockFace, enumFacing, target,
                                searchAlgorithm.getValue(), rayCast.getValue() == RayCast.STRICT);
                    }
                }
                break;
            case GOD_BRIDGE:
                mc.entityRenderer.getMouseOver(1);
                if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                    if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                        ScaffoldUtils.computeNormalRotations(blockFace, enumFacing, target,
                                searchAlgorithm.getValue(), rayCast.getValue() == RayCast.STRICT);
                    }
                }
                directionalChange++;
                if (Math.abs(MathHelper.wrapAngleTo180_double(target[0] - (RotationManager.lastServerRotations != null ? RotationManager.lastServerRotations.getX() : mc.thePlayer.rotationYaw))) > 10) {
                    directionalChange = (int) (Math.random() * 4);
                }

                mc.gameSettings.keyBindSneak.setPressed(directionalChange <= 10);
                break;

            case BREEZILY:
                if (canPlace) {
                    if (enumFacing.getEnumFacing() == EnumFacing.UP) {
                        target[1] = 90;
                    } else {
                        target[0] = (float) ((Math.toDegrees(Math.atan2(
                                enumFacing.getOffset().zCoord, enumFacing.getOffset().xCoord)) % 360) - 90);
                        target[1] = 80;
                    }
                }

                if (mc.gameSettings.keyBindForward.isKeyDown() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    double off = 0, speed = 0;
                    switch (mc.thePlayer.getHorizontalFacing()) {
                        case NORTH:
                            off = mc.thePlayer.posX - Math.floor(mc.thePlayer.posX);
                            speed = mc.thePlayer.motionZ;
                            break;
                        case EAST:
                            off = mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ);
                            speed = mc.thePlayer.motionX;
                            break;
                        case SOUTH:
                            off = 1 - (mc.thePlayer.posX - Math.floor(mc.thePlayer.posX));
                            speed = mc.thePlayer.motionZ;
                            break;
                        case WEST:
                            off = 1 - (mc.thePlayer.posZ - Math.floor(mc.thePlayer.posZ));
                            speed = mc.thePlayer.motionX;
                            break;
                        default:
                            break;
                    }
                    speed = Math.abs(speed);

                    if (!(speed < 0.086 && Math.abs(off - 0.5) < 0.4 && placeDelay.getValue().intValue() <= 1)) {
                        if (off < 0.5 + ((Math.random() - 0.5) / 10)) {
                            mc.gameSettings.keyBindLeft.setPressed(false);
                            mc.gameSettings.keyBindRight.setPressed(true);
                        } else {
                            mc.gameSettings.keyBindRight.setPressed(false);
                            mc.gameSettings.keyBindLeft.setPressed(true);
                        }
                    }
                }
                break;

            case TELLY:
                if (canPlace && !mc.gameSettings.keyBindPickBlock.isKeyDown()) {
                    if (mc.objectMouseOver.sideHit != enumFacing.getEnumFacing() || !mc.objectMouseOver.getBlockPos().equals(blockFace)) {
                        ScaffoldUtils.computeNormalRotations(blockFace, enumFacing, target,
                                searchAlgorithm.getValue(), rayCast.getValue() == RayCast.STRICT);
                    }
                }

                mc.entityRenderer.getMouseOver(1);
                if (mc.thePlayer.onGround && MoveUtils.isMoving()) {
                    if (hypixelTelly.getValue()) {
                        rotSpeed = isDiagonal() || mc.gameSettings.keyBindJump.isKeyDown() ? 11.0f : 10.0f;
                    } else {
                        rotSpeed = 20.0f;
                    }
                    target[0] = mc.thePlayer.rotationYaw;
                } else {
                    if (hypixelTelly.getValue()) {
                        rotSpeed = isDiagonal() || mc.gameSettings.keyBindJump.isKeyDown() ? 7.6f : 2.1f;
                    }
                }
                break;
        }

        if (sprintMode.getValue() == SprintMode.UNIVERSAL && blocksPlaced >= 3) {
            target[0] = mc.thePlayer.rotationYaw;
            blocksPlaced = 0;
        }

        if (rotations.getValue() == Rotations.RANDOMIZED && MoveUtils.isMoving() && blockFace != null && enumFacing != null) {
            float amount = (float) MathUtils.getRandom(randomizedSpeedMin.getValue().floatValue(), randomizedSpeedMax.getValue().floatValue());
            float candidateYaw = target[0] + (float) ((Math.random() - 0.5) * 2 * amount);
            float candidatePitch = target[1] + (float) ((Math.random() - 0.5) * 0.5 * amount);

            if (RayCastUtils.overBlock(new Vector2f(candidateYaw, candidatePitch), enumFacing.getEnumFacing(), blockFace, rayCast.getValue() == RayCast.STRICT)) {
                target[0] = candidateYaw;
                target[1] = candidatePitch;
            }
        }

        targetYaw = target[0];
        targetPitch = target[1];

        if (rotSpeed != 0 && blockFace != null && enumFacing != null) {
            RotationManager.setRotations(new Vector2f(targetYaw, targetPitch), rotSpeed, movementFix);
        }
    }

    private void place(ItemStack blockStack) {
        if (!canPlace || !delayTimer.hasTimeElapsed(placeDelay.getValue().longValue() * 20)) return;

        if (rayCast.getValue() == RayCast.STRICT) {
            mc.rightClickMouse();
        } else {
            Vec3 hitVec = ScaffoldUtils.computeHitVec(blockFace, enumFacing);
            if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld,
                    blockStack, blockFace, enumFacing.getEnumFacing(), hitVec)) {
                PacketUtils.sendPacket(new C0APacketAnimation());
            }
        }
        blocksPlaced++;
        delayTimer.reset();
    }

    public void jump() {
        if (mc.gameSettings.keyBindJump.isKeyDown() || Yuri.INSTANCE.getModuleManager().getModule(SpeedModule.class).isEnabled())
            return;

        if (mode.getValue() == Mode.TELLY) {
            if (!autoJump.getValue()) autoJump.setValue(true);
        }

        if (keepY.getValue() && autoJump.getValue()) {
            if (mc.thePlayer.onGround && MoveUtils.isMoving() && mc.thePlayer.posY == startY) {
                handleJump();
            }
        }

        if (autoJump.getValue() && !keepY.getValue()) {
            if (mc.thePlayer.onGround && MoveUtils.isMoving()) {
                handleJump();
            }
        }
    }

    private void handleJump() {
        if (autoJump.getValue()) mc.thePlayer.jump();
    }

    private void sprint() {
        switch (sprintMode.getValue()) {
            case VANILLA:
                mc.thePlayer.setSprinting(MoveUtils.isMoving());
                break;
            case NCP:
                mc.thePlayer.setSprinting(false);
                MoveUtils.strafe(MoveUtils.getBaseMoveSpeed() / 1.84f);
                break;
            case NONE:
                mc.gameSettings.keyBindSprint.setPressed(false);
                mc.thePlayer.setSprinting(false);
                break;
            case UNIVERSAL:
            case LEGIT:
                if (!moveFix.getValue()) {
                    if (Math.abs(MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw) - MathHelper.wrapAngleTo180_float(RotationManager.rotations.x)) > 90) {
                        mc.gameSettings.keyBindSprint.setPressed(false);
                        mc.thePlayer.setSprinting(false);
                    }
                }
                break;
        }
    }

    private void sneak() {
        if (!sneak.getValue()) return;
        if (blocksPlaced >= sneakEvery.getValue().intValue() && !mc.gameSettings.keyBindSneak.isPressed()) {
            mc.gameSettings.keyBindSneak.setPressed(true);
            blocksPlaced = 0;
        } else {
            mc.gameSettings.keyBindSneak.setPressed(false);
        }
    }

    public void tower() {
        if (towerMode.getValue() == TowerMode.NONE || !mc.gameSettings.keyBindJump.isKeyDown() || !PlayerUtils.isBlockUnder(2))
            return;
        if (!towerMove.getValue() && !MoveUtils.isMoving()) return;

        switch (towerMode.getValue()) {
            case NCP:
                if (mc.thePlayer.posY % 1.0D <= 0.00153598D) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                    mc.thePlayer.motionY = 0.41998D;
                } else if (mc.thePlayer.posY % 1.0D < 0.1D && mc.thePlayer.onGround) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, Math.floor(mc.thePlayer.posY), mc.thePlayer.posZ);
                }
                break;
            case VANILLA:
                mc.thePlayer.motionY = 0.42;
                break;
            case HYPIXEL:
                if (mc.thePlayer.onGround && !MoveUtils.enoughMovementForSprinting()) {
                    mc.thePlayer.jump();
                }

                if (mc.thePlayer.offGroundTicks == 4 && MoveUtils.speed() == 0.0) {
                    mc.thePlayer.motionY -= 0.03;
                }

                if (mc.thePlayer.offGroundTicks == 5 && MoveUtils.speed() == 0.0) {
                    mc.thePlayer.motionY -= 0.5;
                }
                break;
            case POLAR:
                double ground = mc.thePlayer.posY - MoveUtils.findGround(mc.thePlayer);

                if (!MoveUtils.isMovingMotion(mc.thePlayer) && mc.thePlayer.motionY < 0 && ground < 1.26) {
                    mc.thePlayer.motionY -= 0.091F;
                }
                break;
        }
    }

    private void renderBlockCounter() {
        if (blockCounter.getValue() == BlockCounter.NONE) {
            counterAlpha = 0f;
            lastRenderTime = -1L;
            ProgressBarManager.remove(barEntry);
            barEntry = null;
            return;
        }

        long now = System.currentTimeMillis();
        float delta = lastRenderTime < 0 ? 0f : (now - lastRenderTime) / 1000f;
        lastRenderTime = now;

        counterAlpha += ((isEnabled() ? 1f : 0f) - counterAlpha) * Math.min(1f, 4f * delta);

        if (counterAlpha < 0.01f) {
            if (!isEnabled()) {
                blockCount = 0;
                ProgressBarManager.remove(barEntry);
                barEntry = null;
            }
            return;
        }

        blockCount = ScaffoldUtils.countBlocks();

        ScaledResolution sr = new ScaledResolution(mc);
        float centerX = sr.getScaledWidth() / 2.0f;
        float textY = sr.getScaledHeight() / 2.0f + 13;
        float width = 80.0f;
        float thickness = 2.5f;
        float percentage = initialBlockCount > 0 ? Math.min(1.0f, (float) blockCount / initialBlockCount) : 0.0f;

        if (blockCounter.getValue() == BlockCounter.BAR) {
            float barY = textY;
            if (barEntry == null) {
                barEntry = ProgressBarManager.add(percentage, centerX, barY);
                barEntry.setWidth(width);
                barEntry.setThickness(thickness);
            }
            barEntry.setProgress(percentage);
            barEntry.setX(centerX);
            barEntry.setY(barY);
        } else {
            ProgressBarManager.remove(barEntry);
            barEntry = null;
        }

        ScaffoldUtils.renderBlockCounter(blockCounter.getValue(), counterAlpha, blockCount);
    }

    @Override
    public void onEnable() {
        if (mc.thePlayer != null) {
            counterAlpha = 0f;
            targetYaw = mc.thePlayer.rotationYaw - 180;
            targetPitch = 90;
            startY = Math.floor(mc.thePlayer.posY);
            targetBlock = null;
            tellyNoPlace = false;
            this.initialBlockCount = ScaffoldUtils.countBlocks();
        }
        tellySafeTimer.reset();
        lastRenderTime = -1L;
        recursions = 0;
        barEntry = null;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer != null) {
            mc.thePlayer.safeWalk = false;
            mc.timer.timerSpeed = 1.0f;
            SlotManager.swapBack();
            stop = false;
            tellySafeTimer.reset();
            blocksPlaced = 0;
            blockCount = 0;
            initialBlockCount = 0;
            tellyNoPlace = false;
        }
        resetBinds();
        ProgressBarManager.remove(barEntry);
        barEntry = null;
        super.onDisable();
    }
}