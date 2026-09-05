package ddlc.yuri.managers.impl;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;
import ddlc.yuri.api.events.impl.client.ModuleEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.modules.impl.misc.ToggleSoundsModule;
import ddlc.yuri.utils.client.SoundUtils;
import ddlc.yuri.utils.render.notifications.NotificationRenderer;
import net.minecraft.client.Minecraft;

public class NotificationManager {

    @EventHook(value = EventPriority.VERY_HIGH)
    public void onRender(Render2DEvent e) {
        NotificationRenderer.update();
        NotificationRenderer.draw();
    }

    @EventHook(value = EventPriority.VERY_HIGH)
    public void onShader(Shader2DEvent e) {
        NotificationRenderer.draw();
    }

    @EventHook
    public void onModule(ModuleEvent e) {
        boolean enabled = e.getModule().isEnabled();

        if (Minecraft.getMinecraft().thePlayer != null &&
                Yuri.INSTANCE.getModuleManager().getModule(ToggleSoundsModule.class).isEnabled()) {
            switch (Yuri.INSTANCE.getModuleManager().getModule(ToggleSoundsModule.class).moduleToggleSounds.getValue()) {
                case EVISCERATE:
                    if (enabled) {
                        SoundUtils.playSound("eviscerate-enable.wav");
                    } else {
                        SoundUtils.playSound("eviscerate-disable.wav");
                    }
                    break;
                case NURSULTAN:
                    if (enabled) {
                        SoundUtils.playSound("nursultan-enable.wav");
                    } else {
                        SoundUtils.playSound("nursultan-disable.wav");
                    }
                    break;
                case AUGUSTUS:
                    if (enabled) {
                        SoundUtils.playSound("augustus-enable.wav");
                    } else {
                        SoundUtils.playSound("augustus-disable.wav");
                    }
                    break;
                case MINECRAFT:
                    SoundUtils.playSound("minecraft-toggle.wav");
                    break;
                case SMOOTH:
                    if (enabled) {
                        SoundUtils.playSound("smooth-enable.wav");
                    } else {
                        SoundUtils.playSound("smooth-disable.wav");
                    }
                    break;
                case HANABI:
                    if (enabled) {
                        SoundUtils.playSound("hanabi-enable.wav");
                    } else {
                        SoundUtils.playSound("hanabi-disable.wav");
                    }
                    break;
                case SIGMA:
                    if (enabled) {
                        SoundUtils.playSound("sigma-enable.wav");
                    } else {
                        SoundUtils.playSound("sigma-disable.wav");
                    }
                    break;
            }
        }
    }
}
