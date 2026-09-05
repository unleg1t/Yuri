package ddlc.yuri.api.events;

import ddlc.yuri.api.events.annotations.EventHook;
import ddlc.yuri.api.events.annotations.EventPriority;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private static final Comparator<Method> PRIORITY_COMPARATOR = Comparator.comparingInt(EventBus::priorityOf);

    private final Map<Method, Class<?>> registeredMethodMap;
    private final Map<Method, Object> methodObjectMap;
    private final Map<Class<? extends Event>, List<Method>> priorityMethodMap;

    private static int priorityOf(Method method) {
        EventHook priority = method.getAnnotation(EventHook.class);
        return (priority != null) ? priority.value() : EventPriority.MEDIUM;
    }

    public EventBus() {
        registeredMethodMap = new ConcurrentHashMap<>();
        methodObjectMap = new ConcurrentHashMap<>();
        priorityMethodMap = new ConcurrentHashMap<>();
    }

    /**
     * Registers one or more objects to associate their methods with event annotations and stores them in the event handler.
     *
     * @param obj One or more objects to register.
     */
    public void subscribe(Object... obj) {
        for (Object object : obj) {
            subscribe(object);
        }
    }

    /**
     * Registers an object to associate its methods with event annotations and stores them in the event handler.
     *
     * @param obj The object to register.
     */
    public void subscribe(Object obj) {
        Class<?> clazz = obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            Annotation[] annotations = method.getDeclaredAnnotations();

            for (Annotation annotation : annotations) {
                if (annotation.annotationType() == EventHook.class && method.getParameterTypes().length == 1) {
                    registeredMethodMap.put(method, method.getParameterTypes()[0]);
                    methodObjectMap.put(method, obj);
                    method.setAccessible(true);

                    Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
                    List<Method> handlers = priorityMethodMap.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>());
                    handlers.add(method);
                    PriorityHolder.sort(handlers);
                }
            }
        }
    }

    /**
     * Unregisters an object, removing its associated methods from the event handler.
     *
     * @param obj The object to unregister.
     */
    public void unsubscribe(Object obj) {
        Class<?> clazz = obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (registeredMethodMap.containsKey(method)) {
                registeredMethodMap.remove(method);
                methodObjectMap.remove(method);
                Class<? extends Event> eventClass = method.getParameterTypes()[0].asSubclass(Event.class);
                List<Method> priorityMethods = priorityMethodMap.get(eventClass);
                if (priorityMethods != null) {
                    priorityMethods.remove(method);
                    PriorityHolder.sort(priorityMethods);
                }
            }
        }
    }

    /**
     * Calls the registered methods associated with the provided event, respecting their priorities.
     *
     * @param event The event to call the registered methods for.
     * @return The modified or processed event after calling the methods.
     */
    public Event post(Event event) {
        Class<? extends Event> eventClass = event.getClass();

        List<Method> methods = priorityMethodMap.get(eventClass);
        if (methods != null) {
            for (Method method : methods) {
                Object obj = methodObjectMap.get(method);
                try {
                    method.invoke(obj, event);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }

        return event;
    }

    /** Keeps the per-class handler list sorted so {@link #post} never re-sorts on dispatch. */
    private static final class PriorityHolder {
        static void sort(List<Method> methods) {
            methods.sort(PRIORITY_COMPARATOR);
        }
    }
}
