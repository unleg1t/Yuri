package ddlc.yuri;

import ddlc.yuri.api.config.BindsConfig;
import ddlc.yuri.api.config.ConfigManager;
import ddlc.yuri.api.config.VisualsConfig;
import ddlc.yuri.api.events.EventBus;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.GameStartupEvent;
import ddlc.yuri.api.events.impl.client.GameStoppingEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.gui.click.imgui.ImGuiClickGui;
import ddlc.yuri.api.gui.click.novoline.NovolineClickGui;
import ddlc.yuri.api.gui.click.yuri.YuriClickGUI;
import ddlc.yuri.managers.ManagerWrapper;
import ddlc.yuri.modules.ModuleManager;
import ddlc.yuri.modules.impl.render.ClickGUIModule;
import ddlc.yuri.utils.misc.NotificationHandler;
import ddlc.yuri.utils.render.DragUtils;
import lombok.Getter;

public class Yuri {
    public static final Yuri INSTANCE = new Yuri();
    public static final String NAME = "Yuri";
    public static final String BUILD = "Beta";
    public static final String VERSION = "1.2.0";
    public static final String FULL = NAME + " " + VERSION + " " + BUILD;

    private EventBus eventBus;
    @Getter
    private ModuleManager moduleManager;
    @Getter
    private ConfigManager configManager;
    @Getter
    private final YuriClickGUI yuriClickGUI = new YuriClickGUI();
    @Getter
    private final NovolineClickGui novolineClickGui = new NovolineClickGui();
    @Getter
    private final ImGuiClickGui imGuiClickGui = new ImGuiClickGui();
    @Getter
    private NotificationHandler notificationHandler = new NotificationHandler();
    private BindsConfig bindsConfig;
    private VisualsConfig visualsConfig;

    private Yuri() {
        getEventBus().subscribe(this);
    }

    @EventHook
    public void onGameStartup(GameStartupEvent event) {
        moduleManager = new ModuleManager();
        moduleManager.postInit();
        configManager = new ConfigManager();
        configManager.loadConfig("default");
        visualsConfig = new VisualsConfig();
        visualsConfig.loadFromFile();
        bindsConfig = new BindsConfig();
        bindsConfig.loadFromFile();
        ManagerWrapper.init();
        ManagerWrapper.subscribe(getEventBus());
        if (getModuleManager().getModule(ClickGUIModule.class).isEnabled()) getModuleManager().getModule(ClickGUIModule.class).setEnabled(false);
    }

    public EventBus getEventBus() {
        if (eventBus == null) {
            eventBus = new EventBus();
        }

        return eventBus;
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        DragUtils.update();
    }

    @EventHook
    public void onGameStopping(GameStoppingEvent event) {
        configManager.saveConfig("default");
        visualsConfig.saveToFile();
        bindsConfig.saveToFile();
        System.out.println("Autosaved modules and draggable positions.");
    }
}
