package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.SlotManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.AuraModule;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;

@ModuleInfo(label = "Auto Swap", description = "Automatically swaps to the best tool or sword based on your crosshair target.", category = ModuleCategory.MISC)
public final class AutoSwapModule extends Module {

    public enum SwapMode {
        CLIENT("Client"), SERVER("Server");
        public final String name;

        SwapMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Property<Boolean> sneakOnly = new Property<>("Sneak Only", false);
    private final ModeProperty<SwapMode> swapMode = new ModeProperty<>("Swap Mode", SwapMode.CLIENT);

    public static boolean shouldSwap = true;
    private boolean isSwappingState = false;

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!event.isPre()) return;

        if (!shouldStorageSwapValid()) {
            if (isSwappingState) {
                SlotManager.swapBack();
                isSwappingState = false;
            }
            return;
        }

        if (mc.objectMouseOver != null) {

            if (mc.gameSettings.keyBindAttack.isKeyDown() && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos pos = mc.objectMouseOver.getBlockPos();
                if (pos != null) {
                    int itemToUse = getBestToolSlot(pos);
                    if (itemToUse != -1) {
                        if (mc.thePlayer.inventory.currentItem != itemToUse) {
                            SlotManager.swap(itemToUse, swapMode.getValue() == SwapMode.SERVER);
                        }
                        isSwappingState = true;
                        return;
                    }
                }
            }

            else if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled() && AuraModule.target != null && AuraModule.canAttack) {
                int itemToUse = getBestSwordSlot();
                if (itemToUse != -1) {
                    if (mc.thePlayer.inventory.currentItem != itemToUse) {
                        SlotManager.swap(itemToUse, swapMode.getValue() == SwapMode.SERVER);
                    }
                    isSwappingState = true;
                    return;
                }
            }
        }

        if (isSwappingState) {
            SlotManager.swapBack();
            isSwappingState = false;
        }
    }

    private boolean shouldStorageSwapValid() {
        if (mc.thePlayer == null || mc.theWorld == null || !shouldSwap) {
            return false;
        }
        if (sneakOnly.getValue() && !mc.thePlayer.isSneaking()) {
            return false;
        }
        return true;
    }

    @Override
    public void onDisable() {
        SlotManager.swapBack();
        isSwappingState = false;
        super.onDisable();
    }

    private int getBestToolSlot(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();

        float bestStr = 1.0F;
        int itemToUse = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null) continue;

            if (itemStack.getStrVsBlock(block) > bestStr) {
                bestStr = itemStack.getStrVsBlock(block);
                itemToUse = i;
            }
        }

        return itemToUse;
    }

    private int getBestSwordSlot() {
        float bestStr = 0.0F;
        int itemToUse = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.thePlayer.inventory.mainInventory[i];
            if (itemStack == null) continue;

            if (!(itemStack.getItem() instanceof ItemSword)) continue;

            ItemSword item = (ItemSword) itemStack.getItem();
            if (item.attackDamage > bestStr) {
                bestStr = item.attackDamage;
                itemToUse = i;
            }
        }

        return itemToUse;
    }
}