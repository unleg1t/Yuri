package ddlc.yuri.api.gui.alt.comp;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ddlc.yuri.utils.client.NetworkUtils;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;


public class MicrosoftOAuthTranslation {

    static ExecutorService executor = Executors.newSingleThreadExecutor();

    public static boolean isRefreshToken(String token) {
        return token != null && token.startsWith("M.C");
    }

    public static class LoginData {
        public String mcToken;
        public String newRefreshToken;
        public String uuid, username;

        public LoginData() {
        }

        public LoginData(String mcToken, String newRefreshToken, String uuid, String username) {
            this.mcToken = mcToken;
            this.newRefreshToken = newRefreshToken;
            this.uuid = uuid;
            this.username = username;
        }

        public boolean isGood() {
            return mcToken != null;
        }
    }

    private static class RedeemResult {
        AuthTokenResponse response;
        String rpsPrefix;
    }

    private static final String CLIENT_ID = "9fbc7315-7200-4b2b-a655-bb38c865da17", CLIENT_SECRET = "Bzn8Q~YryydJsydgnnxHgJq.NM3Oo4.AEEohLbBb";
    private static final String XBOX_CLIENT_ID = "00000000402b5328";
    private static final String XBOX_REDIRECT_URI = "https://login.live.com/oauth20_desktop.srf";
    private static final String XBOX_SCOPE = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final int PORT = 8247;

    private static HttpServer server;
    private static Consumer<String> callback;

    static void browse(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    return;
                }
            }
        } catch (Exception ignored) {
        }

        String[] candidates = new String[]{"xdg-open", "sensible-browser", "gnome-open", "kde-open", "sensible-browser"};
        for (String cmd : candidates) {
            try {
                Process p = new ProcessBuilder(cmd, url).start();
                return;
            } catch (IOException ignored) {
            }
        }

        try {
            Runtime.getRuntime().exec(new String[]{"/usr/bin/xdg-open", url});
            return;
        } catch (IOException ignored) {
        }

        System.err.println("Could not open URL: " + url + " — please open it manually in your browser.");
    }

    public static void getRefreshToken(Consumer<String> callback) {
        MicrosoftOAuthTranslation.callback = callback;

        startServer();
        browse("https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET + "&response_type=code&redirect_uri=http://localhost:" + PORT + "&scope=XboxLive.signin%20offline_access");
    }

    static Gson gson = new Gson();

    private static <T> T safeFromJson(String json, Class<T> type) {
        if (json == null || json.isEmpty()) return null;
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException | IllegalStateException e) {
            return null;
        }
    }

    public static LoginData login(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) return new LoginData();

        RedeemResult redeemed = redeemRefreshToken(refreshToken);
        if (redeemed == null || redeemed.response == null || redeemed.response.access_token == null || redeemed.response.access_token.isEmpty()) return new LoginData();

        String accessToken = redeemed.response.access_token;
        String newRefreshToken = redeemed.response.refresh_token != null ? redeemed.response.refresh_token : refreshToken;

        XblXstsResponse xblRes = safeFromJson(
                NetworkUtils.postExternal("https://user.auth.xboxlive.com/user/authenticate",
                        "{\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"" + redeemed.rpsPrefix + accessToken + "\"},\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\"}", true),
                XblXstsResponse.class);

        if (xblRes == null || xblRes.Token == null || xblRes.Token.isEmpty()) return new LoginData();

        XblXstsResponse xstsRes = safeFromJson(
                NetworkUtils.postExternal("https://xsts.auth.xboxlive.com/xsts/authorize",
                        "{\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\"" + xblRes.Token + "\"]},\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\"}", true),
                XblXstsResponse.class);

        if (xstsRes == null || xstsRes.Token == null || xstsRes.Token.isEmpty()) return new LoginData();
        if (xblRes.DisplayClaims == null || xblRes.DisplayClaims.xui == null || xblRes.DisplayClaims.xui.length == 0
                || xblRes.DisplayClaims.xui[0].uhs == null || xblRes.DisplayClaims.xui[0].uhs.isEmpty()) return new LoginData();

        McResponse mcRes = safeFromJson(
                NetworkUtils.postExternal("https://api.minecraftservices.com/authentication/login_with_xbox",
                        "{\"identityToken\":\"XBL3.0 x=" + xblRes.DisplayClaims.xui[0].uhs + ";" + xstsRes.Token + "\"}", true),
                McResponse.class);

        if (mcRes == null || mcRes.access_token == null || mcRes.access_token.isEmpty()) return new LoginData();

        GameOwnershipResponse gameOwnershipRes = safeFromJson(
                NetworkUtils.getBearerResponse("https://api.minecraftservices.com/entitlements/mcstore", mcRes.access_token),
                GameOwnershipResponse.class);

        if (gameOwnershipRes == null || !gameOwnershipRes.hasGameOwnership()) return new LoginData();

        ProfileResponse profileRes = safeFromJson(
                NetworkUtils.getBearerResponse("https://api.minecraftservices.com/minecraft/profile", mcRes.access_token),
                ProfileResponse.class);

        if (profileRes == null || profileRes.id == null || profileRes.id.isEmpty() || profileRes.name == null || profileRes.name.isEmpty()) return new LoginData();

        return new LoginData(mcRes.access_token, newRefreshToken, profileRes.id, profileRes.name);
    }

    private static RedeemResult redeemRefreshToken(String refreshToken) {
        AuthTokenResponse res = safeFromJson(
                NetworkUtils.postExternal("https://login.live.com/oauth20_token.srf",
                        "client_id=" + XBOX_CLIENT_ID + "&grant_type=refresh_token&redirect_uri=" + XBOX_REDIRECT_URI + "&refresh_token=" + refreshToken + "&scope=" + XBOX_SCOPE, false),
                AuthTokenResponse.class);

        if (res != null && res.access_token != null && !res.access_token.isEmpty()) {
            RedeemResult result = new RedeemResult();
            result.response = res;
            result.rpsPrefix = "t=";
            return result;
        }

        AuthTokenResponse fallback = safeFromJson(
                NetworkUtils.postExternal("https://login.live.com/oauth20_token.srf",
                        "client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET + "&refresh_token=" + refreshToken + "&grant_type=refresh_token&redirect_uri=http://localhost:" + PORT, false),
                AuthTokenResponse.class);

        RedeemResult result = new RedeemResult();
        result.response = fallback;
        result.rpsPrefix = "d=";
        return result;
    }

    private static void startServer() {
        if (server != null) return;

        try {
            server = HttpServer.create(new InetSocketAddress("localhost", PORT), 0);

            server.createContext("/", new Handler());
            server.setExecutor(executor);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void stopServer() {
        if (server == null) return;

        server.stop(0);
        server = null;

        callback = null;
    }

    private static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange req) throws IOException {
            if (req.getRequestMethod().equals("GET")) {
                String query = req.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);

                String code = params.get("code");

                if (code != null) {
                    handleCode(code);
                    writeText(req, "<html>You may now close this page.<script>close()</script></html>");
                } else {
                    writeText(req, "Cannot authenticate.");
                }
            }

            stopServer();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> params = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length == 2) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }
            return params;
        }

        private void handleCode(String code) {
            String response = NetworkUtils.postExternal("https://login.live.com/oauth20_token.srf",
                    "client_id=" + CLIENT_ID + "&code=" + code + "&client_secret=" + CLIENT_SECRET + "&grant_type=authorization_code&redirect_uri=http://localhost:" + PORT, false);
            AuthTokenResponse res = safeFromJson(response, AuthTokenResponse.class);

            if (res == null) callback.accept(null);
            else callback.accept(res.refresh_token);
        }

        private void writeText(HttpExchange req, String text) throws IOException {
            OutputStream out = req.getResponseBody();

            req.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            req.sendResponseHeaders(200, text.length());

            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
            out.close();
        }
    }

    private static class AuthTokenResponse {
        @Expose
        @SerializedName("access_token")
        public String access_token;
        @Expose
        @SerializedName("refresh_token")
        public String refresh_token;
    }

    private static class XblXstsResponse {
        @Expose
        @SerializedName("Token")
        public String Token;
        @Expose
        @SerializedName("DisplayClaims")
        public DisplayClaims DisplayClaims;

        private static class DisplayClaims {
            @Expose
            @SerializedName("xui")
            private Claim[] xui;

            private static class Claim {
                @Expose
                @SerializedName("uhs")
                private String uhs;
            }
        }
    }

    private static class McResponse {
        @Expose
        @SerializedName("access_token")
        public String access_token;
    }

    private static class GameOwnershipResponse {
        @Expose
        @SerializedName("items")
        private Item[] items;

        private static class Item {
            @Expose
            @SerializedName("name")
            private String name;
        }

        private boolean hasGameOwnership() {
            if (items == null) return false;

            boolean hasProduct = false;
            boolean hasGame = false;

            for (Item item : items) {
                if (item == null || item.name == null) continue;

                if (item.name.equals("product_minecraft")) hasProduct = true;
                else if (item.name.equals("game_minecraft")) hasGame = true;
            }

            return hasProduct && hasGame;
        }
    }

    private static class ProfileResponse {
        @Expose
        @SerializedName("id")
        public String id;
        @Expose
        @SerializedName("name")
        public String name;
    }
}