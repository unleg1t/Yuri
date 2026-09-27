package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PostTickEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "Free Look", description = "Allows you to look around freely while moving.", category = ModuleCategory.RENDER)
public class FreeLookModule extends Module {

    public Property<Boolean> invertPitch = new Property<>("Invert Pitch", false);
    public Property<Boolean> hold = new Property<>("Hold", true);

    private int previousPerspective = 0;
    public float cameraYaw, cameraPitch;
    public float prevCameraYaw, prevCameraPitch;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) {
            setEnabled(false);
            return;
        }
        previousPerspective = mc.gameSettings.thirdPersonView;
        cameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = mc.thePlayer.rotationPitch;
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;
        mc.gameSettings.thirdPersonView = 1;
    }

    @Override
    public void onDisable() {
        if (mc.gameSettings != null) {
            mc.gameSettings.thirdPersonView = previousPerspective;
        }
    }

    @EventHook
    public void onLoadWorld(WorldJoinEvent event) {
        this.setEnabled(false);
    }

    @EventHook
    public void onPostTick(PostTickEvent event) {
        if (mc.gameSettings != null && mc.gameSettings.thirdPersonView != 1) {
            mc.gameSettings.thirdPersonView = 1;
        }
        if (mc.currentScreen == null && hold.getValue() && getKey() != Keyboard.KEY_NONE && !Keyboard.isKeyDown(getKey())) {
            this.setEnabled(false);
        }
    }

    public void handleMouseChange(float deltaX, float deltaY) {
        prevCameraYaw = cameraYaw;
        prevCameraPitch = cameraPitch;

        cameraYaw += deltaX * 0.15F;
        float pitchChange = deltaY * 0.15F;
        if (invertPitch.getValue()) {
            cameraPitch = MathHelper.clamp_float(cameraPitch + pitchChange, -90.0F, 90.0F);
        } else {
            cameraPitch = MathHelper.clamp_float(cameraPitch - pitchChange, -90.0F, 90.0F);
        }
    }

    public float getYaw(float partialTicks) {
        return prevCameraYaw + (cameraYaw - prevCameraYaw) * partialTicks;
    }

    public float getPitch(float partialTicks) {
        return prevCameraPitch + (cameraPitch - prevCameraPitch) * partialTicks;
    }
}
