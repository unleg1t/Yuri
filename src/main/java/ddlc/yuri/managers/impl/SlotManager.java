package ddlc.yuri.managers.impl;

import lombok.Getter;
import net.minecraft.item.ItemStack;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public final class SlotManager {

    @Getter
    private static int originalSlot = -1;
    private static ItemStack originalStack;
    @Getter
    private static boolean serverSwapActive;

    public static void swap(int slot, boolean serverSwap) {
        if (mc.thePlayer == null) return;
        if (originalSlot == -1) {
            originalSlot = mc.thePlayer.inventory.currentItem;
            ItemStack current = mc.thePlayer.inventory.getCurrentItem();
            originalStack = current != null ? current.copy() : null;
        }
        mc.thePlayer.inventory.currentItem = slot;
        serverSwapActive = serverSwap;
    }

    public static void swap(int slot, boolean serverSwap, boolean restore) {
        if (mc.thePlayer == null) return;
        if (restore) {
            if (originalSlot == -1) {
                originalSlot = mc.thePlayer.inventory.currentItem;
                ItemStack current = mc.thePlayer.inventory.getCurrentItem();
                originalStack = current != null ? current.copy() : null;
            }
        } else {
            originalSlot = -1;
            originalStack = null;
        }
        mc.thePlayer.inventory.currentItem = slot;
        serverSwapActive = serverSwap;
    }

    public static void swapBack() {
        if (mc.thePlayer == null || originalSlot < 0) return;
        mc.thePlayer.inventory.currentItem = originalSlot;
        originalSlot = -1;
        originalStack = null;
        serverSwapActive = false;
    }

    public static ItemStack getVisualStack() {
        return originalStack;
    }

    public static boolean isActive() {
        return originalSlot >= 0;
    }
}
