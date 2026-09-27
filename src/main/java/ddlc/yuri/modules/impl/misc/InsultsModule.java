package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.player.KillEvent;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.lang3.RandomUtils;

import java.util.Arrays;
import java.util.List;

@ModuleInfo(label = "Insults", description =  "Insult people on kill", category = ModuleCategory.MISC)
public final class InsultsModule extends Module {

    private final String[] defaultInsults = {
            // added from my old client
            // if anyone has more let me know! -unlegit

            "\"Cool man the sex man\" Awesome, so when did you lose your virginity? you do realize that's illegal because you aren't over the age of 18, right? Sorry, I'll correct myself. It is, by law, legal to have sexual intercourse once both consenting participants are above the legal age of consent, which in most cases is 16 to 17. Based on your immaturity regarding your name, and commend, and lack of grammar + punctuation, and lack of basic human respect towards others. I seriously doubt that you are 16 or 17, much less 18 or above. So, I would recommend that you do some investigating inside of yourself. Gosh. I can't think of the term... Oh! It's uhm... hm. how about you find God. Because clearly you fucking need it, you pathetic delinquent. I'd be surprised if you've even read this far honestly. What, gonna reply with, and i quote \"i ain't readin allat.\" Cool, go sky dive off of a building with a drop that is fatal. Don't bother bringing a parachute, this world would be a much better place if you dove without one. \"fake smile ahh art\" Shit, you make me sick.",
            "maybe i want u to put ur hands where u want to :3",
            "imagine getting killed by a client thats pasted from rise",
            "bald red man wants u to go get yuri @github/unleg1t/Yuri",
            "my dog itches so can u like itch it for me",
            "\"best aac bypasses!!1!1\" like bro stfu ur client is so donkey dooks",
            "how wood would alan wood suck if alan wood could suck wood?",
            "ouija board vs 25 woke students. im charlie kirk back from the dead mf",
            "i bet u love receiving backshots from KotlinProject",
            "unleg1t, is the best client dev of all time. get yuri @github/unleg1t/Yuri",
            "i paste them astolfo scripts like its ur moms first taste of my dih.",
            "rawr xD x3 nuzzles u UwU",
            "womp womp",
            "sniped by ducky $$ get my client @github/unleg1t/Yuri",
            "get beamed by merch f;uck the opps n;igga. on for 4nim. that n;igga Dylan's mom dead he dead as h;ell",
            "polar pop bypass $$",
            "go back to 2022 skid #famous",
            "it's my b-day. b nice 2 me :<",
            "bombies is my little $1utt",
            "\"i'm not a furry but i do like to be called daddy uwu\"",
            "i knew some1 tht said he would let bombies stack donuts on it.. i agree."};

    @EventHook
    public void onKill(KillEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            mc.thePlayer.sendChatMessage(defaultInsults[RandomUtils.nextInt(0, defaultInsults.length)]);
        }
    }
}