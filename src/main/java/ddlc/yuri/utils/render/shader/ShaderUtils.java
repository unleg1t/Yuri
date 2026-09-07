package ddlc.yuri.utils.render.shader;

import ddlc.yuri.utils.client.FileUtils;
import ddlc.yuri.utils.misc.IMinecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;

import static ddlc.yuri.utils.misc.IMinecraft.mc;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;

import java.io.*;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public class ShaderUtils {
    public final int programID;

    public ShaderUtils(String fragmentShaderLoc, String vertexShaderLoc) {
        int program = glCreateProgram();
        try {
            InputStream fragmentStream;
            switch (fragmentShaderLoc) {
                case "shadow":
                    fragmentStream = new ByteArrayInputStream(shadow.getBytes());
                    break;
                case "gradient":
                    fragmentStream = new ByteArrayInputStream(gradient.getBytes());
                    break;
                case "mainmenu":
                    fragmentStream = new ByteArrayInputStream(mainmenu.getBytes());
                    break;
                case "kawaseUp":
                    fragmentStream = new ByteArrayInputStream(kawaseUp.getBytes());
                    break;
                case "kawaseDown":
                    fragmentStream = new ByteArrayInputStream(kawaseDown.getBytes());
                    break;
                case "kawaseUpBloom":
                    fragmentStream = new ByteArrayInputStream(kawaseUpBloom.getBytes());
                    break;
                case "kawaseDownBloom":
                    fragmentStream = new ByteArrayInputStream(kawaseDownBloom.getBytes());
                    break;
                case "gaussianBlur":
                    fragmentStream = new ByteArrayInputStream(gaussianBlur.getBytes());
                    break;
                case "cape":
                    fragmentStream = new ByteArrayInputStream(cape.getBytes());
                    break;
                case "outline":
                    fragmentStream = new ByteArrayInputStream(outline.getBytes());
                    break;
                case "glow":
                    fragmentStream = new ByteArrayInputStream(glow.getBytes());
                    break;
                case "colorBloom":
                    fragmentStream = new ByteArrayInputStream(colorBloom.getBytes());
                    break;
                case "nebula":
                    fragmentStream = new ByteArrayInputStream(nebula.getBytes());
                    break;
                case "yuri":
                    fragmentStream = new ByteArrayInputStream(yuri.getBytes());
                    break;
                default:
                    fragmentStream = mc.getResourceManager().getResource(new ResourceLocation(fragmentShaderLoc)).getInputStream();
                    break;
            }
            int fragmentShaderID = createShader(fragmentStream, GL_FRAGMENT_SHADER);
            glAttachShader(program, fragmentShaderID);

            int vertexShaderID = createShader(mc.getResourceManager().getResource(new ResourceLocation(vertexShaderLoc)).getInputStream(), GL_VERTEX_SHADER);
            glAttachShader(program, vertexShaderID);
        } catch (IOException e) {
            e.printStackTrace();
        }

        glLinkProgram(program);
        int status = glGetProgrami(program, GL_LINK_STATUS);

        if (status == 0) {
            throw new IllegalStateException("Shader failed to link!");
        }
        this.programID = program;
    }

    public ShaderUtils(String fragmentShaderLoc) {
        this(fragmentShaderLoc, "yuri/shaders/vertex.vsh");
    }

    public void init() {
        glUseProgram(programID);
    }

    public void unload() {
        glUseProgram(0);
    }

    public int getUniform(String name) {
        return glGetUniformLocation(programID, name);
    }

    public void setUniform1fArray(String name, FloatBuffer buffer) {
        buffer.rewind();
        int count = buffer.limit();
        for (int i = 0; i < count; i++) {
            glUniform1f(glGetUniformLocation(programID, name + "[" + i + "]"), buffer.get());
        }
    }

    public void setUniformf(String name, float... args) {
        int loc = glGetUniformLocation(programID, name);
        switch (args.length) {
            case 1:
                glUniform1f(loc, args[0]);
                break;
            case 2:
                glUniform2f(loc, args[0], args[1]);
                break;
            case 3:
                glUniform3f(loc, args[0], args[1], args[2]);
                break;
            case 4:
                glUniform4f(loc, args[0], args[1], args[2], args[3]);
                break;
        }
    }

    public void setUniformi(String name, int... args) {
        int loc = glGetUniformLocation(programID, name);
        if (args.length > 1) glUniform2i(loc, args[0], args[1]);
        else glUniform1i(loc, args[0]);
    }

    public static void drawQuads(float x, float y, float width, float height) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + height);
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y + height);
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y);
        glEnd();
    }

    public static void drawQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        float width = (float) sr.getScaledWidth_double();
        float height = (float) sr.getScaledHeight_double();
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glEnd();
    }

    public static void drawQuads(float width, float height) {
        drawQuads(0.0f, 0.0f, width, height);
    }

    public static void drawFixedQuads() {
        ScaledResolution sr = new ScaledResolution(mc);
        drawQuads((float) (mc.displayWidth / sr.getScaleFactor()), (float) mc.displayHeight / sr.getScaleFactor());
    }

    private int createShader(InputStream inputStream, int shaderType) {
        int shader = glCreateShader(shaderType);
        glShaderSource(shader, readInputStream(inputStream));
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            System.out.println(glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile!", shaderType));
        }

        return shader;
    }

    public static String readInputStream(InputStream inputStream) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line).append('\n');
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }

    private final String shadow =
            "#version 120\n" +
                    "\n" +
                    "uniform sampler2D inTexture;\n" +
                    "uniform sampler2D textureToCheck;\n" +
                    "uniform vec2 texelSize;\n" +
                    "uniform vec2 direction;\n" +
                    "uniform float radius;\n" +
                    "uniform float strength;\n" +
                    "uniform float weights[256];\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = gl_TexCoord[0].st;\n" +
                    "\n" +
                    "    if (direction.y > 0.0 && texture2D(textureToCheck, uv).a != 0.0) {\n" +
                    "        discard;\n" +
                    "    }\n" +
                    "\n" +
                    "    float alpha = texture2D(inTexture, uv).a * weights[0];\n" +
                    "    float weightSum = weights[0];\n" +
                    "\n" +
                    "    for (int i = 1; i <= int(radius); ++i) {\n" +
                    "        vec2 offset = texelSize * direction * float(i);\n" +
                    "        float w = weights[i];\n" +
                    "\n" +
                    "        alpha += texture2D(inTexture, clamp(uv + offset, vec2(0.0), vec2(1.0))).a * w;\n" +
                    "        alpha += texture2D(inTexture, clamp(uv - offset, vec2(0.0), vec2(1.0))).a * w;\n" +
                    "        weightSum += 2.0 * w;\n" +
                    "    }\n" +
                    "\n" +
                    "    alpha = (alpha / weightSum) * strength;\n" +
                    "    gl_FragColor = vec4(0.0, 0.0, 0.0, alpha);\n" +
                    "}\n";

    private String kawaseUpBloom = "#version 120\n" +
            "\n" +
            "uniform sampler2D inTexture, textureToCheck;\n" +
            "uniform vec2 halfpixel, offset, iResolution;\n" +
            "uniform int check;\n" +
            "uniform float strength;\n" +
            "uniform vec3 color;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = vec2(gl_FragCoord.xy / iResolution);\n" +
            "\n" +
            "    vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);\n" +
            "    sum.rgb *= sum.a;\n" +
            "    vec4 smpl1 = texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset);\n" +
            "    smpl1.rgb *= smpl1.a;\n" +
            "    sum += smpl1 * 2.0;\n" +
            "    vec4 smp2 = texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);\n" +
            "    smp2.rgb *= smp2.a;\n" +
            "    sum += smp2;\n" +
            "    vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset);\n" +
            "    smp3.rgb *= smp3.a;\n" +
            "    sum += smp3 * 2.0;\n" +
            "    vec4 smp4 = texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);\n" +
            "    smp4.rgb *= smp4.a;\n" +
            "    sum += smp4;\n" +
            "    vec4 smp5 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp5.rgb *= smp5.a;\n" +
            "    sum += smp5 * 2.0;\n" +
            "    vec4 smp6 = texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);\n" +
            "    smp6.rgb *= smp6.a;\n" +
            "    sum += smp6;\n" +
            "    vec4 smp7 = texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp7.rgb *= smp7.a;\n" +
            "    sum += smp7 * 2.0;\n" +
            "    vec4 result = sum / 12.0;\n" +
            "\n" +
            "    float alpha = result.a;\n" +
            "    if (check == 1) {\n" +
            "        alpha *= (1.0 - texture2D(textureToCheck, gl_TexCoord[0].st).a);\n" +
            "    }\n" +
            "\n" +
            "    gl_FragColor = vec4(color * strength, alpha);\n" +
            "}";

    private String kawaseDownBloom = "#version 120\n" +
            "\n" +
            "uniform sampler2D inTexture;\n" +
            "uniform vec2 offset, halfpixel, iResolution;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = vec2(gl_FragCoord.xy / iResolution);\n" +
            "    vec4 sum = texture2D(inTexture, gl_TexCoord[0].st);\n" +
            "    sum.rgb *= sum.a;\n" +
            "    sum *= 4.0;\n" +
            "    vec4 smp1 = texture2D(inTexture, uv - halfpixel.xy * offset);\n" +
            "    smp1.rgb *= smp1.a;\n" +
            "    sum += smp1;\n" +
            "    vec4 smp2 = texture2D(inTexture, uv + halfpixel.xy * offset);\n" +
            "    smp2.rgb *= smp2.a;\n" +
            "    sum += smp2;\n" +
            "    vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp3.rgb *= smp3.a;\n" +
            "    sum += smp3;\n" +
            "    vec4 smp4 = texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp4.rgb *= smp4.a;\n" +
            "    sum += smp4;\n" +
            "    vec4 result = sum / 8.0;\n" +
            "    gl_FragColor = vec4(result.rgb / result.a, result.a);\n" +
            "}";

    private final String kawaseUp =
            "#version 120\n" +
                    "\n" +
                    "uniform sampler2D inTexture, textureToCheck;\n" +
                    "uniform vec2 halfpixel, offset, iResolution;\n" +
                    "uniform int check;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = vec2(gl_FragCoord.xy / iResolution);\n" +
                    "    vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);\n" +
                    "    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;\n" +
                    "    sum += texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);\n" +
                    "    sum += texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;\n" +
                    "    sum += texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);\n" +
                    "    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;\n" +
                    "    sum += texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);\n" +
                    "    sum += texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;\n" +
                    "\n" +
                    "    gl_FragColor = vec4(sum.rgb / 12.0, mix(1.0, texture2D(textureToCheck, gl_TexCoord[0].st).a, check));\n" +
                    "}\n";

    private final String kawaseDown =
            "#version 120\n" +
                    "\n" +
                    "uniform sampler2D inTexture;\n" +
                    "uniform vec2 offset, halfpixel, iResolution;\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = vec2(gl_FragCoord.xy / iResolution);\n" +
                    "    vec4 sum = texture2D(inTexture, gl_TexCoord[0].st) * 4.0;\n" +
                    "    sum += texture2D(inTexture, uv - halfpixel.xy * offset);\n" +
                    "    sum += texture2D(inTexture, uv + halfpixel.xy * offset);\n" +
                    "    sum += texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
                    "    sum += texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
                    "    gl_FragColor = vec4(sum.rgb * .125, 1.0);\n" +
                    "}\n";

    private final String gradient =
            "#version 120\n" +
                    "\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform sampler2D tex;\n" +
                    "uniform vec4 color1, color2, color3, color4;\n" +
                    "\n" +
                    "#define NOISE .5/255.0\n" +
                    "\n" +
                    "vec3 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4){\n" +
                    "    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);\n" +
                    "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898,78.233))) * 43758.5453));\n" +
                    "    return color;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 coords = (gl_FragCoord.xy - location) / rectSize;\n" +
                    "    float texColorAlpha = texture2D(tex, gl_TexCoord[0].st).a;\n" +
                    "    gl_FragColor = vec4(createGradient(coords, color1, color2, color3, color4).rgb, texColorAlpha);\n" +
                    "}";

    private final String mainmenu =
            "uniform float TIME;\n" +
                    "uniform vec2 RESOLUTION;\n" +
                    "\n" +
                    "#define NUM_OCTAVES 6\n" +
                    "\n" +
                    "mat3 rotX(float a) {\n" +
                    "    float c = cos(a);\n" +
                    "    float s = sin(a);\n" +
                    "    return mat3(\n" +
                    "        1, 0, 0,\n" +
                    "        0, c, -s,\n" +
                    "        0, s, c\n" +
                    "    );\n" +
                    "}\n" +
                    "\n" +
                    "mat3 rotY(float a) {\n" +
                    "    float c = cos(a);\n" +
                    "    float s = sin(a);\n" +
                    "    return mat3(\n" +
                    "        c, 0, -s,\n" +
                    "        0, 1, 0,\n" +
                    "        s, 0, c\n" +
                    "    );\n" +
                    "}\n" +
                    "\n" +
                    "float random(vec2 pos) {\n" +
                    "    return fract(sin(dot(pos.xy, vec2(12.9898, 78.233))) * 43758.5453123);\n" +
                    "}\n" +
                    "\n" +
                    "float noise(vec2 pos) {\n" +
                    "    vec2 i = floor(pos);\n" +
                    "    vec2 f = fract(pos);\n" +
                    "    float a = random(i + vec2(0.0, 0.0));\n" +
                    "    float b = random(i + vec2(1.0, 0.0));\n" +
                    "    float c = random(i + vec2(0.0, 1.0));\n" +
                    "    float d = random(i + vec2(1.0, 1.0));\n" +
                    "    vec2 u = f * f * (3.0 - 2.0 * f);\n" +
                    "    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;\n" +
                    "}\n" +
                    "\n" +
                    "float fbm(vec2 pos) {\n" +
                    "    float v = 0.0;\n" +
                    "    float a = 0.5;\n" +
                    "    vec2 shift = vec2(100.0);\n" +
                    "    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));\n" +
                    "    for (int i = 0; i < NUM_OCTAVES; i++) {\n" +
                    "        float dir = mod(float(i), 2.0) > 0.5 ? 1.0 : -1.0;\n" +
                    "        v += a * noise(pos - 0.05 * dir * TIME);\n" +
                    "        pos = rot * pos * 2.0 + shift;\n" +
                    "        a *= 0.5;\n" +
                    "    }\n" +
                    "    return v;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 render(in vec2 fragCoord) {\n" +
                    "    vec2 p = (fragCoord * 2.0 - RESOLUTION.xy) / min(RESOLUTION.x, RESOLUTION.y);\n" +
                    "    p -= vec2(12.0, 0.0);\n" +
                    "\n" +
                    "    float time2 = 1.0;\n" +
                    "    vec2 q = vec2(0.0);\n" +
                    "    q.x = fbm(p + 0.00 * time2);\n" +
                    "    q.y = fbm(p + vec2(1.0));\n" +
                    "    vec2 r = vec2(0.0);\n" +
                    "    r.x = fbm(p + 1.0 * q + vec2(1.7, 9.2) + 0.15 * time2);\n" +
                    "    r.y = fbm(p + 1.0 * q + vec2(8.3, 2.8) + 0.126 * time2);\n" +
                    "    float f = fbm(p + r);\n" +
                    "\n" +
                    "    vec3 color = mix(\n" +
                    "        vec3(0.3, 0.3, 0.6),\n" +
                    "        vec3(0.7, 0.7, 0.7),\n" +
                    "        clamp((f * f) * 4.0, 0.0, 1.0)\n" +
                    "    );\n" +
                    "\n" +
                    "    color = mix(\n" +
                    "        color,\n" +
                    "        vec3(0.7, 0.7, 0.7),\n" +
                    "        clamp(length(q), 0.0, 1.0)\n" +
                    "    );\n" +
                    "\n" +
                    "    color = mix(\n" +
                    "        color,\n" +
                    "        vec3(0.4, 0.4, 0.4),\n" +
                    "        clamp(length(r.x), 0.0, 1.0)\n" +
                    "    );\n" +
                    "\n" +
                    "    color = (f * f * f + 0.9 * f * f + 0.8 * f) * color;\n" +
                    "\n" +
                    "    return color * 0.5;\n" +
                    "}\n" +
                    "\n" +
                    "void mainImage(out vec4 fragColor, in vec2 fragCoord) {\n" +
                    "    vec3 color = render(fragCoord);\n" +
                    "    fragColor = vec4(color, color.r);\n" +
                    "}\n" +
                    "\n" +
                    "void main(void) {\n" +
                    "    mainImage(gl_FragColor, gl_FragCoord.xy);\n" +
                    "}\n";

    private final String gaussianBlur =
            "#version 120\n" +
                    "\n" +
                    "uniform sampler2D textureIn;\n" +
                    "uniform vec2 texelSize;\n" +
                    "uniform vec2 direction;\n" +
                    "uniform float strength;\n" +
                    "uniform float radius;\n" +
                    "uniform float weights[128];\n" +
                    "\n" +
                    "#define offset (texelSize * direction)\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = gl_TexCoord[0].st;\n" +
                    "    vec3 color = texture2D(textureIn, uv).rgb * weights[0];\n" +
                    "\n" +
                    "    for (int i = 1; i < 128; ++i) {\n" +
                    "        if (i > int(radius)) break;\n" +
                    "\n" +
                    "        vec2 delta = float(i) * offset;\n" +
                    "        color += texture2D(textureIn, uv + delta).rgb * weights[i];\n" +
                    "        color += texture2D(textureIn, uv - delta).rgb * weights[i];\n" +
                    "    }\n" +
                    "\n" +
                    "    gl_FragColor = vec4(color * strength, 1.0);\n" +
                    "}\n";

    private final String cape =
            "#extension GL_OES_standard_derivatives : enable\n" +
                    "\n" +
                    "#ifdef GL_ES\n" +
                    "precision highp float;\n" +
                    "#endif\n" +
                    "\n" +
                    "uniform float time;\n" +
                    "uniform vec2  resolution;\n" +
                    "uniform float zoom;\n" +
                    "\n" +
                    "#define PI 3.1415926535\n" +
                    "\n" +
                    "mat2 rotate3d(float angle)\n" +
                    "{\n" +
                    "    return mat2(cos(angle), -sin(angle), sin(angle), cos(angle));\n" +
                    "}\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "    vec2 p = (gl_FragCoord.xy * 2.0 - resolution) / min(resolution.x, resolution.y);\n" +
                    "    p = rotate3d((time * 2.0) * PI) * p;\n" +
                    "    float t;\n" +
                    "    if (sin(time) == 10.0)\n" +
                    "        t = 0.075 / abs(1.0 - length(p));\n" +
                    "    else\n" +
                    "        t = 0.075 / abs(0.4 - length(p));\n" +
                    "    gl_FragColor = vec4((1. - exp(-vec3(t) * vec3(0.13*(sin(time)+12.0), p.y*0.7, 3.0))), 1.0);\n" +
                    "}";

    private final String colorBloom =
            "#version 120\n" +
                    "\n" +
                    "uniform sampler2D inTexture;\n" +
                    "uniform vec2 texelSize;\n" +
                    "uniform vec2 direction;\n" +
                    "uniform float radius;\n" +
                    "uniform float strength;\n" +
                    "uniform float weights[256];\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = gl_TexCoord[0].st;\n" +
                    "\n" +
                    "    vec4 center = texture2D(inTexture, uv);\n" +
                    "    vec4 sum = vec4(center.rgb * center.a, center.a) * weights[0];\n" +
                    "    float weightSum = weights[0];\n" +
                    "\n" +
                    "    for (int i = 1; i <= int(radius); ++i) {\n" +
                    "        vec2 offset = texelSize * direction * float(i);\n" +
                    "        float w = weights[i];\n" +
                    "\n" +
                    "        vec4 sampleA = texture2D(inTexture, clamp(uv + offset, vec2(0.0), vec2(1.0)));\n" +
                    "        vec4 sampleB = texture2D(inTexture, clamp(uv - offset, vec2(0.0), vec2(1.0)));\n" +
                    "\n" +
                    "        sum += vec4(sampleA.rgb * sampleA.a, sampleA.a) * w;\n" +
                    "        sum += vec4(sampleB.rgb * sampleB.a, sampleB.a) * w;\n" +
                    "        weightSum += 2.0 * w;\n" +
                    "    }\n" +
                    "\n" +
                    "    sum /= weightSum;\n" +
                    "    gl_FragColor = vec4(sum.rgb * strength, sum.a);\n" +
                    "}\n";

    private final String glow =
            "#version 120\n" +
                    "\n" +
                    "uniform sampler2D textureIn, textureToCheck;\n" +
                    "uniform vec2 texelSize, direction;\n" +
                    "uniform vec3 color;\n" +
                    "uniform bool avoidTexture;\n" +
                    "uniform float exposure, radius;\n" +
                    "uniform float weights[256];\n" +
                    "\n" +
                    "#define offset direction * texelSize\n" +
                    "\n" +
                    "void main() {\n" +
                    "    if (direction.y == 1 && avoidTexture) {\n" +
                    "        if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;\n" +
                    "    }\n" +
                    "\n" +
                    "    float innerAlpha = texture2D(textureIn, gl_TexCoord[0].st).a * weights[0];\n" +
                    "\n" +
                    "    for (float r = 1.0; r <= radius; r++) {\n" +
                    "        innerAlpha += texture2D(textureIn, gl_TexCoord[0].st + offset * r).a * weights[int(r)];\n" +
                    "        innerAlpha += texture2D(textureIn, gl_TexCoord[0].st - offset * r).a * weights[int(r)];\n" +
                    "    }\n" +
                    "\n" +
                    "    gl_FragColor = vec4(color, mix(innerAlpha, 1.0 - exp(-innerAlpha * exposure), step(0.0, direction.y)));\n" +
                    "}\n";

    private final String outline =
            "#version 120\n" +
                    "\n" +
                    "uniform vec2 texelSize, direction;\n" +
                    "uniform sampler2D texture;\n" +
                    "uniform float radius;\n" +
                    "uniform vec3 color;\n" +
                    "\n" +
                    "#define offset direction * texelSize\n" +
                    "\n" +
                    "void main() {\n" +
                    "    float centerAlpha = texture2D(texture, gl_TexCoord[0].xy).a;\n" +
                    "    float innerAlpha = centerAlpha;\n" +
                    "    for (float r = 1.0; r <= radius; r++) {\n" +
                    "        float alphaCurrent1 = texture2D(texture, gl_TexCoord[0].xy + offset * r).a;\n" +
                    "        float alphaCurrent2 = texture2D(texture, gl_TexCoord[0].xy - offset * r).a;\n" +
                    "        innerAlpha += alphaCurrent1 + alphaCurrent2;\n" +
                    "    }\n" +
                    "\n" +
                    "    gl_FragColor = vec4(color, innerAlpha) * step(0.0, -centerAlpha);\n" +
                    "}\n";

    private final String nebula =
            "#extension GL_OES_standard_derivatives : enable\n" +
                    "\n" +
                    "#ifdef GL_ES\n" +
                    "precision highp float;\n" +
                    "#endif\n" +
                    "\n" +
                    "#define NUM_OCTAVES 6\n" +
                    "\n" +
                    "uniform float time;\n" +
                    "\n" +
                    "mat3 rotX(float a) {\n" +
                    "    float c = cos(a);\n" +
                    "    float s = sin(a);\n" +
                    "    return mat3(\n" +
                    "        1, 0, 0,\n" +
                    "        0, c, -s,\n" +
                    "        0, s, c\n" +
                    "    );\n" +
                    "}\n" +
                    "\n" +
                    "mat3 rotY(float a) {\n" +
                    "    float c = cos(a);\n" +
                    "    float s = sin(a);\n" +
                    "    return mat3(\n" +
                    "        c, 0, -s,\n" +
                    "        0, 1, 0,\n" +
                    "        s, 0, c\n" +
                    "    );\n" +
                    "}\n" +
                    "\n" +
                    "float random(vec2 pos) {\n" +
                    "    return fract(sin(dot(pos.xy, vec2(13.9898, 78.233))) * 43758.5453123);\n" +
                    "}\n" +
                    "\n" +
                    "float noise(vec2 pos) {\n" +
                    "    vec2 i = floor(pos);\n" +
                    "    vec2 f = fract(pos);\n" +
                    "    float a = random(i + vec2(0.0, 0.0));\n" +
                    "    float b = random(i + vec2(1.0, 0.0));\n" +
                    "    float c = random(i + vec2(0.0, 1.0));\n" +
                    "    float d = random(i + vec2(1.0, 1.0));\n" +
                    "    vec2 u = f * f * (3.0 - 2.0 * f);\n" +
                    "    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;\n" +
                    "}\n" +
                    "\n" +
                    "float fbm(vec2 pos) {\n" +
                    "    float v = 0.0;\n" +
                    "    float a = 0.5;\n" +
                    "    vec2 shift = vec2(100.0);\n" +
                    "    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));\n" +
                    "    for (int i = 0; i < NUM_OCTAVES; i++) {\n" +
                    "        float dir = mod(float(i), 2.0) > 0.5 ? 1.0 : -1.0;\n" +
                    "        v += a * noise(pos - 0.05 * dir * time);\n" +
                    "        pos = rot * pos * 2.0 + shift;\n" +
                    "        a *= 0.5;\n" +
                    "    }\n" +
                    "    return v;\n" +
                    "}\n" +
                    "\n" +
                    "void main(void) {\n" +
                    "    vec3 dir = normalize(gl_TexCoord[0].xyz);\n" +
                    "    vec2 p = dir.xy / (1.0 + abs(dir.z));\n" +
                    "    p -= vec2(12.0, 0.0);\n" +
                    "\n" +
                    "    float t = 0.0, d;\n" +
                    "\n" +
                    "    float time2 = 1.0;\n" +
                    "\n" +
                    "    vec2 q = vec2(0.0);\n" +
                    "    q.x = fbm(p + 0.00 * time2);\n" +
                    "    q.y = fbm(p + vec2(1.0));\n" +
                    "    vec2 r = vec2(0.0);\n" +
                    "    r.x = fbm(p + 1.0 * q + vec2(1.7, 1.2) + 0.15 * time2);\n" +
                    "    r.y = fbm(p + 1.0 * q + vec2(8.3, 2.8) + 0.126 * time2);\n" +
                    "    float f = fbm(p + r);\n" +
                    "\n" +
                    "    vec3 color = mix(\n" +
                    "        vec3(1.0, 1.0, 2.0),\n" +
                    "        vec3(1.0, 1.0, 1.0),\n" +
                    "        clamp((f * f) * 5.5, 1.2, 15.5)\n" +
                    "    );\n" +
                    "\n" +
                    "    color = mix(\n" +
                    "        color,\n" +
                    "        vec3(1.0, 1.0, 1.0),\n" +
                    "        clamp(length(q), 2.0, 2.0)\n" +
                    "    );\n" +
                    "\n" +
                    "    color = mix(\n" +
                    "        color,\n" +
                    "        vec3(0.3, 0.2, 1.0),\n" +
                    "        clamp(length(r.x), 0.0, 5.0)\n" +
                    "    );\n" +
                    "\n" +
                    "    color = (f * f * f * 1.0 + 0.5 * 1.7 * 0.0 + 0.9 * f) * color;\n" +
                    "\n" +
                    "    gl_FragColor = vec4(clamp(color, 0.0, 1.0), 1.0);\n" +
                    "}\n";

    private final String yuri =
            "#version 120\n" +
                    "\n" +
                    "uniform float time;\n" +
                    "\n" +
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
                    "    vec3 dir = normalize(gl_TexCoord[0].xyz);\n" +
                    "    vec2 p = dir.xy / (1.0 + abs(dir.z));\n" +
                    "    vec2 flow = p * 1.6 + vec2(time * 0.02, time * 0.015);\n" +
                    "    float n = fbm(flow);\n" +
                    "    vec3 purple = vec3(0.42, 0.20, 0.62);\n" +
                    "    float brightness = 0.35 + 0.65 * n;\n" +
                    "    gl_FragColor = vec4(purple * brightness, 1.0);\n" +
                    "}\n";
}