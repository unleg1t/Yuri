package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.client.LoggingUtils;

public class ToggleCommand extends Command {
    public ToggleCommand() {
        super("toggle",
                "Toggle modules by commands.", "t");
    }

    @Override
    public void execute(String[] args) {

        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .toggle <module>");
            return;
        }

        final String moduleName = args[0];
        final Module module = Yuri.INSTANCE.getModuleManager().getModule(moduleName);

        if (module != null) {
            module.toggle();
            LoggingUtils.sendChatMessage("Toggled " + module.getLabel() + "!");
        } else {
            LoggingUtils.sendChatMessage("Cannot find module \"" + moduleName + "\"");
        }
    }
}
