package ddlc.yuri.api.commands.impl;


import com.mojang.authlib.GameProfile;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.utils.client.LoggingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;

import java.util.concurrent.atomic.AtomicInteger;

public class FakePlayerCommand extends Command {

    private static final AtomicInteger ID_COUNTER = new AtomicInteger(600000000);

    private static EntityOtherPlayerMP fakePlayer;

    public FakePlayerCommand() {
        super("fakeplayer", "Spawns a fake player at your position.", "fp");
    }

    @Override
    public void execute(String[] args) {

        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .fakeplayer <spawn/delete>");
            return;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            spawn();
        } else if (args[0].equalsIgnoreCase("delete")) {
            delete();
        } else {
            LoggingUtils.sendChatMessage("Usage: .fakeplayer <spawn/delete>");
        }
    }

    private void spawn() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.thePlayer;

        if (player == null) {
            return;
        }

        if (fakePlayer != null) {
            LoggingUtils.sendChatMessage("A fake player is already spawned. Use .fakeplayer delete first.");
            return;
        }

        WorldClient world = mc.theWorld;
        GameProfile profile = player.getGameProfile();

        EntityOtherPlayerMP entity = new EntityOtherPlayerMP(world, profile);
        entity.setEntityId(ID_COUNTER.incrementAndGet());
        entity.setPositionAndRotation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
        entity.rotationYawHead = player.rotationYawHead;
        entity.prevRotationYawHead = player.prevRotationYawHead;

        world.addEntityToWorld(entity.getEntityId(), entity);
        fakePlayer = entity;

        LoggingUtils.sendChatMessage("Fake player spawned.");
    }

    private void delete() {
        Minecraft mc = Minecraft.getMinecraft();

        if (fakePlayer == null) {
            LoggingUtils.sendChatMessage("No fake player is currently spawned.");
            return;
        }

        mc.theWorld.removeEntityFromWorld(fakePlayer.getEntityId());
        fakePlayer = null;

        LoggingUtils.sendChatMessage("Fake player deleted.");
    }
}