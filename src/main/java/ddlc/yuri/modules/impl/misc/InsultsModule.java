package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.StringUtils;
import org.apache.commons.lang3.RandomUtils;

@ModuleInfo(label = "Insults", description =  "Insult people on kill", category = ModuleCategory.MISC)
public final class InsultsModule extends Module {

    private enum Mode {
        NORMAL("Normal"),
        HYPIXEL("Hypixel");

        public final String name;

        Mode(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String[] SHOUT_GAMES = {
            "DOUBLES", "3v3v3v3", "4v4v4v4", "4v4", "DUEL"
    };

    public final ModeProperty<Mode> mode = new ModeProperty<>("Mode", Mode.HYPIXEL);

    private final String[] defaultInsults = {
            // added from my old client
            // if anyone has more let me know! -unlegit
            "maybe i want u to put ur hands where u want to :3",
            "imagine getting killed by a client thats pasted from rise",
            "bald red man wants u to go get yuri @github/unleg1t/Yuri",
            "my dog itches so can u like itch it for me",
            "\"best aac bypasses!!1!1\" like bro stfu ur client is so donkey dooks",
            "how wood would alan wood suck if alan wood could suck wood?",
            "ouija board vs 25 woke students. im charlie kirk back from the dead mf",
            "i bet u love receiving backshots from michael stetson",
            "unleg1t, is the best client dev of all time. get yuri @github/unleg1t/Yuri",
            "i paste them astolfo scripts like its ur moms first taste of my d;ick.",
            "rawr xD x3 nuzzles u UwU",
            "womp womp",
            "sniped by ducky $$ get my client @github/unleg1t/Yuri",
            "get beamed by merch f;uck the opps n;igga. on for 4nim. that n;igga Dylan's mom dead he dead as h;ell",
            "polar pop bypass $$",
            "go back to 2022 skid #famous",
            "it's my b-day. b nice 2 me :<",
            "bombies is my little $1utt",
            "\"i'm not a furry but i do like to be called daddy uwu\"",
            "9lua owns the chat // Devs in Shambles",
            "i knew some1 tht said he would let bombies stack donuts on it.. i agree."};

    @Override
    public void onEnable() {
        setSuffix(mode.getValue().toString());
    }

    @EventHook
    public void onKill(KillEvent event) {
        if (!(event.getEntity() instanceof EntityPlayer)) {
            return;
        }

        String insult = defaultInsults[RandomUtils.nextInt(0, defaultInsults.length)];
        if (mode.getValue() == Mode.HYPIXEL && isOnHypixel()) {
            insult = (usesShout() ? "/shout " : "/ac ") + insult;
        }
        mc.thePlayer.sendChatMessage(insult);
    }

    private boolean usesShout() {
        String sidebar = getSidebarText();
        if (sidebar.isEmpty()) {
            return false;
        }

        for (String game : SHOUT_GAMES) {
            if (sidebar.contains(game)) {
                return true;
            }
        }
        return false;
    }

    private String getSidebarText() {
        if (mc.theWorld == null) {
            return "";
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder(strip(objective.getDisplayName()));
        for (Score score : scoreboard.getSortedScores(objective)) {
            if (score.getPlayerName() == null || score.getPlayerName().startsWith("#")) {
                continue;
            }
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            builder.append(' ').append(strip(ScorePlayerTeam.formatPlayerName(team, score.getPlayerName())));
        }
        return builder.toString().toUpperCase();
    }

    private String strip(String text) {
        return text == null ? "" : StringUtils.stripControlCodes(text);
    }

    private boolean isOnHypixel() {
        if (mc.isSingleplayer()) {
            return false;
        }

        ServerData server = mc.getCurrentServerData();
        if (server == null || server.serverIP == null) {
            return false;
        }

        String ip = server.serverIP.toLowerCase();
        return ip.contains("hypixel.net") || ip.endsWith(".hypixel.net");
    }
}