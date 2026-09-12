package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.block.BlockAir;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.BlockPos;
import org.lwjgl.input.Keyboard;

@ModuleInfo(label = "Eagle", description = "Just like scaffold but legit and safe", category = ModuleCategory.PLAYER)
public final class EagleModule extends Module {

    private final NumberProperty delay = new NumberProperty("Delay", 50.0f, 0.0f, 200.0f, 10.0f);
    private final Property<Boolean> blockCheck = new Property<Boolean>("Block Check", true);
    private final Property<Boolean> dirCheck = new Property<Boolean>("Direction Check", false);
    private final Property<Boolean> sneakCheck = new Property<Boolean>("Sneak Check", true);

    private boolean wasOver = false;
    private long lastSneakTime = 0;

    @EventHook
    public void onTick(ClientTickEvent event) {

        if (sneakCheck.getValue() && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            return;
        }

        if (!blockCheck.getValue() || (mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemBlock) && !dirCheck.getValue() || mc.thePlayer.moveForward < 0) {
            if (mc.theWorld.getBlockState(new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ)).getBlock() instanceof BlockAir && mc.thePlayer.onGround) {
                mc.gameSettings.keyBindSneak.pressed = true;
                wasOver = true;

            } else if (mc.thePlayer.onGround) {
                if (wasOver) {
                    lastSneakTime = System.currentTimeMillis();
                }

                long currentTime = System.currentTimeMillis();
                long delayTime = delay.getValue().longValue();
                long randomizedDelay = (long) (delayTime * (Math.random() * 0.1 + 0.95));

                if (currentTime - lastSneakTime >= randomizedDelay) {
                    mc.gameSettings.keyBindSneak.pressed = sneakCheck.getValue() ? false : Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
                }

                wasOver = false;
            }
        } else {
            mc.gameSettings.keyBindSneak.pressed = sneakCheck.getValue() ? false : Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode());
        }
    }
}
