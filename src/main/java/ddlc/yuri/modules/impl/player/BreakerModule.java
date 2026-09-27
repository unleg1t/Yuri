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
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.player.packet.PacketUtils;
import ddlc.yuri.utils.render.progress.ProgressBarEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.Arrays;

@ModuleInfo(label = "Breaker", description = "Automatically breaks beds in the BedWars minigame", category = ModuleCategory.PLAYER)
public final class BreakerModule extends Module {

    private final ModeProperty<Mode> breakType = new ModeProperty<>("Break Mode", Mode.LEGIT);
    private final NumberProperty breakrange = new NumberProperty("Breaker Range", 3f, 3f, 6f, 0.5f);
    public final Property<Boolean> autoTool = new Property<Boolean>("Auto Tool", true);
    public final Property<Boolean> swing = new Property<Boolean>("Visual Swing", true);
    public final Property<Boolean> moveFix = new Property<Boolean>("Move Fix", true);
    public final Property<Boolean> whitelist = new Property<Boolean>("Whitelist", true);
    public final Property<Boolean> progressBar = new Property<Boolean>("Progress Bar", true);

    public enum Mode {
        VANILLA("Vanilla"), HYPIXEL("Hypixel"), LEGIT("Legit");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public BlockPos breakPos;
    public BlockPos bedPos;

    public float blockDamage;
    public float lastBlockDamage;
    public int blockDamageCD;
    public int spoofTick = 0;
    public ItemStack st = null;
    private ProgressBarEntry barEntry;
    private int oldSlot = -1;
    private boolean isBreaking = false;

    @Override
    public void onEnable() {
        blockDamage = 0;
        lastBlockDamage = blockDamage;
        blockDamageCD = 5;
        bedPos = null;
        barEntry = null;
        breakPos = null;
        oldSlot = -1;
        isBreaking = false;
    }

    @Override
    public void onDisable() {
        if (blockDamage > 0 && blockDamage < 1 && breakPos != null) {
            PacketUtils.sendPacket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, breakPos, EnumFacing.UP));
        }
        blockDamage = 0;
        ProgressBarManager.remove(barEntry);
        barEntry = null;
        lastBlockDamage = blockDamage;
        blockDamageCD = 0;
        bedPos = null;
        breakPos = null;
        resetSlot();
    }

    public void resetSlot() {
        if (isBreaking) {
            if (autoTool.getValue() && oldSlot != -1 && oldSlot >= 0 && oldSlot < 9 && mc.thePlayer != null) {
                mc.thePlayer.inventory.currentItem = oldSlot;
                if (mc.playerController != null) {
                    mc.playerController.syncCurrentPlayItem();
                }
            }
            oldSlot = -1;
            isBreaking = false;
        }
    }

    public void sendBlockBreak(BlockPos pos, EnumFacing face) {
        lastBlockDamage = blockDamage;
        if (pos == null) {
            if (blockDamageCD > 0) {
                blockDamageCD--;
            }
            resetSlot();
            return;
        }

        Block block = mc.theWorld.getBlockState(pos).getBlock();
        if (block instanceof BlockAir) {
            this.blockDamage = 0;
            this.blockDamageCD = 6;
            resetSlot();
            return;
        }
        if (blockDamageCD > 0) {
            blockDamageCD--;
            return;
        }
        spoofTick = getSlotFromBlock(block);
        if (spoofTick == -1) {
            spoofTick = mc.thePlayer.inventory.currentItem;
        }
        st = mc.thePlayer.inventory.getStackInSlot(spoofTick);

        AuraModule killAura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
        if (blockDamage == 0f) {
            if (killAura.isEnabled() && AuraModule.target != null && breakPos != null) {
                resetSlot();
                return;
            } else {
                startBreak();
            }
        } else {
            if (autoTool.getValue() && spoofTick != -1 && mc.thePlayer.inventory.currentItem != spoofTick) {
                mc.thePlayer.inventory.currentItem = spoofTick;
                if (mc.playerController != null) {
                    mc.playerController.syncCurrentPlayItem();
                }
            }
            addDamage(breakPos);
        }

        if (blockDamage >= 1f) {
            if (!killAura.isEnabled() || AuraModule.target == null) {
                stopBreak();
            }
        }
    }

    public void sendAnimReqPcket(Packet packet) {
        PacketUtils.sendPacket(packet);
        if (swing.getValue()) {
            mc.thePlayer.swingItem();
        } else {
            PacketUtils.sendPacket(new C0APacketAnimation());
        }
    }

    public void startBreak() {
        if (!isBreaking) {
            oldSlot = mc.thePlayer.inventory.currentItem;
            isBreaking = true;
        }
        if (autoTool.getValue() && spoofTick != -1) {
            mc.thePlayer.inventory.currentItem = spoofTick;
            if (mc.playerController != null) {
                mc.playerController.syncCurrentPlayItem();
            }
        }

        sendAnimReqPcket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, breakPos, EnumFacing.UP));
        addDamage(breakPos);
    }

    public void addDamage(BlockPos pos) {
        Block block = mc.theWorld.getBlockState(pos).getBlock();
        ItemStack held = mc.thePlayer.inventory.getStackInSlot(mc.thePlayer.inventory.currentItem);
        ItemStack tool = held != null ? held : st;
        float addyDMG = mc.thePlayer.getToolDigEfficiency(block, tool) / block.getBlockHardness(mc.theWorld, pos) / 30.0F;
        if (!mc.thePlayer.onGround) {
            addyDMG /= 5;
        }
        blockDamage += addyDMG;
        mc.theWorld.sendBlockBreakProgress(mc.thePlayer.getEntityId(), pos, (int) (this.blockDamage * 10.0F) - 1);
    }

    public void stopBreak() {
        sendAnimReqPcket(new C07PacketPlayerDigging(C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK, breakPos, EnumFacing.UP));
        this.blockDamage = 0;
        this.blockDamageCD = 5;
        mc.theWorld.setBlockToAir(breakPos);
        resetSlot();
    }

    @EventHook(value = EventPriority.VERY_HIGH)
    private void onPreUpdate(PreUpdateEvent event) {
        BlockPos closestBedPos = findBed(breakrange.getValue().floatValue());

        if (closestBedPos != null) {
            if (!checkPosValidity(closestBedPos)) {
                resetSlot();
                return;
            }
            if (blockDamageCD != 0) {
                if (breakType.getValue() == Mode.HYPIXEL) {
                    if (isBedOpen(closestBedPos)) {
                        breakPos = closestBedPos;
                    } else {
                        if (breakPos == null) {
                            breakPos = getNearestBlock(closestBedPos);
                        } else if (!(mc.theWorld.getBlockState(breakPos).getBlock() instanceof BlockAir) && !(mc.theWorld.getBlockState(breakPos).getBlock().isFullBlock())) {
                            breakPos = getNearestBlock(closestBedPos);
                        }
                    }
                } else {
                    breakPos = closestBedPos;
                }
            }
            sendBlockBreak(breakPos, EnumFacing.UP);
        } else {
            blockDamage = 0;
            blockDamageCD = 1;
            bedPos = null;
            breakPos = null;
            resetSlot();
        }

        if (breakPos != null && !(mc.theWorld.getBlockState(breakPos).getBlock() instanceof BlockAir) && AuraModule.target == null) {
            if ((blockDamage == 0 || (blockDamage + mc.theWorld.getBlockState(breakPos).getBlock().getPlayerRelativeBlockHardness(this.mc.thePlayer, this.mc.thePlayer.worldObj, breakPos)) >= 1)) {
                float[] rotations = RotationUtils.getRotationFromPosition(
                        breakPos.getX() + 0.5, breakPos.getY() + 0.5, breakPos.getZ() + 0.5);
                RotationManager.setRotations(rotations, 10, moveFix.getValue() ? RotationManager.MovementFix.NORMAL : RotationManager.MovementFix.OFF);
            }
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        boolean active = blockDamage > 0 && blockDamage < 1 && breakPos != null;
        boolean visible = progressBar.getValue() && active;

        if (!visible) {
            if (barEntry != null) {
                ProgressBarManager.remove(barEntry);
                barEntry = null;
            }
            return;
        }

        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(mc);
        float centerX = sr.getScaledWidth() / 2.0f;
        float textY = sr.getScaledHeight() / 2.0f + 13;

        if (barEntry == null) {
            barEntry = ProgressBarManager.add(blockDamage, centerX, textY);
        }
        barEntry.setProgress(blockDamage);
        barEntry.setX(centerX);
        barEntry.setY(textY);
    }

    private boolean checkPosValidity(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        return !(mc.thePlayer.getDistance(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 6);
    }

    private BlockPos findBed(float distance) {
        float bedDist = 69420;
        BlockPos bedPos = null;

        for (float x = -distance; x < distance; x++) {
            for (float z = -distance; z < distance; z++) {
                for (float y = -distance; y < distance; y++) {
                    BlockPos cPos = new BlockPos(x + mc.thePlayer.posX, y + mc.thePlayer.posY, z + mc.thePlayer.posZ);
                    if (mc.theWorld.getBlockState(cPos).getBlock() instanceof BlockBed) {

                        if (whitelist.getValue()) {
                            if (BreakerWhitelistManager.isWhitelisted(cPos)) {
                                continue;
                            }
                        }


                        double bcd = mc.thePlayer.getDistance(cPos.getX() + 0.5, cPos.getY() + 0.5, cPos.getZ() + 0.5);
                        if (bcd < bedDist) {
                            bedDist = (float) bcd;
                            bedPos = cPos;
                        }
                    }
                }
            }
        }

        return bedPos;
    }

    private boolean isBedOpen(BlockPos pos) {
        BlockPos bed_pos2 = null;
        for (BlockPos adjacentPos : new BlockPos[]{pos.north(), pos.south(), pos.east(), pos.west()}) {
            if (mc.theWorld.getBlockState(adjacentPos).getBlock() instanceof BlockBed) {
                bed_pos2 = adjacentPos;
                break;
            }
        }
        if (bed_pos2 == null) return false;
        return isNearbyAir(pos) || isNearbyAir(bed_pos2);
    }

    public boolean isNearbyAir(BlockPos pos) {
        for (BlockPos adjacentPos : new BlockPos[]{pos.up(), pos.south(), pos.east(), pos.west(), pos.north()}) {
            if (mc.theWorld.getBlockState(adjacentPos).getBlock() instanceof BlockAir) {
                return true;

            }
        }
        return false;
    }

    public BlockPos getNearestBlock(BlockPos pos) {
        double distance = 69;
        BlockPos lasb = null;

        for (BlockPos p : new ArrayList<>(Arrays.asList(pos.up(), pos.west(), pos.south(), pos.east(), pos.north()))) {
            if (!(mc.theWorld.getBlockState(p).getBlock() instanceof BlockBed) && mc.thePlayer.getDistance(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) < distance) {
                lasb = p;
                distance = mc.thePlayer.getDistance(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
            }
        }
        return lasb;
    }

    public int getSlotFromBlock(Block block) {
        if (mc.thePlayer == null || mc.thePlayer.inventory == null) return -1;
        int bestSlot = -1;
        float bestSpeed = 1.0F;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack == null) continue;

            float speed = stack.getStrVsBlock(block);
            if (speed > 1.0F) {
                int eff = EnchantmentHelper.getEnchantmentLevel(Enchantment.efficiency.effectId, stack);
                if (eff > 0) {
                    speed += (float) (eff * eff + 1);
                }
            }

            if (stack.getItem() instanceof ItemShears) {
                if (block instanceof BlockBed || block.getMaterial() == Material.cloth) {
                    speed = Math.max(speed, 5.0F);
                }
            }

            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }

        if (bestSlot == -1 && block instanceof BlockBed) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
                if (stack != null && (stack.getItem() instanceof ItemShears || stack.getItem() instanceof ItemAxe || stack.getItem() instanceof ItemPickaxe)) {
                    bestSlot = i;
                    break;
                }
            }
        }

        return bestSlot;
    }
}