package ddlc.yuri.api.gui.alt;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.alt.comp.CustomTextBox;
import ddlc.yuri.api.gui.alt.comp.LocaltsClient;
import ddlc.yuri.api.gui.alt.comp.LocaltsClient.Order;
import ddlc.yuri.api.gui.alt.comp.LocaltsClient.OrderItem;
import ddlc.yuri.api.gui.alt.comp.LocaltsClient.Product;
import ddlc.yuri.api.gui.alt.comp.LocaltsClient.User;
import ddlc.yuri.api.gui.alt.comp.MicrosoftOAuthTranslation;
import ddlc.yuri.api.gui.alt.comp.TokenEncryption;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static ddlc.yuri.api.gui.alt.YuriAltMenu.*;

final class LocaltsTab extends AltTab {

    private static final String KEY_FILE = "localts.txt";
    private static final String DELIVERY_FILE = "localts_orders.txt";
    private static final int ROW_HEIGHT = 38;
    private static final int ROW_PADDING = 6;
    private static final int MAX_AMOUNT = 25;

    private final CustomTextBox apiKeyField;
    private final List<Product> products = new ArrayList<>();

    private String apiKey = "";
    private User user;
    private int selectedProduct = -1;
    private int amount = 1;
    private int scrollOffset;
    private boolean draggingScrollbar;
    private int dragStartY, scrollStart;

    private String status = "Enter Your Localts API Key";
    private boolean statusIsError;
    private volatile boolean busy;

    private int panelX, panelY, panelWidth, panelHeight;
    private int listPanelX, listPanelY, listPanelWidth, listPanelHeight;
    private int connectX, connectY, connectWidth;
    private int minusX, plusX, amountY, amountBoxX, amountBoxWidth;
    private int buyX, buyY, buyWidth;
    private int infoY, statusY, tipsY;
    private int listX, listY, listWidth, listHeight, visibleRows;
    private int scrollbarX, scrollbarY, scrollbarHeight;

    LocaltsTab(YuriAltMenu menu) {
        super(menu);
        apiKeyField = new CustomTextBox(0, 0, 0, FIELD_HEIGHT);
        apiKeyField.setPlaceholder("Localts API Key (lc.…)");
        apiKeyField.setMasked(true);
        apiKey = loadApiKey();
        apiKeyField.setText(apiKey);
    }

    @Override
    String title() {
        return "Localts";
    }

    @Override
    void onShow() {
        if (user == null && !apiKey.isEmpty() && !busy) connect();
    }

    @Override
    void layout(int x, int y, int w, int h) {
        super.layout(x, y, w, h);

        panelX = x;
        panelY = y;
        panelWidth = (int) (menu.width * ADD_PANEL_RATIO);
        panelHeight = h;

        listPanelX = panelX + panelWidth + PADDING;
        listPanelY = y;
        listPanelWidth = x + w - listPanelX;
        listPanelHeight = h;

        int fontHeight = FontUtils.getFont("sf", 18).getHeight();
        int fieldsStartY = panelY + PADDING + fontHeight + PADDING + 6;

        apiKeyField.xPosition = panelX + PADDING;
        apiKeyField.yPosition = fieldsStartY;
        apiKeyField.setWidth(panelWidth - PADDING * 2);

        connectX = panelX + PADDING;
        connectY = fieldsStartY + FIELD_HEIGHT + PADDING;
        connectWidth = panelWidth - PADDING * 2;

        infoY = connectY + BUTTON_HEIGHT + PADDING;

        amountY = infoY + fontHeight * 2 + 6 + PADDING;
        minusX = panelX + PADDING;
        amountBoxX = minusX + BUTTON_HEIGHT + BUTTON_SPACING;
        amountBoxWidth = connectWidth - (BUTTON_HEIGHT + BUTTON_SPACING) * 2;
        plusX = amountBoxX + amountBoxWidth + BUTTON_SPACING;

        buyX = panelX + PADDING;
        buyY = amountY + BUTTON_HEIGHT + BUTTON_SPACING;
        buyWidth = connectWidth;

        statusY = buyY + BUTTON_HEIGHT + PADDING + 10;
        tipsY = statusY + fontHeight + PADDING * 2;

        int dividerY = listPanelY + PADDING + fontHeight + 10;
        listX = listPanelX + PADDING;
        listY = dividerY + PADDING;
        listWidth = listPanelWidth - PADDING * 2 - SCROLLBAR_WIDTH - 8;
        listHeight = listPanelY + listPanelHeight - listY - PADDING;
        visibleRows = Math.max(1, listHeight / (ROW_HEIGHT + ROW_PADDING));

        scrollbarX = listPanelX + listPanelWidth - PADDING - SCROLLBAR_WIDTH;
        scrollbarY = listY;
        scrollbarHeight = listHeight;
    }

    @Override
    void draw(int mouseX, int mouseY) {
        drawBuyPanel(mouseX, mouseY);
        drawProductPanel(mouseX, mouseY);
    }

    private void drawBuyPanel(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        int fontHeight = regular.getHeight();

        menu.drawPanelShadow(panelX, panelY, panelWidth, panelHeight);
        RoundedUtils.drawRoundOutline(panelX, panelY, panelWidth, panelHeight, RADIUS, -0.5f, BODY_COLOR, accent);
        menu.drawSectionHeader(panelX + PADDING, panelY + PADDING, "Localts", new ChatComponentText(" Shop").getFormattedText());

        apiKeyField.drawTextBox();
        menu.drawButton(connectX, connectY, connectWidth, user == null ? "Connect" : "Refresh", mouseX, mouseY, user == null);

        if (user != null) {
            regular.drawStringWithShadow("Signed In As " + user.username, panelX + PADDING, infoY, Color.WHITE.getRGB());
            regular.drawStringWithShadow("Balance: " + formatCredits(user.balance) + " Credits", panelX + PADDING, infoY + fontHeight + 6, accent.getRGB());
        } else {
            regular.drawStringWithShadow("Not Connected", panelX + PADDING, infoY, 0x999999);
            small.drawString("Get a key at localts.store > API", panelX + PADDING, infoY + fontHeight + 6, 0x777777);
        }

        Product product = selectedProduct >= 0 && selectedProduct < products.size() ? products.get(selectedProduct) : null;
        boolean canBuy = user != null && product != null && product.stock > 0 && !busy;

        menu.drawButton(minusX, amountY, BUTTON_HEIGHT, "-", mouseX, mouseY, false);
        RoundedUtils.drawRoundOutline(amountBoxX, amountY, amountBoxWidth, BUTTON_HEIGHT, RADIUS, -0.5f, BODY_COLOR, RenderUtils.withAlphaColor(accent, 130));
        String amountLabel = amount + "x";
        if (product != null) amountLabel += "  ·  " + formatCredits(product.totalFor(amount)) + " Credits";
        regular.drawCenteredStringWithShadow(amountLabel, amountBoxX + amountBoxWidth / 2f, amountY + (BUTTON_HEIGHT - fontHeight) / 2f, Color.WHITE.getRGB());
        menu.drawButton(plusX, amountY, BUTTON_HEIGHT, "+", mouseX, mouseY, false);

        String buyLabel = busy ? "Working..." : product == null ? "Select A Product" : "Buy & Login";
        menu.drawButton(buyX, buyY, buyWidth, buyLabel, mouseX, mouseY, canBuy);

        menu.drawStatusPill(panelX, panelWidth, statusY, status, statusIsError);

        Gui.drawRect(panelX + PADDING, tipsY - 10, panelX + panelWidth - PADDING, tipsY - 9, RenderUtils.withAlpha(Color.WHITE, 20));
        String tips = "Bought accounts are saved to your alt list";
        regular.drawString(tips, panelX + (panelWidth - regular.getStringWidth(tips)) / 2f, tipsY, 0x777777);
    }

    private void drawProductPanel(int mouseX, int mouseY) {
        Color accent = ColorManager.getColor();
        CustomFontRenderer regular = FontUtils.getFont("sf", 18);
        CustomFontRenderer small = FontUtils.getFont("sf", 14);
        int fontHeight = regular.getHeight();

        menu.drawPanelShadow(listPanelX, listPanelY, listPanelWidth, listPanelHeight);
        RoundedUtils.drawRoundOutline(listPanelX, listPanelY, listPanelWidth, listPanelHeight, RADIUS, -0.5f, BODY_COLOR, accent);
        menu.drawSectionHeader(listPanelX + PADDING, listPanelY + PADDING, "Products", new ChatComponentText(" (" + products.size() + ")").getFormattedText());

        int dividerY = listPanelY + PADDING + fontHeight + 10;
        Gui.drawRect(listPanelX + PADDING, dividerY, listPanelX + listPanelWidth - PADDING, dividerY + 1, RenderUtils.withAlpha(Color.WHITE, 20));

        if (products.isEmpty()) {
            float centerX = listX + listWidth / 2f;
            float centerY = listY + listHeight / 2f - fontHeight;
            regular.drawCenteredStringWithShadow(user == null ? "Connect To Load Products" : "No Products In Stock", centerX, centerY, Color.WHITE.getRGB());
            regular.drawCenteredStringWithShadow("Click A Product, Pick An Amount, Then Buy", centerX, centerY + fontHeight + 4, 0x999999);
            return;
        }

        menu.enableScissor(listX, listY, listWidth, listHeight);
        int stride = ROW_HEIGHT + ROW_PADDING;
        for (int row = 0; row < visibleRows + 1; row++) {
            int index = scrollOffset + row;
            if (index >= products.size()) break;
            Product product = products.get(index);
            int y = listY + row * stride;
            boolean selected = index == selectedProduct;
            boolean hovered = mouseX >= listX && mouseX <= listX + listWidth && mouseY >= y && mouseY <= y + ROW_HEIGHT
                    && mouseY >= listY && mouseY <= listY + listHeight;
            boolean soldOut = product.stock <= 0;

            Color fill = selected ? RenderUtils.withAlphaColor(accent, 35) : BODY_COLOR;
            Color outline = selected ? accent : (hovered ? RenderUtils.withAlphaColor(accent, 150) : RenderUtils.withAlphaColor(Color.WHITE, 25));
            RoundedUtils.drawRoundOutline(listX, y, listWidth, ROW_HEIGHT, RADIUS, selected ? 0.5f : -0.5f, fill, outline);

            int textX = listX + ROW_PADDING + 2;
            int nameY = y + (ROW_HEIGHT - regular.getHeight() - 3 - small.getHeight()) / 2;
            int nameColor = soldOut ? 0x777777 : (selected ? accent.getRGB() : Color.WHITE.getRGB());
            regular.drawString(product.name, textX, nameY, nameColor);

            StringBuilder meta = new StringBuilder(product.category);
            if (product.isRefreshTokenProduct()) appendMeta(meta, "Auto-Login");
            else if (product.isCookieProduct()) appendMeta(meta, "Cookie (Manual)");
            for (String tag : product.tags) {
                if (!tag.trim().isEmpty() && !"cookie".equalsIgnoreCase(tag.trim())) appendMeta(meta, tag.trim());
            }
            small.drawString(meta.toString(), textX, nameY + regular.getHeight() + 3, 0x999999);

            if (product.isUnbanned()) {
                String badge = "Unbanned";
                int badgeWidth = small.getStringWidth(badge) + 10;
                int badgeX = textX + regular.getStringWidth(product.name) + 8;
                RoundedUtils.drawRoundOutline(badgeX, nameY - 1, badgeWidth, small.getHeight() + 4, 4f, -0.5f,
                        RenderUtils.withAlphaColor(new Color(90, 210, 130), 40), RenderUtils.withAlphaColor(new Color(90, 210, 130), 160));
                small.drawString(badge, badgeX + 5, nameY + 1, new Color(90, 210, 130).getRGB());
            }

            String price = formatCredits(product.priceInCredits) + " Credits";
            String stock = soldOut ? "Sold Out" : product.stock + " In Stock";
            int priceX = listX + listWidth - ROW_PADDING - 2 - regular.getStringWidth(price);
            int stockX = listX + listWidth - ROW_PADDING - 2 - small.getStringWidth(stock);
            regular.drawString(price, priceX, nameY, soldOut ? 0x777777 : accent.getRGB());
            small.drawString(stock, stockX, nameY + regular.getHeight() + 3, soldOut ? DANGER.getRGB() : 0x999999);
        }
        menu.disableScissor();

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int thumbHeight = Math.max(scrollbarHeight * visibleRows / Math.max(1, products.size()), 20);
            int thumbY = scrollbarY + (scrollbarHeight - thumbHeight) * scrollOffset / maxScroll;
            boolean scrollbarHovered = mouseX >= scrollbarX - 2 && mouseX <= scrollbarX + SCROLLBAR_WIDTH + 2 && mouseY >= thumbY && mouseY <= thumbY + thumbHeight;
            RoundedUtils.drawRoundOutline(scrollbarX, scrollbarY, SCROLLBAR_WIDTH, scrollbarHeight, SCROLLBAR_WIDTH / 2f, -0.5f,
                    RenderUtils.withAlphaColor(BODY_COLOR, 200), RenderUtils.withAlphaColor(Color.BLACK, 0));
            RoundedUtils.drawRoundOutline(scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbHeight, SCROLLBAR_WIDTH / 2f, -0.5f,
                    (draggingScrollbar || scrollbarHovered) ? accent : RenderUtils.withAlphaColor(accent, 170),
                    RenderUtils.withAlphaColor(Color.BLACK, 0));
        }
    }

    private int maxScroll() {
        return Math.max(0, products.size() - visibleRows);
    }

    private static void appendMeta(StringBuilder meta, String part) {
        if (meta.length() > 0) meta.append("  ·  ");
        meta.append(part);
    }

    @Override
    boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        apiKeyField.mouseClicked(mouseX, mouseY, mouseButton);

        if (menu.isMouseOverButton(mouseX, mouseY, connectX, connectY, connectWidth, BUTTON_HEIGHT)) {
            connect();
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, minusX, amountY, BUTTON_HEIGHT, BUTTON_HEIGHT)) {
            amount = Math.max(1, amount - 1);
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, plusX, amountY, BUTTON_HEIGHT, BUTTON_HEIGHT)) {
            amount = Math.min(MAX_AMOUNT, amount + 1);
            return true;
        }
        if (menu.isMouseOverButton(mouseX, mouseY, buyX, buyY, buyWidth, BUTTON_HEIGHT)) {
            buy();
            return true;
        }

        if (mouseX >= listX && mouseX <= listX + listWidth && mouseY >= listY && mouseY < listY + listHeight) {
            int row = (mouseY - listY) / (ROW_HEIGHT + ROW_PADDING);
            int index = scrollOffset + row;
            if (index >= 0 && index < products.size() && (mouseY - listY) % (ROW_HEIGHT + ROW_PADDING) <= ROW_HEIGHT) {
                selectedProduct = index;
                Product product = products.get(index);
                if (product.stock > 0) amount = Math.min(amount, Math.min(MAX_AMOUNT, product.stock));
                return true;
            }
        }

        if (mouseX >= scrollbarX && mouseX <= scrollbarX + SCROLLBAR_WIDTH && mouseY >= scrollbarY && mouseY <= scrollbarY + scrollbarHeight) {
            draggingScrollbar = true;
            dragStartY = mouseY;
            scrollStart = scrollOffset;
            return true;
        }
        return false;
    }

    @Override
    void mouseReleased(int mouseX, int mouseY, int state) {
        draggingScrollbar = false;
    }

    @Override
    void mouseClickMove(int mouseX, int mouseY) {
        if (!draggingScrollbar || maxScroll() <= 0) return;
        int thumbHeight = Math.max(scrollbarHeight * visibleRows / Math.max(1, products.size()), 20);
        int scrollRange = scrollbarHeight - thumbHeight;
        int scrollDelta = scrollRange > 0 ? (mouseY - dragStartY) * maxScroll() / scrollRange : 0;
        scrollOffset = Math.min(maxScroll(), Math.max(0, scrollStart + scrollDelta));
    }

    @Override
    void mouseScrolled(int wheel) {
        if (wheel > 0) scrollOffset = Math.max(0, scrollOffset - 1);
        else scrollOffset = Math.min(maxScroll(), scrollOffset + 1);
    }

    @Override
    void keyTyped(char typedChar, int keyCode) {
        apiKeyField.keyTyped(typedChar, keyCode);
        if (apiKeyField.isFocused() && keyCode == Keyboard.KEY_RETURN) connect();
    }

    private void setStatus(String message, boolean isError) {
        status = message;
        statusIsError = isError;
    }

    private void connect() {
        if (busy) return;
        String key = apiKeyField.getText().trim();
        if (key.isEmpty()) {
            setStatus("Enter Your API Key First!", true);
            return;
        }
        busy = true;
        setStatus("Connecting To Localts...", false);

        new Thread(() -> {
            try {
                User me = LocaltsClient.getMe(key);
                List<Product> fetched = LocaltsClient.getProducts(key);
                mc.addScheduledTask(() -> {
                    apiKey = key;
                    saveApiKey(key);
                    user = me;
                    products.clear();
                    products.addAll(fetched);
                    selectedProduct = -1;
                    scrollOffset = 0;
                    setStatus("Connected As " + me.username + "!", false);
                    busy = false;
                });
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Connection Failed!" : e.getMessage();
                mc.addScheduledTask(() -> {
                    setStatus(message, true);
                    busy = false;
                });
            }
        }, "Localts Connect Worker").start();
    }

    private void buy() {
        if (busy || user == null) return;
        if (selectedProduct < 0 || selectedProduct >= products.size()) {
            setStatus("Select A Product First!", true);
            return;
        }
        Product product = products.get(selectedProduct);
        if (product.stock <= 0) {
            setStatus("That Product Is Sold Out!", true);
            return;
        }
        int quantity = Math.max(1, Math.min(amount, product.stock));
        if (product.totalFor(quantity) > user.balance + 1e-9) {
            setStatus("Not Enough Credits! Top Up At localts.store", true);
            return;
        }

        busy = true;
        setStatus("Placing Order...", false);
        String key = apiKey;

        new Thread(() -> {
            try {
                String orderId = LocaltsClient.purchase(key, product.id, quantity);
                Order order = LocaltsClient.waitForOrder(key, orderId, message -> mc.addScheduledTask(() -> setStatus(message, false)));
                saveDelivery(product, order);

                int loggedIn = 0;
                String firstUsername = null;
                for (OrderItem item : order.items) {
                    String refreshToken = LocaltsClient.extractRefreshToken(item.content);
                    if (refreshToken.isEmpty()) continue;
                    mc.addScheduledTask(() -> setStatus("Logging In Delivered Account...", false));
                    MicrosoftOAuthTranslation.LoginData login;
                    try {
                        login = MicrosoftOAuthTranslation.login(refreshToken);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (!login.isGood()) continue;
                    String savedToken = login.newRefreshToken != null ? login.newRefreshToken : refreshToken;
                    boolean first = firstUsername == null;
                    if (first) firstUsername = login.username;
                    mc.addScheduledTask(() -> {
                        menu.saveOAuthAltToFile(login.username, savedToken);
                        if (first) mc.setSession(new Session(login.username, login.uuid, login.mcToken, "microsoft"));
                    });
                    loggedIn++;
                }

                int delivered = order.items.size();
                int finalLoggedIn = loggedIn;
                String finalFirst = firstUsername;
                mc.addScheduledTask(() -> {
                    if (finalLoggedIn > 0) {
                        setStatus(finalLoggedIn == 1 ? "Logged In As " + finalFirst + "!" : finalLoggedIn + " Accounts Added, Logged In As " + finalFirst + "!", false);
                    } else if (delivered > 0) {
                        setStatus(delivered + " Delivered, None Could Log In - Saved To " + DELIVERY_FILE, true);
                    } else {
                        setStatus("Order " + orderId + " Delivered Nothing!", true);
                    }
                    busy = false;
                });
                refreshUser(key);
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Purchase Failed!" : e.getMessage();
                mc.addScheduledTask(() -> {
                    setStatus(message, true);
                    busy = false;
                });
            }
        }, "Localts Purchase Worker").start();
    }

    private void refreshUser(String key) {
        try {
            User me = LocaltsClient.getMe(key);
            List<Product> fetched = LocaltsClient.getProducts(key);
            mc.addScheduledTask(() -> {
                user = me;
                String selectedId = selectedProduct >= 0 && selectedProduct < products.size() ? products.get(selectedProduct).id : null;
                products.clear();
                products.addAll(fetched);
                selectedProduct = -1;
                for (int i = 0; i < products.size(); i++) {
                    if (products.get(i).id.equals(selectedId)) selectedProduct = i;
                }
                scrollOffset = Math.min(scrollOffset, maxScroll());
            });
        } catch (Exception ignored) {
        }
    }

    private void saveDelivery(Product product, Order order) {
        File file = new File(menu.getYuriDir(), DELIVERY_FILE);
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date());
        try (PrintWriter out = new PrintWriter(new java.io.FileWriter(file, true))) {
            out.println("# " + stamp + " | order " + order.id + " | " + product.name);
            for (OrderItem item : order.items) out.println(item.content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String loadApiKey() {
        File file = new File(menu.getYuriDir(), KEY_FILE);
        if (!file.exists()) return "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            String decrypted = line == null ? null : TokenEncryption.decrypt(line.trim());
            return decrypted == null ? "" : decrypted;
        } catch (IOException e) {
            return "";
        }
    }

    private void saveApiKey(String key) {
        File file = new File(menu.getYuriDir(), KEY_FILE);
        try (PrintWriter out = new PrintWriter(file)) {
            out.println(TokenEncryption.encrypt(key));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatCredits(double credits) {
        if (Math.abs(credits - Math.rint(credits)) < 1e-9) return String.valueOf((long) Math.rint(credits));
        return String.format(Locale.ROOT, "%.2f", credits);
    }
}
