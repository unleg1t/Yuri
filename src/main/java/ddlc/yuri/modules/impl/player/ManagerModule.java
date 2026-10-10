package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.BlockUtils;
import ddlc.yuri.utils.player.InventoryUtils;
import ddlc.yuri.utils.player.MoveUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ModuleInfo(label = "Manager", description = "Automatically manages your inventory.", category = ModuleCategory.PLAYER)
public final class ManagerModule extends Module {

    private final Property<Boolean> instant = new Property<>("Instant", false);
    private final Property<Boolean> invOnly = new Property<>("Inv Only", true);
    private final Property<Boolean> sendInvPackets = new Property<>("Send Inv Packets", true, () -> !invOnly.getValue());
    private final Property<Boolean> notMoving = new Property<>("Not Moving", false);
    private final Property<Boolean> swapBlocks = new Property<>("Swap Blocks", true);
    private final Property<Boolean> dropArchery = new Property<>("Drop Archery", false);
    private final Property<Boolean> moveArrows = new Property<>("Move Arrows", true, () -> !dropArchery.getValue());
    private static final Property<Boolean> dropFood = new Property<>("Drop Food", false);
    private final Property<Boolean> dropShears = new Property<>("Drop Shears", true);

    private static final Property<Boolean> manageWeapon = new Property<>("Manage Weapon", true);
    private static final Property<Boolean> managePick = new Property<>("Manage Pickaxe", true);
    private static final Property<Boolean> manageAxe = new Property<>("Manage Axe", true);
    private static final Property<Boolean> manageShovel = new Property<>("Manage Shovel", true);
    private static final Property<Boolean> manageBow = new Property<>("Manage Bow", true);
    private static final Property<Boolean> manageBlock = new Property<>("Manage Blocks", true);
    private static final Property<Boolean> manageGapple = new Property<>("Manage Gapple", true, () -> !dropFood.getValue());
    private static final Property<Boolean> managePotion = new Property<>("Manage Potion", true);
    private static final Property<Boolean> manageClutch = new Property<>("Manage Clutch", true);
    private static final Property<Boolean> manageProjectile = new Property<>("Manage Projectile", false);

    private static final NumberProperty delay = new NumberProperty("Delay", 120, 0, 300, 10);
    private static final NumberProperty slotWeapon = new NumberProperty("Weapon Slot", 1, 1, 9, 1, () -> manageWeapon.getValue());
    private static final NumberProperty slotPick = new NumberProperty("Pickaxe Slot", 2, 1, 9, 1, () -> managePick.getValue());
    private static final NumberProperty slotAxe = new NumberProperty("Axe Slot", 3, 1, 9, 1, () -> manageAxe.getValue());
    private static final NumberProperty slotShovel = new NumberProperty("Shovel Slot", 4, 1, 9, 1, () -> manageShovel.getValue());
    private static final NumberProperty slotBow = new NumberProperty("Bow Slot", 5, 1, 9, 1, () -> manageBow.getValue());
    private static final NumberProperty slotBlock = new NumberProperty("Block Slot", 6, 1, 9, 1, () -> manageBlock.getValue());
    private static final NumberProperty slotGapple = new NumberProperty("Gapple Slot", 7, 1, 9, 1, () -> manageGapple.getValue());
    private static final NumberProperty slotPotion = new NumberProperty("Potion Slot", 8, 1, 9, 1, () -> managePotion.getValue());
    private static final NumberProperty slotClutch = new NumberProperty("Clutch Slot", 9, 1, 9, 1, () -> manageClutch.getValue());
    private static final NumberProperty slotProjectile = new NumberProperty("Projectile Slot", 9, 1, 9, 1, () -> manageProjectile.getValue());

    private final String[] blacklist = {"tnt", "stick", "egg", "string", "cake", "mushroom", "flint", "compass", "dyePowder", "feather", "bucket", "chest", "snow", "fish", "enchant", "exp", "anvil", "torch", "seeds", "leather", "reeds", "skull", "record", "snowball", "piston"};
    private final String[] serverItems = {"selector", "tracking compass", "(right click)", "tienda ", "perfil", "salir", "shop", "collectibles", "game", "profil", "lobby", "show all", "hub", "friends only", "cofre", "(click", "teleport", "play", "exit", "hide all", "jeux", "gadget", " (activ", "emote", "amis", "bountique", "choisir", "choose ", "recipe book", "click derecho", "todos", "teletransportador", "configuraci", "jugar de nuevo"};
    private final List<Integer> badPotionIDs = new ArrayList<>(Arrays.asList(Potion.moveSlowdown.getId(), Potion.weakness.getId(), Potion.poison.getId(), Potion.harm.getId()));

    private final TimerUtils timer = new TimerUtils();
    public static boolean isInvOpen;

    @EventHook
    public void onMotionEvent(MotionEvent e) {
        setSuffix(instant.getValue() ? "Instant" : delay.getValue().intValue() + "ms");
        if (!e.isPre() || canContinue()) return;
        if (!mc.thePlayer.isUsingItem() && (mc.currentScreen == null || mc.currentScreen instanceof GuiChat || mc.currentScreen instanceof GuiInventory || mc.currentScreen instanceof GuiIngameMenu)) {
            if (manageWeapon.getValue() && isReady()) {
                Slot slot = ItemType.WEAPON.getSlot();
                if (!slot.getHasStack() || !isBestWeapon(slot.getStack())) {
                    getBestWeapon();
                }
            }
            if (managePick.getValue()) getBestPickaxe();
            if (manageAxe.getValue()) getBestAxe();
            if (manageShovel.getValue()) getBestShovel();
            dropItems();
            if (manageBlock.getValue()) swapBlocks();
            if (manageBow.getValue()) getBestBow();
            if (manageGapple.getValue()) moveFood();
            if (managePotion.getValue()) getBestPotion();
            if (manageClutch.getValue()) getBestClutch();
            if (manageProjectile.getValue()) getBestProjectile();
            moveArrows();
        }
    }

    @EventHook
    public void onPacketSendEvent(PacketSendEvent e) {
        if (isInvOpen) {
            Packet<?> packet = e.getPacket();
            if ((packet instanceof C16PacketClientStatus && ((C16PacketClientStatus) packet).getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)
                    || packet instanceof C0DPacketCloseWindow) {
                e.setCancelled(true);
            } else if (packet instanceof C02PacketUseEntity) {
                fakeClose();
            }
        }
    }

    private boolean isReady() {
        return instant.getValue() || timer.hasTimeElapsed(delay.getValue());
    }

    public static float getDamageScore(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;

        float damage = 0;
        Item item = stack.getItem();

        if (item instanceof ItemSword) {
            damage += ((ItemSword) item).getDamageVsEntity();
        } else if (item instanceof ItemTool) {
            damage += item.getMaxDamage();
        }

        damage += EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, stack) * 1.25F +
                EnchantmentHelper.getEnchantmentLevel(Enchantment.fireAspect.effectId, stack) * 0.1F;

        return damage;
    }

    public static float getProtScore(ItemStack stack) {
        float prot = 0;
        if (stack.getItem() instanceof ItemArmor) {
            ItemArmor armor = (ItemArmor) stack.getItem();
            prot += armor.damageReduceAmount + ((100 - armor.damageReduceAmount) * EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, stack)) * 0.0075F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.blastProtection.effectId, stack) / 100F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.fireProtection.effectId, stack) / 100F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.thorns.effectId, stack) / 100F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 25.F;
            prot += EnchantmentHelper.getEnchantmentLevel(Enchantment.featherFalling.effectId, stack) / 100F;
        }
        return prot;
    }

    private void dropItems() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            if (canContinue()) return;
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            ItemStack is = slot.getStack();
            if (is != null && isBadItem(is, i, false)) {
                InventoryUtils.drop(i);
                timer.reset();
                if (!instant.getValue()) break;
            }
        }
    }

    private boolean isBestWeapon(ItemStack is) {
        if (is == null) return false;
        float damage = getDamageScore(is);
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is2 = slot.getStack();
                if (getDamageScore(is2) > damage && is2.getItem() instanceof ItemSword) {
                    return false;
                }
            }
        }
        return is.getItem() instanceof ItemSword;
    }

    private void getBestWeapon() {
        for (int i = 9; i < 45; i++) {
            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (is != null && is.getItem() instanceof ItemSword && isBestWeapon(is) && getDamageScore(is) > 0) {
                swap(i, ItemType.WEAPON.getDesiredSlot() - 36);
                break;
            }
        }
    }

    public boolean isBadItem(ItemStack stack, int slot, boolean stealing) {
        Item item = stack.getItem();
        String stackName = stack.getDisplayName().toLowerCase(), ulName = item.getUnlocalizedName();
        if (Arrays.stream(serverItems).anyMatch(stackName::contains)) return stealing;

        if (item instanceof ItemBlock) {
            return !BlockUtils.isValidBlock(((ItemBlock) item).getBlock(), true);
        }

        if (stealing) {
            if (isBestWeapon(stack) || isBestAxe(stack) || isBestPickaxe(stack) || isBestBow(stack) || isBestShovel(stack) || isGoodPotion(stack) || isClutchItem(stack) || getProjectileScore(stack) > 0) {
                return false;
            }
            if (item instanceof ItemArmor) {
                for (int type = 1; type < 5; type++) {
                    ItemStack is = mc.thePlayer.inventoryContainer.getSlot(type + 4).getStack();
                    if (is != null) {
                        String typeStr = "";
                        switch (type) {
                            case 1: typeStr = "helmet"; break;
                            case 2: typeStr = "chestplate"; break;
                            case 3: typeStr = "leggings"; break;
                            case 4: typeStr = "boots"; break;
                        }
                        if (stack.getUnlocalizedName().contains(typeStr) && getProtScore(is) > getProtScore(stack)) {
                            continue;
                        }
                    }
                    if (isBestArmor(stack, type)) {
                        return false;
                    }
                }
            }
        }

        int weaponSlot = ItemType.WEAPON.getDesiredSlot();
        int pickaxeSlot = ItemType.PICKAXE.getDesiredSlot();
        int axeSlot = ItemType.AXE.getDesiredSlot();
        int shovelSlot = ItemType.SHOVEL.getDesiredSlot();

        if (stealing || (slot != weaponSlot || !isBestWeapon(ItemType.WEAPON.getStackInSlot()))
                && (slot != pickaxeSlot || !isBestPickaxe(ItemType.PICKAXE.getStackInSlot()))
                && (slot != axeSlot || !isBestAxe(ItemType.AXE.getStackInSlot()))
                && (slot != shovelSlot || !isBestShovel(ItemType.SHOVEL.getStackInSlot()))) {

            if (isGoodPotion(stack) || isClutchItem(stack) || isBestProjectile(stack)) {
                return false;
            }

            if (!stealing && item instanceof ItemArmor) {
                for (int type = 1; type < 5; type++) {
                    ItemStack is = mc.thePlayer.inventoryContainer.getSlot(type + 4).getStack();
                    if (is != null && isBestArmor(is, type)) {
                        continue;
                    }
                    if (isBestArmor(stack, type)) {
                        return false;
                    }
                }
            }

            if ((item == Items.wheat) || item == Items.spawn_egg
                    || (item instanceof ItemFood && dropFood.getValue() && !(item instanceof ItemAppleGold))
                    || (item instanceof ItemPotion && isBadPotion(stack))) {
                return true;
            } else if (!(item instanceof ItemSword) && !(item instanceof ItemTool) && !(item instanceof ItemHoe) && !(item instanceof ItemArmor)) {
                if (dropArchery.getValue() && (item instanceof ItemBow || item == Items.arrow)) {
                    return true;
                } else {
                    return (dropShears.getValue() && ulName.contains("shears")) || item instanceof ItemGlassBottle || Arrays.stream(blacklist).anyMatch(ulName::contains);
                }
            }
            return true;
        }

        return false;
    }

    private void getBestPickaxe() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isBestPickaxe(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.PICKAXE.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestPickaxe(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        if (!instant.getValue()) break;
                    }
                }
            }
        }
    }

    private void getBestAxe() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isBestAxe(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.AXE.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestAxe(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        if (!instant.getValue()) break;
                    }
                }
            }
        }
    }

    private void getBestShovel() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isBestShovel(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.SHOVEL.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestShovel(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        if (!instant.getValue()) break;
                    }
                }
            }
        }
    }

    private void getBestBow() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                String stackName = is.getDisplayName().toLowerCase();
                if (Arrays.stream(serverItems).anyMatch(stackName::contains) || !(is.getItem() instanceof ItemBow))
                    continue;
                if (isBestBow(is) && !isBestWeapon(is)) {
                    int desiredSlot = ItemType.BOW.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isBestBow(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        if (!instant.getValue()) break;
                    }
                }
            }
        }
    }

    private void getBestPotion() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isGoodPotion(is)) {
                    int desiredSlot = ItemType.POTION.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isGoodPotion(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        if (!instant.getValue()) break;
                    }
                }
            }
        }
    }

    private void getBestClutch() {
        if (!isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (isClutchItem(is)) {
                    int desiredSlot = ItemType.CLUTCH.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !isClutchItem(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        if (!instant.getValue()) break;
                    }
                }
            }
        }
    }

    private void getBestProjectile() {
        if (!isReady()) return;
        int bestSlot = -1;
        float bestScore = 0;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                float score = getProjectileScore(is);
                if (score > bestScore) {
                    bestScore = score;
                    bestSlot = i;
                }
            }
        }
        if (bestSlot != -1) {
            int desiredSlot = ItemType.PROJECTILE.getDesiredSlot();
            if (bestSlot == desiredSlot) return;
            Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
            if (!slot2.getHasStack() || getProjectileScore(slot2.getStack()) < bestScore) {
                swap(bestSlot, desiredSlot - 36);
            }
        }
    }

    private float getProjectileScore(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return 0;
        Item item = stack.getItem();
        if (item == Items.snowball || item == Items.egg) {
            return 2.0F;
        } else if (item == Items.fishing_rod) {
            return 1.0F;
        }
        return 0;
    }

    private boolean isBestProjectile(ItemStack stack) {
        return getProjectileScore(stack) > 0;
    }

    private boolean isGoodPotion(ItemStack stack) {
        return stack != null && stack.getItem() instanceof ItemPotion && !isBadPotion(stack);
    }

    private boolean isClutchItem(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        Item item = stack.getItem();
        return item == Items.water_bucket || item == Item.getItemFromBlock(Blocks.web);
    }

    private void moveArrows() {
        if (dropArchery.getValue() || !moveArrows.getValue() || !isReady()) return;
        for (int i = 36; i < 45; i++) {
            ItemStack is = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (is != null && is.getItem() == Items.arrow) {
                for (int j = 0; j < 36; j++) {
                    if (mc.thePlayer.inventoryContainer.getSlot(j).getStack() == null) {
                        fakeOpen();
                        InventoryUtils.click(i, 0, true);
                        fakeClose();
                        timer.reset();
                        if (!instant.getValue()) return;
                        break;
                    }
                }
            }
        }
    }

    private void moveFood() {
        if (dropFood.getValue() || !isReady()) return;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            if (slot.getHasStack()) {
                ItemStack is = slot.getStack();
                if (hasMostGapples(is)) {
                    int desiredSlot = ItemType.GAPPLE.getDesiredSlot();
                    if (i == desiredSlot) return;
                    Slot slot2 = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
                    if (!slot2.getHasStack() || !hasMostGapples(slot2.getStack())) {
                        swap(i, desiredSlot - 36);
                        if (!instant.getValue()) break;
                    }
                }
            }
        }
    }

    private boolean hasMostGapples(ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof ItemAppleGold)) {
            return false;
        } else {
            int value = stack.stackSize;
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getItem() instanceof ItemAppleGold && is.stackSize > value) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private int getMostBlocks() {
        int stack = 0;
        int biggestSlot = -1;
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
            ItemStack is = slot.getStack();
            if (is != null && is.getItem() instanceof ItemBlock && is.stackSize > stack && Arrays.stream(serverItems).noneMatch(is.getDisplayName().toLowerCase()::contains)) {
                stack = is.stackSize;
                biggestSlot = i;
            }
        }
        return biggestSlot;
    }

    private void swapBlocks() {
        if (!swapBlocks.getValue() || !isReady()) return;
        int mostBlocksSlot = getMostBlocks();
        int desiredSlot = ItemType.BLOCK.getDesiredSlot();
        if (mostBlocksSlot != -1 && mostBlocksSlot != desiredSlot) {
            Slot dss = mc.thePlayer.inventoryContainer.getSlot(desiredSlot);
            ItemStack dsis = dss.getStack();
            if (!(dsis != null && dsis.getItem() instanceof ItemBlock && dsis.stackSize >= mc.thePlayer.inventoryContainer.getSlot(mostBlocksSlot).getStack().stackSize && Arrays.stream(serverItems).noneMatch(dsis.getDisplayName().toLowerCase()::contains))) {
                swap(mostBlocksSlot, desiredSlot - 36);
            }
        }
    }

    private boolean isBestPickaxe(ItemStack stack) {
        if (stack == null) return false;
        Item item = stack.getItem();
        if (!(item instanceof ItemPickaxe)) {
            return false;
        } else {
            float value = getToolScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getItem() instanceof ItemPickaxe && getToolScore(is) > value) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private boolean isBestShovel(ItemStack stack) {
        if (stack == null) return false;
        if (!(stack.getItem() instanceof ItemSpade)) {
            return false;
        } else {
            float score = getToolScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getItem() instanceof ItemSpade && getToolScore(is) > score) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private boolean isBestAxe(ItemStack stack) {
        if (stack == null) return false;
        if (!(stack.getItem() instanceof ItemAxe)) {
            return false;
        } else {
            float value = getToolScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (getToolScore(is) > value && is.getItem() instanceof ItemAxe && !isBestWeapon(is)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private boolean isBestBow(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemBow)) {
            return false;
        } else {
            float value = getBowScore(stack);
            for (int i = 9; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (getBowScore(is) > value && is.getItem() instanceof ItemBow && !isBestWeapon(stack)) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private float getBowScore(ItemStack stack) {
        float score = 0;
        Item item = stack.getItem();
        if (item instanceof ItemBow) {
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.power.effectId, stack);
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.flame.effectId, stack) * 0.5F;
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) * 0.1F;
        }
        return score;
    }

    private float getToolScore(ItemStack stack) {
        float score = 0;
        Item item = stack.getItem();
        if (item instanceof ItemTool) {
            ItemTool tool = (ItemTool) item;
            String name = item.getUnlocalizedName().toLowerCase();
            if (item instanceof ItemPickaxe) {
                score = tool.getStrVsBlock(stack, Blocks.stone) - (name.contains("gold") ? 5 : 0);
            } else if (item instanceof ItemSpade) {
                score = tool.getStrVsBlock(stack, Blocks.dirt) - (name.contains("gold") ? 5 : 0);
            } else {
                if (!(item instanceof ItemAxe)) return 1;
                score = tool.getStrVsBlock(stack, Blocks.log) - (name.contains("gold") ? 5 : 0);
            }
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack) * 0.0075F;
            score += EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, stack) / 100F;
        }
        return score;
    }

    private boolean isBadPotion(ItemStack stack) {
        if (stack != null && stack.getItem() instanceof ItemPotion) {
            List<PotionEffect> effects = ((ItemPotion) stack.getItem()).getEffects(stack);
            if (effects != null) {
                for (PotionEffect effect : effects) {
                    if (badPotionIDs.contains(effect.getPotionID())) {
                        return true;
                    }
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isBestArmor(ItemStack stack, int type) {
        String typeStr = "";
        switch (type) {
            case 1: typeStr = "helmet"; break;
            case 2: typeStr = "chestplate"; break;
            case 3: typeStr = "leggings"; break;
            case 4: typeStr = "boots"; break;
        }
        if (stack.getUnlocalizedName().contains(typeStr)) {
            float prot = getProtScore(stack);
            for (int i = 5; i < 45; i++) {
                Slot slot = mc.thePlayer.inventoryContainer.getSlot(i);
                if (slot.getHasStack()) {
                    ItemStack is = slot.getStack();
                    if (is.getUnlocalizedName().contains(typeStr) && getProtScore(is) > prot) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void fakeOpen() {
        if (!isInvOpen) {
            timer.reset();
            if (!invOnly.getValue() && sendInvPackets.getValue())
                PacketUtils.sendSilentPacket(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            isInvOpen = true;
        }
    }

    private void fakeClose() {
        if (isInvOpen) {
            if (!invOnly.getValue() && sendInvPackets.getValue())
                PacketUtils.sendSilentPacket(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            isInvOpen = false;
        }
    }

    private void swap(int slot, int hSlot) {
        fakeOpen();
        InventoryUtils.swap(slot, hSlot);
        fakeClose();
        timer.reset();
    }

    private boolean canContinue() {
        return (invOnly.getValue() && !(mc.currentScreen instanceof GuiInventory)) || (notMoving.getValue() && MoveUtils.isMoving());
    }

    @Getter
    @AllArgsConstructor
    private enum ItemType {
        WEAPON(slotWeapon),
        PICKAXE(slotPick),
        AXE(slotAxe),
        SHOVEL(slotShovel),
        BLOCK(slotBlock),
        BOW(slotBow),
        GAPPLE(slotGapple),
        POTION(slotPotion),
        CLUTCH(slotClutch),
        PROJECTILE(slotProjectile);

        private final NumberProperty setting;

        public int getDesiredSlot() {
            return setting.getValue().intValue() + 35;
        }

        public Slot getSlot() {
            return mc.thePlayer.inventoryContainer.getSlot(getDesiredSlot());
        }

        public ItemStack getStackInSlot() {
            return getSlot().getStack();
        }
    }
}