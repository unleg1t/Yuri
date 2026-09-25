package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleInfo(label = "Keybind Info", description = "Shows a list of active keybinds", category = ModuleCategory.RENDER)
public class KeybindInfoModule extends Module implements IMinecraft {

    private static final String KEY = "KeybindInfo";

    private static final float PADDING_X = 6f;
    private static final float PADDING_Y = 8f;
    private static final float MIN_WIDTH = 145f;
    private static final float RADIUS = 6f;
    private static final float GAP_TITLE_LIST = 8f;
    private static final float GAP_ROW = 5f;
    private static final float GAP_LABEL_KEY = 16f;
    private static final float KEY_BOX_RADIUS = 4f;
    private static final float KEY_BOX_PADDING_X = 6f;
    private static final float KEY_BOX_PADDING_Y = 3f;
    private static final float SLIDE_DISTANCE = 10f;
    private static final float ANIMATION_SPEED = 12f;

    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color TEXT_SECONDARY_COLOR = new Color(220, 220, 220);
    private static final Color TEXT_TERTIARY_COLOR = new Color(150, 150, 150);
    private static final Color HIGHLIGHT_BG_COLOR = new Color(255, 255, 255, 40);
    private static final int WHITE_RGB = Color.WHITE.getRGB();
    private static final int TEXT_SECONDARY_RGB = TEXT_SECONDARY_COLOR.getRGB();
    private static final int TEXT_TERTIARY_RGB = TEXT_TERTIARY_COLOR.getRGB();

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);
    private final Map<Module, Float> animationProgress = new LinkedHashMap<>();
    private long lastFrameTimeNanos = System.nanoTime();

    public KeybindInfoModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        component.setWidth(0);
        component.setHeight(0);
        animationProgress.clear();
        lastFrameTimeNanos = System.nanoTime();
    }

    @Override
    public void onDisable() {
        component.setWidth(0);
        component.setHeight(0);
        animationProgress.clear();
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onRender2D(Render2DEvent event) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        render(false);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent event) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        render(true);

        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    private void render(boolean shaderPass) {
        updateAnimations();

        List<Module> renderModules = new ArrayList<>(animationProgress.keySet());
        renderModules.sort(Comparator.comparing(Module::getLabel, String.CASE_INSENSITIVE_ORDER));

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer rowFont = FontUtils.getFont("sf", 15);
        if (titleFont == null || rowFont == null) return;

        String titleText = "Keybind Info";
        float titleWidth = titleFont.getStringWidth(titleText);
        float titleHeight = titleFont.getHeight();
        float rowHeight = rowFont.getHeight();

        String emptyText = "No modules enabled";
        float emptyWidth = rowFont.getStringWidth(emptyText);
        boolean showEmpty = renderModules.isEmpty();

        float contentWidth = titleWidth;
        if (showEmpty) {
            contentWidth = Math.max(contentWidth, emptyWidth);
        } else {
            for (Module module : renderModules) {
                float labelWidth = rowFont.getStringWidth(module.getLabel());
                float keyWidth = rowFont.getStringWidth(getKeyName(module));
                contentWidth = Math.max(contentWidth, labelWidth + GAP_LABEL_KEY + keyWidth);
            }
        }

        float width = Math.max(MIN_WIDTH, contentWidth + PADDING_X * 2);

        float listHeight;
        if (showEmpty) {
            listHeight = rowHeight;
        } else {
            listHeight = 0f;
            for (Module module : renderModules) {
                listHeight += (rowHeight + GAP_ROW) * animationProgress.get(module);
            }
            listHeight = Math.max(0f, listHeight - GAP_ROW);
        }

        float height = PADDING_Y * 2 + titleHeight + GAP_TITLE_LIST + listHeight;

        component.setWidth(width);
        component.setHeight(height);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        RoundedUtils.drawRoundOutline(x, y, width, height, RADIUS, -0.5f, BG_COLOR, ColorManager.getColor());

        if (shaderPass) return;

        float cx = x + width / 2f;
        float cursorY = y + PADDING_Y;

        titleFont.drawStringWithShadow(titleText, cx - titleWidth / 2f, cursorY, WHITE_RGB);
        cursorY += titleHeight + GAP_TITLE_LIST;

        if (showEmpty) {
            rowFont.drawStringWithShadow(emptyText, cx - emptyWidth / 2f, cursorY, TEXT_TERTIARY_RGB);
            return;
        }

        float leftX = x + PADDING_X;
        float rightX = x + width - PADDING_X * 2.0f;

        for (Module module : renderModules) {
            float progress = animationProgress.get(module);
            if (progress > 0.08f) {
                float rowAlpha = Math.min(1f, progress);
                float slideOffset = (1f - progress) * SLIDE_DISTANCE;

                String label = module.getLabel();
                String keyName = getKeyName(module);
                float keyWidth = rowFont.getStringWidth(keyName);

                float boxWidth = KEY_BOX_PADDING_X + 5.0f;
                float boxHeight = rowHeight + KEY_BOX_PADDING_Y;
                float boxX = rightX - 5.0f - KEY_BOX_PADDING_X + slideOffset + 4.0f;
                float boxY = cursorY - KEY_BOX_PADDING_Y + 1.5f;

                RoundedUtils.drawRoundedRect(boxX, boxY, boxWidth, boxHeight, 2, withAlpha(HIGHLIGHT_BG_COLOR, rowAlpha));

                rowFont.drawStringWithShadow(label, leftX - slideOffset, cursorY, withAlpha(TEXT_SECONDARY_RGB, rowAlpha));
                rowFont.drawStringWithShadow(keyName, rightX - keyWidth + slideOffset, cursorY, withAlpha(TEXT_TERTIARY_RGB, rowAlpha));
            }

            cursorY += (rowHeight + GAP_ROW) * progress;
        }
    }

    private void updateAnimations() {
        long now = System.nanoTime();
        float delta = Math.min((now - lastFrameTimeNanos) / 1_000_000_000f, 0.1f);
        lastFrameTimeNanos = now;

        Set<Module> activeSet = new HashSet<>(getActiveModules());
        for (Module module : activeSet) {
            animationProgress.putIfAbsent(module, 0f);
        }

        List<Module> toRemove = new ArrayList<>();
        for (Map.Entry<Module, Float> entry : animationProgress.entrySet()) {
            float target = activeSet.contains(entry.getKey()) ? 1f : 0f;
            float progress = entry.getValue();
            progress += (target - progress) * Math.min(1f, delta * ANIMATION_SPEED);
            if (target == 0f && progress < 0.001f) {
                progress = 0f;
                toRemove.add(entry.getKey());
            }
            entry.setValue(progress);
        }
        for (Module module : toRemove) {
            animationProgress.remove(module);
        }
    }

    private List<Module> getActiveModules() {
        List<Module> modules = new ArrayList<>();
        for (Module module : Yuri.INSTANCE.getModuleManager().getModules()) {
            if (module.isEnabled() && module != this && module.keybind.getValue() != 0 && Keyboard.getKeyName(module.getKey()).length() == 1) {
                modules.add(module);
            }
        }
        modules.sort(Comparator.comparing(Module::getLabel, String.CASE_INSENSITIVE_ORDER));
        return modules;
    }

    private String getKeyName(Module module) {
        int key = module.getKey();
        return key == Keyboard.KEY_NONE ? "None" : Keyboard.getKeyName(key);
    }

    private static Color withAlpha(Color color, float multiplier) {
        int alpha = Math.max(0, Math.min(255, (int) (color.getAlpha() * multiplier)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static int withAlpha(int rgb, float multiplier) {
        return withAlpha(new Color(rgb, true), multiplier).getRGB();
    }
}