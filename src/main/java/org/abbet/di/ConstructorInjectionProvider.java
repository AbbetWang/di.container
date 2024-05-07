package org.abbet.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class ConstructorInjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private Constructor<T> injectConstructor;
    private List<Field> injectFields;
    private List<Method> injectMethods;

    public ConstructorInjectionProvider(Class<T> component) {
        if (Modifier.isAbstract(component.getModifiers())) throw new IllegalComponentException();
        this.injectConstructor = getInjectConstructor(component);
        this.injectFields = getInjectFields(component);
        this.injectMethods = getInjectMethods(component);
        if (injectFields.stream().anyMatch(f -> Modifier.isFinal(f.getModifiers())))
            throw new IllegalComponentException();
        if (injectMethods.stream().anyMatch(method -> method.getTypeParameters().length != 0))
            throw new IllegalComponentException();
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
            T instance = injectConstructor.newInstance(dependencies);
            for (Field field : injectFields) {
                field.set(instance, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                method.invoke(instance, stream(method.getParameterTypes()).map(t -> context.get(t).get())
                        .toArray(Object[]::new));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Class<?>> getDependencies() {
        return concat(concat(
                        stream(injectConstructor.getParameters())
                                .map(Parameter::getType)
                        , injectFields.stream().map(v -> v.getType())),
                injectMethods.stream().flatMap(method -> stream(method.getParameterTypes()))
        ).collect(Collectors.toList());
    }

    private static <T> List<Method> getInjectMethods(Class<T> component) {
        List<Method> injectMethods = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectMethods.addAll(stream(current.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(Inject.class))
                    .filter(m -> injectMethods.stream().noneMatch(o -> o.getName().equals(m.getName()) &&
                            Arrays.equals(o.getParameters(), m.getParameters())))
                    .filter(m -> stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(o -> o.getName().equals(m.getName()) &&
                            Arrays.equals(o.getParameters(), m.getParameters())))
                    .collect(Collectors.toList()));
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> List<Field> getInjectFields(Class<T> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectFields.addAll(stream(current.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Inject.class))
                    .collect(Collectors.toList()));
            current = current.getSuperclass();
        }
        return injectFields;
    }

    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = stream(implementation.getConstructors())
                .filter(c -> c.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        return (Constructor<Type>) injectConstructors.stream()
                .findFirst().orElseGet(() -> {
                    try {
                        return implementation.getDeclaredConstructor();
                    } catch (Exception e) {
                        throw new IllegalComponentException();
                    }
                });
    }
}
