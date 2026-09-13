package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketSendEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.render.Render3DEvent;
import ddlc.yuri.api.events.impl.world.LivingUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.managers.impl.ColorManager;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.player.MoveUtils;
import net.minecraft.block.BlockBed;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.List;

@ModuleInfo(label = "Effects", category = ModuleCategory.RENDER, description = "Cute hearts, dots, bed break visuals and damage numbers")
public class EffectsModule extends Module {

    public final Property<Boolean> hearts = new Property<>("Hearts", true);
    public final NumberProperty heartRate = new NumberProperty("Hearts Spawn Rate", 200.0, 50.0, 500.0, 10.0, hearts::getValue);
    public final NumberProperty heartLifetime = new NumberProperty("Hearts Lifetime", 1500.0, 500.0, 4000.0, 100.0, hearts::getValue);

    public final Property<Boolean> dots = new Property<>("Dots", true);
    public final NumberProperty dotRate = new NumberProperty("Dots Spawn Rate", 100.0, 20.0, 200.0, 10.0, dots::getValue);
    public final NumberProperty dotLifetime = new NumberProperty("Dots Lifetime", 1500.0, 500.0, 5000.0, 100.0, dots::getValue);
    public final Property<Boolean> pulse = new Property<>("Pulse", false, dots::getValue);

    public final Property<Boolean> onlyWhileMoving = new Property<>("Only While Moving", true);
    public final NumberProperty opacity = new NumberProperty("Opacity", 85.0, 20.0, 100.0, 5.0);

    public final Property<Boolean> bedBurst = new Property<>("Bed Burst", true);
    public final NumberProperty burstCount = new NumberProperty("Bed Burst Count", 20.0, 5.0, 40.0, 1.0, bedBurst::getValue);
    public final NumberProperty burstSize = new NumberProperty("Bed Burst Size", 0.2, 0.05, 0.6, 0.01, bedBurst::getValue);
    public final NumberProperty burstSpeed = new NumberProperty("Bed Burst Speed", 2.5, 0.5, 7.0, 0.1, bedBurst::getValue);
    public final NumberProperty burstLifetime = new NumberProperty("Bed Burst Lifetime", 1500.0, 500.0, 3000.0, 100.0, bedBurst::getValue);

    public final Property<Boolean> rainbow = new Property<>("Rainbow", true);
    public final NumberProperty rainbowWidth = new NumberProperty("Rainbow Line Width", 5.0, 1.0, 12.0, 0.5, rainbow::getValue);
    public final NumberProperty rainbowDuration = new NumberProperty("Rainbow Duration", 3000.0, 1000.0, 6000.0, 200.0, rainbow::getValue);

    public final Property<Boolean> bedSound = new Property<>("Bed Sound", true);

    public final Property<Boolean> damageNumbers = new Property<>("Damage Numbers", true);
    public final Property<Boolean> damageParticles = new Property<>("Damage Particles", true);
    public final ModeProperty<ParticleMode> damageParticleMode = new ModeProperty<>("Damage Particle Mode", ParticleMode.HEART, damageParticles::getValue);

    public enum ParticleMode {
        HEART("Heart"),
        STAR("Star"),
        CIRCLE("Circle");

        public final String name;

        ParticleMode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final ResourceLocation HEART_TEX = new ResourceLocation("yuri/gui/heart.png");
    private static final ResourceLocation STAR_TEX = new ResourceLocation("yuri/gui/star.png");
    private static final ResourceLocation CIRCLE_TEX = new ResourceLocation("yuri/gui/glow_circle.png");

    private static final double[] HEART_X = new double[31];
    private static final double[] HEART_Y = new double[31];
    private static final double[] CIRCLE_X = new double[9];
    private static final double[] CIRCLE_Y = new double[9];
    private static final double[] FILL_X = new double[7];
    private static final double[] FILL_Y = new double[7];
    private static final double[] STAR_X = new double[9];
    private static final double[] STAR_Y = new double[9];

    private static final double[] RAINBOW_RED = {0.85, 0.60, 0.50, 0.50, 1.00, 1.00, 1.00};
    private static final double[] RAINBOW_GREEN = {0.50, 0.50, 0.75, 1.00, 0.90, 0.60, 0.40};
    private static final double[] RAINBOW_BLUE = {1.00, 1.00, 1.00, 0.65, 0.50, 0.40, 0.50};

    private static final double[] BURST_RED = {1.00, 1.00, 1.00, 0.50, 0.50, 0.60, 0.85};
    private static final double[] BURST_GREEN = {0.40, 0.60, 0.90, 1.00, 0.75, 0.50, 0.50};
    private static final double[] BURST_BLUE = {0.50, 0.40, 0.50, 0.65, 1.00, 1.00, 1.00};

    static {
        for (int i = 0; i <= 30; i++) {
            double t = (double) i / 30 * Math.PI * 2;
            double sin = Math.sin(t);
            HEART_X[i] = 16 * sin * sin * sin;
            HEART_Y[i] = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
        }
        for (int i = 0; i <= 8; i++) {
            double angle = (double) i / 8 * Math.PI * 2;
            CIRCLE_X[i] = Math.cos(angle);
            CIRCLE_Y[i] = Math.sin(angle);
        }
        for (int i = 0; i <= 6; i++) {
            double angle = (double) i / 6 * Math.PI * 2;
            FILL_X[i] = Math.cos(angle);
            FILL_Y[i] = Math.sin(angle);
        }
        for (int i = 0; i <= 8; i++) {
            double angle = i * Math.PI / 4 - Math.PI / 2;
            double radius = i % 2 == 0 ? 12 : 5;
            STAR_X[i] = Math.cos(angle) * radius;
            STAR_Y[i] = Math.sin(angle) * radius;
        }
    }

    private static class Particle {
        double x, y, z, vx, vy, vz;
        long time;
        float scale, spin, tilt;
        int type;
    }

    private static class DamageParticle {
        double x, y, z;
        double targetX, targetY, targetZ;
        long spawnTime;
        float alpha;

        public DamageParticle(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = x + (Math.random() - 0.5) * 2.0;
            this.targetY = y + (Math.random() - 0.5) * 2.0;
            this.targetZ = z + (Math.random() - 0.5) * 2.0;
            this.spawnTime = System.currentTimeMillis();
            this.alpha = 0.0f;
        }

        public void update() {
            this.x += (targetX - x) * 0.1;
            this.y += (targetY - y) * 0.1;
            this.z += (targetZ - z) * 0.1;
            if (this.alpha < 1.0f) {
                this.alpha = Math.min(1.0f, this.alpha + 0.1f);
            }
        }
    }

    private final List<Particle> heartList = new ArrayList<>();
    private final List<Particle> dotList = new ArrayList<>();
    private final List<Particle> burstList = new ArrayList<>();
    private final List<Particle> rainbowList = new ArrayList<>();
    private final List<DamageParticle> damageParticleList = new ArrayList<>();

    private final Random random = new Random();

    private long lastHeartSpawn;
    private long lastDotSpawn;
    private double lastX, lastY, lastZ;
    private boolean hasLastPosition;

    private boolean diggingBed;
    private double bedX, bedY, bedZ;

    private final HashMap<EntityLivingBase, Float> healthMap = new HashMap<>();
    private final ArrayDeque<DamageText> damageTexts = new ArrayDeque<>();

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null) return;

        if (!hearts.getValue()) heartList.clear();
        if (!dots.getValue()) dotList.clear();

        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        boolean canSpawnDots = hasLastPosition;

        if (!hasLastPosition) {
            lastX = x;
            lastY = y;
            lastZ = z;
            hasLastPosition = true;
        }

        if (!hearts.getValue() && !dots.getValue()) {
            lastX = x;
            lastY = y;
            lastZ = z;
            return;
        }

        if (onlyWhileMoving.getValue() && !MoveUtils.isMoving()) return;

        long now = System.currentTimeMillis();

        if (hearts.getValue() && now - lastHeartSpawn >= heartRate.getValue()) {
            lastHeartSpawn = now;
            spawnHeart(x, y + 0.5, z, now);
        }

        if (dots.getValue() && canSpawnDots && now - lastDotSpawn >= dotRate.getValue()) {
            lastDotSpawn = now;
            spawnDot(y, now);
        }

        lastX = x;
        lastY = y;
        lastZ = z;
    }

    @EventHook
    public void onLivingUpdate(LivingUpdateEvent e) {
        if (!damageNumbers.getValue() && !damageParticles.getValue()) return;
        if (e.getEntity() == null || !(e.getEntity() instanceof EntityLivingBase)) {
            return;
        }
        EntityLivingBase entity = (EntityLivingBase) e.getEntity();
        if (entity == mc.thePlayer) return;

        if (!this.healthMap.containsKey(entity)) this.healthMap.put(entity, entity.getHealth());
        float floatValue = this.healthMap.get(entity);
        float health = entity.getHealth();

        if (floatValue != health) {
            double diff = floatValue - health;
            boolean isHeal = diff < 0.0;

            if (!isHeal && damageParticles.getValue()) {
                spawnDamageParticles(entity);
            }

            if (damageNumbers.getValue()) {
                double amount = isHeal ? -diff : diff;
                String text = (isHeal ? "+" : "-") + roundToPlace(amount, 1);
                DamageLocation location = new DamageLocation(entity);
                location.setY(entity.getEntityBoundingBox().minY + (entity.getEntityBoundingBox().maxY - entity.getEntityBoundingBox().minY) / 2.0);
                location.setX(location.getX() - 0.5 + new Random(System.currentTimeMillis()).nextInt(5) * 0.1);
                location.setZ(location.getZ() - 0.5 + new Random(System.currentTimeMillis() + 1).nextInt(5) * 0.1);
                this.damageTexts.add(new DamageText(location, text));
            }

            this.healthMap.remove(entity);
            this.healthMap.put(entity, entity.getHealth());
        }
    }

    private void spawnDamageParticles(EntityLivingBase entity) {
        double x = entity.posX;
        double y = entity.posY + entity.height / 2.0;
        double z = entity.posZ;
        for (int i = 0; i < 5; i++) {
            damageParticleList.add(new DamageParticle(x, y, z));
        }
        while (damageParticleList.size() > 200) {
            damageParticleList.remove(0);
        }
    }

    private void spawnHeart(double x, double y, double z, long now) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = random.nextDouble() * 1.5;

        Particle heart = new Particle();
        heart.x = x + Math.cos(angle) * distance;
        heart.y = y + random.nextDouble() * 0.5;
        heart.z = z + Math.sin(angle) * distance;
        heart.time = now;
        heart.spin = (float) (random.nextDouble() * 360);
        heart.tilt = (float) (random.nextDouble() * 30 - 15);
        heart.scale = (float) (0.15 * (0.6 + random.nextDouble() * 0.8));
        heart.type = random.nextInt(3);

        heartList.add(heart);
        while (heartList.size() > 50) heartList.remove(0);
    }

    private void spawnDot(double y, long now) {
        Particle dot = new Particle();
        dot.x = lastX + (random.nextDouble() - 0.5) * 0.9;
        dot.y = y + 0.3 + random.nextDouble() * 1.2;
        dot.z = lastZ + (random.nextDouble() - 0.5) * 0.9;
        dot.vx = (random.nextDouble() - 0.5) * 0.3;
        dot.vy = (0.3 + random.nextDouble() * 0.7) * 0.3;
        dot.vz = (random.nextDouble() - 0.5) * 0.3;
        dot.time = now;
        dot.scale = (float) (0.04 * (0.5 + random.nextDouble()));
        dot.type = random.nextInt(4);

        dotList.add(dot);
        while (dotList.size() > 100) dotList.remove(0);
    }

    @EventHook
    public void onPacketSend(PacketSendEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Packet<?> packet = event.getPacket();
        if (!(packet instanceof C07PacketPlayerDigging)) return;

        C07PacketPlayerDigging digging = (C07PacketPlayerDigging) packet;
        BlockPos position = digging.getPosition();
        if (digging.getStatus() == null || position == null) return;

        if (digging.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
            if (!(mc.theWorld.getBlockState(position).getBlock() instanceof BlockBed)) {
                diggingBed = false;
                return;
            }

            double x = position.getX() + 0.5;
            double y = position.getY() + 0.5;
            double z = position.getZ() + 0.5;

            if (mc.thePlayer.capabilities.isCreativeMode) {
                diggingBed = false;
                spawnBedBreak(x, y, z);
            } else {
                diggingBed = true;
                bedX = x;
                bedY = y;
                bedZ = z;
            }
        } else if (digging.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
            if (diggingBed) {
                spawnBedBreak(bedX, bedY, bedZ);
                diggingBed = false;
            }
        } else if (digging.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
            diggingBed = false;
        }
    }

    private void spawnBedBreak(double x, double y, double z) {
        long now = System.currentTimeMillis();

        if (rainbow.getValue()) {
            Particle arc = new Particle();
            arc.x = x;
            arc.y = y;
            arc.z = z;
            arc.time = now;
            arc.spin = (float) Math.toDegrees(Math.atan2(mc.thePlayer.posX - x, mc.thePlayer.posZ - z));

            rainbowList.add(arc);
            while (rainbowList.size() > 5) rainbowList.remove(0);
        }

        if (bedBurst.getValue()) {
            int count = burstCount.getValue().intValue();
            double speed = burstSpeed.getValue();
            double size = burstSize.getValue();

            for (int i = 0; i < count; i++) {
                double theta = random.nextDouble() * Math.PI * 2;
                double phi = random.nextDouble() * Math.PI * 0.67 - Math.PI / 6;
                double particleSpeed = (0.8 + random.nextDouble() * 1.2) * speed;
                double cosPhi = Math.cos(phi);

                Particle particle = new Particle();
                particle.x = x;
                particle.y = y;
                particle.z = z;
                particle.vx = cosPhi * Math.cos(theta) * particleSpeed;
                particle.vy = Math.sin(phi) * particleSpeed + 1.0;
                particle.vz = cosPhi * Math.sin(theta) * particleSpeed;
                particle.time = now;
                particle.scale = (float) (size * (0.6 + random.nextDouble() * 0.8));

                int roll = random.nextInt(5);
                particle.type = roll < 2 ? 0 : roll - 1;

                burstList.add(particle);
                while (burstList.size() > 200) burstList.remove(0);
            }
        }

        if (bedSound.getValue()) {
            mc.thePlayer.playSound("random.orb", 1.0f, 1.5f);
            mc.thePlayer.playSound("random.levelup", 0.5f, 2.0f);
        }
    }

    @EventHook
    public void onRender3D(Render3DEvent event) {
        if (mc.thePlayer == null) return;
        if (heartList.isEmpty() && dotList.isEmpty() && burstList.isEmpty() && rainbowList.isEmpty() && damageTexts.isEmpty() && damageParticleList.isEmpty())
            return;

        double camX = mc.getRenderManager().viewerPosX;
        double camY = mc.getRenderManager().viewerPosY;
        double camZ = mc.getRenderManager().viewerPosZ;
        long now = System.currentTimeMillis();
        double alphaScale = opacity.getValue() / 100.0;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);

        renderHearts(camX, camY, camZ, now, alphaScale);
        renderDots(camX, camY, camZ, now, alphaScale);
        renderRainbows(camX, camY, camZ, now);
        renderBurst(camX, camY, camZ, now);

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.resetColor();

        renderDamageParticles(camX, camY, camZ, now);
        renderDamageNumbers();
    }

    private void renderDamageParticles(double camX, double camY, double camZ, long now) {
        if (damageParticleList.isEmpty() || !damageParticles.getValue()) return;

        ResourceLocation texture;
        switch (damageParticleMode.getValue()) {
            case STAR:
                texture = STAR_TEX;
                break;
            case CIRCLE:
                texture = CIRCLE_TEX;
                break;
            case HEART:
            default:
                texture = HEART_TEX;
                break;
        }

        mc.getTextureManager().bindTexture(texture);
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableCull();

        Iterator<DamageParticle> iterator = damageParticleList.iterator();
        long maxLifetime = 1500;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        while (iterator.hasNext()) {
            DamageParticle particle = iterator.next();
            long age = now - particle.spawnTime;
            if (age > maxLifetime) {
                iterator.remove();
                continue;
            }

            particle.update();

            double progress = (double) age / maxLifetime;
            float scale = (float) (0.35 * (1.0 - progress));
            float alpha = (float) ((1.0 - progress) * particle.alpha);

            Color color = ColorManager.getColor();
            float red = color.getRed() / 255.0f;
            float green = color.getGreen() / 255.0f;
            float blue = color.getBlue() / 255.0f;

            GlStateManager.pushMatrix();
            GlStateManager.translate(particle.x - camX, particle.y - camY, particle.z - camZ);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
            GlStateManager.rotate(mc.getRenderManager().playerViewX, (mc.gameSettings.thirdPersonView == 2 ? -1.0f : 1.0f), 0.0f, 0.0f);

            GlStateManager.color(red, green, blue, alpha);

            worldrenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(-scale, -scale, 0.0).tex(0.0, 1.0).endVertex();
            worldrenderer.pos(scale, -scale, 0.0).tex(1.0, 1.0).endVertex();
            worldrenderer.pos(scale, scale, 0.0).tex(1.0, 0.0).endVertex();
            worldrenderer.pos(-scale, scale, 0.0).tex(0.0, 0.0).endVertex();
            tessellator.draw();

            GlStateManager.popMatrix();
        }

        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.resetColor();
    }

    private void renderHearts(double camX, double camY, double camZ, long now, double alphaScale) {
        if (heartList.isEmpty()) return;

        long lifetime = heartLifetime.getValue().longValue();
        Iterator<Particle> iterator = heartList.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            Particle heart = iterator.next();
            long age = now - heart.time;

            if (age > lifetime) {
                iterator.remove();
                continue;
            }

            double progress = (double) age / lifetime;
            double alpha = fade(progress, 0.1, 0.6) * alphaScale;
            double grow = fade(progress, 0.1, 0.8);

            double x = heart.x + Math.sin(age * 0.002 + index * 1.7) * 0.1 - camX;
            double y = heart.y + progress * 1.5 - camY;
            double z = heart.z + Math.cos(age * 0.0015 + index * 2.3) * 0.1 - camZ;

            double red = heart.type == 0 ? 1.0 : heart.type == 1 ? 1.0 : 0.9;
            double green = heart.type == 0 ? 0.5 : heart.type == 1 ? 0.3 : 0.4;
            double blue = heart.type == 0 ? 0.8 : heart.type == 1 ? 0.6 : 0.9;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate((float) Math.toDegrees(Math.atan2(-x, -z)), 0, 1, 0);
            GlStateManager.rotate((float) ((age * 0.1 + heart.spin) % 360), 0, 1, 0);
            GlStateManager.rotate(heart.tilt, 0, 0, 1);
            GL11.glLineWidth(4.0f);
            drawHeart(heart.scale * grow, alpha, red, green, blue);
            GlStateManager.popMatrix();

            index++;
        }
    }

    private void renderDots(double camX, double camY, double camZ, long now, double alphaScale) {
        if (dotList.isEmpty()) return;

        long lifetime = dotLifetime.getValue().longValue();
        GL11.glLineWidth(2.0f);

        Iterator<Particle> iterator = dotList.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            Particle dot = iterator.next();
            long age = now - dot.time;

            if (age > lifetime) {
                iterator.remove();
                continue;
            }

            double progress = (double) age / lifetime;
            double alpha = fade(progress, 0.1, 0.5) * alphaScale;

            if (pulse.getValue()) {
                alpha *= 0.5 + 0.5 * Math.sin(age * 0.01 * (3.0 + dot.type * 1.5) + index * 2.7);
            }

            if (alpha < 0.02) {
                index++;
                continue;
            }

            double seconds = age / 1000.0;
            double x = dot.x + dot.vx * seconds + Math.sin(seconds * 1.5 + index * 1.3) * 0.15 - camX;
            double y = dot.y + dot.vy * seconds - camY;
            double z = dot.z + dot.vz * seconds + Math.cos(seconds * 1.2 + index * 2.1) * 0.15 - camZ;

            double green = dot.type == 0 ? 0.45 : dot.type == 1 ? 0.6 : dot.type == 2 ? 0.3 : 0.75;
            double blue = dot.type == 0 ? 0.7 : dot.type == 1 ? 0.85 : dot.type == 2 ? 0.55 : 0.95;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate((float) Math.toDegrees(Math.atan2(-x, -z)), 0, 1, 0);
            GlStateManager.color(1.0f, (float) green, (float) blue, (float) alpha);

            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex3d(0, 0, 0);
            for (int i = 0; i <= 6; i++) {
                GL11.glVertex3d(FILL_X[i] * dot.scale, FILL_Y[i] * dot.scale, 0);
            }
            GL11.glEnd();

            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i <= 8; i++) {
                GL11.glVertex3d(CIRCLE_X[i] * dot.scale, CIRCLE_Y[i] * dot.scale, 0);
            }
            GL11.glEnd();

            GlStateManager.popMatrix();
            index++;
        }
    }

    private void renderRainbows(double camX, double camY, double camZ, long now) {
        if (rainbowList.isEmpty()) return;

        long duration = rainbowDuration.getValue().longValue();
        float width = rainbowWidth.getValue().floatValue();

        Iterator<Particle> iterator = rainbowList.iterator();

        while (iterator.hasNext()) {
            Particle arc = iterator.next();
            long age = now - arc.time;

            if (age > duration) {
                iterator.remove();
                continue;
            }

            double progress = (double) age / duration;
            double alpha = fade(progress, 0.15, 0.6);
            double sweep = progress < 0.2 ? Math.pow(progress / 0.2, 2.0) : 1.0;

            GlStateManager.pushMatrix();
            GlStateManager.translate(arc.x - camX, arc.y - camY, arc.z - camZ);
            GlStateManager.rotate(arc.spin, 0, 1, 0);

            for (int band = 0; band < 7; band++) {
                double radius = 3.0 + (band - 3) * 0.15;
                if (radius < 0.1) continue;

                drawArc(radius, RAINBOW_RED[band], RAINBOW_GREEN[band], RAINBOW_BLUE[band], alpha * 0.15, width + 3.0f, sweep);
                drawArc(radius, RAINBOW_RED[band], RAINBOW_GREEN[band], RAINBOW_BLUE[band], alpha * 0.30, width + 1.5f, sweep);
                drawArc(radius, RAINBOW_RED[band], RAINBOW_GREEN[band], RAINBOW_BLUE[band], alpha * 0.85, width, sweep);
            }

            if (sweep > 0.5) drawSparkles(now, sweep, alpha);

            GlStateManager.popMatrix();
        }
    }

    private void drawArc(double radius, double red, double green, double blue, double alpha, float width, double sweep) {
        GL11.glLineWidth(width);
        GlStateManager.color((float) red, (float) green, (float) blue, (float) alpha);

        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= 30; i++) {
            double angle = (double) i / 30 * Math.PI * sweep;
            GL11.glVertex3d(Math.cos(angle) * radius, Math.sin(angle) * radius, 0);
        }
        GL11.glEnd();
    }

    private void drawSparkles(long now, double sweep, double alpha) {
        double rotation = now * 0.003;
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);

        GlStateManager.color(1.0f, 1.0f, 0.8f, (float) (alpha * 0.7));

        for (int end = 0; end < 2; end++) {
            double angle = end == 0 ? 0.0 : Math.PI * sweep;
            double x = Math.cos(angle) * 3.0;
            double y = Math.sin(angle) * 3.0;

            drawRay(x, y, cos, sin);
            drawRay(x, y, -sin, cos);
            drawRay(x, y, -cos, -sin);
            drawRay(x, y, sin, -cos);
        }
    }

    private void drawRay(double x, double y, double dirX, double dirY) {
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex3d(x, y, 0);
        GL11.glVertex3d(x + dirX * 0.15, y + dirY * 0.15, 0);
        GL11.glEnd();
    }

    private void renderBurst(double camX, double camY, double camZ, long now) {
        if (burstList.isEmpty()) return;

        long lifetime = burstLifetime.getValue().longValue();
        GL11.glLineWidth(rainbowWidth.getValue().floatValue());

        Iterator<Particle> iterator = burstList.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            long age = now - particle.time;

            if (age > lifetime) {
                iterator.remove();
                continue;
            }

            double progress = (double) age / lifetime;
            double seconds = age / 1000.0;
            double alpha = fade(progress, 0.1, 0.7);
            double grow = fade(progress, 0.1, 0.7);

            double x = particle.x + particle.vx * seconds + Math.sin(age * 0.002 + index * 1.7) * 0.05 - camX;
            double y = particle.y + particle.vy * seconds - 1.5 * seconds * seconds - camY;
            double z = particle.z + particle.vz * seconds + Math.cos(age * 0.0015 + index * 2.3) * 0.05 - camZ;

            int color = index % 7;

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate((float) Math.toDegrees(Math.atan2(x, z)), 0, 1, 0);
            GlStateManager.rotate((float) (seconds * 40 + index * 60), 0, 0, 1);

            double size = particle.scale * grow;

            if (particle.type == 0) {
                drawHeart(size, alpha, BURST_RED[color], BURST_GREEN[color], BURST_BLUE[color]);
            } else if (particle.type == 1) {
                drawShape(STAR_X, STAR_Y, 8, size / 16.0, alpha, BURST_RED[color], BURST_GREEN[color], BURST_BLUE[color]);
            } else if (particle.type == 2) {
                drawShape(CIRCLE_X, CIRCLE_Y, 8, size / 2.0, alpha, BURST_RED[color], BURST_GREEN[color], BURST_BLUE[color]);
            } else {
                drawDiamond(size / 16.0, alpha, BURST_RED[color], BURST_GREEN[color], BURST_BLUE[color]);
            }

            GlStateManager.popMatrix();
            index++;
        }
    }

    private void renderDamageNumbers() {
        if (!damageNumbers.getValue() || damageTexts.isEmpty()) return;

        for (Iterator<DamageText> iterator = this.damageTexts.iterator(); iterator.hasNext(); ) {
            DamageText update = iterator.next();
            ++update.ticks;
            if (update.ticks <= 10)
                update.location.setY(update.location.getY() + update.ticks * 0.005);
            if (update.ticks > 20)
                iterator.remove();
        }

        for (DamageText p : this.damageTexts) {
            double x = p.location.getX();
            double n = x - mc.getRenderManager().renderPosX;
            double y = p.location.getY();
            double n2 = y - mc.getRenderManager().renderPosY;
            double z = p.location.getZ();
            double n3 = z - mc.getRenderManager().renderPosZ;
            GlStateManager.pushMatrix();
            GlStateManager.enablePolygonOffset();
            GlStateManager.doPolygonOffset(1.0f, -1500000.0f);
            GlStateManager.translate((float) n, (float) n2, (float) n3);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0f, 1.0f, 0.0f);
            float textY = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
            GlStateManager.rotate(mc.getRenderManager().playerViewX, textY, 0.0f, 0.0f);
            double size = 0.03;
            GlStateManager.scale(-size, -size, size);
            GL11.glDepthMask(false);
            mc.fontRendererObj.drawStringWithShadow(p.text, (float) -(mc.fontRendererObj.getStringWidth(p.text) / 2), (float) -(mc.fontRendererObj.FONT_HEIGHT - 1), ColorManager.getColor().getRGB());
            mc.fontRendererObj.drawStringWithShadow(p.text, (float) -(mc.fontRendererObj.getStringWidth(p.text) / 2), (float) -(mc.fontRendererObj.FONT_HEIGHT - 1), ColorManager.getColor().getRGB());
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glDepthMask(true);
            GlStateManager.doPolygonOffset(1.0f, 1500000.0f);
            GlStateManager.disablePolygonOffset();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    private void drawHeart(double size, double alpha, double red, double green, double blue) {
        drawShape(HEART_X, HEART_Y, 30, size / 16.0, alpha, red, green, blue);
    }

    private void drawShape(double[] shapeX, double[] shapeY, int segments, double scale, double alpha, double red, double green, double blue) {
        for (int layer = 2; layer >= 0; layer--) {
            double glow = scale * (1.0 + layer * 0.1);
            double layerAlpha = layer == 0 ? alpha * 0.9 : alpha * (0.25 / layer);

            GlStateManager.color((float) red, (float) green, (float) blue, (float) layerAlpha);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i <= segments; i++) {
                GL11.glVertex3d(shapeX[i] * glow, shapeY[i] * glow, 0);
            }
            GL11.glEnd();
        }
    }

    private void drawDiamond(double scale, double alpha, double red, double green, double blue) {
        for (int layer = 2; layer >= 0; layer--) {
            double glow = scale * (1.0 + layer * 0.1);
            double layerAlpha = layer == 0 ? alpha * 0.9 : alpha * (0.25 / layer);

            GlStateManager.color((float) red, (float) green, (float) blue, (float) layerAlpha);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex3d(0, 14 * glow, 0);
            GL11.glVertex3d(8 * glow, 0, 0);
            GL11.glVertex3d(0, -14 * glow, 0);
            GL11.glVertex3d(-8 * glow, 0, 0);
            GL11.glVertex3d(0, 14 * glow, 0);
            GL11.glEnd();
        }
    }

    private double fade(double progress, double fadeIn, double fadeOut) {
        if (progress < fadeIn) return progress / fadeIn;
        if (progress > fadeOut) return (1.0 - progress) / (1.0 - fadeOut);
        return 1.0;
    }

    public static double roundToPlace(double p_roundToPlace_0_, int p_roundToPlace_2_) {
        if (p_roundToPlace_2_ < 0) throw new IllegalArgumentException();
        return new BigDecimal(p_roundToPlace_0_).setScale(p_roundToPlace_2_, RoundingMode.HALF_UP).doubleValue();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        clearAll();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        clearAll();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        clearAll();
    }

    private void clearAll() {
        heartList.clear();
        dotList.clear();
        burstList.clear();
        rainbowList.clear();
        damageParticleList.clear();
        lastHeartSpawn = 0;
        lastDotSpawn = 0;
        hasLastPosition = false;
        diggingBed = false;
        healthMap.clear();
        damageTexts.clear();
    }

    private static class DamageText {
        public int ticks;
        public DamageLocation location;
        public String text;

        public DamageText(final DamageLocation location, final String text) {
            this.location = location;
            this.text = text;
            this.ticks = 0;
        }
    }

    private static class DamageLocation {
        private double x;
        private double y;
        private double z;

        public DamageLocation(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public DamageLocation(EntityLivingBase entity) {
            this.x = entity.posX;
            this.y = entity.posY;
            this.z = entity.posZ;
        }

        public double getX() {
            return x;
        }

        public void setX(double x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }

        public double getZ() {
            return z;
        }

        public void setZ(double z) {
            this.z = z;
        }
    }

}