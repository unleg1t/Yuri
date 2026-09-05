package ddlc.yuri.api.commands.impl;


import ddlc.yuri.api.commands.Command;
import ddlc.yuri.utils.client.LoggingUtils;
import ddlc.yuri.utils.player.FriendUtils;

public class FriendCommand extends Command {
    public FriendCommand() {
        super("friend", "Manage your client friends", "f");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            LoggingUtils.sendChatMessage("Usage: .friend <add/remove/list/clear>");
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "add": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .friend add <username>");
                    return;
                }

                String name = args[1];
                FriendUtils.add(name);
                LoggingUtils.sendChatMessage(name + " is now your friend!");
                break;
            }

            case "remove": {
                if (args.length < 2) {
                    LoggingUtils.sendChatMessage("Usage: .friend remove <username>");
                    return;
                }

                String name = args[1];
                FriendUtils.remove(name);
                LoggingUtils.sendChatMessage(name + " was removed from friends.");
                break;
            }

            case "list": {
                if (FriendUtils.getFriends().isEmpty()) {
                    LoggingUtils.sendChatMessage("You have no friends");
                    return;
                }

                LoggingUtils.sendChatMessage("Friends: ");
                for (String friend : FriendUtils.getFriends()) {
                    LoggingUtils.sendChatMessage(" - " + friend);
                }
                break;
            }

            case "clear": {
                FriendUtils.clear();
                LoggingUtils.sendChatMessage("Cleared all friends");
                break;
            }

            default: {
                LoggingUtils.sendChatMessage("Usage: .friend add <username>");
                break;
            }
        }
    }
}