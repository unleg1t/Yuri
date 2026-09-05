package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.utils.client.LoggingUtils;

public class VisualsCommand extends Command {

    public VisualsCommand() {
        super("visuals", "Save or load visual modules and HUD elements.", "visualconfig");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            LoggingUtils.sendChatMessage("Usage: .visuals save/load");
            return;
        }

        String action = args[0].toLowerCase();
        if (action.equals("save")) {
            if (Yuri.INSTANCE.getConfigManager().getVisualsConfig().saveToFile()) {
                LoggingUtils.sendChatMessage("Successfully saved visual config!");
            } else {
                LoggingUtils.sendChatMessage("Failed to save visual config.");
            }
        } else if (action.equals("load")) {
            if (Yuri.INSTANCE.getConfigManager().getVisualsConfig().loadFromFile()) {
                LoggingUtils.sendChatMessage("Successfully loaded visual config!");
            } else {
                LoggingUtils.sendChatMessage("Failed to load visual config.");
            }
        } else {
            LoggingUtils.sendChatMessage("Usage: .visuals save/load");
        }
    }
}