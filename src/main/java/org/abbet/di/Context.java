package org.abbet.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class Context {

    private Map<Class<?>, Provider<?>> providers = new HashMap<>();

    public <Type> void bind(Class<Type> type, Type instance) {
        providers.put(type, (Provider<Type>) () -> instance);
    }

    public <Type, Implementation extends Type>
    void bind(Class<Type> type, Class<Implementation> implementation) {
        List<Constructor<?>> constructors = stream(implementation.getConstructors()).filter(it -> it.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if(constructors.size()>1){
            throw new IllegalInjectConstructorException();
        }
        providers.put(type, (Provider<Type>) () -> {
            try {
                Constructor<?> injectConstructor = getInjectConstructor(implementation);
                Object[] dependencies = stream(injectConstructor.getParameters())
                        .map(p -> get(p.getType()))
                        .toArray(Object[]::new);
                return (Type) injectConstructor.newInstance(dependencies);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        Stream<Constructor<?>> injectConstructors = stream(implementation.getConstructors());
        return (Constructor<Type>) injectConstructors
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .findFirst().orElseGet(() -> {
                    try {
                        return implementation.getConstructor();
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public <Type> Type get(Class<Type> type) {
        return (Type) providers.get(type).get();
    }

}
