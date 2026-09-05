package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.client.LoggingUtils;

public class HideCommand extends Command {

    public HideCommand() {
        super("hide", "Hides a module", "h");
    }

    @Override
    public void execute(String[] args) {
        if (args.length != 1) {
            LoggingUtils.sendChatMessage("Usage: .hide <module>");
            return;
        }

        final String moduleName = args[0];
        final Module module = Yuri.INSTANCE.getModuleManager().getModule(moduleName);
        if (module != null) {
            module.setHidden(!module.isHidden());
            if (module.isHidden()) {
                LoggingUtils.sendChatMessage("Hid " + module.getLabel() + "!");
            } else {
                LoggingUtils.sendChatMessage("Unhid " + module.getLabel() + "!");
            }
        } else {
            LoggingUtils.sendChatMessage("Cannot find module \"" + moduleName + "\"");
        }
    }
}
