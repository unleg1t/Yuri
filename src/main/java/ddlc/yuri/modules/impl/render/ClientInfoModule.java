package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import ddlc.yuri.utils.render.adapters.FontAdapter;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@ModuleInfo(label = "Client Info", category = ModuleCategory.RENDER, description = "Renders a HUD to show your client info")
public final class ClientInfoModule extends Module {

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.CLASSIC);
    private final Property<Boolean> fps = new Property<>("FPS", true);
    private final Property<Boolean> speed = new Property<>("Speed", true);
    private final Property<Boolean> coords = new Property<>("Coords", true, () -> mode.getValue() == Mode.CLASSIC);
    private final Property<Boolean> useCustomFont = new Property<>("Use Custom Font", true);

    public enum Mode {
        YURI("Yuri"),
        CLASSIC("Classic");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private ScaledResolution sr = new ScaledResolution(mc);

    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final int BUBBLE_HEIGHT = 15;
    private static final int BUBBLE_PADDING = 2;
    private static final int RADIUS = 6;

    private static boolean positionInitialized = false;

    private double bpsValue = 0.0;
    private int xCoord = 0;
    private int yCoord = 0;
    private int zCoord = 0;

    private int x = 2;
    private double y = sr.getScaledHeight() - 2;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer != null) {
            double deltaX = mc.thePlayer.posX - mc.thePlayer.prevPosX;
            double deltaZ = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
            bpsValue = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0 * mc.timer.timerSpeed;
            xCoord = mc.thePlayer.getPosition().getX();
            yCoord = mc.thePlayer.getPosition().getY();
            zCoord = mc.thePlayer.getPosition().getZ();
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        drawInfoBubbles();
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        drawInfoBubbles();
    }

    private void initializePositions() {
        if (positionInitialized) return;

        int yOffset = 50;
        int xPos = 10;

        if (fps.getValue()) {
            if (!DragUtils.components.containsKey("InfoDisplay_FPS")) {
                DragUtils.components.put("InfoDisplay_FPS", new DragUtils.DraggableComponent(xPos, yOffset));
            }
            yOffset += BUBBLE_HEIGHT + 5;
        }

        if (speed.getValue()) {
            if (!DragUtils.components.containsKey("InfoDisplay_BPS")) {
                DragUtils.components.put("InfoDisplay_BPS", new DragUtils.DraggableComponent(xPos, yOffset));
            }
        }

        positionInitialized = true;
    }

    private void drawInfoBubbles() {
        if (mode.getValue() == Mode.CLASSIC) {
            drawClassic();
            return;
        }

        initializePositions();
        boolean isInChat = mc.currentScreen instanceof GuiChat;

        if (fps.getValue()) {
            drawBubble("FPS", String.valueOf(mc.getDebugFPS()), "InfoDisplay_FPS", isInChat);
        }

        if (speed.getValue()) {
            drawBubble("BPS", String.format("%.2f", bpsValue), "InfoDisplay_BPS", isInChat);
        }
    }

    private void drawClassic() {
        FontAdapter font = FontAdapter.of("sf", 18, useCustomFont.getValue());
        sr = new ScaledResolution(mc);

        double targetY = (mc.currentScreen instanceof GuiChat) ? (sr.getScaledHeight() - 15) : (sr.getScaledHeight() - 2);

        double speedFactor = 0.125;
        y = y + (targetY - y) * speedFactor;

        int lineHeight = (int) (font.height() + 2);
        List<String> lines = new ArrayList<>();

        if (fps.getValue()) {
            lines.add("FPS: " + "\u00a7f" + mc.getDebugFPS());
        }

        if (speed.getValue()) {
            lines.add(String.format("BPS: \u00a7f%.2f", bpsValue));
        }

        if (coords.getValue()) {
            lines.add("X: " + "\u00a7f" + xCoord + "\u00a7r" + " Y: " + "\u00a7f" + yCoord + "\u00a7r" + " Z: " + "\u00a7f" + zCoord);
        }

        for (int i = lines.size() - 1; i >= 0; i--) {
            font.draw(
                    lines.get(i),
                    x,
                    (float) (y - (lines.size() - i) * lineHeight),
                    ColorManager.getColor().getRGB()
            );
        }
    }

    private void drawBubble(String label, String value, String componentKey, boolean isInChat) {
        DragUtils.DraggableComponent component = DragUtils.components.get(componentKey);
        if (component == null) return;

        FontAdapter font = FontAdapter.of("sf", 18, useCustomFont.getValue());
        String displayText = label + ": " + "\u00a7f" + value;
        int textWidth = (int) font.width(displayText);
        int bubbleWidth = textWidth + BUBBLE_PADDING * 6;

        component.setWidth(bubbleWidth);
        component.setHeight(BUBBLE_HEIGHT);

        float bx = (float) component.getX();
        float by = (float) component.getY();

        RoundedUtils.drawRoundOutline(bx, by, bubbleWidth, BUBBLE_HEIGHT, RADIUS, -0.4f, BG_COLOR, ColorManager.getColor());

        float textY = by + (BUBBLE_HEIGHT - font.height()) / 2f;
        font.draw(displayText, bx + 3 + BUBBLE_PADDING, textY, ColorManager.getColor().getRGB());
    }

    @Override
    public void onDisable() {
        super.onDisable();
        positionInitialized = false;
    }
}