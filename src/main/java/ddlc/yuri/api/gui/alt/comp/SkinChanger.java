package ddlc.yuri.api.gui.alt.comp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class SkinChanger {

    public static final String VARIANT_CLASSIC = "classic";
    public static final String VARIANT_SLIM = "slim";

    private static final String PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    private static final String SKINS_URL = PROFILE_URL + "/skins";
    private static final String ACTIVE_SKIN_URL = SKINS_URL + "/active";
    private static final int TIMEOUT = 20000;

    private SkinChanger() {
    }

    public static boolean isPremiumToken(String accessToken) {
        return accessToken != null && accessToken.length() > 20 && !"0".equals(accessToken);
    }

    public static BufferedImage readSkin(File file) throws SkinException {
        if (file == null || !file.isFile()) throw new SkinException("Skin file not found");
        if (file.length() > 24 * 1024) throw new SkinException("Skin file is too large (max 24 KB)");
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            throw new SkinException("Could not read skin: " + e.getMessage());
        }
        if (image == null) throw new SkinException("Skin must be a PNG image");
        boolean modern = image.getWidth() == 64 && image.getHeight() == 64;
        boolean legacy = image.getWidth() == 64 && image.getHeight() == 32;
        if (!modern && !legacy) throw new SkinException("Skin must be 64x64 or 64x32, got " + image.getWidth() + "x" + image.getHeight());
        return image;
    }

    public static Profile upload(String accessToken, File file, String variant) throws SkinException {
        if (!isPremiumToken(accessToken)) throw new SkinException("Log in with a Microsoft account first");
        readSkin(file);

        byte[] png;
        try {
            png = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new SkinException("Could not read skin: " + e.getMessage());
        }

        String boundary = "----YuriSkin" + Long.toHexString(System.nanoTime());
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        try {
            writeField(body, boundary, "variant", VARIANT_SLIM.equals(variant) ? VARIANT_SLIM : VARIANT_CLASSIC);
            body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName(file.getName()) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            body.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            body.write(png);
            body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SkinException("Could not build upload: " + e.getMessage());
        }

        JsonObject json = request("POST", SKINS_URL, accessToken, "multipart/form-data; boundary=" + boundary, body.toByteArray());
        return Profile.from(json);
    }

    public static void reset(String accessToken) throws SkinException {
        if (!isPremiumToken(accessToken)) throw new SkinException("Log in with a Microsoft account first");
        request("DELETE", ACTIVE_SKIN_URL, accessToken, null, null);
    }

    public static Profile fetchProfile(String accessToken) throws SkinException {
        if (!isPremiumToken(accessToken)) throw new SkinException("Log in with a Microsoft account first");
        return Profile.from(request("GET", PROFILE_URL, accessToken, null, null));
    }

    private static void writeField(OutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String safeName(String name) {
        String cleaned = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return cleaned.toLowerCase().endsWith(".png") ? cleaned : cleaned + ".png";
    }

    private static JsonObject request(String method, String url, String accessToken, String contentType, byte[] body) throws SkinException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Accept", "application/json");
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", contentType);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body);
                }
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String text = stream == null ? "" : readAll(stream);

            JsonObject json = new JsonObject();
            try {
                JsonElement parsed = new JsonParser().parse(text);
                if (parsed.isJsonObject()) json = parsed.getAsJsonObject();
            } catch (RuntimeException ignored) {
            }

            if (status == 401) throw new SkinException("Session expired, log in again");
            if (status == 429) throw new SkinException("Mojang rate limit, try again in a minute");
            if (status < 200 || status >= 300) {
                String message = json.has("errorMessage") ? json.get("errorMessage").getAsString()
                        : json.has("error") ? json.get("error").getAsString() : "HTTP " + status;
                throw new SkinException("Mojang rejected the request: " + message);
            }
            return json;
        } catch (IOException e) {
            throw new SkinException("Could not reach Mojang: " + e.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        try (InputStream in = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    public static final class Profile {
        public final String name, uuid, skinUrl, variant;

        private Profile(String name, String uuid, String skinUrl, String variant) {
            this.name = name;
            this.uuid = uuid;
            this.skinUrl = skinUrl;
            this.variant = variant;
        }

        static Profile from(JsonObject json) {
            String skinUrl = "";
            String variant = VARIANT_CLASSIC;
            JsonElement skins = json.get("skins");
            if (skins != null && skins.isJsonArray()) {
                JsonArray array = skins.getAsJsonArray();
                for (JsonElement element : array) {
                    if (!element.isJsonObject()) continue;
                    JsonObject skin = element.getAsJsonObject();
                    boolean active = skin.has("state") && "ACTIVE".equalsIgnoreCase(skin.get("state").getAsString());
                    if (!active && !skinUrl.isEmpty()) continue;
                    if (skin.has("url")) skinUrl = skin.get("url").getAsString();
                    if (skin.has("variant")) variant = skin.get("variant").getAsString().toLowerCase();
                    if (active) break;
                }
            }
            return new Profile(
                    json.has("name") ? json.get("name").getAsString() : "",
                    json.has("id") ? json.get("id").getAsString() : "",
                    skinUrl, variant);
        }
    }

    public static class SkinException extends Exception {
        public SkinException(String message) {
            super(message);
        }
    }
}
