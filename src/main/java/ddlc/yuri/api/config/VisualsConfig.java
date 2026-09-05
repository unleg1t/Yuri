package ddlc.yuri.api.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ddlc.yuri.Yuri;
import ddlc.yuri.modules.Module;
import ddlc.yuri.modules.ModuleCategory;
import ddlc.yuri.utils.render.DragUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public final class VisualsConfig implements Serializable {

    private static final File VISUALS_FILE = new File(Yuri.NAME, "visuals.json");

    public VisualsConfig() {
        if (!VISUALS_FILE.exists()) {
            try {
                VISUALS_FILE.createNewFile();
            } catch (IOException ignored) {
            }
        }
    }

    public boolean saveToFile() {
        JsonObject object = save();
        String contentPrettyPrint = new GsonBuilder().setPrettyPrinting().create().toJson(object);
        try (FileWriter writer = new FileWriter(VISUALS_FILE)) {
            writer.write(contentPrettyPrint);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean loadFromFile() {
        if (!VISUALS_FILE.exists()) return false;
        try (FileReader reader = new FileReader(VISUALS_FILE)) {
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(reader);

            if (element == null || !element.isJsonObject()) {
                return false;
            }

            JsonObject object = element.getAsJsonObject();
            load(object);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public JsonObject save() {
        JsonObject jsonObject = new JsonObject();
        JsonObject modulesObject = new JsonObject();
        JsonObject draggingObject = new JsonObject();

        for (Module module : Yuri.INSTANCE.getModuleManager().getModules()) {
            if (module.getCategory() == ModuleCategory.RENDER) {
                modulesObject.add(module.getLabel(), module.save(false));
            }
        }

        for (String key : DragUtils.components.keySet()) {
            draggingObject.add(key, DragUtils.components.get(key).save());
        }

        jsonObject.add("Modules", modulesObject);
        jsonObject.add("Dragging", draggingObject);

        return jsonObject;
    }

    @Override
    public void load(JsonObject object) {
        if (object.has("Modules")) {
            JsonObject modulesObject = object.getAsJsonObject("Modules");

            for (Module module : Yuri.INSTANCE.getModuleManager().getModules()) {
                if (module.getCategory() == ModuleCategory.RENDER) {
                    if (modulesObject.has(module.getLabel())) {
                        module.load(modulesObject.getAsJsonObject(module.getLabel()));
                    }
                }
            }
        }
        if (object.has("Dragging")) {
            JsonObject draggingObject = object.getAsJsonObject("Dragging");
            for (Map.Entry<String, JsonElement> entry : draggingObject.entrySet()) {
                String key = entry.getKey();
                JsonObject componentData = entry.getValue().getAsJsonObject();
                if (!DragUtils.components.containsKey(key)) {
                    DragUtils.components.put(key, new DragUtils.DraggableComponent(0, 0));
                }
                DragUtils.components.get(key).load(componentData);
            }
        }
    }
}