package org.abbet.di;

import java.util.HashMap;
import java.util.Map;

public class Context {
    private Map<Class<?>, Object> components = new HashMap<>();

    public <ComponentType> void bind(Class<ComponentType> typeClass, ComponentType instance) {
        components.put(typeClass, instance);
    }

    public <ComponentType> ComponentType get(Class<ComponentType> type) {
        return (ComponentType) components.get(type);
    }

    public <ComponentType, ComponentImplementation>
    void bind(Class<ComponentType> typeClass, Class<ComponentImplementation> implementation) {
        try {
            components.put(typeClass, implementation.newInstance());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
