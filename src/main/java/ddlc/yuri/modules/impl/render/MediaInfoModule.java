package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.media.MediaTrack;
import ddlc.yuri.utils.media.MediaTracker;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleInfo(label = "Media Info", description = "Displays the currently playing media on screen.", category = ModuleCategory.RENDER)
public class MediaInfoModule extends Module implements IMinecraft {

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.YURI);

    private enum Mode {
        YURI("Yuri"),
        PULSIVE("Pulsive");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String KEY = "MediaInfo";

    private static final float PADDING_X = 12f;
    private static final float MIN_WIDTH = 170f;
    private static final float PADDING_Y = 8f;
    private static final float RADIUS = 6f;
    private static final float HEADER_PADDING_Y = 5f;
    private static final float GAP_TITLE_ARTIST = 2f;
    private static final float GAP_ARTIST_BAR = 6f;
    private static final float GAP_BAR_TIME = 3f;
    private static final float COVER_SIZE = 34f;
    private static final float GAP_COVER_TEXT = 8f;
    private static final float BAR_HEIGHT = 4f;

    private static final float YURI_MIN_WIDTH = 130f;
    private static final float YURI_PADDING_X = 8f;
    private static final float YURI_PADDING_Y = 8f;
    private static final float YURI_COVER_SIZE = 30f;
    private static final float YURI_BAR_WIDTH = 110f;
    private static final float YURI_GAP_TITLE_COVER = 6f;
    private static final float YURI_GAP_COVER_TRACK = 6f;
    private static final float YURI_GAP_TRACK_ARTIST = 3f;
    private static final float YURI_GAP_ARTIST_BAR = 6f;
    private static final float YURI_GAP_BAR_TIME = 4f;

    private static final Color BG_COLOR = new Color(0, 0, 0, 130);
    private static final Color HEADER_COLOR = new Color(40, 40, 44, 100);
    private static final Color BODY_COLOR = new Color(18, 18, 20, 150);
    private static final Color BAR_BG_COLOR = new Color(255, 255, 255, 40);
    private static final Color COVER_PLACEHOLDER_COLOR = new Color(255, 255, 255, 25);
    private static final Color TEXT_SECONDARY_COLOR = new Color(220, 220, 220);
    private static final Color TEXT_TERTIARY_COLOR = new Color(190, 190, 190);
    private static final Color TEXT_QUATERNARY_COLOR = new Color(150, 150, 150);
    private static final int WHITE_RGB = Color.WHITE.getRGB();
    private static final int TEXT_SECONDARY_RGB = TEXT_SECONDARY_COLOR.getRGB();
    private static final int TEXT_TERTIARY_RGB = TEXT_TERTIARY_COLOR.getRGB();
    private static final int TEXT_QUATERNARY_RGB = TEXT_QUATERNARY_COLOR.getRGB();

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);
    private final MediaTracker tracker = new MediaTracker();

    public MediaInfoModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        component.setWidth(0);
        component.setHeight(0);
        tracker.start();
    }

    @Override
    public void onDisable() {
        component.setWidth(0);
        component.setHeight(0);
        tracker.stop();
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
        if (!tracker.isSupported()) {
            component.setWidth(0);
            component.setHeight(0);
            return;
        }

        switch (mode.getValue()) {
            case YURI:
                renderYuri(shaderPass);
                break;
            case PULSIVE:
                renderPulsive(shaderPass);
                break;
        }
    }

    private void renderYuri(boolean shaderPass) {
        MediaTrack track = tracker.getTrack();

        CustomFontRenderer titleFont = FontUtils.getFont("sf-bold", 15);
        CustomFontRenderer trackFont = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer body = FontUtils.getFont("sf", 13);
        if (titleFont == null || trackFont == null || body == null) return;

        String titleText = "Media Info";
        String trackText = track != null ? track.getTitle() : "No song playing";
        String artistText = track != null ? (track.getArtist().isEmpty() ? track.getSource() : track.getArtist()) : "";

        long position = track != null ? tracker.getPositionMillis() : 0L;
        long lengthMillis = track != null ? track.getLengthMillis() : 0L;
        String timeText = track != null ? formatTime(position) + (lengthMillis > 0 ? " / " + formatTime(lengthMillis) : "") : "";

        float titleWidth = titleFont.getStringWidth(titleText);
        float trackWidth = trackFont.getStringWidth(trackText);
        float artistWidth = body.getStringWidth(artistText);
        float timeWidth = body.getStringWidth(timeText);

        float contentWidth = Math.max(titleWidth, Math.max(YURI_COVER_SIZE, Math.max(trackWidth,
                Math.max(artistWidth, Math.max(YURI_BAR_WIDTH, timeWidth)))));
        float width = Math.max(YURI_MIN_WIDTH, contentWidth + YURI_PADDING_X * 2);

        float titleHeight = titleFont.getHeight();
        float trackHeight = trackFont.getHeight();
        float lineHeight = body.getHeight();

        float height = YURI_PADDING_Y * 2 + titleHeight + YURI_GAP_TITLE_COVER + YURI_COVER_SIZE + YURI_GAP_COVER_TRACK
                + trackHeight + YURI_GAP_TRACK_ARTIST + lineHeight + YURI_GAP_ARTIST_BAR + BAR_HEIGHT
                + YURI_GAP_BAR_TIME + lineHeight;

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
        float cursorY = y + YURI_PADDING_Y;

        titleFont.drawStringWithShadow(titleText, cx - titleWidth / 2f, cursorY, WHITE_RGB);
        cursorY += titleHeight + YURI_GAP_TITLE_COVER;

        ResourceLocation cover = track != null ? tracker.getCoverLocation() : null;
        float coverX = cx - YURI_COVER_SIZE / 2f;
        if (cover != null) {
            drawCoverTexture(cover, coverX, cursorY, YURI_COVER_SIZE);
        } else {
            RoundedUtils.drawCustomRoundedRect(coverX, cursorY, YURI_COVER_SIZE, YURI_COVER_SIZE, 3f,
                    true, true, true, true, COVER_PLACEHOLDER_COLOR);
        }
        cursorY += YURI_COVER_SIZE + YURI_GAP_COVER_TRACK;

        trackFont.drawStringWithShadow(trackText, cx - trackWidth / 2f, cursorY, WHITE_RGB);
        cursorY += trackHeight + YURI_GAP_TRACK_ARTIST;

        body.drawStringWithShadow(artistText, cx - artistWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
        cursorY += lineHeight + YURI_GAP_ARTIST_BAR;

        float barX = cx - YURI_BAR_WIDTH / 2f;
        float progress = (track != null && lengthMillis > 0) ? Math.min(1f, (float) position / (float) lengthMillis) : 0f;

        RoundedUtils.drawCustomRoundedRect(barX, cursorY, YURI_BAR_WIDTH, BAR_HEIGHT, BAR_HEIGHT / 2f,
                true, true, true, true, BAR_BG_COLOR);
        if (progress > 0f) {
            float progressWidth = Math.min(YURI_BAR_WIDTH, Math.max(BAR_HEIGHT, YURI_BAR_WIDTH * progress));
            RoundedUtils.drawCustomRoundedRect(barX, cursorY, progressWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                    true, true, true, true, ColorManager.getColor());
        }
        cursorY += BAR_HEIGHT + YURI_GAP_BAR_TIME;

        body.drawStringWithShadow(timeText, cx - timeWidth / 2f, cursorY, TEXT_SECONDARY_RGB);
    }

    private void renderPulsive(boolean shaderPass) {
        MediaTrack track = tracker.getTrack();

        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 16);
        CustomFontRenderer regular = FontUtils.getFont("sf", 16);
        CustomFontRenderer title = FontUtils.getFont("sf-bold", 14);
        CustomFontRenderer artist = FontUtils.getFont("sf", 13);
        CustomFontRenderer time = FontUtils.getFont("sf", 11);
        if (bold == null || regular == null || title == null || artist == null || time == null) return;

        String nowWord = "now";
        String playingWord = " playing";
        String titleText = track != null ? track.getTitle() : "No song playing";
        String artistText = track != null ? (track.getArtist().isEmpty() ? track.getSource() : track.getArtist()) : "";

        long position = track != null ? tracker.getPositionMillis() : 0L;
        long lengthMillis = track != null ? track.getLengthMillis() : 0L;
        String timeText = track != null ? formatTime(position) + (lengthMillis > 0 ? " / " + formatTime(lengthMillis) : "") : "";

        float nowWidth = bold.getStringWidth(nowWord);
        float playingWidth = regular.getStringWidth(playingWord);
        float headerTitleWidth = nowWidth + playingWidth;
        float headerTitleHeight = Math.max(bold.getHeight(), regular.getHeight());

        float titleWidth = title.getStringWidth(titleText);
        float artistWidth = artist.getStringWidth(artistText);
        float timeWidth = time.getStringWidth(timeText);

        float textBlockWidth = Math.max(titleWidth, Math.max(artistWidth, timeWidth));
        float contentWidth = COVER_SIZE + GAP_COVER_TEXT + textBlockWidth;
        float width = Math.max(MIN_WIDTH, Math.max(headerTitleWidth + PADDING_X * 2, contentWidth + PADDING_X * 2));

        float headerHeight = headerTitleHeight + HEADER_PADDING_Y * 2;

        float textStackHeight = title.getHeight() + GAP_TITLE_ARTIST + artist.getHeight()
                + GAP_ARTIST_BAR + BAR_HEIGHT + GAP_BAR_TIME + time.getHeight();
        float bodyContentHeight = Math.max(COVER_SIZE, textStackHeight);
        float bodyHeight = bodyContentHeight + PADDING_Y * 2;

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

        if (shaderPass) return;

        float cx = x + width / 2f;
        float headerTitleX = cx - headerTitleWidth / 2f;
        float headerTitleY = y + HEADER_PADDING_Y;
        bold.drawStringWithShadow(nowWord, headerTitleX, headerTitleY, WHITE_RGB);
        regular.drawStringWithShadow(playingWord, headerTitleX + nowWidth, headerTitleY, TEXT_SECONDARY_RGB);

        float coverX = x + PADDING_X;
        float coverY = y + headerHeight + PADDING_Y + (bodyContentHeight - COVER_SIZE) / 2f;

        ResourceLocation cover = track != null ? tracker.getCoverLocation() : null;
        if (cover != null) {
            drawCoverTexture(cover, coverX, coverY, COVER_SIZE);
        } else {
            RoundedUtils.drawCustomRoundedRect(coverX, coverY, COVER_SIZE, COVER_SIZE, 4f,
                    true, true, true, true, COVER_PLACEHOLDER_COLOR);
        }

        float textX = coverX + COVER_SIZE + GAP_COVER_TEXT;
        float textY = y + headerHeight + PADDING_Y + (bodyContentHeight - textStackHeight) / 2f;

        title.drawStringWithShadow(titleText, textX, textY, WHITE_RGB);
        textY += title.getHeight() + GAP_TITLE_ARTIST;

        artist.drawStringWithShadow(artistText, textX, textY, TEXT_TERTIARY_RGB);
        textY += artist.getHeight() + GAP_ARTIST_BAR;

        float barWidth = (x + width - PADDING_X) - textX;
        float progress = (track != null && lengthMillis > 0) ? Math.min(1f, (float) position / (float) lengthMillis) : 0f;

        RoundedUtils.drawCustomRoundedRect(textX, textY, barWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                true, true, true, true, BAR_BG_COLOR);
        if (progress > 0f) {
            float progressWidth = Math.min(barWidth, Math.max(BAR_HEIGHT, barWidth * progress));
            RoundedUtils.drawCustomRoundedRect(textX, textY, progressWidth, BAR_HEIGHT, BAR_HEIGHT / 2f,
                    true, true, true, true, ColorManager.getColor());
        }
        textY += BAR_HEIGHT + GAP_BAR_TIME;

        time.drawStringWithShadow(timeText, textX, textY, TEXT_QUATERNARY_RGB);
    }

    private void drawCoverTexture(ResourceLocation location, float x, float y, float size) {
        if (mode.getValue() == Mode.PULSIVE) {
            RoundedUtils.drawRoundedImage(location, x, y, size, size, 4f);
        } else if (mode.getValue() == Mode.YURI) {
            RoundedUtils.drawRoundedImage(location, x, y, size, size, 6f);
        }
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" + seconds : String.valueOf(seconds));
    }
}