package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

/**
 * Detaches the camera from the body: the mouse turns a camera of its own while your player keeps
 * facing, moving and hitting where it did. Two hooks carry it - {@code Entity#setAngles} hands the
 * mouse over instead of turning the player, and {@code EntityRenderer#setupCameraTransform} swaps
 * these angles in for the length of the camera setup.
 */
@ModuleInfo(label = "Free Look", description = "Look around freely while your body keeps facing forward", category = ModuleCategory.RENDER)
public class FreeLookModule extends Module {

    public final Property<Boolean> holdKey = new Property<>("Hold Key", true);
    public final Property<Boolean> invertPitch = new Property<>("Invert Pitch", false);
    private final NumberProperty sensitivity = new NumberProperty("Sensitivity", 1.0, 0.1, 2.0, 0.1);

    private int previousPerspective;
    private float cameraYaw, cameraPitch;

    private float storedYaw, storedPitch, storedPrevYaw, storedPrevPitch;
    private boolean swapped;

    @Override
    public void onEnable() {
        if (mc.thePlayer == null) {
            setEnabled(false);
            return;
        }

        previousPerspective = mc.gameSettings.thirdPersonView;
        cameraYaw = mc.thePlayer.rotationYaw;
        cameraPitch = mc.thePlayer.rotationPitch;
        swapped = false;
        mc.gameSettings.thirdPersonView = 1;
    }

    @Override
    public void onDisable() {
        swapOut();
        mc.gameSettings.thirdPersonView = previousPerspective;
    }

    @EventHook
    public void onLoadWorld(WorldJoinEvent event) {
        setEnabled(false);
    }

    @EventHook
    public void onClientTick(ClientTickEvent event) {
        if (!holdKey.getValue() || getKey() == Keyboard.KEY_NONE) {
            return;
        }

        if (!Keyboard.isKeyDown(getKey())) {
            setEnabled(false);
        }
    }

    public boolean isActive() {
        return isEnabled() && mc.thePlayer != null;
    }

    /** Fed straight from {@code Entity#setAngles}, so the camera gets exactly the input the body would. */
    private void applyMouse(float yaw, float pitch) {
        float scale = sensitivity.getValue().floatValue();
        cameraYaw += yaw * 0.15f * scale;
        cameraPitch += (invertPitch.getValue() ? pitch : -pitch) * 0.15f * scale;
        cameraPitch = MathHelper.clamp_float(cameraPitch, -90f, 90f);
    }

    private void swapIn() {
        if (swapped || mc.thePlayer == null) {
            return;
        }

        storedYaw = mc.thePlayer.rotationYaw;
        storedPrevYaw = mc.thePlayer.prevRotationYaw;
        storedPitch = mc.thePlayer.rotationPitch;
        storedPrevPitch = mc.thePlayer.prevRotationPitch;

        mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = cameraYaw;
        mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = cameraPitch;
        swapped = true;
    }

    private void swapOut() {
        if (!swapped || mc.thePlayer == null) {
            return;
        }

        mc.thePlayer.rotationYaw = storedYaw;
        mc.thePlayer.prevRotationYaw = storedPrevYaw;
        mc.thePlayer.rotationPitch = storedPitch;
        mc.thePlayer.prevRotationPitch = storedPrevPitch;
        swapped = false;
    }

    private static FreeLookModule get() {
        if (Yuri.INSTANCE == null || Yuri.INSTANCE.getModuleManager() == null) {
            return null;
        }

        FreeLookModule module = Yuri.INSTANCE.getModuleManager().getModule(FreeLookModule.class);
        return module != null && module.isActive() ? module : null;
    }

    /** @return true when the module took the mouse input and the player must not turn with it. */
    public static boolean handleMouse(float yaw, float pitch) {
        FreeLookModule module = get();
        if (module == null) {
            return false;
        }

        module.applyMouse(yaw, pitch);
        return true;
    }

    public static void beginCamera() {
        FreeLookModule module = get();
        if (module != null) {
            module.swapIn();
        }
    }

    public static void endCamera() {
        FreeLookModule module = get();
        if (module != null) {
            module.swapOut();
        }
    }
}
