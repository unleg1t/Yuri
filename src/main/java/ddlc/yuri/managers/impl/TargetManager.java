package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PlayerAttackEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.utils.client.TimerUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

public class TargetManager {

    private static final Pattern TEAM_COLOR_PATTERN = Pattern.compile("\u00a7(.).*\u00a7r");
    private static final List<Entity> mutableTargetList = new ArrayList<>();

    @Getter
    @Setter
    private static EntityLivingBase target;
    @Getter
    private static List<Entity> targetList = mutableTargetList;
    private static final TimerUtils switchTimer = new TimerUtils();
    @Getter
    @Setter
    private static Mode mode;
    @Setter
    private static List<Targets> targets;
    @Getter
    @Setter
    private static float seekRange;
    @Getter
    @Setter
    private static int switchTime;
    private int targetIndex;

    public TargetManager(float seekRange) {
        mode = Mode.ADAPTIVE;
        targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);
        TargetManager.seekRange = seekRange;
        switchTime = 2;
    }

    public TargetManager() {
        mode = Mode.ADAPTIVE;
        targets = Arrays.asList(Targets.PLAYERS, Targets.HOSTILES);
        seekRange = 6.0f;
        switchTime = 2;
    }

    public static void configure(List<Targets> targets) {
        TargetManager.targets = targets;
    }

    public enum Mode {
        ADAPTIVE("Adaptive"),
        SWITCH("Switch"),
        SINGLE("Single");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Targets {
        PLAYERS("Players"),
        TEAMMATES("Teammates"),
        INVISIBLES("Invisibles"),
        HOSTILES("Hostiles"),
        ANIMALS("Animals");

        public final String name;

        Targets(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        targetList = getTargets();

        if (targetList.isEmpty()) {
            target = null;
            return;
        }

        selectTarget();
    }

    @EventHook
    public void onPlayerAttack(PlayerAttackEvent event) {
        event.target = target;
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        target = null;
    }

    private void selectTarget() {
        if (targetList.isEmpty()) {
            target = null;
            return;
        }

        if (mode.equals(Mode.SINGLE)) {
            target = (EntityLivingBase) targetList.get(0);
        } else if (mode.equals(Mode.SWITCH)) {
            if (targetIndex >= targetList.size()) {
                targetIndex = 0;
            }

            if (switchTimer.hasTimeElapsed(switchTime * 100)) {
                targetIndex = (targetIndex + 1) % targetList.size();
                switchTimer.reset();
            }
            target = (EntityLivingBase) targetList.get(targetIndex);
        } else if (mode.equals(Mode.ADAPTIVE)) {
            EntityLivingBase closest = null;
            double closestDist = Double.MAX_VALUE;
            for (Entity e : targetList) {
                double d = mc.thePlayer.getDistanceToEntity(e);
                if (d < closestDist) {
                    closestDist = d;
                    closest = (EntityLivingBase) e;
                }
            }
            target = closest;
        } else {
            throw new IllegalStateException("Unexpected value: " + this.mode);
        }
    }

    private List<Entity> getTargets() {
        mutableTargetList.clear();
        List<Entity> loaded = mc.theWorld.loadedEntityList;
        int size = loaded.size();
        for (int i = 0; i < size; i++) {
            Entity entity = loaded.get(i);
            if (entity == mc.thePlayer || entity.isDead || !(entity instanceof EntityLivingBase)) {
                continue;
            }
            if (((EntityLivingBase) entity).getHealth() <= 0) {
                continue;
            }
            if (mc.thePlayer.getDistanceToEntity(entity) > seekRange) {
                continue;
            }
            if (isValidEntity(entity)) {
                mutableTargetList.add(entity);
            }
        }
        return mutableTargetList;
    }

    private boolean isValidEntity(Entity entity) {
        if (entity instanceof EntityArmorStand) {
            return false;
        }

        if (entity.isInvisible() && !targets.contains(Targets.INVISIBLES)) {
            return false;
        }

        if (entity instanceof EntityPlayer) {
            boolean teammate = inTeam(mc.thePlayer, entity);
            if (teammate) {
                return targets.contains(Targets.TEAMMATES);
            }
            return targets.contains(Targets.PLAYERS);
        }

        if (targets.contains(Targets.HOSTILES) && entity instanceof EntityMob) return true;
        if (targets.contains(Targets.ANIMALS) && entity instanceof EntityAnimal) return true;

        return false;
    }

    public static boolean inTeam(@NonNull ICommandSender entity0, @NonNull ICommandSender entity1) {
        String s = "\u00a7" + teamColor(entity0);

        return entity0.getDisplayName().getFormattedText().contains(s)
                && entity1.getDisplayName().getFormattedText().contains(s);
    }

    private static @NonNull String teamColor(@NonNull ICommandSender player) {
        Matcher matcher = TEAM_COLOR_PATTERN.matcher(player.getDisplayName().getFormattedText());
        return matcher.find() ? matcher.group(1) : "f";
    }
}