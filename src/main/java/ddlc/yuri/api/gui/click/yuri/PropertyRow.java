package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.utils.client.KeyUtil;
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

public class PropertyRow {

    private static final float PADDING_X = 5f;
    private static final float GAP = 2f;
    private static final float ITEM_HEIGHT = 11f;

    public final Property<?> property;
    private final ModuleRow module;
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
        Enum<?>[] values = null;
        if (property instanceof ModeProperty) {
            values = ((ModeProperty<?>) property).getValues();
        } else if (property instanceof MultiModeProperty) {
            values = ((MultiModeProperty<?>) property).getValues();
        }
        if (values == null) return new String[0];
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].toString();
        }
        return names;
    }

    private float getModeLayoutHeight(String[] options) {
        float innerX = module.getX() + PADDING_X;
        float rightX = module.getX() + module.getWidth() - PADDING_X;
        float currentX = innerX;
        float currentY = 10f;
        CustomFontRenderer font = FontUtils.getFont("sf", 12);

        for (String opt : options) {
            float itemWidth = font.getStringWidth(opt) + 5f;
            if (currentX + itemWidth > rightX && currentX > innerX) {
                currentX = innerX;
                currentY += ITEM_HEIGHT + GAP;
            }
            currentX += itemWidth + GAP;
        }
        return currentY + ITEM_HEIGHT + 2f;
    }

    public int getHeight() {
        if (property instanceof NumberProperty) return 16;
        if (property.getValue() instanceof Boolean) return 10;
        if (property.getValue() instanceof Integer) return 9;
        if (property instanceof ModeProperty || property instanceof MultiModeProperty) {
            return (int) Math.ceil(getModeLayoutHeight(getOptions()));
        }
        if (property instanceof DescriptorProperty) {
            DescriptorProperty desc = (DescriptorProperty) property;
            return Math.max(10, desc.getPaddingTop() + desc.getPaddingBottom());
        }
        if (property.getValue() instanceof String) return 22;
        return 12;
    }

    public float getY() {
        float y = module.getY() + 16f;
        for (PropertyRow row : module.settings) {
            if (!row.property.isAvailable()) continue;
            if (row == this) break;
            y += row.getHeight() + 2f;
        }
        return y;
    }

    private static int scaledAlpha(Color base, float safeAlpha) {
        return MathHelper.clamp_int((int) (base.getAlpha() * safeAlpha), 0, 255);
    }

    public void drawScreen(int mouseX, int mouseY, float alpha) {
        float safeAlpha = MathHelper.clamp_float(alpha, 0.0f, 1.0f);
        if (safeAlpha < 0.05f) return;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        float innerX = module.getX() + PADDING_X;
        float innerWidth = module.getWidth() - (PADDING_X * 2f);
        float rightX = innerX + innerWidth;
        float y = getY();

        int argb = MathHelper.clamp_int((int) (255 * safeAlpha), 0, 255);
        Color barBgColor = RenderUtils.withAlphaColor(Theme.BAR_BG, scaledAlpha(Theme.BAR_BG, safeAlpha));
        CustomFontRenderer font = FontUtils.getFont("sf", 12);

        if (property instanceof NumberProperty) {
            NumberProperty number = (NumberProperty) property;
            double percent = MathHelper.clamp_double((number.getValue() - number.getMin()) / (number.getMax() - number.getMin()), 0.0, 1.0);

            font.drawString(property.getLabel(), innerX, y, RenderUtils.withAlpha(Theme.TEXT, argb));

            String valStr = formatNumber(number);
            font.drawString(valStr, rightX - font.getStringWidth(valStr), y, RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));

            float trackY = y + 10f;
            float trackH = 3f;

            float sliderX = innerX + 2f;

            RoundedUtils.drawCustomRoundedRect(sliderX, trackY, innerWidth, trackH, 1f, true, true, true, true,
                    RenderUtils.withAlphaColor(Theme.SLIDER_TRACK, scaledAlpha(Theme.SLIDER_TRACK, safeAlpha)));

            if (percent > 0) {
                float progressW = Math.max(trackH, (float) (innerWidth * percent));
                RoundedUtils.drawCustomRoundedRect(sliderX, trackY, progressW, trackH, 1f, true, true, true, true,
                        RenderUtils.withAlphaColor(Theme.accent().brighter(), argb));
            }

            if (dragging) {
                double value = number.getMin() + MathHelper.clamp_double((mouseX - innerX) / innerWidth, 0.0, 1.0) * (number.getMax() - number.getMin());
                number.setValue(RenderUtils.incValue(value, number.getIncrement()));
            }
        } else if (property.getValue() instanceof Boolean) {
            boolean enabled = (Boolean) property.getValue();
            toggleAnimation = MathUtils.lerp(toggleAnimation, enabled ? 1f : 0f, 0.25f);

            font.drawString(property.getLabel(), innerX, y + 1, RenderUtils.withAlpha(Theme.TEXT, argb));

            float boxSize = 9f;
            float boxX = rightX - boxSize + 2f;
            float boxY = y + (getHeight() - boxSize) / 2f - 1;

            RoundedUtils.drawRoundOutline(boxX, boxY, boxSize, boxSize, 2.5f, -0.5f,
                    barBgColor, RenderUtils.withAlphaColor(Theme.BAR_BORDER, scaledAlpha(Theme.BAR_BORDER, safeAlpha)));

            if (toggleAnimation > 0.01f) {
                float maxInner = boxSize - 3f;
                float currentInner = maxInner * toggleAnimation;
                float innerBoxX = boxX + 1.5f + (maxInner - currentInner) / 2f;
                float innerBoxY = boxY + 1.5f + (maxInner - currentInner) / 2f;
                Color fillColor = RenderUtils.withAlphaColor(Theme.accent().brighter(), MathHelper.clamp_int((int) (255 * safeAlpha * toggleAnimation), 0, 255));
                RoundedUtils.drawCustomRoundedRect(innerBoxX, innerBoxY, currentInner, currentInner, 1f, true, true, true, true, fillColor);
            }
        } else if (property instanceof ModeProperty || property instanceof MultiModeProperty) {
            font.drawString(property.getLabel(), innerX, y, RenderUtils.withAlpha(Theme.TEXT, argb));

            String[] options = getOptions();
            float currentX = innerX;
            float currentY = y + 10f;

            for (int i = 0; i < options.length; i++) {
                String opt = options[i];
                float strWidth = font.getStringWidth(opt);
                float itemWidth = strWidth + 5f;

                if (currentX + itemWidth > rightX && currentX > innerX) {
                    currentX = innerX;
                    currentY += ITEM_HEIGHT + GAP;
                }

                boolean selected = (property instanceof ModeProperty)
                        ? ((ModeProperty<?>) property).getValue().ordinal() == i
                        : ((MultiModeProperty<?>) property).isSelected(((MultiModeProperty<?>) property).getValues()[i]);

                float optTextX = currentX + (itemWidth - strWidth) / 2f - 1f;
                float optTextY = currentY + (ITEM_HEIGHT - font.getHeight()) / 2f;

                Color bg = selected ? RenderUtils.withAlphaColor(Theme.accent().darker(), argb) : barBgColor;
                int textCol = selected ? RenderUtils.withAlpha(Theme.TEXT, argb) : RenderUtils.withAlpha(Theme.TEXT_MUTED, argb);

                RoundedUtils.drawRoundOutline(currentX, currentY, itemWidth, ITEM_HEIGHT, 2f, -0.5f,
                        bg, RenderUtils.withAlphaColor(Theme.BAR_BORDER, scaledAlpha(Theme.BAR_BORDER, safeAlpha)));
                font.drawString(opt, optTextX, optTextY, textCol);

                currentX += itemWidth + GAP;
            }
        } else if (property instanceof DescriptorProperty) {
            DescriptorProperty desc = (DescriptorProperty) property;
            font.drawString(desc.getLabel(), innerX, y + desc.getPaddingTop(),
                    RenderUtils.withAlpha(Theme.TEXT_MUTED, MathHelper.clamp_int((int) (200 * safeAlpha), 0, 255)));
        } else if (property.getValue() instanceof String) {
            String value = (String) property.getValue();
            if (textHovered && Keyboard.isKeyDown(Keyboard.KEY_BACK) && backspace.hasTimeElapsed(100, true) && !value.isEmpty()) {
                ((Property<String>) property).setValue(value.substring(0, value.length() - 1));
            }

            font.drawString(property.getLabel(), innerX, y, RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));

            float boxY = y + 10f;
            float boxH = 11f;
            Color boxBg = textHovered
                    ? RenderUtils.withAlphaColor(Theme.accent().brighter(), MathHelper.clamp_int((int) (40 * safeAlpha), 0, 255))
                    : barBgColor;

            RoundedUtils.drawRoundOutline(innerX, boxY, innerWidth, boxH, 2.5f, -0.5f,
                    boxBg, RenderUtils.withAlphaColor(Theme.BAR_BORDER, scaledAlpha(Theme.BAR_BORDER, safeAlpha)));

            String displayVal = value + (textHovered && (System.currentTimeMillis() % 1000 > 500) ? "_" : "");
            float textY = boxY + (boxH - font.getHeight()) / 2f - 0.5f;
            font.drawString(displayVal, innerX + 3f, textY, RenderUtils.withAlpha(Theme.TEXT, argb));
        } else if (property.getValue() instanceof Integer) {
            font.drawString(property.getLabel(), innerX, y + 0.5f, RenderUtils.withAlpha(Theme.TEXT, argb));
            String key = listening ? "..." : KeyUtil.getKeyName((Integer) property.getValue());
            font.drawString(key, rightX - font.getStringWidth(key), y + 0.5f, RenderUtils.withAlpha(Theme.TEXT_MUTED, argb));
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
            float currentY = getY() + 10f;

            String[] options = getOptions();
            CustomFontRenderer optionFont = FontUtils.getFont("sf", 12);

            for (int i = 0; i < options.length; i++) {
                float itemWidth = optionFont.getStringWidth(options[i]) + 5f;
                if (currentX + itemWidth > rightX && currentX > innerX) {
                    currentX = innerX;
                    currentY += ITEM_HEIGHT + GAP;
                }

                if (mouseX >= currentX && mouseX <= currentX + itemWidth && mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT) {
                    if (property instanceof ModeProperty) {
                        ((ModeProperty<?>) property).setValue(i);
                    } else {
                        ((MultiModeProperty<?>) property).setValue(i);
                    }
                    break;
                }
                currentX += itemWidth + GAP;
            }
        } else if (property.getValue() instanceof String) {
            textHovered = !textHovered;
        } else if (property.getValue() instanceof Integer) {
            if (listening) {
                ((Property<Integer>) property).setValue(KeyUtil.mouseButtonToKeyCode(button));
                listening = false;
            } else if (button == 0 || button == 2) {
                listening = !listening;
            }
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
            ((Property<Integer>) property).setValue(keyCode == Keyboard.KEY_ESCAPE ? 0 : keyCode);
            listening = false;
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