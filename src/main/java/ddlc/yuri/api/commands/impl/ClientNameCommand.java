package ddlc.yuri.api.commands.impl;


import ddlc.yuri.api.commands.Command;
import ddlc.yuri.modules.impl.render.WatermarkModule;
import ddlc.yuri.utils.client.LoggingUtils;

public class ClientNameCommand extends Command {

    public ClientNameCommand() {
        super("clientname", "Changes the client name in watermarks", "cname");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .clientname <name>");
            return;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            nameBuilder.append(args[i]);
            if (i < args.length - 1) nameBuilder.append(" ");
        }
        String newName = nameBuilder.toString();

        WatermarkModule.name.setValue(newName);
        LoggingUtils.sendChatMessage("Set client name to: " + newName);
    }
}
