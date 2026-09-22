package ddlc.yuri.api.gui.alt;

import net.minecraft.client.Minecraft;

abstract class AltTab {

    protected final YuriAltMenu menu;
    protected final Minecraft mc = Minecraft.getMinecraft();

    protected int contentX, contentY, contentWidth, contentHeight;

    AltTab(YuriAltMenu menu) {
        this.menu = menu;
    }

    abstract String title();

    void layout(int x, int y, int w, int h) {
        contentX = x;
        contentY = y;
        contentWidth = w;
        contentHeight = h;
    }

    abstract void draw(int mouseX, int mouseY);

    abstract boolean mouseClicked(int mouseX, int mouseY, int mouseButton);

    void mouseReleased(int mouseX, int mouseY, int state) {
    }

    void mouseClickMove(int mouseX, int mouseY) {
    }

    void mouseScrolled(int wheel) {
    }

    void keyTyped(char typedChar, int keyCode) {
    }

    void onShow() {
    }
}
