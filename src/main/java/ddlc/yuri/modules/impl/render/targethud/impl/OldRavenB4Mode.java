package ddlc.yuri.modules.impl.render.targethud.impl;

import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.modules.impl.render.TargetHudModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudHelper;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Locale;

public final class OldRavenB4Mode extends TargetHudMode {

    private final TargetHudModule parentModule;

    private static final int MIN_WIDTH = 90;
    private static final int HEIGHT = 30;
    private static final float RADIUS = 6f;
    private static final float OUTLINE_WIDTH = -0.4f;
    private static final float PADDING = 3f;
    private static final float TEXT_Y = 6f;
    private static final float BAR_HEIGHT = 3.5f;
    private static final float BAR_RADIUS = 1.5f;

    private static final Color TEXT_WHITE = new Color(255, 255, 255);
    private static final Color PANEL_BG = new Color(20, 20, 20, 120);
    private static final Color BAR_BG = new Color(30, 30, 30, 180);

    private static final Color HP_GREEN = new Color(96, 243, 96);
    private static final Color HP_YELLOW = new Color(248, 227, 77);
    private static final Color HP_ORANGE = new Color(248, 140, 82);
    private static final Color HP_RED = new Color(255, 73, 73);

    private int lastWidth = MIN_WIDTH;

    public OldRavenB4Mode(TargetHudModule parentModule) {
        super("Old Raven B4");
        this.parentModule = parentModule;
    }

    @Override
    public int getMinWidth() {
        EntityLivingBase current = mc.currentScreen instanceof GuiChat ? mc.thePlayer : AuraModule.target;
        if (current != null) {
            lastWidth = (int) Math.ceil(calculateWidth(current));
        }
        return lastWidth;
    }

    @Override
    public int getHudHeight() { return HEIGHT; }

    @Override
    public int getLabelHeight() { return 0; }

    private float calculateWidth(EntityLivingBase target) {
        if (target == null || mc.thePlayer == null) return MIN_WIDTH;

        FontRenderer fr = mc.fontRendererObj;
        String teamLetter = getTeamInitial(target.getTeam());

        float realHealth = Math.max(0f, target.getHealth());
        String hpFormatted = String.format(Locale.US, "%.1f", realHealth);
        String stateLetter = getWinLossState(target, realHealth);

        float leftWidth = fr.getStringWidth(target.getName());
        if (!teamLetter.isEmpty()) {
            leftWidth += fr.getStringWidth(teamLetter + " ");
        }
        float rightWidth = fr.getStringWidth(" " + hpFormatted + " " + stateLetter);

        return Math.max(MIN_WIDTH, leftWidth + rightWidth + 4f);
    }

    @Override
    public void draw(EntityLivingBase targetEntity, TargetHudModule.TargetState state,
                     double x, double y, long now, float delta) {

        float alpha = state.alpha;
        int maxAlpha = (int) (255f * alpha);
        if (maxAlpha <= 4 || mc.thePlayer == null) return;

        TargetHudHelper.update(state, targetEntity, now, delta);

        FontRenderer fr = mc.fontRendererObj;
        float width = calculateWidth(targetEntity);

        Team team = targetEntity.getTeam();
        String teamLetter = getTeamInitial(team);
        Color teamColor = withAlpha(getTeamColor(team), maxAlpha);

        String name = targetEntity.getName();
        float realHealth = Math.max(0f, targetEntity.getHealth());
        float maxHealth = Math.max(1f, targetEntity.getMaxHealth());
        String hpFormatted = String.format(Locale.US, "%.1f", realHealth);
        String stateLetter = getWinLossState(targetEntity, realHealth);

        Color hpColor = withAlpha(resolveHpColor(realHealth), maxAlpha);
        Color damageColor = withAlpha(hpColor.darker().darker(), maxAlpha);
        Color stateColor = withAlpha(resolveStateColor(targetEntity, realHealth), maxAlpha);

        RoundedUtils.drawRoundOutline((float) x, (float) y, width, HEIGHT, RADIUS, OUTLINE_WIDTH,
                TargetHudHelper.withAlpha(PANEL_BG, alpha),
                TargetHudHelper.withAlpha(ColorManager.getColor(), alpha));

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

        float currentX = PADDING;

        if (!teamLetter.isEmpty()) {
            fr.drawStringWithShadow(teamLetter + " ", currentX, TEXT_Y, teamColor.getRGB());
            currentX += fr.getStringWidth(teamLetter + " ");
        }

        fr.drawStringWithShadow(name, currentX, TEXT_Y, new Color(255, 255, 255, maxAlpha).getRGB());

        String rightText = hpFormatted + " " + stateLetter;
        float rightX = width - fr.getStringWidth(rightText) - PADDING;

        fr.drawStringWithShadow(hpFormatted, rightX, TEXT_Y, hpColor.getRGB());
        fr.drawStringWithShadow(stateLetter, rightX + fr.getStringWidth(hpFormatted + " "), TEXT_Y, stateColor.getRGB());

        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        float barX = PADDING;
        float barWidth = width - PADDING * 2f;
        float barY = fr.FONT_HEIGHT + 10f;

        RoundedUtils.drawCustomRoundedRect(barX, barY, barWidth, BAR_HEIGHT, BAR_RADIUS,
                true, true, true, true, TargetHudHelper.withAlpha(BAR_BG, alpha));

        float damagePercentage = TargetHudHelper.clamp01(state.previousDisplayHealth / maxHealth);
        if (damagePercentage > 0f) {
            float damageWidth = Math.max(BAR_HEIGHT, barWidth * damagePercentage);
            RoundedUtils.drawCustomRoundedRect(barX, barY, damageWidth, BAR_HEIGHT, BAR_RADIUS,
                    true, true, true, true, damageColor);
        }

        float healthPercentage = TargetHudHelper.clamp01(realHealth / maxHealth);
        if (healthPercentage > 0f) {
            float healthWidth = Math.max(BAR_HEIGHT, barWidth * healthPercentage);
            RoundedUtils.drawCustomRoundedRect(barX, barY, healthWidth, BAR_HEIGHT, BAR_RADIUS,
                    true, true, true, true, hpColor);
        }

        GlStateManager.disableBlend();
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    private String getTeamInitial(Team team) {
        if (team == null) return "";
        String teamName = EnumChatFormatting.getTextWithoutFormattingCodes(team.getRegisteredName());
        if (teamName != null && !teamName.trim().isEmpty()) {
            return String.valueOf(teamName.trim().charAt(0)).toUpperCase();
        }
        return "";
    }

    private Color getTeamColor(Team team) {
        if (team instanceof ScorePlayerTeam) {
            String prefix = ((ScorePlayerTeam) team).getColorPrefix();
            if (prefix != null && prefix.length() >= 2) {
                char colorCode = prefix.charAt(1);
                for (EnumChatFormatting formatting : EnumChatFormatting.values()) {
                    String control = formatting.toString();
                    if (control.length() >= 2 && control.charAt(1) == colorCode) {
                        return getRGBFromFormatting(formatting);
                    }
                }
            }
        }
        return TEXT_WHITE;
    }

    private Color getRGBFromFormatting(EnumChatFormatting formatting) {
        switch (formatting) {
            case RED: return new Color(255, 75, 75);
            case BLUE: return new Color(85, 85, 255);
            case GREEN: return new Color(85, 255, 85);
            case YELLOW: return new Color(255, 255, 85);
            case AQUA: return new Color(85, 255, 255);
            case LIGHT_PURPLE: return new Color(255, 85, 255);
            case GRAY: return new Color(170, 170, 170);
            default: return TEXT_WHITE;
        }
    }

    private String getWinLossState(EntityLivingBase target, float targetHealth) {
        if (mc.thePlayer == null || target == mc.thePlayer) return "N";
        float playerHealth = mc.thePlayer.getHealth();

        if (Math.abs(playerHealth - targetHealth) <= 0.25f) return "N";
        return playerHealth > targetHealth ? "W" : "L";
    }

    private Color resolveHpColor(float health) {
        if (health <= 5f) return HP_RED;
        if (health <= 9f) return HP_ORANGE;
        if (health <= 17f) return HP_YELLOW;
        return HP_GREEN;
    }

    private Color resolveStateColor(EntityLivingBase target, float targetHealth) {
        if (mc.thePlayer == null) return HP_YELLOW;

        float playerHealth = mc.thePlayer.getHealth();
        if (Math.abs(playerHealth - targetHealth) <= 0.25f) return HP_YELLOW;

        return playerHealth > targetHealth ? HP_GREEN : HP_RED;
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, Math.max(0, alpha)));
    }
}