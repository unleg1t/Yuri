package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;

@ModuleInfo(label = "Capes", description = "Allows you to change your cape", category = ModuleCategory.RENDER)
public class CapesModule extends Module {
    public static ModeProperty<Cape> cape = new ModeProperty<>("Cape", Cape.YURI);

    public static enum Cape {
        YURI("Yuri"), NATSUKI("Natsuki"), PULSIVE("Pulsive"), SAD("Sad"), ZERO_TWO("Zero Two"), SKY("Sky");

        public final String name;

        Cape(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }
}
