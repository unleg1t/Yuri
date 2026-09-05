package ddlc.yuri.managers;

import ddlc.yuri.api.events.EventBus;
import ddlc.yuri.managers.impl.*;
import ddlc.yuri.utils.player.FriendUtils;

import java.util.Arrays;

public class ManagerWrapper {
    private static RotationManager rotationManager;
    private static RotationLearnerManager rotationLearnerManager;
    private static ColorManager colorManager;
    private static CommandManager commandManager;
    private static TargetManager targetManager;
    private static KillEventManager killEventManager;
    private static BlinkManager blinkManager;
    private static LagManager lagManager;
    private static BreakerWhitelistManager breakerWhitelistManager;
    private static ProgressBarManager progressBarManager;
    private static BadPacketsManager badPacketsManager;
    private static LastConnectionManager lastConnectionManager;
    private static NotificationManager notificationManager;
    private static SessionStatsManager sessionStatsManager;

    public static void init() {
        rotationManager = new RotationManager();
        rotationLearnerManager = RotationLearnerManager.INSTANCE;
        colorManager = new ColorManager();
        commandManager = new CommandManager();
        killEventManager = new KillEventManager();
        targetManager = new TargetManager();
        TargetManager.configure(Arrays.asList(TargetManager.Targets.PLAYERS, TargetManager.Targets.HOSTILES, TargetManager.Targets.INVISIBLES, TargetManager.Targets.TEAMMATES));
        blinkManager = new BlinkManager();
        lagManager = new LagManager();
        breakerWhitelistManager = new BreakerWhitelistManager();
        progressBarManager = new ProgressBarManager();
        badPacketsManager = new BadPacketsManager();
        lastConnectionManager = new LastConnectionManager();
        notificationManager = new NotificationManager();
        sessionStatsManager = new SessionStatsManager();
        FriendUtils.init();
    }

    public static void subscribe(EventBus eventBus) {
        eventBus.subscribe(rotationManager);
        eventBus.subscribe(rotationLearnerManager);
        eventBus.subscribe(colorManager);
        eventBus.subscribe(commandManager);
        eventBus.subscribe(killEventManager);
        eventBus.subscribe(targetManager);
        eventBus.subscribe(blinkManager);
        eventBus.subscribe(lagManager);
        eventBus.subscribe(lastConnectionManager);
        eventBus.subscribe(badPacketsManager);
        eventBus.subscribe(breakerWhitelistManager);
        eventBus.subscribe(progressBarManager);
        eventBus.subscribe(notificationManager);
        eventBus.subscribe(sessionStatsManager);
    }
}
