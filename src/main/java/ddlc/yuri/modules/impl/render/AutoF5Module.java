package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.modules.impl.combat.AuraModule;
import ddlc.yuri.modules.impl.player.ScaffoldModule;

@ModuleInfo(label = "Auto F5", description = "Goes third person in optimal situations", category = ModuleCategory.RENDER)
public class AutoF5Module extends Module {

    private final Property<Boolean> onAura = new Property<>("On Aura", true);
    private final Property<Boolean> onScaffold = new Property<>("On Scaffold", true);

    private boolean changed = false;

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        AuraModule aura = Yuri.INSTANCE.getModuleManager().getModule(AuraModule.class);
        ScaffoldModule scaffold = Yuri.INSTANCE.getModuleManager().getModule(ScaffoldModule.class);

        boolean active = (onAura.getValue() && aura.isEnabled() && AuraModule.target != null)
                || (onScaffold.getValue() && scaffold.isEnabled());

        if (active) {
            if (!changed && mc.gameSettings.thirdPersonView == 0) {
                mc.gameSettings.thirdPersonView = 1;
                changed = true;
            }
        } else if (changed) {
            mc.gameSettings.thirdPersonView = 0;
            changed = false;
        }
    }

    @Override
    public void onDisable() {
        if (changed) {
            mc.gameSettings.thirdPersonView = 0;
            changed = false;
        }
        super.onDisable();
    }
}