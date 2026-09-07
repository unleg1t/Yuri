package ddlc.yuri.api.gui.main.api;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class MenuShaderBackground {

    private static MenuShaderBackground instance;

    private boolean initialized;
    private int program;
    private int uTimeLocation;
    private int uResolutionLocation;
    private long startTime;

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
                    "    vec3 purple = vec3(0.42, 0.20, 0.62);\n" +
                    "    float alpha = n * 0.8;\n" +
                    "    gl_FragColor = vec4(purple, alpha);\n" +
                    "}\n";

    private MenuShaderBackground() {
    }

    public static MenuShaderBackground get() {
        if (instance == null) {
            instance = new MenuShaderBackground();
        }
        return instance;
    }

    private void init() {
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, VERTEX_SOURCE);
        GL20.glCompileShader(vertexShader);

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, FRAGMENT_SOURCE);
        GL20.glCompileShader(fragmentShader);

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);

        uTimeLocation = GL20.glGetUniformLocation(program, "uTime");
        uResolutionLocation = GL20.glGetUniformLocation(program, "uResolution");

        startTime = System.currentTimeMillis();
        initialized = true;
    }

    public void render(float width, float height) {
        if (!initialized) {
            init();
        }

        float elapsed = (System.currentTimeMillis() - startTime) / 1000f;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        GL20.glUseProgram(program);
        GL20.glUniform1f(uTimeLocation, elapsed);
        GL20.glUniform2f(uResolutionLocation, width, height);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0, 0);
        GL11.glVertex2f(width, 0);
        GL11.glVertex2f(width, height);
        GL11.glVertex2f(0, height);
        GL11.glEnd();

        GL20.glUseProgram(0);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }
}