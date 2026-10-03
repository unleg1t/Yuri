package ddlc.yuri.api.gui.click.sigma;

import ddlc.yuri.api.config.Config;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.utils.render.animations.Direction;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SigmaConfigPanel {

    private static final float WIDTH = 250f;
    private static final float HEIGHT = 500f;
    private static final int CONFIG_COLOR = 0xFFF4F4F4;

    private final float x;
    private final float y;
    private final List<String> names = new CopyOnWriteArrayList<>();
    private final SigmaAnimation animation = new SigmaAnimation(300, 100);

    private String selected;
    private float scroll;
    private float targetScroll;
    private boolean closing;
    private boolean finished;
    private float currentScale = 1f;

    public SigmaConfigPanel(float screenWidth, float screenHeight) {
        this.x = screenWidth - 14f - WIDTH;
        this.y = screenHeight - 14f - HEIGHT;
        refresh();
    }

    public void refresh() {
        names.clear();
        for (Config config : ConfigManager.getInstance().getElements()) {
            names.add(config.getName());
        }
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
        animation.changeDirection(Direction.BACKWARDS);
    }

    public void update() {
        finished = closing && animation.calcPercent() <= 0f;
    }

    public void draw(float mouseX, float mouseY) {
        float progress = animation.calcPercent();
        if (progress <= 0f && closing) {
            return;
        }

        float eased = SigmaEasing.easeOutQuad(progress, 0f, 1f, 1f);
        currentScale = 0.8f + eased * 0.2f;
        float cx = x + WIDTH / 2f;
        float cy = y + HEIGHT / 2f;
        org.lwjgl.opengl.GL11.glPushMatrix();
        org.lwjgl.opengl.GL11.glTranslatef(SigmaRenderer.s(cx), SigmaRenderer.s(cy), 0f);
        org.lwjgl.opengl.GL11.glScalef(currentScale, currentScale, 1f);
        org.lwjgl.opengl.GL11.glTranslatef(-SigmaRenderer.s(cx), -SigmaRenderer.s(cy), 0f);

        float[] local = toLocal(mouseX, mouseY);

        SigmaRenderer.glow(x + 5f, y + 5f, WIDTH - 10f, HEIGHT - 10f, 35f, progress);
        SigmaRenderer.rect(x + 5f, y + 5f, x + WIDTH - 5f, y + HEIGHT - 5f,
                SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, progress * 0.25f));
        SigmaRenderer.roundRect(x, y, WIDTH, HEIGHT, 10f,
                SigmaTheme.applyAlpha(CONFIG_COLOR, SigmaEasing.easeOutQuad(progress, 0f, 1f, 1f)));

        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, x + 25f, y + 20f, "Profiles",
                SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, 0.8f * progress));
        SigmaRenderer.rect(x + 25f, y + 69f, x + WIDTH - 25f, y + 70f,
                SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, 0.05f * progress));

        SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 25, x + WIDTH - 40f, y + 20f, "+",
                SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, 0.8f * progress));

        float listTop = y + 80f;
        float listBottom = y + HEIGHT - 10f;
        float visible = listBottom - listTop;
        float content = names.size() * 70f;
        float maxScroll = Math.max(0f, content - visible);
        targetScroll = Math.max(0f, Math.min(maxScroll, targetScroll));
        scroll += (targetScroll - scroll) * 0.3f;
        if (Math.abs(scroll - targetScroll) < 0.5f) {
            scroll = targetScroll;
        }

        SigmaRenderer.scissor(
                SigmaRenderer.transformCoord(x + 10f, cx, currentScale, 0f),
                SigmaRenderer.transformCoord(listTop, cy, currentScale, 0f),
                SigmaRenderer.transformCoord(x + WIDTH - 10f, cx, currentScale, 0f),
                SigmaRenderer.transformCoord(listBottom, cy, currentScale, 0f));
        float rowY = listTop - scroll;
        for (String name : names) {
            boolean hovered = local[0] >= x + 12f && local[0] <= x + WIDTH - 12f && local[1] >= rowY
                    && local[1] <= rowY + 60f;
            if (hovered || name.equals(selected)) {
                SigmaRenderer.roundRect(x + 12f, rowY + 5f, WIDTH - 24f, 50f, 8f,
                        SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, progress * (name.equals(selected) ? 0.08f : 0.05f)));
            }
            SigmaRenderer.font(SigmaTheme.LIGHT_FONT, 24, x + 26f, rowY + 16f, name,
                    SigmaTheme.applyAlpha(SigmaTheme.DEEP_TEAL, progress));
            rowY += 70f;
        }
        SigmaRenderer.endScissor();

        org.lwjgl.opengl.GL11.glPopMatrix();
    }

    public float[] toLocal(float mouseX, float mouseY) {
        float cx = x + WIDTH / 2f;
        float cy = y + HEIGHT / 2f;
        return new float[]{
                cx + (mouseX - cx) / currentScale,
                cy + (mouseY - cy) / currentScale
        };
    }

    public void mouseClicked(float mouseX, float mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return;
        }
        float[] local = toLocal(mouseX, mouseY);
        if (local[0] >= x + WIDTH - 55f && local[0] <= x + WIDTH - 10f && local[1] >= y + 10f && local[1] <= y + 60f) {
            String name = "New Profile";
            int index = 1;
            while (containsName(name + " " + index)) {
                index++;
            }
            ConfigManager.getInstance().saveConfig(name + " " + index);
            refresh();
            return;
        }

        float listTop = y + 80f;
        float listBottom = y + HEIGHT - 10f;
        if (local[0] >= x + 10f && local[0] <= x + WIDTH - 10f && local[1] >= listTop && local[1] <= listBottom) {
            float rowY = listTop - scroll;
            for (String name : names) {
                if (local[1] >= rowY && local[1] <= rowY + 70f) {
                    ConfigManager.getInstance().loadConfig(name);
                    selected = name;
                    return;
                }
                rowY += 70f;
            }
        }
    }

    private boolean containsName(String name) {
        for (String existing : names) {
            if (existing.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public void scroll(float amount) {
        float visible = (y + HEIGHT - 10f) - (y + 80f);
        float maxScroll = Math.max(0f, names.size() * 70f - visible);
        targetScroll = Math.max(0f, Math.min(maxScroll, targetScroll + amount));
    }

    public boolean isMouseOver(float mouseX, float mouseY) {
        float[] local = toLocal(mouseX, mouseY);
        return local[0] >= x && local[0] <= x + WIDTH && local[1] >= y && local[1] <= y + HEIGHT;
    }
}
