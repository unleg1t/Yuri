package ddlc.yuri.api.commands.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.commands.Command;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.client.LoggingUtils;

public class ModuleCommand extends Command {
    public ModuleCommand() {
        super("module", "Manage settings with commands.", "m");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(String[] args) {

        if (args.length < 3) {
            LoggingUtils.sendChatMessage("Usage: .module <module> <setting> <value>");
            return;
        }

        String moduleName = args[0];
        String settingName = args[1];
        String value = args[2];

        Module module = Yuri.INSTANCE.getModuleManager().getModule(moduleName);

        if (module == null) {
            LoggingUtils.sendChatMessage("Module not found: " + moduleName);
            return;
        }

        Property<?> targetProperty = null;
        String normalizedInput = normalize(settingName);

        for (Property<?> property : module.getElements()) {
            if (normalize(property.getLabel()).equals(normalizedInput)) {
                targetProperty = property;
                break;
            }
        }

        if (targetProperty == null) {
            LoggingUtils.sendChatMessage("Setting not found: " + settingName);
            return;
        }

        try {

            if (targetProperty.getType() == Boolean.class) {
                targetProperty.setValueObj(Boolean.parseBoolean(value));
            } else if (targetProperty instanceof NumberProperty) {

                NumberProperty number = (NumberProperty) targetProperty;
                double parsed = Double.parseDouble(value);

                number.setValue(parsed);

            } else if (targetProperty instanceof ModeProperty) {

                ModeProperty enumProp = (ModeProperty) targetProperty;
                Enum current = (Enum) enumProp.getValue();
                Class<? extends Enum> enumClass = current.getDeclaringClass();

                Enum newValue = null;

                for (Object constant : enumClass.getEnumConstants()) {
                    Enum e = (Enum) constant;

                    if (e.name().equalsIgnoreCase(value)) {
                        newValue = e;
                        break;
                    }
                }

                if (newValue == null) {
                    LoggingUtils.sendChatMessage("Invalid mode");

                    for (Object constant : enumClass.getEnumConstants()) {
                        Enum e = (Enum) constant;
                        LoggingUtils.sendChatMessage("§7- " + e.name());
                    }
                    return;
                }

                enumProp.setValue(newValue);

            } else {
                LoggingUtils.sendChatMessage("Unsupported setting type.");
                return;
            }

            LoggingUtils.sendChatMessage("Set " + module.getLabel() + " " + settingName + " to " + value);
        } catch (Exception e) {
            LoggingUtils.sendChatMessage("Invalid value for setting.");
        }
    }

    public static String normalize(String input) {
        return input == null ? "" : input.replace(" ", "")
                .replace("_", "")
                .toLowerCase();
    }
}
