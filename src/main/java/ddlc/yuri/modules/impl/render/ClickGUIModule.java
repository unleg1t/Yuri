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
        CSGO("CSGO"),
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
        PURPLE("Purple"),
        ROYAL_BLUE("Royal Blue"),
        PASTEL_PURPLE("Pastel Purple"),
        PASTEL_BLUE("Pastel Blue"),
        MIDNIGHT("Midnight"),
        OCEAN_BLUE("Ocean Blue"),
        TURQUOISE("Turquoise"),
        PINK("Pink"),
        LIME("Lime"),
        FOREST_GREEN("Forest Green"),
        GOLD("Gold"),
        ORANGE("Orange"),
        RED("Red"),
        ICE_BLUE("Ice Blue"),
        MONOCHROME("Monochrome");

        public final String name;

        Color(String name) {
            this.name = name;
        }

        @Override
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
            case CSGO:
                mc.displayGuiScreen(Yuri.INSTANCE.getCsgoClickGui());
                break;
        }
    }

    @Override
    public void onDisable() {
       if (mc.currentScreen == Yuri.INSTANCE.getNovolineClickGui() && !Yuri.INSTANCE.getNovolineClickGui().isClosing()) {
            Yuri.INSTANCE.getNovolineClickGui().beginClose();
        } else if (mc.currentScreen == Yuri.INSTANCE.getImGuiClickGui() && !Yuri.INSTANCE.getImGuiClickGui().isClosing()) {
            Yuri.INSTANCE.getImGuiClickGui().beginClose();
        } else if (mc.currentScreen == Yuri.INSTANCE.getCsgoClickGui() && !Yuri.INSTANCE.getCsgoClickGui().isClosing()) {
            Yuri.INSTANCE.getCsgoClickGui().beginClose();
        } else if (mc.currentScreen == Yuri.INSTANCE.getYuriClickGUI() && !Yuri.INSTANCE.getYuriClickGUI().isClosing()) {
            Yuri.INSTANCE.getYuriClickGUI().beginClose();
        }
    }
}