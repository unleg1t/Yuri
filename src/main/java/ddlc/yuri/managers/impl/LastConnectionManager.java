package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ServerJoinEvent;

public class LastConnectionManager {
    public static String ip;
    public static int port;

    @EventHook
    public void onServerJoin(ServerJoinEvent event) {
        ip = event.getIp();
        port = event.getPort();
    }
}
