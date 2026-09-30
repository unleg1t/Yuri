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
    private static final Color DANGER = new Color(232, 90, 90);
    private static final Color SUCCESS = new Color(90, 210, 130);
    private static final Color MUTED = new Color(138, 138, 147);
    private static final ResourceLocation PLACEHOLDER_HEAD = new ResourceLocation("yuri/gui/steve.png");
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM_SOURCE = new SecureRandom();

    private static final float RADIUS = 4f;
    private static final int MAX_PANEL_WIDTH = 360;
    private static final int PANEL_PADDING = 14;
    private static final Color PANEL_COLOR = new Color(0, 0, 0, 130);
    private static final int FIELD_HEIGHT = 22;
    private static final int GAP = 6;
    private static final int SMALL_BUTTON_WIDTH = 56;
    private static final int ROW_HEIGHT = 26;
    private static final int ROW_STRIDE = 28;
    private static final int HEAD_SIZE = 18;

    private final ArrayList<Integer> selectedAlts = new ArrayList<>();
    private final ArrayList<String> alts = new ArrayList<>();
    private final Map<String, ResourceLocation> headCache = new HashMap<>();
    private final Map<String, Boolean> headLoading = new HashMap<>();
    private final Map<String, Integer> headTries = new HashMap<>();

    private CustomTextBox username, tokenField;
    private int scrollOffset = 0;
    private float scrollAnim = 0f;
    private String statusString = "";
    private boolean statusIsError = false;
    private boolean isLoggingIn = false;

    private int panelX, panelY, panelW, panelH;
    private int colX, colW;
    private int titleY, statusY, tipsY;
    private int userRowY, tokenRowY, msRowY;
    private int loginBtnX, randomBtnX, tokenBtnX;
    private int listY, listH;
    private int deleteX, deleteW;

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

    private void computeLayout() {
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);

        panelW = Math.min(MAX_PANEL_WIDTH, width - 24);
        panelX = (width - panelW) / 2;
        panelY = 34;
        panelH = Math.max(120, height - panelY - 16);

        colW = panelW - PANEL_PADDING * 2;
        colX = panelX + PANEL_PADDING;

        titleY = panelY + PANEL_PADDING;
        statusY = titleY + regular.getHeight() + 4;

        userRowY = statusY + small.getHeight() + 12;
        tokenRowY = userRowY + FIELD_HEIGHT + GAP;
        msRowY = tokenRowY + FIELD_HEIGHT + GAP;

        int userFieldW = colW - SMALL_BUTTON_WIDTH * 2 - GAP * 2;
        username.xPosition = colX;
        username.yPosition = userRowY;
        username.setWidth(userFieldW);
        loginBtnX = colX + userFieldW + GAP;
        randomBtnX = loginBtnX + SMALL_BUTTON_WIDTH + GAP;

        int tokenFieldW = colW - SMALL_BUTTON_WIDTH - GAP;
        tokenField.xPosition = colX;
        tokenField.yPosition = tokenRowY;
        tokenField.setWidth(tokenFieldW);
        tokenBtnX = colX + tokenFieldW + GAP;

        tipsY = panelY + panelH - PANEL_PADDING - small.getHeight();
        listY = msRowY + FIELD_HEIGHT + 16;
        listH = Math.max(ROW_STRIDE, tipsY - 10 - listY);

        deleteW = small.getStringWidth("Delete " + selectedAlts.size()) + 4;
        deleteX = colX + colW - deleteW;
    }

    private int visibleRows() {
        return Math.max(1, listH / ROW_STRIDE);
    }

    private int maxScroll() {
        return Math.max(0, alts.size() - visibleRows());
    }

    private void updateScroll() {
        scrollOffset = Math.min(maxScroll(), Math.max(0, scrollOffset));
        float diff = scrollOffset - scrollAnim;
        if (Math.abs(diff) < 0.005f) scrollAnim = scrollOffset;
        else scrollAnim += diff * 0.25f;
        scrollAnim = Math.min(maxScroll(), Math.max(0f, scrollAnim));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, width, height, BACKGROUND.getRGB());
        MenuShaderBackground.get().render(width, height);

        computeLayout();
        updateScroll();

        Color accent = ColorManager.getColor();
        CustomFontRenderer bold = FontUtils.getFont("sf-bold", 18);
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);

        RoundedUtils.drawRoundOutline(panelX, panelY, panelW, panelH, RADIUS + 2f, -0.4f, PANEL_COLOR, accent);

        boolean backHovered = isMouseOverButton(mouseX, mouseY, 10, 10, regular.getStringWidth("Back") + 10, regular.getHeight() + 6);
        regular.drawString("Back", 15, 13, backHovered ? accent.getRGB() : MUTED.getRGB());

        bold.drawString("Accounts", colX, titleY, accent.getRGB());
        String signed = Minecraft.getMinecraft().getSession().getUsername();
        small.drawString(signed, colX + colW - small.getStringWidth(signed), titleY + (regular.getHeight() - small.getHeight()) / 2f, MUTED.getRGB());

        if (!selectedAlts.isEmpty()) {
            boolean deleteHovered = isMouseOverButton(mouseX, mouseY, deleteX, statusY - 2, deleteW, small.getHeight() + 4);
            small.drawString("Delete " + selectedAlts.size(), deleteX, statusY, deleteHovered ? Color.WHITE.getRGB() : DANGER.getRGB());
        }
        if (statusString != null && !statusString.isEmpty()) {
            int statusMax = colW - (selectedAlts.isEmpty() ? 0 : deleteW + GAP);
            small.drawString(fit(small, statusString, statusMax), colX, statusY, statusIsError ? DANGER.getRGB() : MUTED.getRGB());
        }

        username.drawTextBox();
        drawButton(loginBtnX, userRowY, SMALL_BUTTON_WIDTH, "Login", mouseX, mouseY, !isLoggingIn, true);
        drawButton(randomBtnX, userRowY, SMALL_BUTTON_WIDTH, "Random", mouseX, mouseY, true, false);

        tokenField.drawTextBox();
        drawButton(tokenBtnX, tokenRowY, SMALL_BUTTON_WIDTH, "Token", mouseX, mouseY, !isLoggingIn, false);

        drawButton(colX, msRowY, colW, "Microsoft", mouseX, mouseY, !isLoggingIn, false);

        drawList(mouseX, mouseY);

        String tips = "Alt+Click select  \u00b7  Alt+A all  \u00b7  Alt+Backspace delete";
        small.drawString(tips, width / 2f - small.getStringWidth(tips) / 2f, tipsY, RenderUtils.withAlpha(MUTED, 160));

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String fit(CustomFontRenderer font, String text, int maxWidth) {
        if (font.getStringWidth(text) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && font.getStringWidth(cut + "..") > maxWidth) cut = cut.substring(0, cut.length() - 1);
        return cut + "..";
    }

    private void drawButton(int x, int y, int w, String label, int mouseX, int mouseY, boolean enabled, boolean primary) {
        boolean hovered = enabled && isMouseOverButton(mouseX, mouseY, x, y, w, FIELD_HEIGHT);
        Color accent = ColorManager.getColor();

        RoundedUtils.drawRoundOutline(x, y, w, FIELD_HEIGHT, RADIUS, -0.4f,
                RenderUtils.withAlphaColor(primary ? accent : Color.WHITE, hovered ? 40 : 14),
                RenderUtils.withAlphaColor(primary ? accent : Color.WHITE, hovered ? 140 : 35));

        CustomFontRenderer font = FontUtils.getFont("sf", 18);
        String text = fit(font, label, w - 8);
        int color = !enabled ? MUTED.getRGB() : (primary || hovered ? accent.getRGB() : Color.WHITE.getRGB());
        font.drawString(text, x + (w - font.getStringWidth(text)) / 2f, y + (FIELD_HEIGHT - font.getHeight()) / 2f, color);
    }

    private static String typeLabel(String type) {
        switch (type) {
            case "microsoftOAuth": return "Microsoft";
            case "token": return "Token";
            default: return "Cracked";
        }
    }

    private void drawList(int mouseX, int mouseY) {
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        Color accent = ColorManager.getColor();

        Gui.drawRect(colX, listY - 8, colX + colW, listY - 7, RenderUtils.withAlpha(Color.WHITE, 20));

        if (alts.isEmpty()) {
            regular.drawCenteredStringWithShadow("No accounts", width / 2f, listY + 10, MUTED.getRGB());
            return;
        }

        enableScissor(colX, listY, colW, listH);

        int firstRow = (int) Math.floor(scrollAnim);
        int shift = (int) ((scrollAnim - firstRow) * ROW_STRIDE);
        String current = Minecraft.getMinecraft().getSession().getUsername();

        for (int row = 0; row < visibleRows() + 2; row++) {
            int index = firstRow + row;
            if (index >= alts.size()) break;

            int y = listY + row * ROW_STRIDE - shift;

            String[] parts = alts.get(index).split("\\|", 4);
            String type = parts[0];
            boolean premium = type.equals("microsoftOAuth") || type.equals("token");
            String altName = parts.length > 1 ? parts[1] : "Unknown";
            String uuid = (type.equals("token") && parts.length > 2) ? parts[2] : (premium ? altName : "");

            boolean selected = selectedAlts.contains(index);
            boolean hovered = mouseX >= colX && mouseX <= colX + colW && mouseY >= Math.max(y, listY) && mouseY <= Math.min(y + ROW_HEIGHT, listY + listH);
            boolean active = altName.equals(current);

            if (selected || hovered) {
                RoundedUtils.drawRoundOutline(colX, y, colW, ROW_HEIGHT, RADIUS, -0.4f,
                        RenderUtils.withAlphaColor(selected ? accent : Color.WHITE, selected ? 30 : 12),
                        RenderUtils.withAlphaColor(selected ? accent : Color.WHITE, selected ? 120 : 0));
            }

            loadHead(uuid);
            drawHead(colX + 5, y + (ROW_HEIGHT - HEAD_SIZE) / 2, uuid, HEAD_SIZE);

            String label = typeLabel(type);
            int labelWidth = small.getStringWidth(label);
            int labelX = colX + colW - 8 - labelWidth;
            small.drawString(label, labelX, y + (ROW_HEIGHT - small.getHeight()) / 2f, MUTED.getRGB());

            int nameX = colX + 5 + HEAD_SIZE + 8;
            int nameMax = labelX - 8 - nameX;
            int nameColor = selected ? accent.getRGB() : (active ? SUCCESS.getRGB() : Color.WHITE.getRGB());
            regular.drawString(fit(regular, altName, nameMax), nameX, y + (ROW_HEIGHT - regular.getHeight()) / 2f, nameColor);
        }

        disableScissor();

        if (maxScroll() > 0) {
            int trackX = colX + colW + 5;
            int thumbH = Math.max(16, listH * visibleRows() / alts.size());
            int thumbY = listY + (int) ((listH - thumbH) * scrollAnim / maxScroll());
            Gui.drawRect(trackX, thumbY, trackX + 2, thumbY + thumbH, RenderUtils.withAlpha(accent, 150));
        }
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

    public void drawHead(int x, int y, String uuid, int size) {
        ResourceLocation head = uuid == null || uuid.isEmpty() ? PLACEHOLDER_HEAD : headCache.getOrDefault(uuid, PLACEHOLDER_HEAD);
        RoundedUtils.drawRoundedImage(head, x, y, size, size, 3f);
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

        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);

        if (isMouseOverButton(mouseX, mouseY, 10, 10, regular.getStringWidth("Back") + 10, regular.getHeight() + 6)) {
            mc.displayGuiScreen(new YuriMenu());
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, loginBtnX, userRowY, SMALL_BUTTON_WIDTH, FIELD_HEIGHT)) {
            handleCrackedLogin(username.getText());
            return;
        }

        if (isMouseOverButton(mouseX, mouseY, randomBtnX, userRowY, SMALL_BUTTON_WIDTH, FIELD_HEIGHT)) {
            handleCrackedLogin(generateRandomString());
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, tokenBtnX, tokenRowY, SMALL_BUTTON_WIDTH, FIELD_HEIGHT)) {
            handleTokenLogin();
            return;
        }

        if (!isLoggingIn && isMouseOverButton(mouseX, mouseY, colX, msRowY, colW, FIELD_HEIGHT)) {
            handleOAuthLogin();
            return;
        }

        if (!selectedAlts.isEmpty() && isMouseOverButton(mouseX, mouseY, deleteX, statusY - 2, deleteW, small.getHeight() + 4)) {
            deleteSelected();
            return;
        }

        boolean inList = mouseX >= colX && mouseX < colX + colW && mouseY >= listY && mouseY < listY + listH;
        if (inList) {
            int relY = mouseY - listY + (int) (scrollAnim * ROW_STRIDE);
            int index = relY / ROW_STRIDE;
            boolean inRow = relY % ROW_STRIDE < ROW_HEIGHT;

            if (inRow && index >= 0 && index < alts.size()) {
                if (GuiScreen.isAltKeyDown()) {
                    if (selectedAlts.contains(index)) selectedAlts.remove((Integer) index);
                    else selectedAlts.add(index);
                } else if (!isLoggingIn) {
                    loginWithAlt(alts.get(index));
                }
            }
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
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;

        if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScroll(), scrollOffset + 1);
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