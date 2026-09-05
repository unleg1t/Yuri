package net.minecraft.client.renderer;

import ddlc.yuri.Yuri;
import ddlc.yuri.managers.impl.SlotManager;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.modules.impl.render.CameraModule;
import ddlc.yuri.utils.player.InventoryUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.minecraft.src.Config;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.MapData;
import net.optifine.DynamicLights;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.Shaders;
import org.lwjgl.opengl.GL11;

public class ItemRenderer {
    private static final ResourceLocation RES_MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");
    private static final ResourceLocation RES_UNDERWATER_OVERLAY = new ResourceLocation("textures/misc/underwater.png");
    private final Minecraft mc;
    private ItemStack itemToRender;
    private float equippedProgress;
    private float prevEquippedProgress;
    private final RenderManager renderManager;
    private final RenderItem itemRenderer;
    private int equippedItemSlot = -1;

    public ItemRenderer(Minecraft mcIn) {
        this.mc = mcIn;
        this.renderManager = mcIn.getRenderManager();
        this.itemRenderer = mcIn.getRenderItem();
    }

    public void renderItem(EntityLivingBase entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform) {
        if (heldStack != null) {
            Item item = heldStack.getItem();
            Block block = Block.getBlockFromItem(item);
            GlStateManager.pushMatrix();

            if (this.itemRenderer.shouldRenderItemIn3D(heldStack)) {
                GlStateManager.scale(2.0F, 2.0F, 2.0F);

                if (this.isBlockTranslucent(block) && (!Config.isShaders() || !Shaders.renderItemKeepDepthMask)) {
                    GlStateManager.depthMask(false);
                }
            }

            this.itemRenderer.renderItemModelForEntity(heldStack, entityIn, transform);

            if (this.isBlockTranslucent(block)) {
                GlStateManager.depthMask(true);
            }

            GlStateManager.popMatrix();
        }
    }

    private boolean isBlockTranslucent(Block blockIn) {
        return blockIn != null && blockIn.getBlockLayer() == EnumWorldBlockLayer.TRANSLUCENT;
    }

    private void rotateArroundXAndY(float angle, float angleY) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(angle, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(angleY, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.popMatrix();
    }

    private void setLightMapFromPlayer(AbstractClientPlayer clientPlayer) {
        int i = this.mc.theWorld.getCombinedLight(new BlockPos(clientPlayer.posX, clientPlayer.posY + (double) clientPlayer.getEyeHeight(), clientPlayer.posZ), 0);

        if (Config.isDynamicLights()) {
            i = DynamicLights.getCombinedLight(this.mc.getRenderViewEntity(), i);
        }

        float f = (float) (i & 65535);
        float f1 = (float) (i >> 16);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);
    }

    private void rotateWithPlayerRotations(EntityPlayerSP entityplayerspIn, float partialTicks) {
        float f = entityplayerspIn.prevRenderArmPitch + (entityplayerspIn.renderArmPitch - entityplayerspIn.prevRenderArmPitch) * partialTicks;
        float f1 = entityplayerspIn.prevRenderArmYaw + (entityplayerspIn.renderArmYaw - entityplayerspIn.prevRenderArmYaw) * partialTicks;
        GlStateManager.rotate((entityplayerspIn.rotationPitch - f) * 0.1F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate((entityplayerspIn.rotationYaw - f1) * 0.1F, 0.0F, 1.0F, 0.0F);
    }

    private float getMapAngleFromPitch(float pitch) {
        float f = 1.0F - pitch / 45.0F + 0.1F;
        f = MathHelper.clamp_float(f, 0.0F, 1.0F);
        f = -MathHelper.cos(f * (float) Math.PI) * 0.5F + 0.5F;
        return f;
    }

    private void renderRightArm(RenderPlayer renderPlayerIn) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(54.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(64.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-62.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(0.25F, -0.85F, 0.75F);
        renderPlayerIn.renderRightArm(this.mc.thePlayer);
        GlStateManager.popMatrix();
    }

    private void renderLeftArm(RenderPlayer renderPlayerIn) {
        GlStateManager.pushMatrix();
        GlStateManager.rotate(92.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(45.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(41.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.translate(-0.3F, -1.1F, 0.45F);
        renderPlayerIn.renderLeftArm(this.mc.thePlayer);
        GlStateManager.popMatrix();
    }

    private void renderPlayerArms(AbstractClientPlayer clientPlayer) {
        this.mc.getTextureManager().bindTexture(clientPlayer.getLocationSkin());
        Render<AbstractClientPlayer> render = this.renderManager.<AbstractClientPlayer>getEntityRenderObject(this.mc.thePlayer);
        RenderPlayer renderplayer = (RenderPlayer) render;

        if (!clientPlayer.isInvisible()) {
            GlStateManager.disableCull();
            this.renderRightArm(renderplayer);
            this.renderLeftArm(renderplayer);
            GlStateManager.enableCull();
        }
    }

    private void renderItemMap(AbstractClientPlayer clientPlayer, float pitch, float equipmentProgress, float swingProgress) {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        GlStateManager.translate(f, f1, f2);
        float f3 = this.getMapAngleFromPitch(pitch);
        GlStateManager.translate(0.0F, 0.04F, -0.72F);
        GlStateManager.translate(0.0F, equipmentProgress * -1.2F, 0.0F);
        GlStateManager.translate(0.0F, f3 * -0.5F, 0.0F);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * -85.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
        this.renderPlayerArms(clientPlayer);
        float f4 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f5 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f4 * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f5 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f5 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.38F, 0.38F, 0.38F);
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-1.0F, -1.0F, 0.0F);
        GlStateManager.scale(0.015625F, 0.015625F, 0.015625F);
        this.mc.getTextureManager().bindTexture(RES_MAP_BACKGROUND);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GL11.glNormal3f(0.0F, 0.0F, -1.0F);
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(-7.0D, 135.0D, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldrenderer.pos(135.0D, 135.0D, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldrenderer.pos(135.0D, -7.0D, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldrenderer.pos(-7.0D, -7.0D, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        MapData mapdata = Items.filled_map.getMapData(this.itemToRender, this.mc.theWorld);

        if (mapdata != null) {
            this.mc.entityRenderer.getMapItemRenderer().renderMap(mapdata, false);
        }
    }

    private void renderPlayerArm(AbstractClientPlayer clientPlayer, float equipProgress, float swingProgress) {
        float f = -0.3F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        float f1 = 0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI * 2.0F);
        float f2 = -0.4F * MathHelper.sin(swingProgress * (float) Math.PI);
        GlStateManager.translate(f, f1, f2);
        GlStateManager.translate(0.64000005F, -0.6F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f3 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f4 * 70.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * -20.0F, 0.0F, 0.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(clientPlayer.getLocationSkin());
        GlStateManager.translate(-1.0F, 3.6F, 3.5F);
        GlStateManager.rotate(120.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(200.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F);
        GlStateManager.translate(5.6F, 0.0F, 0.0F);
        Render<AbstractClientPlayer> render = this.renderManager.<AbstractClientPlayer>getEntityRenderObject(this.mc.thePlayer);
        GlStateManager.disableCull();
        RenderPlayer renderplayer = (RenderPlayer) render;
        renderplayer.renderRightArm(this.mc.thePlayer);
        GlStateManager.enableCull();
    }

    private void doItemUsedTransformations(float swingProgress) {
        float f = -0.4F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        float f1 = 0.2F * MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI * 2.0F);
        float f2 = -0.2F * MathHelper.sin(swingProgress * (float) Math.PI);
        GlStateManager.translate(f, f1, f2);
    }

    private void performDrinking(AbstractClientPlayer clientPlayer, float partialTicks) {
        float f = (float) clientPlayer.getItemInUseCount() - partialTicks + 1.0F;
        float f1 = f / (float) this.itemToRender.getMaxItemUseDuration();
        float f2 = MathHelper.abs(MathHelper.cos(f / 4.0F * (float) Math.PI) * 0.1F);

        if (f1 >= 0.8F) {
            f2 = 0.0F;
        }

        GlStateManager.translate(0.0F, f2, 0.0F);
        float f3 = 1.0F - (float) Math.pow((double) f1, 27.0D);
        GlStateManager.translate(f3 * 0.6F, f3 * -0.5F, f3 * 0.0F);
        GlStateManager.rotate(f3 * 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f3 * 10.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f3 * 30.0F, 0.0F, 0.0F, 1.0F);
    }

    private void transformFirstPersonItem(float equipProgress, float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.translate(0.0F, equipProgress * -0.6F, 0.0F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private void doBowTransformations(float partialTicks, AbstractClientPlayer clientPlayer) {
        GlStateManager.rotate(-18.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-12.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-8.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(-0.9F, 0.2F, 0.0F);
        float f = (float) this.itemToRender.getMaxItemUseDuration() - ((float) clientPlayer.getItemInUseCount() - partialTicks + 1.0F);
        float f1 = f / 20.0F;
        f1 = (f1 * f1 + f1 * 2.0F) / 3.0F;

        if (f1 > 1.0F) {
            f1 = 1.0F;
        }

        if (f1 > 0.1F) {
            float f2 = MathHelper.sin((f - 0.1F) * 1.3F);
            float f3 = f1 - 0.1F;
            float f4 = f2 * f3;
            GlStateManager.translate(f4 * 0.0F, f4 * 0.01F, f4 * 0.0F);
        }

        GlStateManager.translate(f1 * 0.0F, f1 * 0.0F, f1 * 0.1F);
        GlStateManager.scale(1.0F, 1.0F, 1.0F + f1 * 0.2F);
    }

    private void doBlockTransformations() {
        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }

    public void renderItemInFirstPerson(float partialTicks) {
        if (!Config.isShaders() || !Shaders.isSkipRenderHand()) {
            float f = 1.0F - (this.prevEquippedProgress + (this.equippedProgress - this.prevEquippedProgress) * partialTicks);
            AbstractClientPlayer abstractclientplayer = this.mc.thePlayer;
            float f1 = abstractclientplayer.getSwingProgress(partialTicks);
            float f2 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
            float f3 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
            float var16 = MathHelper.sin(MathHelper.sqrt_float(f1) * 3.1415927F);
            final float swingProgress = abstractclientplayer.swingProgress;
            final float convertedProgress = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI * 2);
            this.rotateArroundXAndY(f2, f3);
            this.setLightMapFromPlayer(abstractclientplayer);
            this.rotateWithPlayerRotations((EntityPlayerSP) abstractclientplayer, partialTicks);
            GlStateManager.enableRescaleNormal();
            GlStateManager.translate(CameraModule.x.getValue().floatValue(), CameraModule.y.getValue().floatValue(), CameraModule.z.getValue().floatValue());
            GlStateManager.pushMatrix();

            if (this.itemToRender != null) {
                boolean renderMap = this.itemToRender.getItem() instanceof ItemMap;
                boolean shouldBlock = !renderMap && (AuraModule.autoBlocking && this.itemToRender.getItem() instanceof ItemSword);
                boolean shouldBlock2 = !shouldBlock && abstractclientplayer.getItemInUseCount() > 0;
                boolean shouldBlock3 = shouldBlock2 && this.itemToRender.getItemUseAction() == EnumAction.BLOCK;

                if (!shouldBlock3 && this.itemToRender.getItem() instanceof ItemSword) {
                    GlStateManager.translate(0.0F, 0.0F, -0.02F);
                    GlStateManager.rotate(1.0F, 0.0F, 0.0F, -0.1F);
                } else if (this.itemToRender.getItem() instanceof ItemPotion) {
                    GlStateManager.translate(-0.0225F, -0.02F, 0.0F);
                    GlStateManager.rotate(1.0F, 0.0F, 0.0F, 0.1F);
                } else if (this.itemToRender.getItem() instanceof ItemFishingRod || this.itemToRender.getItem() instanceof ItemCarrotOnAStick) {
                    GlStateManager.translate(0.08F, -0.0275F, -0.33F);
                    GlStateManager.scale(0.949999988079071D, 1.0D, 1.0D);
                } else if (this.itemToRender.getItem() instanceof ItemBow) {
                    GlStateManager.translate(0.0F, 0.0F, 0.0F);
                    GlStateManager.rotate(0.9F, 0.0F, 0.001F, 0.0F);
                } else if (this.itemToRender.getItem() instanceof ItemBlock) {
                    Block block = ((ItemBlock) this.itemToRender.getItem()).getBlock();
                    if (block instanceof BlockCarpet || block instanceof BlockSnow) {
                        GlStateManager.translate(0.0F, -0.25F, 0.0F);
                    }
                }
                if (this.itemToRender.getItem() instanceof ItemMap) {
                    this.renderItemMap(abstractclientplayer, f2, f, f1);
                } else if (abstractclientplayer.getItemInUseCount() > 0 || AuraModule.autoBlocking) {
                    EnumAction enumaction = this.itemToRender.getItemUseAction();

                    if (Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class).isEnabled() && AuraModule.target != null
                            && InventoryUtils.isHoldingSword() && AuraModule.autoBlocking)
                        enumaction = EnumAction.BLOCK;

                    switch (enumaction) {
                        case NONE:
                            this.transformFirstPersonItem(f, 0.0F);
                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                            break;
                        case EAT:
                        case DRINK:
                            if (CameraModule.swingEating.getValue()
                                    && Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled()) {
                                this.Swing();
                            }
                            this.performDrinking(abstractclientplayer, partialTicks);
                            this.transformFirstPersonItem(f, swingProgress);
                            GlStateManager.scale(CameraModule.scale.getValue(),
                                    CameraModule.scale.getValue(), CameraModule.scale.getValue());
                            break;

                        case BLOCK:
                            if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled() && CameraModule.dontResetBlock.getValue())
                                f = 0.0f;
                            if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled()) {
                                switch (CameraModule.mode.getValue()) {
                                    case INERTIA:
                                        this.transformFirstPersonItem(0.05f, f1);
                                        GlStateManager.translate(-0.5F, 0.5F, 0.0F);
                                        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                                        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                                        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        break;
                                    case PUNCH:
                                        transformFirstPersonItem(f, 0.0f);
                                        func_178103_d(0.2F);
                                        GlStateManager.translate(0.1f, 0.2f, 0.3f);
                                        GlStateManager.rotate(-var16 * 30.0f, -5.0f, 0.0f, 9.0f);
                                        GlStateManager.rotate(-var16 * 10.0f, 1.0f, -0.4f, -0.5f);
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        break;
                                    case STELLA:
                                        transformFirstPersonItem(-0.1f, f1);
                                        GlStateManager.translate(-0.5F, 0.4F, -0.2F);
                                        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                                        GlStateManager.rotate(-70.0F, 1.0F, 0.0F, 0.0F);
                                        GlStateManager.rotate(40.0F, 0.0F, 1.0F, 0.0F);
                                        break;
                                    case STYLES:
                                        this.transformFirstPersonItem(f, 0.0F);
                                        this.func_178103_d(0.2F);
                                        float var11 = MathHelper.sin((float) (MathHelper.sqrt_float(f1) * Math.PI));
                                        GlStateManager.translate(-0.05f, 0.2f, 0.0f);
                                        GlStateManager.rotate(-var11 * 70.0f / 2.0f, -8.0f, -0.0f, 9.0f);
                                        GlStateManager.rotate(-var11 * 70.0f, 1.0f, -0.4f, -0.0f);
                                        break;
                                    case SWING:
                                        this.transformFirstPersonItem(f / 2.0F, f1);
                                        this.func_178103_d(0.4F);
                                        break;
                                    case ETHEREAL:
                                        transformFirstPersonItem(f, 0.0f);
                                        func_178103_d(0.2F);
                                        GlStateManager.translate(-0.05f, 0.2f, 0.2f);
                                        GlStateManager.rotate(-var16 * 70.0f / 2.0f, -8.0f, -0.0f, 9.0f);
                                        GlStateManager.rotate(-var16 * 70.0f, 1.0f, -0.4f, -0.0f);
                                        break;
                                    case OLD:
                                        this.transformFirstPersonItem(f, f1);
                                        GlStateManager.translate(0, 0.3, 0);
                                        this.doBlockTransformations();
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        break;
                                    case EXHIBITION:
                                        this.transformFirstPersonItem(f / 2, 0);
                                        GlStateManager.rotate(-var16 * 40.0F / 2.0F, var16 / 2.0F, -0.0F, 9.0F);
                                        GlStateManager.rotate(-var16 * 30.0F, 1.0F, var16 / 2.0F, -0.0F);
                                        this.doBlockTransformations();
                                        GL11.glTranslatef(-0.05F, this.mc.thePlayer.isSneaking() ? -0F : 0.0F, 0.1F);
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        break;
                                    case NOVOLINE:
                                        this.transformFirstPersonItem(f / 1.5F, 0.0F);
                                        GlStateManager.translate(-0.5F, 0.2F, 0.0F);
                                        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
                                        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
                                        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                        GlStateManager.translate(-0.05F, 0.3F, 0.3F);
                                        GlStateManager.rotate(-var16 * 140.0F, 8.0F, 0.0F, 8.0F);
                                        GlStateManager.rotate(var16 * 90.0F, 8.0F, 0.0F, 8.0F);
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        break;
                                    case SPIN:
                                        transformFirstPersonItem(f, 0.0F);
                                        GlStateManager.translate(0, 0.2F, -1);
                                        GlStateManager.rotate(-59, -1, 0, 3);
                                        GlStateManager.rotate(-(System.currentTimeMillis() / 2 % 360), 1, 0, 0.0F);
                                        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        break;
                                    case LEAKED:
                                        GlStateManager.translate(.0f, -.03f, -.13f);
                                        transformFirstPersonItem(f / 3F, 0.0F);
                                        GlStateManager.translate(0.0f, 0.1F, 0.0F);
                                        doBlockTransformations();
                                        GlStateManager.rotate(var16 * 20.0F / 2.0F, 0.0F, 1.0F, 1.5F);
                                        GlStateManager.rotate(-var16 * 200.0F / 4.0F, 1.0f, 0.9F, 0.0F);
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        break;
                                    case SMOOTH:
                                        transformFirstPersonItem(f / 2.0F - 0.18F, 0.0F);
                                        if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled())
                                            GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                                        final float swing = MathHelper.sin((float)
                                                (MathHelper.sqrt_float(f1) * Math.PI));

                                        GL11.glRotatef(-swing * 80.0f / 5.0f,
                                                swing / 3.0f, -0.0f, 9.0f);
                                        GL11.glRotatef(-swing * 40.0f, 8.0f,
                                                swing / 9.0f, -0.1f);
                                        doBlockTransformations();
                                        break;
                                }
                            } else {
                                this.transformFirstPersonItem(f, 0.0F);
                                this.doBlockTransformations();
                                GlStateManager.scale(CameraModule.scale.getValue(),
                                        CameraModule.scale.getValue(), CameraModule.scale.getValue());
                            }
                            break;

                        case BOW:
                            this.transformFirstPersonItem(f, 0.0F);
                            this.doBowTransformations(partialTicks, abstractclientplayer);
                            GlStateManager.scale(CameraModule.scale.getValue(),
                                    CameraModule.scale.getValue(), CameraModule.scale.getValue());
                    }
                } else {
                    if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled() && CameraModule.fluxSwing.getValue()) {
                        this.transformFirstPersonItem(f, f1);
                    } else {
                        this.doItemUsedTransformations(f1);
                        this.transformFirstPersonItem(f, f1);
                    }
                    GlStateManager.scale(CameraModule.scale.getValue(), CameraModule.scale.getValue(), CameraModule.scale.getValue());
                }

                this.renderItem(abstractclientplayer, this.itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
            } else if (!abstractclientplayer.isInvisible()) {
                this.renderPlayerArm(abstractclientplayer, f, f1);
            }

            GlStateManager.popMatrix();
            GlStateManager.disableRescaleNormal();
            RenderHelper.disableStandardItemLighting();
        }
    }

    public void Swing() {
        if (mc.thePlayer.getItemInUseCount() > 0) {
            if (mc.objectMouseOver != null) {
                mc.thePlayer.swingItemClient();
            }
        }
    }

    private void func_178103_d(float qq) {
        GlStateManager.translate(-0.5F, qq, 0.0F);
        GlStateManager.rotate(30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
    }

    public void renderOverlays(float partialTicks) {
        GlStateManager.disableAlpha();

        if (this.mc.thePlayer.isEntityInsideOpaqueBlock()) {
            IBlockState iblockstate = this.mc.theWorld.getBlockState(new BlockPos(this.mc.thePlayer));
            BlockPos blockpos = new BlockPos(this.mc.thePlayer);
            EntityPlayer entityplayer = this.mc.thePlayer;

            for (int i = 0; i < 8; ++i) {
                double d0 = entityplayer.posX + (double) (((float) ((i >> 0) % 2) - 0.5F) * entityplayer.width * 0.8F);
                double d1 = entityplayer.posY + (double) (((float) ((i >> 1) % 2) - 0.5F) * 0.1F);
                double d2 = entityplayer.posZ + (double) (((float) ((i >> 2) % 2) - 0.5F) * entityplayer.width * 0.8F);
                BlockPos blockpos1 = new BlockPos(d0, d1 + (double) entityplayer.getEyeHeight(), d2);
                IBlockState iblockstate1 = this.mc.theWorld.getBlockState(blockpos1);

                if (iblockstate1.getBlock().isVisuallyOpaque()) {
                    iblockstate = iblockstate1;
                    blockpos = blockpos1;
                }
            }

            if (iblockstate.getBlock().getRenderType() != -1) {
                this.renderBlockInHand(partialTicks, this.mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(iblockstate));
            }
        }

        if (!this.mc.thePlayer.isSpectator()) {
            if (this.mc.thePlayer.isInsideOfMaterial(Material.water)) {
                this.renderWaterOverlayTexture(partialTicks);
            }

            if (this.mc.thePlayer.isBurning()) {
                if (Yuri.INSTANCE.getModuleManager().getModule(CameraModule.class).isEnabled() && CameraModule.noFireOverlay.getValue())
                    return;
                this.renderFireInFirstPerson(partialTicks);
            }
        }

        GlStateManager.enableAlpha();
    }

    private void renderBlockInHand(float partialTicks, TextureAtlasSprite atlas) {
        this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        float f = 0.1F;
        GlStateManager.color(0.1F, 0.1F, 0.1F, 0.5F);
        GlStateManager.pushMatrix();
        float f1 = -1.0F;
        float f2 = 1.0F;
        float f3 = -1.0F;
        float f4 = 1.0F;
        float f5 = -0.5F;
        float f6 = atlas.getMinU();
        float f7 = atlas.getMaxU();
        float f8 = atlas.getMinV();
        float f9 = atlas.getMaxV();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(-1.0D, -1.0D, -0.5D).tex((double) f7, (double) f9).endVertex();
        worldrenderer.pos(1.0D, -1.0D, -0.5D).tex((double) f6, (double) f9).endVertex();
        worldrenderer.pos(1.0D, 1.0D, -0.5D).tex((double) f6, (double) f8).endVertex();
        worldrenderer.pos(-1.0D, 1.0D, -0.5D).tex((double) f7, (double) f8).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderWaterOverlayTexture(float partialTicks) {
        if (!Config.isShaders() || Shaders.isUnderwaterOverlay()) {
            this.mc.getTextureManager().bindTexture(RES_UNDERWATER_OVERLAY);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            float f = this.mc.thePlayer.getBrightness(partialTicks);
            GlStateManager.color(f, f, f, 0.5F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.pushMatrix();
            float f1 = 4.0F;
            float f2 = -1.0F;
            float f3 = 1.0F;
            float f4 = -1.0F;
            float f5 = 1.0F;
            float f6 = -0.5F;
            float f7 = -this.mc.thePlayer.rotationYaw / 64.0F;
            float f8 = this.mc.thePlayer.rotationPitch / 64.0F;
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(-1.0D, -1.0D, -0.5D).tex((double) (4.0F + f7), (double) (4.0F + f8)).endVertex();
            worldrenderer.pos(1.0D, -1.0D, -0.5D).tex((double) (0.0F + f7), (double) (4.0F + f8)).endVertex();
            worldrenderer.pos(1.0D, 1.0D, -0.5D).tex((double) (0.0F + f7), (double) (0.0F + f8)).endVertex();
            worldrenderer.pos(-1.0D, 1.0D, -0.5D).tex((double) (4.0F + f7), (double) (0.0F + f8)).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.disableBlend();
        }
    }

    private void renderFireInFirstPerson(float partialTicks) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.9F);
        GlStateManager.depthFunc(519);
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        float f = 1.0F;

        for (int i = 0; i < 2; ++i) {
            GlStateManager.pushMatrix();
            TextureAtlasSprite textureatlassprite = this.mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/fire_layer_1");
            this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            float f1 = textureatlassprite.getMinU();
            float f2 = textureatlassprite.getMaxU();
            float f3 = textureatlassprite.getMinV();
            float f4 = textureatlassprite.getMaxV();
            float f5 = (0.0F - f) / 2.0F;
            float f6 = f5 + f;
            float f7 = 0.0F - f / 2.0F;
            float f8 = f7 + f;
            float f9 = -0.5F;
            GlStateManager.translate((float) (-(i * 2 - 1)) * 0.24F, -0.3F, 0.0F);
            GlStateManager.rotate((float) (i * 2 - 1) * 10.0F, 0.0F, 1.0F, 0.0F);
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.setSprite(textureatlassprite);
            worldrenderer.pos((double) f5, (double) f7, (double) f9).tex((double) f2, (double) f4).endVertex();
            worldrenderer.pos((double) f6, (double) f7, (double) f9).tex((double) f1, (double) f4).endVertex();
            worldrenderer.pos((double) f6, (double) f8, (double) f9).tex((double) f1, (double) f3).endVertex();
            worldrenderer.pos((double) f5, (double) f8, (double) f9).tex((double) f2, (double) f3).endVertex();
            tessellator.draw();
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.depthFunc(515);
    }

    public void updateEquippedItem() {
        this.prevEquippedProgress = this.equippedProgress;
        EntityPlayer entityplayer = this.mc.thePlayer;
        ItemStack itemstack = entityplayer.inventory.getCurrentItem();
        int visualSlot = entityplayer.inventory.currentItem;
        if (SlotManager.isServerSwapActive()) {
            itemstack = SlotManager.getVisualStack();
            visualSlot = SlotManager.getOriginalSlot();
        }
        boolean flag = false;

        if (this.itemToRender != null && itemstack != null) {
            if (!this.itemToRender.getIsItemStackEqual(itemstack)) {
                if (Reflector.ForgeItem_shouldCauseReequipAnimation.exists()) {
                    boolean flag1 = Reflector.callBoolean(this.itemToRender.getItem(), Reflector.ForgeItem_shouldCauseReequipAnimation, new Object[]{this.itemToRender, itemstack, Boolean.valueOf(this.equippedItemSlot != visualSlot)});

                    if (!flag1) {
                        this.itemToRender = itemstack;
                        this.equippedItemSlot = visualSlot;
                        return;
                    }
                }

                flag = true;
            }
        } else if (this.itemToRender == null && itemstack == null) {
            flag = false;
        } else {
            flag = true;
        }

        float f2 = 0.4F;
        float f = flag ? 0.0F : 1.0F;
        float f1 = MathHelper.clamp_float(f - this.equippedProgress, -f2, f2);
        this.equippedProgress += f1;

        if (this.equippedProgress < 0.1F) {
            this.itemToRender = itemstack;
            this.equippedItemSlot = visualSlot;

            if (Config.isShaders()) {
                Shaders.setItemToRenderMain(itemstack);
            }
        }
    }

    public void resetEquippedProgress() {
        this.equippedProgress = 0.0F;
    }

    public void resetEquippedProgress2() {
        this.equippedProgress = 0.0F;
    }
}
