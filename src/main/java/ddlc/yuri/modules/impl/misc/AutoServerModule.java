package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleInfo(label = "Auto Server", description = "Automatically responds to chat, handling registration on cracked servers, queuing a new game, reporting players and blaming killers", category = ModuleCategory.MISC)
public final class AutoServerModule extends Module {

    private static final String[] REGISTER_KEYWORDS = {
            "/register",
            "/reg",
            "/registrar",
            "/зарег",
            "/rejestracja",
            "/cadastrar",
            "/kayit",
            "/enregistrer"
    };

    private static final String[] EXCUSES = {
            "%s is hacking, reported",
            "%s is definitely cheating",
            "reported %s for hacks, no way that was legit",
            "%s is using a hacked client",
            "%s you're cheating, reported",
            "how is %s not banned yet"
    };

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final long REPORT_DELAY_MS = 1500L;

    public enum Mode {
        REGISTER("Register"),
        AUTO_PLAY("Auto Play"),
        POLICIES("Auto Policies"),
        AUTO_REPORT("Auto Report"),
        AUTO_EXCUSE("Auto Excuse");

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
    private final Property<String> password = new Property<>("Password", "yuri420", () -> modes.isSelected(Mode.REGISTER));
    private final Property<Boolean> doublePassword = new Property<>("Double Password", true, () -> modes.isSelected(Mode.REGISTER));

    private final Random random = new Random();
    private boolean acceptedPolicies = false;
    private volatile boolean reporting = false;

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

        if (modes.isSelected(Mode.POLICIES)) {
            handlePolicies();
        }

        if (modes.isSelected(Mode.AUTO_REPORT)) {
            handleAutoReport(packetChat);
        }

        if (modes.isSelected(Mode.AUTO_EXCUSE)) {
            handleAutoExcuse(packetChat);
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
                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Automatically registered!");
                break;
            }
        }
    }

    private void handlePolicies() {
        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.toLowerCase().contains("hypixel")) {
            if (mc.currentScreen instanceof GuiScreenBook) {
                if (!acceptedPolicies) {
                    mc.thePlayer.sendChatMessage("/policies accept");
                    acceptedPolicies = true;
                    Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Policies accepted!");
                }
            } else {
                acceptedPolicies = false;
            }
        }
    }

    private void handleAutoPlay(S02PacketChat chat) {
        if (chat.isChat()) return;

        if (chat.getChatComponent().getUnformattedText().contains("You were spawned in Limbo.")) {
            mc.thePlayer.sendChatMessage("/lobby");
            Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Sent to limbo, returned to lobby!");
        }

        if (chat.getChatComponent().getFormattedText().contains("play again?")) {
            for (IChatComponent iChatComponent : chat.getChatComponent().getSiblings()) {
                for (String value : iChatComponent.toString().split("'")) {
                    if (value.startsWith("/play") && !value.contains(".")) {
                        mc.thePlayer.sendChatMessage(value);
                        Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Started a new game!");
                        break;
                    }
                }
            }
        }
    }

    private void handleAutoReport(S02PacketChat chat) {
        if (reporting) return;

        String raw = chat.getChatComponent().getUnformattedText();

        if (!isGameStart(raw)) return;

        if (mc.getNetHandler() == null) return;

        String self = mc.thePlayer.getName();
        List<String> targets = new ArrayList<>();

        for (NetworkPlayerInfo info : new ArrayList<>(mc.getNetHandler().getPlayerInfoMap())) {
            String name = info.getGameProfile().getName();

            if (name == null || name.equalsIgnoreCase(self) || !PLAYER_NAME.matcher(name).matches() || targets.contains(name))
                continue;

            targets.add(name);
        }

        if (targets.isEmpty()) return;

        reporting = true;

        Thread thread = new Thread(() -> {
            try {
                for (String target : targets) {
                    if (mc.thePlayer == null || !modes.isSelected(Mode.AUTO_REPORT)) break;

                    mc.addScheduledTask(() -> {
                        if (mc.thePlayer != null) {
                            mc.thePlayer.sendChatMessage("/report " + target + " cheating");
                        }
                    });

                    Thread.sleep(REPORT_DELAY_MS);
                }
            } catch (InterruptedException ignored) {
            } finally {
                reporting = false;
            }
        }, "AutoReport");

        thread.setDaemon(true);
        thread.start();

        Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Reporting " + targets.size() + " players!");
    }

    private void handleAutoExcuse(S02PacketChat chat) {
        String stripped = StringUtils.stripControlCodes(chat.getChatComponent().getUnformattedText()).trim();
        String self = mc.thePlayer.getName();

        if (!stripped.startsWith(self + " ")) return;

        Matcher matcher = Pattern.compile("^" + Pattern.quote(self) + " (?:was|got) .*?\\b(?:by|from) ([A-Za-z0-9_]{1,16})\\b").matcher(stripped);

        if (!matcher.find()) return;

        String killer = matcher.group(1);

        if (killer.equalsIgnoreCase(self)) return;

        mc.thePlayer.sendChatMessage(String.format(EXCUSES[random.nextInt(EXCUSES.length)], killer));
    }

    private boolean isGameStart(String raw) {
        String stripped = StringUtils.stripControlCodes(raw);

        if (stripped.startsWith(" ") && (stripped.contains("Protect your bed and destroy the enemy beds.") || stripped.contains("Goodluck with your BedWars Game"))) {
            return true;
        }

        switch (raw) {
            case "Cages opened! FIGHT!":
            case "§r§r§r                               §r§f§lSkyWars Duel§r":
            case "§r§eCages opened! §r§cFIGHT!§r":
                return true;
            default:
                break;
        }

        String trimmed = stripped.trim();

        return trimmed.equals("Cages opened! FIGHT!") || trimmed.equals("SkyWars Duel");
    }
}