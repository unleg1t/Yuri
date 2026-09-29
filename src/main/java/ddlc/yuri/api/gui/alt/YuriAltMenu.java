package ddlc.yuri.api.gui.alt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.alt.comp.CustomTextBox;
import ddlc.yuri.api.gui.alt.comp.MicrosoftOAuthTranslation;
import ddlc.yuri.api.gui.alt.comp.SessionChanger;
import ddlc.yuri.api.gui.alt.comp.TokenEncryption;
import ddlc.yuri.api.gui.main.YuriMenu;
import ddlc.yuri.api.gui.main.api.MenuShaderBackground;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class YuriAltMenu extends GuiScreen {

    private static final Color BACKGROUND = new Color(14, 14, 17, 255);
    private static final Color BODY_COLOR = new Color(0, 0, 0, 130);
    private static final Color HEADER_COLOR = new Color(0, 0, 0, 110);
    private static final Color DANGER = new Color(232, 90, 90);
    private static final Color SUCCESS = new Color(90, 210, 130);
    private static final Color MUTED = new Color(138, 138, 147);
    private static final Color CRACKED_COLOR = new Color(150, 150, 158);
    private static final ResourceLocation PLACEHOLDER_HEAD = new ResourceLocation("yuri/gui/steve.png");
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM_SOURCE = new SecureRandom();

    private static final float RADIUS = 6f;
    private static final int HEADER_HEIGHT = 44;
    private static final int PADDING = 12;
    private static final int FIELD_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 27;
    private static final int BUTTON_SPACING = 8;
    private static final int SHADOW_OFFSET = 3;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int MAX_COLUMNS = 3;
    private static final int MIN_CELL_WIDTH = 150;
    private static final int ENTRY_PADDING = 7;
    private static final int ENTRY_HEIGHT = 50;
    private static final int STATUS_HEIGHT = 22;
    private static final int CHIP_HEIGHT = 20;
    private static final float ADD_PANEL_RATIO = 0.32f;

    private final ArrayList<Integer> selectedAlts = new ArrayList<>();
    private final ArrayList<String> alts = new ArrayList<>();
    private final Map<String, ResourceLocation> headCache = new HashMap<>();
    private final Map<String, Boolean> headLoading = new HashMap<>();
    private final Map<String, Integer> headTries = new HashMap<>();

    private CustomTextBox username, tokenField;
    private int scrollOffset = 0;
    private float scrollAnim = 0f;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int scrollStart;
    private String statusString = "Ready to work!";
    private boolean statusIsError = false;
    private boolean isLoggingIn = false;

    private int contentY, contentHeight;
    private int addX, addY, addWidth, addHeight;
    private int accountsX, accountsY, accountsWidth, accountsHeight;
    private int accountsDividerY;

    private int addTitleY, titleDividerY, offlineLabelY, msLabelY, tokenLabelY, divider1Y, divider2Y;
    private int primaryButtonX, primaryButtonY, primaryButtonWidth;
    private int oauthButtonX, oauthButtonY, oauthButtonWidth;
    private int generateButtonX, generateButtonY, generateButtonWidth;
    private int tokenButtonX, tokenButtonY, tokenButtonWidth;
    private int statusPillY, tipsDividerY, tipsY;
    private int backButtonWidth;

    private int columns = MAX_COLUMNS;
    private int gridListX, gridListY, gridListWidth, gridListHeight;
    private int gridCellWidth = 1, gridRowStride = ENTRY_HEIGHT + ENTRY_PADDING, gridVisibleRows = 1;
    private int scrollbarX, scrollbarY, scrollbarHeight;

    private int deleteChipX, deleteChipY, deleteChipWidth;
    private int selectedChipX, selectedChipWidth;

    @Override
    public void initGui() {
        alts.clear();
        loadAltsFromFile();

        selectedAlts.clear();
        buttonList.clear();
        scrollOffset = 0;
        scrollAnim = 0f;

        username = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        username.setPlaceholder("Username");

        tokenField = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        tokenField.setPlaceholder("Access / Refresh token");

        super.initGui();
        computeLayout();
    }

    private File getYuriDir() {
        File dir = new File(Minecraft.getMinecraft().mcDataDir, "Yuri");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private void appendLine(String fileName, String line) {
        File file = new File(getYuriDir(), fileName);
        try (FileWriter fw = new FileWriter(file, true); PrintWriter out = new PrintWriter(fw)) {
            out.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAltsFromFile() {
        File file = new File(getYuriDir(), "alts.txt");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && (line.startsWith("cracked|") || line.startsWith("microsoftOAuth|") || line.startsWith("token|"))) {
                    alts.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAltsToFile() {
        File file = new File(getYuriDir(), "alts.txt");
        try (PrintWriter out = new PrintWriter(file)) {
            for (String alt : alts) out.println(alt);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, width, height, BACKGROUND.getRGB());
        MenuShaderBackground.get().render(width, height);

        computeLayout();
        updateScroll();

        drawHeader(mouseX, mouseY);
        drawAddAccountPanel(mouseX, mouseY);
        drawAccountsPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private int totalRows() {
        return (int) Math.ceil(alts.size() / (float) columns);
    }

    private int fullVisibleRows() {
        return Math.max(1, (gridListHeight + ENTRY_PADDING) / gridRowStride);
    }

    private int maxScroll() {
        return Math.max(0, totalRows() - fullVisibleRows());
    }

    private void updateScroll() {
        scrollOffset = Math.min(maxScroll(), Math.max(0, scrollOffset));
        float diff = scrollOffset - scrollAnim;
        if (Math.abs(diff) < 0.005f) scrollAnim = scrollOffset;
        else scrollAnim += diff * 0.25f;
        scrollAnim = Math.min(maxScroll(), Math.max(0f, scrollAnim));
    }

    private void computeLayout() {
        contentY = HEADER_HEIGHT + PADDING + 1;
        contentHeight = height - contentY - PADDING;

        addWidth = Math.max(210, Math.min(280, (int) (width * ADD_PANEL_RATIO)));
        addX = PADDING;
        addY = contentY;
        addHeight = contentHeight;

        accountsX = addX + addWidth + PADDING;
        accountsY = contentY;
        accountsWidth = width - accountsX - PADDING;
        accountsHeight = contentHeight;

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        int fontHeight = regular.getHeight();
        int smallHeight = small.getHeight();

        int innerX = addX + PADDING;
        int innerW = addWidth - PADDING * 2;

        int y = addY + PADDING;
        addTitleY = y;
        y += fontHeight + 8;
        titleDividerY = y;
        y += PADDING + 1;

        offlineLabelY = y;
        y += smallHeight + 6;
        username.xPosition = innerX;
        username.yPosition = y;
        username.setWidth(innerW);
        y += FIELD_HEIGHT + BUTTON_SPACING;

        generateButtonWidth = (innerW - BUTTON_SPACING) * 2 / 5;
        primaryButtonWidth = innerW - BUTTON_SPACING - generateButtonWidth;
        primaryButtonX = innerX;
        primaryButtonY = y;
        generateButtonX = primaryButtonX + primaryButtonWidth + BUTTON_SPACING;
        generateButtonY = y;
        y += BUTTON_HEIGHT + PADDING;

        divider1Y = y;
        y += 1 + PADDING;

        msLabelY = y;
        y += smallHeight + 6;
        oauthButtonX = innerX;
        oauthButtonY = y;
        oauthButtonWidth = innerW;
        y += BUTTON_HEIGHT + PADDING;

        divider2Y = y;
        y += 1 + PADDING;

        tokenLabelY = y;
        y += smallHeight + 6;
        tokenField.xPosition = innerX;
        tokenField.yPosition = y;
        tokenField.setWidth(innerW);
        y += FIELD_HEIGHT + BUTTON_SPACING;

        tokenButtonX = innerX;
        tokenButtonY = y;
        tokenButtonWidth = innerW;

        tipsY = addY + addHeight - PADDING - smallHeight * 2 - 2;
        tipsDividerY = tipsY - 8;
        statusPillY = tipsDividerY - 10 - STATUS_HEIGHT;

        backButtonWidth = regular.getStringWidth("Back") + 8;

        accountsDividerY = accountsY + PADDING + fontHeight + 8;

        gridListX = accountsX + PADDING;
        gridListY = accountsDividerY + PADDING;
        int rawWidth = accountsWidth - PADDING * 2 - SCROLLBAR_WIDTH - 8;
        columns = Math.max(1, Math.min(MAX_COLUMNS, (rawWidth + ENTRY_PADDING) / MIN_CELL_WIDTH));
        gridCellWidth = Math.max(1, (rawWidth + ENTRY_PADDING) / columns);
        gridListWidth = rawWidth;
        gridListHeight = accountsY + accountsHeight - gridListY - PADDING;

        gridRowStride = ENTRY_HEIGHT + ENTRY_PADDING;
        gridVisibleRows = fullVisibleRows() + 2;

        scrollbarX = accountsX + accountsWidth - PADDING - SCROLLBAR_WIDTH;
        scrollbarY = gridListY;
        scrollbarHeight = gridListHeight;

        deleteChipWidth = regular.getStringWidth("Delete") + 16;
        deleteChipX = accountsX + accountsWidth - PADDING - deleteChipWidth;
        deleteChipY = accountsY + PADDING - 2;
        selectedChipWidth = regular.getStringWidth(selectedAlts.size() + " selected") + 16;
        selectedChipX = deleteChipX - 6 - selectedChipWidth;
    }

    private void drawPanelShadow(int x, int y, int w, int h) {
        RoundedUtils.drawRoundOutline(x + SHADOW_OFFSET, y + SHADOW_OFFSET, w, h, RADIUS, -0.4f,
                RenderUtils.withAlphaColor(Color.BLACK, 90), RenderUtils.withAlphaColor(Color.BLACK, 0));
    }

    private void drawPanel(int x, int y, int w, int h) {
        RoundedUtils.drawRoundOutline(x, y, w, h, RADIUS, -0.4f, BODY_COLOR, RenderUtils.withAlphaColor(Color.WHITE, 28));
    }

    private void drawDivider(int x1, int y, int x2) {
        Gui.drawRect(x1, y, x2, y + 1, RenderUtils.withAlpha(Color.WHITE, 20));
    }

    private void drawTitle(int x, int y, String bold, String rest) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer boldFont = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regularFont = FontUtils.getFont("sf", 18);
        boldFont.drawStringWithShadow(bold, x, y, accent.getRGB());
        regularFont.drawStringWithShadow(rest, x + boldFont.getStringWidth(bold), y, MUTED.getRGB());
    }

    private void drawSectionLabel(int x, int y, String label) {
        FontUtils.getFont("sf", 14).drawString(label, x, y, MUTED.getRGB());
    }

    private String fit(CustomFontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && font.getStringWidth(cut + "..") > maxWidth) cut = cut.substring(0, cut.length() - 1);
        return cut + "..";
    }

    private void drawHeader(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();

        Gui.drawRect(0, 0, width, HEADER_HEIGHT, HEADER_COLOR.getRGB());
        Gui.drawRect(0, HEADER_HEIGHT, width, HEADER_HEIGHT + 1, RenderUtils.withAlpha(Color.WHITE, 20));
        int underlineWidth = 60;
        Gui.drawRect(width / 2 - underlineWidth / 2, HEADER_HEIGHT, width / 2 + underlineWidth / 2, HEADER_HEIGHT + 2, accent.getRGB());

        boolean backHovered = isMouseOverButton(mouseX, mouseY, PADDING - 6, 6, backButtonWidth + 12, HEADER_HEIGHT - 12);
        RoundedUtils.drawRoundOutline(PADDING - 6, 6, backButtonWidth + 12, HEADER_HEIGHT - 12, RADIUS, -0.4f,
                RenderUtils.withAlphaColor(Color.WHITE, backHovered ? 22 : 8), RenderUtils.withAlphaColor(Color.WHITE, backHovered ? 40 : 18));
        regular.drawStringWithShadow("Back", PADDING, (HEADER_HEIGHT - fontHeight) / 2, backHovered ? accent.getRGB() : Color.WHITE.getRGB());

        String titleBold = "Yuri";
        String titleRest = " Account Manager";
        float titleBoldWidth = bold.getStringWidth(titleBold);
        float titleTotalWidth = titleBoldWidth + regular.getStringWidth(titleRest);
        float titleX = width / 2f - titleTotalWidth / 2f;
        float titleY = (HEADER_HEIGHT - fontHeight) / 2f;

        bold.drawStringWithShadow(titleBold, titleX, titleY, accent.getRGB());
        regular.drawStringWithShadow(titleRest, titleX + titleBoldWidth, titleY, Color.WHITE.getRGB());

        String currentUser = Minecraft.getMinecraft().getSession().getUsername();
        String pillLabel = "Signed in as " + currentUser;
        int pillTextWidth = regular.getStringWidth(pillLabel);
        int dotSize = 6;
        int pillPaddingX = 10;
        int pillHeight = 24;
        int pillWidth = dotSize + 6 + pillTextWidth + pillPaddingX * 2;
        int pillX = width - PADDING - pillWidth;
        int pillY = (HEADER_HEIGHT - pillHeight) / 2;

        RoundedUtils.drawRoundOutline(pillX, pillY, pillWidth, pillHeight, RADIUS, -0.4f, RenderUtils.withAlphaColor(accent, 25), RenderUtils.withAlphaColor(accent, 130));
        int dotX = pillX + pillPaddingX;
        int dotY = pillY + (pillHeight - dotSize) / 2;
        RoundedUtils.drawRoundOutline(dotX, dotY, dotSize, dotSize, 2.0f, -0.4f, SUCCESS, RenderUtils.withAlphaColor(Color.BLACK, 0));
        regular.drawString(pillLabel, dotX + dotSize + 6, pillY + (pillHeight - fontHeight) / 2f, Color.WHITE.getRGB());
    }

    private void drawAddAccountPanel(int mouseX, int mouseY) {
        drawPanel(addX, addY, addWidth, addHeight);

        int innerX = addX + PADDING;
        int innerRight = addX + addWidth - PADDING;

        drawTitle(innerX, addTitleY, "Add", " account");
        drawDivider(innerX, titleDividerY, innerRight);

        drawSectionLabel(innerX, offlineLabelY, "OFFLINE");
        username.drawTextBox();
        drawButton(primaryButtonX, primaryButtonY, primaryButtonWidth, "Login", mouseX, mouseY, true, !isLoggingIn);
        drawButton(generateButtonX, generateButtonY, generateButtonWidth, "Random", mouseX, mouseY, false, true);

        drawDivider(innerX, divider1Y, innerRight);

        drawSectionLabel(innerX, msLabelY, "MICROSOFT");
        drawButton(oauthButtonX, oauthButtonY, oauthButtonWidth, "Sign in with Microsoft", mouseX, mouseY, false, !isLoggingIn);

        drawDivider(innerX, divider2Y, innerRight);

        drawSectionLabel(innerX, tokenLabelY, "TOKEN");
        tokenField.drawTextBox();
        drawButton(tokenButtonX, tokenButtonY, tokenButtonWidth, "Login with token", mouseX, mouseY, false, !isLoggingIn);

        drawStatusPill();

        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        drawDivider(innerX, tipsDividerY, innerRight);
        String tipA = "Alt+Click select  \u00b7  Alt+A select all";
        String tipB = "Alt+Backspace delete selected";
        small.drawString(tipA, addX + (addWidth - small.getStringWidth(tipA)) / 2f, tipsY, MUTED.getRGB());
        small.drawString(tipB, addX + (addWidth - small.getStringWidth(tipB)) / 2f, tipsY + small.getHeight() + 2, MUTED.getRGB());
    }

    private void drawStatusPill() {
        if (statusString == null || statusString.isEmpty()) return;

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        Color tint = statusIsError ? DANGER : ColorManager.getColor();
        int maxWidth = addWidth - PADDING * 2 - 20;
        String text = fit(regular, statusString, maxWidth);
        int pillWidth = regular.getStringWidth(text) + 20;
        int pillX = addX + (addWidth - pillWidth) / 2;

        RoundedUtils.drawRoundOutline(pillX, statusPillY, pillWidth, STATUS_HEIGHT, RADIUS, -0.4f,
                RenderUtils.withAlphaColor(tint, 25), RenderUtils.withAlphaColor(tint, 130));
        regular.drawCenteredStringWithShadow(text, addX + addWidth / 2f, statusPillY + (STATUS_HEIGHT - regular.getHeight()) / 2f, tint.getRGB());
    }

    private void drawButton(int x, int y, int w, String label, int mouseX, int mouseY, boolean primary, boolean enabled) {
        boolean hovered = enabled && isMouseOverButton(mouseX, mouseY, x, y, w, BUTTON_HEIGHT);
        Color accent = ColorManager.getColor();
        int dim = enabled ? 1 : 2;

        int fillAlpha = primary ? (hovered ? 220 : 190) : (hovered ? 45 : 18);
        int outlineAlpha = primary ? 255 : (hovered ? 255 : 130);
        Color fill = RenderUtils.withAlphaColor(accent, fillAlpha / dim);
        Color outline = RenderUtils.withAlphaColor(accent, outlineAlpha / dim);

        RoundedUtils.drawRoundOutline(x, y, w, BUTTON_HEIGHT, RADIUS, -0.4f, fill, outline);

        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        String text = fit(font, label, w - 12);
        int textColor;
        if (primary) textColor = enabled ? Color.BLACK.getRGB() : new Color(20, 20, 20, 160).getRGB();
        else if (!enabled) textColor = MUTED.getRGB();
        else textColor = hovered ? accent.getRGB() : Color.WHITE.getRGB();
        int textX = x + (w - font.getStringWidth(text)) / 2;
        int textY = y + (BUTTON_HEIGHT - font.getHeight()) / 2;
        if (primary) font.drawString(text, textX, textY, textColor);
        else font.drawStringWithShadow(text, textX, textY, textColor);
    }

    private void drawChip(int x, int y, int w, String label, Color tint, boolean hovered) {
        RoundedUtils.drawRoundOutline(x, y, w, CHIP_HEIGHT, RADIUS, -0.4f,
                RenderUtils.withAlphaColor(tint, hovered ? 60 : 28), RenderUtils.withAlphaColor(tint, hovered ? 220 : 150));
        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        font.drawCenteredStringWithShadow(label, x + w / 2f, y + (CHIP_HEIGHT - font.getHeight()) / 2f, tint.getRGB());
    }

    private void drawAccountsPanel(int mouseX, int mouseY) {
        drawPanel(accountsX, accountsY, accountsWidth, accountsHeight);

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();
        Color accent = ColorManager.getColor();

        drawTitle(accountsX + PADDING, accountsY + PADDING, "Accounts", "  " + alts.size());

        if (!selectedAlts.isEmpty()) {
            drawChip(selectedChipX, deleteChipY, selectedChipWidth, selectedAlts.size() + " selected", accent, false);
            boolean deleteHovered = isMouseOverButton(mouseX, mouseY, deleteChipX, deleteChipY, deleteChipWidth, CHIP_HEIGHT);
            drawChip(deleteChipX, deleteChipY, deleteChipWidth, "Delete", DANGER, deleteHovered);
        }

        drawDivider(accountsX + PADDING, accountsDividerY, accountsX + accountsWidth - PADDING);

        if (alts.isEmpty()) {
            float centerX = accountsX + accountsWidth / 2f;
            float centerY = gridListY + gridListHeight / 2f - fontHeight;
            regular.drawCenteredStringWithShadow("No accounts yet", centerX, centerY, Color.WHITE.getRGB());
            regular.drawCenteredStringWithShadow("Add one using the form on the left", centerX, centerY + fontHeight + 4, MUTED.getRGB());
            return;
        }

        enableScissor(gridListX, gridListY, gridListWidth, gridListHeight);

        int firstRow = (int) Math.floor(scrollAnim);
        int shift = (int) ((scrollAnim - firstRow) * gridRowStride);
        boolean mouseInGrid = mouseX >= gridListX && mouseX <= gridListX + gridListWidth && mouseY >= gridListY && mouseY <= gridListY + gridListHeight;

        for (int row = 0; row < gridVisibleRows; row++) {
            for (int col = 0; col < columns; col++) {
                int altIndex = (firstRow + row) * columns + col;
                if (altIndex >= alts.size()) break;

                int x = gridListX + col * gridCellWidth;
                int y = gridListY + row * gridRowStride - shift;

                String[] parts = alts.get(altIndex).split("\\|", 4);
                String type = parts[0];
                boolean premium = type.equals("microsoftOAuth") || type.equals("token");
                String altName = parts.length > 1 ? parts[1] : "Unknown";
                String uuid = (type.equals("token") && parts.length > 2) ? parts[2] : (premium ? altName : "");

                drawAccountCell(x, y, gridCellWidth - ENTRY_PADDING, ENTRY_HEIGHT, altName, uuid, type, altIndex, mouseInGrid ? mouseX : -1, mouseInGrid ? mouseY : -1);
            }
        }

        disableScissor();

        int maxScroll = maxScroll();
        if (maxScroll <= 0) return;

        int totalRows = totalRows();
        int thumbHeight = Math.max(scrollbarHeight * fullVisibleRows() / Math.max(1, totalRows), 20);
        int thumbY = scrollbarY + (int) ((scrollbarHeight - thumbHeight) * scrollAnim / maxScroll);
        boolean scrollbarHovered = mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + SCROLLBAR_WIDTH + 2 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;

        RoundedUtils.drawRoundOutline(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight, SCROLLBAR_WIDTH / 2f, -0.4f,
                RenderUtils.withAlphaColor(Color.WHITE, 14), RenderUtils.withAlphaColor(Color.BLACK, 0));
        RoundedUtils.drawRoundOutline(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH / 2f, -0.4f,
                (draggingScrollbar || scrollbarHovered) ? accent : RenderUtils.withAlphaColor(accent, 150),
                RenderUtils.withAlphaColor(Color.BLACK, 0));
    }

    private static String typeLabel(String type) {
        switch (type) {
            case "microsoftOAuth": return "Microsoft";
            case "token": return "Token";
            default: return "Cracked";
        }
    }

    private void drawAccountCell(int x, int y, int w, int h, String text, String uuid, String type, int index, int mouseX, int mouseY) {
        boolean selected = selectedAlts.contains(index);
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        Color accent = ColorManager.getColor();
        String current = Minecraft.getMinecraft().getSession().getUsername();
        boolean active = text.equals(current);

        Color fill = selected ? RenderUtils.withAlphaColor(accent, 35) : (hovered ? RenderUtils.withAlphaColor(Color.WHITE, 14) : RenderUtils.withAlphaColor(Color.BLACK, 90));
        Color outline = selected ? accent : (hovered ? RenderUtils.withAlphaColor(accent, 150) : (active ? RenderUtils.withAlphaColor(SUCCESS, 140) : RenderUtils.withAlphaColor(Color.WHITE, 25)));
        RoundedUtils.drawRoundOutline(x, y, w, h, RADIUS, selected ? 0.5f : -0.4f, fill, outline);

        loadHead(uuid);
        drawHead(x, y, uuid, h);

        int avatarSize = h - ENTRY_PADDING * 2;
        boolean premium = type.equals("microsoftOAuth") || type.equals("token");
        Color typeColor = premium ? SUCCESS : CRACKED_COLOR;

        int boxSize = 10;
        int boxX = x + w - ENTRY_PADDING - boxSize;
        int boxY = y + (h - boxSize) / 2;
        if (selected || hovered || !selectedAlts.isEmpty()) {
            RoundedUtils.drawRoundOutline(boxX, boxY, boxSize, boxSize, 3f, -0.4f,
                    selected ? accent : RenderUtils.withAlphaColor(Color.WHITE, 10),
                    selected ? accent : RenderUtils.withAlphaColor(Color.WHITE, 60));
            if (selected) Gui.drawRect(boxX + 3, boxY + 3, boxX + boxSize - 3, boxY + boxSize - 3, Color.BLACK.getRGB());
        }

        int textX = x + ENTRY_PADDING + avatarSize + ENTRY_PADDING;
        int textMax = boxX - 6 - textX;
        CustomFontRenderer nameFont = FontUtils.getFont("sf", 18);
        CustomFontRenderer typeFont = FontUtils.getFont("sf", 14);
        int blockHeight = nameFont.getHeight() + 3 + typeFont.getHeight();
        int nameY = y + (h - blockHeight) / 2;
        int typeY = nameY + nameFont.getHeight() + 3;

        nameFont.drawString(fit(nameFont, text, textMax), textX, nameY, selected ? accent.getRGB() : Color.WHITE.getRGB());

        int dotSize = 5;
        RoundedUtils.drawRoundOutline(textX, typeY + (typeFont.getHeight() - dotSize) / 2, dotSize, dotSize, 2.0f, -0.4f, typeColor, RenderUtils.withAlphaColor(Color.BLACK, 0));
        String label = typeLabel(type);
        int labelX = textX + dotSize + 4;
        typeFont.drawString(label, labelX, typeY, typeColor.getRGB());
        if (active) typeFont.drawString("\u00b7 Active", labelX + typeFont.getStringWidth(label) + 4, typeY, SUCCESS.getRGB());
    }

    public void loadHead(String uuid) {
        if (uuid == null || uuid.isEmpty()) return;
        if (headCache.containsKey(uuid)) return;
        if (headLoading.getOrDefault(uuid, false)) return;
        if (headTries.getOrDefault(uuid, 0) > 5) return;

        headLoading.put(uuid, true);
        headTries.put(uuid, headTries.getOrDefault(uuid, 0) + 1);
        headCache.put(uuid, PLACEHOLDER_HEAD);

        new Thread(() -> {
            try {
                URI uri = URI.create("https://mc-heads.net/avatar/" + uuid);
                URLConnection connection = uri.toURL().openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                connection.setRequestProperty("Accept", "image/png");

                BufferedImage image = ImageIO.read(connection.getInputStream());
                if (image == null) throw new IOException("Failed to read image");

                mc.addScheduledTask(() -> {
                    DynamicTexture texture = new DynamicTexture(image);
                    ResourceLocation head = mc.getTextureManager().getDynamicTextureLocation("HEAD-" + uuid, texture);
                    headCache.put(uuid, head);
                    headLoading.put(uuid, false);
                });
            } catch (IOException e) {
                e.printStackTrace();
                headLoading.put(uuid, false);
            }
        }).start();
    }

    public void drawHead(int x, int y, String uuid, int cellHeight) {
        ResourceLocation head = uuid == null || uuid.isEmpty() ? PLACEHOLDER_HEAD : headCache.getOrDefault(uuid, PLACEHOLDER_HEAD);
        int size = cellHeight - (ENTRY_PADDING * 2);

        RoundedUtils.drawRoundedImage(head, x + ENTRY_PADDING, y + ENTRY_PADDING, size, size, RADIUS);
    }

    private void enableScissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - y - h) * scale, w * scale, h * scale);
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void setStatus(String message, boolean isError) {
        statusString = message;
        statusIsError = isError;
    }

    private void deleteSelected() {
        if (selectedAlts.isEmpty()) return;
        selectedAlts.sort((a, b) -> b - a);
        for (int index : selectedAlts) {
            if (index >= 0 && index < alts.size()) alts.remove(index);
        }
        selectedAlts.clear();
        saveAltsToFile();
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        username.mouseClicked(mouseX, mouseY, mouseButton);
        tokenField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (isMouseOverButton(mouseX, mouseY, PADDING - 6, 6, backButtonWidth + 12, HEADER_HEIGHT - 12)) {
            mc.displayGuiScreen(new YuriMenu());
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, primaryButtonX, primaryButtonY, primaryButtonWidth, BUTTON_HEIGHT)) {
            handleCrackedLogin(username.getText());
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, oauthButtonX, oauthButtonY, oauthButtonWidth, BUTTON_HEIGHT)) {
            handleOAuthLogin();
            return;
        }

        if (isMouseOverButton(mouseX, mouseY, generateButtonX, generateButtonY, generateButtonWidth, BUTTON_HEIGHT)) {
            handleCrackedLogin(generateRandomString());
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, tokenButtonX, tokenButtonY, tokenButtonWidth, BUTTON_HEIGHT)) {
            handleTokenLogin();
            return;
        }

        if (!selectedAlts.isEmpty() && isMouseOverButton(mouseX, mouseY, deleteChipX, deleteChipY, deleteChipWidth, CHIP_HEIGHT)) {
            deleteSelected();
            return;
        }

        boolean inGrid = mouseX >= gridListX && mouseX < gridListX + gridListWidth && mouseY >= gridListY && mouseY < gridListY + gridListHeight;
        if (inGrid) {
            int relX = mouseX - gridListX;
            int relY = mouseY - gridListY + (int) (scrollAnim * gridRowStride);
            int col = relX / gridCellWidth;
            int row = relY / gridRowStride;
            boolean inCell = relX % gridCellWidth < gridCellWidth - ENTRY_PADDING && relY % gridRowStride < ENTRY_HEIGHT;
            int index = row * columns + col;

            if (inCell && col >= 0 && col < columns && row >= 0 && index >= 0 && index < alts.size()) {
                if (GuiScreen.isAltKeyDown()) {
                    if (selectedAlts.contains(index)) selectedAlts.remove((Integer) index);
                    else selectedAlts.add(index);
                } else if (!isLoggingIn) {
                    loginWithAlt(alts.get(index));
                }
                return;
            }
        }

        if (mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + SCROLLBAR_WIDTH + 2 && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            scrollStart = scrollOffset;
        }
    }

    private void loginWithAlt(String alt) {
        String[] parts = alt.split("\\|");
        if (alt.startsWith("cracked|")) {
            SessionChanger.getInstance().setUserOffline(parts[1]);
            setStatus("Logged in with " + parts[1] + "!", false);
        } else if (alt.startsWith("microsoftOAuth|")) {
            loginWithStoredMicrosoftAlt(parts[1]);
        } else if (alt.startsWith("token|")) {
            if (parts.length >= 4) {
                mc.setSession(new Session(parts[1], parts[2], parts[3], "mojang"));
                setStatus("Logged in with " + parts[1] + "!", false);
            }
        }
    }

    private void loginWithStoredMicrosoftAlt(String user) {
        String refreshToken = loadRefreshToken(user);
        if (refreshToken == null) {
            setStatus("No stored token for " + user + "!", true);
            return;
        }

        isLoggingIn = true;
        setStatus("Authenticating " + user + "...", false);

        new Thread(() -> {
            try {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                mc.addScheduledTask(() -> {
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        setStatus("Logged in with " + login.username + "!", false);
                    } else {
                        setStatus("Stored token for " + user + " is no longer valid!", true);
                    }
                    isLoggingIn = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    setStatus("Microsoft auth failed for " + user + "!", true);
                    isLoggingIn = false;
                });
            }
        }, "Stored Microsoft Alt Auth Worker").start();
    }

    private void handleTokenLogin() {
        if (isLoggingIn) return;
        String rawToken = tokenField.getText().trim();
        if (rawToken.isEmpty()) {
            setStatus("Enter a token first!", true);
            return;
        }
        isLoggingIn = true;

        if (MicrosoftOAuthTranslation.isRefreshToken(rawToken)) {
            handleRefreshTokenLogin(rawToken);
        } else {
            handleAccessTokenLogin(rawToken);
        }
    }

    private void handleRefreshTokenLogin(String refreshToken) {
        setStatus("Authenticating refresh token...", false);

        new Thread(() -> {
            try {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                mc.addScheduledTask(() -> {
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        saveOAuthAltToFile(login.username, login.newRefreshToken != null ? login.newRefreshToken : refreshToken);
                        tokenField.setText("");
                        setStatus("Logged in via token as " + login.username + "!", false);
                    } else {
                        setStatus("Invalid refresh token!", true);
                    }
                    isLoggingIn = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    setStatus("Refresh token auth failed!", true);
                    isLoggingIn = false;
                });
            }
        }, "Refresh Token Auth Worker").start();
    }

    private void handleAccessTokenLogin(String rawToken) {
        setStatus("Authenticating token...", false);

        new Thread(() -> {
            try {
                URL profUrl = new URL("https://api.minecraftservices.com/minecraft/profile");
                HttpURLConnection profConn = (HttpURLConnection) profUrl.openConnection();
                profConn.setRequestMethod("GET");
                profConn.setRequestProperty("Authorization", "Bearer " + rawToken);

                int responseCode = profConn.getResponseCode();
                if (responseCode != 200) {
                    mc.addScheduledTask(() -> {
                        setStatus("Invalid token! (HTTP " + responseCode + ")", true);
                        isLoggingIn = false;
                    });
                    return;
                }

                StringBuilder profRespStr = new StringBuilder();
                try (Scanner profScan = new Scanner(profConn.getInputStream(), "UTF-8")) {
                    while (profScan.hasNextLine()) profRespStr.append(profScan.nextLine());
                }
                JsonObject profileRes = new JsonParser().parse(profRespStr.toString()).getAsJsonObject();

                String profileName = profileRes.get("name").getAsString();
                String profileId = profileRes.get("id").getAsString();

                mc.addScheduledTask(() -> {
                    mc.setSession(new Session(profileName, profileId, rawToken, "mojang"));
                    saveTokenAltToFile(profileName, profileId, rawToken);
                    tokenField.setText("");
                    setStatus("Logged in via token as " + profileName + "!", false);
                    isLoggingIn = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    setStatus("Token auth failed!", true);
                    isLoggingIn = false;
                });
            }
        }, "Token Auth Worker").start();
    }

    private void saveTokenAltToFile(String name, String uuid, String mcToken) {
        String entry = "token|" + name + "|" + uuid + "|" + mcToken;
        appendLine("alts.txt", entry);
        alts.add(entry);
    }

    private void saveOAuthAltToFile(String username, String refreshToken) {
        String entry = "microsoftOAuth|" + username;
        appendLine("alts.txt", entry);
        alts.add(entry);
        appendLine("tokens.txt", username + "|" + TokenEncryption.encrypt(refreshToken));
    }

    private String loadRefreshToken(String username) {
        File file = new File(getYuriDir(), "tokens.txt");
        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && parts[0].equals(username)) return TokenEncryption.decrypt(parts[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (!draggingScrollbar) return;

        int maxScrollLocal = maxScroll();
        if (maxScrollLocal <= 0) return;

        int deltaY = mouseY - dragStartY;
        int thumbHeight = Math.max(scrollbarHeight * fullVisibleRows() / Math.max(1, totalRows()), 20);
        int scrollRange = scrollbarHeight - thumbHeight;
        int scrollDelta = scrollRange > 0 ? deltaY * maxScrollLocal / scrollRange : 0;
        scrollOffset = Math.min(maxScrollLocal, Math.max(0, scrollStart + scrollDelta));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        int maxScrollLocal = maxScroll();

        if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScrollLocal, scrollOffset + 1);
    }

    private void handleCrackedLogin(String loginUsername) {
        if (isLoggingIn || loginUsername.isEmpty()) return;
        isLoggingIn = true;

        mc.setSession(new Session(loginUsername, loginUsername, "0", "legacy"));
        saveCrackedToFile(loginUsername);

        setStatus("Logged in with " + loginUsername + "!", false);
        clearTextBoxes();
        isLoggingIn = false;
    }

    private void handleOAuthLogin() {
        if (isLoggingIn) return;
        isLoggingIn = true;
        setStatus("Awaiting response for Microsoft login...", false);

        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            try {
                if (refreshToken != null) {
                    MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        saveOAuthAltToFile(login.username, login.newRefreshToken);
                        setStatus("Logged in with " + login.username + "!", false);
                    } else {
                        setStatus("Failed to login with Microsoft OAuth!", true);
                    }
                } else {
                    setStatus("Failed to get refresh token!", true);
                }
            } finally {
                isLoggingIn = false;
            }
        });
    }

    private void saveCrackedToFile(String sessionUsername) {
        String entry = "cracked|" + sessionUsername;
        appendLine("alts.txt", entry);
        alts.add(entry);
    }

    public static String generateRandomString() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < 4; i++) result.append(LETTERS.charAt(RANDOM_SOURCE.nextInt(LETTERS.length())));
        for (int i = 0; i < 4; i++) result.append(NUMBERS.charAt(RANDOM_SOURCE.nextInt(NUMBERS.length())));
        return result.toString();
    }

    private void clearTextBoxes() {
        username.setText("");
        tokenField.setText("");
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        username.keyTyped(typedChar, keyCode);
        tokenField.keyTyped(typedChar, keyCode);

        if (tokenField.isFocused() && keyCode == Keyboard.KEY_RETURN) {
            handleTokenLogin();
            return;
        }

        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_A) {
            selectedAlts.clear();
            for (int i = 0; i < alts.size(); i++) selectedAlts.add(i);
            return;
        }

        if (GuiScreen.isAltKeyDown() && keyCode == Keyboard.KEY_BACK) {
            deleteSelected();
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }
}