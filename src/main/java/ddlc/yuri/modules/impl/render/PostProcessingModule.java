package ddlc.yuri.modules.impl.render;

import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.impl.render.Shader2DEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.modules.ModuleInfo;
import ddlc.yuri.utils.render.RenderUtils;
import ddlc.yuri.utils.render.shader.impl.Bloom;
import ddlc.yuri.utils.render.shader.impl.Blur;
import ddlc.yuri.utils.render.shader.impl.Shadow;
import net.minecraft.client.shader.Framebuffer;

@ModuleInfo(label = "Post Processing", description = "Handles post-processing effects like blur and shadows.", category = ModuleCategory.RENDER)
public class PostProcessingModule extends Module {
    private static final Property<Boolean> blur = new Property<>("Blur", true);
    public static final NumberProperty blurRadius = new NumberProperty("Blur Radius", 10.0, 1.0, 128.0, 1.0, blur::getValue);
    public final Property<Boolean> bloom = new Property<>("Bloom", true);
    public final NumberProperty bloomRadius = new NumberProperty("Bloom Radius", 2, 1, 8, 1, bloom::getValue);
    public final NumberProperty bloomOffset = new NumberProperty("Bloom Offset",  1, 1, 10, 1, bloom::getValue);
    public final NumberProperty bloomStrength = new NumberProperty("Bloom Strength", 2.5, 0.5, 8.0, 0.1, bloom::getValue);
    public final static Property<Boolean> shadow = new Property<>("Shadow", true);
    public final static NumberProperty shadowStrength = new NumberProperty("Shadow Strength", 1.2, 0.0, 5.0, 0.1, shadow::getValue);;

    public static Framebuffer stencilFramebuffer = new Framebuffer(1, 1, false);
    public static Framebuffer bloomFramebuffer = new Framebuffer(1, 1, false);

    public void renderShaders() {
        if (!this.isEnabled()) return;

        if (blur.getValue()) {
            Blur.startBlur();
            Yuri.INSTANCE.getEventBus().post(new Shader2DEvent(Shader2DEvent.ShaderType.BLUR));
            Blur.endBlur(blurRadius.getValue().floatValue(), 1.0f, 1.0f);
            RenderUtils.resetColor();
        }

        if (bloom.getValue()) {
            bloomFramebuffer = RenderUtils.createFrameBuffer(bloomFramebuffer);
            bloomFramebuffer.framebufferClear();
            bloomFramebuffer.bindFramebuffer(false);
            Yuri.INSTANCE.getEventBus().post(new Shader2DEvent(Shader2DEvent.ShaderType.BLOOM));
            bloomFramebuffer.unbindFramebuffer();

            if (bloomFramebuffer.framebufferTexture > 0) {
                Bloom.renderBloom(
                        bloomFramebuffer.framebufferTexture,
                        bloomRadius.getValue().intValue(),
                        bloomOffset.getValue().intValue(),
                        bloomStrength.getValue().floatValue()
                );
            }
        }

        if (shadow.getValue()) {
            stencilFramebuffer = RenderUtils.createFrameBuffer(stencilFramebuffer, true);
            stencilFramebuffer.framebufferClear();
            stencilFramebuffer.bindFramebuffer(true);
            RenderUtils.resetColor();
            Yuri.INSTANCE.getEventBus().post(new Shader2DEvent(Shader2DEvent.ShaderType.SHADOW));
            stencilFramebuffer.unbindFramebuffer();
            RenderUtils.resetColor();

            if (stencilFramebuffer.framebufferTexture > 0) {
                Shadow.renderShadow(
                        stencilFramebuffer.framebufferTexture,
                        18,
                        1,
                        shadowStrength.getValue().floatValue()
                );
            }
        }
    }
}
