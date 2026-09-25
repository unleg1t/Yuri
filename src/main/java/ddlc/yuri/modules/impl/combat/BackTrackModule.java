
package ddlc.yuri.modules.impl.combat;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.TimerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.Vec3;

import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleInfo(label = "Back Track", category = ModuleCategory.COMBAT, description = "Allows you to backtrack players' positions")
public class BackTrackModule extends Module {

    public final NumberProperty minRange = new NumberProperty("Min Range", 2.0, 0.0, 6.0, 0.1);
    public final NumberProperty maxRange = new NumberProperty("Max Range", 4.0, 0.0, 10.0, 0.1);
    public final NumberProperty maxDelay = new NumberProperty("Max Delay", 400.0, 40.0, 2000.0, 10.0);
    public final Property<Boolean> onlyKillAura = new Property<>("Only Kill Aura", true);

    private volatile EntityLivingBase target;
    private volatile Vec3 spoofedPosition;
    private volatile boolean spoofing;

    private final TimerUtils timer = new TimerUtils();
    private final ConcurrentLinkedQueue<Packet<INetHandlerPlayClient>> packetBuffer = new ConcurrentLinkedQueue<>();


    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            releaseBuffer();
            spoofing = false;
            return;
        }

        target = resolveTarget();

        if (target == null) {
            releaseBuffer();
            spoofing = false;
            return;
        }

        float distance = calculateDistance(target.posX, target.posY, target.posZ);
        spoofing = distance > minRange.getValue() && distance < maxRange.getValue() && !timer.hasTimeElapsed(maxDelay.getValue());

        setSuffix(maxDelay.getValue().intValue() + "ms");

        if (!spoofing) {
            releaseBuffer();
            timer.reset();
            spoofedPosition = null;
        }
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || target == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof S08PacketPlayerPosLook) {
            releaseBuffer();
            spoofing = false;
            return;
        }

        if (packet instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
                releaseBuffer();
                spoofing = false;
                timer.reset();
                spoofedPosition = null;
                return;
            }
        }

        if (!spoofing || !shouldBufferPacket(packet)) return;

        packetBuffer.add((Packet<INetHandlerPlayClient>) packet);
        event.setCancelled(true);

        if (packet instanceof S14PacketEntity) {
            S14PacketEntity p = (S14PacketEntity) packet;
            if (p.getEntity(mc.theWorld) == target) {
                backertrack(new Vec3(
                        target.posX + p.getPosX() / 32.0,
                        target.posY + p.getPosY() / 32.0,
                        target.posZ + p.getPosZ() / 32.0
                ));
            }
        } else if (packet instanceof S18PacketEntityTeleport) {
            S18PacketEntityTeleport p = (S18PacketEntityTeleport) packet;
            if (p.getEntityId() == target.getEntityId()) {
                backertrack(new Vec3(p.getX() / 32.0, p.getY() / 32.0, p.getZ() / 32.0));
            }
        }
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null || target == null || !spoofing) return;

        Packet<?> packet = event.getPacket();
        Vec3 position = spoofedPosition;

        if (packet instanceof C02PacketUseEntity && position != null) {
            if (((C02PacketUseEntity) packet).getEntityFromWorld(mc.theWorld) == target) {
                target.setPosition(position.xCoord, position.yCoord, position.zCoord);
            }
        }
    }

/*    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null || target == null || !spoofing || spoofedPosition == null) return;

        double drawX = spoofedPosition.xCoord - mc.getRenderManager().viewerPosX;
        double drawY = spoofedPosition.yCoord - mc.getRenderManager().viewerPosY;
        double drawZ = spoofedPosition.zCoord - mc.getRenderManager().viewerPosZ;

        double half = target.width / 2.0;
        double height = target.height;

        AxisAlignedBB box = new AxisAlignedBB(
                drawX - half, drawY, drawZ - half,
                drawX + half, drawY + height, drawZ + half);

        Color color = ColorManager.getColors().getFirst();

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        GlStateManager.color(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, 0.16f);
        RenderUtils.drawBoundingBox(box);

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
    }*/

    private void backertrack(Vec3 position) {
        spoofedPosition = position;
    }

    private EntityLivingBase resolveTarget() {
        if (onlyKillAura.getValue()) {
            AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
            return aura != null && aura.isEnabled() ? AuraModule.target : null;
        }

        Entity entity = TargetManager.getTarget();
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    private float calculateDistance(double x, double y, double z) {
        Vec3 playerEyes = mc.thePlayer.getPositionEyes(mc.timer.renderPartialTicks);
        return (float) playerEyes.distanceTo(new Vec3(x, y, z));
    }

    private boolean shouldBufferPacket(Packet<?> packet) {
        return packet instanceof S14PacketEntity || packet instanceof S18PacketEntityTeleport;
    }

    private void releaseBuffer() {
        INetHandlerPlayClient handler = mc.getNetHandler();

        while (!packetBuffer.isEmpty()) {
            Packet<INetHandlerPlayClient> packet = packetBuffer.poll();
            if (packet == null || handler == null) continue;

            try {
                packet.processPacket(handler);
            } catch (Exception ignored) {
            }
        }

        packetBuffer.clear();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        packetBuffer.clear();
        spoofedPosition = null;
        spoofing = false;
        target = null;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        spoofing = false;
        spoofedPosition = null;
        packetBuffer.clear();
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        releaseBuffer();
        spoofedPosition = null;
        spoofing = false;
        target = null;
    }
}
