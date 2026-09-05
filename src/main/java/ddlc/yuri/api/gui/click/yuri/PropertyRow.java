package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.misc.Timer;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class PropertyRow {

    private static final float PADDING_X = 4f;
    private static final float GAP = 3f;

    public final Property<?> property;
    private final ModuleRow module;
    public boolean opened;
    private boolean dragging;
    private boolean listening;
    private boolean textHovered;
    private float toggleAnimation;
    private final Timer backspace = new Timer();

    public PropertyRow(Property<?> property, ModuleRow module) {
        this.property = property;
        this.module = module;
    }

    private String[] getOptions() {
        if (property instanceof ModeProperty) {
            Enum<?>[] values = ((ModeProperty<?>) property).getValues();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) names[i] = values[i].toString();
            return names;
        } else if (property instanceof MultiModeProperty) {
            Enum<?>[] values = ((MultiModeProperty<?>) property).getValues();
            String[] names = new String[values.length];
            for (int i = 0; i < values.length; i++) names[i] = values[i].toString();
            return names;
        }
        return new String[0];
    }

    private float getModeLayoutHeight(String[] options) {
        float innerX = module.getX() + PADDING_X;
        float rightX = module.getX() + module.getWidth() - PADDING_X;
        float currentX = innerX;
        float currentY = 15f;
        float itemHeight = 14f;
        CustomFontRenderer font = FontUtils.getFont("sf", 12);

        for (String opt : options) {
            float itemWidth = font.getStringWidth(opt) + 8f;
            if (currentX + itemWidth > rightX && currentX > innerX) {
                currentX = innerX;
                currentY += itemHeight + GAP;
            }
            currentX += itemWidth + GAP;
        }
        return currentY + itemHeight + 3f;
    }

    public int getHeight() {
        if (property instanceof NumberProperty) {
            return 22;
        }
        if (property.getValue() instanceof Boolean) {
            return 18;
        }
        if (property instanceof ModeProperty || property instanceof MultiModeProperty) {
            return (int) Math.ceil(getModeLayoutHeight(getOptions()));
        }
        if (property instanceof DescriptorProperty) {
            DescriptorProperty desc = (DescriptorProperty) property;
            return Math.max(16, desc.getPaddingTop() + desc.getPaddingBottom());
        }
        if (property.getValue() instanceof String) {
            return 26;
        }
        if (property.getValue() instanceof Integer) {
            return 18;
        }
        return 18;
    }

    public float getY() {
        float y = module.getY() + 16f;
        for (PropertyRow row : visibleRows()) {
            if (row == this) break;
            y += row.getHeight() + 2f;
        }
        return y;
    }

    private List<PropertyRow> visibleRows() {
        return module.settings.stream().filter(row -> row.property.isAvailable()).collect(Collectors.toList());
    }

    public void drawScreen(int mouseX, int mouseY, float alpha) {
        float safeAlpha = MathHelper.clamp_float(alpha, 0.0f, 1.0f);
        if (safeAlpha < 0.08f) return;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        float innerX = module.getX() + PADDING_X;
        float innerWidth = module.getWidth() - (PADDING_X * 2f);
        float rightX = innerX + innerWidth;
        float y = getY();

        int argb = MathHelper.clamp_int((int) (255 * safeAlpha), 0, 255);
        Color accent = Theme.accent();

        int barAlpha = MathHelper.clamp_int((int) (Theme.BAR_BG.getAlpha() * safeAlpha), 0, 255);
        Color barBgColor = new Color(Theme.BAR_BG.getRed(), Theme.BAR_BG.getGreen(), Theme.BAR_BG.getBlue(), barAlpha);

        CustomFontRenderer mainFont = FontUtils.getFont("sf", 16);
        CustomFontRenderer subFont = FontUtils.getFont("sf", 13);

        if (property instanceof NumberProperty) {
            NumberProperty number = (NumberProperty) property;
            double percent = MathHelper.clamp_double(
                    (number.getValue() - number.getMin()) / (number.getMax() - number.getMin()), 0.0, 1.0);

            mainFont.drawString(property.getLabel(), innerX, y + 2f, RenderUtils.withAlpha(Theme.TEXT, argb));

            String valStr = formatNumber(number);
            float valWidth = subFont.getStringWidth(valStr);
            subFont.drawString(valStr, rightX - valWidth, y + 3f, RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));

            float trackY = y + 16f;
            float trackH = 4f;

            RoundedUtils.drawCustomRoundedRect(innerX, trackY, innerWidth, trackH, 2f, true, true, true, true, barBgColor);

            if (percent > 0) {
                float progressW = Math.max(trackH, (float) (innerWidth * percent));
                RoundedUtils.drawCustomRoundedRect(innerX, trackY, progressW, trackH, 2f, true, true, true, true,
                        RenderUtils.withAlphaColor(accent, argb));
            }

            if (dragging) {
                double value = number.getMin() + MathHelper.clamp_double((mouseX - innerX) / innerWidth, 0.0, 1.0)
                        * (number.getMax() - number.getMin());
                number.setValue(RenderUtils.incValue(value, number.getIncrement()));
            }
        } else if (property.getValue() instanceof Boolean) {
            boolean enabled = (Boolean) property.getValue();
            toggleAnimation = MathUtils.lerp(toggleAnimation, enabled ? 1f : 0f, 0.25f);

            float rowHeight = getHeight();
            float textY = y + (rowHeight - mainFont.getHeight()) / 2f;
            mainFont.drawString(property.getLabel(), innerX, textY, RenderUtils.withAlpha(Theme.TEXT, argb));

            float boxSize = 12f;
            float boxX = rightX - boxSize;
            float boxY = y + (rowHeight - boxSize) / 2f;

            RoundedUtils.drawCustomRoundedRect(boxX, boxY, boxSize, boxSize, 3f, true, true, true, true, barBgColor);

            if (toggleAnimation > 0.001f) {
                float maxInnerSize = boxSize - 4f;
                float currentInnerSize = maxInnerSize * toggleAnimation;
                float innerBoxX = boxX + 2f + (maxInnerSize - currentInnerSize) / 2f;
                float innerBoxY = boxY + 2f + (maxInnerSize - currentInnerSize) / 2f;
                Color fillColor = RenderUtils.withAlphaColor(accent, MathHelper.clamp_int((int) (255 * safeAlpha * toggleAnimation), 0, 255));
                RoundedUtils.drawCustomRoundedRect(innerBoxX, innerBoxY, currentInnerSize, currentInnerSize, 2f, true, true, true, true, fillColor);
            }
        } else if (property instanceof ModeProperty || property instanceof MultiModeProperty) {
            mainFont.drawString(property.getLabel(), innerX, y + 2f, RenderUtils.withAlpha(Theme.TEXT, argb));

            String[] options = getOptions();
            float currentX = innerX;
            float currentY = y + 15f;
            float itemHeight = 14f;

            for (int i = 0; i < options.length; i++) {
                String opt = options[i];
                float strWidth = subFont.getStringWidth(opt);
                float itemWidth = strWidth + 8f;

                if (currentX + itemWidth > rightX && currentX > innerX) {
                    currentX = innerX;
                    currentY += itemHeight + GAP;
                }

                boolean selected;
                if (property instanceof ModeProperty) {
                    selected = ((ModeProperty<?>) property).getValue().ordinal() == i;
                } else {
                    MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
                    selected = multi.isSelected(multi.getValues()[i]);
                }

                float optTextY = currentY + (itemHeight - subFont.getHeight()) / 2f;

                if (selected) {
                    Color pillBg = RenderUtils.withAlphaColor(accent, MathHelper.clamp_int((int) (200 * safeAlpha), 0, 255));
                    RoundedUtils.drawCustomRoundedRect(currentX, currentY, itemWidth, itemHeight, 3f, true, true, true, true, pillBg);
                    subFont.drawString(opt, currentX + 3f, optTextY, RenderUtils.withAlpha(Theme.TEXT, argb));
                } else {
                    RoundedUtils.drawCustomRoundedRect(currentX, currentY, itemWidth, itemHeight, 3f, true, true, true, true, barBgColor);
                    subFont.drawString(opt, currentX + 3f, optTextY, RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));
                }

                currentX += itemWidth + GAP;
            }
        } else if (property instanceof DescriptorProperty) {
            DescriptorProperty desc = (DescriptorProperty) property;
            subFont.drawString(desc.getLabel(), innerX, y + desc.getPaddingTop(),
                    RenderUtils.withAlpha(Theme.TEXT_MUTED, MathHelper.clamp_int((int) (200 * safeAlpha), 0, 255)));
        } else if (property.getValue() instanceof String) {
            String value = (String) property.getValue();
            if (textHovered && Keyboard.isKeyDown(Keyboard.KEY_BACK) && backspace.hasTimeElapsed(100, true) && !value.isEmpty()) {
                ((Property<String>) property).setValue(value.substring(0, value.length() - 1));
            }

            mainFont.drawString(property.getLabel(), innerX, y + 2f, RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));

            float boxY = y + 14f;
            float boxH = 10f;
            Color boxBg = textHovered ? RenderUtils.withAlphaColor(accent, MathHelper.clamp_int((int) (40 * safeAlpha), 0, 255)) : barBgColor;
            RoundedUtils.drawCustomRoundedRect(innerX, boxY, innerWidth, boxH, 2f, true, true, true, true, boxBg);

            String displayVal = value + (textHovered && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
            float textY = boxY + (boxH - subFont.getHeight()) / 2f;
            subFont.drawString(displayVal, innerX + 3f, textY, RenderUtils.withAlpha(Theme.TEXT, argb));
        } else if (property.getValue() instanceof Integer) {
            float rowHeight = getHeight();
            float textY = y + (rowHeight - mainFont.getHeight()) / 2f;

            mainFont.drawString(property.getLabel(), innerX, textY, RenderUtils.withAlpha(Theme.TEXT, argb));
            String key = listening ? "..." : Keyboard.getKeyName((Integer) property.getValue());
            float keyY = y + (rowHeight - subFont.getHeight()) / 2f;
            subFont.drawString(key, rightX - subFont.getStringWidth(key), keyY, RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));
        }
    }

    private static String formatNumber(NumberProperty number) {
        double rounded = Math.round(number.getValue() * 100.0) / 100.0;
        switch (number.getRepresentation()) {
            case INT:
                return String.valueOf((int) rounded);
            case PERCENTAGE:
                return (int) (rounded * 100) + "%";
            case MILLISECONDS:
                return (int) rounded + "ms";
            case DISTANCE:
                return rounded + "m";
            default:
                return String.valueOf(rounded);
        }
    }

    public boolean isHovered(int mouseX, int mouseY) {
        if (property instanceof DescriptorProperty) return false;
        float innerX = module.getX() + PADDING_X;
        float innerWidth = module.getWidth() - (PADDING_X * 2f);
        float y = getY();
        return mouseX >= innerX && mouseX <= innerX + innerWidth && mouseY >= y && mouseY <= y + getHeight();
    }

    public void mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) {
            if (property.getValue() instanceof String) textHovered = false;
            return;
        }

        if (property instanceof NumberProperty && button == 0) {
            dragging = true;
        } else if (property.getValue() instanceof Boolean && button == 0) {
            Property<Boolean> bool = (Property<Boolean>) property;
            bool.setValue(!bool.getValue());
        } else if ((property instanceof ModeProperty || property instanceof MultiModeProperty) && (button == 0 || button == 1)) {
            float innerX = module.getX() + PADDING_X;
            float rightX = module.getX() + module.getWidth() - PADDING_X;
            float currentX = innerX;
            float currentY = getY() + 15f;
            float itemHeight = 14f;
            CustomFontRenderer optionFont = FontUtils.getFont("sf", 12);

            String[] options = getOptions();
            for (int i = 0; i < options.length; i++) {
                float itemWidth = optionFont.getStringWidth(options[i]) + 8f;
                if (currentX + itemWidth > rightX && currentX > innerX) {
                    currentX = innerX;
                    currentY += itemHeight + GAP;
                }

                if (mouseX >= currentX && mouseX <= currentX + itemWidth && mouseY >= currentY && mouseY <= currentY + itemHeight) {
                    if (property instanceof ModeProperty) {
                        ((ModeProperty<?>) property).setValue(i);
                    } else {
                        MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
                        multi.setValue(i);
                    }
                    break;
                }

                currentX += itemWidth + GAP;
            }
        } else if (property.getValue() instanceof String) {
            textHovered = !textHovered;
        } else if (property.getValue() instanceof Integer && (button == 0 || button == 2)) {
            listening = !listening;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) dragging = false;
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (property.getValue() instanceof String && textHovered) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                textHovered = false;
            } else if (!isIgnoredKey(keyCode)) {
                Property<String> stringProperty = (Property<String>) property;
                stringProperty.setValue(stringProperty.getValue() + typedChar);
            }
        } else if (property.getValue() instanceof Integer && listening) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                listening = false;
            } else {
                ((Property<Integer>) property).setValue(keyCode);
                listening = false;
            }
        }
    }

    public boolean isTextHovered() {
        return textHovered;
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_BACK
                || keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB;
    }
}