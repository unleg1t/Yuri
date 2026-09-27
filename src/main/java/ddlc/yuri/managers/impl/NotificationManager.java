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

        ToggleSoundsModule toggleSounds = Yuri.INSTANCE.getModuleManager() != null ? Yuri.INSTANCE.getModuleManager().getModule(ToggleSoundsModule.class) : null;
        if (toggleSounds != null && (toggleSounds.isEnabled() || e.getModule() instanceof ToggleSoundsModule)) {
            float volume = toggleSounds.volume != null ? toggleSounds.volume.getValue().floatValue() : 1.0f;
            switch (toggleSounds.moduleToggleSounds.getValue()) {
                case EVISCERATE:
                    if (enabled) {
                        SoundUtils.playSound("eviscerate-enable.wav", volume);
                    } else {
                        SoundUtils.playSound("eviscerate-disable.wav", volume);
                    }
                    break;
                case NURSULTAN:
                    if (enabled) {
                        SoundUtils.playSound("nursultan-enable.wav", volume);
                    } else {
                        SoundUtils.playSound("nursultan-disable.wav", volume);
                    }
                    break;
                case AUGUSTUS:
                    if (enabled) {
                        SoundUtils.playSound("augustus-enable.wav", volume);
                    } else {
                        SoundUtils.playSound("augustus-disable.wav", volume);
                    }
                    break;
                case MINECRAFT:
                    SoundUtils.playSound("minecraft-toggle.wav", volume);
                    break;
                case SMOOTH:
                    if (enabled) {
                        SoundUtils.playSound("smooth-enable.wav", volume);
                    } else {
                        SoundUtils.playSound("smooth-disable.wav", volume);
                    }
                    break;
                case HANABI:
                    if (enabled) {
                        SoundUtils.playSound("hanabi-enable.wav", volume);
                    } else {
                        SoundUtils.playSound("hanabi-disable.wav", volume);
                    }
                    break;
                case SIGMA:
                    if (enabled) {
                        SoundUtils.playSound("sigma-enable.wav", volume);
                    } else {
                        SoundUtils.playSound("sigma-disable.wav", volume);
                    }
                    break;
            }
        }
    }
}
