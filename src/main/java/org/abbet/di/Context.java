package org.abbet.di;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Object> components = new HashMap<>();
    private Map<Class<?>, Class<?>> componentsImplementation = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> typeClass, ComponentType instance) {
        components.put(typeClass, instance);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        if (components.containsKey(type)) {
            return (ComponentType) components.get(type);
        } else {
            Class<?> implementation = componentsImplementation.get(type);
            try {
                return (ComponentType) implementation.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    public <ComponentType, ComponentImplementation extends ComponentType>
    void bind(Class<ComponentType> typeClass, Class<ComponentImplementation> implementation) {
        componentsImplementation.put(typeClass, implementation);
    }
}
