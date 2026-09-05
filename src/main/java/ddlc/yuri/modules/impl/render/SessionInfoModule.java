package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.impl.ModeProperty;
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

@ModuleInfo(label = "Session Info", description = "Displays session information on the screen.", category = ModuleCategory.RENDER)
public class
SessionInfoModule extends Module implements IMinecraft {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.PULSIVE);

    private enum Mode {
        PULSIVE("Pulsive"),
        YURI("Yuri");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private static final String KEY = "SessionInfo";
    private static final float PADDING_X = 14f;
    private static final float MIN_WIDTH = 145f;
    private static final float PADDING_Y = 8f;
    private static final float RADIUS = 6f;
    private static final float HEADER_PADDING_Y = 6f;
    private static final float GAP_HEADER_TIME = 8f;
    private static final float GAP_TIME = 3f;
    private static final float GAP_LINE = 2f;

    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color HEADER_COLOR = new Color(40, 40, 44, 100);
    private static final Color BODY_COLOR = new Color(18, 18, 20, 150);
    private static final Color TEXT_SECONDARY_COLOR = new Color(220, 220, 220);
    private static final Color TEXT_TERTIARY_COLOR = new Color(190, 190, 190);
    private static final int WHITE_RGB = Color.WHITE.getRGB();
    private static final int TEXT_SECONDARY_RGB = TEXT_SECONDARY_COLOR.getRGB();
    private static final int TEXT_TERTIARY_RGB = TEXT_TERTIARY_COLOR.getRGB();

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
        switch (mode.getValue()) {
            case PULSIVE:
                renderPulsive();
                break;
            case YURI:
                renderYuri();
                break;
        }
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        switch (mode.getValue()) {
            case PULSIVE:
                renderPulsive();
                break;
            case YURI:
                renderYuri();
                break;
        }
    }

    public void renderPulsive() {
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer body = FontUtils.getFont("sf", 16);
        CustomFontRenderer timeFont = FontUtils.getFont("sf-bold", 24);
        if (bold == null || regular == null || body == null || timeFont == null) return;

        String sessionWord = "session";
        String infoWord = "information";
        String timeText = formatTime(SessionStatsManager.getSessionMs());
        String killsText = "You have gotten " + SessionStatsManager.getKills() + " kills";
        String deathsText = "You have died " + SessionStatsManager.getDeaths() + " times";
        String winsText = "Games won " + SessionStatsManager.getWins() + " times";

        float sessionWidth = bold.getStringWidth(sessionWord);
        float infoWidth = regular.getStringWidth(infoWord);
        float titleWidth = sessionWidth + infoWidth;
        float titleHeight = Math.max(bold.getHeight(), regular.getHeight());

        float timeWidth = timeFont.getStringWidth(timeText);
        float timeHeight = timeFont.getHeight();

        float killsWidth = body.getStringWidth(killsText);
        float deathsWidth = body.getStringWidth(deathsText);
        float winsWidth = body.getStringWidth(winsText);
        float lineHeight = body.getHeight();

        float contentWidth = Math.max(titleWidth, Math.max(timeWidth,
                Math.max(killsWidth, Math.max(deathsWidth, winsWidth))));

        float headerHeight = titleHeight + HEADER_PADDING_Y * 2;
        float bodyContentHeight = timeHeight + GAP_TIME + lineHeight + GAP_LINE
                + lineHeight + GAP_LINE + lineHeight;

        float width = Math.max(MIN_WIDTH, contentWidth + PADDING_X * 2);
        float bodyHeight = GAP_HEADER_TIME + bodyContentHeight + PADDING_Y;
        float totalHeight = headerHeight + bodyHeight;

        component.setWidth(width);
        component.setHeight(totalHeight);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - totalHeight;

        RoundedUtils.drawCustomRoundedRect(x, y, width, headerHeight, RADIUS,
                true, true, false, false, HEADER_COLOR);

        float seamFixOffset = 1.35f;
        RoundedUtils.drawCustomRoundedRect(x, y + headerHeight + seamFixOffset, width, bodyHeight + seamFixOffset, RADIUS,
                false, false, true, true, BODY_COLOR);

        float cx = x + width / 2f;
        float titleX = cx - titleWidth / 2f;
        float titleY = y + HEADER_PADDING_Y;
        bold.drawStringWithShadow(sessionWord, titleX, titleY, WHITE_RGB);
        regular.drawStringWithShadow(infoWord, titleX + sessionWidth, titleY, TEXT_SECONDARY_RGB);

        float leftX = x + PADDING_X;
        float cursorY = y + headerHeight + GAP_HEADER_TIME;

        timeFont.drawStringWithShadow(timeText, leftX, cursorY, WHITE_RGB);
        cursorY += timeHeight + GAP_TIME;

        body.drawStringWithShadow(killsText, leftX, cursorY, TEXT_TERTIARY_RGB);
        cursorY += lineHeight + GAP_LINE;

        body.drawStringWithShadow(deathsText, leftX, cursorY, TEXT_TERTIARY_RGB);
        cursorY += lineHeight + GAP_LINE;

        body.drawStringWithShadow(winsText, leftX, cursorY, TEXT_TERTIARY_RGB);
    }

    public void renderYuri() {
        CustomFontRenderer title = FontUtils.getFont("sf-bold", 20);
        CustomFontRenderer welcome = FontUtils.getFont("sf-bold", 22);
        CustomFontRenderer body = FontUtils.getFont("sf", 16);
        if (title == null || welcome == null || body == null) return;

        String titleText = "Session Info";
        String welcomeText = "Welcome, " + mc.getSession().getUsername() + "!";
        String singleplayerText = "No stats to render.";
        String killsText = "You have " + SessionStatsManager.getKills() + " kills and "
                + SessionStatsManager.getDeaths() + " deaths.";
        String timeText = "You have been playing for "
                + formatTimeYuri(SessionStatsManager.getSessionMs()) + ".";
        String serverText = "Server: " + getServerName();

        float titleWidth = title.getStringWidth(titleText);
        float welcomeWidth = welcome.getStringWidth(welcomeText);
        float singleplayerWidth = body.getStringWidth(singleplayerText);
        float killsWidth = body.getStringWidth(killsText);
        float timeWidth = body.getStringWidth(timeText);
        float serverWidth = body.getStringWidth(serverText);

        float contentWidth = Math.max(titleWidth, Math.max(welcomeWidth, Math.max(killsWidth,
                Math.max(timeWidth, serverWidth))));
        float width = Math.max(MIN_WIDTH, contentWidth + PADDING_X * 2);

        float titleHeight = title.getHeight();
        float welcomeHeight = welcome.getHeight();
        float lineHeight = body.getHeight();

        float gapTitleWelcome = 10f;
        float gapWelcomeKills = 8f;
        float gapLine = 6f;
        float gapKillsServer = 14f;

        float height = PADDING_Y * 2 + titleHeight + gapTitleWelcome + welcomeHeight + gapWelcomeKills
                + lineHeight + gapLine + lineHeight + gapLine + lineHeight + gapKillsServer;

        component.setWidth(width);
        component.setHeight(height);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();
        if (x > sr.getScaledWidth()) x = sr.getScaledWidth() - width;
        if (y > sr.getScaledHeight()) y = sr.getScaledHeight() - height;

        RoundedUtils.drawRoundOutline(x, y, width, height, RADIUS, -0.5f, BG_COLOR,
                ColorManager.getColor());

        float cx = x + width / 2f;
        float cursorY = y + PADDING_Y;

        title.drawStringWithShadow(titleText, cx - titleWidth / 2f, cursorY, WHITE_RGB);
        cursorY += getServerName().equals("Singleplayer") ? titleHeight + gapTitleWelcome + 10f : titleHeight + gapTitleWelcome;

        welcome.drawStringWithShadow(welcomeText, cx - welcomeWidth / 2f, cursorY, WHITE_RGB);
        cursorY += welcomeHeight + gapWelcomeKills;

        if (getServerName().equals("Singleplayer")) {
            body.drawStringWithShadow(singleplayerText, cx - singleplayerWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
        } else {
            body.drawStringWithShadow(killsText, cx - killsWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
            cursorY += lineHeight + gapLine;

            body.drawStringWithShadow(timeText, cx - timeWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
            cursorY += lineHeight + gapKillsServer;

            body.drawStringWithShadow(serverText, cx - serverWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    private String formatTimeYuri(long millis) {
        long totalMinutes = millis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0 && minutes == 0) {
            return "less than a minute";
        }
        if (hours == 0 && minutes > 0) {
            return minutes + " mins";
        }
        return hours + " hrs and " + minutes + " mins";
    }

    private String getServerName() {
        return mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }
}