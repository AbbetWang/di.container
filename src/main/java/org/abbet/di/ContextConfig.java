package org.abbet.di;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

public class ContextConfig {

    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();
    private Map<Class<?>, List<Class<?>>> dependencies = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, context -> instance);
        dependencies.put(type, asList());
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        Constructor<?> injectConstructor = getInjectConstructor(implementation);

        providers.put(type, new ConstructorInjectionProvider<>(injectConstructor));
        dependencies.put(type, stream(injectConstructor.getParameters())
                .map(Parameter::getType)
                .collect(Collectors.toList()));
    }


    public Context getContext() {
        dependencies.keySet().forEach(component -> checkDependency(component, new Stack<>()));

        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }
        };
    }


    private void checkDependency(Class<?> component, Stack<Class<?>> visits) {
        for (Class<?> dependency : dependencies.get(component)) {
            checkMissingDependency(component, dependency);
            if (visits.contains(dependency)) throw new CyclicDependencyFoundException(visits);
            visits.push(dependency);
            checkDependency(dependency, visits);
            visits.pop();

        }
    }

    private void checkMissingDependency(Class<?> component, Class<?> dependency) {
        if (!dependencies.containsKey(dependency)) {
            throw new DependencyNotFoundException(component, dependency);
        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        List<Class<?>> getDependencies();
    }

    public class ConstructorInjectionProvider<T> implements ComponentProvider<T> {
        private Constructor<T> injectConstructor;

        public ConstructorInjectionProvider(Constructor<T> injectConstructor) {
            this.injectConstructor = injectConstructor;
        }

        @Override
        public T get(Context context) {
            try {
                Object[] dependencies = stream(injectConstructor.getParameters())
                        .map(p -> {
                            Class<?> type = p.getType();
                            return context.get(type).get();
                        })
                        .toArray(Object[]::new);
                return injectConstructor.newInstance(dependencies);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<Class<?>> getDependencies() {
            return null;
        }
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        return (Constructor<Type>) injectConstructors.stream()
                .findFirst().orElseGet(() -> {
                    try {
                        return implementation.getConstructor();
                    } catch (Exception e) {
                        throw new IllegalComponentException();
                    }
                });
    }
}
