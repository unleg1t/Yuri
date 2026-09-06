package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.config.GithubConfigFetcher;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class OnlineConfigPanel {

    private static final float WIDTH = 120f;
    private static final float HEIGHT = 180f;
    private static final float HANDLE_WIDTH = 10f;
    private static final float HANDLE_HEIGHT = 38f;
    private static final float RADIUS = 6f;

    private boolean open = false;
    private float slideAnimation = 0f;
    private final List<String> configs = new CopyOnWriteArrayList<>();
    private boolean loadingList = false;
    private String statusText = null;
    private long statusTimer = 0;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;

    public OnlineConfigPanel() {
        refreshConfigs();
    }

    public void refreshConfigs() {
        if (loadingList) return;
        loadingList = true;
        statusText = "Fetching...";
        CompletableFuture.runAsync(() -> {
            List<String> fetched = GithubConfigFetcher.fetchConfigList();
            configs.clear();
            configs.addAll(fetched);
            loadingList = false;
            statusText = fetched.isEmpty() ? "No configs" : null;
        });
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

        if (slideAnimation > 0.01f) {
            int animAlpha = (int) (argb * slideAnimation);
            Color bg = RenderUtils.withAlphaColor(Theme.WINDOW_BG, scaledAlpha(Theme.WINDOW_BG, safeAlpha * slideAnimation));

            RoundedUtils.drawRoundOutline(panelX, panelY, WIDTH, HEIGHT, RADIUS, -0.5f, bg, RenderUtils.withAlphaColor(Theme.accent(), animAlpha));

            CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 14);
            CustomFontRenderer font = FontUtils.getFont("sf", 12);

            float headerY = panelY + 8f;
            titleFont.drawCenteredStringWithShadow("Online Configs", panelX + WIDTH / 2f, headerY, RenderUtils.withAlpha(Theme.TEXT, animAlpha));

            if (System.currentTimeMillis() - statusTimer > 3000 && statusText != null && !loadingList && !statusText.equals("No configs")) {
                statusText = null;
            }

            if (statusText != null) {
                font.drawCenteredStringWithShadow(statusText, panelX + WIDTH / 2f, panelY + HEIGHT / 2f, RenderUtils.withAlpha(Theme.TEXT_MUTED, animAlpha));
            } else if (!configs.isEmpty()) {
                float listY = panelY + 24f;
                float listH = HEIGHT - 30f;

                float rawScaleX = (float) mc.displayWidth / effectiveWidth;
                float rawScaleY = (float) mc.displayHeight / effectiveHeight;

                int scissorX = (int) (panelX * rawScaleX);
                int scissorY = (int) ((effectiveHeight - (listY + listH)) * rawScaleY);
                int scissorW = (int) (WIDTH * rawScaleX);
                int scissorH = (int) (listH * rawScaleY);

                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(Math.max(0, scissorX), Math.max(0, scissorY), Math.max(1, scissorW), Math.max(1, scissorH));

                float totalListH = configs.size() * 18f;
                float maxScroll = Math.max(0f, totalListH - listH);
                targetScrollOffset = MathHelper.clamp_float(targetScrollOffset, 0f, maxScroll);
                scrollOffset = MathUtils.lerp(scrollOffset, targetScrollOffset, 0.25f);

                float currentY = listY - scrollOffset;
                for (String configName : configs) {
                    if (currentY + 16f >= listY && currentY <= listY + listH) {
                        boolean itemHovered = mouseX >= panelX + 4f && mouseX <= panelX + WIDTH - 4f && mouseY >= currentY && mouseY <= currentY + 16f;
                        Color itemBg = itemHovered ? RenderUtils.withAlphaColor(Theme.MODULE_HOVER, (int) (180 * safeAlpha * slideAnimation)) : RenderUtils.withAlphaColor(Theme.BAR_BG, scaledAlpha(Theme.BAR_BG, safeAlpha * slideAnimation));

                        Gui.drawRect2(panelX + 4f, currentY, WIDTH - 8f, 16f, itemBg.getRGB());

                        Color textColor = itemHovered ? Theme.TEXT : Theme.TEXT_MUTED;
                        font.drawString(configName, panelX + 8f, currentY + (16f - font.getHeight()) / 2f, RenderUtils.withAlpha(textColor, animAlpha));
                    }
                    currentY += 18f;
                }

                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
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
            if (open && configs.isEmpty() && !loadingList) {
                refreshConfigs();
            }
            return true;
        }

        if (open && slideAnimation > 0.8f && mouseX >= panelX && mouseX <= panelX + WIDTH && mouseY >= panelY && mouseY <= panelY + HEIGHT) {
            if (button == 0 && !loadingList && !configs.isEmpty()) {
                float listY = panelY + 24f;
                float listH = HEIGHT - 30f;
                if (mouseY >= listY && mouseY <= listY + listH) {
                    float currentY = listY - scrollOffset;
                    for (String configName : configs) {
                        if (mouseX >= panelX + 4f && mouseX <= panelX + WIDTH - 4f && mouseY >= currentY && mouseY <= currentY + 16f) {
                            statusText = "Loading...";
                            CompletableFuture.runAsync(() -> {
                                boolean success = GithubConfigFetcher.downloadAndLoadConfig(configName);
                                statusText = success ? "Loaded!" : "Failed!";
                                statusTimer = System.currentTimeMillis();
                            });
                            return true;
                        }
                        currentY += 18f;
                    }
                }
            }
            return true;
        }

        return false;
    }

    public boolean scroll(float amount) {
        if (open) {
            targetScrollOffset = Math.max(0f, targetScrollOffset + amount);
            return true;
        }
        return false;
    }
}