package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;

@ModuleInfo(label = "Auto Chat", description = "Automatically responds to chat, handling registration on cracked servers and queuing a new game", category = ModuleCategory.MISC)
public final class AutoChatModule extends Module {

    private static final String[] REGISTER_KEYWORDS = {
            "/register",    // English
            "/reg",         // Short English
            "/registrar",   // Spanish/Portuguese
            "/зарег",       // Russian
            "/rejestracja", // Polish
            "/cadastrar",   // Portuguese
            "/kayit",       // Turkish
            "/enregistrer"  // French
    };

    public enum Mode {
        REGISTER("Register"),
        AUTO_PLAY("Auto Play");

        private final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final MultiModeProperty<Mode> modes = new MultiModeProperty<>("Modes", Mode.REGISTER, Mode.AUTO_PLAY);
    private final Property<String> password = new Property<>("Password", "yuri420");
    private final Property<Boolean> doublePassword = new Property<>("Double Password", true);

    @EventHook
    public void onPacket(PacketReceivedEvent event) {
        if (mc.thePlayer == null || !(event.getPacket() instanceof S02PacketChat))
            return;

        S02PacketChat packetChat = (S02PacketChat) event.getPacket();

        if (modes.isSelected(Mode.REGISTER)) {
            handleRegister(packetChat);
        }

        if (modes.isSelected(Mode.AUTO_PLAY)) {
            handleAutoPlay(packetChat);
        }
    }

    private void handleRegister(S02PacketChat packetChat) {
        String chatComponent = packetChat.getChatComponent().getUnformattedText().toLowerCase();

        String passwordMessage;

        if (doublePassword.getValue()) {
            passwordMessage = password.getValue() + " " + password.getValue();
        } else {
            passwordMessage = password.getValue();
        }

        for (String keyword : REGISTER_KEYWORDS) {
            if (chatComponent.contains(keyword.toLowerCase())) {
                mc.thePlayer.sendChatMessage(keyword + " " + passwordMessage);
                break;
            }
        }
    }

    private void handleAutoPlay(S02PacketChat chat) {
        if (chat.isChat()) return;

        if (chat.getChatComponent().getUnformattedText().contains("You were spawned in Limbo.")) {
            mc.thePlayer.sendChatMessage("/lobby");
        }

        if (chat.getChatComponent().getFormattedText().contains("play again?")) {
            for (IChatComponent iChatComponent : chat.getChatComponent().getSiblings()) {
                for (String value : iChatComponent.toString().split("'")) {
                    if (value.startsWith("/play") && !value.contains(".")) {
                        mc.thePlayer.sendChatMessage(value);
                        break;
                    }
                }
            }
        }
    }
}