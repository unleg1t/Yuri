package ddlc.yuri.api.gui.click.imgui;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.config.GithubConfigFetcher;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.render.animations.Direction;
import ddlc.yuri.utils.render.animations.impl.DecelerateAnimation;
import ddlc.yuri.utils.render.imgui.ImGuiManager;
import imgui.ImGui;
import imgui.flag.ImGuiStyleVar;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

public class ImGuiClickGui extends GuiScreen {

    private final DecelerateAnimation openAnimation = new DecelerateAnimation(280, 1.0D, Direction.FORWARDS);
    private final Set<Module> openModules = new HashSet<>();
    private final Map<Property<?>, ImString> stringBuffers = new HashMap<>();
    private Property<Integer> listeningKeybind;
    @Getter
    private boolean closing;

    private final List<String> remoteConfigs = new ArrayList<>();
    private final ImBoolean githubWindowOpen = new ImBoolean(false);
    private long lastRemoteFetch = 0L;

    @Override
    public void initGui() {
        ImGuiManager.get().init(ClickGUIModule.style.getValue().build());
        openAnimation.setDirection(Direction.FORWARDS);
        openAnimation.reset();
        closing = false;
        super.initGui();
    }

    @Override
    public void onGuiClosed() {
        Yuri.INSTANCE.getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
        super.onGuiClosed();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        float progress = openAnimation.getOutput().floatValue();

        if (closing && openAnimation.finished(Direction.BACKWARDS)) {
            Minecraft.getMinecraft().displayGuiScreen(null);
            return;
        }

        ImGuiManager.get().newFrame(mc.displayWidth, mc.displayHeight);

        ImGui.pushStyleVar(ImGuiStyleVar.Alpha, progress);
        buildWindow();
        if (githubWindowOpen.get()) {
            buildGithubWindow();
        }
        ImGui.popStyleVar();

        ImGuiManager.get().render();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void buildWindow() {
        ImGui.setNextWindowSize(760, 520, imgui.flag.ImGuiCond.Once);
        ImGui.begin("Yuri", ImGuiWindowFlags.NoCollapse);

        ImGui.separator();
        if (ImGui.button("Online configs")) {
            githubWindowOpen.set(true);
            new Thread(() -> {
                List<String> fetched = GithubConfigFetcher.fetchConfigList();
                remoteConfigs.clear();
                if (fetched != null) {
                    remoteConfigs.addAll(fetched);
                }
            }, "github-config-fetch").start();
            lastRemoteFetch = System.currentTimeMillis();
        }
        ImGui.separator();

        if (ImGui.beginTabBar("##categories")) {
            for (ModuleCategory category : ModuleCategory.values()) {
                if (ImGui.beginTabItem(category.getName())) {
                    buildCategory(category);
                    ImGui.endTabItem();
                }
            }
            ImGui.endTabBar();
        }

        ImGui.end();
    }

    private void buildGithubWindow() {
        ImGui.setNextWindowSize(360, 420, imgui.flag.ImGuiCond.FirstUseEver);
        if (ImGui.begin("Github Configs", githubWindowOpen, ImGuiWindowFlags.NoCollapse)) {
            if (ImGui.button("Refresh")) {
                new Thread(() -> {
                    List<String> fetched = GithubConfigFetcher.fetchConfigList();
                    remoteConfigs.clear();
                    if (fetched != null) {
                        remoteConfigs.addAll(fetched);
                    }
                }, "github-config-fetch").start();
                lastRemoteFetch = System.currentTimeMillis();
            }

            ImGui.separator();

            if (remoteConfigs.isEmpty()) {
                ImGui.textDisabled("No configs found");
            } else {
                for (String configName : remoteConfigs) {
                    if (ImGui.button(configName)) {
                        new Thread(() -> GithubConfigFetcher.downloadAndLoadConfig(configName), "github-config-download").start();
                    }
                }
            }
        }
        ImGui.end();
    }

    private void buildCategory(ModuleCategory category) {
        for (Module module : Yuri.INSTANCE.getModuleManager().getModulesForCategory(category)) {
            buildModule(module);
        }
    }

    private void buildModule(Module module) {
        ImBoolean enabled = new ImBoolean(module.isEnabled());
        if (ImGui.checkbox(module.getLabel(), enabled)) {
            module.toggle();
        }

        boolean hasProperties = !module.getElements().isEmpty();
        if (hasProperties) {
            ImGui.sameLine();
            boolean opened = openModules.contains(module);
            if (ImGui.smallButton(opened ? "-##" + module.getLabel() : "+##" + module.getLabel())) {
                if (opened) {
                    openModules.remove(module);
                } else {
                    openModules.add(module);
                }
            }

            if (openModules.contains(module)) {
                ImGui.indent();
                for (Property<?> property : module.getElements()) {
                    if (property.isAvailable()) {
                        buildProperty(property);
                    }
                }
                ImGui.unindent();
            }
        }

        ImGui.separator();
    }

    @SuppressWarnings("unchecked")
    private void buildProperty(Property<?> property) {
        if (property instanceof NumberProperty) {
            NumberProperty numberProperty = (NumberProperty) property;
            float[] holder = {numberProperty.getValue().floatValue()};

            String format = "%.2f";

            double step = numberProperty.getIncrement();

            if (ImGui.sliderFloat(property.getLabel(), holder, (float) numberProperty.getMin(), (float) numberProperty.getMax(), format)) {
                if (step > 0.0D) {
                    double val = holder[0];
                    val = Math.round(val / step) * step;
                    holder[0] = (float) val;
                }

                double finalVal = Math.max(numberProperty.getMin(), Math.min(numberProperty.getMax(), holder[0]));
                numberProperty.setValue(finalVal);
            }
        } else if (property.getValue() instanceof Boolean) {
            Property<Boolean> booleanProperty = (Property<Boolean>) property;
            ImBoolean holder = new ImBoolean(booleanProperty.getValue());
            if (ImGui.checkbox(property.getLabel(), holder)) {
                booleanProperty.setValue(holder.get());
            }
        } else if (property instanceof ModeProperty) {
            ModeProperty<?> modeProperty = (ModeProperty<?>) property;
            Enum<?>[] values = modeProperty.getValues();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                names[i] = values[i].toString();
            }
            ImInt current = new ImInt(modeProperty.getValue().ordinal());
            if (ImGui.combo(property.getLabel(), current, names)) {
                modeProperty.setValue(current.get());
            }
        } else if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
            if (ImGui.treeNode(property.getLabel())) {
                Enum<?>[] values = multiModeProperty.getValues();
                for (int i = 0; i < values.length; i++) {
                    Enum<?> value = values[i];
                    ImBoolean selected = new ImBoolean(multiModeProperty.isSelected(value));
                    if (ImGui.checkbox(value.toString(), selected)) {
                        multiModeProperty.setValue(i);
                    }
                }
                ImGui.treePop();
            }
        } else if (property instanceof DescriptorProperty) {
            ImGui.textDisabled(property.getLabel());
        } else if (property.getValue() instanceof String) {
            Property<String> stringProperty = (Property<String>) property;
            ImString buffer = stringBuffers.computeIfAbsent(property, p -> new ImString(stringProperty.getValue(), 256));
            if (ImGui.inputText(property.getLabel(), buffer)) {
                stringProperty.setValue(buffer.get());
            }
        } else if (property.getValue() instanceof Integer) {
            Property<Integer> keybindProperty = (Property<Integer>) property;
            boolean listening = listeningKeybind == keybindProperty;
            String label = property.getLabel() + ": " + (listening ? ".." : Keyboard.getKeyName(keybindProperty.getValue()));
            if (ImGui.button(label)) {
                listeningKeybind = listening ? null : keybindProperty;
            }
            try {
                if (ImGui.isItemHovered() && ImGui.isMouseClicked(2)) {
                    listeningKeybind = listening ? null : keybindProperty;
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
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
            listeningKeybind.setValue(keyCode);
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