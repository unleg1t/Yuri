package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Camera", category = ModuleCategory.RENDER, description = "Changes the rendered hand appearance properties")
public class CameraModule extends Module {

    public static NumberProperty x = new NumberProperty("X", 0.0F, -2.0F, 2.0F, 0.05f);
    public static NumberProperty y = new NumberProperty("Y", 0.0F, -2.0F, 2.0F, 0.05f);
    public static NumberProperty z = new NumberProperty("Z", 0.0F, -2.0F, 2.0F, 0.05f);
    public static NumberProperty scale = new NumberProperty("Scale", 1.0F, 0.1F, 2.0F, 0.1F);
    public static NumberProperty slowdown = new NumberProperty("Slowdown", 1.0F, 1.0F, 15.0F, 1.0F);
    public static Property<Boolean> noHurtCamera = new Property<>("No Hurt Camera", true);
    public static Property<Boolean> noFireOverlay = new Property<>("No Fire Overlay", true);
    public static Property<Boolean> noBlindness = new Property<>("No Blindness", true);
    public static Property<Boolean> noBossBar = new Property<>("No Boss Bar", true);
    public final Property<Boolean> fullBright = new Property<>("Full Bright", false);

    private float originalGamma;

    @Override
    public void onEnable() {
        if (fullBright.getValue()) {
            originalGamma = mc.gameSettings.gammaSetting;
            mc.gameSettings.gammaSetting = 100;
        }
    }

    @Override
    public void onDisable() {
        if (fullBright.getValue()) {
            mc.gameSettings.gammaSetting = originalGamma > 10 ? 1 : originalGamma;
        }
    }
}