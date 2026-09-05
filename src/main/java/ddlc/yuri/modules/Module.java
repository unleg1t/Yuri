package ddlc.yuri.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import ddlc.yuri.Yuri;
import ddlc.yuri.api.config.Serializable;
import ddlc.yuri.api.events.EventBus;
import ddlc.yuri.api.events.impl.client.ModuleEvent;
import ddlc.yuri.api.properties.Property;
import ddlc.yuri.api.properties.impl.DescriptorProperty;
import ddlc.yuri.api.properties.impl.ModeProperty;
import ddlc.yuri.api.properties.impl.MultiModeProperty;
import ddlc.yuri.api.properties.impl.NumberProperty;
import ddlc.yuri.utils.misc.IMinecraft;
import ddlc.yuri.utils.misc.Manager;
import ddlc.yuri.utils.misc.Translate;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Module extends Manager<Property<?>> implements Toggleable, Serializable, IMinecraft {

    private final String label = getClass().getAnnotation(ModuleInfo.class).label();
    private final String description = getClass().getAnnotation(ModuleInfo.class).description();
    private final ModuleCategory category = getClass().getAnnotation(ModuleInfo.class).category();
    private int key = getClass().getAnnotation(ModuleInfo.class).key();
    private boolean enabled;
    private boolean hidden;
    @Getter
    @Setter
    private String suffix;
    private final Translate translate = new Translate(0.0, 0.0);

    public void resetPropertyValues() {
        for (Property<?> property : getElements())
            property.callFirstTime();
    }

    public Translate getTranslate() {
        return translate;
    }

    public ModuleCategory getCategory() {
        return category;
    }

    public void reflectProperties() {
        for (final Field field : getClass().getDeclaredFields()) {
            final Class<?> type = field.getType();
            if (type.isAssignableFrom(Property.class) ||
                    type.isAssignableFrom(NumberProperty.class) ||
                    type.isAssignableFrom(ModeProperty.class) ||
                    type.isAssignableFrom(MultiModeProperty.class) ||
                    type.isAssignableFrom(DescriptorProperty.class)) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                try {
                    elements.add((Property<?>) field.get(this));
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getDescription() {
        return description;
    }

    public String getLabel() {
        return label;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        EventBus bus = Yuri.INSTANCE.getEventBus();
        ModuleEvent event = new ModuleEvent(this);
        if (this.enabled != enabled) {
            this.enabled = enabled;

            if (enabled) {
                onEnable();
                Yuri.INSTANCE.getEventBus().subscribe(this);
                bus.post(event);
            } else {
                Yuri.INSTANCE.getEventBus().unsubscribe(this);
                onDisable();
                bus.post(event);
            }
        }
    }

    public boolean isVisible() {
        return enabled && !hidden;
    }

    @Override
    public void toggle() {
        setEnabled(!enabled);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public JsonObject save() {
        return save(true);
    }

    public JsonObject save(boolean saveKey) {
        JsonObject object = new JsonObject();
        object.addProperty("toggled", isEnabled());
        if (saveKey) {
            object.addProperty("key", getKey());
        }
        object.addProperty("hidden", isHidden());
        List<Property<?>> properties = getElements();
        if (!properties.isEmpty()) {
            JsonObject propertiesObject = new JsonObject();

            for (Property<?> property : properties) {
                if (property instanceof NumberProperty) {
                    propertiesObject.addProperty(property.getLabel(), ((NumberProperty) property).getValue());
                } else if (property instanceof ModeProperty) {
                    ModeProperty<?> ModeProperty = (ModeProperty<?>) property;
                    propertiesObject.add(property.getLabel(), new JsonPrimitive(ModeProperty.getValue().name()));
                } else if (property instanceof MultiModeProperty) {
                    MultiModeProperty<?> multiSelect = (MultiModeProperty<?>) property;
                    final JsonArray array = new JsonArray();
                    if (multiSelect.getValue() != null) {
                        for (Enum<?> e : multiSelect.getValue()) {
                            array.add(new JsonPrimitive(e.name()));
                        }
                    }
                    propertiesObject.add(property.getLabel(), array);
                } else if (property.getType() == Boolean.class) {
                    propertiesObject.addProperty(property.getLabel(), (Boolean) property.getValue());
                } else if (property.getType() == Integer.class) {
                    propertiesObject.addProperty(property.getLabel(), Integer.toHexString((Integer) property.getValue()));
                } else if (property.getType() == String.class) {
                    propertiesObject.addProperty(property.getLabel(), (String) property.getValue());
                }
            }

            object.add("Properties", propertiesObject);
        }
        return object;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load(JsonObject object) {
        load(object, false);
    }

    public void load(JsonObject object, boolean loadKey) {
        if (object.has("toggled"))
            setEnabled(object.get("toggled").getAsBoolean());

        if (loadKey && object.has("key"))
            setKey(object.get("key").getAsInt());

        if (object.has("hidden"))
            setHidden(object.get("hidden").getAsBoolean());

        if (object.has("Properties") && !getElements().isEmpty()) {
            JsonObject propertiesObject = object.getAsJsonObject("Properties");
            for (Property<?> property : getElements()) {
                if (propertiesObject.has(property.getLabel())) {
                    if (property instanceof NumberProperty) {
                        ((NumberProperty) property).setValue(propertiesObject.get(property.getLabel()).getAsDouble());
                    } else if (property instanceof ModeProperty) {
                        findEnumValue(property, propertiesObject);
                    } else if (property instanceof MultiModeProperty) {
                        findMultiEnumValues(property, propertiesObject);
                    } else if (property.getValue() instanceof Boolean) {
                        ((Property<Boolean>) property).setValue(propertiesObject.get(property.getLabel()).getAsBoolean());
                    } else if (property.getValue() instanceof Integer) {
                        ((Property<Integer>) property).setValue((int) Long.parseLong(propertiesObject.get(property.getLabel()).getAsString(), 16));
                    } else if (property.getValue() instanceof String) {
                        ((Property<String>) property).setValue(propertiesObject.get(property.getLabel()).getAsString());
                    }
                }
            }
        }
    }

    private static <T extends Enum<T>> void findEnumValue(Property<?> property, JsonObject propertiesObject) {
        ModeProperty<T> ModeProperty = (ModeProperty<T>) property;
        String value = propertiesObject.getAsJsonPrimitive(property.getLabel()).getAsString();
        for (T possibleValue : ModeProperty.getValues()) {
            if (possibleValue.name().equalsIgnoreCase(value)) {
                ModeProperty.setValue(possibleValue);
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Enum<T>> void findMultiEnumValues(Property<?> property, JsonObject propertiesObject) {
        MultiModeProperty<T> multiProperty = (MultiModeProperty<T>) property;
        JsonElement element = propertiesObject.get(property.getLabel());
        if (element == null) return;

        List<T> selected = new ArrayList<>();
        T[] possibleValues = multiProperty.getValues();

        if (element.isJsonArray()) {
            for (JsonElement e : element.getAsJsonArray()) {
                String name = e.getAsString();
                for (T possibleValue : possibleValues) {
                    if (possibleValue.name().equalsIgnoreCase(name)) {
                        selected.add(possibleValue);
                        break;
                    }
                }
            }
        } else if (element.isJsonPrimitive()) {
            String name = element.getAsString();
            for (T possibleValue : possibleValues) {
                if (possibleValue.name().equalsIgnoreCase(name)) {
                    selected.add(possibleValue);
                    break;
                }
            }
        }
        multiProperty.setValue(selected);
    }
}