package org.abbet.di;

import java.util.*;

import static java.util.Arrays.stream;

public class ContextConfig {

    private Map<Class<?>, ComponentProvider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, new ComponentProvider<Type>() {
            @Override
            public Type get(Context context) {
                return instance;
            }

            @Override
            public List<Class<?>> getDependencies() {
                return List.of();
            }
        });
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        providers.put(type, new ConstructorInjectionProvider<>(implementation));
    }


    public Context getContext() {
        providers.keySet().forEach(component -> checkDependency(component, new Stack<>()));
        return new Context() {
            @Override
            public <Type> Optional<Type> get(Class<Type> type) {
                return Optional.ofNullable(providers.get(type)).map(provider -> (Type) provider.get(this));
            }
        };
    }


    private void checkDependency(Class<?> component, Stack<Class<?>> visits) {
        for (Class<?> dependency : providers.get(component).getDependencies()) {
            if (!providers.containsKey(dependency)) {
                throw new DependencyNotFoundException(component, dependency);
            }
            if (visits.contains(dependency)) throw new CyclicDependencyFoundException(visits);
            visits.push(dependency);
            checkDependency(dependency, visits);
            visits.pop();

        }
    }

    interface ComponentProvider<T> {
        T get(Context context);

        List<Class<?>> getDependencies();
    }

}
