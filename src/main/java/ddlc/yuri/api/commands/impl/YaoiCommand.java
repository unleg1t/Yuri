package ddlc.yuri.api.commands.impl;

import ddlc.yuri.api.commands.Command;
import ddlc.yuri.utils.client.LoggingUtils;

public class YaoiCommand extends Command {
    public YaoiCommand() {
        super("yaoi", ".yaoi makes client visuals gay", "gay");
    }

    @Override
    public void execute(String[] args) {
        LoggingUtils.sendChatMessage(".yaoi makes client visuals gay");
    }
}
