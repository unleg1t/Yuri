package ddlc.yuri.api.commands.impl;

import ddlc.yuri.api.commands.Command;
import ddlc.yuri.utils.client.LoggingUtils;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class VClipCommand extends Command {
    public VClipCommand() {
        super("vclip", "Makes you vertically clip through a certain amount of blocks.", "v");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .vclip blocks");
            return;
        }

        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + Double.parseDouble(args[0]), mc.thePlayer.posZ);
        LoggingUtils.sendChatMessage("Successfully vertically clipped " + Double.parseDouble(args[0]) + " blocks");
    }
}
