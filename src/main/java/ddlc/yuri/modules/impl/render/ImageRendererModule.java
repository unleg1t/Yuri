package ddlc.yuri.modules.impl.render;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.render.Render2DEvent;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.DragUtils;
import ddlc.yuri.utils.render.RenderUtils.GifTexture;
import ddlc.yuri.utils.render.RoundedUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

@ModuleInfo(label = "Image Renderer", description = "Renders a custom image or animated GIF on screen", category = ModuleCategory.RENDER)
public final class ImageRendererModule extends Module {

    public enum Images {
        YURI("Yuri", false),
        YURI_2("Yuri 2", false),
        YURI_3("Yuri 3", false),
        YURI_NSFW("Yuri NSFW", false),
        NATSUKI("Natsuki", false),
        DEATH_THREATS("Death Threats", true),
        NEP("Nep", false),
        GAMER("Gamer", false),
        CUSTOM("Custom", false);

        public final String name;
        public final boolean isGif;

        Images(String name, boolean isGif) {
            this.name = name;
            this.isGif = isGif;
        }

        Images(String name) {
            this(name, false);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public final ModeProperty<Images> image = new ModeProperty<>("Image", Images.YURI);
    public final Property<String> customUrl = new Property<>("URL", "https://i.imgur.com/example.gif");
    public static NumberProperty size = new NumberProperty("Size", 100, 100, 1000, 50);

    public static final ImageRendererModule INSTANCE = new ImageRendererModule();

    private final DragUtils.DraggableComponent draggable = new DragUtils.DraggableComponent(100, 100);

    private String lastLoadedUrl = "";
    private ResourceLocation customTextureLocation = null;
    private GifTexture customGifTexture = null;
    private DynamicTexture dynamicTexture = null;
    private boolean isLoading = false;

    private Images lastPresetImage = null;
    private GifTexture presetGifTexture = null;

    public ImageRendererModule() {
        DragUtils.registerComponent("ImageRenderer", draggable);
    }

    @EventHook
    public void onRender(Render2DEvent event) {
        render();
    }

    @EventHook
    public void onShader2D(Shader2DEvent event) {
        if (event.getShaderType() == Shader2DEvent.ShaderType.BLUR) return;
        render();
    }

    private void render() {
        int currentSize = size.getValue().intValue();
        draggable.setWidth(currentSize);
        draggable.setHeight(currentSize);

        int renderX = (int) draggable.getX();
        int renderY = (int) draggable.getY();

        Images selectedMode = image.getValue();

        if (selectedMode == Images.CUSTOM) {
            updateCustomTexture(customUrl.getValue());

            if (customGifTexture != null && customGifTexture.getFrameCount() > 0) {
                RoundedUtils.drawRoundedGif(customGifTexture, renderX, renderY, currentSize, currentSize, 6f);
            } else if (customTextureLocation != null) {
                RoundedUtils.drawRoundedImage(customTextureLocation, renderX, renderY, currentSize, currentSize, 6f);
            }
        } else if (selectedMode.isGif) {
            updatePresetGif(selectedMode);

            if (presetGifTexture != null && presetGifTexture.getFrameCount() > 0) {
                RoundedUtils.drawRoundedGif(presetGifTexture, renderX, renderY, currentSize, currentSize, 6f);
            }
        } else {
            String imageName = selectedMode.toString().toLowerCase().replace(" ", "_");
            ResourceLocation imageLocation = new ResourceLocation("yuri/images/" + imageName + ".png");
            RoundedUtils.drawRoundedImage(imageLocation, renderX, renderY, currentSize, currentSize, 6f);
        }
    }

    private void updatePresetGif(Images mode) {
        if (mode == lastPresetImage) {
            return;
        }

        if (presetGifTexture != null) {
            presetGifTexture.clear();
            presetGifTexture = null;
        }

        lastPresetImage = mode;
        String fileName = mode.toString().toLowerCase().replace(" ", "_") + ".gif";
        ResourceLocation gifLocation = new ResourceLocation("yuri/images/" + fileName);

        try (InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(gifLocation).getInputStream()) {
            presetGifTexture = new GifTexture(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCustomTexture(String urlString) {
        if (urlString == null || urlString.trim().isEmpty() || urlString.equals(lastLoadedUrl) || isLoading) {
            return;
        }

        lastLoadedUrl = urlString;
        isLoading = true;

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (InputStream is = connection.getInputStream()) {
                    byte[] data = new byte[8192];
                    int nRead;
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                }
                byte[] bytes = buffer.toByteArray();

                boolean isGif = bytes.length >= 4 &&
                        bytes[0] == 0x47 && bytes[1] == 0x49 &&
                        bytes[2] == 0x46 && bytes[3] == 0x38;

                if (isGif) {
                    GifTexture gif = new GifTexture(new ByteArrayInputStream(bytes));
                    if (gif.getFrameCount() > 0) {
                        Minecraft.getMinecraft().addScheduledTask(() -> {
                            cleanupTextures();
                            customGifTexture = gif;
                            isLoading = false;
                        });
                        return;
                    }
                }

                BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
                if (bufferedImage != null) {
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                        cleanupTextures();
                        dynamicTexture = new DynamicTexture(bufferedImage);
                        customTextureLocation = Minecraft.getMinecraft().getTextureManager()
                                .getDynamicTextureLocation("custom_image_renderer", dynamicTexture);
                        isLoading = false;
                    });
                } else {
                    isLoading = false;
                }

            } catch (Exception e) {
                isLoading = false;
            }
        });
    }

    private void cleanupTextures() {
        if (dynamicTexture != null) {
            dynamicTexture.deleteGlTexture();
            dynamicTexture = null;
        }
        customTextureLocation = null;
        if (customGifTexture != null) {
            customGifTexture.clear();
            customGifTexture = null;
        }
        if (presetGifTexture != null) {
            presetGifTexture.clear();
            presetGifTexture = null;
        }
        lastPresetImage = null;
    }
}