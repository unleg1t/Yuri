package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.utils.client.KeyUtil;
import ddlc.yuri.utils.misc.Timer;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.awt.Color;
import java.io.IOException;
import java.util.stream.Collectors;

public class YuriPropertyEntry {

    public final Property<?> property;
    private final YuriModuleEntry module;
    public boolean opened;
    public float percent;
    private boolean dragging;
    private boolean listening;
    private boolean textHovered;
    private final Timer backSpace = new Timer();

    public YuriPropertyEntry(Property<?> property, YuriModuleEntry module) {
        this.property = property;
        this.module = module;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public void drawScreen(int mouseX, int mouseY, float animationProgress) {
        float y = getY();
        float left = module.panel.getPosX() + YuriTheme.PADDING_X;
        float right = module.panel.getPosX() + YuriTheme.PANEL_WIDTH - YuriTheme.PADDING_X;
        float width = right - left;
        Color accent = YuriTheme.fade(YuriTheme.accent(), animationProgress);
        Color label = YuriTheme.fadeSolid(YuriTheme.TEXT_MUTED, animationProgress);
        Color valueColor = YuriTheme.fadeSolid(YuriTheme.TEXT_SECONDARY, animationProgress);
        double clamp = MathHelper.clamp_double(Minecraft.getMinecraft().getDebugFPS() / 30.0D, 1.0D, 9999.0D);

        if (property instanceof NumberProperty) {
            NumberProperty numberProperty = (NumberProperty) property;
            double rounded = Math.round(numberProperty.getValue() * 100.0D) / 100.0D;
            double percentBar = (numberProperty.getValue() - numberProperty.getMin())
                    / (numberProperty.getMax() - numberProperty.getMin());
            percent = Math.max(0, Math.min(1, (float) (percent + (Math.max(0, Math.min(percentBar, 1)) - percent) * (0.2 / clamp))));

            FontUtils.getFont("sf", 11).drawStringWithShadow(property.getLabel(), left, y + 1f, label.getRGB());
            String value = formatNumber(numberProperty, rounded);
            FontUtils.getFont("sf", 11).drawStringWithShadow(value, right - FontUtils.getFont("sf", 11).getStringWidth(value), y + 1f, valueColor.getRGB());
            RoundedUtils.drawRoundedRect(left, y + 11f, width, 2.2f, 1.1f, YuriTheme.fade(YuriTheme.BAR_BG, animationProgress));
            if (percent > 0.01f) {
                RoundedUtils.drawRoundedRect(left, y + 11f, Math.max(2.2f, width * percent), 2.2f, 1.1f, accent);
            }

            if (dragging) {
                double difference = numberProperty.getMax() - numberProperty.getMin();
                double next = numberProperty.getMin()
                        + MathHelper.clamp_double((mouseX - left) / width, 0.0D, 1.0D) * difference;
                numberProperty.setValue(RenderUtils.incValue(next, numberProperty.getIncrement()));
            }
        } else if (property.getValue() instanceof Boolean) {
            boolean enabled = (Boolean) property.getValue();
            FontUtils.getFont("sf", 11).drawStringWithShadow(property.getLabel(), left, y + 3f, label.getRGB());
            float box = 7f;
            float boxX = right - box;
            float boxY = y + 3.2f;
            RoundedUtils.drawRoundedRect(boxX, boxY, box, box, 2f, enabled ? accent : YuriTheme.fade(YuriTheme.BAR_BG, animationProgress));
            if (enabled) {
                RenderUtils.drawCheck(boxX + 1.5f, boxY + 3.6f, 1.4f, YuriTheme.text(YuriTheme.TEXT_PRIMARY, animationProgress));
            }
        } else if (property instanceof ModeProperty) {
            ModeProperty<?> modeProperty = (ModeProperty<?>) property;
            FontUtils.getFont("sf", 11).drawStringWithShadow(property.getLabel(), left, y + 3f, label.getRGB());
            String value = modeProperty.getValue().toString();
            FontUtils.getFont("sf", 11).drawStringWithShadow(value, right - FontUtils.getFont("sf", 11).getStringWidth(value), y + 3f, valueColor.getRGB());
        } else if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
            FontUtils.getFont("sf", 11).drawStringWithShadow(property.getLabel(), left, y + 3f, label.getRGB());
            FontUtils.getFont("sf", 11).drawStringWithShadow(opened ? "-" : "+", right - 5f, y + 3f, valueColor.getRGB());
            if (opened) {
                float optionY = y + YuriTheme.SETTING_HEIGHT;
                for (Enum<?> value : multiModeProperty.getValues()) {
                    boolean selected = multiModeProperty.isSelected(value);
                    FontUtils.getFont("sf", 11).drawStringWithShadow(value.toString(), left + 4f, optionY + 3f,
                            selected ? YuriTheme.text(YuriTheme.accent(), animationProgress) : label.getRGB());
                    optionY += YuriTheme.SETTING_HEIGHT;
                }
            }
        } else if (property instanceof DescriptorProperty) {
            FontUtils.getFont("sf", 10).drawStringWithShadow(
                    property.getLabel(),
                    left,
                    y + ((DescriptorProperty) property).getPaddingTop(),
                    label.getRGB()
            );
        } else if (property.getValue() instanceof String) {
            String value = (String) property.getValue();
            if (textHovered && Keyboard.isKeyDown(Keyboard.KEY_BACK)
                    && backSpace.hasTimeElapsed(100, true) && !value.isEmpty()) {
                ((Property<String>) property).setValue(value.substring(0, value.length() - 1));
                value = (String) property.getValue();
            }
            FontUtils.getFont("sf", 10).drawStringWithShadow(property.getLabel(), left, y + 1f, label.getRGB());
            String display = value + (textHovered && (System.currentTimeMillis() / 400) % 2 == 0 ? "|" : "");
            FontUtils.getFont("sf", 11).drawStringWithShadow(display, left, y + 10f, valueColor.getRGB());
        } else if (property.getValue() instanceof Integer) {
            FontUtils.getFont("sf", 11).drawStringWithShadow(property.getLabel(), left, y + 3f, label.getRGB());
            String key = listening ? "..." : KeyUtil.getKeyName((Integer) property.getValue());
            FontUtils.getFont("sf", 11).drawStringWithShadow(key, right - FontUtils.getFont("sf", 11).getStringWidth(key), y + 3f, valueColor.getRGB());
        }
    }

    public float getHeight() {
        if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
            return opened ? YuriTheme.SETTING_HEIGHT + multiModeProperty.getValues().length * YuriTheme.SETTING_HEIGHT : YuriTheme.SETTING_HEIGHT;
        }
        if (property instanceof DescriptorProperty) {
            DescriptorProperty descProperty = (DescriptorProperty) property;
            return descProperty.getPaddingTop() + descProperty.getPaddingBottom();
        }
        if (property.getValue() instanceof String) {
            return 20f;
        }
        if (property instanceof NumberProperty) {
            return 17f;
        }
        return YuriTheme.SETTING_HEIGHT;
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (!isHovered(mouseX, mouseY)) {
            if (property.getValue() instanceof String && textHovered) {
                textHovered = false;
            }
            return;
        }

        if (property instanceof NumberProperty && mouseButton == 0) {
            dragging = true;
        } else if (property.getValue() instanceof Boolean && mouseButton == 0) {
            Property<Boolean> booleanProperty = (Property<Boolean>) property;
            booleanProperty.setValue(!booleanProperty.getValue());
        } else if (property instanceof ModeProperty && (mouseButton == 0 || mouseButton == 1)) {
            ModeProperty<?> modeProperty = (ModeProperty<?>) property;
            Enum<?>[] values = modeProperty.getValues();
            int currentIndex = modeProperty.getValue().ordinal();
            int newIndex = ((currentIndex + (mouseButton == 0 ? 1 : -1)) % values.length + values.length) % values.length;
            modeProperty.setValue(newIndex);
        } else if (property instanceof MultiModeProperty && (mouseButton == 0 || mouseButton == 1)) {
            if (opened && mouseY > getY() + YuriTheme.SETTING_HEIGHT) {
                MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
                float optionY = getY() + YuriTheme.SETTING_HEIGHT;
                for (int i = 0; i < multiModeProperty.getValues().length; i++) {
                    if (mouseY >= optionY && mouseY <= optionY + YuriTheme.SETTING_HEIGHT) {
                        multiModeProperty.setValue(i);
                        break;
                    }
                    optionY += YuriTheme.SETTING_HEIGHT;
                }
            } else {
                opened = !opened;
            }
        } else if (property.getValue() instanceof String) {
            textHovered = !textHovered;
        } else if (property.getValue() instanceof Integer) {
            if (listening) {
                ((Property<Integer>) property).setValue(KeyUtil.mouseButtonToKeyCode(mouseButton));
                listening = false;
            } else if (mouseButton == 0 || mouseButton == 2) {
                listening = !listening;
            }
        }
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
            ((Property<Integer>) property).setValue(keyCode);
            listening = false;
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) {
            dragging = false;
        }
    }

    public boolean isTextHovered() {
        return textHovered || listening;
    }

    private float getY() {
        float y = module.y + YuriTheme.MODULE_HEIGHT;
        for (YuriPropertyEntry setting : module.settings.stream()
                .filter(entry -> entry.property.isAvailable())
                .collect(Collectors.toList())) {
            if (setting == this) {
                break;
            }
            y += setting.getHeight();
        }
        return y;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        float y = getY();
        if (property instanceof DescriptorProperty) {
            return false;
        }
        return mouseX >= module.panel.getPosX() + 6 && mouseY >= y
                && mouseX <= module.panel.getPosX() + YuriTheme.PANEL_WIDTH - 6 && mouseY <= y + getHeight();
    }

    private static String formatNumber(NumberProperty numberProperty, double value) {
        switch (numberProperty.getRepresentation()) {
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
