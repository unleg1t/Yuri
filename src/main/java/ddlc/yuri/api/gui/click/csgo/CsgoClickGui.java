package ddlc.yuri.api.gui.click.csgo;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.config.Config;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.config.GithubConfigFetcher;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.api.properties.impl.Representation;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.client.KeyUtil;
import ddlc.yuri.utils.render.animations.Direction;
import ddlc.yuri.utils.render.animations.impl.DecelerateAnimation;
import ddlc.yuri.utils.render.imgui.ImGuiManager;
import ddlc.yuri.utils.render.imgui.style.ImGuiStyles;
import imgui.ImDrawList;
import imgui.ImFont;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CsgoClickGui extends GuiScreen {

    private static final int WINDOW_FLAGS = ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoScrollbar;

    private final DecelerateAnimation openAnimation = new DecelerateAnimation(280, 1.0D, Direction.FORWARDS);
    private final Set<Module> openModules = new HashSet<Module>();
    private final Map<Property<?>, ImString> stringBuffers = new HashMap<Property<?>, ImString>();
    private final List<String> remoteConfigs = new ArrayList<String>();
    private final ImString configName = new ImString(64);
    private Property<Integer> listeningKeybind;
    @Getter
    private boolean closing;

    private int currentPage;
    private String selectedLocalConfig;
    private String statusText = "";
    private boolean fetchedRemote;
    private boolean resizing;
    private float resizeStartMouseX;
    private float resizeStartMouseY;
    private float resizeStartWidth;
    private float resizeStartHeight;

    @Override
    public void initGui() {
        ImGuiManager.get().init(ImGuiStyles.csgo());
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;
        CsgoAnim.reset();
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        float progress = Math.max(0f, Math.min(1f, openAnimation.getOutput().floatValue()));
        ImGuiManager.get().newFrame(mc.displayWidth, mc.displayHeight);
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, progress);
        buildWindow();
        ImGui.popStyleVar();
        ImGuiManager.get().render();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void buildWindow() {
        ImGui.setNextWindowSize(CsgoTheme.WINDOW_WIDTH, CsgoTheme.WINDOW_HEIGHT, ImGuiCond.Once);
        ImGui.setNextWindowSizeConstraints(CsgoTheme.MIN_WINDOW_WIDTH, CsgoTheme.MIN_WINDOW_HEIGHT, 1600f, 1200f);
        ImGui.begin("csgo", WINDOW_FLAGS);

        float winX = ImGui.getWindowPosX();
        float winY = ImGui.getWindowPosY();
        float winW = ImGui.getWindowWidth();
        float winH = ImGui.getWindowHeight();
        CsgoTheme.updateScale(winW, winH);
        ImFont compact = ImGuiManager.get().getCompactFont(CsgoTheme.fontSize());
        if (compact != null) {
            ImGui.pushFont(compact);
        }
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRect(winX, winY, winX + winW, winY + winH, CsgoTheme.col(ImGuiCol.BorderShadow));
        draw.addRect(winX + 1, winY + 1, winX + winW - 1, winY + winH - 1, CsgoTheme.col(ImGuiCol.Border));

        drawHeader(winX, winY, winW);
        drawBody(winW, winH);
        drawFooter(winX, winY, winW, winH);
        handleResize(winX, winY, winW, winH);

        if (compact != null) {
            ImGui.popFont();
        }
        ImGui.end();
    }

    private void drawHeader(float winX, float winY, float winW) {
        ImGui.setCursorPos(2, 2);
        ImGui.beginChild("header", winW - 4, CsgoTheme.headerHeight(), false);
        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        float w = ImGui.getWindowWidth();
        float h = ImGui.getWindowHeight();
        ImDrawList draw = ImGui.getWindowDrawList();

        draw.addQuadFilled(x + CsgoTheme.s(60f), y, x + CsgoTheme.s(30f), y, x + CsgoTheme.s(30f), y + h, x + CsgoTheme.s(50f), y + h, CsgoTheme.scheme(0.03f));
        draw.addQuadFilled(x + CsgoTheme.s(175f), y, x + CsgoTheme.s(130f), y, x + CsgoTheme.s(120f), y + h, x + CsgoTheme.s(165f), y + h, CsgoTheme.scheme(0.03f));
        draw.addLine(x, y + h - 3, x + w, y + h - 3, CsgoTheme.col(ImGuiCol.BorderShadow));
        draw.addLine(x, y + h - 2, x + w, y + h - 2, CsgoTheme.col(ImGuiCol.Border));
        draw.addRectFilledMultiColor(x + w / 2f, y + h - 1, x + w, y + h,
                unsigned(CsgoTheme.scheme(0f)),
                unsigned(CsgoTheme.scheme(1f)),
                unsigned(CsgoTheme.scheme(1f)),
                unsigned(CsgoTheme.scheme(0f)));
        CsgoWidgets.addText(x + CsgoTheme.s(6f), y + h / 2f - CsgoTheme.fontSize() / 2f - 2f, CsgoTheme.scheme(1f), "yuri client");
        ImGui.endChild();
    }

    private void drawBody(float winW, float winH) {
        float pad = CsgoTheme.s(15f);
        float header = CsgoTheme.headerHeight();
        float footer = CsgoTheme.footerHeight();
        ImGui.setCursorPos(pad, header + CsgoTheme.s(12f));
        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, pad, pad);
        ImGui.beginChild("main wrapper", winW - pad * 2f, winH - header - footer - CsgoTheme.s(26f), false, ImGuiWindowFlags.NoBackground);

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, CsgoTheme.s(3f), CsgoTheme.s(3f));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, CsgoTheme.s(8f), CsgoTheme.s(8f));
        ImGui.beginChild("pages", ImGui.getWindowWidth() / 3f, ImGui.getWindowHeight(), true, ImGuiWindowFlags.AlwaysUseWindowPadding);

        ModuleCategory[] categories = ModuleCategory.values();
        int pageCount = categories.length + 1;
        float pageWidth = ImGui.calcItemWidth();
        for (int i = 0; i < pageCount; i++) {
            String label = i < categories.length ? categories[i].getName() : "Configs";
            if (CsgoWidgets.page(label, currentPage == i, pageWidth)) {
                currentPage = i;
                CsgoAnim.set("page-content", 0f);
            }
        }

        ImGui.endChild();
        ImGui.popStyleVar(2);
        ImGui.sameLine();

        ImGui.beginChild("main", ImGui.getContentRegionAvailX(), ImGui.getWindowHeight(), false, ImGuiWindowFlags.NoBackground);
        float content = CsgoAnim.approach("page-content", 1f, 0.16f);
        ImGui.setCursorPosY(ImGui.getCursorPosY() + (1f - content) * CsgoTheme.s(8f));
        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, ImGui.getStyle().getAlpha() * (0.35f + content * 0.65f));
        if (currentPage < categories.length) {
            buildCategory(categories[currentPage]);
        } else {
            if (!fetchedRemote) {
                fetchedRemote = true;
                refreshRemote();
            }
            buildConfigs();
        }
        ImGui.popStyleVar();
        ImGui.endChild();

        ImGui.endChild();
        ImGui.popStyleVar();
    }

    private void drawFooter(float winX, float winY, float winW, float winH) {
        float grip = CsgoTheme.s(16f);
        ImGui.setCursorPos(2, winH - CsgoTheme.footerHeight() - 2f);
        ImGui.beginChild("footer", winW - 4 - grip, CsgoTheme.footerHeight(), false);
        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        float w = ImGui.getWindowWidth();
        float h = ImGui.getWindowHeight();
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addLine(x, y, x + w, y, CsgoTheme.col(ImGuiCol.Border));
        draw.addLine(x, y + 1, x + w, y + 1, CsgoTheme.col(ImGuiCol.BorderShadow));

        Date now = new Date();
        String monthDay = new SimpleDateFormat("MMM  d", Locale.US).format(now);
        String year = new SimpleDateFormat("yyyy", Locale.US).format(now);
        float textY = y + h / 2f - CsgoTheme.fontSize() / 2f - 1f;
        CsgoWidgets.addText(x + CsgoTheme.s(6f), textY, CsgoTheme.col(ImGuiCol.TextDisabled), monthDay);
        CsgoWidgets.addText(x + CsgoTheme.s(6f) + CsgoWidgets.textSize(monthDay + " ").x, textY, CsgoTheme.scheme(1f), year);

        String right = Yuri.VERSION + " " + Yuri.BUILD;
        ImVec2 rightSize = CsgoWidgets.textSize(right);
        CsgoWidgets.addText(x + w - rightSize.x - CsgoTheme.s(6f), textY, CsgoTheme.col(ImGuiCol.TextDisabled), right);
        ImGui.endChild();
    }

    private void handleResize(float winX, float winY, float winW, float winH) {
        float grip = Math.max(14f, CsgoTheme.s(16f));
        float gx = winX + winW - grip;
        float gy = winY + winH - grip;
        ImDrawList draw = ImGui.getWindowDrawList();
        int line = CsgoTheme.scheme(0.85f);
        draw.addLine(gx + 4f, winY + winH - 3f, winX + winW - 3f, gy + 4f, line);
        draw.addLine(gx + 8f, winY + winH - 3f, winX + winW - 3f, gy + 8f, line);
        draw.addLine(gx + 12f, winY + winH - 3f, winX + winW - 3f, gy + 12f, line);

        ImGui.setCursorScreenPos(gx, gy);
        ImGui.invisibleButton("##csgo-resize", grip, grip);
        boolean hovered = ImGui.isItemHovered();
        boolean held = ImGui.isItemActive();
        if (hovered || held) {
            draw.addTriangleFilled(gx + 2f, winY + winH - 2f, winX + winW - 2f, winY + winH - 2f, winX + winW - 2f, gy + 2f, CsgoTheme.scheme(held ? 0.45f : 0.2f));
        }

        if (held) {
            float mx = ImGui.getMousePosX();
            float my = ImGui.getMousePosY();
            if (!resizing) {
                resizing = true;
                resizeStartMouseX = mx;
                resizeStartMouseY = my;
                resizeStartWidth = winW;
                resizeStartHeight = winH;
            }
            float nextW = Math.max(CsgoTheme.MIN_WINDOW_WIDTH, Math.min(1600f, resizeStartWidth + (mx - resizeStartMouseX)));
            float nextH = Math.max(CsgoTheme.MIN_WINDOW_HEIGHT, Math.min(1200f, resizeStartHeight + (my - resizeStartMouseY)));
            ImGui.setWindowSize(nextW, nextH);
        } else {
            resizing = false;
        }
    }

    private void buildCategory(ModuleCategory category) {
        CsgoWidgets.beginChild(category.getName(), 0, 0);
        List<Module> modules = Yuri.INSTANCE.getModuleManager().getModulesForCategory(category);
        for (Module module : modules) {
            buildModule(module);
        }
        CsgoWidgets.endChild();
    }

    private void buildModule(Module module) {
        ImGui.pushID(module.getLabel());
        boolean enabled = module.isEnabled();
        if (CsgoWidgets.checkbox("enable", module.getLabel(), enabled)) {
            module.toggle();
        }

        if (!module.getElements().isEmpty()) {
            ImGui.sameLine();
            boolean opened = openModules.contains(module);
            if (CsgoWidgets.button("toggle-settings", opened ? "-" : "+", CsgoTheme.s(18f), CsgoTheme.s(16f))) {
                if (opened) {
                    openModules.remove(module);
                } else {
                    openModules.add(module);
                }
            }

            if (openModules.contains(module)) {
                ImGui.indent(CsgoTheme.s(12f));
                for (Property<?> property : module.getElements()) {
                    if (property.isAvailable()) {
                        buildProperty(property);
                    }
                }
                ImGui.unindent(CsgoTheme.s(12f));
            }
        }
        ImGui.popID();
    }

    @SuppressWarnings("unchecked")
    private void buildProperty(Property<?> property) {
        ImGui.pushID(property.getLabel());
        if (property instanceof NumberProperty) {
            NumberProperty numberProperty = (NumberProperty) property;
            float[] holder = {numberProperty.getValue().floatValue()};
            if (CsgoWidgets.slider("num", property.getLabel(), holder, (float) numberProperty.getMin(), (float) numberProperty.getMax(), sliderFormat(numberProperty))) {
                double step = numberProperty.getIncrement();
                double val = holder[0];
                if (step > 0.0D) {
                    val = Math.round(val / step) * step;
                }
                numberProperty.setValue(Math.max(numberProperty.getMin(), Math.min(numberProperty.getMax(), val)));
            }
        } else if (property.getValue() instanceof Boolean) {
            Property<Boolean> booleanProperty = (Property<Boolean>) property;
            if (CsgoWidgets.checkbox("bool", property.getLabel(), booleanProperty.getValue())) {
                booleanProperty.setValue(!booleanProperty.getValue());
            }
        } else if (property instanceof ModeProperty) {
            ModeProperty<?> modeProperty = (ModeProperty<?>) property;
            Enum<?>[] values = modeProperty.getValues();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                names[i] = values[i].toString();
            }
            int[] current = {modeProperty.getValue().ordinal()};
            if (CsgoWidgets.combo("mode", property.getLabel(), modeProperty.getValue().toString(), names, current)) {
                modeProperty.setValue(current[0]);
            }
        } else if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
            Enum<?>[] values = multiModeProperty.getValues();
            String preview = selectedPreview(multiModeProperty);
            if (CsgoWidgets.button("multi", preview + "  " + property.getLabel(), ImGui.calcItemWidth(), CsgoTheme.s(20f))) {
                ImGui.openPopup("##multi");
            }
            if (ImGui.beginPopup("##multi")) {
                for (int i = 0; i < values.length; i++) {
                    Enum<?> value = values[i];
                    if (!multiModeProperty.isVisible(value)) {
                        continue;
                    }
                    if (CsgoWidgets.checkbox("m" + i, value.toString(), multiModeProperty.isSelected(value))) {
                        multiModeProperty.setValue(i);
                    }
                }
                ImGui.endPopup();
            }
        } else if (property instanceof DescriptorProperty) {
            ImGui.textDisabled(property.getLabel());
        } else if (property.getValue() instanceof String) {
            Property<String> stringProperty = (Property<String>) property;
            ImString buffer = stringBuffers.computeIfAbsent(property, p -> new ImString(stringProperty.getValue(), 256));
            if (!buffer.get().equals(stringProperty.getValue()) && !ImGui.isItemActive()) {
                buffer.set(stringProperty.getValue());
            }
            if (CsgoWidgets.input("str", property.getLabel(), buffer)) {
                stringProperty.setValue(buffer.get());
            }
        } else if (property.getValue() instanceof Integer) {
            Property<Integer> keybindProperty = (Property<Integer>) property;
            boolean listening = listeningKeybind == keybindProperty;
            String label = listening ? ".." : KeyUtil.getKeyName(keybindProperty.getValue());
            if (CsgoWidgets.button("bind", property.getLabel() + ": " + label, ImGui.calcItemWidth(), CsgoTheme.s(20f))) {
                listeningKeybind = listening ? null : keybindProperty;
            }
            if (ImGui.isItemHovered() && ImGui.isMouseClicked(2)) {
                listeningKeybind = listening ? null : keybindProperty;
            }
        }
        ImGui.popID();
    }

    private void buildConfigs() {
        CsgoWidgets.beginChild("Configs", 0, 0);

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, CsgoTheme.s(11f), CsgoTheme.s(9f));
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, CsgoTheme.s(15f), CsgoTheme.s(13f));
        ImGui.beginChild("config wrapper", ImGui.getContentRegionAvailX(), ImGui.getContentRegionAvailY(), false, ImGuiWindowFlags.AlwaysUseWindowPadding);

        float width = ImGui.calcItemWidth();
        if (CsgoWidgets.input("cfgname", "Name", configName)) {
            selectedLocalConfig = configName.get();
        }

        if (CsgoWidgets.button("save", "Save", width, CsgoTheme.buttonHeight())) {
            String name = configName.get().trim();
            if (!name.isEmpty()) {
                Yuri.INSTANCE.getConfigManager().saveConfig(name);
                selectedLocalConfig = name;
                statusText = "Saved " + name;
            }
        }
        if (CsgoWidgets.button("load", "Load", width, CsgoTheme.buttonHeight())) {
            String name = selectedName();
            if (!name.isEmpty() && Yuri.INSTANCE.getConfigManager().loadConfig(name)) {
                statusText = "Loaded " + name;
            } else {
                statusText = "Failed to load";
            }
        }
        if (CsgoWidgets.button("delete", "Delete", width, CsgoTheme.buttonHeight())) {
            String name = selectedName();
            if (!name.isEmpty() && Yuri.INSTANCE.getConfigManager().deleteConfig(name)) {
                if (name.equals(configName.get())) {
                    configName.set("");
                }
                selectedLocalConfig = null;
                statusText = "Deleted " + name;
            }
        }
        if (CsgoWidgets.button("refresh-online", "Refresh online", width, CsgoTheme.buttonHeight())) {
            refreshRemote();
        }

        drawConfigList("Local", localConfigNames(), true);
        drawConfigList("Online", remoteConfigs, false);

        if (statusText != null && !statusText.isEmpty()) {
            CsgoWidgets.centerText(statusText);
        }

        ImGui.endChild();
        ImGui.popStyleVar(2);
        CsgoWidgets.endChild();
    }

    private void drawConfigList(String title, List<String> names, boolean local) {
        ImGui.textDisabled(title);
        float width = ImGui.calcItemWidth();
        float height = Math.min(CsgoTheme.s(100f), Math.max(CsgoTheme.s(40f), names.size() * CsgoTheme.s(20f) + CsgoTheme.s(4f)));
        ImGui.beginChild(title + " list", width, height, false, ImGuiWindowFlags.NoBackground);
        float x = ImGui.getWindowPosX();
        float y = ImGui.getWindowPosY();
        float w = ImGui.getWindowWidth();
        float h = ImGui.getWindowHeight();
        ImDrawList draw = ImGui.getWindowDrawList();
        draw.addRectFilledMultiColor(x, y, x + w, y + h,
                unsigned(CsgoTheme.col(ImGuiCol.FrameBg)),
                unsigned(CsgoTheme.col(ImGuiCol.FrameBg)),
                unsigned(CsgoTheme.frameBg2()),
                unsigned(CsgoTheme.frameBg2()));
        draw.addRect(x, y, x + w, y + h, CsgoTheme.col(ImGuiCol.BorderShadow));

        ImGui.pushStyleVar(ImGuiStyleVar.ItemSpacing, 0f, 0f);
        if (names.isEmpty()) {
            CsgoWidgets.addText(x + CsgoTheme.s(8f), y + CsgoTheme.s(6f), CsgoTheme.col(ImGuiCol.TextDisabled), local ? "No configs" : "No online configs");
        } else {
            for (String name : names) {
                boolean selected = local && name.equalsIgnoreCase(selectedLocalConfig);
                if (CsgoWidgets.selectable((local ? "local" : "online") + name, name, selected, width, CsgoTheme.s(20f))) {
                    if (local) {
                        selectedLocalConfig = name;
                        configName.set(name);
                    } else {
                        new Thread(() -> {
                            boolean ok = GithubConfigFetcher.downloadAndLoadConfig(name);
                            statusText = ok ? "Loaded " + name : "Failed to load " + name;
                        }, "csgo-config-download").start();
                    }
                }
            }
        }
        ImGui.popStyleVar();
        ImGui.endChild();
    }

    private List<String> localConfigNames() {
        List<String> names = new ArrayList<String>();
        ConfigManager manager = Yuri.INSTANCE.getConfigManager();
        if (manager == null) {
            return names;
        }
        for (Config config : manager.getElements()) {
            names.add(config.getName());
        }
        return names;
    }

    private void refreshRemote() {
        statusText = "Fetching...";
        new Thread(() -> {
            List<String> fetched = GithubConfigFetcher.fetchConfigList();
            remoteConfigs.clear();
            if (fetched != null) {
                remoteConfigs.addAll(fetched);
            }
            statusText = remoteConfigs.isEmpty() ? "No online configs" : "Fetched " + remoteConfigs.size();
        }, "csgo-config-fetch").start();
    }

    private String selectedName() {
        if (selectedLocalConfig != null && !selectedLocalConfig.isEmpty()) {
            return selectedLocalConfig;
        }
        return configName.get().trim();
    }

    private String sliderFormat(NumberProperty property) {
        Representation representation = property.getRepresentation();
        if (representation == Representation.INT || property.getIncrement() >= 1.0D) {
            return "%.0f";
        }
        if (representation == Representation.PERCENTAGE) {
            return "%.0f%%";
        }
        if (representation == Representation.MILLISECONDS) {
            return "%.0fms";
        }
        return "%.2f";
    }

    private String selectedPreview(MultiModeProperty<?> property) {
        StringBuilder builder = new StringBuilder();
        for (Enum<?> value : property.getValues()) {
            if (property.isSelected(value)) {
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(value.toString());
            }
        }
        return builder.length() == 0 ? "None" : builder.toString();
    }

    private static long unsigned(int color) {
        return color & 0xFFFFFFFFL;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (listeningKeybind != null && mouseButton != -1) {
            listeningKeybind.setValue(KeyUtil.mouseButtonToKeyCode(mouseButton));
            listeningKeybind = null;
            return;
        }
        ImGuiManager.get().mouseClicked(mouseButton);
        if (!ImGuiManager.get().wantsMouse()) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state >= 0 && state < 5) {
            ImGui.getIO().setMouseDown(state, false);
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            ImGuiManager.get().mouseScrolled(Math.signum(wheel));
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        if (Keyboard.getEventKey() != Keyboard.KEY_NONE) {
            ImGuiManager.get().keyEvent(Keyboard.getEventKey(), Keyboard.getEventKeyState());
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (listeningKeybind != null) {
            listeningKeybind.setValue(keyCode == Keyboard.KEY_ESCAPE ? 0 : keyCode);
            listeningKeybind = null;
            return;
        }

        ImGuiManager.get().charTyped(typedChar);

        if (keyCode == Keyboard.KEY_ESCAPE && !ImGuiManager.get().wantsKeyboard()) {
            beginClose();
        }
    }

    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
