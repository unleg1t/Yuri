package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.managers.impl.SessionStatsManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;

@ModuleInfo(label = "Session Info", description = "Kills, deaths, wins and rounds of this session", category = ModuleCategory.RENDER)
public class SessionInfoModule extends Module implements IMinecraft {

    public final Property<Boolean> background = new Property<>("Background", true);

    private static final String KEY = "SessionInfo";
    private static final float PADDING_X = 6f;
    private static final float PADDING_Y = 5f;
    private static final float GAP_LINE = 2f;
    private static final float GAP_VALUE = 6f;
    private static final float RADIUS = 4f;

    private static final Color BG_COLOR = new Color(18, 18, 20, 150);
    private static final int LABEL_RGB = new Color(200, 200, 205).getRGB();

    private static final String[] LABELS = {"Kills", "Deaths", "Wins", "Rounds"};

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);

    public SessionInfoModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        component.setWidth(0);
        component.setHeight(0);
    }

    @Override
    public void onDisable() {
        component.setWidth(0);
        component.setHeight(0);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        render();
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        render();
    }

    private void render() {
        CustomFontRenderer font = FontUtils.getFont("sf", 16);
        if (font == null) return;

        String[] values = {
                String.valueOf(SessionStatsManager.getKills()),
                String.valueOf(SessionStatsManager.getDeaths()),
                String.valueOf(SessionStatsManager.getWins()),
                String.valueOf(SessionStatsManager.getRounds())
        };

        float labelWidth = 0f;
        float valueWidth = 0f;
        for (int i = 0; i < LABELS.length; i++) {
            labelWidth = Math.max(labelWidth, font.getStringWidth(LABELS[i]));
            valueWidth = Math.max(valueWidth, font.getStringWidth(values[i]));
        }

        float lineHeight = font.getHeight();
        float width = PADDING_X * 2 + labelWidth + GAP_VALUE + valueWidth;
        float height = PADDING_Y * 2 + lineHeight * LABELS.length + GAP_LINE * (LABELS.length - 1);

        component.setWidth(width);
        component.setHeight(height);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        if (background.getValue()) {
            RoundedUtils.drawRoundedRect(x, y, width, height, RADIUS, BG_COLOR);
        }

        int accent = ColorManager.getColor().getRGB();
        float cursorY = y + PADDING_Y;
        float valueRight = x + width - PADDING_X;

        for (int i = 0; i < LABELS.length; i++) {
            font.drawStringWithShadow(LABELS[i], x + PADDING_X, cursorY, LABEL_RGB);
            font.drawStringWithShadow(values[i], valueRight - font.getStringWidth(values[i]), cursorY, accent);
            cursorY += lineHeight + GAP_LINE;
        }
    }
}
