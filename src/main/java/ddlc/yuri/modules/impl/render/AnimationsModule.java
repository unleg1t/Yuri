package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Animations", category = ModuleCategory.RENDER, description = "Changes the rendered hand appearance properties")
public class AnimationsModule extends Module {

    public enum AnimationMode {
        OLD("Old"),
        EXHIBITION("Exhibition"),
        NOVOLINE("Novoline"),
        SPIN("Spin"),
        SMOOTH("Smooth"),
        LEAKED("Leaked"),
        INERTIA("Inertia"),
        PUNCH("Punch"),
        SWING("Swing"),
        STELLA("Stella"),
        STYLES("Styles"),
        ETHEREAL("Ethereal"),
        EXPENSIVE("Expensive");
        public final String name;

        AnimationMode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public static ModeProperty<AnimationMode> mode = new ModeProperty<>("Block Animations", AnimationMode.OLD);
    public static Property<Boolean> fluxSwing = new Property<>("Flux Swing", false);
    public static Property<Boolean> dontResetBlock = new Property<>("Dont Reset Block", true);
    public static Property<Boolean> swingEating = new Property<>("Swing While Eating", false);
}