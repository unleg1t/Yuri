package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.config.Config;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.config.GithubConfigFetcher;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.misc.Timer;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class YuriConfigPanel extends YuriPanel {

    private final List<ConfigRow> rows = new CopyOnWriteArrayList<>();
    private ConfigRow selected;
    private NameField nameField;

    public YuriConfigPanel(float posX, float posY) {
        super(null, posX, posY);
        refreshConfigs();
    }

    @Override
    public void resetAnimations() {
        refreshConfigs();
    }

    @Override
    public String drawScreen(int mouseX, int mouseY, float animationProgress) {
        float offsetY = (1.0F - animationProgress) * -8.0F;
        float drawX = getPosX();
        float drawY = getPosY() + offsetY;

        float extra = opened ? 4.0F : 0.0F;
        float bodyHeight = opened ? Math.min(getContentHeight(), YuriTheme.MAX_BODY_HEIGHT) + extra : 0.0F;
        float totalHeight = YuriTheme.HEADER_HEIGHT + bodyHeight;
        RoundedUtils.drawRoundedRect(drawX, drawY, YuriTheme.PANEL_WIDTH, totalHeight, YuriTheme.RADIUS,
                YuriTheme.fade(YuriTheme.BG, animationProgress));
        RoundedUtils.drawRoundedRect(drawX + YuriTheme.PADDING_X, drawY + YuriTheme.HEADER_HEIGHT - 3f,
                YuriTheme.PANEL_WIDTH - YuriTheme.PADDING_X * 2f, 1.1f, 0.6f,
                YuriTheme.fade(YuriTheme.accent(), animationProgress));

        FontUtils.getFont("sf-bold", 14).drawStringWithShadow(
                "Configs",
                drawX + YuriTheme.PADDING_X,
                drawY + 6f,
                YuriTheme.text(YuriTheme.TEXT_PRIMARY, animationProgress)
        );

        if (opened) {
            float maxScroll = Math.max(0.0F, getContentHeight() - YuriTheme.MAX_BODY_HEIGHT);
            targetScrollOffset = Math.max(0.0F, Math.min(maxScroll, targetScrollOffset));
            scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.25F);
            bodyTop = drawY + YuriTheme.HEADER_HEIGHT;
            bodyBottom = bodyTop + Math.min(getContentHeight(), YuriTheme.MAX_BODY_HEIGHT);

            Minecraft mc = Minecraft.getMinecraft();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            ScaleUtils.applyScissor(mc, drawX, bodyTop, drawX + YuriTheme.PANEL_WIDTH, bodyBottom);
            for (ConfigRow row : rows) {
                row.draw(mouseX, mouseY, drawX, drawY - getCurrentScrollOffset(), animationProgress);
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        return null;
    }

    @Override
    public float getContentHeight() {
        float height = 4f;
        for (ConfigRow row : rows) {
            height += row.getHeight();
        }
        return height;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isHeaderHovered(mouseX, mouseY) && mouseButton == 1) {
            opened = !opened;
        }

        if (opened) {
            for (ConfigRow row : rows) {
                row.click(mouseX, mouseY, mouseButton);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        for (ConfigRow row : rows) {
            row.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean isTyping() {
        return nameField != null && nameField.focused;
    }

    public void refreshConfigs() {
        rows.clear();
        selected = null;

        for (Config config : ConfigManager.getInstance().getElements()) {
            rows.add(new ConfigItemRow(config.getName()));
        }

        nameField = new NameField();
        rows.add(nameField);
        rows.add(new ConfigButtonRow("Load", name -> {
            if (!name.isEmpty()) {
                ConfigManager.getInstance().loadConfig(name);
            }
        }));
        rows.add(new ConfigButtonRow("Save", ignored -> {
            String name = selected instanceof ConfigItemRow ? selected.name : nameField.value;
            if (name == null || name.isEmpty()) {
                return;
            }
            ConfigManager.getInstance().saveConfig(name);
            refreshConfigs();
        }));
        rows.add(new ConfigButtonRow("Delete", name -> {
            if (!name.isEmpty()) {
                ConfigManager.getInstance().deleteConfig(name);
                refreshConfigs();
            }
        }));

        try {
            List<String> remote = GithubConfigFetcher.fetchConfigList();
            if (remote != null && !remote.isEmpty()) {
                rows.add(new ConfigLabelRow("Online"));
                for (String remoteName : remote) {
                    rows.add(new ConfigButtonRow(remoteName, ignored -> new Thread(() -> {
                        boolean loaded = GithubConfigFetcher.downloadAndLoadConfig(remoteName);
                        if (loaded) {
                            Minecraft.getMinecraft().addScheduledTask(this::refreshConfigs);
                        }
                    }, "github-config-download").start()));
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private abstract class ConfigRow {
        protected final String name;
        protected float y;

        protected ConfigRow(String name) {
            this.name = name;
        }

        abstract void draw(int mouseX, int mouseY, float panelX, float panelY, float progress);

        void click(int mouseX, int mouseY, int mouseButton) {
        }

        void keyTyped(char typedChar, int keyCode) {
        }

        float getHeight() {
            return YuriTheme.SETTING_HEIGHT + 2f;
        }

        boolean hovered(int mouseX, int mouseY) {
            layoutY();
            return mouseX >= getPosX() && mouseY >= y && mouseX <= getPosX() + YuriTheme.PANEL_WIDTH && mouseY <= y + getHeight();
        }

        void layoutY() {
            y = getPosY() + YuriTheme.HEADER_HEIGHT - getCurrentScrollOffset() + 2f;
            for (ConfigRow row : rows) {
                if (row == this) {
                    break;
                }
                y += row.getHeight();
            }
        }
    }

    private class ConfigItemRow extends ConfigRow {
        private float fraction;
        private final Timer lastClick = new Timer();

        private ConfigItemRow(String name) {
            super(name);
        }

        @Override
        void draw(int mouseX, int mouseY, float panelX, float panelY, float progress) {
            layoutY();
            boolean isSelected = selected == this;
            fraction = MathUtils.lerp(fraction, isSelected ? 1f : 0f, 0.18f);

            Color color = new Color(ddlc.yuri.utils.render.RenderUtils.interpolateColor(
                    YuriTheme.fadeSolid(YuriTheme.TEXT_SECONDARY, progress),
                    YuriTheme.fadeSolid(YuriTheme.TEXT_PRIMARY, progress),
                    fraction
            ), true);
            FontUtils.getFont("sf", 13).drawStringWithShadow(name, panelX + YuriTheme.PADDING_X, y + 4f, color.getRGB());
        }

        @Override
        void click(int mouseX, int mouseY, int mouseButton) {
            if (hovered(mouseX, mouseY) && mouseButton == 0) {
                if (!lastClick.hasTimeElapsed(200)) {
                    ConfigManager.getInstance().loadConfig(name);
                }
                selected = this;
                lastClick.reset();
            }
        }
    }

    private class ConfigButtonRow extends ConfigRow {
        private final Consumer<String> action;
        private float hover;

        private ConfigButtonRow(String name, Consumer<String> action) {
            super(name);
            this.action = action;
        }

        @Override
        void draw(int mouseX, int mouseY, float panelX, float panelY, float progress) {
            layoutY();
            hover = MathUtils.lerp(hover, hovered(mouseX, mouseY) ? 1f : 0f, 0.2f);
            Color color = new Color(ddlc.yuri.utils.render.RenderUtils.interpolateColor(
                    YuriTheme.fadeSolid(YuriTheme.TEXT_SECONDARY, progress),
                    YuriTheme.fadeSolid(YuriTheme.TEXT_PRIMARY, progress),
                    hover
            ), true);
            FontUtils.getFont("sf", 13).drawStringWithShadow(name, panelX + YuriTheme.PADDING_X, y + 4f, color.getRGB());
        }

        @Override
        void click(int mouseX, int mouseY, int mouseButton) {
            if (hovered(mouseX, mouseY) && mouseButton == 0) {
                String argument = selected instanceof ConfigItemRow ? selected.name : "";
                action.accept(argument);
            }
        }
    }

    private class ConfigLabelRow extends ConfigRow {
        private ConfigLabelRow(String name) {
            super(name);
        }

        @Override
        void draw(int mouseX, int mouseY, float panelX, float panelY, float progress) {
            layoutY();
            FontUtils.getFont("sf-bold", 11).drawStringWithShadow(name.toUpperCase(), panelX + YuriTheme.PADDING_X, y + 5f,
                    YuriTheme.text(YuriTheme.TEXT_MUTED, progress));
        }
    }

    private class NameField extends ConfigRow {
        private String value = "";
        private boolean focused;
        private final Timer backspace = new Timer();

        private NameField() {
            super("Name");
        }

        @Override
        float getHeight() {
            return 24f;
        }

        @Override
        void draw(int mouseX, int mouseY, float panelX, float panelY, float progress) {
            layoutY();
            if (focused && Keyboard.isKeyDown(Keyboard.KEY_BACK) && backspace.hasTimeElapsed(100, true) && !value.isEmpty()) {
                value = value.substring(0, value.length() - 1);
            }

            FontUtils.getFont("sf", 10).drawStringWithShadow("Name", panelX + YuriTheme.PADDING_X, y + 1f,
                    YuriTheme.text(YuriTheme.TEXT_MUTED, progress));
            String display = (value.isEmpty() ? "..." : value) + (focused && (System.currentTimeMillis() / 400) % 2 == 0 ? "|" : "");
            FontUtils.getFont("sf", 12).drawStringWithShadow(display, panelX + YuriTheme.PADDING_X, y + 11f,
                    YuriTheme.text(YuriTheme.TEXT_SECONDARY, progress));
        }

        @Override
        void click(int mouseX, int mouseY, int mouseButton) {
            focused = hovered(mouseX, mouseY) && mouseButton == 0;
            if (focused) {
                selected = this;
            }
        }

        @Override
        void keyTyped(char typedChar, int keyCode) {
            if (!focused) {
                return;
            }
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                focused = false;
            } else if (keyCode != Keyboard.KEY_BACK && !isIgnored(keyCode)) {
                value += typedChar;
            }
        }

        private boolean isIgnored(int keyCode) {
            return keyCode == Keyboard.KEY_RCONTROL
                    || keyCode == Keyboard.KEY_LCONTROL
                    || keyCode == Keyboard.KEY_RSHIFT
                    || keyCode == Keyboard.KEY_LSHIFT
                    || keyCode == Keyboard.KEY_TAB;
        }
    }
}
