package ddlc.yuri.api.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import ddlc.yuri.Yuri;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GithubConfigFetcher {
    private static final String REPO_API_URL = "https://api.github.com/repos/unleg1t/yuri-configs/contents";
    private static final String RAW_URL = "https://raw.githubusercontent.com/unleg1t/yuri-configs/main/";

    public static List<String> fetchConfigList() {
        List<String> configs = new ArrayList<>();
        try {
            URL url = new URL(REPO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JsonArray files = new JsonParser().parse(response.toString()).getAsJsonArray();
            for (JsonElement element : files) {
                String name = element.getAsJsonObject().get("name").getAsString();
                if (name.endsWith(".json")) {
                    configs.add(name.replace(".json", ""));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return configs;
    }

    public static boolean downloadAndLoadConfig(String configName) {
        try {
            String configUrl = RAW_URL + configName + ".json";
            URL url = new URL(configUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();

            // Save to configs directory
            java.io.File configFile = new java.io.File(ConfigManager.CONFIGS_DIR, configName + ".json");
            Files.write(Paths.get(configFile.getPath()), content.toString().getBytes());

            // Load the config
            return Yuri.INSTANCE.getConfigManager().loadConfig(configName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}