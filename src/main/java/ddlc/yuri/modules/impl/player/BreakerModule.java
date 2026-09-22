package ddlc.yuri.modules.impl.player;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.BreakerWhitelistManager;
import ddlc.yuri.managers.impl.ProgressBarManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.utils.client.ClientInfoUtils;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import ddlc.yuri.utils.render.progress.ProgressBarEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.potion.Potion;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

@ModuleInfo(label = "Breaker", description = "Breaks beds with raw digging packets, optionally straight through walls", category = ModuleCategory.PLAYER)
public final class BreakerModule extends Module {

    /**
     * NetHandlerPlayServer#processPlayerDigging only validates this squared distance (from the feet
     * position raised by 1.5 to the block center) - there is no line of sight or facing check, which
     * is exactly why a purely packet driven dig goes through walls.
     */
    private static final double REACH_SQUARED = 36.0D;

    /**
     * ItemInWorldManager#blockRemoving harvests immediately once hardness * (ticks + 1) >= 0.7F.
     * Below that it latches the dig (receivedFinishDiggingPacket) and finishes it on its own once
     * the same term reaches 1.0F - no further packets required.
     */
    private static final float SERVER_THRESHOLD = 0.7F;
    private static final float VANILLA_THRESHOLD = 1.0F;

    public enum Mode {
        LEGIT("Legit"), VANILLA("Vanilla"), HYPIXEL("Hypixel"), PACKET("Packet"), QUEUE("Queue");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Break Mode", Mode.PACKET);
    private final NumberProperty breakRange = new NumberProperty("Breaker Range", 4.5f, 1f, 6f, 0.5f);
    private final NumberProperty safety = new NumberProperty("Safety Ticks", 1, 0, 5, 1);
    public final Property<Boolean> rotate = new Property<Boolean>("Rotations", true, () -> mode.getValue() != Mode.QUEUE);
    public final Property<Boolean> moveFix = new Property<Boolean>("Move Fix", true, () -> mode.getValue() != Mode.QUEUE && rotate.getValue());
    public final Property<Boolean> toolSpoof = new Property<Boolean>("Tool Spoof", true);
    public final Property<Boolean> swing = new Property<Boolean>("Visual Swing", true);
    public final Property<Boolean> whitelist = new Property<Boolean>("Whitelist", true);
    public final Property<Boolean> progressBar = new Property<Boolean>("Progress Bar", true);

    public BlockPos breakPos;
    private EnumFacing breakFace;
    /** Mirrors the server side (curblockDamage - initialDamage) of our own dig. */
    private int digTicks;
    private boolean digging;
    /** STOP_DESTROY_BLOCK is out, the server owns the rest of the dig now. */
    private boolean queued;
    private int timeout;
    private int cooldown;
    /** Ticks held on top of the finished dig so a laggy STOP still lands behind the server. */
    private int readyTicks;
    private int spoofSlot = -1;
    private int lastRealSlot = -1;
    private float progress;
    private ProgressBarEntry barEntry;

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        if (digging && !queued && breakPos != null) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, breakPos, breakFace));
        }
        releaseSpoof();
        resetState();
        ProgressBarManager.remove(barEntry);
        barEntry = null;
    }

    private void resetState() {
        breakPos = null;
        breakFace = EnumFacing.UP;
        digTicks = 0;
        digging = false;
        queued = false;
        timeout = 0;
        cooldown = 0;
        readyTicks = 0;
        progress = 0f;
    }

    @EventHook(value = EventPriority.VERY_HIGH)
    private void onPreUpdate(PreUpdateEvent event) {
        setSuffix(mode.getValue().toString());

        if (mc.thePlayer == null || mc.theWorld == null) {
            resetState();
            return;
        }

        keepSpoofAlive();

        if (cooldown > 0) {
            cooldown--;
        }

        if (digging) {
            tickDig();
            return;
        }

        if (cooldown > 0) {
            return;
        }

        BlockPos target = selectTarget();
        if (target == null) {
            progress = 0f;
            return;
        }

        EnumFacing face = bestFacing(target);
        if (mode.getValue() == Mode.LEGIT && !hasLineOfSight(target, face)) {
            return;
        }
        if (mode.getValue() == Mode.LEGIT && auraBusy()) {
            return;
        }

        startDig(target, face);
    }

    // ------------------------------------------------------------------ dig state machine

    private void startDig(BlockPos pos, EnumFacing face) {
        breakPos = pos;
        breakFace = face;
        digTicks = 0;
        digging = true;
        queued = false;
        readyTicks = 0;
        progress = 0f;

        applySpoof(pos);
        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, pos, face));
        sendSwing();
        aim(true);

        float perTick = hardnessPerTick(pos);
        timeout = ticksUntil(perTick, VANILLA_THRESHOLD) + graceTicks();

        // Fire and forget: the START/STOP pair latches the dig on the server, which then finishes it
        // by itself a full break time later. Not another packet is needed, not even staying in range.
        if (mode.getValue() == Mode.QUEUE) {
            stopDig(perTick);
        }
    }

    private void tickDig() {
        if (breakPos == null) {
            abortDig();
            return;
        }

        // The server confirming the harvest is the only thing that ends a dig for good.
        if (mc.theWorld.getBlockState(breakPos).getBlock() instanceof BlockAir) {
            finishDig();
            return;
        }

        if (!isTargetValid(breakPos)) {
            abortDig();
            return;
        }

        digTicks++;

        if (--timeout <= 0) {
            abortDig();
            return;
        }

        float perTick = hardnessPerTick(breakPos);
        float damage = perTick * (digTicks + 1);
        progress = MathHelper.clamp_float(damage, 0f, 1f);
        showCracks();

        if (queued) {
            aim(false);
            return;
        }

        if (damage >= threshold()) {
            if (readyTicks++ >= safety.getValue().intValue()) {
                aim(true);
                stopDig(perTick);
                return;
            }
        }

        aim(false);
    }

    private void stopDig(float perTick) {
        applySpoof(breakPos);
        PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, breakPos, breakFace));
        sendSwing();

        queued = true;
        // Either the server harvests right away (>= 0.7F) or it latches the dig and finishes it once
        // the same term hits 1.0F - both cases are covered by waiting for the block to turn to air.
        timeout = Math.max(0, ticksUntil(perTick, VANILLA_THRESHOLD) - digTicks) + graceTicks();
    }

    private void finishDig() {
        releaseSpoof();
        resetState();
        cooldown = 2;
        progress = 0f;
    }

    private void abortDig() {
        if (digging && !queued && breakPos != null) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, breakPos, breakFace));
        }
        releaseSpoof();
        resetState();
        cooldown = 2;
    }

    private float threshold() {
        switch (mode.getValue()) {
            case PACKET:
            case QUEUE:
                return SERVER_THRESHOLD;
            default:
                return VANILLA_THRESHOLD;
        }
    }

    private int ticksUntil(float perTick, float wanted) {
        if (perTick <= 0f) {
            return 200;
        }
        return Math.min(200, MathHelper.ceiling_float_int(wanted / perTick));
    }

    private int graceTicks() {
        return 10 + ClientInfoUtils.getPing() / 50 + safety.getValue().intValue();
    }

    // ------------------------------------------------------------------ target selection

    private BlockPos selectTarget() {
        BlockPos bed = findBed();
        if (bed == null) {
            return null;
        }

        if (mode.getValue() == Mode.HYPIXEL && !isBedOpen(bed)) {
            BlockPos cover = getNearestBlock(bed);
            if (cover != null && isTargetValid(cover)) {
                return cover;
            }
        }

        return isTargetValid(bed) ? bed : null;
    }

    private boolean isTargetValid(BlockPos pos) {
        if (pos == null || !inServerReach(pos)) {
            return false;
        }

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block instanceof BlockAir || block.getBlockHardness(mc.theWorld, pos) < 0f) {
            return false;
        }

        return !(whitelist.getValue() && BreakerWhitelistManager.isWhitelisted(pos));
    }

    /** Byte for byte the check NetHandlerPlayServer runs before it touches the dig at all. */
    private boolean inServerReach(BlockPos pos) {
        double dx = mc.thePlayer.posX - (pos.getX() + 0.5D);
        double dy = mc.thePlayer.posY - (pos.getY() + 0.5D) + 1.5D;
        double dz = mc.thePlayer.posZ - (pos.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz <= REACH_SQUARED;
    }

    private BlockPos findBed() {
        int radius = MathHelper.ceiling_double_int(breakRange.getValue());
        int px = MathHelper.floor_double(mc.thePlayer.posX);
        int py = MathHelper.floor_double(mc.thePlayer.posY);
        int pz = MathHelper.floor_double(mc.thePlayer.posZ);

        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = new BlockPos(px + x, py + y, pz + z);
                    if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed)) {
                        continue;
                    }
                    if (whitelist.getValue() && BreakerWhitelistManager.isWhitelisted(pos)) {
                        continue;
                    }
                    if (!inServerReach(pos)) {
                        continue;
                    }

                    double dist = mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                    if (dist > breakRange.getValue() || dist >= closestDist) {
                        continue;
                    }

                    closestDist = dist;
                    closest = pos;
                }
            }
        }

        return closest;
    }

    private boolean isBedOpen(BlockPos pos) {
        BlockPos otherHalf = null;
        for (BlockPos adjacent : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            if (mc.theWorld.getBlockState(adjacent).getBlock() instanceof BlockBed) {
                otherHalf = adjacent;
                break;
            }
        }
        if (otherHalf == null) {
            return false;
        }
        return isNearbyAir(pos) || isNearbyAir(otherHalf);
    }

    public boolean isNearbyAir(BlockPos pos) {
        for (BlockPos adjacent : new BlockPos[]{pos.up(), pos.south(), pos.east(), pos.west(), pos.north()}) {
            if (mc.theWorld.getBlockState(adjacent).getBlock() instanceof BlockAir) {
                return true;
            }
        }
        return false;
    }

    public BlockPos getNearestBlock(BlockPos pos) {
        double distance = Double.MAX_VALUE;
        BlockPos nearest = null;

        for (BlockPos adjacent : new BlockPos[]{pos.up(), pos.west(), pos.south(), pos.east(), pos.north()}) {
            Block block = mc.theWorld.getBlockState(adjacent).getBlock();
            if (block instanceof BlockAir || block instanceof BlockBed) {
                continue;
            }

            double dist = mc.thePlayer.getDistance(adjacent.getX() + 0.5, adjacent.getY() + 0.5, adjacent.getZ() + 0.5);
            if (dist < distance) {
                nearest = adjacent;
                distance = dist;
            }
        }

        return nearest;
    }

    // ------------------------------------------------------------------ server side dig model

    /**
     * Block#getPlayerRelativeBlockHardness rebuilt for an arbitrary stack, because
     * EntityPlayer#getToolDigEfficiency(Block, ItemStack) still reads the strength off the slot that
     * is currently selected and therefore lies as soon as a tool is spoofed.
     */
    private float hardnessPerTick(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        float hardness = block.getBlockHardness(mc.theWorld, pos);
        if (hardness < 0f) {
            return 0f;
        }

        ItemStack stack = serverHeldItem();
        float efficiency = digSpeed(block, stack);
        return canHarvest(block, stack) ? efficiency / hardness / 30f : efficiency / hardness / 100f;
    }

    private float digSpeed(Block block, ItemStack stack) {
        float speed = stack == null ? 1f : stack.getStrVsBlock(block);

        if (speed > 1f) {
            int level = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
            if (level > 0) {
                speed += level * level + 1;
            }
        }

        if (mc.thePlayer.isPotionActive(Potion.digSpeed)) {
            speed *= 1f + (mc.thePlayer.getActivePotionEffect(Potion.digSpeed).getAmplifier() + 1) * 0.2f;
        }

        if (mc.thePlayer.isPotionActive(Potion.digSlowdown)) {
            switch (mc.thePlayer.getActivePotionEffect(Potion.digSlowdown).getAmplifier()) {
                case 0:
                    speed *= 0.3f;
                    break;
                case 1:
                    speed *= 0.09f;
                    break;
                case 2:
                    speed *= 0.0027f;
                    break;
                default:
                    speed *= 8.1E-4f;
            }
        }

        if (mc.thePlayer.isInsideOfMaterial(Material.water) && !EnchantmentHelper.getAquaAffinityModifier(mc.thePlayer)) {
            speed /= 5f;
        }

        if (!mc.thePlayer.onGround) {
            speed /= 5f;
        }

        return speed;
    }

    private boolean canHarvest(Block block, ItemStack stack) {
        if (block.getMaterial().isToolNotRequired()) {
            return true;
        }
        return stack != null && stack.canHarvestBlock(block);
    }

    private ItemStack serverHeldItem() {
        int slot = spoofSlot == -1 ? mc.thePlayer.inventory.currentItem : spoofSlot;
        return mc.thePlayer.inventory.getStackInSlot(slot);
    }

    // ------------------------------------------------------------------ silent tool spoof

    private void applySpoof(BlockPos pos) {
        if (!toolSpoof.getValue() || pos == null) {
            return;
        }

        int best = bestSlot(mc.theWorld.getBlockState(pos).getBlock());
        if (best == -1 || best == mc.thePlayer.inventory.currentItem) {
            releaseSpoof();
            return;
        }
        if (best == spoofSlot) {
            return;
        }

        lastRealSlot = mc.thePlayer.inventory.currentItem;
        spoofSlot = best;
        PacketUtils.sendPacket(new C09PacketHeldItemChange(best));
    }

    /**
     * The server keeps re-reading the held item while the dig runs, so a slot change made by the
     * player has to be answered with another spoof packet instead of silently desyncing us.
     */
    private void keepSpoofAlive() {
        if (spoofSlot == -1) {
            return;
        }

        if (!digging || !toolSpoof.getValue()) {
            releaseSpoof();
            return;
        }

        if (mc.thePlayer.inventory.currentItem != lastRealSlot) {
            lastRealSlot = mc.thePlayer.inventory.currentItem;
            if (lastRealSlot == spoofSlot) {
                spoofSlot = -1;
            } else {
                PacketUtils.sendPacket(new C09PacketHeldItemChange(spoofSlot));
            }
        }
    }

    private void releaseSpoof() {
        if (spoofSlot == -1) {
            return;
        }

        spoofSlot = -1;
        if (mc.thePlayer != null) {
            PacketUtils.sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
        }
    }

    private int bestSlot(Block block) {
        int slot = -1;
        // Start from what we already hold so an equally good hotbar slot never costs a packet.
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        float best = held == null ? 1f : held.getStrVsBlock(block);

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getStrVsBlock(block) > best) {
                best = stack.getStrVsBlock(block);
                slot = i;
            }
        }

        return slot;
    }

    // ------------------------------------------------------------------ rotations / visuals

    private void aim(boolean force) {
        if (!rotate.getValue() || mode.getValue() == Mode.QUEUE || breakPos == null) {
            return;
        }
        if (auraBusy()) {
            return;
        }

        // Packet modes only snap on the two ticks that actually carry a packet, the legit modes hold
        // the rotation for the whole dig like a real client would.
        boolean hold = mode.getValue() == Mode.LEGIT || mode.getValue() == Mode.VANILLA || mode.getValue() == Mode.HYPIXEL;
        if (!force && !hold) {
            return;
        }

        float[] rotations = RotationUtils.getRotationsTo(mc.thePlayer.getPositionEyes(1f), hitVec(breakPos, breakFace));
        RotationManager.setRotations(rotations, 10, moveFix.getValue() ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.OFF);
    }

    private boolean auraBusy() {
        AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
        return aura != null && aura.isEnabled() && AuraModule.target != null;
    }

    private void sendSwing() {
        PacketUtils.sendPacket(new C0APacketAnimation());
        if (swing.getValue()) {
            mc.thePlayer.swingItemClient();
        }
    }

    private void showCracks() {
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), breakPos, (int) (progress * 10f) - 1);
    }

    /** Prefers a face that is actually exposed, so the facing byte stays plausible even through a wall. */
    private EnumFacing bestFacing(BlockPos pos) {
        Vec3 eyes = mc.thePlayer.getPositionEyes(1f);
        EnumFacing best = EnumFacing.UP;
        double bestScore = Double.MAX_VALUE;

        for (EnumFacing facing : EnumFacing.VALUES) {
            boolean exposed = !mc.theWorld.getBlockState(pos.offset(facing)).getBlock().isFullBlock();
            double score = eyes.distanceTo(hitVec(pos, facing)) + (exposed ? 0D : 100D);

            if (score < bestScore) {
                bestScore = score;
                best = facing;
            }
        }

        return best;
    }

    private Vec3 hitVec(BlockPos pos, EnumFacing facing) {
        return new Vec3(
                pos.getX() + 0.5D + facing.getFrontOffsetX() * 0.5D,
                pos.getY() + 0.5D + facing.getFrontOffsetY() * 0.5D,
                pos.getZ() + 0.5D + facing.getFrontOffsetZ() * 0.5D);
    }

    private boolean hasLineOfSight(BlockPos pos, EnumFacing facing) {
        MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(mc.thePlayer.getPositionEyes(1f), hitVec(pos, facing), false, true, false);
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && pos.equals(hit.getBlockPos());
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        boolean visible = progressBar.getValue() && digging && breakPos != null;

        if (!visible) {
            if (barEntry != null) {
                ProgressBarManager.remove(barEntry);
                barEntry = null;
            }
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float centerX = sr.getScaledWidth() / 2.0f;
        float textY = sr.getScaledHeight() / 2.0f + 13;

        if (barEntry == null) {
            barEntry = ProgressBarManager.add(progress, centerX, textY);
        }
        barEntry.setProgress(progress);
        barEntry.setX(centerX);
        barEntry.setY(textY);
    }
}
