package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.block.BlockBed;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleInfo(label = "Bed ESP", description = "Shows block types around beds", category = ModuleCategory.RENDER)
public class BedESPModule extends Module {


    private final NumberProperty range = new NumberProperty("Range", 15, 2, 30, 1);
    private final NumberProperty rate = new NumberProperty("Rate", 0.4D, 0.1D, 3D, 0.1D);

    private BlockPos[] bed = null;
    private final List<BlockPos[]> beds = new ArrayList<>();
    private long lastCheck = 0L;

    @EventHook
    public void onUpdate(PreUpdateEvent event) {
        if (System.currentTimeMillis() - lastCheck >= rate.getValue() * 1000.0) {
            lastCheck = System.currentTimeMillis();

            int rangeValue = range.getValue().intValue();
            for (int i = -rangeValue; i <= rangeValue; ++i) {
                for (int j = -rangeValue; j <= rangeValue; ++j) {
                    for (int k = -rangeValue; k <= rangeValue; ++k) {
                        BlockPos blockPos = new BlockPos(mc.thePlayer.posX + j, mc.thePlayer.posY + i, mc.thePlayer.posZ + k);
                        IBlockState getBlockState = mc.theWorld.getBlockState(blockPos);
                        if (getBlockState.getBlock() == Blocks.bed && getBlockState.getValue(BlockBed.PART) == BlockBed.EnumPartType.FOOT) {
                            for (BlockPos[] bedPair : beds) {
                                if (BlockPos.isSamePos(blockPos, bedPair[0])) {
                                    continue;
                                }
                            }
                            beds.add(new BlockPos[]{blockPos, blockPos.offset(getBlockState.getValue(BlockBed.FACING))});
                        }
                    }
                }
            }
        }
    }

    @EventHook
    public void onRender(Render3DEvent event) {
        if (BlockPos.nullCheck() && !beds.isEmpty()) {
            Iterator<BlockPos[]> iterator = beds.iterator();
            while (iterator.hasNext()) {
                BlockPos[] blockPos = iterator.next();
                if (mc.theWorld.getBlockState(blockPos[0]).getBlock() instanceof BlockBed) {
                    RenderUtils.renderBed(blockPos);
                } else {
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void onDisable() {
        bed = null;
        beds.clear();
    }
}