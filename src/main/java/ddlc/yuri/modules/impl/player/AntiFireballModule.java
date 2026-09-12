package ddlc.yuri.modules.impl.player;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.managers.impl.BadPacketsManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFireball;
import net.minecraft.item.ItemFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0APacketAnimation;

import java.util.HashSet;
import java.util.UUID;

@ModuleInfo(label = "Anti Fireball", description = "Automatically attacks fireballs that are close to you", category = ModuleCategory.PLAYER)
public class AntiFireballModule extends Module {

    public final TimerUtils stopWatch = new TimerUtils();
    public int delay = 50;
    private final HashSet<UUID> attackedFireballs = new HashSet<>();
    private boolean hasNotified = false;

    @EventHook(value = -100)
    public void onPreUpdate(PreUpdateEvent event) {
        detectAndAttackFB();
    }

    public final void detectAndAttackFB() {
        if (BadPacketsManager.bad() || !stopWatch.hasTimeElapsed(delay)) return;
        if (mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().getItem() != null && mc.thePlayer.inventory.getCurrentItem().getItem() instanceof ItemFireball) return;

        boolean fireballNearby = false;
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityFireball && entity.getDistanceToEntity(mc.thePlayer) < 12) {
                fireballNearby = true;
                if (!hasNotified) {
                    Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Fireball detected!");
                    hasNotified = true;
                }
                if (entity.getDistanceToEntity(mc.thePlayer) <= 3 && !attackedFireballs.contains(entity.getUniqueID())) {
                    RotationManager.setRotations(RotationUtils.calculate(entity), 10, RotationManager.MovementFix.NORMAL);
                    PacketUtils.sendPacket(new C0APacketAnimation());
                    PacketUtils.sendPacket(new C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK));
                    attackedFireballs.add(entity.getUniqueID());
                }
                break;
            }
        }
        if (!fireballNearby) hasNotified = false;
    }
}