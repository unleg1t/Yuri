package ddlc.yuri.api.font;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


public class CustomFontRenderer extends CustomFont {
    protected CustomFont.CharData[] boldChars = new CustomFont.CharData[65536];
    protected CustomFont.CharData[] italicChars = new CustomFont.CharData[65536];
    protected CustomFont.CharData[] boldItalicChars = new CustomFont.CharData[65536];

    public int FONT_HEIGHT = 9;

    private final int[] colorCode = new int[32];

    private boolean useMCustomFont = false;

    public CustomFontRenderer(Font font, boolean antiAlias, boolean fractionalMetrics) {
        super(font, antiAlias, fractionalMetrics);
        setupMinecraftColorcodes();
    }

    String nameFontTTF;

    public CustomFontRenderer(String NameFontTTF, float size, int fonttype, boolean antiAlias, boolean fractionalMetrics) {
        super(getFontFromTTF(new ResourceLocation("yuri/fonts/" + NameFontTTF+".ttf"), size,fonttype), antiAlias, fractionalMetrics);
        this.nameFontTTF = NameFontTTF;
        this.useMCustomFont = NameFontTTF.equalsIgnoreCase("mc");
        setupMinecraftColorcodes();
    }

    public String getNameFontTTF() {
        return this.nameFontTTF;
    }

    public float drawString(String text, float x, float y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x, y, color, false);
        }
        return drawString(text, x, y, color, false);
    }

    public float drawString(String text, double x, double y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) x, (float) y, color, false);
        }
        return drawString(text, x, y, color, false);
    }

    public float drawStringWithShadow(String text, float x, float y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x, y, color, true);
        }
        float shadowWidth = drawString(text, x + 0.5, y + 0.5, color, true);
        return Math.max(shadowWidth, drawString(text, x, y, color, false));
    }

    public float drawStringWithShadow(String text, double x, double y, int color) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) x, (float) y, color, true);
        }
        float shadowWidth = drawString(text, x + 0.5, y + 0.5, color, true);
        return Math.max(shadowWidth, drawString(text, x, y, color, true));
    }

    public float drawCenteredString(String text, float x, float y, int color) {
        if (useMCustomFont) {
            int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x - width / 2f, y, color, false);
        }
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public float drawCenteredString(String text, double x, double y, int color) {
        if (useMCustomFont) {
            int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) (x - width / 2f), (float) y, color, false);
        }
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public float drawCenteredStringWithShadow(String text, float x, float y, int color) {
        if (useMCustomFont) {
            int width = Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, x - width / 2f, y, color, true);
        }
        float shadowWidth = drawString(text, x - getStringWidth(text) / 2 + 0.45D, y + 0.5D, color, true);
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public void drawStringWithOutline(String text, double x, double y, int color) {
        drawString(text, x - .5, y, 0x000000);

        drawString(text, x + .5, y, 0x000000);

        drawString(text, x, y - .5, 0x000000);

        drawString(text, x, y + .5, 0x000000);

        drawString(text, x, y, color);
    }

    public void drawCenteredStringWithOutline(String text, double x, double y, int color) {
        drawCenteredString(text, x - .5, y, 0x000000);

        drawCenteredString(text, x + .5, y, 0x000000);

        drawCenteredString(text, x, y - .5, 0x000000);

        drawCenteredString(text, x, y + .5, 0x000000);

        drawCenteredString(text, x, y, color);
    }

    public float drawCenteredStringWithShadow(String text, double x, double y, int color) {
        float shadowWidth = drawString(text, x - getStringWidth(text) / 2 + 0.45D, y + 0.5D, color, true);
        return drawString(text, x - getStringWidth(text) / 2, y, color);
    }

    public float drawString(String text, double x, double y, int color, boolean shadow) {
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) x, (float) y, color, shadow);
        }

        if (text == null) {
            return 0.0F;
        }

        if (tex == null) {
            // atlas failed to build - fall back to the vanilla renderer instead of crashing
            return Minecraft.getMinecraft().fontRendererObj.drawString(text, (float) x, (float) y, color, shadow);
        }

        if (color == 553648127) {
            color = 16777215;
        }

        if ((color & 0xFC000000) == 0) {
            color |= -16777216;
        }

        if (shadow) {
            color = (color & 0xFCFCFC) >> 2 | color & new Color(20, 20, 20, 200).getRGB();
        }

        int currentColor = color;
        CustomFont.CharData[] currentData = this.charData;
        float alpha = (color >> 24 & 0xFF) / 255.0F;
        boolean randomCase = false;
        boolean bold = false;
        boolean italic = false;
        boolean strikethrough = false;
        boolean underline = false;
        boolean render = true;
        x *= 2.0D;
        y = (y - 2.0D) * 2.0D;

        if (render) {
            GL11.glPushMatrix();
            try {
                GlStateManager.scale(0.5D, 0.5D, 0.5D);
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(770, 771);
                GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
                int size = text.length();
                GlStateManager.enableTexture2D();
                GlStateManager.bindTexture(tex.getGlTextureId());

            GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getGlTextureId());

            for (int i = 0; i < size; i++) {
                char character = text.charAt(i);
                if ((String.valueOf(character).equals("\247")) && (i < size)) {
                    int colorIndex = 21;

                    try {
                        colorIndex = "0123456789abcdefklmnor".indexOf(text.charAt(i + 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (colorIndex < 16) {
                        bold = false;
                        italic = false;
                        randomCase = false;
                        underline = false;
                        strikethrough = false;
                        GlStateManager.bindTexture(tex.getGlTextureId());
                        currentData = this.charData;

                        if ((colorIndex < 0) || (colorIndex > 15)) {
                            colorIndex = 15;
                        }

                        if (shadow) {
                            colorIndex += 16;
                        }

                        int colorcode = this.colorCode[colorIndex];
                        currentColor = (color & 0xFF000000) | (colorcode & 0x00FFFFFF);
                        GlStateManager.color((colorcode >> 16 & 0xFF) / 255.0F, (colorcode >> 8 & 0xFF) / 255.0F, (colorcode & 0xFF) / 255.0F, alpha);
                    } else if (colorIndex == 16) {
                        randomCase = true;
                    } else if (colorIndex == 17) {
                        bold = true;

                        if (italic) {
                            currentData = this.charData;
                        } else {
                            currentData = this.charData;
                        }
                    } else if (colorIndex == 18) {
                        strikethrough = true;
                    } else if (colorIndex == 19) {
                        underline = true;
                    } else if (colorIndex == 20) {
                        italic = true;

                        if (bold) {
                            currentData = this.charData;
                        } else {
                            currentData = this.charData;
                        }
                    } else if (colorIndex == 21) {
                        bold = false;
                        italic = false;
                        randomCase = false;
                        underline = false;
                        strikethrough = false;
                        currentColor = color;
                        GlStateManager.color((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
                        GlStateManager.bindTexture(tex.getGlTextureId());
                        currentData = this.charData;
                    }

                    i++;
                } else {
                    CustomFont.CharData data = character < currentData.length ? currentData[character] : null;

                    if (data != null && data.valid) {
                        GL11.glBegin(GL11.GL_TRIANGLES);
                        drawChar(currentData, character, (float) x, (float) y);
                        GL11.glEnd();

                        if (strikethrough) {
                            drawLine(x, y + data.height / 2, x + data.width - 8.0D, y + data.height / 2, 1.0F);
                        }

                        if (underline) {
                            drawLine(x, y + data.height - 2.0D, x + data.width - 8.0D, y + data.height - 2.0D, 1.0F);
                        }

                        x += data.width - 8 + this.charOffset;
                    } else {
                        // glyph missing from the atlas: still advance so the rest of
                        // the line stays aligned instead of sliding left
                        if (shouldDelegateToVanilla(character)) {
                            // wide east-asian chars have complete coverage in the vanilla
                            // unicode font pages - draw them with a 2x scale to cancel
                            // this renderer's 0.5 scale matrix
                            String vanillaChar = String.valueOf(character);
                            GlStateManager.pushMatrix();
                            GlStateManager.scale(2.0D, 2.0D, 2.0D);
                            Minecraft.getMinecraft().fontRendererObj.drawString(vanillaChar, (float) (x / 2.0D), (float) (y / 2.0D) + 1.0F, currentColor, false);
                            GlStateManager.popMatrix();
                            GlStateManager.enableTexture2D();
                            GlStateManager.bindTexture(tex.getGlTextureId());
                            GlStateManager.color((currentColor >> 16 & 0xFF) / 255.0F, (currentColor >> 8 & 0xFF) / 255.0F, (currentColor & 0xFF) / 255.0F, alpha);

                            x += Minecraft.getMinecraft().fontRendererObj.getStringWidth(vanillaChar) * 2.0D;
                        } else {
                            x += advanceFor(character);
                        }

                        // astral glyphs (emoji) arrive as surrogate pairs - consume both halves
                        if (Character.isHighSurrogate(character) && i + 1 < size && Character.isLowSurrogate(text.charAt(i + 1))) {
                            i++;
                        }
                    }
                }
            }

            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
            } finally {
                GL11.glPopMatrix();
            }
        }

        return (float) x / 2.0F;
    }

    public int getStringWidth(String text) {
        if (text == null) {
            return 0;
        }
        if (useMCustomFont) {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(text);
        }
        int width = 0;
        int size = text.length();

        for (int i = 0; i < size; i++) {
            char character = text.charAt(i);

            if (character == '\247') {
                // formatting code renders with zero width, drawString skips it the same way
                i++;
            } else {
                CustomFont.CharData data = character < this.charData.length ? this.charData[character] : null;

                if (data != null && data.valid) {
                    width += data.width - 8 + this.charOffset;
                } else {
                    width += missingCharWidth(character);

                    if (Character.isHighSurrogate(character) && i + 1 < size && Character.isLowSurrogate(text.charAt(i + 1))) {
                        i++;
                    }
                }
            }
        }

        return width / 2;
    }

    public int getStringWidthCust(String text) {
        return getStringWidth(text);
    }

    public void setFont(Font font) {
        super.setFont(font);
    }

    public void setAntiAlias(boolean antiAlias) {
        super.setAntiAlias(antiAlias);
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        super.setFractionalMetrics(fractionalMetrics);
    }

    protected DynamicTexture texBold;
    protected DynamicTexture texItalic;
    protected DynamicTexture texItalicBold;

    private void drawLine(double x, double y, double x1, double y1, float width) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(width);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x1, y1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public List<String> wrapWords(String text, double width) {
        List finalWords = new ArrayList();

        if (getStringWidth(text) > width) {
            String[] words = text.split(" ");
            String currentWord = "";
            char lastColorCode = 65535;

            for (String word : words) {
                for (int i = 0; i < word.toCharArray().length; i++) {
                    char c = word.toCharArray()[i];

                    if ((c == '\247') && (i < word.toCharArray().length - 1)) {
                        lastColorCode = word.toCharArray()[(i + 1)];
                    }
                }

                if (getStringWidth(currentWord + word + " ") < width) {
                    currentWord = currentWord + word + " ";
                } else {
                    finalWords.add(currentWord);
                    currentWord = "" + lastColorCode + word + " ";
                }
            }

            if (currentWord.length() > 0) if (getStringWidth(currentWord) < width) {
                finalWords.add("" + lastColorCode + currentWord + " ");
                currentWord = "";
            } else {
                for (String s : formatString(currentWord, width)) {
                    finalWords.add(s);
                }
            }
        } else {
            finalWords.add(text);
        }

        return finalWords;
    }

    public List<String> formatString(String string, double width) {
        List finalWords = new ArrayList();
        String currentWord = "";
        char lastColorCode = 65535;
        char[] chars = string.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if ((c == '\247') && (i < chars.length - 1))
            {
                lastColorCode = chars[(i + 1)];
            }

            if (getStringWidth(currentWord + c) < width) {
                currentWord = currentWord + c;
            } else {
                finalWords.add(currentWord);
                currentWord = "" + lastColorCode + String.valueOf(c);
            }
        }

        if (currentWord.length() > 0) {
            finalWords.add(currentWord);
        }

        return finalWords;
    }

    private void setupMinecraftColorcodes() {
        for (int index = 0; index < 32; index++) {
            int noClue = (index >> 3 & 0x1) * 85;
            int red = (index >> 2 & 0x1) * 170 + noClue;
            int green = (index >> 1 & 0x1) * 170 + noClue;
            int blue = (index >> 0 & 0x1) * 170 + noClue;

            if (index == 6) {
                red += 85;
            }

            if (index >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }

            this.colorCode[index] = ((red & 0xFF) << 16 | (green & 0xFF) << 8 | blue & 0xFF);
        }
    }

    public static Font getFontFromTTF(ResourceLocation fontLocation, float fontSize, int fontType) {
        Font output = null;
        try {
            output = Font.createFont(fontType, Minecraft.getMinecraft().getResourceManager().getResource(fontLocation).getInputStream());
            output = output.deriveFont(fontSize);
        } catch (Exception e) {
            e.printStackTrace();
            return new Font("Default", fontType, (int) fontSize);
        }
        return output;
    }

    public float getMiddleOfBox(float height) {
        return height / 2f - getHeight() / 2f;
    }

    public String trimStringToWidth(String text, int width)
    {
        return this.trimStringToWidth(text, width, false);
    }

    public String trimStringToWidth(String text, int width, boolean reverse)
    {
        if (text == null)
        {
            return "";
        }

        StringBuilder stringbuilder = new StringBuilder();
        float f = 0.0F;
        int i = reverse ? text.length() - 1 : 0;
        int j = reverse ? -1 : 1;
        boolean skipNextWidth = false;

        for (int k = i; k >= 0 && k < text.length() && f < (float)width; k += j)
        {
            char c0 = text.charAt(k);
            float f1 = charWidth(c0);

            if (c0 == '\247')
            {
                f1 = 0.0F;
                skipNextWidth = true;
            }
            else if (skipNextWidth)
            {
                f1 = 0.0F;
                skipNextWidth = false;
            }

            f += f1;

            if (f > (float)width)
            {
                break;
            }

            if (reverse)
            {
                stringbuilder.insert(0, (char)c0);
            }
            else
            {
                stringbuilder.append(c0);
            }
        }

        return stringbuilder.toString();
    }

    private float charWidth(char c)
    {
        if (c >= 0 && c < this.charData.length && this.charData[c] != null && this.charData[c].valid)
        {
            return (this.charData[c].width - 8 + this.charOffset) / 2.0F;
        }

        if (shouldDelegateToVanilla(c))
        {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(String.valueOf(c)) / 2.0F;
        }

        return advanceFor(c) / 2.0F;
    }

    private int missingCharWidth(char c)
    {
        if (shouldDelegateToVanilla(c))
        {
            return Minecraft.getMinecraft().fontRendererObj.getStringWidth(String.valueOf(c));
        }

        return advanceFor(c);
    }

    // chinese/japanese/hangul can never fully fit in the atlas - the vanilla
    // unicode font pages cover them completely
    private boolean shouldDelegateToVanilla(char c)
    {
        return isWideEastAsian(c) || (c >= 0xFF61 && c <= 0xFF9F);
    }

    public float drawBorderedString(String text, double x, double y, int color, int border) {
        GlStateManager.pushMatrix();
        drawString(text, x + 0.5F, y, border, true);
        drawString(text, x - 0.5F, y, border, true);
        drawString(text, x, y + 0.5F, border, true);
        drawString(text, x, y - 0.5F, border, true);
        float resultX = drawString(text, x, y, color, false);
        GlStateManager.popMatrix();
        return resultX;
    }
}