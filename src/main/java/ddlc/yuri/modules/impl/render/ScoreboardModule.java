package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;

@ModuleInfo(label = "Scoreboard", description = "Custom scoreboard rendering", category = ModuleCategory.RENDER)
public class ScoreboardModule extends Module {
    
    public static ModeProperty<Mode> scoreboardStyle = new ModeProperty<>("Scoreboard Style", Mode.LEFT_OFFSET);
    public static Property<Boolean> yuriRect = new Property<Boolean>("Yuri Rect", true);
    public static Property<Boolean> customFont = new Property<Boolean>("Custom Font", true);
    public static Property<Boolean> smartY = new Property<Boolean>("Smart Y", true, () -> scoreboardStyle.getValue() == Mode.VANILLA || scoreboardStyle.getValue() == Mode.VANILLA_OFFSET);
    public static NumberProperty yOffset = new NumberProperty("Y Offset", 20, -150, 150, 1);

    public enum Mode {
        VANILLA("Vanilla"), VANILLA_OFFSET("Vanilla Offset"), LEFT("Left"), LEFT_OFFSET("Left Offset");

        public String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }

    }

    private boolean renderedThisFrame = false;

    @EventHook(EventPriority.VERY_HIGH)
    public void onRender2D(Render2DEvent e) {
        renderedThisFrame = false;
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent e) {
        if (!this.isEnabled()) return;
        if (renderedThisFrame) return;
        if (e.getShaderType() == Shader2DEvent.ShaderType.BLOOM) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) return;

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective scoreobjective = null;
        net.minecraft.scoreboard.ScorePlayerTeam scoreplayerteam = scoreboard.getPlayersTeam(mc.thePlayer.getName());

        if (scoreplayerteam != null) {
            int i1 = scoreplayerteam.getChatFormat().getColorIndex();

            if (i1 >= 0) {
                scoreobjective = scoreboard.getObjectiveInDisplaySlot(3 + i1);
            }
        }

        ScoreObjective scoreobjective1 = scoreobjective != null ? scoreobjective : scoreboard.getObjectiveInDisplaySlot(1);

        if (scoreobjective1 != null) {
            mc.ingameGUI.renderCustomScoreboard(scoreobjective1, new ScaledResolution(mc));
            renderedThisFrame = true;
        }
    }
}