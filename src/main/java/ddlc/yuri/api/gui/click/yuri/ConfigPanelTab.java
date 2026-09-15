package ddlc.yuri.api.gui.click.yuri;

public interface ConfigPanelTab {

    String getLabel();

    void draw(ConfigPanelContext ctx);

    boolean mouseClicked(ConfigPanelContext ctx, int mouseX, int mouseY, int button);

    default boolean keyTyped(char typedChar, int keyCode) {
        return false;
    }

    default boolean scroll(float amount) {
        return false;
    }

    default void onShown() {
    }
}
