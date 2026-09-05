package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.imgui.style.ImGuiStyleType;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "ClickGUI", category = ModuleCategory.RENDER, key = Keyboard.KEY_RSHIFT, description = "Opens the click GUI")
public class ClickGUIModule extends Module implements IMinecraft {

    public static final ModeProperty<Color> color = new ModeProperty<>("Color", Color.YURI);
    public static final NumberProperty colorSpeed = new NumberProperty("Color Speed", 5, 1, 10, 1);
    public static final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.YURI);
    public static final ModeProperty<ImGuiStyleType> style = new ModeProperty<>("Style", ImGuiStyleType.REGULAR, () -> mode.getValue() == Mode.IMGUI);
    private final Property<Boolean> closePrevious = new Property<>("Close Previous", true, () -> mode.getValue() == Mode.NOVOLINE);
    public static final Property<Boolean> logoInGuis = new Property<>("Logo In GUIS", false);

    public enum Mode {
        YURI("Yuri"),
        IMGUI("ImGui"),
        NOVOLINE("Novoline");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public enum Color {
        YURI("Yuri"),
        NOVOLINE("Novoline"),
        RAINBOW("Rainbow"),
        ASTOLFO("Astolfo"),
        TENACITY("Tenacity"),
        SUNSET("Sunset"),
        AMETHYST("Amethyst"),
        ROYAL("Royal"),
        LAVENDER("Lavender"),
        AZURE("Azure"),
        INDIGO("Indigo"),
        OCEAN("Ocean"),
        CRYSTAL("Crystal"),
        PETAL("Petal"),
        CITRUS("Citrus"),
        EVERGREEN("Evergreen"),
        LEMON("Lemon"),
        EMBER("Ember"),
        CRIMSON("Crimson"),
        ICE("Ice"),
        GRAPHITE("Graphite");

        public final String name;

        Color(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public Property<Boolean> getClosePrevious() {
        return closePrevious;
    }

    @Override
    public void onEnable() {
        switch (mode.getValue()) {
            case NOVOLINE:
                mc.displayGuiScreen(Yuri.INSTANCE.getNovolineClickGui());
                break;
            case YURI:
                mc.displayGuiScreen(Yuri.INSTANCE.getYuriClickGUI());
                break;
            case IMGUI:
                mc.displayGuiScreen(Yuri.INSTANCE.getImGuiClickGui());
                break;
        }
    }

    @Override
    public void onDisable() {
       if (mc.currentScreen == Yuri.INSTANCE.getNovolineClickGui() && !Yuri.INSTANCE.getNovolineClickGui().isClosing()) {
            Yuri.INSTANCE.getNovolineClickGui().beginClose();
        } else if (mc.currentScreen == Yuri.INSTANCE.getImGuiClickGui() && !Yuri.INSTANCE.getImGuiClickGui().isClosing()) {
            Yuri.INSTANCE.getImGuiClickGui().beginClose();
        } else if (mc.currentScreen == Yuri.INSTANCE.getYuriClickGUI() && !Yuri.INSTANCE.getYuriClickGUI().isClosing()) {
            Yuri.INSTANCE.getYuriClickGUI().beginClose();
        }
    }
}