package org.abbet.di;

import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

class InjectionProvider<T> implements ContextConfig.ComponentProvider<T> {
    private Constructor<T> injectConstructor;
    private List<Field> injectFields;
    private List<Method> injectMethods;

    public InjectionProvider(Class<T> component) {
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
            T instance = injectConstructor.newInstance(toDependencies(context, injectConstructor));
            for (Field field : injectFields) {
                field.set(instance, toDependency(context, field));
            }
            for (Method method : injectMethods) {
                method.invoke(instance, toDependencies(context, method));
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
            injectMethods.addAll(injectable(current.getDeclaredMethods())
                    .filter(m -> isOverrideByInjectMethod(injectMethods, m))
                    .filter(m -> isOverrideByNonInjectMethod(component, m))
                    .toList());
            current = current.getSuperclass();
        }
        Collections.reverse(injectMethods);
        return injectMethods;
    }

    private static <T> boolean isOverrideByNonInjectMethod(Class<T> component, Method m) {
        return stream(component.getDeclaredMethods()).filter(m1 -> !m1.isAnnotationPresent(Inject.class)).noneMatch(o -> isOverride(m, o));
    }

    private static boolean isOverrideByInjectMethod(List<Method> injectMethods, Method m) {
        return injectMethods.stream().noneMatch(o -> isOverride(m, o));
    }


    private static <T> List<Field> getInjectFields(Class<T> component) {
        List<Field> injectFields = new ArrayList<>();
        Class<?> current = component;
        while (current != Object.class) {
            injectFields.addAll(injectable(current.getDeclaredFields()).
                    toList());
            current = current.getSuperclass();
        }
        return injectFields;
    }


    private static <Type> Constructor<Type> getInjectConstructor(Class<Type> implementation) {
        List<Constructor<?>> injectConstructors = injectable(implementation.getConstructors()).toList();
        if (injectConstructors.size() > 1) throw new IllegalComponentException();
        return (Constructor<Type>) injectConstructors.stream().findFirst().orElseGet(() -> defaultConstructor(implementation));
    }

    private static <Type> Constructor<Type> defaultConstructor(Class<Type> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (Exception e) {
            throw new IllegalComponentException();
        }
    }

    private static <T extends AnnotatedElement> Stream<T> injectable(T[] declaredFields) {
        return stream(declaredFields)
                .filter(f -> f.isAnnotationPresent(Inject.class));
    }

    private static boolean isOverride(Method m, Method o) {
        return o.getName().equals(m.getName()) && Arrays.equals(o.getParameters(), m.getParameters());
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return stream(executable.getParameterTypes()).map(t -> context.get(t).get()).toArray(Object[]::new);
    }

    private static Object toDependency(Context context, Field field) {
        return context.get(field.getType()).get();
    }
}
