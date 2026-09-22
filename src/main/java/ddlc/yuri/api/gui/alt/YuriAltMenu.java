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
import net.minecraft.util.ChatComponentText;
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
    static final Color BODY_COLOR = new Color(0, 0, 0, 130);
    static final Color DANGER = new Color(232, 90, 90);
    private static final ResourceLocation PLACEHOLDER_HEAD = new ResourceLocation("yuri/gui/steve.png");
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM_SOURCE = new SecureRandom();

    static final float RADIUS = 6f;
    private static final int HEADER_HEIGHT = 44;
    private static final int TAB_HEIGHT = 30;
    private static final int TAB_BUTTON_HEIGHT = 22;
    static final int PADDING = 12;
    static final int FIELD_HEIGHT = 24;
    static final int BUTTON_HEIGHT = 27;
    static final int BUTTON_SPACING = 8;
    private static final int SHADOW_OFFSET = 3;
    static final int SCROLLBAR_WIDTH = 4;
    private static final int COLUMNS = 3;
    private static final int ENTRY_PADDING = 7;
    private static final int ENTRY_HEIGHT = 50;
    static final float ADD_PANEL_RATIO = 0.34f;

    private static final String[] TAB_TITLES = {"Accounts", "Localts", "Skins"};
    private final AltTab[] tabs = {null, new LocaltsTab(this), new SkinTab(this)};
    private final int[] tabX = new int[TAB_TITLES.length];
    private final int[] tabWidth = new int[TAB_TITLES.length];
    private int activeTab = 0;

    private final ArrayList<Integer> selectedAlts = new ArrayList<>();
    private final ArrayList<String> alts = new ArrayList<>();
    private final Map<String, ResourceLocation> headCache = new HashMap<>();
    private final Map<String, Boolean> headLoading = new HashMap<>();
    private final Map<String, Integer> headTries = new HashMap<>();

    private CustomTextBox username, tokenField;
    private int scrollOffset = 0;
    private boolean draggingScrollbar = false;
    private int dragStartY;
    private int scrollStart;
    private String statusString = "Ready To Work!";
    private boolean statusIsError = false;
    private boolean isLoggingIn = false;

    private int contentY, contentHeight;
    private int addX, addY, addWidth, addHeight;
    private int accountsX, accountsY, accountsWidth, accountsHeight;
    private int accountsDividerY;

    private int primaryButtonX, primaryButtonY, primaryButtonWidth;
    private int oauthButtonX, oauthButtonY, oauthButtonWidth;
    private int generateButtonX, generateButtonY, generateButtonWidth;
    private int tokenButtonX, tokenButtonY, tokenButtonWidth;
    private int statusY, tipsY;
    private int backButtonWidth;

    private int gridListX, gridListY, gridListWidth, gridListHeight;
    private int gridCellWidth, gridRowStride, gridVisibleRows;
    private int scrollbarX, scrollbarY, scrollbarHeight;

    @Override
    public void initGui() {
        alts.clear();
        loadAltsFromFile();

        selectedAlts.clear();
        buttonList.clear();

        username = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        username.setPlaceholder("Username");

        tokenField = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        tokenField.setPlaceholder("Access / Refresh Token");

        super.initGui();
    }

    File getYuriDir() {
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

        drawHeader(mouseX, mouseY);
        drawTabBar(mouseX, mouseY);

        AltTab tab = tabs[activeTab];
        if (tab == null) {
            drawAddAccountPanel(mouseX, mouseY);
            drawAccountsPanel(mouseX, mouseY);
        } else {
            tab.layout(PADDING, contentY, width - PADDING * 2, contentHeight);
            tab.draw(mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void computeLayout() {
        contentY = HEADER_HEIGHT + TAB_HEIGHT + PADDING;
        contentHeight = height - contentY - PADDING;

        CustomFontRenderer tabFont = FontUtils.getFont("sf", 18);
        int totalTabsWidth = 0;
        for (int i = 0; i < TAB_TITLES.length; i++) {
            tabWidth[i] = tabFont.getStringWidth(TAB_TITLES[i]) + 28;
            totalTabsWidth += tabWidth[i] + (i > 0 ? BUTTON_SPACING : 0);
        }
        int cursor = width / 2 - totalTabsWidth / 2;
        for (int i = 0; i < TAB_TITLES.length; i++) {
            tabX[i] = cursor;
            cursor += tabWidth[i] + BUTTON_SPACING;
        }

        addWidth = (int) (width * ADD_PANEL_RATIO);
        addX = PADDING;
        addY = contentY;
        addHeight = contentHeight;

        accountsX = addX + addWidth + PADDING;
        accountsY = contentY;
        accountsWidth = width - accountsX - PADDING;
        accountsHeight = contentHeight;

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        int fieldsStartY = addY + PADDING + fontHeight + PADDING + 6;

        username.xPosition = addX + PADDING;
        username.yPosition = fieldsStartY;
        username.setWidth(addWidth - PADDING * 2);

        primaryButtonX = addX + PADDING;
        primaryButtonY = fieldsStartY + FIELD_HEIGHT + PADDING;
        primaryButtonWidth = addWidth - PADDING * 2;

        int secondaryWidth = (primaryButtonWidth - BUTTON_SPACING) / 2;
        oauthButtonX = primaryButtonX;
        oauthButtonY = primaryButtonY + BUTTON_HEIGHT + BUTTON_SPACING;
        oauthButtonWidth = secondaryWidth;

        generateButtonX = oauthButtonX + secondaryWidth + BUTTON_SPACING;
        generateButtonY = oauthButtonY;
        generateButtonWidth = secondaryWidth;

        int tokenFieldY = oauthButtonY + BUTTON_HEIGHT + PADDING;
        tokenField.xPosition = addX + PADDING;
        tokenField.yPosition = tokenFieldY;
        tokenField.setWidth(addWidth - PADDING * 2);

        tokenButtonX = addX + PADDING;
        tokenButtonY = tokenFieldY + FIELD_HEIGHT + BUTTON_SPACING;
        tokenButtonWidth = addWidth - PADDING * 2;

        statusY = tokenButtonY + BUTTON_HEIGHT + PADDING + 10;
        tipsY = statusY + fontHeight + PADDING * 2;

        backButtonWidth = FontUtils.getFont("sf", 18).getStringWidth("Back") + 8;

        accountsDividerY = accountsY + PADDING + fontHeight + 10;

        gridListX = accountsX + PADDING;
        gridListY = accountsDividerY + PADDING;
        gridListWidth = accountsWidth - PADDING * 2 - SCROLLBAR_WIDTH - 8;
        gridListHeight = accountsY + accountsHeight - gridListY - PADDING;

        gridCellWidth = gridListWidth / COLUMNS;
        gridRowStride = ENTRY_HEIGHT + ENTRY_PADDING;
        gridVisibleRows = Math.max(1, gridListHeight / gridRowStride);

        scrollbarX = accountsX + accountsWidth - PADDING - SCROLLBAR_WIDTH;
        scrollbarY = gridListY;
        scrollbarHeight = gridListHeight;
    }

    void drawPanelShadow(int x, int y, int w, int h) {
        RoundedUtils.drawRoundOutline(x + SHADOW_OFFSET, y + SHADOW_OFFSET, w, h, RADIUS, -0.5f,
                RenderUtils.withAlphaColor(Color.BLACK, 90), RenderUtils.withAlphaColor(Color.BLACK, 0));
    }

    void drawSectionHeader(int x, int y, String bold, String rest) {
        Color accent = ColorManager.getColor();

        CustomFontRenderer boldFont = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regularFont = FontUtils.getFont("sf", 18);
        int textX = x + 2;
        boldFont.drawStringWithShadow(bold, textX, y, accent.getRGB());
        regularFont.drawStringWithShadow(rest, textX + boldFont.getStringWidth(bold), y, Color.WHITE.getRGB());
    }

    private void drawHeader(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();

        Gui.drawRect(0, HEADER_HEIGHT, width, HEADER_HEIGHT + 1, RenderUtils.withAlpha(Color.WHITE, 20));
        int underlineWidth = 60;
        Gui.drawRect(width / 2 - underlineWidth / 2, HEADER_HEIGHT, width / 2 + underlineWidth / 2, HEADER_HEIGHT + 2, accent.getRGB());

        boolean backHovered = isMouseOverButton(mouseX, mouseY, PADDING - 6, 6, backButtonWidth + 12, HEADER_HEIGHT - 12);
        if (backHovered) {
            RoundedUtils.drawRoundOutline(PADDING - 6, 6, backButtonWidth + 12, HEADER_HEIGHT - 12, RADIUS, -0.5f,
                    RenderUtils.withAlphaColor(Color.WHITE, 20), RenderUtils.withAlphaColor(Color.WHITE, 0));
        }
        regular.drawStringWithShadow("Back", PADDING, (HEADER_HEIGHT - fontHeight) / 2, backHovered ? accent.getRGB() : Color.WHITE.getRGB());

        String titleBold = "Y";
        String titleRest = new ChatComponentText("uri Account Manager").getFormattedText();
        float titleBoldWidth = bold.getStringWidth(titleBold);
        float titleRestWidth = regular.getStringWidth(titleRest);
        float titleTotalWidth = titleBoldWidth + titleRestWidth;
        float titleX = width / 2f - titleTotalWidth / 2f;
        float titleY = (HEADER_HEIGHT - fontHeight) / 2f;

        bold.drawStringWithShadow(titleBold, titleX, titleY, accent.getRGB());
        regular.drawStringWithShadow(titleRest, titleX + titleBoldWidth, titleY, Color.WHITE.getRGB());

        String currentUser = Minecraft.getMinecraft().getSession().getUsername();
        String pillLabel = "Signed In As " + currentUser;
        int pillTextWidth = regular.getStringWidth(pillLabel);
        int dotSize = 6;
        int pillPaddingX = 10;
        int pillHeight = 24;
        int pillWidth = dotSize + 6 + pillTextWidth + pillPaddingX * 2;
        int pillX = width - PADDING - pillWidth;
        int pillY = (HEADER_HEIGHT - pillHeight) / 2;

        RoundedUtils.drawRoundOutline(pillX, pillY, pillWidth, pillHeight, RADIUS, -0.5f, BODY_COLOR, RenderUtils.withAlphaColor(accent, 160));
        int dotX = pillX + pillPaddingX;
        int dotY = pillY + (pillHeight - dotSize) / 2;
        RoundedUtils.drawRoundOutline(dotX, dotY, dotSize, dotSize, 2.0f, -0.5f, accent, RenderUtils.withAlphaColor(Color.BLACK, 180));
        regular.drawString(pillLabel, dotX + dotSize + 6, pillY + (pillHeight - fontHeight) / 2f, Color.WHITE.getRGB());
    }

    private void drawAddAccountPanel(int mouseX, int mouseY) {
        drawPanelShadow(addX, addY, addWidth, addHeight);
        RoundedUtils.drawRoundOutline(addX, addY, addWidth, addHeight, RADIUS, -0.5f, BODY_COLOR, ColorManager.getColor());

        drawSectionHeader(addX + PADDING, addY + PADDING, "Add", new ChatComponentText(" Account").getFormattedText());

        username.drawTextBox();

        drawButton(primaryButtonX, primaryButtonY, primaryButtonWidth, "Login", mouseX, mouseY, true);
        drawButton(oauthButtonX, oauthButtonY, oauthButtonWidth, "Microsoft", mouseX, mouseY, false);
        drawButton(generateButtonX, generateButtonY, generateButtonWidth, "Generate Random", mouseX, mouseY, false);

        tokenField.drawTextBox();
        drawButton(tokenButtonX, tokenButtonY, tokenButtonWidth, "Login Via Token", mouseX, mouseY, false);

        drawStatusPill();

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        Gui.drawRect(addX + PADDING, tipsY - 10, addX + addWidth - PADDING, tipsY - 9, RenderUtils.withAlpha(Color.WHITE, 20));
        String tips = "Alt+Click Select  \u00b7  Alt+A All  \u00b7  Alt+Backspace Delete";
        float tipsX = addX + (addWidth - regular.getStringWidth(tips)) / 2f;
        regular.drawString(tips, tipsX, tipsY, 0x777777);
    }

    private void drawStatusPill() {
        drawStatusPill(addX, addWidth, statusY, statusString, statusIsError);
    }

    void drawStatusPill(int panelX, int panelWidth, int centerY, String message, boolean isError) {
        if (message == null || message.isEmpty()) return;

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        Color tint = isError ? DANGER : ColorManager.getColor();
        int maxWidth = panelWidth - PADDING * 2 - 20;
        String text = message;
        while (regular.getStringWidth(text) > maxWidth && text.length() > 4) text = text.substring(0, text.length() - 4) + "...";
        int pillWidth = regular.getStringWidth(text) + 20;
        int pillHeight = 22;
        int pillX = panelX + (panelWidth - pillWidth) / 2;
        int pillY = centerY - pillHeight / 2;

        RoundedUtils.drawRoundOutline(pillX, pillY, pillWidth, pillHeight, RADIUS, -0.5f,
                RenderUtils.withAlphaColor(tint, 25), RenderUtils.withAlphaColor(tint, 130));
        regular.drawCenteredStringWithShadow(text, panelX + panelWidth / 2f, pillY + (pillHeight - regular.getHeight()) / 2f, tint.getRGB());
    }

    private int tabBarY() {
        return HEADER_HEIGHT + (TAB_HEIGHT - TAB_BUTTON_HEIGHT) / 2 + 2;
    }

    private void drawTabBar(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        int tabY = tabBarY();
        for (int i = 0; i < TAB_TITLES.length; i++) {
            boolean active = i == activeTab;
            boolean hovered = isMouseOverButton(mouseX, mouseY, tabX[i], tabY, tabWidth[i], TAB_BUTTON_HEIGHT);
            Color fill = active ? RenderUtils.withAlphaColor(accent, 60) : RenderUtils.withAlphaColor(accent, hovered ? 30 : 10);
            Color outline = active || hovered ? accent : RenderUtils.withAlphaColor(accent, 90);
            RoundedUtils.drawRoundOutline(tabX[i], tabY, tabWidth[i], TAB_BUTTON_HEIGHT, RADIUS, active ? 0.5f : -0.5f, fill, outline);
            font.drawStringWithShadow(TAB_TITLES[i], tabX[i] + (tabWidth[i] - font.getStringWidth(TAB_TITLES[i])) / 2f,
                    tabY + (TAB_BUTTON_HEIGHT - font.getHeight()) / 2f, active ? accent.getRGB() : Color.WHITE.getRGB());
        }
    }

    private void switchTab(int index) {
        if (index == activeTab) return;
        activeTab = index;
        username.setFocused(false);
        tokenField.setFocused(false);
        if (tabs[index] != null) tabs[index].onShow();
    }

    void drawButton(int x, int y, int w, String label, int mouseX, int mouseY, boolean primary) {
        boolean hovered = isMouseOverButton(mouseX, mouseY, x, y, w, BUTTON_HEIGHT);
        Color accent = ColorManager.getColor();

        Color fill = primary
                ? RenderUtils.withAlphaColor(accent, hovered ? 220 : 190)
                : RenderUtils.withAlphaColor(accent, hovered ? 45 : 18);
        Color outline = primary ? accent : (hovered ? accent : RenderUtils.withAlphaColor(accent, 130));

        RoundedUtils.drawRoundOutline(x, y, w, BUTTON_HEIGHT, RADIUS, -0.5f, fill, outline);

        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        int textColor = primary ? Color.BLACK.getRGB() : (hovered ? accent.getRGB() : Color.WHITE.getRGB());
        int textX = x + (w - font.getStringWidth(label)) / 2;
        int textY = y + (BUTTON_HEIGHT - font.getHeight()) / 2;
        if (primary) font.drawString(label, textX, textY, textColor);
        else font.drawStringWithShadow(label, textX, textY, textColor);
    }

    private void drawAccountsPanel(int mouseX, int mouseY) {
        drawPanelShadow(accountsX, accountsY, accountsWidth, accountsHeight);
        RoundedUtils.drawRoundOutline(accountsX, accountsY, accountsWidth, accountsHeight, RADIUS, -0.5f, BODY_COLOR, ColorManager.getColor());

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        int fontHeight = regular.getHeight();

        drawSectionHeader(accountsX + PADDING, accountsY + PADDING, "Accounts", new ChatComponentText(" (" + alts.size() + ")").getFormattedText());

        if (!selectedAlts.isEmpty()) {
            String selectedLabel = selectedAlts.size() + " Selected";
            CustomFontRenderer small = FontUtils.getFont("sf", 18);
            int labelWidth = small.getStringWidth(selectedLabel) + 16;
            int labelX = accountsX + accountsWidth - PADDING - labelWidth;
            RoundedUtils.drawRoundOutline(labelX, accountsY + PADDING - 2, labelWidth, 20, RADIUS, -0.5f,
                    RenderUtils.withAlphaColor(ColorManager.getColor(), 30), RenderUtils.withAlphaColor(ColorManager.getColor(), 150));
            small.drawCenteredStringWithShadow(selectedLabel, labelX + labelWidth / 2f, accountsY + PADDING, ColorManager.getColor().getRGB());
        }

        Gui.drawRect(accountsX + PADDING, accountsDividerY, accountsX + accountsWidth - PADDING, accountsDividerY + 1, RenderUtils.withAlpha(Color.WHITE, 20));

        if (alts.isEmpty()) {
            float centerX = gridListX + gridListWidth / 2f;
            float centerY = gridListY + gridListHeight / 2f - fontHeight;
            regular.drawCenteredStringWithShadow("No Accounts Yet", centerX, centerY, Color.WHITE.getRGB());
            regular.drawCenteredStringWithShadow("Add One Using The Form On The Left", centerX, centerY + fontHeight + 4, 0x999999);
            return;
        }

        enableScissor(gridListX, gridListY, gridListWidth, gridListHeight);

        int startIndex = scrollOffset * COLUMNS;
        for (int row = 0; row < gridVisibleRows; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int altIndex = startIndex + row * COLUMNS + col;
                if (altIndex >= alts.size()) break;

                int x = gridListX + col * gridCellWidth;
                int y = gridListY + row * gridRowStride;

                String[] parts = alts.get(altIndex).split("\\|", 4);
                String type = parts[0];
                boolean premium = type.equals("microsoftOAuth") || type.equals("token");
                String altName = parts.length > 1 ? parts[1] : "Unknown";
                String uuid = (type.equals("token") && parts.length > 2) ? parts[2] : (premium ? altName : "");

                drawAccountCell(x, y, gridCellWidth - ENTRY_PADDING, ENTRY_HEIGHT, altName, uuid, type, altIndex, mouseX, mouseY);
            }
        }

        disableScissor();

        enableScissor(accountsX, accountsY, accountsWidth, accountsHeight);

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScroll = Math.max(0, totalRows - gridVisibleRows);
        int thumbHeight = Math.max(scrollbarHeight * gridVisibleRows / Math.max(1, totalRows), 20);
        int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / Math.max(1, maxScroll);
        boolean scrollbarHovered = mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + SCROLLBAR_WIDTH + 2 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;

        RoundedUtils.drawRoundOutline(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight, SCROLLBAR_WIDTH / 2f, -0.5f,
                RenderUtils.withAlphaColor(BODY_COLOR, 200), RenderUtils.withAlphaColor(Color.BLACK, 0));
        RoundedUtils.drawRoundOutline(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH / 2f, -0.5f,
                (draggingScrollbar || scrollbarHovered) ? ColorManager.getColor() : RenderUtils.withAlphaColor(ColorManager.getColor(), 170),
                RenderUtils.withAlphaColor(Color.BLACK, 0));

        disableScissor();
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

        Color fill = selected ? RenderUtils.withAlphaColor(accent, 35) : BODY_COLOR;
        Color outline = selected ? accent : (hovered ? RenderUtils.withAlphaColor(accent, 150) : RenderUtils.withAlphaColor(Color.WHITE, 25));
        RoundedUtils.drawRoundOutline(x, y, w, h, RADIUS, selected ? 0.5f : -0.5f, fill, outline);

        loadHead(uuid);
        drawHead(x, y, uuid, h);

        int avatarSize = h - ENTRY_PADDING * 2;
        boolean premium = type.equals("microsoftOAuth") || type.equals("token");
        int dotSize = 6;
        int dotX = x + ENTRY_PADDING + avatarSize - dotSize + 2;
        int dotY = y + ENTRY_PADDING + avatarSize - dotSize + 2;
        Color dotColor = premium ? new Color(90, 210, 130) : new Color(150, 150, 150);
        RoundedUtils.drawRoundOutline(dotX, dotY, dotSize, dotSize, 2.0f, -0.5f, dotColor, RenderUtils.withAlphaColor(Color.BLACK, 180));

        int textX = x + ENTRY_PADDING + avatarSize + ENTRY_PADDING;
        CustomFontRenderer nameFont = FontUtils.getFont("sf", 18);
        CustomFontRenderer typeFont = FontUtils.getFont("sf", 14);
        int blockHeight = nameFont.getHeight() + 3 + typeFont.getHeight();
        int nameY = y + (h - blockHeight) / 2;
        int typeY = nameY + nameFont.getHeight() + 3;

        nameFont.drawString(text, textX, nameY, selected ? accent.getRGB() : Color.WHITE.getRGB());
        typeFont.drawString(typeLabel(type), textX, typeY, dotColor.getRGB());
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

    void enableScissor(int x, int y, int w, int h) {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale, (sr.getScaledHeight() - y - h) * scale, w * scale, h * scale);
    }

    void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    boolean isMouseOverButton(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void setStatus(String message, boolean isError) {
        statusString = message;
        statusIsError = isError;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (isMouseOverButton(mouseX, mouseY, PADDING - 6, 6, backButtonWidth + 12, HEADER_HEIGHT - 12)) {
            mc.displayGuiScreen(new YuriMenu());
            return;
        }

        int tabY = tabBarY();
        for (int i = 0; i < TAB_TITLES.length; i++) {
            if (isMouseOverButton(mouseX, mouseY, tabX[i], tabY, tabWidth[i], TAB_BUTTON_HEIGHT)) {
                switchTab(i);
                return;
            }
        }

        AltTab tab = tabs[activeTab];
        if (tab != null) {
            tab.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        username.mouseClicked(mouseX, mouseY, mouseButton);
        tokenField.mouseClicked(mouseX, mouseY, mouseButton);

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

        int col = (mouseX - gridListX) / gridCellWidth;
        int row = (mouseY - gridListY) / gridRowStride;

        if (col >= 0 && col < COLUMNS && row >= 0 && mouseX >= gridListX && mouseY >= gridListY && mouseY < gridListY + gridListHeight) {
            int index = (scrollOffset + row) * COLUMNS + col;
            if (index >= 0 && index < alts.size()) {
                if (GuiScreen.isAltKeyDown()) {
                    if (selectedAlts.contains(index)) selectedAlts.remove((Integer) index);
                    else selectedAlts.add(index);
                } else {
                    loginWithAlt(alts.get(index));
                }
                return;
            }
        }

        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            scrollStart = scrollOffset;
        }
    }

    private void loginWithAlt(String alt) {
        String[] parts = alt.split("\\|");
        if (alt.startsWith("cracked|")) {
            SessionChanger.getInstance().setUserOffline(parts[1]);
            setStatus("Logged In With " + parts[1] + "!", false);
        } else if (alt.startsWith("microsoftOAuth|")) {
            String user = parts[1];
            String refreshToken = loadRefreshToken(user);
            if (refreshToken == null) {
                setStatus("No Stored Token For " + user + "!", true);
                return;
            }
            if (isLoggingIn) return;
            isLoggingIn = true;
            setStatus("Logging In As " + user + "...", false);

            new Thread(() -> {
                MicrosoftOAuthTranslation.LoginData login;
                try {
                    login = MicrosoftOAuthTranslation.login(refreshToken);
                } catch (Exception e) {
                    e.printStackTrace();
                    login = new MicrosoftOAuthTranslation.LoginData();
                }
                MicrosoftOAuthTranslation.LoginData result = login;
                mc.addScheduledTask(() -> {
                    if (result.isGood()) {
                        mc.setSession(new Session(result.username, result.uuid, result.mcToken, "microsoft"));
                        if (result.newRefreshToken != null && !result.newRefreshToken.equals(refreshToken)) {
                            storeRefreshToken(user, result.newRefreshToken);
                        }
                        setStatus("Logged In With " + result.username + "!", false);
                    } else {
                        setStatus("Login Failed For " + user + " - Token Expired Or Account Has No Minecraft!", true);
                    }
                    isLoggingIn = false;
                });
            }, "Alt Login Worker").start();
        } else if (alt.startsWith("token|")) {
            if (parts.length >= 4) {
                mc.setSession(new Session(parts[1], parts[2], parts[3], "mojang"));
                setStatus("Logged In With " + parts[1] + "!", false);
            }
        }
    }

    private void handleTokenLogin() {
        if (isLoggingIn) return;
        String rawToken = tokenField.getText().trim();
        if (rawToken.isEmpty()) {
            setStatus("Enter A Token First!", true);
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
        setStatus("Authenticating Refresh Token...", false);

        new Thread(() -> {
            try {
                MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                mc.addScheduledTask(() -> {
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        saveOAuthAltToFile(login.username, login.newRefreshToken != null ? login.newRefreshToken : refreshToken);
                        tokenField.setText("");
                        setStatus("Logged In Via Token As " + login.username + "!", false);
                    } else {
                        setStatus("Invalid Refresh Token!", true);
                    }
                    isLoggingIn = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    setStatus("Refresh Token Auth Failed!", true);
                    isLoggingIn = false;
                });
            }
        }, "Refresh Token Auth Worker").start();
    }

    private void handleAccessTokenLogin(String rawToken) {
        setStatus("Authenticating Token...", false);

        new Thread(() -> {
            try {
                URL profUrl = new URL("https://api.minecraftservices.com/minecraft/profile");
                HttpURLConnection profConn = (HttpURLConnection) profUrl.openConnection();
                profConn.setRequestMethod("GET");
                profConn.setRequestProperty("Authorization", "Bearer " + rawToken);

                int responseCode = profConn.getResponseCode();
                if (responseCode != 200) {
                    mc.addScheduledTask(() -> {
                        setStatus("Invalid Token! (HTTP " + responseCode + ")", true);
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
                    setStatus("Logged In Via Token As " + profileName + "!", false);
                    isLoggingIn = false;
                });
            } catch (Exception e) {
                e.printStackTrace();
                mc.addScheduledTask(() -> {
                    setStatus("Token Auth Failed!", true);
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

    void saveOAuthAltToFile(String username, String refreshToken) {
        String entry = "microsoftOAuth|" + username;
        if (!alts.contains(entry)) {
            appendLine("alts.txt", entry);
            alts.add(entry);
        }
        storeRefreshToken(username, refreshToken);
    }

    private String loadRefreshToken(String username) {
        File file = new File(getYuriDir(), "tokens.txt");
        if (!file.exists()) return null;

        String found = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2 && parts[0].equals(username)) found = TokenEncryption.decrypt(parts[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return found;
    }

    private void storeRefreshToken(String username, String refreshToken) {
        File file = new File(getYuriDir(), "tokens.txt");
        ArrayList<String> lines = new ArrayList<>();
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (!(parts.length == 2 && parts[0].equals(username))) lines.add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        lines.add(username + "|" + TokenEncryption.encrypt(refreshToken));
        try (PrintWriter out = new PrintWriter(file)) {
            for (String line : lines) out.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingScrollbar = false;
        if (tabs[activeTab] != null) tabs[activeTab].mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (tabs[activeTab] != null) {
            tabs[activeTab].mouseClickMove(mouseX, mouseY);
            return;
        }
        if (!draggingScrollbar) return;

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScrollLocal = Math.max(0, totalRows - gridVisibleRows);
        if (maxScrollLocal <= 0) return;

        int deltaY = mouseY - dragStartY;
        int thumbHeight = Math.max(scrollbarHeight * gridVisibleRows / Math.max(1, totalRows), 20);
        int scrollRange = scrollbarHeight - thumbHeight;
        int scrollDelta = scrollRange > 0 ? deltaY * maxScrollLocal / scrollRange : 0;
        scrollOffset = Math.min(maxScrollLocal, Math.max(0, scrollStart + scrollDelta));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        if (tabs[activeTab] != null) {
            tabs[activeTab].mouseScrolled(wheel);
            return;
        }

        int totalRows = (int) Math.ceil(alts.size() / (float) COLUMNS);
        int maxScrollLocal = Math.max(0, totalRows - gridVisibleRows);

        if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScrollLocal, scrollOffset + 1);
    }

    private void handleCrackedLogin(String loginUsername) {
        if (isLoggingIn || loginUsername.isEmpty()) return;
        isLoggingIn = true;

        mc.setSession(new Session(loginUsername, loginUsername, "0", "legacy"));
        saveCrackedToFile(loginUsername);

        setStatus("Logged In With " + loginUsername + "!", false);
        clearTextBoxes();
        isLoggingIn = false;
    }

    private void handleOAuthLogin() {
        if (isLoggingIn) return;
        isLoggingIn = true;
        setStatus("Awaiting Response For Microsoft Login...", false);

        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> {
            try {
                if (refreshToken != null) {
                    MicrosoftOAuthTranslation.LoginData login = MicrosoftOAuthTranslation.login(refreshToken);
                    if (login.isGood()) {
                        mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                        saveOAuthAltToFile(login.username, login.newRefreshToken);
                        setStatus("Logged In With " + login.username + "!", false);
                    } else {
                        setStatus("Failed To Login With Microsoft OAuth!", true);
                    }
                } else {
                    setStatus("Failed To Get Refresh Token!", true);
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
        if (tabs[activeTab] != null) {
            tabs[activeTab].keyTyped(typedChar, keyCode);
            if (keyCode == Keyboard.KEY_ESCAPE) super.keyTyped(typedChar, keyCode);
            return;
        }

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
            if (!selectedAlts.isEmpty()) {
                selectedAlts.sort((a, b) -> b - a);
                for (int index : selectedAlts) {
                    if (index >= 0 && index < alts.size()) alts.remove(index);
                }
                selectedAlts.clear();
                saveAltsToFile();
            }
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }
}