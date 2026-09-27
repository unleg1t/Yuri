package ddlc.yuri.api.gui.alt.comp;

import ddlc.yuri.api.gui.click.novoline.GuiTheme;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class CustomTextBox extends Gui {

    private static final Color FIELD_BACKGROUND = new Color(0, 0, 0);
    private final int maxStringLength = 9999;
    private final Minecraft mc = Minecraft.getMinecraft();

    public int xPosition;
    public int yPosition;

    private int width, height;
    private String text = "";
    private String placeholder = "";
    private boolean masked = false;
    private boolean focused;
    private int cursorPosition = 0;
    private int selectionEnd = 0;
    private int scrollOffset = 0;

    public CustomTextBox(int x, int y, int w, int h) {
        this.xPosition = x;
        this.yPosition = y;
        this.width = w;
        this.height = h;
    }

    public void drawTextBox() {
        String text = masked ? mask(this.text) : this.text;

        RoundedUtils.drawRoundOutline(xPosition, yPosition, width, height, 6, -0.4f, FIELD_BACKGROUND, ColorManager.getColor());

        boolean empty = text.isEmpty();
        String renderText = empty ? placeholder : text;
        int color = empty ? 0x777777 : 0xFFFFFF;

        int textX = xPosition + 6;
        int textY = (int) (yPosition + (height / 2f) - (FontUtils.getFont("sf", 18).getHeight() / 2f));
        int availableWidth = width - 12;

        if (!empty) {
            String displayText = text;
            int totalWidth = FontUtils.getFont("sf", 18).getStringWidth(displayText);

            if (totalWidth > availableWidth) {
                String beforeCursor = text.substring(0, Math.min(cursorPosition, text.length()));
                int cursorX = FontUtils.getFont("sf", 18).getStringWidth(beforeCursor);

                if (cursorX > scrollOffset + availableWidth) {
                    scrollOffset = cursorX - availableWidth;
                } else if (cursorX < scrollOffset) {
                    scrollOffset = cursorX;
                }

                int startIdx = 0;
                for (int i = 0; i < text.length(); i++) {
                    String sub = text.substring(0, i + 1);
                    int subWidth = FontUtils.getFont("sf", 18).getStringWidth(sub);
                    if (subWidth > scrollOffset) {
                        startIdx = i;
                        break;
                    }
                }

                int endIdx = text.length();
                for (int i = startIdx; i < text.length(); i++) {
                    String sub = text.substring(startIdx, i + 1);
                    int subWidth = FontUtils.getFont("sf", 18).getStringWidth(sub);
                    if (subWidth > availableWidth) {
                        endIdx = i;
                        break;
                    }
                }

                displayText = text.substring(startIdx, endIdx);
                textX = xPosition + 6;

                int selectionStart = Math.min(cursorPosition, selectionEnd);
                int selectionEndPos = Math.max(cursorPosition, selectionEnd);

                if (focused && cursorPosition != selectionEnd) {
                    int startSelX = textX;
                    int endSelX = textX;

                    if (selectionStart >= startIdx && selectionStart <= endIdx) {
                        String beforeSel = text.substring(startIdx, selectionStart);
                        startSelX = textX + FontUtils.getFont("sf", 18).getStringWidth(beforeSel);
                    }

                    if (selectionEndPos >= startIdx && selectionEndPos <= endIdx) {
                        String beforeSelEnd = text.substring(startIdx, selectionEndPos);
                        endSelX = textX + FontUtils.getFont("sf", 18).getStringWidth(beforeSelEnd);
                    } else if (selectionEndPos > endIdx) {
                        endSelX = xPosition + width - 6;
                    }

                    if (startSelX < endSelX) {
                        enableScissor(xPosition + 1, yPosition + 1, width - 2, height - 2);
                        drawRect(startSelX, yPosition + 2, endSelX, yPosition + height - 2, new Color(77, 166, 255, 100).getRGB());
                        disableScissor();
                    }
                }

                enableScissor(xPosition + 1, yPosition + 1, width - 2, height - 2);
                boolean showCursor = focused && (System.currentTimeMillis() / 500) % 2 == 0;
                String cursorText = displayText + (showCursor ? "|" : "");
                FontUtils.getFont("sf", 18).drawString(cursorText, textX, textY, color);
                disableScissor();
            } else {
                if (focused && cursorPosition != selectionEnd) {
                    int start = Math.min(cursorPosition, selectionEnd);
                    int end = Math.max(cursorPosition, selectionEnd);
                    String beforeSelection = text.substring(0, start);
                    String selected = text.substring(start, end);

                    int startX = xPosition + 6 + FontUtils.getFont("sf", 18).getStringWidth(beforeSelection);
                    int endX = xPosition + 6 + FontUtils.getFont("sf", 18).getStringWidth(beforeSelection + selected);

                    drawRect(startX, yPosition + 2, endX, yPosition + height - 2, new Color(77, 166, 255, 100).getRGB());
                }

                boolean showCursor = focused && !empty && (System.currentTimeMillis() / 500) % 2 == 0;
                FontUtils.getFont("sf", 18).drawString(
                        renderText + (showCursor ? "|" : ""),
                        textX,
                        textY,
                        color
                );
            }
        } else {
            FontUtils.getFont("sf", 18).drawString(placeholder, textX, textY, color);
        }
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

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        focused = mouseButton == 0 &&
                mouseX >= xPosition &&
                mouseX <= xPosition + width &&
                mouseY >= yPosition &&
                mouseY <= yPosition + height;

        if (focused) {
            String displayText = masked ? mask(text) : text;
            int clickX = mouseX - xPosition - 6;
            int totalWidth = FontUtils.getFont("sf", 18).getStringWidth(displayText);

            if (clickX >= totalWidth) {
                cursorPosition = displayText.length();
                selectionEnd = cursorPosition;
            } else {
                for (int i = 0; i <= displayText.length(); i++) {
                    String sub = displayText.substring(0, i);
                    int subWidth = FontUtils.getFont("sf", 18).getStringWidth(sub);
                    if (clickX <= subWidth) {
                        cursorPosition = i;
                        selectionEnd = cursorPosition;
                        break;
                    }
                }
            }
            scrollOffset = 0;
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) return;

        if (GuiScreen.isCtrlKeyDown() && keyCode == Keyboard.KEY_A) {
            cursorPosition = text.length();
            selectionEnd = 0;
            return;
        }

        if (GuiScreen.isCtrlKeyDown() && keyCode == Keyboard.KEY_C) {
            String selectedText = getSelectedText();
            if (!selectedText.isEmpty()) {
                GuiScreen.setClipboardString(selectedText);
            }
            return;
        }

        if (GuiScreen.isCtrlKeyDown() && keyCode == Keyboard.KEY_V) {
            String clipboard = GuiScreen.getClipboardString();
            if (clipboard != null && !clipboard.isEmpty()) {
                StringBuilder filtered = new StringBuilder();
                for (char c : clipboard.toCharArray()) {
                    if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                        filtered.append(c);
                    }
                }
                String pasteText = filtered.toString();
                if (!pasteText.isEmpty()) {
                    if (cursorPosition != selectionEnd) {
                        deleteSelectedText();
                    }
                    int insertPos = Math.min(cursorPosition, text.length());
                    text = text.substring(0, insertPos) + pasteText + text.substring(insertPos);
                    if (text.length() > maxStringLength) {
                        text = text.substring(0, maxStringLength);
                    }
                    cursorPosition = Math.min(insertPos + pasteText.length(), text.length());
                    selectionEnd = cursorPosition;
                }
            }
            return;
        }

        if (GuiScreen.isCtrlKeyDown() && keyCode == Keyboard.KEY_X) {
            String selectedText = getSelectedText();
            if (!selectedText.isEmpty()) {
                GuiScreen.setClipboardString(selectedText);
                deleteSelectedText();
            }
            return;
        }

        if (keyCode == Keyboard.KEY_BACK) {
            if (cursorPosition != selectionEnd) {
                deleteSelectedText();
            } else if (cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
                selectionEnd = cursorPosition;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_DELETE) {
            if (cursorPosition != selectionEnd) {
                deleteSelectedText();
            } else if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
                selectionEnd = cursorPosition;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_LEFT) {
            if (GuiScreen.isShiftKeyDown()) {
                selectionEnd = Math.max(0, selectionEnd - 1);
            } else {
                cursorPosition = Math.max(0, cursorPosition - 1);
                selectionEnd = cursorPosition;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_RIGHT) {
            if (GuiScreen.isShiftKeyDown()) {
                selectionEnd = Math.min(text.length(), selectionEnd + 1);
            } else {
                cursorPosition = Math.min(text.length(), cursorPosition + 1);
                selectionEnd = cursorPosition;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_HOME) {
            if (GuiScreen.isShiftKeyDown()) {
                selectionEnd = 0;
            } else {
                cursorPosition = 0;
                selectionEnd = cursorPosition;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_END) {
            if (GuiScreen.isShiftKeyDown()) {
                selectionEnd = text.length();
            } else {
                cursorPosition = text.length();
                selectionEnd = cursorPosition;
            }
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
            focused = false;
            return;
        }

        if (typedChar >= 32 && typedChar != 127) {
            if (cursorPosition != selectionEnd) {
                deleteSelectedText();
            }
            if (text.length() < maxStringLength) {
                int insertPos = Math.min(cursorPosition, text.length());
                text = text.substring(0, insertPos) + typedChar + text.substring(insertPos);
                cursorPosition = insertPos + 1;
                selectionEnd = cursorPosition;
            }
        }
    }

    private void deleteSelectedText() {
        if (cursorPosition == selectionEnd) return;
        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);
        text = text.substring(0, start) + text.substring(end);
        cursorPosition = start;
        selectionEnd = cursorPosition;
    }

    public String getSelectedText() {
        if (cursorPosition == selectionEnd) return "";
        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);
        return text.substring(start, end);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text.length() > maxStringLength ? text.substring(0, maxStringLength) : text;
        this.cursorPosition = this.text.length();
        this.selectionEnd = this.cursorPosition;
        this.scrollOffset = 0;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public void setMasked(boolean masked) {
        this.masked = masked;
    }

    public boolean isMasked() {
        return masked;
    }

    private String mask(String value) {
        char[] chars = new char[value.length()];
        java.util.Arrays.fill(chars, '*');
        return new String(chars);
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            cursorPosition = text.length();
            selectionEnd = cursorPosition;
            scrollOffset = 0;
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setCursorPositionEnd() {
        this.cursorPosition = text.length();
        this.selectionEnd = this.cursorPosition;
        this.scrollOffset = 0;
    }

    public void setSelectionPos(int position) {
        this.selectionEnd = Math.max(0, Math.min(text.length(), position));
    }

    public void writeText(String textToWrite) {
        if (textToWrite == null || textToWrite.isEmpty()) return;

        StringBuilder filtered = new StringBuilder();
        for (char c : textToWrite.toCharArray()) {
            if (ChatAllowedCharacters.isAllowedCharacter(c)) {
                filtered.append(c);
            }
        }

        String filteredText = filtered.toString();
        if (filteredText.isEmpty()) return;

        if (cursorPosition != selectionEnd) {
            deleteSelectedText();
        }

        int insertPos = Math.min(cursorPosition, text.length());
        text = text.substring(0, insertPos) + filteredText + text.substring(insertPos);

        if (text.length() > maxStringLength) {
            text = text.substring(0, maxStringLength);
        }

        cursorPosition = Math.min(insertPos + filteredText.length(), text.length());
        selectionEnd = cursorPosition;
        scrollOffset = 0;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int position) {
        this.cursorPosition = Math.max(0, Math.min(text.length(), position));
    }

    public int getSelectionEnd() {
        return selectionEnd;
    }

    public void setSelectionEnd(int position) {
        this.selectionEnd = Math.max(0, Math.min(text.length(), position));
    }
}