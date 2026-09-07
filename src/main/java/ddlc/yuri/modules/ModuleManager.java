package ddlc.yuri.modules;

import com.google.common.collect.ImmutableClassToInstanceMap;
import ddlc.yuri.Yuri;
import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.impl.client.KeyPressEvent;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class ModuleManager {

    private final ImmutableClassToInstanceMap<Module> instanceMap;

    public ModuleManager() {
        instanceMap = scanAndBuildInstanceMap();
        getModules().forEach(Module::reflectProperties);
        getModules().forEach(Module::resetPropertyValues);
        Yuri.INSTANCE.getEventBus().subscribe(this);
    }

    @EventHook
    public void onKeyPress(KeyPressEvent event) {
        final int keyPressed = event.getKey();
        for (final Module module : this.getModules()) {
            final int moduleBind = module.getKey();
            if (moduleBind == keyPressed) {
                module.toggle();
            }
        }
    }

    public void postInit() {
        getModules().forEach(Module::resetPropertyValues);

        for (final Module module : getModules()) {
            ModuleInfo info = module.getClass().getAnnotation(ModuleInfo.class);
            if (info != null && info.enabledByDefault() && !module.isEnabled()) {
                module.setEnabled(true);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ImmutableClassToInstanceMap<Module> scanAndBuildInstanceMap() {
        List<Module> modules = new ArrayList<>();
        Reflections reflections = new Reflections("ddlc.yuri.modules");

        for (Class<? extends Module> clazz : reflections.getSubTypesOf(Module.class)) {
            if (Modifier.isAbstract(clazz.getModifiers()))
                continue;

            Module module = instantiate(clazz);
            if (module != null) {
                modules.add(module);
            }
        }

        modules.sort(Comparator.comparing(Module::getLabel, String.CASE_INSENSITIVE_ORDER));

        ImmutableClassToInstanceMap.Builder<Module> modulesBuilder = ImmutableClassToInstanceMap.builder();
        for (Module module : modules) {
            modulesBuilder.put((Class<Module>) module.getClass(), module);
        }

        return modulesBuilder.build();
    }

    private Module instantiate(Class<? extends Module> clazz) {
        try {
            Field instanceField = clazz.getField("INSTANCE");
            if (Module.class.isAssignableFrom(instanceField.getType())) {
                Object value = instanceField.get(null);
                if (value instanceof Module) {
                    return (Module) value;
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception ignored) {
        }

        return null;
    }

    public Collection<Module> getModules() {
        return instanceMap.values();
    }

    public <T extends Module> T getModule(Class<T> moduleClass)  {
        return instanceMap.getInstance(moduleClass);
    }

    public Module getModule(String label) {
        return getModules().stream().filter(module -> module.getLabel().replaceAll(" ", "").equalsIgnoreCase(label)).findFirst().orElse(null);
    }

    public static <T extends Module> T getInstance(Class<T> clazz) {
        return Yuri.INSTANCE.getModuleManager().getModule(clazz);
    }

    public List<Module> getModulesForCategory(ModuleCategory category) {
        return getModules().stream()
                .filter(module -> module.getCategory() == category)
                .collect(Collectors.toList());
    }
}