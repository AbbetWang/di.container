package org.abbet.di;

import java.util.*;
import java.util.stream.Collectors;

public class CyclicDependencyFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();

    public CyclicDependencyFoundException(Class<?>... component) {
        components.addAll(Arrays.stream(component).collect(Collectors.toSet()));
    }


    public CyclicDependencyFoundException(List<Class<?>> visits) {
        components.addAll(visits);
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
