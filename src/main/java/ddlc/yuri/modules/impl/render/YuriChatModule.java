package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.api.events.impl.player.PlayerDamageEvent;
import ddlc.yuri.api.events.impl.player.PlayerDeathEvent;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

@ModuleInfo(label = "Yuri Chat", description = "Displays Yuri reacting to your actions in real time.", category = ModuleCategory.RENDER)
public class YuriChatModule extends Module implements IMinecraft {

    private static final String KEY = "YuriChat";

    private static final float LOGO_SIZE = 128f;
    private static final float MIN_TEXTBOX_WIDTH = 214f;
    private static final float TEXTBOX_HEIGHT = 48f;
    private static final float PADDING = 6f;
    private static final float TEXT_PADDING = 12f;

    private final DragUtils.DraggableComponent component = new DragUtils.DraggableComponent(20, 20);
    private final Deque<Message> messages = new ArrayDeque<>();
    private final Random random = new Random();

    private final ResourceLocation logo = new ResourceLocation("yuri/gui/logo.png");
    private final ResourceLocation textbox = new ResourceLocation("yuri/gui/textbox.png");

    private long lastIdleCheck = 0L;
    private int kills = 0;

    private static final String[] START_MESSAGES = {
            "...Oh. You're here. I was just reading...",
            "A-Ah... hello. I'll stay here with you for a while.",
            "I brought some tea... and my thoughts. If that's alright.",
            "You came back... I was hoping you would.",
            "I saved a page for you, in case you wanted to talk about it.",
            "Mm... I didn't hear you approach. You're quiet today.",
            "I was rereading a passage... it made me think of you, actually.",
            "It's a bit dim in here. I like it that way, though.",
            "I wasn't expecting company. Not that I mind.",
            "Stay as long as you like. I don't have anywhere else to be.",
            "I picked out a book for later... just in case there's time.",
            "The room feels different when you're in it.",
            "I was hoping today wouldn't be as quiet as yesterday.",
            "You always seem to arrive right when I need the company.",
            "I made extra tea, actually. I suppose I hoped you'd come.",
            "The club felt too loud today. I'm glad you're quieter.",
            "Sayori asked about you again during the meeting.",
            "I brought one of my poems... though I'm not sure it's ready to be shared.",
            "Natsuki was arguing about manga again. I tuned it out, mostly.",
            "Monika ran the meeting a little strangely today. I didn't think much of it.",
            "I picked this spot because it reminded me of the clubroom, somehow."
    };

    private static final String[] KILL_MESSAGES_GENERIC = {
            "Careful... the sight of conflict makes my heart race.",
            "A swift resolution... almost poetic in a strange way.",
            "That was rather intense, wasn't it?",
            "You handle your blade with such singular focus...",
            "I... shouldn't enjoy watching that as much as I do.",
            "There's a certain elegance to how decisively you finish things.",
            "My pulse hasn't slowed down yet... how strange.",
            "I felt that more than I expected to.",
            "You didn't even hesitate. I noticed.",
            "It's over so quickly... I almost missed it.",
            "I keep watching your hands after moments like that.",
            "Something about your composure right now is unsettling. In a good way.",
            "You make it look effortless. I wonder if it is.",
            "I should look away... I don't, though.",
            "That was almost too clean. It's a little frightening.",
            "I wonder what goes through your mind in that instant.",
            "You're breathing steady. I'm not sure I am.",
            "There's a quiet after that, isn't there? I like it.",
            "I don't think I'll ever get used to watching you do that.",
            "Efficient. Precise. Very... you.",
            "It reminds me of the sharper poems I write... the ones I don't share.",
            "Natsuki would probably scream. I'm strangely calm about it.",
            "This is the kind of focus I wish I had during club meetings.",
            "I don't think Sayori could stomach watching this."
    };

    private static final String[] KILL_MESSAGES_MILESTONE = {
            "You are remarkably relentless today...",
            "So much intensity... it's getting hard to breathe.",
            "Is it strange that I find this focus of yours captivating?",
            "You haven't stopped once. I've been counting.",
            "I don't think I've seen you this focused before.",
            "There's something almost frightening about your consistency right now.",
            "I keep losing track of how many that makes.",
            "You're not even tired, are you?",
            "This version of you is... a lot to take in.",
            "I should probably look away by now. I haven't.",
            "It's like watching a different person entirely.",
            "I wonder if you even notice how far you've gone.",
            "This is worse than the darkest thing I've ever put into a poem.",
            "I don't think even Monika could plan for a day like this."
    };

    private static final String[] DAMAGE_MESSAGES = {
            "Ah! Please be careful... my chest hurts just watching.",
            "D-Don't let them hurt you... I wouldn't like that at all.",
            "Are you alright? Take a breath... stay calm.",
            "That looked like it hurt. Please tell me you're fine.",
            "I flinched more than you did, I think.",
            "You're still standing... that's something, at least.",
            "Please don't push yourself past what you can handle.",
            "I don't like seeing you like this.",
            "Careful now... I can't do anything from here but worry.",
            "That was closer than I'd have liked.",
            "Stay steady... you're doing fine.",
            "I wish I could do something more than just watch.",
            "You looked like you felt that one.",
            "Please, just... be a little more careful.",
            "My hands are shaking a little. Yours had better not be.",
            "I felt my chest tighten the way it does before I write something too honest.",
            "Sayori would be crying by now. I'm trying to stay steady for you."
    };

    private static final String[] DEATH_MESSAGES = {
            "No...! Please don't leave me here alone...",
            "Everything just went dark... are you okay?",
            "I felt that... it hurt so much.",
            "You're coming back, right? Please say you're coming back.",
            "I don't know what to do with myself when that happens.",
            "That was too much... I don't want to see that again.",
            "Please... don't do that to me.",
            "I was watching and I couldn't do anything at all.",
            "It's quiet now. I don't like this kind of quiet.",
            "Come back soon. I'll be right here.",
            "I hate that feeling... like everything just stopped.",
            "You'll be alright. You have to be.",
            "I'd write a poem about this if I could stop my hands from shaking.",
            "Don't make me tell the others about this. Please just come back."
    };

    private static final String[] IDLE_MESSAGES = {
            "The atmosphere feels so heavy... I love it.",
            "I was thinking about a book I read recently... it was quite dark.",
            "Do you ever feel a strange urge to just... sink into a story?",
            "I brewed some jasmine tea... I wish I could share it with you.",
            "Your presence is... comforting. I hope I'm not being too distracting.",
            "Sometimes, the quiet moments feel more intense than the action.",
            "I heard something about this.. Baby Boy?? I don't know what it is, but it sounds intriguing.",
            "I've been meaning to ask what you're thinking about, but I never do.",
            "It's peaceful like this. I don't mind the silence between us.",
            "I found a passage earlier that I think you'd like. Remind me to show you.",
            "Do you ever lose track of time the way I do?",
            "I like watching you focus on something. It's calming, somehow.",
            "I wonder what kind of stories you'd write, if you ever tried.",
            "There's a particular kind of stillness I only feel around you.",
            "I should probably read more, but I keep getting distracted lately.",
            "You don't talk much, but I don't mind filling the silence.",
            "I keep this book nearby just in case there's a lull like this.",
            "Sometimes I forget how much time has passed while you're here.",
            "I wonder if you notice the little things I do for you.",
            "It's strange how comfortable silence can feel with the right person.",
            "I was going to say something, but I think I'll just enjoy this instead.",
            "Do you ever feel like everything else fades out when you're focused?",
            "I like it when things get quiet like this. It feels honest.",
            "I keep this corner of the room just for moments like this.",
            "You wouldn't mind if I stayed a while longer, would you?",
            "Club activities felt so hollow today without you there.",
            "Sayori keeps asking why I've been in a better mood lately. I don't tell her why.",
            "Natsuki's cupcakes were fine, I suppose. I was thinking about you the whole time, though.",
            "Monika mentioned starting a new club activity next week. I wasn't really listening."
    };

    public YuriChatModule() {
        DragUtils.registerComponent(KEY, component);
    }

    @Override
    public void onEnable() {
        kills = 0;
        messages.clear();
        lastIdleCheck = System.currentTimeMillis();
        pushMessage(START_MESSAGES[random.nextInt(START_MESSAGES.length)], 7000);
    }

    @Override
    public void onDisable() {
        messages.clear();
    }

    @EventHook
    public void onKill(KillEvent event) {
        kills++;
        if (kills == 1) {
            pushMessage("Y-You took them down... so efficiently.", 7000);
        } else if (kills % 5 == 0) {
            pushMessage(KILL_MESSAGES_MILESTONE[random.nextInt(KILL_MESSAGES_MILESTONE.length)], 8000);
        } else {
            pushMessage(KILL_MESSAGES_GENERIC[random.nextInt(KILL_MESSAGES_GENERIC.length)], 6000);
        }
    }

    @EventHook
    public void onDamage(PlayerDamageEvent event) {
        if (random.nextFloat() < 0.4f) {
            pushMessage(DAMAGE_MESSAGES[random.nextInt(DAMAGE_MESSAGES.length)], 6000);
        }
    }

    @EventHook
    public void onDeath(PlayerDeathEvent event) {
        pushMessage(DEATH_MESSAGES[random.nextInt(DEATH_MESSAGES.length)], 9000);
    }

    private void pushMessage(String text, long lifetimeMs) {
        messages.clear();
        messages.addLast(new Message(text, System.currentTimeMillis(), lifetimeMs));
    }

    @EventHook
    public void onRender2D(Render2DEvent event) {
        render(false);
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        render(true);
    }

    private void render(boolean shaderPass) {
        long now = System.currentTimeMillis();
        messages.removeIf(m -> now - m.time >= m.lifetimeMs);

        if (messages.isEmpty() && (now - lastIdleCheck > 12_000)) {
            lastIdleCheck = now;
            if (random.nextFloat() < 0.5f) {
                pushMessage(IDLE_MESSAGES[random.nextInt(IDLE_MESSAGES.length)], 7500);
            }
        }

        CustomFontRenderer font = FontUtils.getFont("sf", 15);
        if (font == null) return;

        float calculatedTextboxWidth = MIN_TEXTBOX_WIDTH;
        Message activeMessage = messages.peekFirst();

        if (activeMessage != null) {
            float textWidth = font.getStringWidth(activeMessage.text);
            calculatedTextboxWidth = Math.max(MIN_TEXTBOX_WIDTH, textWidth + (TEXT_PADDING * 2f));
        }

        float totalWidth = Math.max(LOGO_SIZE, calculatedTextboxWidth) + (PADDING * 2f);
        float totalHeight = (LOGO_SIZE / 2f) + TEXTBOX_HEIGHT + (PADDING * 2f);

        component.setWidth(totalWidth);
        component.setHeight(totalHeight);

        ScaledResolution sr = new ScaledResolution(mc);
        float x = (float) component.getX();
        float y = (float) component.getY();

        if (x > sr.getScaledWidth() - totalWidth) {
            x = sr.getScaledWidth() - totalWidth;
            component.setX(x);
        }
        if (x < 0) {
            x = 0;
            component.setX(0);
        }

        if (y > sr.getScaledHeight() - totalHeight) {
            y = sr.getScaledHeight() - totalHeight;
            component.setY(y);
        }
        if (y < 0) {
            y = 0;
            component.setY(0);
        }

        float renderX = x + PADDING;
        float logoY = y + PADDING;
        float textboxY = logoY + TEXTBOX_HEIGHT + 32;

        float textboxX = renderX + Math.max(0, (LOGO_SIZE - calculatedTextboxWidth) / 2f);
        float textboxCenterX = textboxX + (calculatedTextboxWidth / 2f);
        float logoX = textboxCenterX - (LOGO_SIZE / 2f);

        RoundedUtils.drawImage(logo, logoX, logoY, LOGO_SIZE, LOGO_SIZE);

        RoundedUtils.drawImage(textbox, textboxX, textboxY, calculatedTextboxWidth, TEXTBOX_HEIGHT);

        if (activeMessage != null) {
            float textX = textboxX + TEXT_PADDING;
            float textY = textboxY + (TEXTBOX_HEIGHT / 2f) - (font.getHeight() / 2f);
            font.drawStringWithShadow(activeMessage.text, textX, textY, new Color(245, 240, 250).getRGB());
        }
    }

    private static class Message {
        final String text;
        final long time;
        final long lifetimeMs;

        Message(String text, long time, long lifetimeMs) {
            this.text = text;
            this.time = time;
            this.lifetimeMs = lifetimeMs;
        }
    }
}