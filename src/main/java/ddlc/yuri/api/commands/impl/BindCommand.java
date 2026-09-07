package ddlc.yuri.api.commands.impl;


import java.util.ArrayList;
import java.util.List;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.client.KeyUtil;
import ddlc.yuri.utils.client.LoggingUtils;
import org.lwjgl.input.Keyboard;

public class BindCommand extends Command {
    public BindCommand() {
        super("bind", "Binds a module to a key.", "b");
    }

    @Override
    public void execute(String[] args) {

        if (args.length == 2) {
            Module module = Yuri.INSTANCE.getModuleManager().getModule(args[0]);

            String keyName = args[1];

            if (module != null) {
                int keyCode = KeyUtil.stringToKey(keyName);

                module.setKey(keyCode);

                LoggingUtils.sendChatMessage(
                        "Bound " + module.getLabel() + " to " + KeyUtil.getKeyName(keyCode) + "."
                );
            } else {
                LoggingUtils.sendChatMessage("Module \"" + args[0] + "\" was not found.");
            }
            return;
        }

        if (args.length == 1) {

            if (args[0].equalsIgnoreCase("clear")) {

                for (Module m : Yuri.INSTANCE.getModuleManager().getModules()) {
                    m.setKey(Keyboard.KEY_NONE);
                }

                LoggingUtils.sendChatMessage("Cleared all binds.");
            }

            else if (args[0].equalsIgnoreCase("list")) {

                List<Module> boundModules = new ArrayList<>();

                for (Module m : Yuri.INSTANCE.getModuleManager().getModules()) {
                    if (m.getKey() != Keyboard.KEY_NONE) {
                        boundModules.add(m);
                    }
                }

                if (!boundModules.isEmpty()) {
                    LoggingUtils.sendChatMessage("Current binds:");

                    for (Module module : boundModules) {
                        LoggingUtils.sendChatMessage(
                                module.getLabel() + ": " + KeyUtil.getKeyName(module.getKey())
                        );
                    }
                } else {
                    LoggingUtils.sendChatMessage("No modules are currently bound.");
                }
            }
        }
    }

    public static int stringToKey(String keyName) {
        return KeyUtil.stringToKey(keyName);
    }

}
