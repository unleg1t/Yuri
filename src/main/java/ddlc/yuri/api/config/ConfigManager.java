package ddlc.yuri.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ddlc.yuri.Yuri;
import ddlc.yuri.utils.misc.Manager;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public final class ConfigManager extends Manager<Config> {

    @Getter
    private static ConfigManager instance;

    @Getter
    private final BindsConfig bindsConfig;

    @Getter
    private final VisualsConfig visualsConfig;

    public ConfigManager() {
        super(loadConfigs());
        instance = this;

        if (!CONFIGS_DIR.exists()) {
            boolean ignored = CONFIGS_DIR.mkdirs();
        }

        this.bindsConfig = new BindsConfig();
        this.visualsConfig = new VisualsConfig();
    }

    public static final File CONFIGS_DIR = new File(Yuri.NAME, "configs");
    public static final String EXTENSION = ".json";

    public boolean loadConfig(String configName) {
        if (configName == null) return false;
        Config config = findConfig(configName);

        if (config == null) return false;
        try (FileReader reader = new FileReader(config.getFile())) {
            JsonParser parser = new JsonParser();
            JsonObject object = (JsonObject) parser.parse(reader);
            config.load(object);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean saveConfig(String configName) {
        if (configName == null) return false;
        Config config;
        if ((config = findConfig(configName)) == null) {
            Config newConfig = (config = new Config(configName));
            getElements().add(newConfig);
        }

        String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(config.save());
        try (FileWriter writer = new FileWriter(config.getFile())) {
            writer.write(contentPrettyPrint);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public Config findConfig(String configName) {
        if (configName == null) return null;
        for (Config config : getElements()) {
            if (config.getName().equalsIgnoreCase(configName))
                return config;
        }

        if (new File(CONFIGS_DIR, configName + EXTENSION).exists())
            return new Config(configName);

        return null;
    }

    public boolean deleteConfig(String configName) {
        if (configName == null) return false;
        Config config;
        if ((config = findConfig(configName)) != null) {
            final File f = config.getFile();
            getElements().remove(config);
            return f.exists() && f.delete();
        }
        return false;
    }

    private static ArrayList<Config> loadConfigs() {
        final ArrayList<Config> loadedConfigs = new ArrayList<>();
        File[] files = CONFIGS_DIR.listFiles();
        if (files != null) {
            for (File file : files) {
                if (FilenameUtils.getExtension(file.getName()).equals("json"))
                    loadedConfigs.add(new Config(FilenameUtils.removeExtension(file.getName())));
            }
        }
        return loadedConfigs;
    }
}