package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.MotionEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.managers.impl.RotationManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.player.InvUtils;
import ddlc.yuri.utils.player.RotationUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.GLUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.StringUtils;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(label = "Minigame Aim", description = "Aim bots for Hypixel minigames: Zombies and Halloween Simulator", category = ModuleCategory.MISC)
public final class MinigameAimModule extends Module {

    private static final Minecraft mc = IMinecraft.mc;

    public enum GameMode {
        ZOMBIE("Zombies"),
        HALLOWEEN("Halloween Simulator");

        private final String name;

        GameMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum FireMode {
        AUTO_FIRE("Auto Fire"),
        ON_HELD("On Held");

        private final String name;

        FireMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Hitbox {
        HITSCAN_HEAD("Hitscan Head"),
        HEAD("Head"),
        CHEST("Chest"),
        LEG("Leg");

        private final String name;

        Hitbox(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum PriorityMode {
        CLOSEST("Closest"),
        MAX_HEALTH("Max Health"),
        LOWEST_HEALTH("Lowest Health"),
        FOV("FOV");

        private final String name;

        PriorityMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<GameMode> gameMode = new ModeProperty<>("Game", GameMode.ZOMBIE);

    private final Property<Boolean> silent = new Property<Boolean>("Silent", true, this::isZombies);
    private final Property<Boolean> showPrediction = new Property<Boolean>("Show Prediction", true, this::isZombies);
    private final Property<Boolean> showFOV = new Property<Boolean>("Show FOV", true, this::isZombies);
    private final Property<Boolean> autoHeal = new Property<Boolean>("Auto Heal", true, this::isZombies);
    private final Property<Boolean> autoRevive = new Property<Boolean>("Auto Revive", true, this::isZombies);
    private final Property<Boolean> autoAmmo = new Property<Boolean>("Auto Ammo", false, this::isZombies);
    private final Property<Boolean> hud = new Property<Boolean>("Hud", true, this::isZombies);

    private final NumberProperty predictionScale = new NumberProperty("Prediction Scale", 1, 0, 2, 0.05, () -> isZombies() && showPrediction.getValue());
    private final NumberProperty predictionTicks = new NumberProperty("Prediction Ticks", 2, 0, 10, 1, () -> isZombies() && showPrediction.getValue());
    private final NumberProperty delay = new NumberProperty("Delay", 4, 0, 20, 1, () -> isZombies() && isAutoFireMode());
    private final NumberProperty bufferSize = new NumberProperty("Buffer", 3, 1, 10, 1, () -> isZombies() && showPrediction.getValue());
    private final NumberProperty fov = new NumberProperty("FOV", 90, 1, 180, 0.1, this::isZombies);
    private final NumberProperty health = new NumberProperty("Health", 3, 1, 20, 1, () -> isZombies() && autoHeal.getValue());

    private final ModeProperty<FireMode> fireMode = new ModeProperty<>("Aimbot Mode", FireMode.AUTO_FIRE, this::isZombies);
    private final ModeProperty<Hitbox> hitbox = new ModeProperty<>("Hitbox", Hitbox.HITSCAN_HEAD, this::isZombies);
    private final ModeProperty<PriorityMode> priorityMode = new ModeProperty<>("Priority", PriorityMode.CLOSEST, this::isZombies);

    private final NumberProperty range = new NumberProperty("Range", 4.5f, 1, 6, 0.5, this::isHalloween);
    private final NumberProperty rotationSpeed = new NumberProperty("Rotation Speed", 10, 0.5, 10, 0.5, this::isHalloween);

    private boolean sneakingForRevive;

    private final Map<EntityLivingBase, EntityDelta> deltaHashMap = new HashMap<>();
    public static EntityLivingBase target;
    private int shootDelay = 0;
    private float pendingYaw;
    private float pendingPitch;
    private boolean hasPendingRotation;

    private final List<BlockPos> skullList = new CopyOnWriteArrayList<>();
    private boolean looking;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!isZombies()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        setSuffix(priorityMode.getValue().toString());

        shootDelay++;

        handleAutoHeal();
        handleAutoAmmo();
        handleAutoRevive();

        boolean shouldAim = fireMode.getValue() == FireMode.AUTO_FIRE || (fireMode.getValue() == FireMode.ON_HELD && mc.gameSettings.keyBindUseItem.isKeyDown());
        if (!shouldAim) {
            target = null;
            return;
        }

        for (Entity e : mc.theWorld.getLoadedEntityList()) {
            if (!(e instanceof EntityLivingBase)) continue;
            EntityLivingBase ent = (EntityLivingBase) e;
            if (!isValidEntity(ent)) {
                deltaHashMap.remove(ent);
                continue;
            }

            double xDelta = ent.posX - ent.lastTickPosX;
            double zDelta = ent.posZ - ent.lastTickPosZ;

            if (Math.hypot(xDelta, zDelta) < 3) {
                deltaHashMap.putIfAbsent(ent, new EntityDelta(xDelta, zDelta));
                EntityDelta d = deltaHashMap.get(ent);
                if (d != null) d.logDeltas(xDelta, zDelta, mc.thePlayer.ticksExisted);
            }
        }

        target = null;
        double bestWeight = Double.NEGATIVE_INFINITY;
        Vec3 bestHit = null;

        for (Entity e : mc.theWorld.getLoadedEntityList()) {
            if (!(e instanceof EntityLivingBase)) continue;
            EntityLivingBase ent = (EntityLivingBase) e;
            if (!isValidEntity(ent)) continue;

            Vec3 hitVec = getHitVec(ent);
            if (hitVec == null) continue;

            double[] p = getPrediction(ent, predictionTicks.getValue().intValue(), predictionScale.getValue());

            float[] rot = RotationUtils.getRotationFromPosition(hitVec.xCoord + p[0], hitVec.yCoord + p[1], hitVec.zCoord + p[2]);

            double weight = getTargetWeight(ent, rot);
            if (weight == Double.NEGATIVE_INFINITY) continue;

            if (canBeSeen(hitVec)) weight += 5;

            if (weight > bestWeight) {
                bestWeight = weight;
                target = ent;
                bestHit = hitVec;
            }
        }

        hasPendingRotation = false;

        if (target != null && bestHit != null) {
            double[] p = getPrediction(target, predictionTicks.getValue().intValue(), predictionScale.getValue());
            float[] rot = RotationUtils.getRotationFromPosition(bestHit.xCoord + (float) p[0], bestHit.yCoord + (float) p[1], bestHit.zCoord + (float) p[2]);
            pendingYaw = rot[0];
            pendingPitch = MathHelper.clamp_float(rot[1], -90f, 90f);
            hasPendingRotation = true;

            if (silent.getValue()) {
                RotationManager.setRotations(pendingYaw, pendingPitch, 4.0, RotationManager.MovementFix.NORMAL);
            }

            if (shouldFire(pendingYaw, pendingPitch) && shootDelay >= Math.max(0, delay.getValue().intValue())) {
                mc.rightClickMouse();
                shootDelay = 0;
            }
        }

    }

    @EventHook
    public void onMotion(MotionEvent event) {
        if (!isHalloween()) return;
        if (!event.isPre() || !checkHalloweenStatus() || mc.thePlayer == null || mc.theWorld == null) return;

        EntityPlayer player = mc.thePlayer;
        double rangeValue = range.getValue();
        double threshold = rangeValue * 1.5;
        double reachSq = rangeValue * rangeValue;

        double eyeX = player.posX;
        double eyeY = player.posY + player.getEyeHeight();
        double eyeZ = player.posZ;

        boolean sentPacket = false;
        skullList.clear();

        for (TileEntity tileEntity : mc.theWorld.loadedTileEntityList) {
            if (!(tileEntity instanceof TileEntitySkull)) continue;

            TileEntitySkull skull = (TileEntitySkull) tileEntity;
            if (skull.getPlayerProfile() == null) continue;

            BlockPos skullPos = skull.getPos();
            double dx = eyeX - skullPos.getX();
            double dy = eyeY - skullPos.getY();
            double dz = eyeZ - skullPos.getZ();
            double distanceSq = dx * dx + dy * dy + dz * dz;

            if (!sentPacket && distanceSq < reachSq
                    && Math.abs(dx) < threshold && Math.abs(dy) < threshold && Math.abs(dz) < threshold) {

                float[] targetRotations = getRotations(skullPos, eyeX, eyeY, eyeZ);
                RotationManager.setRotations(targetRotations, rotationSpeed.getValue(), RotationManager.MovementFix.NORMAL);

                if (!looking) {
                    mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(
                            skullPos, 1, player.getHeldItem(), 0.5f, 0.5f, 0.5f));
                    looking = true;
                } else {
                    looking = false;
                }

                sentPacket = true;
            }

            skullList.add(skullPos);
        }
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        if (!isZombies()) return;
        if (!silent.getValue()) {
            applyRots();
        }
        renderHud();
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        if (!isZombies()) return;
        renderHud();
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (isZombies()) {
            renderZombiePrediction();
        }
        if (isHalloween()) {
            renderSkulls();
        }
    }

    private void renderZombiePrediction() {
        if (!showPrediction.getValue()) return;
        if (mc.theWorld == null) return;

        for (Entity e : mc.theWorld.getLoadedEntityList()) {
            if (!(e instanceof EntityLivingBase)) continue;
            EntityLivingBase ent = (EntityLivingBase) e;
            if (!isValidEntity(ent)) continue;

            double[] p = getPrediction(ent, predictionTicks.getValue().intValue(), predictionScale.getValue());

            final float partial = mc.timer.renderPartialTicks;
            double x = ent.prevPosX + (ent.posX - ent.prevPosX) * partial - mc.getRenderManager().renderPosX + p[0];
            double y = ent.prevPosY + (ent.posY - ent.prevPosY) * partial - mc.getRenderManager().renderPosY + p[1];
            double z = ent.prevPosZ + (ent.posZ - ent.prevPosZ) * partial - mc.getRenderManager().renderPosZ + p[2];

            AxisAlignedBB bb = ent.getEntityBoundingBox().offset(-ent.posX + x + mc.getRenderManager().renderPosX,
                    -ent.posY + y + mc.getRenderManager().renderPosY,
                    -ent.posZ + z + mc.getRenderManager().renderPosZ);

            GLUtils.start3D();
            RenderUtils.color(0xFF00FF00, 0.6f);
            RenderUtils.drawBoundingBox(bb);
            GLUtils.stop3D();
        }
    }

    private void renderSkulls() {
        if (!checkHalloweenStatus() || mc.thePlayer == null) return;

        EntityPlayer player = mc.thePlayer;

        double camX = player.lastTickPosX + (player.posX - player.lastTickPosX) * mc.timer.renderPartialTicks;
        double camY = player.lastTickPosY + (player.posY - player.lastTickPosY) * mc.timer.renderPartialTicks;
        double camZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * mc.timer.renderPartialTicks;

        for (BlockPos skullPos : skullList) {
            drawBox(skullPos, camX, camY, camZ, ColorManager.getColor());
        }
    }

    private void renderHud() {
        if (!hud.getValue() && !showFOV.getValue()) return;
        if (mc.thePlayer == null) return;

        ScaledResolution sr = new ScaledResolution(mc);
        double width = sr.getScaledWidth();
        double height = sr.getScaledHeight();

        if (showFOV.getValue()) {
            float screen_fov = mc.gameSettings.fovSetting;
            double aimbot_fov = fov.getValue();
            float ratio = (float) (Math.tan(Math.toRadians(aimbot_fov / 2)) * 0.05F / (Math.tan(Math.toRadians(screen_fov / 2)) * 0.05F));
            double bruh = width / 2 * ratio;
            RenderUtils.drawCircle((float) width / 2, (float) height / 2, (float) bruh, 12, ColorManager.getColor().getRGB());
        }

        if (hud.getValue()) {
            int currentHealth = (int) mc.thePlayer.getHealth() * 5;
            int currentAmmo = 0;

            ScaledResolution scaledRes = new ScaledResolution(mc);
            CustomFontRenderer smallFont = FontUtils.getFont("sf", 14);

            if (isHoldingWeapon()) {
                currentAmmo = mc.thePlayer.getCurrentEquippedItem().stackSize;
                if (isReloading() && mc.thePlayer.getCurrentEquippedItem().stackSize == 1) {
                    currentAmmo = 0;
                }
                smallFont.drawBorderedString(currentAmmo + "/" + (mc.thePlayer.experienceLevel - currentAmmo), scaledRes.getScaledWidth() / 2D - 15 - (int) smallFont.getStringWidth("30/30"), scaledRes.getScaledHeight_double() / 2 - 0.5, -1, new Color(0, 0,0, 200).getRGB());
            }
            smallFont.drawBorderedString(currentHealth + "HP", scaledRes.getScaledWidth() / 2D + 15, scaledRes.getScaledHeight_double() / 2 - 0.5, -1, new Color(0, 0,0, 200).getRGB());
            if (isReloading() && currentAmmo == 0 && mc.thePlayer.experienceLevel <= 0) {
                smallFont.drawBorderedString("No Ammo", scaledRes.getScaledWidth() / 2D - (double) (int) smallFont.getStringWidth("No Ammo") / 2, scaledRes.getScaledHeight_double() / 2 + 15, new Color(255, 122, 122).getRGB(), new Color(0, 0,0, 200).getRGB());
            } else if (isReloading())
                smallFont.drawBorderedString("Reloading", scaledRes.getScaledWidth() / 2D - (double) (int) smallFont.getStringWidth("Reloading") / 2, scaledRes.getScaledHeight_double() / 2 + 15, new Color(91, 255, 51).getRGB(), new Color(0, 0,0, 200).getRGB());
        }
    }

    private boolean isZombies() {
        return gameMode.getValue() == GameMode.ZOMBIE;
    }

    private boolean isHalloween() {
        return gameMode.getValue() == GameMode.HALLOWEEN;
    }

    private void drawBox(BlockPos pos, double camX, double camY, double camZ, Color color) {
        double x = pos.getX() - camX, y = pos.getY() - camY, z = pos.getZ() - camZ;

        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableDepth();
        GL11.glLineWidth(2f);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int r = color.getRed(), g = color.getGreen(), b = color.getBlue();
        double[][] corners = {
                {x, y, z}, {x + 1, y, z}, {x + 1, y, z + 1}, {x, y, z + 1}, {x, y, z},
                {x, y + 1, z}, {x + 1, y + 1, z}, {x + 1, y + 1, z + 1}, {x, y + 1, z + 1}, {x, y + 1, z}
        };
        for (double[] corner : corners) {
            worldRenderer.pos(corner[0], corner[1], corner[2]).color(r, g, b, 255).endVertex();
        }
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.popMatrix();
    }

    private boolean checkHalloweenStatus() {
        if (mc.theWorld == null) return false;
        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) return false;
        String title = StringUtils.stripControlCodes(objective.getDisplayName());
        return title != null && title.startsWith("HALLOWEEN SIMULATOR");
    }

    private float[] getRotations(BlockPos point, double eyeX, double eyeY, double eyeZ) {
        double x = point.getX() + 0.5 - eyeX;
        double y = point.getY() + 0.5 - eyeY;
        double z = point.getZ() + 0.5 - eyeZ;
        double dist = Math.sqrt(x * x + z * z);

        float yaw = (float) Math.toDegrees(Math.atan2(z, x)) - 90f;
        float pitch = (float) Math.toDegrees(-Math.atan2(y, dist));

        return new float[]{yaw, pitch};
    }

    private boolean isValidEntity(EntityLivingBase entity) {
        return entity instanceof IAnimals && !(entity instanceof EntityVillager) && entity.getHealth() > 0;
    }

    private Vec3 getHitVec(EntityLivingBase entity) {
        double[] p = getPrediction(entity, predictionTicks.getValue().intValue(), predictionScale.getValue());
        double posX = entity.posX + p[0];
        double posY = entity.posY + p[1];
        double posZ = entity.posZ + p[2];

        List<Vec3> points = new ArrayList<>();
        boolean hitscan = hitbox.getValue() == Hitbox.HITSCAN_HEAD;

        if (hitscan || hitbox.getValue() == Hitbox.HEAD) {
            points.add(new Vec3(posX, posY + entity.getEyeHeight(), posZ));
        }

        if (hitscan || hitbox.getValue() == Hitbox.CHEST) {
            points.add(new Vec3(posX, posY + entity.getEyeHeight() / 2D, posZ));
        }

        if (hitscan || hitbox.getValue() == Hitbox.LEG) {
            points.add(new Vec3(posX, posY + entity.getEyeHeight() / 3D, posZ));
        }

        for (Vec3 point : points) {
            if (canBeSeen(point)) {
                return point;
            }
        }

        return null;
    }

    private boolean canBeSeen(Vec3 vec) {
        return mc.theWorld.rayTraceBlocks(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), vec, false, true, false) == null;
    }

    private double getTargetWeight(EntityLivingBase entity, float[] rotation) {
        double weight = -mc.thePlayer.getDistanceToEntity(entity);

        switch (priorityMode.getValue()) {
            case MAX_HEALTH:
                weight += entity.getMaxHealth();
                break;
            case LOWEST_HEALTH:
                weight -= entity.getHealth();
                break;
            case FOV:
                weight -= Math.hypot(MathHelper.wrapAngleTo180_float(rotation[0] - mc.thePlayer.rotationYaw), MathHelper.wrapAngleTo180_float(rotation[1] - mc.thePlayer.rotationPitch));
                break;
            case CLOSEST:
            default:
                break;
        }

        return weight;
    }

    private boolean shouldFire(float targetYaw, float targetPitch) {
        if (fireMode.getValue() == FireMode.ON_HELD && !mc.gameSettings.keyBindUseItem.isKeyDown()) {
            return false;
        }

        float currentYaw = !silent.getValue() ? mc.thePlayer.rotationYaw : RotationManager.rotations.x;
        float currentPitch = !silent.getValue() ? mc.thePlayer.rotationPitch : RotationManager.rotations.y;

        float yawDiff = Math.abs(MathHelper.wrapAngleTo180_float(targetYaw - currentYaw));
        float pitchDiff = Math.abs(MathHelper.wrapAngleTo180_float(targetPitch - currentPitch));

        return yawDiff < 3.0f && pitchDiff < 5.0f;
    }

    private boolean isAutoFireMode() {
        return fireMode.getValue() == FireMode.AUTO_FIRE;
    }

    private void applyRots() {
        if (!hasPendingRotation || mc.thePlayer == null) return;

        mc.thePlayer.rotationYaw = pendingYaw;
        mc.thePlayer.rotationPitch = pendingPitch;
    }

    private void handleAutoHeal() {
        if (!autoHeal.getValue() || mc.thePlayer == null || mc.theWorld == null) return;

        float threshold = health.getValue().floatValue();
        if (mc.thePlayer.getHealth() > threshold) return;

        int gappleSlot = findGoldenAppleInventorySlot();
        if (gappleSlot == -1) return;

        int currentSlot = mc.thePlayer.inventory.currentItem;
        boolean swapped = false;

        if (gappleSlot != currentSlot + 36) {
            InvUtils.swap(gappleSlot, currentSlot);
            swapped = true;
        }

        ItemStack stack = mc.thePlayer.inventory.getCurrentItem();
        if (stack != null) {
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(stack));
        }

        if (swapped) {
            InvUtils.swap(gappleSlot, currentSlot);
        }
    }

    private void handleAutoAmmo() {
        if (!autoAmmo.getValue() || mc.thePlayer == null) return;
        if (mc.thePlayer.getCurrentEquippedItem() == null || !(mc.thePlayer.getCurrentEquippedItem().getItem() instanceof net.minecraft.item.ItemBow))
            return;

        if (findArrowHotbarSlot() == -1) {
            int arrowSlot = findBestArrowInventorySlot();
            int emptyHotbar = findEmptyHotbarSlot();
            if (arrowSlot != -1 && emptyHotbar != -1) {
                InvUtils.swap(arrowSlot, emptyHotbar);
            }
        }
    }

    private void handleAutoRevive() {
        if (!autoRevive.getValue() || mc.thePlayer == null || mc.theWorld == null) return;

        boolean shouldSneak = false;
        for (Entity e : mc.theWorld.getLoadedEntityList()) {
            if (!(e instanceof EntityArmorStand)) continue;
            EntityArmorStand stand = (EntityArmorStand) e;
            if (!stand.hasCustomName()) continue;

            String text = StringUtils.stripControlCodes(stand.getDisplayName().getFormattedText()).toLowerCase();
            if ((text.contains("downed") || text.contains("revive") || text.matches(".*\\d+(\\.\\d+)?s$")) && mc.thePlayer.getDistanceToEntity(stand) < 2.5D) {
                shouldSneak = true;
                break;
            }
        }

        if (shouldSneak) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), true);
            KeyBinding.onTick(mc.gameSettings.keyBindSneak.getKeyCode());
            sneakingForRevive = true;
        } else if (sneakingForRevive && !Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.getKeyCode())) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
            KeyBinding.onTick(mc.gameSettings.keyBindSneak.getKeyCode());
            sneakingForRevive = false;
        }
    }

    private boolean isHoldingWeapon() {
        return mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().isItemStackDamageable();
    }

    private boolean isReloading() {
        return mc.thePlayer.inventory.getCurrentItem() != null && mc.thePlayer.inventory.getCurrentItem().isItemDamaged();
    }

    private int getHeldItemID() {
        return mc.thePlayer.inventory.getCurrentItem() == null ? -1 : Item.getIdFromItem(mc.thePlayer.inventory.getCurrentItem().getItem());
    }

    private int findArrowHotbarSlot() {
        for (int i = 36; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == Items.arrow) {
                return i - 36;
            }
        }
        return -1;
    }

    private int findGoldenAppleInventorySlot() {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == Items.golden_apple) {
                return i;
            }
        }
        return -1;
    }

    private int findBestArrowInventorySlot() {
        int bestSlot = -1;
        int bestSize = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() == Items.arrow && stack.stackSize > bestSize) {
                bestSize = stack.stackSize;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.thePlayer.inventory.getStackInSlot(i) == null) {
                return i;
            }
        }
        return -1;
    }

    private final double[] ZERO = new double[]{0, 0, 0};

    private double[] getPrediction(EntityLivingBase player, int ticks, double scale) {
        if (!deltaHashMap.containsKey(player) || (player.lastTickPosX == player.posX && player.lastTickPosZ == player.posZ && player.lastTickPosY == player.posY)) {
            return ZERO;
        }

        EntityDelta delta = deltaHashMap.get(player);
        double[] weighted = delta.getWeightedDeltas();

        double yDelta = (player.posY - player.lastTickPosY);
        if (Math.abs(Math.round(yDelta * 10d) / 10d) != 0 || !player.onGround) {
            yDelta -= 0.08D;
            yDelta *= 0.9800000190734863D;
        }

        if (yDelta >= 0.5) yDelta = 0;

        double lastMotionY = yDelta;
        double currentPos = 0;
        double finalX = 0, finalY = 0, finalZ = 0;

        for (int i = 0; i < ticks; i++) {
            double motionX = (weighted[0] * (i));
            double motionZ = (weighted[1] * (i));

            double motionY = lastMotionY;

            AxisAlignedBB playerBoundingBox = player.getEntityBoundingBox();
            AxisAlignedBB tempBoundingBox = new AxisAlignedBB(playerBoundingBox.minX, playerBoundingBox.minY, playerBoundingBox.minZ, playerBoundingBox.maxX, playerBoundingBox.maxY, playerBoundingBox.maxZ).offset(motionX, currentPos, motionZ);

            final List<AxisAlignedBB> var15 = mc.theWorld.getCollidingBoundingBoxes(player, tempBoundingBox.addCoord(0, motionY, 0));
            for (final AxisAlignedBB var18 : var15) {
                motionY = var18.calculateYOffset(tempBoundingBox, motionY);
            }

            currentPos += motionY;

            finalX = motionX;
            finalY = motionY;
            finalZ = motionZ;

            motionY -= 0.08D;
            motionY *= 0.9800000190734863D;

            lastMotionY = motionY;
        }

        return new double[]{finalX * scale, finalY, finalZ * scale};
    }

    @Override
    public void onEnable() {
        setSuffix(gameMode.getValue().toString());
    }

    @Override
    public void onDisable() {
        skullList.clear();
        looking = false;
        deltaHashMap.clear();
        target = null;
    }

    private class EntityDelta {
        private final ArrayBlockingQueue<double[]> deltas = new ArrayBlockingQueue<>(10);
        private int lastUpdatedTick;

        private EntityDelta(double initialDeltaX, double initialDeltaY) {
            deltas.add(new double[]{initialDeltaX, initialDeltaY});
        }

        private void logDeltas(double deltaX, double deltaY, int currentTick) {
            int tickDelay = Math.max(1, currentTick - lastUpdatedTick);

            if (currentTick - lastUpdatedTick > 3) {
                deltas.clear();
            }

            while (deltas.remainingCapacity() == 0 || deltas.size() > bufferSize.getValue().intValue()) {
                deltas.remove();
            }

            if (deltaX != 0 && deltaY != 0)
                lastUpdatedTick = currentTick;

            deltas.add(new double[]{deltaX / tickDelay, deltaY / tickDelay});
        }

        public double[] getWeightedDeltas() {
            if (deltas.isEmpty()) return new double[]{0, 0};
            int denominator = deltas.size();
            double deltaX = 0, deltaY = 0;
            for (double[] d : deltas) {
                deltaX += d[0];
                deltaY += d[1];
            }
            return new double[]{deltaX / denominator, deltaY / denominator};
        }
    }
}