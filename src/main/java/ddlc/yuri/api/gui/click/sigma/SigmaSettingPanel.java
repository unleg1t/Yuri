package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.utils.client.MathUtils;
import ddlc.yuri.utils.render.animations.Direction;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class SigmaSettingPanel {

    private final Module module;
    private final float screenWidth;
    private final float screenHeight;
    private final float width = SigmaTheme.SETTINGS_WIDTH;
    private final float height;
    private final float x;
    private final float y;
    private final float listX;
    private final float listY;
    private final float listWidth = SigmaTheme.SETTINGS_WIDTH - 20f;
    private final float listHeight;

    private final List<SigmaPropertyRow> rows = new CopyOnWriteArrayList<>();
    private final SigmaAnimation animation1 = new SigmaAnimation(200, 120);
    private final SigmaAnimation animation = new SigmaAnimation(240, 200);

    private float scroll;
    private float targetScroll;
    private float currentScale = 1f;
    private boolean closing;
    private boolean finished;
    private boolean mouseDown;
    private String hoverName = "";
    private String hoverDescription = "";

    public SigmaSettingPanel(Module module, float screenWidth, float screenHeight) {
        this.module = module;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.height = Math.min(SigmaTheme.SETTINGS_MAX_HEIGHT, screenHeight * 0.7f);
        this.x = (screenWidth - width) / 2f;
        this.y = (screenHeight - height) / 2f + 20f;
        this.listX = x + 10f;
        this.listY = y + 59f;
        this.listHeight = height - 69f;
        for (Property<?> property : module.getElements()) {
            rows.add(new SigmaPropertyRow(property, this));
        }
    }

    public Module getModule() {
        return module;
    }

    public float getX() {
        return listX;
    }

    public float getListWidth() {
        return listWidth;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public SigmaAnimation getOpenAnimation() {
        return animation;
    }

    public boolean isClosing() {
        return closing;
    }

    public boolean isFinished() {
        return finished;
    }

    public void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        animation1.changeDirection(Direction.BACKWARDS);
        animation.changeDirection(Direction.BACKWARDS);
    }

    public boolean isMouseDownOver(float mouseX, float mouseY) {
        return mouseDown && contains(mouseX, mouseY);
    }

    public boolean isMouseDown() {
        return mouseDown;
    }

    public boolean contains(float mouseX, float mouseY) {
        float[] local = toLocal(mouseX, mouseY);
        return local[0] >= x && local[0] <= x + width && local[1] >= y && local[1] <= y + height;
    }

    public float[] toLocal(float mouseX, float mouseY) {
        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        return new float[]{
                cx + (mouseX - cx) / currentScale,
                cy + (mouseY - cy) / currentScale
        };
    }

    public void update(float mouseX, float mouseY, boolean mouseDown) {
        this.mouseDown = mouseDown;
        if (closing && animation1.calcPercent() <= 0f) {
            finished = true;
        } else {
            finished = false;
        }
    }

    public void draw(float mouseX, float mouseY) {
        float progress = animation1.calcPercent();
        if (progress <= 0f && closing) {
            return;
        }

        float eased = SigmaEasing.easeOutBack(progress, 0f, 1f, 1f);
        if (closing) {
            eased = SigmaEasing.easeOutQuad(progress, 0f, 1f, 1f);
        }
        currentScale = 0.8f + eased * 0.2f;

        SigmaRenderer.rect(0, 0, screenWidth, screenHeight,
                SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, 0.45f * progress));

        float cx = screenWidth / 2f;
        float cy = screenHeight / 2f;
        GL11.glPushMatrix();
        GL11.glTranslatef(SigmaRenderer.s(cx), SigmaRenderer.s(cy), 0f);
        GL11.glScalef(currentScale, currentScale, 1f);
        GL11.glTranslatef(-SigmaRenderer.s(cx), -SigmaRenderer.s(cy), 0f);

        float[] local = toLocal(mouseX, mouseY);

        SigmaRenderer.roundRect(x, y, width, height, SigmaTheme.SETTINGS_RADIUS,
                SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, progress));

        SigmaRenderer.font(SigmaTheme.MEDIUM_FONT, 40, x, y - 60f, module.getLabel(),
                SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, progress));

        String description = module.getDescription();
        if (description != null && !description.isEmpty()) {
            SigmaRenderer.scissor(
                    SigmaRenderer.transformCoord(x, cx, currentScale, 0f),
                    SigmaRenderer.transformCoord(y, cy, currentScale, 0f),
                    SigmaRenderer.transformCoord(x + width - 30f, cx, currentScale, 0f),
                    SigmaRenderer.transformCoord(y + height, cy, currentScale, 0f));
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 20, x + 30f, y + 30f, description,
                    SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, progress * 0.7f));
            SigmaRenderer.endScissor();
        }

        float contentHeight = computeContentHeight();
        float maxScroll = Math.max(0f, contentHeight - listHeight);
        targetScroll = Math.max(0f, Math.min(maxScroll, targetScroll));
        scroll = MathUtils.lerp(scroll, targetScroll, 0.35f);
        if (Math.abs(scroll - targetScroll) < 0.5f) {
            scroll = targetScroll;
        }

        SigmaRenderer.scissor(
                SigmaRenderer.transformCoord(listX, cx, currentScale, 0f),
                SigmaRenderer.transformCoord(listY, cy, currentScale, 0f),
                SigmaRenderer.transformCoord(listX + listWidth, cx, currentScale, 0f),
                SigmaRenderer.transformCoord(listY + listHeight, cy, currentScale, 0f));
        float rowY = listY + SigmaTheme.SETTING_START_Y - scroll;
        for (SigmaPropertyRow row : rows) {
            if (!row.getProperty().isAvailable()) {
                continue;
            }
            row.setY(rowY);
            row.draw(local[0], local[1], progress);
            rowY += row.getHeight();
        }
        SigmaRenderer.endScissor();

        updateHoverDescription(local[0], local[1]);

        GL11.glPopMatrix();

        if (animation.calcPercent() > 0.01f) {
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 14, x + 10f, y + height + 14f, hoverName,
                    SigmaTheme.applyAlpha(SigmaTheme.LIGHT_GREYISH_BLUE, 0.5f * animation.calcPercent()));
        }
    }

    private void updateHoverDescription(float mouseX, float mouseY) {
        for (SigmaPropertyRow row : rows) {
            if (!row.getProperty().isAvailable()) {
                continue;
            }
            if (mouseX >= listX + SigmaTheme.SETTING_X && mouseX <= listX + listWidth - SigmaTheme.SETTING_PAD
                    && mouseY >= row.getRowY() && mouseY <= row.getRowY() + row.getHeight()) {
                hoverName = row.getProperty().getLabel();
                hoverDescription = "";
                animation.changeDirection(Direction.FORWARDS);
                return;
            }
        }
        animation.changeDirection(Direction.BACKWARDS);
    }

    private float computeContentHeight() {
        float total = SigmaTheme.SETTING_START_Y;
        for (SigmaPropertyRow row : rows) {
            if (row.getProperty().isAvailable()) {
                total += row.getHeight();
            }
        }
        return total;
    }

    private List<SigmaPropertyRow> visibleRows() {
        return rows.stream().filter(row -> row.getProperty().isAvailable()).collect(Collectors.toList());
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        float[] local = toLocal(mouseX, mouseY);
        for (SigmaPropertyRow row : visibleRows()) {
            row.mouseClicked(local[0], local[1], mouseButton);
        }
    }

    public void mouseReleased(float mouseX, float mouseY, int state) {
        float[] local = toLocal(mouseX, mouseY);
        for (SigmaPropertyRow row : visibleRows()) {
            row.mouseReleased(local[0], local[1], state);
        }
    }

    public void keyTyped(char typedChar, int keyCode) {
        for (SigmaPropertyRow row : visibleRows()) {
            row.keyTyped(typedChar, keyCode);
        }
    }

    public boolean isTyping() {
        for (SigmaPropertyRow row : visibleRows()) {
            if (row.isTyping()) {
                return true;
            }
        }
        return false;
    }

    public void scroll(float amount) {
        float maxScroll = Math.max(0f, computeContentHeight() - listHeight);
        targetScroll = Math.max(0f, Math.min(maxScroll, targetScroll + amount));
    }
}
