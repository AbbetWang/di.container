package org.abbet.di;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Object> components = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> componentClass, ComponentType instance) {
        components.put(componentClass, instance);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) components.get(type);
    }
}
