package ddlc.yuri.api.gui.click.novoline;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.utils.client.KeyUtil;
import ddlc.yuri.utils.misc.Timer;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.ScaleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.util.stream.Collectors;

public class PropertyEntry {

    public final Property<?> property;
    private final ModuleEntry module;
    public boolean opened;
    public float percent;
    private boolean dragging;
    private boolean listening;
    private boolean textHovered;
    private final Timer backSpace = new Timer();

    public PropertyEntry(Property<?> property, ModuleEntry module) {
        this.property = property;
        this.module = module;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

    public void drawScreen(int mouseX, int mouseY, float animationProgress) {
        int y = getY();
        int alpha = (int) (255 * animationProgress);
        Color accent = GuiTheme.getAccent();
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        boolean scissor = scaledResolution.getScaleFactor() != 1;
        double clamp = MathHelper.clamp_double(Minecraft.getMinecraft().getDebugFPS() / 30.0D, 1.0D, 9999.0D);

        if (property instanceof NumberProperty) {
            NumberProperty numberProperty = (NumberProperty) property;
            boolean rowClipped = false;
            float rowClipTop = Math.max(module.tab.bodyTop, y);
            float rowClipBottom = Math.min(module.tab.bodyBottom, y + getHeight());
            if (module.yPerModule == module.getTargetHeight() && scissor && rowClipBottom > rowClipTop) {
                GL11.glPushMatrix();
                ScaleUtils.applyScissor(Minecraft.getMinecraft(), module.tab.getPosX() + 1, rowClipTop, module.tab.getPosX() + 99, rowClipBottom);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                rowClipped = true;
            }

            double rounded = Math.round(numberProperty.getValue() * 100.0D) / 100.0D;
            double percentBar = (numberProperty.getValue() - numberProperty.getMin())
                    / (numberProperty.getMax() - numberProperty.getMin());
            percent = Math.max(0, Math.min(1, (float) (percent + (Math.max(0, Math.min(percentBar, 1)) - percent) * (0.2 / clamp))));

            Gui.drawRect(module.tab.getPosX() + 1, y + 3, module.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
            Gui.drawRect(module.tab.getPosX() + 1, y + 3, module.tab.getPosX() + 1 + (int) (98 * percent), y + 14,
                    RenderUtils.withAlpha(accent, alpha));
            FontUtils.getFont("sf", 18).drawStringWithShadow(
                    property.getLabel() + " " + formatNumber(numberProperty, rounded),
                    module.tab.getPosX() + 4,
                    y + 5.5F,
                    new Color(255, 255, 255, alpha).getRGB()
            );

            if (dragging) {
                double difference = numberProperty.getMax() - numberProperty.getMin();
                double value = numberProperty.getMin()
                        + MathHelper.clamp_double((mouseX - (module.tab.getPosX() + 1)) / 99.0D, 0.0D, 1.0D) * difference;
                numberProperty.setValue(RenderUtils.incValue(value, numberProperty.getIncrement()));
            }

            if (rowClipped) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glPopMatrix();
            }
        } else if (property.getValue() instanceof Boolean) {
            Gui.drawRect(module.tab.getPosX() + 89, y + 4, module.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
            if ((Boolean) property.getValue()) {
                RenderUtils.drawCheck(module.tab.getPosX() + 91, y + 8.5F, 2,
                        RenderUtils.withAlpha(accent.brighter(), alpha));
            }
            FontUtils.getFont("sf", 18).drawStringWithShadow(
                    property.getLabel(),
                    module.tab.getPosX() + 4,
                    y + 5.5F,
                    new Color(227, 227, 227, alpha).getRGB()
            );
        } else if (property instanceof ModeProperty) {
            ModeProperty<?> modeProperty = (ModeProperty<?>) property;
            FontUtils.getFont("sf", 18).drawStringWithShadow(
                    property.getLabel(),
                    module.tab.getPosX() + 3,
                    y + 6,
                    new Color(255, 255, 255, alpha).getRGB()
            );
            String value = modeProperty.getValue().toString();
            FontUtils.getFont("sf", 18).drawStringWithShadow(
                    value,
                    module.tab.getPosX() + 97 - FontUtils.getFont("sf", 18).getStringWidth(value),
                    y + 7,
                    new Color(255, 255, 255, alpha).getRGB()
            );
        } else if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
            int dropdownHeight = opened ? multiModeProperty.getValues().length * 17 + 2 : 0;
            Gui.drawRect(module.tab.getPosX() + 1, y + 3, module.tab.getPosX() + 99, y + 14 + dropdownHeight,
                    new Color(0, 0, 0, 50).getRGB());
            FontUtils.getFont("sf", 18).drawCenteredStringWithShadow(
                    property.getLabel(),
                    module.tab.getPosX() + 50,
                    y + 5.5F,
                    Color.WHITE.getRGB()
            );

            if (opened) {
                int optionY = y + 17;
                for (Enum<?> value : multiModeProperty.getValues()) {
                    boolean selected = multiModeProperty.isSelected(value);
                    int rgb = selected ? new Color(255, 255, 255, alpha).getRGB() : new Color(174, 174, 174, 150).getRGB();
                    FontUtils.getFont("sf", 18).drawStringWithShadow(
                            value.toString(),
                            module.tab.getPosX() + 15,
                            optionY + 6,
                            rgb
                    );
                    FontUtils.getFont("sf", 21).drawString(
                            ".",
                            module.tab.getPosX() + 9,
                            optionY,
                            rgb
                    );
                    optionY += 17;
                }
            }
        } else if (property instanceof DescriptorProperty) {
            DescriptorProperty descProperty = (DescriptorProperty) property;
            int startY = y + descProperty.getPaddingTop();

            alpha = (int) (150 * animationProgress);

            FontUtils.getFont("sf", 14).drawStringWithShadow(
                    descProperty.getLabel(),
                    module.tab.getPosX() + 2,
                    startY,
                    new Color(200, 200, 200, alpha).getRGB()
            );
            Gui.drawRect2(module.tab.getPosX() + 2, startY + FontUtils.getFont("sf", 14).getHeight() + 2, FontUtils.getFont("sf", 14).getStringWidth(descProperty.getLabel()), 0.5f, GuiTheme.ACCENT.getRGB());
        } else if (property.getValue() instanceof String) {
            String value = (String) property.getValue();
            if (textHovered && Keyboard.isKeyDown(Keyboard.KEY_BACK)
                    && backSpace.hasTimeElapsed(100, true) && !value.isEmpty()) {
                ((Property<String>) property).setValue(value.substring(0, value.length() - 1));
            }

            Gui.drawRect(module.tab.getPosX() + 6, y + 18, module.tab.getPosX() + 84, y + 18.5F,
                    new Color(195, 195, 195, 220).getRGB());
            FontUtils.getFont("sf", 18).drawString(
                    property.getLabel(),
                    module.tab.getPosX() + 5.5F,
                    y + 1.5F,
                    new Color(227, 227, 227, alpha).getRGB()
            );
            FontUtils.getFont("sf", 18).drawString(
                    value,
                    module.tab.getPosX() + 6,
                    y + 10,
                    new Color(255, 255, 255, alpha).getRGB()
            );
        } else if (property.getValue() instanceof Integer) {
            Gui.drawRect(module.tab.getPosX() + 1, y + 3, module.tab.getPosX() + 99, y + 14, new Color(0, 0, 0, 50).getRGB());
            FontUtils.getFont("sf", 18).drawStringWithShadow(
                    property.getLabel(),
                    module.tab.getPosX() + 3,
                    y + 6,
                    new Color(255, 255, 255, alpha).getRGB()
            );
            String key = "[" + (listening ? ".." : KeyUtil.getKeyName((Integer) property.getValue())) + "]";
            FontUtils.getFont("sf", 18).drawStringWithShadow(
                    key,
                    module.tab.getPosX() + 97.5F - FontUtils.getFont("sf", 18).getStringWidth(key),
                    y + 5.5F,
                    new Color(170, 170, 170, alpha).getRGB()
            );
        }
    }

    public int getHeight() {
        if (property instanceof MultiModeProperty) {
            MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
            return opened ? 17 + multiModeProperty.getValues().length * 17 : 15;
        }
        if (property instanceof DescriptorProperty) {
            DescriptorProperty descProperty = (DescriptorProperty) property;
            return descProperty.getPaddingTop() + descProperty.getPaddingBottom();
        }
        return 15;
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
            if (opened && mouseY > getY() + 15) {
                MultiModeProperty<?> multiModeProperty = (MultiModeProperty<?>) property;
                int optionY = getY() + 17;
                for (int i = 0; i < multiModeProperty.getValues().length; i++) {
                    if (mouseY >= optionY && mouseY <= optionY + 17) {
                        multiModeProperty.setValue(i);
                        break;
                    }
                    optionY += 17;
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
        return textHovered;
    }

    private int getY() {
        int y = module.y + 14;
        for (PropertyEntry setting : module.settings.stream()
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
        int y = getY();
        if (property instanceof DescriptorProperty) {
            return false;
        }
        if (property instanceof NumberProperty) {
            return mouseX >= module.tab.getPosX() + 1 && mouseY >= y + 3
                    && mouseX <= module.tab.getPosX() + 99 && mouseY <= y + 14;
        }
        if (property.getValue() instanceof Boolean) {
            return mouseX >= module.tab.getPosX() + 89 && mouseY >= y + 4
                    && mouseX <= module.tab.getPosX() + 99 && mouseY <= y + 14;
        }
        if (property.getValue() instanceof Integer) {
            String key = "[" + KeyUtil.getKeyName((Integer) property.getValue()) + "]";
            return mouseX >= module.tab.getPosX() + 97.5F - FontUtils.getFont("sf", 18).getStringWidth(key)
                    && mouseX <= module.tab.getPosX() + 97.5F && mouseY >= y + 4 && mouseY <= y + 14;
        }
        return mouseX >= module.tab.getPosX() + 1 && mouseY >= y
                && mouseX <= module.tab.getPosX() + 99 && mouseY <= y + getHeight();
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
