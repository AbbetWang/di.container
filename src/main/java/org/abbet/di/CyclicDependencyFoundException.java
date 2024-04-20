package org.abbet.di;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CyclicDependencyFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependencyFoundException(Class<?>... component) {
        components.addAll(Arrays.stream(component).collect(Collectors.toSet()));
    }


    public CyclicDependencyFoundException(Class<?> componentType, CyclicDependencyFoundException e) {
        components.add(componentType);
        components.addAll(e.components);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
