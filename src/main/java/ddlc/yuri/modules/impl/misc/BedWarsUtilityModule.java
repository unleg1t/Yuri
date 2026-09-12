package ddlc.yuri.modules.impl.misc;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.PacketReceivedEvent;
import ddlc.yuri.api.events.impl.player.PreUpdateEvent;
import ddlc.yuri.api.events.impl.world.WorldJoinEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.potion.Potion;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StringUtils;

import java.util.Collection;
import java.util.HashSet;

@ModuleInfo(label = "BedWars Utility", description = "Utility module for BedWars", category = ModuleCategory.MISC)
public class BedWarsUtilityModule extends Module {

    private final Collection<EntityPlayer> ironSword = new HashSet<>();
    private final Collection<EntityPlayer> diamondSword = new HashSet<>();
    private final Collection<EntityPlayer> stoneSword = new HashSet<>();
    private final Collection<EntityPlayer> diamondArmor = new HashSet<>();
    private final Collection<EntityPlayer> chainArmor = new HashSet<>();
    private final Collection<EntityPlayer> ironArmor = new HashSet<>();
    private final Collection<EntityPlayer> invisible = new HashSet<>();

    private final Property<Boolean> swords = new Property<Boolean>("Sword Reveal", true);
    private final Property<Boolean> includeStone = new Property<Boolean>("Include Stone", false, () -> !swords.getValue());
    private final Property<Boolean> armor = new Property<Boolean>("Armor Reveal", true);
    private final Property<Boolean> invisibleCheck = new Property<Boolean>("Invisible Check", true);
    private final Property<Boolean> potionInvis = new Property<Boolean>("Invisibility Status", false);

    private boolean wasThePlayerInvis = false;
    private boolean check = false;

    @Override
    public void onEnable() {
        this.diamondSword.clear();
        this.ironSword.clear();
        this.stoneSword.clear();
        this.chainArmor.clear();
        this.ironArmor.clear();
        this.diamondArmor.clear();
        this.invisible.clear();
    }

    @EventHook
    public void onWorldJoin(WorldJoinEvent event) {
        this.diamondSword.clear();
        this.ironSword.clear();
        this.stoneSword.clear();
        this.chainArmor.clear();
        this.ironArmor.clear();
        this.diamondArmor.clear();
        this.invisible.clear();
        check = false;
    }

    @EventHook
    public void onPacketReceived(PacketReceivedEvent event) {
        if (mc.thePlayer == null || !(event.getPacket() instanceof S02PacketChat))
            return;

        S02PacketChat packetChat = (S02PacketChat) event.getPacket();
        String chatComponent = packetChat.getChatComponent().getUnformattedText();

        String strippedMessage = StringUtils.stripColor(chatComponent);
        if (strippedMessage.startsWith(" ") && (strippedMessage.contains("Protect your bed and destroy the enemy beds.") || strippedMessage.contains("Goodluck with your BedWars Game"))) {
            Yuri.INSTANCE.getNotificationHandler().pop(getLabel(),EnumChatFormatting.GREEN + "Game has started!");
            mc.thePlayer.playSound("note.pling", 1.0f, 1.0f);
            check = true;
        }
    }

    @EventHook
    public void onPreUpdate(PreUpdateEvent event) {
        if (!check) return;
        for (final EntityPlayer entity : mc.theWorld.playerEntities) {
            if (mc.thePlayer != null || mc.theWorld != null) {
                if (entity.getHeldItem() != null) {
                    final Item heldItem = entity.getHeldItem().getItem();

                    if (this.swords.getValue()) {
                        if (heldItem instanceof ItemSword) {
                            final String type = ((ItemSword) heldItem).getToolMaterialName().toLowerCase();

                            if (type.contains("iron")) {
                                if (!this.ironSword.contains(entity)) {
                                    this.ironSword.add(entity);
                                    Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has an " + EnumChatFormatting.AQUA + "Iron Sword");
                                }
                            }

                            if (type.contains("emerald")) {
                                if (!this.diamondSword.contains(entity)) {
                                    this.diamondSword.add(entity);
                                    Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has a " + EnumChatFormatting.AQUA + "Diamond Sword");
                                }
                            }

                            if (type.contains("stone")) {
                                if (!stoneSword.contains(entity)) {
                                    this.stoneSword.add(entity);
                                    if (this.includeStone.getValue()) {
                                        Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has a " + EnumChatFormatting.AQUA + "Stone Sword");
                                    }
                                }
                            }

                            if (type.contains("wood")) {
                                this.stoneSword.remove(entity);
                                this.ironSword.remove(entity);
                                diamondSword.remove(entity);
                            }
                        }
                    }
                }
                if (this.armor.getValue()) {
                    ItemStack entityCurrentArmor = entity.getCurrentArmor(1);

                    if (entityCurrentArmor != null && entityCurrentArmor.getItem() instanceof ItemArmor) {

                        if (((ItemArmor) entityCurrentArmor.getItem()).getArmorMaterial().equals(ItemArmor.ArmorMaterial.CHAIN)) {
                            if (!chainArmor.contains(entity)) {
                                chainArmor.add(entity);
                                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has " + EnumChatFormatting.LIGHT_PURPLE + "Chain Armor");
                            }
                        }

                        if (((ItemArmor) entityCurrentArmor.getItem()).getArmorMaterial().equals(ItemArmor.ArmorMaterial.IRON)) {
                            if (!this.ironArmor.contains(entity)) {
                                this.ironArmor.add(entity);
                                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has " + EnumChatFormatting.LIGHT_PURPLE + "Iron Armor");
                            }
                        }

                        if (((ItemArmor) entityCurrentArmor.getItem()).getArmorMaterial().equals(ItemArmor.ArmorMaterial.DIAMOND)) {
                            if (!this.diamondArmor.contains(entity)) {
                                this.diamondArmor.add(entity);
                                Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " has " + EnumChatFormatting.LIGHT_PURPLE + "Diamond Armor");
                            }
                        }

                        if (((ItemArmor) entityCurrentArmor.getItem()).getArmorMaterial().equals(ItemArmor.ArmorMaterial.LEATHER)) {
                            this.diamondArmor.remove(entity);
                            this.ironArmor.remove(entity);
                            this.chainArmor.remove(entity);
                        }
                    }
                }

                if (this.invisibleCheck.getValue()) {
                    if (entity.getActivePotionEffect(Potion.invisibility) != null) {
                        if (!this.invisible.contains(entity)) {
                            this.invisible.add(entity);
                            Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " is now " + EnumChatFormatting.GOLD + "Invisible");
                        }
                    } else if (this.invisible.contains(entity)) {
                        this.invisible.remove(entity);
                        Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Player " + EnumChatFormatting.RED + entity.getDisplayName().getFormattedText() + EnumChatFormatting.WHITE + " is now " + EnumChatFormatting.GOLD + "Visible");
                    }
                }

                if (this.potionInvis.getValue()) {
                    if (mc.thePlayer.getActivePotionEffect(Potion.invisibility) != null) {
                        wasThePlayerInvis = true;
                        if (mc.thePlayer.ticksExisted % 200 == 0) {
                            Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Your Invisibility" + EnumChatFormatting.RED + " expires " + EnumChatFormatting.RESET + "in " + EnumChatFormatting.RED + mc.thePlayer.getActivePotionEffect(Potion.invisibility).getDuration() / 20 + EnumChatFormatting.RESET + " second(s)");
                        }
                    }
                } else if (wasThePlayerInvis) {
                    Yuri.INSTANCE.getNotificationHandler().pop(getLabel(), "Invisibility" + EnumChatFormatting.RED + " Expired");
                    wasThePlayerInvis = false;
                }

            } else {
                this.diamondSword.clear();
                this.ironSword.clear();
                this.stoneSword.clear();
                this.diamondArmor.clear();
                this.ironArmor.clear();
                this.chainArmor.clear();
                this.invisible.clear();
            }
        }
    }
}
