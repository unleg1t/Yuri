package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.utils.client.KeyUtil;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

public class SigmaPropertyRow {

    private static final int SLIDER_COLOR = 0xFF3B99FD;

    private final Property<?> property;
    private final SigmaSettingPanel panel;

    private float y;
    private boolean expanded;
    private float expandProgress;
    private boolean listeningKey;
    private boolean focusedText;
    private boolean draggingSlider;
    private float sliderFraction;
    private float checkFraction;

    public SigmaPropertyRow(Property<?> property, SigmaSettingPanel panel) {
        this.property = property;
        this.panel = panel;
        this.checkFraction = property.getValue() instanceof Boolean && (Boolean) property.getValue() ? 1f : 0f;
    }

    public Property<?> getProperty() {
        return property;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getRowY() {
        return y;
    }

    public float getHeight() {
        if (property instanceof MultiModeProperty) {
            return 44f + ((MultiModeProperty<?>) property).getValues().length * 24f * expandProgress;
        }
        if (property instanceof DescriptorProperty) {
            DescriptorProperty descriptor = (DescriptorProperty) property;
            return descriptor.getPaddingTop() + descriptor.getPaddingBottom() + 10f;
        }
        if (property instanceof ModeProperty) {
            return 47f + ((ModeProperty<?>) property).getValues().length * 27f * expandProgress;
        }
        if (property.getValue() instanceof String) {
            return 47f;
        }
        return 44f;
    }

    private float left() {
        return panel.getX() + SigmaTheme.SETTING_X;
    }

    private float right() {
        return panel.getX() + panel.getListWidth() - SigmaTheme.SETTING_PAD;
    }

    public void draw(float mouseX, float mouseY, float alpha) {
        float left = left();
        float right = right();
        float labelHeight = SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 25);
        int labelColor = SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, alpha);

        float expandTarget = expanded ? 1f : 0f;
        expandProgress += (expandTarget - expandProgress) * 0.35f;
        if (Math.abs(expandProgress - expandTarget) < 0.01f) {
            expandProgress = expandTarget;
        }

        if (property instanceof DescriptorProperty) {
            DescriptorProperty descriptor = (DescriptorProperty) property;
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 18, left, y + descriptor.getPaddingTop(), property.getLabel(),
                    SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, alpha * 0.6f));
            return;
        }

        boolean hovered = isHovered(mouseX, mouseY);

        if (property instanceof NumberProperty) {
            NumberProperty number = (NumberProperty) property;
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, left, y + 12f - labelHeight / 2f, property.getLabel(), labelColor);
            drawSlider(right - 126f, y + 6f, 126f, 24f, number, mouseX, mouseY, hovered, alpha);
        } else if (property.getValue() instanceof Boolean) {
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, left, y + 12f - labelHeight / 2f, property.getLabel(), labelColor);
            drawCheckbox(right - 24f, y + 6f, 24f, (Boolean) property.getValue(), hovered,
                    hovered && panel.isMouseDown(), alpha);
        } else if (property instanceof ModeProperty) {
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, left, y + 15.5f - labelHeight / 2f, property.getLabel(), labelColor);
            drawDropdown(right - 123f, y + 5f, 123f, 27f, (ModeProperty<?>) property, mouseX, mouseY, alpha);
        } else if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, left, y + 12f - labelHeight / 2f, property.getLabel(), labelColor);
            String sign = expanded ? "-" : "+";
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, right - SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 25, sign),
                    y + 12f - labelHeight / 2f, sign, SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, alpha));
            if (expandProgress > 0.01f) {
                float optionY = y + 44f;
                for (Enum<?> value : multi.getValues()) {
                    boolean selected = multi.isSelected(value);
                    boolean optionHovered = mouseX >= left + 10f && mouseX <= right && mouseY >= optionY
                            && mouseY <= optionY + 24f;
                    drawCheckbox(right - 24f, optionY, 24f, selected, optionHovered, false, alpha * expandProgress);
                    SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 20, left + 10f,
                            optionY + 12f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 20) / 2f, value.toString(),
                            SigmaTheme.applyAlpha(selected ? SigmaTheme.ENABLED_BLUE.getRGB() : SigmaTheme.DEEP_TEAL.getRGB(),
                                    alpha * expandProgress));
                    optionY += 24f;
                }
            }
        } else if (property.getValue() instanceof String) {
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, left, y + 13.5f - labelHeight / 2f, property.getLabel(), labelColor);
            drawInput(right - 114f, y + 5.75f, 114f, 27f, (String) property.getValue(), alpha);
        } else if (property.getValue() instanceof Integer) {
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, left, y + 12f - labelHeight / 2f, property.getLabel(), labelColor);
            drawKeybind(right - 123f, y + 6f, 123f, 24f, (Integer) property.getValue(), alpha);
        }
    }

    private void drawCheckbox(float x, float y, float size, boolean checked, boolean hovered, boolean mouseDown, float alpha) {
        float target = checked ? 1f : 0f;
        checkFraction += (target - checkFraction) * 0.35f;
        if (Math.abs(checkFraction - target) < 0.01f) {
            checkFraction = target;
        }
        float over = mouseDown ? 0.6f : 0.43f;
        SigmaRenderer.roundRect(x, y, size, size, 10f,
                SigmaTheme.applyAlpha(SigmaTheme.CHECKBOX_GREY, over * (1f - checkFraction) * alpha));
        SigmaRenderer.roundRect(x, y, size, size, 10f,
                SigmaTheme.applyAlpha(SigmaTheme.blend(SigmaTheme.ENABLED_BLUE.getRGB(), SigmaTheme.DEEP_TEAL.getRGB(),
                        mouseDown ? 0.9f : 1f), checkFraction * alpha));
        if (checkFraction > 0.01f) {
            float cx = x + size / 2f;
            float cy = y + size / 2f;
            float scale = 1.5f - 0.5f * checkFraction;
            GL11.glPushMatrix();
            GL11.glTranslatef(SigmaRenderer.s(cx), SigmaRenderer.s(cy), 0f);
            GL11.glScalef(scale, scale, 1f);
            GL11.glTranslatef(-SigmaRenderer.s(cx), -SigmaRenderer.s(cy), 0f);
            SigmaRenderer.image(SigmaTheme.CHECK, x, y, size, size,
                    SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, checkFraction * alpha));
            GL11.glPopMatrix();
        }
    }

    private void drawSlider(float x, float y, float width, float height, NumberProperty number,
                            float mouseX, float mouseY, boolean hovered, float alpha) {
        float knob = height;
        float trackThickness = height / 4f;
        float trackEnd = width - knob / 2f - 3f;
        float trackStart = knob / 4f + 3f;
        float trackY = y + height / 2f - trackThickness / 2f;

        double percent = (number.getValue() - number.getMin()) / (number.getMax() - number.getMin());
        float target = (float) MathHelper.clamp_double(percent, 0.0, 1.0);
        sliderFraction += (target - sliderFraction) * 0.25f;
        sliderFraction = MathHelper.clamp_float(sliderFraction, 0f, 1f);

        float knobX = x + (width - knob) * sliderFraction;
        float center = knobX + knob / 2f - 6f;

        SigmaRenderer.roundRect(x + trackStart, trackY, center, trackThickness, trackThickness / 2f,
                SigmaTheme.applyAlpha(SLIDER_COLOR, alpha));
        SigmaRenderer.roundRect(x + trackStart + center, trackY, trackEnd - center, trackThickness, trackThickness / 2f,
                SigmaTheme.applyAlpha(SigmaTheme.lighter(SLIDER_COLOR, 0.8f), alpha));
        SigmaRenderer.glow(knobX + 5f, y + 5f, knob - 10f, knob - 10f, 10f, alpha * 0.8f);
        SigmaRenderer.filledCircle(knobX + knob / 2f, y + knob / 2f, knob / 2f,
                SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha));

        if (hovered || draggingSlider) {
            String value = formatNumber(number, Math.round(number.getValue() * 100.0) / 100.0);
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 14,
                    x + trackStart - SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 14, value) - 10f, trackY - 5f,
                    value, SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, 0.5f * alpha));
        }

        if (draggingSlider) {
            double difference = number.getMax() - number.getMin();
            double next = number.getMin()
                    + MathHelper.clamp_double((mouseX - x - knob / 2f) / Math.max(1f, width - knob), 0.0, 1.0) * difference;
            number.setValue(RenderUtils.incValue(next, number.getIncrement()));
        }
    }

    private void drawDropdown(float x, float y, float width, float height, ModeProperty<?> mode,
                              float mouseX, float mouseY, float alpha) {
        if (expandProgress > 0.01f) {
            Enum<?>[] values = mode.getValues();
            for (int i = 0; i < values.length; i++) {
                float rowY = y + height + i * height;
                boolean rowHovered = mouseX >= x && mouseX <= x + width && mouseY >= rowY && mouseY <= rowY + height;
                int color = rowHovered ? 0xFFEAEAEA : 0xFFFFFFFF;
                SigmaRenderer.rect(x, rowY, x + width, rowY + height, SigmaTheme.applyAlpha(color, alpha * expandProgress));
                SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 18, x + 10f,
                        rowY + height / 2f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 18) / 2f + 1f,
                        values[i].toString(), SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, alpha * expandProgress));
            }
            SigmaRenderer.roundRect(x, y, width, height + values.length * height - 1f, 6f,
                    SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, alpha * 0.1f * expandProgress));
        }

        String value = String.valueOf(mode.getValue());
        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 18, x + 10f,
                y + height / 2f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 18) / 2f + 1f, value,
                SigmaTheme.applyAlpha(0xFF131313, alpha * 0.7f));
        String arrow = expanded ? "v" : ">";
        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 18, x + width - 16f,
                y + height / 2f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 18) / 2f + 1f, arrow,
                SigmaTheme.applyAlpha(0xFF131313, alpha * 0.7f));
    }

    private void drawInput(float x, float y, float width, float height, String value, float alpha) {
        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 18, x + 4f,
                y + height / 2f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 18) / 2f + 1f, value,
                SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, alpha * (focusedText ? 0.9f : 0.5f)));
        if (focusedText && (System.currentTimeMillis() / 500) % 2 == 0) {
            float cursorX = x + 4f + SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 18, value);
            SigmaRenderer.rect(cursorX, y + height / 2f - 7f, cursorX + 1f, y + height / 2f + 7f,
                    SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, alpha));
        }
        SigmaRenderer.rect(x, y + height - 2f, x + width, y + height,
                SigmaTheme.applyAlpha(SLIDER_COLOR, alpha * (focusedText ? 1f : 0.5f)));
    }

    private void drawKeybind(float x, float y, float width, float height, int key, float alpha) {
        SigmaRenderer.roundRect(x, y, width, height, 6f,
                SigmaTheme.applyAlpha(listeningKey ? SigmaTheme.ENABLED_BLUE.getRGB() : 0x131313,
                        alpha * (listeningKey ? 1f : 0.05f)));
        String name = listeningKey ? "..." : KeyUtil.getKeyName(key);
        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 18,
                x + width / 2f - SigmaRenderer.fontWidth(SigmaTheme.LIGHT_FONT, 18, name) / 2f,
                y + height / 2f - SigmaRenderer.fontHeight(SigmaTheme.LIGHT_FONT, 18) / 2f + 1f, name,
                SigmaTheme.applyAlpha(listeningKey ? SigmaTheme.LIGHT_GREYISH_BLUE.getRGB() : SigmaTheme.DEEP_TEAL.getRGB(), alpha));
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (!isHovered(mouseX, mouseY)) {
            if (focusedText || listeningKey) {
                focusedText = false;
                listeningKey = false;
            }
            return;
        }

        if (property instanceof NumberProperty && mouseButton == 0) {
            draggingSlider = true;
        } else if (property.getValue() instanceof Boolean && mouseButton == 0) {
            ((Property<Boolean>) property).setValue(!(Boolean) property.getValue());
        } else if (property instanceof ModeProperty && (mouseButton == 0 || mouseButton == 1)) {
            ModeProperty<?> mode = (ModeProperty<?>) property;
            float x = right() - 123f;
            float y0 = y + 5f;
            if (expanded && mouseY > y0 + 27f) {
                int index = (int) ((mouseY - y0) / 27f) - 1;
                if (index >= 0 && index < mode.getValues().length) {
                    mode.setValue(index);
                    expanded = false;
                    return;
                }
            }
            if (mouseX >= x && mouseX <= x + 123f && mouseY >= y0 && mouseY <= y0 + 27f) {
                expanded = !expanded;
            } else {
                expanded = false;
            }
        } else if (property instanceof MultiModeProperty && (mouseButton == 0 || mouseButton == 1)) {
            MultiModeProperty<?> multi = (MultiModeProperty<?>) property;
            if (expanded && mouseY > y + 44f) {
                int index = (int) ((mouseY - (y + 44f)) / 24f);
                if (index >= 0 && index < multi.getValues().length) {
                    multi.setValue(index);
                }
            } else {
                expanded = !expanded;
            }
        } else if (property.getValue() instanceof String) {
            focusedText = !focusedText;
        } else if (property.getValue() instanceof Integer) {
            if (listeningKey) {
                ((Property<Integer>) property).setValue(KeyUtil.mouseButtonToKeyCode(mouseButton));
                listeningKey = false;
            } else if (mouseButton == 0 || mouseButton == 2) {
                listeningKey = !listeningKey;
            }
        }
    }

    public void mouseReleased(float mouseX, float mouseY, int state) {
        if (state == 0) {
            draggingSlider = false;
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        if (property.getValue() instanceof String && focusedText) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                focusedText = false;
            } else if (!isIgnoredKey(keyCode)) {
                Property<String> stringProperty = (Property<String>) property;
                stringProperty.setValue(stringProperty.getValue() + typedChar);
            }
        } else if (property.getValue() instanceof Integer && listeningKey) {
            ((Property<Integer>) property).setValue(keyCode);
            listeningKey = false;
        }
    }

    public boolean isTyping() {
        return focusedText || listeningKey;
    }

    private boolean isHovered(float mouseX, float mouseY) {
        if (property instanceof DescriptorProperty) {
            return false;
        }
        return mouseX >= left() && mouseX <= right() && mouseY >= y && mouseY <= y + getHeight();
    }

    private static String formatNumber(NumberProperty number, double value) {
        switch (number.getRepresentation()) {
            case INT:
                return String.valueOf((int) value);
            case PERCENTAGE:
                return (int) (value * 100) + "%";
            case MILLISECONDS:
                return (int) value + "ms";
            case DISTANCE:
                return value + "m";
            default:
                return String.valueOf(value);
        }
    }

    private static boolean isIgnoredKey(int keyCode) {
        return keyCode == Keyboard.KEY_BACK
                || keyCode == Keyboard.KEY_RCONTROL
                || keyCode == Keyboard.KEY_LCONTROL
                || keyCode == Keyboard.KEY_RSHIFT
                || keyCode == Keyboard.KEY_LSHIFT
                || keyCode == Keyboard.KEY_TAB
                || keyCode == Keyboard.KEY_CAPITAL
                || keyCode == Keyboard.KEY_DELETE
                || keyCode == Keyboard.KEY_HOME
                || keyCode == Keyboard.KEY_INSERT
                || keyCode == Keyboard.KEY_UP
                || keyCode == Keyboard.KEY_DOWN
                || keyCode == Keyboard.KEY_RIGHT
                || keyCode == Keyboard.KEY_LEFT
                || keyCode == Keyboard.KEY_LMENU
                || keyCode == Keyboard.KEY_RMENU;
    }
}
