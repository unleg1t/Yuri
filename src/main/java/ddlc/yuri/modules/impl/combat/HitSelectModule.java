package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.entity.EntityLivingBase;

import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(label = "Hit Select", description = "Baits the first hit then counters into a combo", category = ModuleCategory.COMBAT)
public final class HitSelectModule extends Module {

    private final NumberProperty chance = new NumberProperty("Chance", 100.0, 10.0, 100.0, 1.0);
    private final NumberProperty comboDelay = new NumberProperty("Combo Delay", 50.0, 0.0, 300.0, 5.0);
    private final NumberProperty resetTimeout = new NumberProperty("Reset Timeout", 1200.0, 300.0, 3000.0, 50.0);
    private final Property<Boolean> predict = new Property<>("Predict", true);
    private final NumberProperty predictReach = new NumberProperty("Predict Reach", 3.2, 2.0, 4.5, 0.1);
    private final NumberProperty swingThreshold = new NumberProperty("Swing Threshold", 0.1, 0.0, 1.0, 0.05);
    private final Property<Boolean> idleAttack = new Property<>("Idle Attack", false);
    private final NumberProperty idleCps = new NumberProperty("Idle CPS", 2.0, 1.0, 10.0, 0.5);

    private boolean hitReceived;
    private long hitTimestamp;
    private long lastAttackTimestamp;
    private float prevTargetSwing;
    private boolean attackAllowed = true;

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        reset();
        AuraModule.canAttack = true;
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        reset();
    }

    @EventHook
    public void onAttack(PlayerAttackEvent event) {
        if (!attackAllowed) {
            event.setCancelled(true);
            return;
        }
        lastAttackTimestamp = System.currentTimeMillis();
    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        EntityLivingBase target = AuraModule.target instanceof EntityLivingBase ? (EntityLivingBase) AuraModule.target : null;

        if (target == null) {
            reset();
            AuraModule.canAttack = true;
            return;
        }

        long now = System.currentTimeMillis();

        if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.hurtTime >= mc.thePlayer.maxHurtTime - 1) {
            if (!hitReceived) {
                hitReceived = true;
                hitTimestamp = now;
            }
        }

        if (!hitReceived && predict.getValue() && isPredictingHit(target)) {
            hitReceived = true;
            hitTimestamp = now;
        }

        if (hitReceived && (now - hitTimestamp >= resetTimeout.getValue())) {
            hitReceived = false;
        }

        boolean rollSuccess = ThreadLocalRandom.current().nextDouble(0.0, 100.0) <= chance.getValue();

        if (!rollSuccess) {
            attackAllowed = true;
        } else if (!hitReceived) {
            if (idleAttack.getValue()) {
                long idleDelay = (long) (1000.0 / idleCps.getValue());
                attackAllowed = now - lastAttackTimestamp >= idleDelay;
            } else {
                attackAllowed = false;
            }
        } else {
            long requiredDelay = (long) comboDelay.getValue().longValue();
            attackAllowed = now - hitTimestamp >= requiredDelay;
        }

        if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled()) {
            AuraModule.canAttack = attackAllowed;
        }
    }

    private boolean isPredictingHit(EntityLivingBase target) {
        float swing = target.swingProgress;
        boolean swingStarted = swing > swingThreshold.getValue() && prevTargetSwing <= swingThreshold.getValue();
        prevTargetSwing = swing;

        if (!swingStarted) {
            return false;
        }

        return mc.thePlayer.getDistanceToEntity(target) <= predictReach.getValue();
    }

    private void reset() {
        hitReceived = false;
        hitTimestamp = -1L;
        lastAttackTimestamp = -1L;
        prevTargetSwing = 0.0f;
        attackAllowed = true;
    }
}