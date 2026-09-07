package ddlc.yuri.modules.impl.combat.velocity.impl;

import ddlc.yuri.api.events.impl.player.HitSlowDownEvent;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.modules.impl.combat.VelocityModule;
import ddlc.yuri.modules.impl.combat.velocity.VelocityMode;
import net.minecraft.entity.EntityLivingBase;

public class IntaveVelocity implements VelocityMode {
    private final VelocityModule parent;

    public IntaveVelocity(VelocityModule parent) {
        this.parent = parent;
    }

    private boolean hitSlowdown = false;

    @Override
    public void onAttack(PlayerAttackEvent event) {
        switch (parent.intaveMode.getValue()) {
            case INTAVE_13:
                if (event.target instanceof EntityLivingBase && mc.thePlayer.hurtTime > 0 && !isInLiquidOrWeb()) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionX *= 0.52;
                        mc.thePlayer.motionZ *= 0.52;
                    } else {
                        mc.thePlayer.motionX *= 0.8;
                        mc.thePlayer.motionZ *= 0.8;
                    }
                }
                break;
            case INTAVE_LATEST:
                if (mc.thePlayer.hurtTime > 0) {
                    if (!hitSlowdown && mc.thePlayer.isSprinting()) {
                        mc.thePlayer.motionX *= 0.6D;
                        mc.thePlayer.motionZ *= 0.6D;
                        mc.thePlayer.setSprinting(false);
                    }
                    hitSlowdown = false;
                }
                break;
        }
    }

    @Override
    public void onHitSlowdown(HitSlowDownEvent event) {
        hitSlowdown = true;
    }

    private boolean isInLiquidOrWeb() {
        return mc.thePlayer.isInWater() || mc.thePlayer.isInLava() || mc.thePlayer.isInWeb;
    }
}
