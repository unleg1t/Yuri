package ddlc.yuri.modules.impl.player;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.client.TimerUtils;
import ddlc.yuri.utils.player.InvUtils;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockTNT;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;

@ModuleInfo(
        label = "Stealer",
        description = "Automatically steals chest loot",
        category = ModuleCategory.PLAYER)
public final class StealerModule extends Module {

    public final TimerUtils timer = new TimerUtils();
    public final TimerUtils startTimer = new TimerUtils();
    public final TimerUtils missClickTimer = new TimerUtils();

    private final Property<Boolean> instant = new Property<>("Instant", false);
    private final NumberProperty stealDelay = new NumberProperty("Steal Delay", 50.0, 0.0, 1000.0, 25.0, () -> !instant.getValue());
    private final NumberProperty minDelay = new NumberProperty("Min Delay", 5.0, 0.0, 1000.0, 25.0, () -> !instant.getValue());
    private final NumberProperty maxDelay = new NumberProperty("Max Delay", 5.0, 0.0, 1000.0, 25.0, () -> !instant.getValue());
    private final Property<Boolean> stealTrashItems = new Property<>("Steal Trash Items", false);
    private final Property<Boolean> autoClose = new Property<>("Auto Close", true);
    private final Property<Boolean> grabMouse = new Property<>("Grab Mouse", false);
    public static final Property<Boolean> autoDisable = new Property<>("Auto Disable", false);
    private final Property<Boolean> distanceScaling = new Property<>("Distance Scaling", false);
    private final NumberProperty distanceMultiplier = new NumberProperty("Distance Multiplier", 15.0, 0.0, 100.0, 5.0, distanceScaling::getValue);
    private final Property<Boolean> positionScaling = new Property<>("Position Scaling", false);
    private final NumberProperty positionMultiplier = new NumberProperty("Position Multiplier", 5.0, 0.0, 50.0, 5.0, positionScaling::getValue);
    private final Property<Boolean> missClickEnabled = new Property<>("Miss Click", false);
    private final NumberProperty missClickChance = new NumberProperty("Miss Click Chance", 15.0, 0.0, 100.0, 5.0, missClickEnabled::getValue);
    private final NumberProperty missClickRecoveryMin = new NumberProperty("Miss Recovery Min", 80.0, 0.0, 1000.0, 25.0, missClickEnabled::getValue);
    private final NumberProperty missClickRecoveryMax = new NumberProperty("Miss Recovery Max", 220.0, 0.0, 1000.0, 25.0, missClickEnabled::getValue);

    private int decidedTimer = 0;
    private boolean gotItems;
    private int ticksInChest;
    private boolean lastInChest;

    private int targetSlot = -1;
    private boolean missClickPending;
    private boolean missClickDone;
    private int missClickRecovery;

    private boolean isValidChest() {
        if (!(mc.currentScreen instanceof GuiChest)) {
            return false;
        }
        GuiChest guiChest = (GuiChest) mc.currentScreen;
        if (guiChest.lowerChestInventory == null) {
            return false;
        }

        String name = guiChest.lowerChestInventory.getDisplayName().getUnformattedText().toLowerCase();
        String[] menuKeywords = {"menu", "selector", "game", "shop", "server", "teleport", "lobby", "hub", "profile", "setting", "play", "vault", "collectible", "bountique", "choisir", "choose", "recipe"};
        for (String keyword : menuKeywords) {
            if (name.contains(keyword)) {
                return false;
            }
        }
        return mc.thePlayer.openContainer instanceof ContainerChest;
    }

    private double getChestDistance(ContainerChest chest) {
        if (mc.thePlayer == null) {
            return 0;
        }
        if (!(chest.getLowerChestInventory() instanceof TileEntity)) {
            return 0;
        }
        TileEntity te = (TileEntity) chest.getLowerChestInventory();
        BlockPos pos = te.getPos();
        return mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private int computeDynamicDelay(int baseDelay, ContainerChest chest, int slotIndex) {
        int delay = baseDelay;
        if (this.distanceScaling.getValue()) {
            double distance = getChestDistance(chest);
            delay += (int) Math.round(distance * this.distanceMultiplier.getValue());
        }
        if (this.positionScaling.getValue()) {
            delay += (int) Math.round(slotIndex * this.positionMultiplier.getValue());
        }
        return delay;
    }

    private int findMissSlot(ContainerChest chest, int targetIndex) {
        int size = chest.getLowerChestInventory().getSizeInventory();
        if (size <= 1) {
            return -1;
        }
        ArrayList<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i == targetIndex) {
                continue;
            }
            if (chest.getLowerChestInventory().getStackInSlot(i) == null) {
                emptySlots.add(i);
            }
        }
        if (!emptySlots.isEmpty()) {
            return emptySlots.get(RandomUtils.nextInt(0, emptySlots.size()));
        }
        int slot;
        do {
            slot = RandomUtils.nextInt(0, size);
        } while (slot == targetIndex);
        return slot;
    }

    @EventHook
    public void onPreMotion(MotionEvent event) {
        setSuffix(instant.getValue() ? "Instant" : maxDelay.getValue().intValue() + "ms");
        if (!event.isPre()) {
            return;
        }
        if (mc.thePlayer.ticksExisted <= 60) {
            return;
        }
        if (this.grabMouse.getValue() && isValidChest() && Display.isActive()) {
            mc.mouseHelper.mouseXYChange();
            mc.mouseHelper.ungrabMouseCursor();
            mc.mouseHelper.grabMouseCursor();
        }
        if (isValidChest()) {
            ++this.ticksInChest;
            if (this.ticksInChest * 50 > 255) {
                this.ticksInChest = 10;
            }
        } else {
            --this.ticksInChest;
            this.gotItems = false;
            this.targetSlot = -1;
            this.missClickPending = false;
            this.missClickDone = false;
            if (this.ticksInChest < 0) {
                this.ticksInChest = 0;
            }
        }
    }

    @EventHook
    public void onTick(ClientTickEvent event) {
        if (mc.thePlayer.ticksExisted <= 60) {
            return;
        }
        boolean validChest = isValidChest();
        if (!this.lastInChest && validChest) {
            this.startTimer.reset();
        }
        this.lastInChest = validChest;
        if (!validChest) {
            return;
        }
        ContainerChest chest = (ContainerChest) mc.thePlayer.openContainer;
        if (this.instant.getValue()) {
            boolean tookAny = false;
            int size = chest.getLowerChestInventory().getSizeInventory();
            for (int i = 0; i < size; i++) {
                ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
                if (stack != null && this.itemWhitelisted(stack) && !this.stealTrashItems.getValue()) {
                    mc.playerController.windowClick(chest.windowId, i, 0, 1, mc.thePlayer);
                    tookAny = true;
                    this.gotItems = true;
                }
            }
            if (tookAny && this.autoClose.getValue()) {
                mc.thePlayer.closeScreen();
            }
            return;
        }
        if (!this.startTimer.hasTimeElapsed(stealDelay.getValue(), false)) {
            return;
        }

        int size = chest.getLowerChestInventory().getSizeInventory();
        int foundSlot = -1;
        for (int i = 0; i < size; i++) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(i);
            if (stack != null && this.itemWhitelisted(stack) && !this.stealTrashItems.getValue()) {
                foundSlot = i;
                break;
            }
        }

        if (foundSlot == -1) {
            if (this.gotItems && this.autoClose.getValue() && this.ticksInChest > 3) {
                mc.thePlayer.closeScreen();
            }
            this.targetSlot = -1;
            this.missClickPending = false;
            this.missClickDone = false;
            this.decidedTimer = 0;
            return;
        }

        if (foundSlot != this.targetSlot) {
            this.targetSlot = foundSlot;
            this.decidedTimer = 0;
            this.missClickPending = false;
            this.missClickDone = false;
        }

        if (this.decidedTimer == 0) {
            int delayFirst = (int) Math.floor(Math.min(this.minDelay.getValue(), this.maxDelay.getValue()));
            int delaySecond = (int) Math.ceil(Math.max(this.minDelay.getValue(), this.maxDelay.getValue()));
            int base = RandomUtils.nextInt(delayFirst, delaySecond);
            this.decidedTimer = computeDynamicDelay(base, chest, this.targetSlot);
        }

        if (this.missClickEnabled.getValue() && !this.missClickDone && !this.missClickPending) {
            if (RandomUtils.nextDouble(0.0, 100.0) < this.missClickChance.getValue()) {
                int missSlot = findMissSlot(chest, this.targetSlot);
                if (missSlot != -1) {
                    mc.playerController.windowClick(chest.windowId, missSlot, 0, 1, mc.thePlayer);
                    int recFirst = (int) Math.floor(Math.min(this.missClickRecoveryMin.getValue(), this.missClickRecoveryMax.getValue()));
                    int recSecond = (int) Math.ceil(Math.max(this.missClickRecoveryMin.getValue(), this.missClickRecoveryMax.getValue()));
                    this.missClickRecovery = RandomUtils.nextInt(recFirst, recSecond);
                    this.missClickPending = true;
                    this.missClickTimer.reset();
                    return;
                }
            }
            this.missClickDone = true;
        }

        if (this.missClickPending) {
            if (!this.missClickTimer.hasTimeElapsed(this.missClickRecovery, false)) {
                return;
            }
            this.missClickPending = false;
            this.missClickDone = true;
        }

        if (this.timer.hasTimeElapsed(this.decidedTimer, false)) {
            ItemStack stack = chest.getLowerChestInventory().getStackInSlot(this.targetSlot);
            if (stack != null && this.itemWhitelisted(stack) && !this.stealTrashItems.getValue()) {
                mc.playerController.windowClick(chest.windowId, this.targetSlot, 0, 1, mc.thePlayer);
                this.gotItems = true;
            }
            this.timer.reset();
            this.decidedTimer = 0;
            this.targetSlot = -1;
            this.missClickDone = false;
        }
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent e) {
        if (autoDisable.getValue()) {
            toggle();
        }
    }

    private boolean itemWhitelisted(ItemStack itemStack) {
        if (InvUtils.isBadStackStealer(itemStack, true, true)) {
            return false;
        }
        ArrayList<Item> whitelistedItems = new ArrayList<Item>(){
            {
                this.add(Items.ender_pearl);
                this.add(Items.iron_ingot);
                this.add(Items.snowball);
                this.add(Items.gold_ingot);
                this.add(Items.redstone);
                this.add(Items.diamond);
                this.add(Items.emerald);
                this.add(Items.quartz);
                this.add(Items.bow);
                this.add(Items.arrow);
                this.add(Items.fishing_rod);
                this.add(Items.egg);
                this.add(Items.water_bucket);
                this.add(Items.lava_bucket);
            }
        };
        Item item = itemStack.getItem();
        String itemName = itemStack.getDisplayName();
        if (itemName.contains("Right Click") || itemName.contains("Click to Use") || itemName.contains("Players Finder")) {
            return true;
        }
        ArrayList<Integer> whitelistedPotions = new ArrayList<Integer>(){
            {
                this.add(6);
                this.add(1);
                this.add(5);
                this.add(8);
                this.add(14);
                this.add(12);
                this.add(10);
                this.add(16);
            }
        };
        if (item instanceof ItemPotion) {
            int potionID = this.getPotionId(itemStack);
            return whitelistedPotions.contains(potionID);
        }
        return item instanceof ItemBlock && !(((ItemBlock)item).getBlock() instanceof BlockTNT) && !(((ItemBlock)item).getBlock() instanceof BlockSlime) && !(((ItemBlock)item).getBlock() instanceof BlockFalling) || item instanceof ItemAnvilBlock || item instanceof ItemSword || item instanceof ItemArmor || item instanceof ItemTool || item instanceof ItemFood || item instanceof ItemSkull || itemName.contains("§") || whitelistedItems.contains(item) && !item.equals(Items.spider_eye);
    }

    private int getPotionId(ItemStack potion) {
        Item item = potion.getItem();
        try {
            if (item instanceof ItemPotion) {
                ItemPotion p = (ItemPotion)item;
                return p.getEffects(potion.getMetadata()).get(0).getPotionID();
            }
        }
        catch (NullPointerException ignored) {
        }
        return 0;
    }
}