package ddlc.yuri.api.gui.click.yuri;

import ddlc.yuri.api.config.GithubConfigFetcher;
import ddlc.yuri.api.font.CustomFontRenderer;
import ddlc.yuri.utils.render.FontUtils;
import ddlc.yuri.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class OnlineConfigsTab implements ConfigPanelTab {

    private static final float ITEM_HEIGHT = 18f;
    private static final float ITEM_GAP = 2f;

    private final List<String> configs = new CopyOnWriteArrayList<>();
    private final ScrollableList scrollableList = new ScrollableList();
    private boolean loadingList = false;
    private String statusText = null;
    private long statusTimer = 0;

    @Override
    public String getLabel() {
        return "Online";
    }

    @Override
    public void onShown() {
        if (configs.isEmpty() && !loadingList) {
            refreshConfigs();
        }
    }

    public void refreshConfigs() {
        if (loadingList) return;
        loadingList = true;
        statusText = "Fetching...";
        CompletableFuture.runAsync(() -> {
            List<String> fetched = GithubConfigFetcher.fetchConfigList();
            configs.clear();
            configs.addAll(fetched);
            loadingList = false;
            statusText = fetched.isEmpty() ? "No configs" : null;
        });
    }

    @Override
    public void draw(ConfigPanelContext ctx) {
        CustomFontRenderer font = FontUtils.getFont("sf", 12);

        if (System.currentTimeMillis() - statusTimer > 3000 && statusText != null && !loadingList && !statusText.equals("No configs")) {
            statusText = null;
        }

        if (statusText != null) {
            font.drawCenteredStringWithShadow(statusText, ctx.contentX + ctx.contentWidth / 2f,
                    ctx.contentY + ctx.contentHeight / 2f, RenderUtils.withAlpha(Theme.TEXT_MUTED, ctx.animAlpha));
            return;
        }

        if (configs.isEmpty()) return;

        ctx.beginScissor(ctx.contentX, ctx.contentY, ctx.contentWidth, ctx.contentHeight);
        try {
            float totalHeight = configs.size() * ITEM_HEIGHT;
            float scrollOffset = scrollableList.update(totalHeight, ctx.contentHeight);

            float currentY = ctx.contentY - scrollOffset;
            for (String configName : configs) {
                if (currentY + ITEM_HEIGHT >= ctx.contentY && currentY <= ctx.contentY + ctx.contentHeight) {
                    boolean itemHovered = ctx.mouseX >= ctx.contentX && ctx.mouseX <= ctx.contentX + ctx.contentWidth
                            && ctx.mouseY >= currentY && ctx.mouseY <= currentY + ITEM_HEIGHT - ITEM_GAP;

                    Color itemBg = itemHovered
                            ? RenderUtils.withAlphaColor(Theme.MODULE_HOVER, ctx.scaledAlpha(Theme.MODULE_HOVER))
                            : RenderUtils.withAlphaColor(Theme.BAR_BG, ctx.scaledAlpha(Theme.BAR_BG));

                    Gui.drawRect2(ctx.contentX, currentY, ctx.contentWidth, ITEM_HEIGHT - ITEM_GAP, itemBg.getRGB());

                    Color textColor = itemHovered ? Theme.TEXT : Theme.TEXT_MUTED;
                    font.drawString(configName, ctx.contentX + 4f,
                            currentY + (ITEM_HEIGHT - ITEM_GAP - font.getHeight()) / 2f,
                            RenderUtils.withAlpha(textColor, ctx.animAlpha));
                }
                currentY += ITEM_HEIGHT;
            }
        } finally {
            ctx.endScissor();
        }
    }

    @Override
    public boolean mouseClicked(ConfigPanelContext ctx, int mouseX, int mouseY, int button) {
        if (loadingList || configs.isEmpty() || button != 0) return false;
        if (mouseY < ctx.contentY || mouseY > ctx.contentY + ctx.contentHeight) return false;

        float currentY = ctx.contentY - scrollableList.getScrollOffset();
        for (String configName : configs) {
            if (mouseX >= ctx.contentX && mouseX <= ctx.contentX + ctx.contentWidth
                    && mouseY >= currentY && mouseY <= currentY + ITEM_HEIGHT - ITEM_GAP) {
                statusText = "Loading...";
                CompletableFuture.runAsync(() -> {
                    boolean success = GithubConfigFetcher.downloadAndLoadConfig(configName);
                    statusText = success ? "Loaded!" : "Failed!";
                    statusTimer = System.currentTimeMillis();
                });
                return true;
            }
            currentY += ITEM_HEIGHT;
        }
        return false;
    }

    @Override
    public boolean scroll(float amount) {
        scrollableList.scroll(amount);
        return true;
    }
}
