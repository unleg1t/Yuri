package ddlc.yuri.managers.impl;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.ClientTickEvent;
import ddlc.yuri.api.events.impl.client.GameStoppingEvent;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.player.PlayerDeathEvent;
import ddlc.yuri.utils.misc.SessionStatsDebug;
import ddlc.yuri.utils.misc.SessionStatsExporter;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S42PacketCombatEvent;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static ddlc.yuri.utils.misc.IMinecraft.mc;

/**
 * Counts kills, deaths and wins for the whole client run and hands them to
 * {@link SessionStatsExporter} so Yuri Launcher can show them.
 *
 * <p>This used to live in {@code SessionInfoModule}, which meant the numbers only
 * existed while that HUD was switched on: {@code Module.setEnabled} subscribes a
 * module to the event bus on enable and unsubscribes it on disable, so with the
 * module off its {@code KillEvent} hook never ran. A manager is subscribed once
 * at startup by {@link ddlc.yuri.managers.ManagerWrapper} and never
 * unsubscribed, so the count follows the client rather than an overlay.
 *
 * <p>A "session" is one client launch, matching what the launcher already
 * measures for playtime. Nothing resets mid-run except {@link #reset()}.
 *
 * <h3>Where each number comes from</h3>
 *
 * <ul>
 *   <li><b>Kills:</b> {@code KillEvent}, posted by
 *       {@link KillEventManager} once an entity you attacked leaves the world.
 *       One event, one kill - nothing is inferred from chat.</li>
 *   <li><b>Deaths:</b> four sources, in the order a server is likely to give
 *       them: {@code S06PacketUpdateHealth} carrying zero health,
 *       {@code S42PacketCombatEvent} {@code ENTITY_DIED} naming your own entity
 *       id, {@code PlayerDeathEvent} (the vanilla game-over screen going up), and
 *       - because a minigame server may send none of those - the death the server
 *       announces on screen, either addressed to you ("YOU DIED", "VOCE MORREU")
 *       or naming you as the victim of a chat death message. Void, fall damage and
 *       being killed by another player all arrive that way and are labelled as
 *       such in the log. See {@link #iAmTheVictim} for how the victim is told
 *       apart from the killer in one line.</li>
 *   <li><b>Wins:</b> no packet means "you won", so the wording on screen is
 *       matched: titles, subtitles and chat lines that carry your name. Every
 *       distinct line is also written to {@code yuri_session_debug.log}, so a
 *       server whose phrasing is missing can be read off the log after one game
 *       and added to the lists below.</li>
 * </ul>
 *
 * <p>One death per burst, not per signal. A single death routinely arrives as a
 * chat line, a title and a packet within the same tick, and kaizenmc.gg resends
 * its title every 50ms for a couple of seconds - so the first signal counts and
 * everything for {@link #DEATH_COOLDOWN_MS} after it is noted and ignored, the
 * same wording for {@link #DEATH_TEXT_REPEAT_MS}, and nothing at all while you are
 * still down ({@link #deathEpisode}).
 *
 * <p>Every death-shaped packet is written to {@code yuri_session_debug.log} even
 * when it counts nothing - low health readings, every {@code ENTITY_DIED} with the
 * death message the server attached to it, and every respawn. kaizenmc.gg's
 * minigames answer a death by healing you and moving you, so if none of the three
 * packet signals ever reaches this class there, the log is what shows that rather
 * than leaving it to be guessed at - which is exactly how the wording sources came
 * to be added: the log of one twelve-minute session held five self-named death
 * lines and no death packet whatsoever.
 *
 * <p>Packet hooks run on netty's thread while {@link #onTick} runs on the main
 * one, so every counter is mutated under this class's monitor and no file is
 * written from a packet hook - notes are parked and flushed on the next tick.
 */
public class SessionStatsManager {

    /**
     * Win wording addressed to whoever is looking at the screen, so it needs no
     * name to be sure it is about you. Matched as whole words, upper case, after
     * colour codes are stripped and accents folded: "VICTORY" also catches
     * "VICTORY!", and "VITORIA" catches Portuguese "VITORIA!" with its accent.
     */
    private static final String[] SELF_WIN_PHRASES = {
            "VICTORY", "YOU WIN", "YOU WON", "YOU ARE THE WINNER",
            "WON THE GAME", "GAME WON", "1ST PLACE", "FIRST PLACE",
            // kaizenmc.gg and mineberry are Brazilian, so their screens are in
            // Portuguese - taken from what actually turned up in the debug log.
            "VITORIA", "VOCE GANHOU", "VOCE VENCEU", "GANHOU O JOGO",
            "PRIMEIRO LUGAR"
    };

    /**
     * Wording that announces a winner to everyone. Only counts when your own
     * username is in the same line, otherwise every game somebody else wins
     * would count as yours.
     */
    private static final String[] NAMED_WIN_PHRASES = {
            "WINNER", "WINS", "WON", "VENCEDOR", "VENCEU", "GANHOU"
    };

    /**
     * The end-of-game half of {@link #LOSS_PHRASES}. A round is over when one of these shows up,
     * while the rest of that table ("YOU DIED", "CAMA DESTRUIDA") is ordinary mid-game wording that
     * would otherwise count a round per respawn.
     */
    private static final String[] GAME_OVER_PHRASES = {
            "DEFEAT", "GAME OVER", "BETTER LUCK", "TRY AGAIN",
            "DERROTA", "VOCE PERDEU"
    };

    /** Checked first: these mean the opposite, whatever else is in the line. */
    private static final String[] LOSS_PHRASES = {
            "DEFEAT", "YOU LOST", "YOU DIED", "GAME OVER", "ELIMINATED",
            "BETTER LUCK", "TRY AGAIN",
            "DERROTA", "VOCE PERDEU", "VOCE MORREU", "CAMA DESTRUIDA"
    };

    /**
     * Death wording addressed to whoever is looking at the screen, so it needs no
     * name to be about you. This is the whole of what kaizenmc.gg gives you: it
     * heals and teleports instead of killing, so the only announcement is the
     * title "VOCE MORREU!" and a chat line.
     */
    private static final String[] SELF_DEATH_PHRASES = {
            "YOU DIED", "YOU ARE DEAD", "YOU HAVE DIED", "YOU WERE KILLED",
            "YOU WERE SLAIN", "YOU DROWNED", "YOU BURNED", "YOU STARVED",
            "YOU FELL", "YOU BLEW UP",
            "VOCE MORREU", "VOCE FOI MORTO", "VOCE FOI MORTA", "VOCE CAIU",
            "VOCE SE AFOGOU", "VOCE QUEIMOU",
            "HAS MUERTO"
    };

    /**
     * Wording that says somebody died. A chat line needs one of these <i>and</i>
     * your name in the victim slot - see {@link #iAmTheVictim} - because the same
     * sentence with the names the other way round is one of your kills.
     */
    private static final String[] DEATH_CUES = {
            "DIED", "DEAD", "KILLED", "SLAIN", "SHOT", "FELL", "THREW",
            "THROWN", "DROWNED", "BURNED", "BURNT", "SUFFOCATED", "STARVED",
            "BLEW UP", "WITHERED", "SQUASHED", "GROUND TOO HARD",
            "HIGH PLACE", "INTO THE VOID", "FINAL KILL",
            // Portuguese, from kaizenmc.gg's own chat: "FURINKA22 FOI MORTO(A)
            // POR X", "FURINKA22 FOI JOGADO(A) NO VOID POR X", "FURINKA22
            // MORREU NO VOID".
            "MORREU", "MORTO", "MORTA", "MORTE", "MATOU", "JOGADO", "JOGADA",
            "ARREMESSADO", "CAIU", "AFOGOU", "QUEIMOU", "EXPLODIU"
    };

    /**
     * A line that mentions one of these is about something other than a death,
     * however death-shaped the rest of it looks - bedwars announces a destroyed
     * bed with the destroyer's name and the verb "FOI DESTRUIDA".
     */
    private static final String[] DEATH_VETO = {
            "BED", "CAMA", "DESTROYED", "DESTRUIDA", "DESTRUIU", "BROKE"
    };

    /**
     * The word that separates victim from killer: "X was killed <b>by</b> Y",
     * "X FOI MORTO(A) <b>POR</b> Y". Everything before it is the victim's side.
     */
    private static final String[] AGENT_MARKERS = { "BY", "POR", "PELO", "PELA" };

    /**
     * Verbs used the other way round, where the killer comes first: "Y killed X".
     * Also what stops "FURINKA22 MATOU X" from reading as your own death.
     */
    private static final String[] ACTIVE_KILL_VERBS = {
            "KILLED", "SLAIN", "SHOT", "MATOU", "ASSASSINOU", "ELIMINOU"
    };

    /** Cause labels for the log: he asked for void, fall damage and players. */
    private static final String[] VOID_CUES = { "VOID", "ABISMO", "VAZIO" };
    private static final String[] FALL_CUES = {
            "FELL", "FALL", "GROUND TOO HARD", "HIGH PLACE", "CAIU", "QUEDA"
    };
    private static final String[] FIRE_CUES = {
            "FIRE", "LAVA", "BURNED", "BURNT", "QUEIMOU", "FOGO"
    };
    private static final String[] WATER_CUES = {
            "DROWNED", "AFOGOU", "AFOGADO", "WATER"
    };

    /**
     * A win screen usually arrives as a title, a subtitle and a chat line within
     * a second of each other. One win, three signals - so after counting one,
     * ignore the rest for a while.
     */
    private static final long WIN_COOLDOWN_MS = 20000L;

    /**
     * One death arrives as several signals at once - a chat line, a title, and on
     * a vanilla-ish server a health packet and a combat event too. Five seconds
     * covers that burst without swallowing a real second death: every respawn
     * timer seen so far is at least three seconds and you still have to get back
     * to where you can die.
     */
    private static final long DEATH_COOLDOWN_MS = 5000L;

    /**
     * The same wording arriving again is the same death for much longer than that.
     * kaizenmc.gg resends its title every 50ms for a couple of seconds, and this is
     * what stops one death from counting once per resend.
     */
    private static final long DEATH_TEXT_REPEAT_MS = 15000L;

    /**
     * Enough distinct wording to identify a server's vocabulary. Counted per
     * distinct line rather than per line seen: a countdown ticking down or a
     * title resent every second would otherwise spend the whole budget in the
     * first three minutes and leave the rest of the session unlogged.
     */
    private static final int MAX_TEXTS_LOGGED = 240;
    private static final int MAX_PENDING_NOTES = 64;

    /**
     * Budget for "seen it, ignored it" notes, which are also kept one per distinct
     * reason. Before this existed, a win title resent 677 times filled 59% of a
     * whole session's log with the same cooldown line.
     */
    private static final int MAX_NOTED_ONCE = 120;

    /**
     * Health at or below this is logged, whoever it belongs to. Two hearts is
     * low enough to be the run-up to a death and rare enough not to spam.
     */
    private static final float LOW_HEALTH_NOTE = 2.0F;

    /**
     * A budget for the death-shaped packets, separate from the wording budget so
     * a chatty server cannot exhaust the packet trail or the other way round.
     */
    private static final int MAX_PACKET_NOTES = 200;

    /**
     * Static so the HUD can read them without a handle on this instance -
     * ManagerWrapper keeps its managers private and exposes no getters.
     */
    private static long sessionStart = System.currentTimeMillis();
    private static int kills;
    private static int deaths;
    private static int wins;

    /** Games that ended in front of you this session, won or lost. */
    private static int rounds;

    private static long lastWinAt;

    private static long lastRoundAt;

    private static long lastDeathAt;

    /** The wording of the last counted death, so a resend of it counts nothing. */
    private static String lastDeathText = "";

    /**
     * True between the first signal of a death and the moment you are alive
     * again. Several sources can report the same death - the health packet, the
     * combat event, the game-over screen and the server's own announcement often
     * all arrive - so the first one counts and the others are noted and ignored.
     */
    private static boolean deathEpisode;

    /** Spent against {@link #MAX_PACKET_NOTES}. */
    private static int packetNotes;

    /**
     * Every distinct line the server has shown, so a repeat costs nothing.
     */
    private static final Set<String> loggedTexts = new HashSet<String>();

    /** Reasons already written once, keyed by kind, source and text. */
    private static final Set<String> notedOnce = new HashSet<String>();

    /**
     * Packet hooks run on the netty thread, and {@link SessionStatsDebug} opens a
     * file per line. Lines are parked here and written by the next tick instead,
     * so two threads never append to the log at once.
     */
    private static final List<String> pendingNotes = new ArrayList<String>();

    public SessionStatsManager() {
        reset();

        // Constructed from ManagerWrapper.init() during GameStartupEvent, which is
        // posted after startGame(), so the session and the game directory are both
        // ready. Publishing here means the launcher's panel shows the username and
        // a live link the moment the client is up, instead of waiting for a world.
        publish();
    }

    public static int getKills() {
        return kills;
    }

    public static int getDeaths() {
        return deaths;
    }

    public static int getWins() {
        return wins;
    }

    public static int getRounds() {
        return rounds;
    }

    public static long getSessionStart() {
        return sessionStart;
    }

    public static long getSessionMs() {
        return Math.max(0L, System.currentTimeMillis() - sessionStart);
    }

    /** Starts the session over without restarting the client. */
    public static synchronized void reset() {
        sessionStart = System.currentTimeMillis();
        kills = 0;
        deaths = 0;
        wins = 0;
        rounds = 0;
        lastWinAt = 0L;
        lastRoundAt = 0L;
        lastDeathAt = 0L;
        lastDeathText = "";
        deathEpisode = false;
        packetNotes = 0;
        synchronized (loggedTexts) {
            loggedTexts.clear();
        }
        synchronized (notedOnce) {
            notedOnce.clear();
        }
    }

    /** Posted by KillEventManager once an attacked entity leaves the world. */
    @EventHook
    public void onKill(KillEvent event) {
        countKill();
    }

    /**
     * Posted from {@code Minecraft.displayGuiScreen}, just before the vanilla
     * game-over screen. Reliable where it fires at all, and on servers that skip
     * that screen the two packet signals below are what is left.
     */
    @EventHook
    public void onDeath(PlayerDeathEvent event) {
        countDeath("death screen");
    }

    /**
     * Every inbound packet passes through here (posted from
     * {@code NetworkManager.channelRead0}), on the netty thread, before the
     * client has processed it.
     */
    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        try {
            Object packet = event.getPacket();
            if (packet instanceof S45PacketTitle) {
                onTitle((S45PacketTitle) packet);
            } else if (packet instanceof S02PacketChat) {
                onChat((S02PacketChat) packet);
            } else if (packet instanceof S06PacketUpdateHealth) {
                onHealthPacket((S06PacketUpdateHealth) packet);
            } else if (packet instanceof S42PacketCombatEvent) {
                onCombatPacket((S42PacketCombatEvent) packet);
            } else if (packet instanceof S07PacketRespawn) {
                onRespawnPacket((S07PacketRespawn) packet);
            }
        } catch (Throwable ignored) {
            // On the network thread: a stat is never worth dropping a packet.
        }
    }

    /**
     * The only always-running tick the client posts (it is guarded on a world and
     * a player being present). The exporter throttles itself to one write a
     * second, so 20 calls a second cost one file write.
     */
    @EventHook
    public void onTick(ClientTickEvent event) {
        closeEpisodeIfAlive();
        publish();
        // After publish, not before: SessionStatsDebug truncates the log when it
        // sees a new session, and a note flushed first would go with it.
        flushNotes();
    }

    /** Marks the file as finished so the launcher labels it as the last session. */
    @EventHook
    public void onGameStopping(GameStoppingEvent event) {
        try {
            SessionStatsExporter.publishStopped(mc.mcDataDir);
            flushNotes();
        } catch (Throwable ignored) {
            // Shutting down; a stat is never worth holding up the exit.
        }
    }

    // ---------------------------------------------------------------- counting

    private static synchronized void countKill() {
        kills++;
        note("KILL | kills now " + kills);
    }

    private static synchronized void countDeath(String source) {
        countDeath(source, null);
    }

    /**
     * One death per burst. Three guards, cheapest first: nothing counts while you
     * are still down, the same wording never counts twice inside
     * {@link #DEATH_TEXT_REPEAT_MS}, and no second death of any kind counts inside
     * {@link #DEATH_COOLDOWN_MS} of the last one.
     *
     * <p>The cooldown is what makes the wording sources safe. The episode guard
     * cannot carry them on its own: kaizenmc.gg answers a death by healing you, so
     * the tick after the announcement finds you alive and closes the episode
     * immediately - and the chat line and the title for that one death are two
     * different strings, so the wording guard cannot pair them either. Five seconds
     * is under every respawn timer seen so far and far over the width of a burst.
     *
     * @param text the wording that reported it, or null for a packet source
     */
    private static synchronized void countDeath(String source, String text) {
        long now = System.currentTimeMillis();
        String said = text == null || text.length() == 0 ? "" : " | " + text;
        if (deathEpisode) {
            noteOnce("death-episode|" + source + said,
                    "DEATH ignored, same episode | " + source + said);
            return;
        }
        if (said.length() > 0 && text.equals(lastDeathText)
                && now - lastDeathAt < DEATH_TEXT_REPEAT_MS) {
            noteOnce("death-repeat|" + source + said,
                    "DEATH ignored, same wording | " + source + said);
            return;
        }
        if (lastDeathAt > 0L && now - lastDeathAt < DEATH_COOLDOWN_MS) {
            noteOnce("death-cooldown|" + source + said,
                    "DEATH ignored, within cooldown | " + source + said);
            return;
        }
        deathEpisode = true;
        lastDeathAt = now;
        lastDeathText = text == null ? "" : text;
        deaths++;
        note("DEATH from " + source + " | " + causeOf(text)
                + " | deaths now " + deaths + said);
    }

    /**
     * Closes the episode once you are alive again, so the next death can count.
     * Reading health here is de-duplication, not a source: a death is never
     * counted from the local player's own health, only from what the server said.
     */
    private static synchronized void closeEpisodeIfAlive() {
        if (!deathEpisode) {
            return;
        }
        try {
            if (mc.thePlayer != null && mc.thePlayer.getHealth() > 0.0F) {
                deathEpisode = false;
                note("death episode closed | alive again");
            }
        } catch (Throwable ignored) {
            // No player yet, or a world swap in progress; try again next tick.
        }
    }

    private static synchronized void countWin(String source, String text) {
        long now = System.currentTimeMillis();
        if (now - lastWinAt < WIN_COOLDOWN_MS) {
            noteOnce("win-cooldown|" + source + "|" + text,
                    "WIN ignored, within cooldown | " + source + " | " + text);
            return;
        }
        lastWinAt = now;
        wins++;
        note("WIN from " + source + " | " + text + " | wins now " + wins);
        countRound(source, text, true);
    }

    /**
     * Counted off the same screens as a win, on its own cooldown, because a server that resends the
     * win title for a couple of seconds sends the defeat one the same way.
     */
    private static synchronized void countRound(String source, String text, boolean won) {
        long now = System.currentTimeMillis();
        if (now - lastRoundAt < WIN_COOLDOWN_MS) {
            noteOnce("round-cooldown|" + source + "|" + text,
                    "ROUND ignored, within cooldown | " + source + " | " + text);
            return;
        }
        lastRoundAt = now;
        rounds++;
        note("ROUND " + (won ? "won" : "lost") + " from " + source + " | " + text
                + " | rounds now " + rounds);
    }

    // ----------------------------------------------------------------- sources

    /**
     * The server's own statement of your health. Zero means you are dead by the
     * only authority that decides it, whether or not a game-over screen follows.
     * Anything low is logged either way, because a server that heals you instead
     * of killing you shows up here as a dip that never reaches zero.
     */
    private void onHealthPacket(S06PacketUpdateHealth packet) {
        float health = packet.getHealth();
        if (health <= 0.0F) {
            packetNote("health packet | 0 health");
            countDeath("health packet");
        } else if (health <= LOW_HEALTH_NOTE) {
            packetNote("health packet | low health " + health);
        }
    }

    /**
     * Vanilla combat reporting. {@code ENTITY_DIED} names the fighter that died in
     * {@code field_179774_b}, and servers send it for everybody, so it only counts
     * when the id is yours - but every one is logged with the death message the
     * server attached, which is the cheapest way to learn a server's wording.
     */
    private void onCombatPacket(S42PacketCombatEvent packet) {
        if (packet.eventType != S42PacketCombatEvent.Event.ENTITY_DIED) {
            return;
        }
        boolean mine = false;
        try {
            mine = mc.thePlayer != null
                    && packet.field_179774_b == mc.thePlayer.getEntityId();
        } catch (Throwable ignored) {
            // Between worlds; treat it as somebody else's death.
        }
        String said = normalise(packet.deathMessage);
        packetNote("combat ENTITY_DIED | mine=" + mine + " | entity "
                + packet.field_179774_b + " | " + packet.deathMessage);
        if (mine) {
            countDeath("combat packet", said);
        }
    }

    /**
     * Sent when you are put back into a world, which is the end of a death
     * whether or not anything reported the death itself. Closes the episode so
     * the next one can count, and is logged because on a minigame server this may
     * be the only trace a death leaves at all.
     */
    private void onRespawnPacket(S07PacketRespawn packet) {
        packetNote("respawn packet | dimension " + packet.getDimensionID()
                + " | gametype " + packet.getGameType());
        synchronized (SessionStatsManager.class) {
            if (deathEpisode) {
                deathEpisode = false;
                note("death episode closed | respawn packet");
            }
        }
    }

    /**
     * Titles and subtitles are where minigame servers put both screens - the win
     * and, on kaizenmc.gg, the only announcement of a death that reaches the client
     * at all ("VOCE MORREU!"). Every one is logged, whether it matches or not, so
     * the wording a given server uses can be read out of the debug log instead of
     * guessed at.
     */
    private void onTitle(S45PacketTitle packet) {
        if (packet.getType() != S45PacketTitle.Type.TITLE
                && packet.getType() != S45PacketTitle.Type.SUBTITLE) {
            return;
        }
        if (packet.getMessage() == null) {
            return;
        }
        String text = normalise(packet.getMessage().getUnformattedText());
        if (text.length() == 0) {
            return;
        }
        String source = packet.getType() == S45PacketTitle.Type.TITLE
                ? "title" : "subtitle";
        logText(source, text);
        considerDeath(source, text);
        considerWin(source, text);
    }

    /**
     * Wins need your own name in the line, which rules out both other people's wins
     * and anybody typing the word "won" in public chat. Deaths do not: a server that
     * writes "You were killed by X" has told you it is yours already, and one that
     * names you is checked for which side of the sentence your name is on.
     */
    private void onChat(S02PacketChat packet) {
        if (packet.getChatComponent() == null) {
            return;
        }
        String text = normalise(packet.getChatComponent().getUnformattedText());
        if (text.length() == 0) {
            return;
        }
        considerDeath("chat", text);
        String name = normalise(username());
        if (name.length() == 0 || !containsWord(text, name)) {
            return;
        }
        logText("chat", text);
        considerWin("chat", text);
    }

    // -------------------------------------------------------------- text match

    /**
     * Reads a death out of what the server put on screen. This is the last resort
     * and the only thing that works on kaizenmc.gg, where a twelve-minute session
     * produced five self-named death lines and not one death-shaped packet.
     *
     * <p>Two ways in. Wording addressed to you needs no name
     * ({@link #SELF_DEATH_PHRASES}); wording that names people needs a death cue
     * <i>and</i> your name in the victim slot, because the same sentence with the
     * names swapped is one of your kills.
     */
    private static void considerDeath(String source, String text) {
        // Anything a player typed is quoted after a colon - "[MVP+] X: you died"
        // must not count. A server that puts a colon in its own death announcement
        // will show up in the log unmatched, which is the place to fix it from.
        if (text.indexOf(':') >= 0) {
            return;
        }
        // Bedwars announces a destroyed bed with a name and a passive verb, which
        // is otherwise shaped exactly like a death.
        if (containsAny(text, DEATH_VETO)) {
            return;
        }
        if (containsAny(text, SELF_DEATH_PHRASES)) {
            countDeath(source + ", addressed to you", text);
            return;
        }
        if (!containsAny(text, DEATH_CUES)) {
            return;
        }
        String name = normalise(username());
        if (name.length() == 0 || !containsWord(text, name)) {
            return;
        }
        if (iAmTheVictim(words(text), name)) {
            countDeath(source + ", named", text);
        }
    }

    /**
     * Which side of the sentence your name is on. "FURINKA22 FOI MORTO(A) POR X" is
     * a death and "X FOI MORTO(A) POR FURINKA22" is a kill, and they differ by
     * nothing but the slot - so position decides, never the verb alone.
     *
     * <ul>
     *   <li>With an agent marker ("BY", "POR"), the victim is on its left.</li>
     *   <li>Without one, a name at the front is the subject: "X died", "X hit the
     *       ground too hard" - unless what follows is an active kill verb, which
     *       makes it "X killed Y".</li>
     *   <li>Otherwise your name is late in a sentence whose verb comes first,
     *       "Somebody killed X".</li>
     * </ul>
     */
    private static boolean iAmTheVictim(String[] words, String me) {
        int name = indexOfWord(words, me);
        if (name < 0) {
            return false;
        }
        int marker = indexOfAnyWord(words, AGENT_MARKERS);
        if (marker >= 0) {
            return name < marker;
        }
        if (name <= 1) {
            return !(name + 1 < words.length
                    && isAnyWord(words[name + 1], ACTIVE_KILL_VERBS));
        }
        for (int i = 0; i < name; i++) {
            if (isAnyWord(words[i], ACTIVE_KILL_VERBS)) {
                return true;
            }
        }
        return false;
    }

    /**
     * How it happened, for the log only - he asked for the void, fall damage and
     * other players by name, so the log says which of them it was rather than
     * leaving every death looking alike.
     */
    private static String causeOf(String text) {
        if (text == null || text.length() == 0) {
            return "unknown cause";
        }
        StringBuilder out = new StringBuilder();
        if (containsAny(text, VOID_CUES)) {
            out.append("void");
        } else if (containsAny(text, FALL_CUES)) {
            out.append("fall");
        } else if (containsAny(text, FIRE_CUES)) {
            out.append("fire");
        } else if (containsAny(text, WATER_CUES)) {
            out.append("water");
        }
        if (containsAny(text, AGENT_MARKERS)
                || containsAny(text, ACTIVE_KILL_VERBS)) {
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append("by a player");
        }
        return out.length() == 0 ? "unknown cause" : out.toString();
    }

    /**
     * Splits on anything that is not a letter, a digit or an underscore, so
     * "MORTO(A)" is two words and a username keeps its underscores - those are the
     * only punctuation Minecraft allows in a name.
     */
    private static String[] words(String text) {
        List<String> out = new ArrayList<String>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                word.append(c);
            } else if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) {
            out.add(word.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    private static int indexOfWord(String[] words, String needle) {
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOfAnyWord(String[] words, String[] needles) {
        for (int i = 0; i < words.length; i++) {
            if (isAnyWord(words[i], needles)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isAnyWord(String word, String[] needles) {
        for (int i = 0; i < needles.length; i++) {
            if (word.equals(needles[i])) {
                return true;
            }
        }
        return false;
    }

    private static void considerWin(String source, String text) {
        // A loss screen can easily contain a win word ("VICTORY" on the winner's
        // name, "GAME OVER - WINNER: someone"), so these veto the whole line.
        if (containsAny(text, LOSS_PHRASES)) {
            if (containsAny(text, GAME_OVER_PHRASES)) {
                countRound(source, text, false);
            }
            return;
        }
        if (containsAny(text, SELF_WIN_PHRASES)) {
            countWin(source, text);
            return;
        }
        if (containsAny(text, NAMED_WIN_PHRASES)) {
            String name = normalise(username());
            if (name.length() > 0 && containsWord(text, name)) {
                countWin(source, text);
            }
        }
    }

    /**
     * Colour codes stripped, accents folded, upper cased, runs of whitespace
     * collapsed to one space. Punctuation is left in place: whole-word matching
     * handles it, and keeping it makes the logged line the real thing the server
     * sent.
     *
     * <p>Folding the accents is what lets a pure-ASCII needle match what a
     * Brazilian server actually sends. kaizenmc.gg writes "VITORIA" with one, and
     * this class cannot spell it: Gradle compiles with the platform encoding
     * unless told otherwise, so a non-ASCII source file is a mojibake risk.
     * {@link Normalizer.Form#NFD} splits each accented letter into a plain letter
     * plus a combining mark, and the loop below drops the marks - so the file
     * stays ASCII and the match still happens.
     */
    private static String normalise(String text) {
        if (text == null) {
            return "";
        }
        // ROOT locale: under a Turkish default "I" upper cases to a dotless one,
        // which would silently stop "VICTORY" from ever matching.
        String plain = Normalizer.normalize(
                StringUtils.stripControlCodes(text), Normalizer.Form.NFD)
                .toUpperCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(plain.length());
        boolean pendingSpace = false;
        for (int i = 0; i < plain.length(); i++) {
            char c = plain.charAt(i);
            if (c <= ' ') {
                pendingSpace = out.length() > 0;
                continue;
            }
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;   // an accent NFD has just separated from its letter
            }
            if (pendingSpace) {
                out.append(' ');
                pendingSpace = false;
            }
            out.append(c);
        }
        return out.toString();
    }

    private static boolean containsAny(String text, String[] phrases) {
        for (int i = 0; i < phrases.length; i++) {
            if (containsWord(text, phrases[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whole-word contains. A plain {@code contains("WON")} would fire on
     * "WONDERFUL" and {@code contains("WINS")} on "TWINS", which on a chat line is
     * not far-fetched.
     */
    private static boolean containsWord(String haystack, String needle) {
        if (haystack == null || needle == null || needle.length() == 0) {
            return false;
        }
        int from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                return false;
            }
            int end = at + needle.length();
            boolean startsClean = at == 0
                    || !Character.isLetterOrDigit(haystack.charAt(at - 1));
            boolean endsClean = end >= haystack.length()
                    || !Character.isLetterOrDigit(haystack.charAt(end));
            if (startsClean && endsClean) {
                return true;
            }
            from = at + 1;
        }
    }

    // ------------------------------------------------------------- diagnostics

    /**
     * Logged once per distinct line, so a countdown ticking or a title resent
     * every second costs one entry rather than one a second.
     */
    private static void logText(String source, String text) {
        synchronized (loggedTexts) {
            if (loggedTexts.contains(text)
                    || loggedTexts.size() >= MAX_TEXTS_LOGGED) {
                return;
            }
            loggedTexts.add(text);
        }
        note(source + " seen | " + text);
    }

    /**
     * A "seen it, ignored it" note, written once per distinct reason. Deaths and
     * wins both arrive as a signal resent every 50ms for a couple of seconds, and
     * before this existed one win title spent 1368 lines - 59% of a whole session's
     * log - saying it had been ignored.
     */
    private static void noteOnce(String key, String line) {
        synchronized (notedOnce) {
            if (notedOnce.contains(key) || notedOnce.size() >= MAX_NOTED_ONCE) {
                return;
            }
            notedOnce.add(key);
        }
        note(line);
    }

    private static void note(String line) {
        synchronized (pendingNotes) {
            if (pendingNotes.size() >= MAX_PENDING_NOTES) {
                return;
            }
            pendingNotes.add(line);
        }
    }

    /**
     * A note about a death-shaped packet, spent against its own budget. Unlike
     * {@link #logText} these are logged every time rather than once per distinct
     * line, because two identical death reports a minute apart are two deaths and
     * the timestamps are the point.
     */
    private static void packetNote(String line) {
        synchronized (SessionStatsManager.class) {
            if (packetNotes >= MAX_PACKET_NOTES) {
                return;
            }
            packetNotes++;
        }
        note(line);
    }

    /** Called from the tick, so every log write happens on the main thread. */
    private static void flushNotes() {
        List<String> lines;
        synchronized (pendingNotes) {
            if (pendingNotes.isEmpty()) {
                return;
            }
            lines = new ArrayList<String>(pendingNotes);
            pendingNotes.clear();
        }
        for (int i = 0; i < lines.size(); i++) {
            try {
                SessionStatsDebug.note(mc.mcDataDir, lines.get(i));
            } catch (Throwable ignored) {
            }
        }
    }

    // ------------------------------------------------------------------ output

    private void publish() {
        try {
            SessionStatsExporter.publish(mc.mcDataDir, username(), getServerName(),
                    kills, deaths, wins, sessionStart);
        } catch (Throwable ignored) {
            // EventBus.post swallows hook exceptions silently, so an escape from
            // here would be invisible rather than loud - catch it on the spot.
        }
    }

    /**
     * Read fresh on every publish rather than cached, so switching account in the
     * alt menu (which calls {@code mc.setSession}) shows up within a second.
     */
    private static String username() {
        try {
            return mc.getSession() == null ? "" : mc.getSession().getUsername();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String getServerName() {
        return mc.getCurrentServerData() != null
                ? mc.getCurrentServerData().serverIP : "Singleplayer";
    }
}
