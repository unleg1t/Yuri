package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.utils.client.LoggingUtils;

public class BindsCommand extends Command {

    public BindsCommand() {
        super("binds", "Save or load key binds.", "bindconfig");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            LoggingUtils.sendChatMessage("Usage: .binds save/load");
            return;
        }

        String action = args[0].toLowerCase();
        if (action.equals("save")) {
            if (Yuri.INSTANCE.getConfigManager().getBindsConfig().saveToFile()) {
                LoggingUtils.sendChatMessage("Successfully saved key binds!");
            } else {
                LoggingUtils.sendChatMessage("Failed to save key binds.");
            }
        } else if (action.equals("load")) {
            if (Yuri.INSTANCE.getConfigManager().getBindsConfig().loadFromFile()) {
                LoggingUtils.sendChatMessage("Successfully loaded key binds!");
            } else {
                LoggingUtils.sendChatMessage("Failed to load key binds.");
            }
        } else {
            LoggingUtils.sendChatMessage("Usage: .binds save/load");
        }
    }
}