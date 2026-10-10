package ddlc.yuri.modules.impl.movement;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.player.StrafeEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.LagManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.InventoryUtils;
import ddlc.yuri.utils.player.MoveUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.init.Items;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;

@ModuleInfo(label = "Long Jump", category = ModuleCategory.MOVEMENT, description = "Allows you to jump long distances")
public final class LongJumpModule extends Module {

    public final Property<Boolean> fireballToggle = new Property<Boolean>("Fireball", true);
    public final ModeProperty<FireballMode> fireballMode = new ModeProperty<>("Fireball Mode", FireballMode.LEGIT, fireballToggle::getValue);
    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.MOSPIXEL, () -> !fireballToggle.getValue());
    private final NumberProperty delay = new NumberProperty("Delay", 400, 50, 2000, 5, () -> fireballToggle.getValue() && fireballMode.getValue() == FireballMode.LAG);

    public enum Mode {
        MOSPIXEL("Mospixel");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum FireballMode {
        LEGIT("Legit"),
        LAG("Lag");

        public final String name;

        FireballMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private boolean canDisable;
    private int tick;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(fireballToggle.getValue() ? fireballMode.getValue().toString() : mode.getValue().toString());

        if (fireballToggle.getValue()) {
            int item = InventoryUtils.findItem(Items.fire_charge);

            if (mc.thePlayer.onGroundTicks == 1) {
                MoveUtils.stop();
            }

            if (item == -1) {;
                setEnabled(false);
                return;
            }

            tick++;

            mc.thePlayer.inventory.currentItem = item;

            RotationManager.setRotations(mc.thePlayer.rotationYaw, 90, 10, RotationManager.MovementFix.NORMAL);

            if (fireballMode.getValue() == FireballMode.LAG) {
                // crazy stuff here guys
                LagManager.spoof(delay.getValue().intValue(), true, true, true, true, false, true);
            }

            if (tick == 2) {
                PacketUtils.sendPacket(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getStackInSlot(item)));
            }

            if (tick == 5 && fireballMode.getValue() == FireballMode.LEGIT) {
                setEnabled(false);
            }

            if (tick == 40 && fireballMode.getValue() == FireballMode.LAG) {
                setEnabled(false);
            }
        }
    }

    @EventHook
    public void onStrafe(StrafeEvent event) {

        if (mc.thePlayer.onGround && !canDisable) {
            mc.thePlayer.jump();
        }

        if (mode.getValue() == Mode.MOSPIXEL && !fireballToggle.getValue()) {
            if (mc.thePlayer.offGroundTicks > 21 && mc.thePlayer.offGroundTicks < 59) {
                mc.thePlayer.motionY = -0.2f;
                canDisable = true;
            }

            if (mc.thePlayer.offGroundTicks == 1) {
                canDisable = true;
                event.setSpeed(1.5f);
            }

            if (canDisable && mc.thePlayer.onGround) {
                setEnabled(false);
            }
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (event.getPacket() instanceof S08PacketPlayerPosLook) {
            canDisable = false;
        }
    }

    @Override
    public void onDisable() {
        if (mode.getValue() == Mode.MOSPIXEL && !fireballToggle.getValue()) {
            MoveUtils.stop();
        }

        if (fireballToggle.getValue() && fireballMode.getValue() == FireballMode.LAG) {
            LagManager.disable();
        }
    }

    @Override
    public void onEnable() {
        canDisable = false;
        tick = 0;
    }
}