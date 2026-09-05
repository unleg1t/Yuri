package net.minecraft.client.gui;

import ddlc.yuri.api.gui.main.YuriMenu;
import ddlc.yuri.managers.impl.LastConnectionManager;
import ddlc.yuri.utils.render.FontUtils;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.IChatComponent;

import java.io.IOException;
import java.util.List;

public class GuiDisconnected extends GuiScreen
{
    private String reason;
    private IChatComponent message;
    private List<String> multilineMessage;
    private final GuiScreen parentScreen;
    private int field_175353_i;

    public GuiDisconnected(GuiScreen screen, String reasonLocalizationKey, IChatComponent chatComp)
    {
        this.parentScreen = screen;
        this.reason = I18n.format(reasonLocalizationKey, new Object[0]);
        this.message = chatComp;
    }

    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
    }

    public void initGui()
    {
        this.buttonList.clear();
        this.multilineMessage = this.fontRendererObj.listFormattedStringToWidth(this.message.getFormattedText(), this.width - 50);
        this.field_175353_i = this.multilineMessage.size() * FontUtils.getFont("sf", 18).FONT_HEIGHT;
        int padding = 3;

        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 2 + this.field_175353_i / 2 + FontUtils.getFont("sf", 18).FONT_HEIGHT, 100 - padding, 20, "Leave"));
        this.buttonList.add(new GuiButton(1, this.width / 2 + padding, this.height / 2 + this.field_175353_i / 2 + FontUtils.getFont("sf", 18).FONT_HEIGHT, 100 - padding, 20, "Reconnect"));
    }

    protected void actionPerformed(final GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(this.parentScreen);
        }

        if (button.id == 1) {
            this.mc.displayGuiScreen(new GuiConnecting(new GuiMultiplayer(new YuriMenu()), this.mc, new ServerData("", LastConnectionManager.ip, false)));
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.drawCenteredString(this.fontRendererObj, this.reason, this.width / 2, this.height / 2 - this.field_175353_i / 2 - this.fontRendererObj.FONT_HEIGHT * 2, 11184810);
        int i = this.height / 2 - this.field_175353_i / 2;

        if (this.multilineMessage != null)
        {
            for (String s : this.multilineMessage)
            {
                this.drawCenteredString(this.fontRendererObj, s, this.width / 2, i, 16777215);
                i += this.fontRendererObj.FONT_HEIGHT;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
