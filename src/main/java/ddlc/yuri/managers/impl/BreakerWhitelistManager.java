package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.block.BlockBed;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;

public class BreakerWhitelistManager implements IMinecraft {
    private static final int SCAN_COOLDOWN_TICKS = 20;
    private static final ArrayList<BlockPos> whitelisted = new ArrayList<>();
    private boolean check = true;
    private int scanCooldown = 0;

    public static boolean isWhitelisted(BlockPos pos) {
        for (BlockPos bp : whitelisted) {
            if (bp.distanceTo(pos) < 1.5) {
                return true;
            }
        }
        return false;
    }

    @EventHook
    private void onPlayerTick(PreUpdateEvent event) {
        if (check && scanCooldown-- <= 0) {
            scanCooldown = SCAN_COOLDOWN_TICKS;
            BlockPos bed = findBed(16);

            if (bed != null) {
                whitelisted.clear();
                whitelisted.add(bed);

                for (EnumFacing facing : EnumFacing.VALUES) {
                    whitelisted.add(bed.offset(facing));
                }

                check = false;
            }
        }
    }

    @EventHook
    private void onPacketReceive(PacketReceivedEvent event) {
        if (event.getPacket() instanceof S02PacketChat) {
            S02PacketChat s02PacketChat = (S02PacketChat) event.getPacket();
            if (s02PacketChat.getChatComponent().getUnformattedText().contains("Protect your bed")) {
                whitelisted.clear();
                check = true;
                scanCooldown = 0;
            }
        }
    }

    private BlockPos findBed(int distance) {
        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int py = MathHelper.floor_double(mc.thePlayer.posY);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);

        for (int dx = -distance; dx < distance; dx++) {
            for (int dy = -distance; dy < distance; dy++) {
                for (int dz = -distance; dz < distance; dz++) {
                    BlockPos cPos = new BlockPos(px + dx, py + dy, pz + dz);
                    if (mc.theWorld.getBlockState(cPos).getBlock() instanceof BlockBed) {
                        return cPos;
                    }
                }
            }
        }

        return null;
    }

}
