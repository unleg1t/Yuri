package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.api.config.Config;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.utils.client.LoggingUtils;

import java.io.File;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Manage your client configs, binds, and visuals.", "c");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            LoggingUtils.sendChatMessage("Usage: .config save/load/list/delete/binds/visuals");
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "save": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config save <config name>");
                    return;
                }

                String name = args[1];
                Yuri.INSTANCE.getConfigManager().saveConfig(name);
                LoggingUtils.sendChatMessage("Successfully saved config " + name + "!");
                break;
            }

            case "load": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config load <config name>");
                    return;
                }

                String name = args[1];
                if (Yuri.INSTANCE.getConfigManager().loadConfig(name)) {
                    LoggingUtils.sendChatMessage("Successfully loaded config " + name + "!");
                } else {
                    LoggingUtils.sendChatMessage("Failed to load config " + name + ".");
                }
                break;
            }

            case "list": {
                listConfigs();
                break;
            }

            case "delete": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config delete <config name>");
                    return;
                }

                String name = args[1];
                if (deleteConfig(name)) {
                    LoggingUtils.sendChatMessage("Successfully deleted config profile " + name + ".");
                } else {
                    LoggingUtils.sendChatMessage("The config " + name + " does not exist.");
                }
                break;
            }

            case "binds": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config binds save/load");
                    return;
                }

                String action = args[1].toLowerCase();
                if (action.equals("save")) {
                    if (Yuri.INSTANCE.getConfigManager().getBindsConfig().saveToFile()) {
                        LoggingUtils.sendChatMessage("Successfully saved binds config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to save binds config.");
                    }
                } else if (action.equals("load")) {
                    if (Yuri.INSTANCE.getConfigManager().getBindsConfig().loadFromFile()) {
                        LoggingUtils.sendChatMessage("Successfully loaded binds config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to load binds config.");
                    }
                } else {
                    LoggingUtils.sendChatMessage("Usage: .config binds save/load");
                }
                break;
            }

            case "visuals": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .config visuals save/load");
                    return;
                }

                String action = args[1].toLowerCase();
                if (action.equals("save")) {
                    if (Yuri.INSTANCE.getConfigManager().getVisualsConfig().saveToFile()) {
                        LoggingUtils.sendChatMessage("Successfully saved visuals config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to save visuals config.");
                    }
                } else if (action.equals("load")) {
                    if (Yuri.INSTANCE.getConfigManager().getVisualsConfig().loadFromFile()) {
                        LoggingUtils.sendChatMessage("Successfully loaded visuals config!");
                    } else {
                        LoggingUtils.sendChatMessage("Failed to load visuals config.");
                    }
                } else {
                    LoggingUtils.sendChatMessage("Usage: .config visuals save/load");
                }
                break;
            }

            default:
                LoggingUtils.sendChatMessage("Usage: .config save/load/list/delete/binds/visuals");
        }
    }

    private void listConfigs() {
        if (Yuri.INSTANCE.getConfigManager().getElements().isEmpty()) {
            LoggingUtils.sendChatMessage("No configs found.");
            return;
        }
        for (Config config : Yuri.INSTANCE.getConfigManager().getElements()) {
            LoggingUtils.sendChatMessage(config.getName());
        }
    }

    private boolean deleteConfig(String name) {
        Config config = Yuri.INSTANCE.getConfigManager().findConfig(name);
        if (config == null) {
            File file = new File(ConfigManager.CONFIGS_DIR, name + ".json");
            return file.exists() && file.delete();
        }
        return Yuri.INSTANCE.getConfigManager().deleteConfig(name);
    }
}