package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Toggle Sounds", description = "Plays sounds when toggling modules.", category = ModuleCategory.MISC)
public class ToggleSoundsModule extends Module {
    public ModeProperty<ToggleSounds> moduleToggleSounds = new ModeProperty<>("Toggle Sounds", ToggleSounds.NURSULTAN);

    public enum ToggleSounds {
        EVISCERATE("Eviscerate"),
        NURSULTAN("Nursultan"),
        SIGMA("Sigma"),
        AUGUSTUS("Augustus"),
        MINECRAFT("Minecraft"),
        SMOOTH("Smooth"),
        HANABI("Hanabi");

        private final String name;

        ToggleSounds(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
