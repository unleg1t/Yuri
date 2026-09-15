package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OnlineConfigPanel {

    private static final float WIDTH = 120f;
    private static final float HEIGHT = 180f;
    private static final float HANDLE_WIDTH = 10f;
    private static final float HANDLE_HEIGHT = 38f;
    private static final float RADIUS = 6f;
    private static final float HEADER_HEIGHT = 20f;
    private static final float TAB_BAR_HEIGHT = 14f;
    private static final float CONTENT_PADDING = 4f;
    private static final float CONTENT_BOTTOM_PADDING = 6f;

    private boolean open = false;
    private float slideAnimation = 0f;

    private final List<ConfigPanelTab> tabs = new ArrayList<>();
    private int activeTabIndex = 0;

    public OnlineConfigPanel() {
        addTab(new LocalConfigsTab());
        addTab(new OnlineConfigsTab());
    }

    public void addTab(ConfigPanelTab tab) {
        tabs.add(tab);
        if (tabs.size() == 1) {
            tab.onShown();
        }
    }

    private ConfigPanelTab activeTab() {
        return tabs.get(activeTabIndex);
    }

    private static int scaledAlpha(Color base, float safeAlpha) {
        return MathHelper.clamp_int((int) (base.getAlpha() * safeAlpha), 0, 255);
    }

    public void drawScreen(int mouseX, int mouseY, float guiAlpha) {
        float safeAlpha = MathHelper.clamp_float(guiAlpha, 0.0f, 1.0f);
        if (safeAlpha < 0.08f) return;

        slideAnimation = MathUtils.lerp(slideAnimation, open ? 1f : 0f, 0.25f);
        if (Math.abs(slideAnimation - (open ? 1f : 0f)) < 0.005f) {
            slideAnimation = open ? 1f : 0f;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float guiScale = ScaleUtils.getScale(mc);
        float effectiveWidth = sr.getScaledWidth() / guiScale;
        float effectiveHeight = sr.getScaledHeight() / guiScale;

        float panelY = effectiveHeight / 2f - HEIGHT / 2f;
        float panelX = effectiveWidth - (WIDTH * slideAnimation);

        float handleX = panelX - HANDLE_WIDTH;
        float handleY = effectiveHeight / 2f - HANDLE_HEIGHT / 2f;

        int argb = MathHelper.clamp_int((int) (255 * safeAlpha), 0, 255);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        boolean handleHovered = isHandleHovered(mouseX, mouseY, handleX, handleY);
        Color handleBg = RenderUtils.withAlphaColor(handleHovered ? Theme.accent().darker() : Theme.WINDOW_BG, scaledAlpha(Theme.WINDOW_BG, safeAlpha));
        Color handleOutline = RenderUtils.withAlphaColor(Theme.accent(), argb);

        RoundedUtils.drawRoundOutline(handleX, handleY, HANDLE_WIDTH, HANDLE_HEIGHT, 4f, -0.5f, handleBg, handleOutline);

        CustomFontRenderer iconFont = FontUtils.getFont("sf-bold", 12);
        String arrow = open ? ">" : "<";
        float arrowY = handleY + (HANDLE_HEIGHT - iconFont.getHeight()) / 2f;
        iconFont.drawCenteredStringWithShadow(arrow, handleX + HANDLE_WIDTH / 2f, arrowY, RenderUtils.withAlpha(Theme.TEXT, argb));

        if (slideAnimation <= 0.01f) return;

        int animAlpha = (int) (argb * slideAnimation);
        Color bg = RenderUtils.withAlphaColor(Theme.WINDOW_BG, scaledAlpha(Theme.WINDOW_BG, safeAlpha * slideAnimation));

        RoundedUtils.drawRoundOutline(panelX, panelY, WIDTH, HEIGHT, RADIUS, -0.5f, bg, RenderUtils.withAlphaColor(Theme.accent(), animAlpha));

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 14);
        float headerY = panelY + 8f;
        titleFont.drawCenteredStringWithShadow("Configs", panelX + WIDTH / 2f, headerY, RenderUtils.withAlpha(Theme.TEXT, animAlpha));

        float tabBarY = panelY + HEADER_HEIGHT;
        drawTabBar(panelX, tabBarY, mouseX, mouseY, safeAlpha, slideAnimation, animAlpha);

        float contentX = panelX + CONTENT_PADDING;
        float contentY = tabBarY + TAB_BAR_HEIGHT + 3f;
        float contentWidth = WIDTH - CONTENT_PADDING * 2f;
        float contentHeight = panelY + HEIGHT - CONTENT_BOTTOM_PADDING - contentY;

        ConfigPanelContext ctx = new ConfigPanelContext(mc, panelX, panelY, contentX, contentY, contentWidth, contentHeight,
                effectiveWidth, effectiveHeight, mouseX, mouseY, safeAlpha, slideAnimation, animAlpha);

        activeTab().draw(ctx);
    }

    private void drawTabBar(float panelX, float tabBarY, int mouseX, int mouseY, float safeAlpha, float slideAnimation, int animAlpha) {
        CustomFontRenderer font = FontUtils.getFont("sf", 12);
        float tabWidth = WIDTH / tabs.size();

        for (int i = 0; i < tabs.size(); i++) {
            ConfigPanelTab tab = tabs.get(i);
            float tabX = panelX + tabWidth * i;
            boolean active = i == activeTabIndex;
            boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= tabBarY && mouseY <= tabBarY + TAB_BAR_HEIGHT;

            if (active) {
                Gui.drawRect2(tabX, tabBarY, tabWidth, TAB_BAR_HEIGHT,
                        RenderUtils.withAlphaColor(Theme.accent(), (int) (90 * safeAlpha * slideAnimation)).getRGB());
            } else if (hovered) {
                Gui.drawRect2(tabX, tabBarY, tabWidth, TAB_BAR_HEIGHT,
                        RenderUtils.withAlphaColor(Theme.MODULE_HOVER, (int) (100 * safeAlpha * slideAnimation)).getRGB());
            }

            Color textColor = active ? Theme.TEXT : Theme.TEXT_MUTED;
            font.drawCenteredStringWithShadow(tab.getLabel(), tabX + tabWidth / 2f,
                    tabBarY + (TAB_BAR_HEIGHT - font.getHeight()) / 2f, RenderUtils.withAlpha(textColor, animAlpha));
        }
    }

    public boolean isHandleHovered(int mouseX, int mouseY, float handleX, float handleY) {
        return mouseX >= handleX && mouseX <= handleX + HANDLE_WIDTH && mouseY >= handleY && mouseY <= handleY + HANDLE_HEIGHT;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        float guiScale = ScaleUtils.getScale(mc);
        float effectiveWidth = sr.getScaledWidth() / guiScale;
        float effectiveHeight = sr.getScaledHeight() / guiScale;

        float panelY = effectiveHeight / 2f - HEIGHT / 2f;
        float panelX = effectiveWidth - (WIDTH * slideAnimation);

        float handleX = panelX - HANDLE_WIDTH;
        float handleY = effectiveHeight / 2f - HANDLE_HEIGHT / 2f;

        if (isHandleHovered(mouseX, mouseY, handleX, handleY) && button == 0) {
            open = !open;
            return true;
        }

        if (!open || slideAnimation <= 0.8f) return false;
        if (mouseX < panelX || mouseX > panelX + WIDTH || mouseY < panelY || mouseY > panelY + HEIGHT) return false;

        float tabBarY = panelY + HEADER_HEIGHT;
        if (mouseY >= tabBarY && mouseY <= tabBarY + TAB_BAR_HEIGHT) {
            float tabWidth = WIDTH / tabs.size();
            int clickedIndex = (int) ((mouseX - panelX) / tabWidth);
            if (button == 0 && clickedIndex >= 0 && clickedIndex < tabs.size() && clickedIndex != activeTabIndex) {
                activeTabIndex = clickedIndex;
                activeTab().onShown();
            }
            return true;
        }

        float contentX = panelX + CONTENT_PADDING;
        float contentY = tabBarY + TAB_BAR_HEIGHT + 3f;
        float contentWidth = WIDTH - CONTENT_PADDING * 2f;
        float contentHeight = panelY + HEIGHT - CONTENT_BOTTOM_PADDING - contentY;

        ConfigPanelContext ctx = new ConfigPanelContext(mc, panelX, panelY, contentX, contentY, contentWidth, contentHeight,
                effectiveWidth, effectiveHeight, mouseX, mouseY, 1f, slideAnimation, 255);

        return activeTab().mouseClicked(ctx, mouseX, mouseY, button);
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!open) return false;
        return activeTab().keyTyped(typedChar, keyCode);
    }

    public boolean scroll(float amount) {
        if (open) {
            return activeTab().scroll(amount);
        }
        return false;
    }
}
