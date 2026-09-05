package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "Free Look", description = "Allows you to look around freely while moving.", category = ddlc.yuri.modules.ModuleCategory.RENDER)
public class FreeLookModule extends Module {

    public Property<Boolean> invertPitch = new Property<>("Invert Pitch", false);

    private int previousPerspective;
    public float originalYaw, originalPitch, lastYaw, lastPitch;

    @Override
    public void onEnable() {
        previousPerspective = mc.gameSettings.thirdPersonView;
        originalYaw = lastYaw = mc.thePlayer.rotationYaw;
        originalPitch = lastPitch = mc.thePlayer.rotationPitch;

        if (invertPitch.getValue()) lastPitch *= -1;
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void onDisable() {
        mc.thePlayer.rotationYaw = originalYaw;
        mc.thePlayer.rotationPitch = originalPitch;
        mc.gameSettings.thirdPersonView = previousPerspective;
    }


    @EventHook
    public void onLoadWorld(WorldJoinEvent event) {
        this.setEnabled(false);
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (this.getKey() == Keyboard.KEY_NONE || !Keyboard.isKeyDown(this.getKey())) {
            this.setEnabled(false);
            return;
        }

        mc.mouseHelper.mouseXYChange();
        final float f = mc.gameSettings.mouseSensitivity * 0.6F + 0.2F;
        final float f1 = (float) (f * f * f * 1.5);
        lastYaw += mc.mouseHelper.deltaX * f1;
        lastPitch -= mc.mouseHelper.deltaY * f1;

        lastPitch = MathHelper.clamp_float(lastPitch, -90, 90);
        mc.gameSettings.thirdPersonView = 1;
    }
}
