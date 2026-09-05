package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.managers.impl.TargetManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.modules.impl.render.targethud.TargetHudMode;
import ddlc.yuri.modules.impl.render.targethud.impl.*;
import ddlc.yuri.utils.render.DragUtils;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

import java.util.*;

@ModuleInfo(label = "Target HUD",
        description = "Displays information about your current target(s) on the screen.",
        category = ModuleCategory.RENDER)
public final class TargetHudModule extends Module {

    public enum Mode {
        YURI("Yuri"),
        ASTOLFO("Astolfo"),
        NOVOLINE("Novoline"),
        OLD_NOVOLINE("Old Novoline"),
        EXHIBITION("Exhibition"),
        OLD_EXHIBITION("Old Exhibition");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.YURI);
    private final Property<Boolean> grid = new Property<Boolean>("Grid", true);
    public final Property<Boolean> showPrevious = new Property<Boolean>("Show Previous", true);
    private final Map<Mode, TargetHudMode> modeMap = new HashMap<>();

    private static boolean positionInitialized = false;

    private static final int MAX_COLS = 3;
    private static final int MAX_ROWS = 4;
    private static final int INFO_SPACING_X = 145;
    private static final int INFO_SPACING_Y = 53;


    private long lastRender2DTime = 0;
    private final Map<UUID, TargetState> targetStates = new LinkedHashMap<>();
    private final Random particleRandom = new Random();
    private final Set<UUID> activeTargetsThisFrame = new HashSet<>();
    private final List<EntityLivingBase> listToRender = new ArrayList<>();
    private final List<TargetState> allRenderStates = new ArrayList<>();

    public TargetHudModule() {
        modeMap.put(Mode.YURI, new YuriMode(this));
        modeMap.put(Mode.ASTOLFO, new AstolfoMode(this));
        modeMap.put(Mode.NOVOLINE, new NovolineMode(this));
        modeMap.put(Mode.OLD_NOVOLINE, new OldNovolineMode(this));
        modeMap.put(Mode.EXHIBITION, new ExhibitionMode(this));
        modeMap.put(Mode.OLD_EXHIBITION, new OldExhibitionMode(this));
    }

    private TargetHudMode getCurrentModeInstance() {
        return modeMap.get(mode.getValue());
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        targetStates.clear();
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onRender2D(Render2DEvent event) {
        long now = System.currentTimeMillis();
        float delta = lastRender2DTime == 0 ? 0f : (now - lastRender2DTime) / 500f;
        lastRender2DTime = now;

        if (delta < 0.0005f) return;

        activeTargetsThisFrame.clear();
        listToRender.clear();

        EntityLivingBase mainTarget = AuraModule.target;

        if (mc.currentScreen instanceof GuiChat) {
            mainTarget = mc.thePlayer;
        }

        if (mainTarget != null) {
            listToRender.add(mainTarget);
            activeTargetsThisFrame.add(mainTarget.getUniqueID());
        }

        if (grid.getValue()) {
            addAdditionalTargets(listToRender, activeTargetsThisFrame, mainTarget);
        }

        updateTargetStates(listToRender, activeTargetsThisFrame, delta);

        allRenderStates.clear();
        allRenderStates.addAll(targetStates.values());
        if (allRenderStates.isEmpty()) return;

        TargetHudMode modeInstance = getCurrentModeInstance();
        if (modeInstance == null) return;

        if (!positionInitialized && !DragUtils.components.containsKey("TargetHud")) {
            ScaledResolution sr = new ScaledResolution(mc);
            initializePosition(sr, modeInstance.getMinWidth());
        }

        DragUtils.DraggableComponent draggable = DragUtils.components.get("TargetHud");
        renderGrid(allRenderStates, draggable, modeInstance, now, delta);
    }

    @EventHook(EventPriority.VERY_HIGH)
    public void onShader2D(Shader2DEvent event) {
        if (event.getShaderType() != Shader2DEvent.ShaderType.BLUR) return;

        allRenderStates.clear();
        allRenderStates.addAll(targetStates.values());
        if (allRenderStates.isEmpty()) return;

        TargetHudMode modeInstance = getCurrentModeInstance();
        if (modeInstance == null) return;

        DragUtils.DraggableComponent draggable = DragUtils.components.get("TargetHud");
        if (draggable == null) return;

        renderBlurMask(allRenderStates, draggable, modeInstance);
    }

    private void renderBlurMask(List<TargetState> states, DragUtils.DraggableComponent draggable, TargetHudMode modeInstance) {
        boolean useGrid = grid.getValue();
        int panelWidth = modeInstance.getMinWidth();
        int panelHeight = modeInstance.getHudHeight() + modeInstance.getLabelHeight();

        for (int i = 0; i < states.size(); i++) {
            if (!useGrid && i > 0) break;

            int col = i % MAX_COLS;
            int row = i / MAX_COLS;

            double baseX = draggable.getX() + col * INFO_SPACING_X;
            double baseY = draggable.getY() + row * INFO_SPACING_Y;

            Gui.drawRect((int) baseX, (int) baseY, (int) (baseX + panelWidth), (int) (baseY + panelHeight), 0xFFFFFFFF);
        }
    }

    private void addAdditionalTargets(List<EntityLivingBase> list, Set<UUID> active, EntityLivingBase mainTarget) {
        if (mc.currentScreen instanceof GuiChat) return;

        try {
            AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
            if (aura.isEnabled() && !TargetManager.getTargetList().isEmpty()) {
                for (Entity target : TargetManager.getTargetList()) {
                    if (target instanceof EntityLivingBase) {
                        EntityLivingBase entity = (EntityLivingBase) target;
                        if (mainTarget == null || !entity.getUniqueID().equals(mainTarget.getUniqueID())) {
                            if (list.size() < MAX_COLS * MAX_ROWS) {
                                list.add(entity);
                                active.add(entity.getUniqueID());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            if (mainTarget == null && mc.thePlayer != null && mc.theWorld != null) {
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (entity instanceof EntityLivingBase && entity != mc.thePlayer) {
                        EntityLivingBase living = (EntityLivingBase) entity;
                        if (mc.thePlayer.getDistanceToEntity(entity) <= 6.0) {
                            if (list.size() < MAX_COLS * MAX_ROWS) {
                                list.add(living);
                                active.add(entity.getUniqueID());
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateTargetStates(List<EntityLivingBase> listToRender, Set<UUID> active, float delta) {
        for (EntityLivingBase entity : listToRender) {
            UUID id = entity.getUniqueID();
            targetStates.putIfAbsent(id, new TargetState(entity));
            TargetState state = targetStates.get(id);
            state.active = true;
            state.alpha = Math.min(1f, state.alpha + delta * 4f);
        }

        Iterator<Map.Entry<UUID, TargetState>> it = targetStates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TargetState> entry = it.next();
            if (!active.contains(entry.getKey())) {
                TargetState state = entry.getValue();
                state.active = false;
                state.alpha -= delta * 4f;
                if (state.alpha <= 0f) {
                    it.remove();
                    continue;
                }
            }
            updateParticles(entry.getValue(), delta);
        }
    }

    private void updateParticles(TargetState state, float delta) {
        if (state.particles.isEmpty()) return;

        float seconds = delta * 0.5f;

        Iterator<HealthParticle> it = state.particles.iterator();
        while (it.hasNext()) {
            HealthParticle particle = it.next();

            particle.life -= seconds;
            if (particle.life <= 0f) {
                it.remove();
                continue;
            }

            particle.x += particle.vx * seconds;
            particle.y += particle.vy * seconds;
            particle.vy += 42f * seconds;
            particle.vx *= 1f - Math.min(1f, 1.6f * seconds);
        }
    }

    public void spawnHealthParticles(TargetState state, float damage, float x, float y, int spreadWidth, int count) {
        if (damage <= 0f || count <= 0) return;

        int amount = Math.max(1, Math.round(count * Math.min(damage / 2f, 3f)));
        int rgb = ColorManager.getColors().getFirst().getRGB() & 0x00FFFFFF;

        for (int i = 0; i < amount; i++) {
            float px = x + particleRandom.nextFloat() * spreadWidth;
            float py = y + particleRandom.nextFloat() * 2f;
            float vx = (particleRandom.nextFloat() - 0.5f) * 30f;
            float vy = -(6f + particleRandom.nextFloat() * 24f);
            float life = 0.45f + particleRandom.nextFloat() * 0.45f;

            state.particles.add(new HealthParticle(px, py, vx, vy, life, rgb));
        }

        while (state.particles.size() > 120) {
            state.particles.remove(0);
        }
    }

    public void renderParticles(TargetState state, double originX, double originY, float alpha) {
        if (state.particles.isEmpty() || alpha <= 0f) return;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();

        for (HealthParticle particle : state.particles) {
            float fade = Math.max(0f, particle.life / particle.maxLife);
            float particleAlpha = fade * alpha;
            if (particleAlpha <= 0.01f) continue;

            double left = particle.x - originX;
            double top = particle.y - originY;

            GlStateManager.color(
                    (particle.color >> 16 & 255) / 255F,
                    (particle.color >> 8 & 255) / 255F,
                    (particle.color & 255) / 255F,
                    particleAlpha);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2d(left, top);
            GL11.glVertex2d(left, top + 1.2);
            GL11.glVertex2d(left + 1.2, top + 1.2);
            GL11.glVertex2d(left + 1.2, top);
            GL11.glEnd();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.resetColor();
    }

    private void renderGrid(List<TargetState> states, DragUtils.DraggableComponent draggable, TargetHudMode modeInstance, long now, float delta) {
        boolean useGrid = grid.getValue();
        int totalColumns = useGrid ? Math.min(states.size(), MAX_COLS) : 1;
        int totalRows = useGrid ? (int) Math.ceil((double) states.size() / MAX_COLS) : 1;

        if (totalColumns == 0) totalColumns = 1;
        if (totalRows == 0) totalRows = 1;

        int totalWidth = modeInstance.getMinWidth() + (totalColumns - 1) * INFO_SPACING_X;
        int totalHeight = (modeInstance.getHudHeight() + modeInstance.getLabelHeight()) + (totalRows - 1) * INFO_SPACING_Y;

        draggable.setWidth(totalWidth);
        draggable.setHeight(totalHeight);

        for (int i = 0; i < states.size(); i++) {
            if (!useGrid && i > 0) break;

            TargetState state = states.get(i);
            EntityLivingBase entity = state.entity;
            if (entity == null) continue;

            int col = i % MAX_COLS;
            int row = i / MAX_COLS;

            int baseX = col * INFO_SPACING_X;
            int baseY = row * INFO_SPACING_Y;

            modeInstance.draw(entity, state, draggable.getX() + baseX, draggable.getY() + baseY, now, delta);
        }
    }

    private void initializePosition(ScaledResolution sr, int width) {
        DragUtils.components.put("TargetHud", new DragUtils.DraggableComponent(
                (sr.getScaledWidth() - width) / 2.0,
                (double) sr.getScaledHeight() / 10));
        positionInitialized = true;
    }

    public void renderTargetEquipment(EntityLivingBase targetEntity, int xOffset, int yOffset, float alpha) {
        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().zLevel = 0.0F;

        int itemX = xOffset;
        if (targetEntity.getHeldItem() != null) {
            renderItem(targetEntity.getHeldItem(), itemX, yOffset, alpha);
            itemX += 15;
        }

        for (int i = 3; i >= 0; i--) {
            ItemStack armor = targetEntity.getCurrentArmor(i);
            if (armor != null) {
                renderItem(armor, itemX, yOffset, alpha);
                itemX += 15;
            }
        }

        RenderHelper.disableStandardItemLighting();
        GlStateManager.popMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

    private void renderItem(ItemStack item, int itemX, int yOffset, float alpha) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(itemX, yOffset, 0);
        GlStateManager.scale(0.75f, 0.75f, 0.75f);

        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);

        mc.getRenderItem().renderItemAndEffectIntoGUI(item, 0, 0);
        mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRendererObj, item, 0, 0, null);

        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public void renderPlayerFace(AbstractClientPlayer player, float x, float y, float size, float scale, float tintAmount, float alpha) {
        GlStateManager.pushMatrix();
        float centerX = x + size / 2f;
        float centerY = y + size / 2f;
        GlStateManager.translate(centerX, centerY, 0);
        GlStateManager.scale(scale, scale, 1f);
        GlStateManager.translate(-centerX, -centerY, 0);

        GlStateManager.color(1f, 1f - tintAmount, 1f - tintAmount, alpha);

        mc.getTextureManager().bindTexture(player.getLocationSkin());
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8, 8, 8, 8, (int) size, (int) size, 64, 64);

        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    public void render3DEntity(EntityLivingBase entity, int x, int y, int scale, float sizeScale, float tintAmount, float alpha) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, 50.0F);
        GlStateManager.scale((float) (-scale) * sizeScale, (float) scale * sizeScale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F);

        RenderManager rendermanager = mc.getRenderManager();
        rendermanager.setRenderShadow(false);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1f, 1f - tintAmount, 1f - tintAmount, alpha);

        rendermanager.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, entity.rotationYaw, 1.0F);
        rendermanager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    public static final class TargetState {
        public EntityLivingBase entity;
        public float displayHealth = -1f;
        public float lastActualHealth = -1f;
        public float previousDisplayHealth = -1f;
        public long hurtAnimStart = 0;
        public float alpha = 0f;
        public boolean active = true;
        private final List<HealthParticle> particles = new ArrayList<>();

        TargetState(EntityLivingBase entity) {
            this.entity = entity;
            this.displayHealth = entity.getHealth();
            this.previousDisplayHealth = entity.getHealth();
            this.lastActualHealth = entity.getHealth();
        }
    }

    private static final class HealthParticle {
        float x, y, vx, vy, life, maxLife;
        int color;

        HealthParticle(float x, float y, float vx, float vy, float life, int color) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = life;
            this.color = color;
        }
    }
}