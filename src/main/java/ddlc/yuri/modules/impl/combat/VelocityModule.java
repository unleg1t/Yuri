package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import ddlc.yuri.modules.impl.combat.velocity.impl.CancelVelocity;
import ddlc.yuri.modules.impl.combat.velocity.impl.CustomVelocity;
import ddlc.yuri.modules.impl.combat.velocity.impl.IntaveVelocity;
import ddlc.yuri.modules.impl.combat.velocity.impl.LegitVelocity;

import java.util.EnumMap;
import java.util.Map;

@ModuleInfo(label = "Velocity", description = "Stops knockback or reduces it", category = ModuleCategory.COMBAT)
public final class VelocityModule extends Module {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CANCEL);
    public final Property<Boolean> ignoreOnFire = new Property<>("Ignore On Fire", true);

    public final NumberProperty xModify = new NumberProperty("Velocity X Modifier", 0.0, 0.0, 5.0, 1.0, () -> mode.getValue() == Mode.CUSTOM);
    public final NumberProperty yModify = new NumberProperty("Velocity Y Modifier", 1.0, 0.0, 5.0, 1.0, () -> mode.getValue() == Mode.CUSTOM);
    public final NumberProperty zModify = new NumberProperty("Velocity Z Modifier", 0.0, 0.0, 5.0, 1.0, () -> mode.getValue() == Mode.CUSTOM);

    public final ModeProperty<IntaveMode> intaveMode = new ModeProperty<>("Intave Mode", IntaveMode.INTAVE_LATEST, () -> mode.getValue() == Mode.INTAVE);

    public final Property<Boolean> universalReduce = new Property<>("Reduce", true, () -> mode.getValue() == Mode.LEGIT);
    public final NumberProperty attackTimes = new NumberProperty("Attack Times", 1, 1, 5, 1, () -> mode.getValue() == Mode.LEGIT && universalReduce.getValue());
    public final Property<Boolean> onlySprinting = new Property<>("Only Sprinting", true, () -> mode.getValue() == Mode.LEGIT && universalReduce.getValue());
    public final Property<Boolean> reduceWhenCanAttack = new Property<>("Reduce When Can Attack", true, () -> mode.getValue() == Mode.LEGIT && universalReduce.getValue());

    public enum Mode {
        LEGIT("Legit"),
        INTAVE("Intave"),
        CANCEL("Cancel"),
        CUSTOM("Custom");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum IntaveMode {
        INTAVE_LATEST("Intave Latest"),
        INTAVE_13("Intave 13");

        public final String name;

        IntaveMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final Map<Mode, VelocityMode> velocityMode;

    {
        velocityMode = new EnumMap<>(Mode.class);

        velocityMode.put(Mode.LEGIT, new LegitVelocity(this));
        velocityMode.put(Mode.INTAVE, new IntaveVelocity(this));
        velocityMode.put(Mode.CANCEL, new CancelVelocity(this));
        velocityMode.put(Mode.CUSTOM, new CustomVelocity(this));
    }

    @EventHook
    public void onTick(ClientTickEvent event) {

        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onTick(event);
        }
    }

    @EventHook
    public void onPacket(PacketReceivedEvent event) {

        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onPacket(event);
        }
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        VelocityMode currentMode = velocityMode.get(mode.getValue());
        if (currentMode != null) {
            currentMode.onAttack(event);
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        VelocityMode currentMode = velocityMode.get(mode.getValue());

        if (currentMode != null) {
            currentMode.onPreUpdate(event);
        }
    }

    @EventHook
    public void onHitSlowdown(HitSlowDownEvent event) {
        VelocityMode currentMode = velocityMode.get(mode.getValue());

        if (currentMode != null) {
            currentMode.onHitSlowdown(event);
        }
    }
}
