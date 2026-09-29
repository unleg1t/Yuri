package ddlc.yuri.api.gui.main.api;

import ddlc.yuri.managers.impl.ColorManager;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.awt.Color;

public final class MenuShaderBackground {

    private static final long START_NANOS = System.nanoTime();
    private static MenuShaderBackground instance;

    private boolean failed;
    private int program;
    private int uTimeLocation;
    private int uResolutionLocation;
    private int uColorLocation;

    private static final String VERTEX_SOURCE =
            "#version 120\n" +
                    "varying vec2 vPos;\n" +
                    "void main() {\n" +
                    "    vPos = gl_Vertex.xy;\n" +
                    "    gl_Position = ftransform();\n" +
                    "}\n";

    private static final String FRAGMENT_SOURCE =
            "#version 120\n" +
                    "uniform float uTime;\n" +
                    "uniform vec2 uResolution;\n" +
                    "varying vec2 vPos;\n" +
                    "uniform vec3 color;\n" +
                    "float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123); }\n" +
                    "float noise(vec2 p) {\n" +
                    "    vec2 i = floor(p);\n" +
                    "    vec2 f = fract(p);\n" +
                    "    float a = hash(i);\n" +
                    "    float b = hash(i + vec2(1.0, 0.0));\n" +
                    "    float c = hash(i + vec2(0.0, 1.0));\n" +
                    "    float d = hash(i + vec2(1.0, 1.0));\n" +
                    "    vec2 u = f * f * (3.0 - 2.0 * f);\n" +
                    "    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;\n" +
                    "}\n" +
                    "float fbm(vec2 p) {\n" +
                    "    float total = 0.0;\n" +
                    "    float amp = 0.5;\n" +
                    "    for (int i = 0; i < 5; i++) {\n" +
                    "        total += noise(p) * amp;\n" +
                    "        p *= 2.0;\n" +
                    "        amp *= 0.5;\n" +
                    "    }\n" +
                    "    return total;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 uv = vPos / max(uResolution.y, 1.0);\n" +
                    "    vec2 flow = uv * 1.6 + vec2(uTime * 0.02, uTime * 0.015);\n" +
                    "    float n = fbm(flow);\n" +
                    "    float alpha = n * 0.8;\n" +
                    "    gl_FragColor = vec4(color, alpha);\n" +
                    "}\n";

    private MenuShaderBackground() {
    }

    public static MenuShaderBackground get() {
        if (instance == null) {
            instance = new MenuShaderBackground();
        }
        return instance;
    }

    private int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.err.println("MenuShaderBackground compile error: " + GL20.glGetShaderInfoLog(shader, 4096));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    private boolean init() {
        int vertexShader = compile(GL20.GL_VERTEX_SHADER, VERTEX_SOURCE);
        int fragmentShader = compile(GL20.GL_FRAGMENT_SHADER, FRAGMENT_SOURCE);
        if (vertexShader == 0 || fragmentShader == 0) return false;

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.err.println("MenuShaderBackground link error: " + GL20.glGetProgramInfoLog(program, 4096));
            GL20.glDeleteProgram(program);
            program = 0;
            return false;
        }

        uColorLocation = GL20.glGetUniformLocation(program, "color");
        uTimeLocation = GL20.glGetUniformLocation(program, "uTime");
        uResolutionLocation = GL20.glGetUniformLocation(program, "uResolution");
        return true;
    }

    public void render(float width, float height) {
        if (failed) return;

        if (program == 0 || !GL20.glIsProgram(program)) {
            if (!init()) {
                failed = true;
                return;
            }
        }

        float elapsed = (float) (((System.nanoTime() - START_NANOS) / 1.0e9) % 100000.0);

        int previousProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GL20.glUseProgram(program);
        GL20.glUniform1f(uTimeLocation, elapsed);
        GL20.glUniform2f(uResolutionLocation, width, height);

        Color c = ColorManager.getColor();
        GL20.glUniform3f(uColorLocation, c.getRed() / 255.0f, c.getGreen() / 255.0f, c.getBlue() / 255.0f);

        GlStateManager.disableCull();
        GlStateManager.disableDepth();

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, height);
        GL11.glVertex2f(width, height);
        GL11.glVertex2f(width, 0);
        GL11.glVertex2f(0, 0);
        GL11.glEnd();

        GlStateManager.enableDepth();

        GL20.glUseProgram(previousProgram);

        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}