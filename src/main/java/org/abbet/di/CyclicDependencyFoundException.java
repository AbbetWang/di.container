package org.abbet.di;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependencyFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependencyFoundException(Class<?> component) {
        components.add(component);
    }

    public CyclicDependencyFoundException(Class<?> componentType, CyclicDependencyFoundException e) {
        components.add(componentType);
        components.addAll(e.components);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
