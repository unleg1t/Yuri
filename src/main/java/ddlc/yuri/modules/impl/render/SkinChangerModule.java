package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.network.NetworkPlayerInfo;

@ModuleInfo(label = "Skin Changer", description = "Changes your skin to another skin", category = ModuleCategory.RENDER)
public class SkinChangerModule extends Module {
    public final Property<Boolean> slimSkin = new Property<>("Slim", true);
    public final Property<Boolean> customUrl = new Property<>("Custom URL", false);
    public final ModeProperty<Mode> skinMode = new ModeProperty<>("Skin", Mode.BOMBIES, () -> !customUrl.getValue());
    public final Property<String> customSkinUrl = new Property<String>("URL", "https://s.namemc.com/i/270b21cfe8c63f6e.png", customUrl::getValue);

    public enum Mode {
        IZUNA("izuna"),
        SKEPPY("Skeppy"),
        MARLOWWW("Marlowww"),
        BOMBIES("Bombies"),
        MARTIN_SUGAR_2K("MartinSugar2K");
        public final String name;

        Mode(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    private String lastAppliedState = "";

    @Override
    public void onEnable() {
        lastAppliedState = "";
    }

    @Override
    public void onDisable() {
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getGameProfile().getId());
        if (info != null) {
            info.resetPlayerTextures();
        }
        super.onDisable();
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.thePlayer == null || mc.getNetHandler() == null) {
            return;
        }

        String currentState = getURL() + "|" + slimSkin.getValue();
        if (currentState.equals(lastAppliedState)) {
            return;
        }

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getGameProfile().getId());
        if (info != null) {
            info.resetPlayerTextures();
            lastAppliedState = currentState;
        }
    }

    public String getURL() {
        if (!customUrl.getValue()) {
            switch (skinMode.getValue()) {
                case IZUNA:
                    return "https://s.namemc.com/i/b95cabd61250a94d.png";
                case MARTIN_SUGAR_2K:
                    return "https://s.namemc.com/i/871094745e98ef86.png";
                case BOMBIES:
                    return "https://s.namemc.com/i/270b21cfe8c63f6e.png";
                case MARLOWWW:
                    return "https://s.namemc.com/i/241d5780cc237aee.png";
                case SKEPPY:
                    return "https://s.namemc.com/i/5f9997b3c42be686.png";
                default:
                    return "";
            }
        } else {
            return customSkinUrl.getValue();
        }
    }
}
