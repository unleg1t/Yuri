package ddlc.yuri.modules.impl.render.targethud;

import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.utils.render.adapters.FontAdapter;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public final class TargetHudHelper {

    private static final float HURT_DURATION_MS = 400f;

    private TargetHudHelper() {
    }

    public static void update(TargetHudModule.TargetState state, EntityLivingBase entity, long now, float delta) {
        float health = entity.isEntityAlive() ? Math.max(0f, entity.getHealth()) : 0f;

        if (state.displayHealth < 0f) state.displayHealth = health;
        if (state.previousDisplayHealth < 0f) state.previousDisplayHealth = health;
        if (state.lastActualHealth < 0f) state.lastActualHealth = health;

        if (health < state.lastActualHealth - 0.01f) {
            state.hurtAnimStart = now;
            state.previousDisplayHealth = Math.max(state.previousDisplayHealth, state.displayHealth);
        }
        state.lastActualHealth = health;

        state.displayHealth += (health - state.displayHealth) * Math.min(1f, delta * 10f);

        if (state.previousDisplayHealth > state.displayHealth) {
            state.previousDisplayHealth += (state.displayHealth - state.previousDisplayHealth) * Math.min(1f, delta * 2.5f);
        } else {
            state.previousDisplayHealth = state.displayHealth;
        }
    }

    public static float hurtProgress(TargetHudModule.TargetState state, long now) {
        long elapsed = now - state.hurtAnimStart;
        return elapsed >= HURT_DURATION_MS ? 1f : Math.max(0f, elapsed / HURT_DURATION_MS);
    }

    public static float tintAmount(float progress) {
        return progress < 1f ? (1f - progress) * 0.7f : 0f;
    }

    public static float pushAmount(float progress) {
        return progress < 1f ? (float) Math.sin(progress * Math.PI) * 0.18f : 0f;
    }

    public static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    public static Color withAlpha(Color color, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (color.getAlpha() * alpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    public static Color lerp(Color from, Color to, float t) {
        float f = clamp01(t);
        return new Color(
                (int) (from.getRed() + (to.getRed() - from.getRed()) * f),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * f),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * f),
                (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * f));
    }

    public static String trim(FontAdapter font, String text, float maxWidth) {
        String result = text;
        while (result.length() > 1 && font.width(result) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static void fillRect(float x, float y, float width, float height, Color color) {
        drawHorizontalGradient(x, y, width, height, color, color);
    }

    public static void drawHorizontalGradient(float x, float y, float width, float height, Color left, Color right) {
        if (width <= 0f || height <= 0f) return;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(left.getRed() / 255f, left.getGreen() / 255f, left.getBlue() / 255f, left.getAlpha() / 255f);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + height);
        GL11.glColor4f(right.getRed() / 255f, right.getGreen() / 255f, right.getBlue() / 255f, right.getAlpha() / 255f);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GlStateManager.enableTexture2D();
        GlStateManager.resetColor();
    }
}