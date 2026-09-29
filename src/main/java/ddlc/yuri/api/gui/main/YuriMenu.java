package ddlc.yuri.api.gui.main;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.gui.alt.YuriAltMenu;
import ddlc.yuri.api.gui.main.api.MenuButton;
import ddlc.yuri.api.gui.main.api.MenuShaderBackground;
import ddlc.yuri.api.gui.main.windows.WelcomeWindow;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class YuriMenu extends GuiScreen {

    private final List<MenuButton> buttons = new ArrayList<>();
    private WelcomeWindow welcomeWindow;
    private static boolean displayWindow = true;

    public static final Color BASE_BG = new Color(10, 9, 13, 255);
    private static final Color ERROR_COLOR = new Color(214, 64, 69);
    private static final float CARD_PADDING_TOP = 32f;
    private static final float CARD_PADDING_BOTTOM = 28f;

    private float animX;
    private float animY;

    private ScaledResolution sr;

    private String currentLine;
    private boolean showError;

    private final String[] lines = {
            "natsuki is gonna steal you again?!",
            "i picked out this font just for you",
            "sayori would never notice the little things like i do",
            "i brought my knives, just in case",
            "did you know obsession is just love that hasn't been understood yet",
            "monika keeps rearranging the club, i've noticed",
            "i wrote you something, but i'm too embarrassed to show it yet",
            "the deeper you go into me, the more you'll find",
            "i trimmed a page out of my favorite book for you",
            "i don't share well. you should know that by now",
            "some feelings cut deeper than they should",
            "i rehearsed this greeting for three days",
            "i like it when you look at only me",
            "there's a certain beauty in the things people are afraid of",
            "i saved you a seat, right next to mine",
            "i've been reading about you as much as my books"
    };

    private final String[] errorLines = {
            "monika hacked into your client",
            "i think someone's been messing with my pages",
            "it seems the club isn't quite the same anymore",
            "something's missing, and it isn't supposed to be",
            "i don't remember writing this line",
            "the character files feel... off today",
            "she's not supposed to know she's not real"
    };

    @Override
    public void initGui() {
        sr = new ScaledResolution(mc);

        Random rng = new Random();

        showError = rng.nextFloat() < 0.02f;

        currentLine = showError
                ? errorLines[rng.nextInt(errorLines.length)]
                : lines[rng.nextInt(lines.length)];

        buttons.clear();

        buttons.add(new MenuButton("Singleplayer", () -> mc.displayGuiScreen(new GuiSelectWorld(this))));
        buttons.add(new MenuButton("Multiplayer", () -> mc.displayGuiScreen(new GuiMultiplayer(this))));
        buttons.add(new MenuButton("Account Manager", () -> mc.displayGuiScreen(new YuriAltMenu())));
        buttons.add(new MenuButton("Settings", () -> mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings))));
        buttons.add(new MenuButton("Exit", () -> mc.shutdown()));

        welcomeWindow = null;
        if (displayWindow) {
            float windowWidth = 250f;
            float windowHeight = 100f;
            float windowX = 12f;
            float windowY = 12f;

            String placeholderText = "Welcome to Yuri Client!\n\n" +
                    "Any questions? Join our Discord! https://discord.gg/8VhKD2QQHc\n\n" +
                    "Happy Halloween!";

            welcomeWindow = new WelcomeWindow(windowX, windowY, windowWidth, windowHeight, "Welcome Back!", placeholderText);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        sr = new ScaledResolution(mc);

        float w = sr.getScaledWidth();
        float h = sr.getScaledHeight();

        Gui.drawRect(0, 0, (int) w, (int) h, BASE_BG.getRGB());
        MenuShaderBackground.get().render(w, h);

        float mx = (mouseX - w / 2f) * 0.01f;
        float my = (mouseY - h / 2f) * 0.01f;

        animX += (mx - animX) * 0.05f;
        animY += (my - animY) * 0.05f;

        float center = w / 2f;

        drawContent(w, h, mouseX, mouseY, center);

        if (welcomeWindow != null) {
            if (!welcomeWindow.shouldWindowClose()) {
                welcomeWindow.render(mouseX, mouseY);
            } else {
                displayWindow = false;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawContent(float w, float h, float mouseX, float mouseY, float center) {

        CustomFontRenderer font = FontUtils.getScaledFont("sf", 18, (float) sr.getScaleFactor() / 2f);
        if (font == null) return;

        float logoHeight = 128f;
        float logoGap = 20f;
        float lineHeight = font.getHeight();
        float lineGap = 30f;
        float buttonHeight = FontUtils.getScaledFont("sf", 14, (float) sr.getScaleFactor() / 2f).getHeight() + 10f;
        float rowSpacing = 2.0f;
        float buttonsHeight = buttonHeight * 2 + rowSpacing;

        float cardHeight = CARD_PADDING_TOP + logoHeight + logoGap + buttonsHeight + lineGap + lineHeight + CARD_PADDING_BOTTOM;

        float cardY = h / 2f - cardHeight / 2f;

        float logoY = cardY + CARD_PADDING_TOP;

        GlStateManager.pushMatrix();
        GlStateManager.translate(animX, animY, 0);

        RenderUtils.drawImage(
                new ResourceLocation("yuri/gui/logo.png"),
                center - logoHeight / 2f,
                logoY,
                logoHeight,
                logoHeight
        );

        GlStateManager.popMatrix();

        float buttonsY = logoY + logoHeight + logoGap;
        drawButtons(center, buttonsY, mouseX, mouseY, buttonHeight, rowSpacing);

        float lineY = buttonsY + buttonsHeight + lineGap;
        int lineColor = showError
                ? new Color(ERROR_COLOR.getRed(), ERROR_COLOR.getGreen(), ERROR_COLOR.getBlue(), 190).getRGB()
                : RenderUtils.withAlpha(ColorManager.getColor(), 190);
        font.drawCenteredString(currentLine, center, lineY, lineColor);

        drawFooterBadge(font, "Yuri " + Yuri.VERSION, 0, h - font.getHeight() - 2f, false);

        String credits = "Brought to you by: unlegit!";
        drawFooterBadge(font, credits, w - font.getStringWidth(credits), h - font.getHeight() - 2f, true);
    }

    private void drawFooterBadge(CustomFontRenderer font, String text, float x, float y, boolean alignRight) {
        float paddingX = 2f;
        float badgeX = alignRight ? x - paddingX : x;

        font.drawString(text, badgeX + paddingX, y, new Color(196, 199, 199, 190).getRGB());
    }

    private void drawButtons(float center, float y, float mx, float my, float buttonHeight, float spacing) {
        CustomFontRenderer font = FontUtils.getScaledFont("sf", 14, (float) sr.getScaleFactor() / 2f);
        if (font == null || buttons.size() < 5) return;

        drawRow(new MenuButton[]{buttons.get(0), buttons.get(1), buttons.get(2)}, center, y, spacing, mx, my);

        drawRow(new MenuButton[]{buttons.get(3), buttons.get(4)}, center, y + buttonHeight + spacing, spacing, mx, my);
    }

    private void drawRow(MenuButton[] row, float center, float y, float gap, float mx, float my) {

        CustomFontRenderer font = FontUtils.getScaledFont("sf", 14, (float) sr.getScaleFactor() / 2f);
        if (font == null) return;

        float totalWidth = 0;

        for (int i = 0; i < row.length; i++) {
            float itemWidth = row[i].label.isEmpty()
                    ? font.getHeight() + 10f
                    : row[i].getLayoutWidth(font.getStringWidth(row[i].label));
            totalWidth += itemWidth;
            if (i != row.length - 1) totalWidth += gap;
        }

        float x = center - totalWidth / 2f;

        for (MenuButton b : row) {

            float textWidth = font.getStringWidth(b.label);
            float textHeight = font.getHeight();

            float itemHeight = b.getLayoutHeight(textHeight);
            float itemWidth = b.label.isEmpty() ? itemHeight : b.getLayoutWidth(textWidth);

            if (b.label.isEmpty()) {
                b.layoutBox(x, y - 5f, itemWidth, itemHeight);
            } else {
                b.layout(x + 8f, y, textWidth, textHeight);
            }

            b.updateHover(mx, my);
            b.renderBox();

            if (!b.label.isEmpty()) {
                font.drawString(b.label, b.x + (b.width - textWidth) / 2f, y, b.getTextColor());
            }

            x += b.width + gap;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (welcomeWindow != null && !welcomeWindow.shouldWindowClose()) {
            welcomeWindow.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mouseButton == 0) {
            for (MenuButton button : buttons) {
                if (button.isHovered(mouseX, mouseY)) {
                    button.mouseClicked();
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (welcomeWindow != null) {
            welcomeWindow.mouseReleased();
        }
        super.mouseReleased(mouseX, mouseY, state);
    }
}