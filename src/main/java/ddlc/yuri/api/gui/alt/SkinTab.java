package ddlc.yuri.api.gui.alt;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.alt.comp.FileDialogs;
import ddlc.yuri.api.gui.alt.comp.SkinChanger;
import ddlc.yuri.api.gui.alt.comp.SkinChanger.Profile;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

import static ddlc.yuri.api.gui.alt.YuriAltMenu.*;

final class SkinTab extends AltTab {

    private static final int PREVIEW_SCALE = 5;

    private File chosenFile;
    private String variant = SkinChanger.VARIANT_CLASSIC;
    private ResourceLocation chosenTexture, currentTexture;
    private boolean chosenIs64, currentIs64;
    private String currentVariant = SkinChanger.VARIANT_CLASSIC;
    private String loadedForToken = "";

    private String status = "Pick A PNG Skin File";
    private boolean statusIsError;
    private volatile boolean busy;

    private int panelX, panelY, panelWidth, panelHeight;
    private int previewX, previewY, previewWidth, previewHeight;
    private int chooseX, chooseY, chooseWidth;
    private int classicX, slimX, variantY, variantWidth;
    private int uploadX, uploadY, uploadWidth;
    private int resetX, resetY, resetWidth;
    private int fileY, statusY, tipsY;

    SkinTab(YuriAltMenu menu) {
        super(menu);
    }

    @Override
    String title() {
        return "Skins";
    }

    @Override
    void onShow() {
        loadCurrentSkin();
    }

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);

        panelX = x;
        panelY = y;
        panelWidth = (int) (menu.width * ADD_PANEL_RATIO);
        panelHeight = h;

        previewX = panelX + panelWidth + PADDING;
        previewY = y;
        previewWidth = x + w - previewX;
        previewHeight = h;

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        int startY = panelY + PADDING + fontHeight + PADDING + 6;

        chooseX = panelX + PADDING;
        chooseY = startY;
        chooseWidth = panelWidth - PADDING * 2;

        fileY = chooseY + BUTTON_HEIGHT + PADDING;

        variantY = fileY + fontHeight + PADDING;
        variantWidth = (chooseWidth - BUTTON_SPACING) / 2;
        classicX = panelX + PADDING;
        slimX = classicX + variantWidth + BUTTON_SPACING;

        uploadX = panelX + PADDING;
        uploadY = variantY + BUTTON_HEIGHT + PADDING;
        uploadWidth = chooseWidth;

        resetX = panelX + PADDING;
        resetY = uploadY + BUTTON_HEIGHT + BUTTON_SPACING;
        resetWidth = chooseWidth;

        statusY = resetY + BUTTON_HEIGHT + PADDING + 10;
        tipsY = statusY + fontHeight + PADDING * 2;
    }

    @Override
    void draw(int mouseX, int mouseY) {
        drawControlPanel(mouseX, mouseY);
        drawPreviewPanel();
    }

    private void drawControlPanel(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);

        menu.drawPanelShadow(panelX, panelY, panelWidth, panelHeight);
        RoundedUtils.drawRoundOutline(panelX, panelY, panelWidth, panelHeight, RADIUS, -0.5f, BODY_COLOR, accent);
        menu.drawSectionHeader(panelX + PADDING, panelY + PADDING, "Skin", new ChatComponentText(" Changer").getFormattedText());

        menu.drawButton(chooseX, chooseY, chooseWidth, "Choose PNG File", mouseX, mouseY, chosenFile == null);

        String fileLabel = chosenFile == null ? "No File Selected" : chosenFile.getName();
        int maxWidth = chooseWidth;
        while (regular.getStringWidth(fileLabel) > maxWidth && fileLabel.length() > 4) fileLabel = fileLabel.substring(0, fileLabel.length() - 4) + "...";
        regular.drawStringWithShadow(fileLabel, panelX + PADDING, fileY, chosenFile == null ? 0x999999 : Color.WHITE.getRGB());

        drawToggle(classicX, variantY, variantWidth, "Classic", SkinChanger.VARIANT_CLASSIC.equals(variant), mouseX, mouseY);
        drawToggle(slimX, variantY, variantWidth, "Slim", SkinChanger.VARIANT_SLIM.equals(variant), mouseX, mouseY);

        boolean premium = SkinChanger.isPremiumToken(mc.getSession().getToken());
        menu.drawButton(uploadX, uploadY, uploadWidth, busy ? "Working..." : "Upload Skin", mouseX, mouseY, chosenFile != null && premium && !busy);
        menu.drawButton(resetX, resetY, resetWidth, "Reset To Default", mouseX, mouseY, false);

        menu.drawStatusPill(panelX, panelWidth, statusY, status, statusIsError);

        Gui.drawRect(panelX + PADDING, tipsY - 10, panelX + panelWidth - PADDING, tipsY - 9, RenderUtils.withAlpha(Color.WHITE, 20));
        String tips = premium ? "Changes Apply To " + mc.getSession().getUsername() : "Log In With A Microsoft Account First";
        small.drawString(tips, panelX + (panelWidth - small.getStringWidth(tips)) / 2f, tipsY, premium ? 0x777777 : DANGER.getRGB());
    }

    private void drawToggle(int x, int y, int w, String label, boolean active, int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        boolean hovered = menu.isMouseOverButton(mouseX, mouseY, x, y, w, BUTTON_HEIGHT);
        Color fill = active ? RenderUtils.withAlphaColor(accent, 60) : RenderUtils.withAlphaColor(accent, hovered ? 30 : 12);
        Color outline = active || hovered ? accent : RenderUtils.withAlphaColor(accent, 110);
        RoundedUtils.drawRoundOutline(x, y, w, BUTTON_HEIGHT, RADIUS, active ? 0.5f : -0.5f, fill, outline);

        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        font.drawStringWithShadow(label, x + (w - font.getStringWidth(label)) / 2f, y + (BUTTON_HEIGHT - font.getHeight()) / 2f,
                active ? accent.getRGB() : Color.WHITE.getRGB());
    }

    private void drawPreviewPanel() {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();

        menu.drawPanelShadow(previewX, previewY, previewWidth, previewHeight);
        RoundedUtils.drawRoundOutline(previewX, previewY, previewWidth, previewHeight, RADIUS, -0.5f, BODY_COLOR, accent);
        menu.drawSectionHeader(previewX + PADDING, previewY + PADDING, "Preview", "");

        int dividerY = previewY + PADDING + fontHeight + 10;
        Gui.drawRect(previewX + PADDING, dividerY, previewX + previewWidth - PADDING, dividerY + 1, RenderUtils.withAlpha(Color.WHITE, 20));

        int areaY = dividerY + PADDING;
        int areaHeight = previewY + previewHeight - areaY - PADDING;
        int half = previewWidth / 2;

        drawFigure(previewX + half / 2, areaY, areaHeight, "Current", currentTexture, currentIs64, currentVariant);
        Gui.drawRect(previewX + half, areaY, previewX + half + 1, areaY + areaHeight, RenderUtils.withAlpha(Color.WHITE, 20));
        drawFigure(previewX + half + half / 2, areaY, areaHeight, "New", chosenTexture, chosenIs64, variant);
    }

    private void drawFigure(int centerX, int top, int areaHeight, String label, ResourceLocation texture, boolean is64, String skinVariant) {
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        regular.drawCenteredStringWithShadow(label, centerX, top, Color.WHITE.getRGB());

        int figureHeight = 32 * PREVIEW_SCALE;
        int scale = PREVIEW_SCALE;
        if (figureHeight > areaHeight - regular.getHeight() - PADDING * 2) {
            scale = Math.max(2, (areaHeight - regular.getHeight() - PADDING * 2) / 32);
            figureHeight = 32 * scale;
        }
        int y = top + regular.getHeight() + PADDING + Math.max(0, (areaHeight - regular.getHeight() - PADDING - figureHeight) / 2);

        if (texture == null) {
            small.drawCenteredStringWithShadow(label.equals("New") ? "Choose A File" : "Not Loaded", centerX, y + figureHeight / 2f, 0x777777);
            return;
        }

        boolean slim = SkinChanger.VARIANT_SLIM.equals(skinVariant);
        int armWidth = slim ? 3 : 4;
        int totalWidth = 8 + armWidth * 2;
        int x = centerX - totalWidth * scale / 2;

        mc.getTextureManager().bindTexture(texture);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();

        int headX = x + armWidth * scale;
        int bodyY = y + 8 * scale;
        int legY = y + 20 * scale;

        part(headX, y, 8, 8, 8, 8, scale);
        part(headX, bodyY, 20, 20, 8, 12, scale);
        part(x, bodyY, 44, 20, armWidth, 12, scale);
        if (is64) part(headX + 8 * scale, bodyY, 36, 52, armWidth, 12, scale);
        else part(headX + 8 * scale, bodyY, 44, 20, armWidth, 12, scale);
        part(headX, legY, 4, 20, 4, 12, scale);
        if (is64) part(headX + 4 * scale, legY, 20, 52, 4, 12, scale);
        else part(headX + 4 * scale, legY, 4, 20, 4, 12, scale);

        part(headX, y, 40, 8, 8, 8, scale);
        if (is64) {
            part(headX, bodyY, 20, 36, 8, 12, scale);
            part(x, bodyY, 44, 36, armWidth, 12, scale);
            part(headX + 8 * scale, bodyY, 52, 52, armWidth, 12, scale);
            part(headX, legY, 4, 36, 4, 12, scale);
            part(headX + 4 * scale, legY, 4, 52, 4, 12, scale);
        }
        GlStateManager.disableBlend();
    }

    private static void part(int x, int y, int u, int v, int w, int h, int scale) {
        Gui.drawScaledCustomSizeModalRect(x, y, u, v, w, h, w * scale, h * scale, 64f, 64f);
    }

    @Override
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (menu.isMouseOverButton(mouseX, mouseY, chooseX, chooseY, chooseWidth, BUTTON_HEIGHT)) {
            chooseFile();
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, classicX, variantY, variantWidth, BUTTON_HEIGHT)) {
            variant = SkinChanger.VARIANT_CLASSIC;
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, slimX, variantY, variantWidth, BUTTON_HEIGHT)) {
            variant = SkinChanger.VARIANT_SLIM;
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, uploadX, uploadY, uploadWidth, BUTTON_HEIGHT)) {
            upload();
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, resetX, resetY, resetWidth, BUTTON_HEIGHT)) {
            reset();
            return true;
        }
        return false;
    }

    private void setStatus(String message, boolean isError) {
        status = message;
        statusIsError = isError;
    }

    private void chooseFile() {
        if (busy) return;
        busy = true;
        setStatus("Waiting For File Dialog...", false);
        new Thread(() -> {
            File file = FileDialogs.openFile("Choose Minecraft Skin", "PNG Skins", "png");
            if (file == null) {
                mc.addScheduledTask(() -> {
                    setStatus(chosenFile == null ? "Pick A PNG Skin File" : "Kept " + chosenFile.getName(), false);
                    busy = false;
                });
                return;
            }
            try {
                BufferedImage image = SkinChanger.readSkin(file);
                boolean modern = image.getHeight() == 64;
                BufferedImage padded = modern ? image : padLegacy(image);
                mc.addScheduledTask(() -> {
                    chosenFile = file;
                    chosenIs64 = modern;
                    chosenTexture = replaceTexture("yuri-skin-new", chosenTexture, padded);
                    setStatus("Ready To Upload " + file.getName(), false);
                    busy = false;
                });
            } catch (SkinChanger.SkinException e) {
                mc.addScheduledTask(() -> {
                    setStatus(e.getMessage(), true);
                    busy = false;
                });
            }
        }, "Skin File Dialog").start();
    }

    private void upload() {
        if (busy) return;
        if (chosenFile == null) {
            setStatus("Choose A File First!", true);
            return;
        }
        Session session = mc.getSession();
        if (!SkinChanger.isPremiumToken(session.getToken())) {
            setStatus("Log In With A Microsoft Account First!", true);
            return;
        }
        busy = true;
        setStatus("Uploading Skin...", false);
        File file = chosenFile;
        String chosenVariant = variant;

        new Thread(() -> {
            try {
                Profile profile = SkinChanger.upload(session.getToken(), file, chosenVariant);
                mc.addScheduledTask(() -> {
                    setStatus("Skin Updated For " + (profile.name.isEmpty() ? session.getUsername() : profile.name) + "!", false);
                    busy = false;
                    loadedForToken = "";
                    loadCurrentSkin();
                });
            } catch (SkinChanger.SkinException e) {
                mc.addScheduledTask(() -> {
                    setStatus(e.getMessage(), true);
                    busy = false;
                });
            }
        }, "Skin Upload Worker").start();
    }

    private void reset() {
        if (busy) return;
        Session session = mc.getSession();
        if (!SkinChanger.isPremiumToken(session.getToken())) {
            setStatus("Log In With A Microsoft Account First!", true);
            return;
        }
        busy = true;
        setStatus("Resetting Skin...", false);
        new Thread(() -> {
            try {
                SkinChanger.reset(session.getToken());
                mc.addScheduledTask(() -> {
                    setStatus("Skin Reset To Default!", false);
                    busy = false;
                    loadedForToken = "";
                    loadCurrentSkin();
                });
            } catch (SkinChanger.SkinException e) {
                mc.addScheduledTask(() -> {
                    setStatus(e.getMessage(), true);
                    busy = false;
                });
            }
        }, "Skin Reset Worker").start();
    }

    private void loadCurrentSkin() {
        Session session = mc.getSession();
        String token = session.getToken();
        if (!SkinChanger.isPremiumToken(token) || token.equals(loadedForToken)) return;
        loadedForToken = token;

        new Thread(() -> {
            try {
                Profile profile = SkinChanger.fetchProfile(token);
                BufferedImage image = null;
                if (!profile.skinUrl.isEmpty()) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(profile.skinUrl).openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    image = ImageIO.read(connection.getInputStream());
                }
                if (image == null) {
                    image = ImageIO.read(mc.getResourceManager().getResource(new ResourceLocation("textures/entity/steve.png")).getInputStream());
                }
                boolean modern = image.getHeight() == 64;
                BufferedImage padded = modern ? image : padLegacy(image);
                mc.addScheduledTask(() -> {
                    currentIs64 = modern;
                    currentVariant = profile.variant;
                    currentTexture = replaceTexture("yuri-skin-current", currentTexture, padded);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "Skin Fetch Worker").start();
    }

    private ResourceLocation replaceTexture(String name, ResourceLocation previous, BufferedImage image) {
        if (previous != null) mc.getTextureManager().deleteTexture(previous);
        return mc.getTextureManager().getDynamicTextureLocation(name, new DynamicTexture(image));
    }

    private static BufferedImage padLegacy(BufferedImage legacy) {
        BufferedImage padded = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        padded.getGraphics().drawImage(legacy, 0, 0, null);
        return padded;
    }
}
