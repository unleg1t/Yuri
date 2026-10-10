package ddlc.yuri.utils.player;

import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;

public class InventoryUtils implements IMinecraft {

    public static TimerUtils timer = new TimerUtils();

    public static void click(int slot, int mouseButton, boolean shiftClick) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, mouseButton, shiftClick ? 1 : 0, mc.thePlayer);
    }

    public static int findItem(final Item item) {
        for (int i = 0; i < 9; i++) {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            if (itemStack == null) {
                if (item == null) {
                    return i;
                }
                continue;
            }

            if (itemStack.getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    public static int getBucketSlot() {
        int item = -1;
        for (int i = 36; i < 45; ++i) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBucket) {
                Item itemSlot = mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
                if (itemSlot == Items.water_bucket) {
                    item = i - 36;
                }
            }
        }
        return item;
    }

    public static int getCobwebSlot() {
        int item = -1;
        for (int i = 36; i < 45; ++i) {
            if (mc.thePlayer.inventoryContainer.getSlot(i).getStack() != null && mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem() instanceof ItemBlock) {
                final ItemBlock block = (ItemBlock) mc.thePlayer.inventoryContainer.getSlot(i).getStack().getItem();
                if (block.getBlock() == Blocks.web) {
                    item = i - 36;
                }
            }
        }
        return item;
    }

    public static float getProtection(ItemStack stack) {
        float prot = 0.0f;
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            prot = (float) ((double) (prot + (float) armor.damageReduceAmount) + (double) ((100 - armor.damageReduceAmount) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack)) * 0.0075);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) / 100.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) / 100.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack) / 100.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 50.0);
            prot = (float) ((double) prot + (double) EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack) / 100.0);
        }
        return prot;
    }

    public static boolean isBestArmor(ItemStack stack, int type) {
        float prot = InventoryUtils.getProtection(stack);
        String strType = "";
        if (type == 1) {
            strType = "helmet";
        } else if (type == 2) {
            strType = "chestplate";
        } else if (type == 3) {
            strType = "leggings";
        } else if (type == 4) {
            strType = "boots";
        }
        if (!stack.getUnlocalizedName().contains(strType)) {
            return false;
        }
        int i = 5;
        while (i < 45) {
            ItemStack is;
            if (Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(i).getHasStack() && InventoryUtils.getProtection(is = Minecraft.getMinecraft().thePlayer.inventoryContainer.getSlot(i).getStack()) > prot && is.getUnlocalizedName().contains(strType)) {
                return false;
            }
            ++i;
        }
        return true;
    }

    public static void drop(int slot) {
        Minecraft.getMinecraft().playerController.windowClick(Minecraft.getMinecraft().thePlayer.inventoryContainer.windowId, slot, 1, 4, Minecraft.getMinecraft().thePlayer);
    }

    public static boolean isBadStackStealer(ItemStack is, boolean preferSword, boolean keepTools) {
        int type = 1;
        while (type < 5) {
            String strType = "";
            if (type == 1) {
                strType = "helmet";
            } else if (type == 2) {
                strType = "chestplate";
            } else if (type == 3) {
                strType = "leggings";
            } else if (type == 4) {
                strType = "boots";
            }
            if (is.getItem() instanceof ItemArmor && !InventoryUtils.isBestArmor(is, type) && is.getUnlocalizedName().contains(strType)) {
                return true;
            }
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getHasStack() && InventoryUtils.isBestArmor(InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack(), type) && InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(4 + type).getStack().getUnlocalizedName().contains(strType) && is.getUnlocalizedName().contains(strType)) {
                return true;
            }
            ++type;
        }
        if (is.getItem() instanceof ItemSword && InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill() && !preferSword) {
            return true;
        }
        if (is.getItem() instanceof ItemSword && InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestSwordSkill() && preferSword) {
            return true;
        }
        if (is.getItem() instanceof ItemBow && InventoryUtils.getBowSkill(is) <= InventoryUtils.bestBowSkill()) {
            return true;
        }
        if (keepTools) {
            if (is.getItem() instanceof ItemAxe && InventoryUtils.getToolSkill(is) <= InventoryUtils.bestAxeSkill() && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            if (is.getItem() instanceof ItemPickaxe && InventoryUtils.getToolSkill(is) <= InventoryUtils.bestPickSkill() && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            return is.getItem() instanceof ItemSpade && InventoryUtils.getToolSkill(is) <= InventoryUtils.bestShovelSkill() && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill());
        } else {
            if (is.getItem() instanceof ItemAxe && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            if (is.getItem() instanceof ItemPickaxe && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill())) {
                return true;
            }
            return is.getItem() instanceof ItemSpade && (preferSword || InventoryUtils.getWeaponSkill(is) <= InventoryUtils.bestWeaponSkill());
        }
    }

    public static float getWeaponSkill(ItemStack is) {
        return InventoryUtils.getItemDamage(is);
    }

    public static float getBowSkill(ItemStack is) {
        return InventoryUtils.getBowDamage(is);
    }

    public static float getToolSkill(ItemStack is) {
        return InventoryUtils.getToolRating(is);
    }

    public static float bestWeaponSkill() {
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && InventoryUtils.getItemDamage(is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()) >= itemDamage) {
                itemDamage = InventoryUtils.getItemDamage(is);
            }
            ++i;
        }
        return itemDamage;
    }

    public static float bestSwordSkill() {
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemSword && InventoryUtils.getItemDamage(is) >= itemDamage) {
                itemDamage = InventoryUtils.getItemDamage(is);
            }
            ++i;
        }
        return itemDamage;
    }

    public static float bestBowSkill() {
        float itemDamage = -1.0f;
        int i = 9;
        while (i < 45) {
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemBow && InventoryUtils.getBowDamage(is) >= itemDamage) {
                itemDamage = InventoryUtils.getBowDamage(is);
            }
            ++i;
        }
        return itemDamage;
    }

    public static float bestAxeSkill() {
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemAxe && InventoryUtils.getToolRating(is) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
            }
            ++i;
        }
        return itemSkill;
    }

    public static float bestPickSkill() {
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemPickaxe && InventoryUtils.getToolRating(is) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
            }
            ++i;
        }
        return itemSkill;
    }

    public static float bestShovelSkill() {
        float itemSkill = -1.0f;
        int i = 9;
        while (i < 45) {
            ItemStack is;
            if (InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getHasStack() && (is = InventoryUtils.mc.thePlayer.inventoryContainer.getSlot(i).getStack()).getItem() instanceof ItemSpade && InventoryUtils.getToolRating(is) >= itemSkill) {
                itemSkill = InventoryUtils.getToolRating(is);
            }
            ++i;
        }
        return itemSkill;
    }

    public static float getToolRating(ItemStack itemStack) {
        float damage = InventoryUtils.getToolMaterialRating(itemStack, false);
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, itemStack) * 2.0f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.silkTouch.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.1f;
        return damage + (float) (itemStack.getMaxDamage() - itemStack.getItemDamage()) * 1.0E-12f;
    }

    public static float getItemDamage(ItemStack itemStack) {
        float damage = InventoryUtils.getToolMaterialRating(itemStack, true);
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack) * 1.25f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.01f;
        damage += (float) (itemStack.getMaxDamage() - itemStack.getItemDamage()) * 1.0E-12f;
        if (itemStack.getItem() instanceof ItemSword) {
            damage = (float) ((double) damage + 0.2);
        }
        return damage;
    }

    public static float getBowDamage(ItemStack itemStack) {
        float damage = 5.0f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, itemStack) * 1.25f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.punch.effectId, itemStack) * 0.75f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, itemStack) * 0.5f;
        damage += (float) EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, itemStack) * 0.1f;
        return damage + ((float) itemStack.getMaxDamage() - (float) itemStack.getItemDamage() * 0.001f);
    }

    public static float getToolMaterialRating(ItemStack itemStack, boolean checkForDamage) {
        float rating;
        block78:
        {
            Item is;
            block81:
            {
                block79:
                {
                    block76:
                    {
                        is = itemStack.getItem();
                        rating = 0.0f;
                        if (!(is instanceof ItemSword)) break block76;
                        switch (((ItemSword) is).getToolMaterialName()) {
                            case "WOOD":
                            case "GOLD": {
                                rating = 4.0f;
                                break;
                            }
                            case "STONE": {
                                rating = 5.0f;
                                break;
                            }
                            case "IRON": {
                                rating = 6.0f;
                                break;
                            }
                            case "EMERALD": {
                                rating = 7.0f;
                            }
                        }
                        break block78;
                    }
                    if (!(is instanceof ItemPickaxe)) break block79;
                    switch (((ItemPickaxe) is).getToolMaterialName()) {
                        case "WOOD":
                        case "GOLD": {
                            rating = 2.0f;
                            break;
                        }
                        case "STONE": {
                            rating = 3.0f;
                            break;
                        }
                        case "IRON": {
                            rating = checkForDamage ? 4 : 40;
                            break;
                        }
                        case "EMERALD": {
                            rating = checkForDamage ? 5 : 50;
                        }
                    }
                    break block78;
                }
                if (!(is instanceof ItemAxe)) break block81;
                switch (((ItemAxe) is).getToolMaterialName()) {
                    case "WOOD":
                    case "GOLD": {
                        rating = 3.0f;
                        break;
                    }
                    case "STONE": {
                        rating = 4.0f;
                        break;
                    }
                    case "IRON": {
                        rating = 5.0f;
                        break;
                    }
                    case "EMERALD": {
                        rating = 6.0f;
                    }
                }
                break block78;
            }
            if (!(is instanceof ItemSpade)) break block78;
            switch (((ItemSpade) is).getToolMaterialName()) {
                case "WOOD":
                case "GOLD": {
                    rating = 1.0f;
                    break;
                }
                case "STONE": {
                    rating = 2.0f;
                    break;
                }
                case "IRON": {
                    rating = 3.0f;
                    break;
                }
                case "EMERALD": {
                    rating = 4.0f;
                }
            }
        }
        return rating;
    }

    public static void swap(int slot, int hSlot) {
        mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, slot, hSlot, 2, mc.thePlayer);
    }

    public static boolean isHoldingSword() {
        final ItemStack stack;
        return (stack = mc.thePlayer.getCurrentEquippedItem()) != null && stack.getItem() instanceof ItemSword;
    }
}