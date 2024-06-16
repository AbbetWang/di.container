package org.abbet.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void beforeEach() {
        config = new ContextConfig();
    }


    @Nested
    class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            assertSame(instance, config.getContext().get(Component.class).get());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends Component> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(Component.class, componentType);
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(Arguments.of(Named.of("Constructor Injection", TypeBinding.ConstructorInjection.class)),
                    Arguments.of(Named.of("Field Injection", TypeBinding.FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", TypeBinding.MethodInjection.class))
            );
        }

        static class ConstructorInjection implements Component {
            private Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return this.dependency;
            }
        }

        static class FieldInjection implements Component {

            @Inject
            Dependency dependency;

            @Override
            public Dependency dependency() {
                return this.dependency;
            }
        }

        static class MethodInjection implements Component {

            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            @Override
            public Dependency dependency() {
                return this.dependency;
            }
        }

        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());
        }

    }

    @Nested
    public class DependencyCheck {

        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependencies_not_found(Class<? extends Component> component) {
            config.bind(Component.class, component);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());

        }

        public static Stream<Arguments> should_throw_exception_if_dependencies_not_found() {
            return Stream.of(Arguments.of(Named.of("Inject Constructor", DependencyCheck.MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Inject Field", DependencyCheck.MissingDependencyField.class)),
                    Arguments.of(Named.of("Inject Method", DependencyCheck.MissingDependencyMethod.class))
            );
        }

        static class MissingDependencyConstructor implements Component {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements Component {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements Component {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends Component> component, Class<? extends Dependency> dependency) {
            config.bind(Component.class, component);
            config.bind(Dependency.class, dependency);
            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () -> {
                config.getContext();
            });

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());
            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));

        }

        static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class)
            ))
                for (Named dependency : List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class)
                ))
                    arguments.add(Arguments.of(component, dependency));
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements Component {
            private Dependency dependency;

            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            private Component component;

            @Inject
            public CyclicDependencyInjectConstructor(Component component) {
                this.component = component;
            }
        }

        static class CyclicComponentInjectField implements Component {

            @Inject
            Dependency dependency;
        }

        static class CyclicDependencyInjectField implements Dependency {

            @Inject
            Component component;
        }

        static class CyclicComponentInjectMethod implements Component {

            private Dependency dependency;

            @Inject
            public void install(Dependency dependency) {
                this.dependency = dependency;
            }
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            private Component component;

            @Inject
            void install(Component component) {
                this.component = component;
            }
        }

        @Test
        public void should_throw_exception_if_transitive_cyclic_dependencies_found() {
            config.bind(Component.class, ComponentCyclicDependencyConstructor.class);
            config.bind(Dependency.class, DependencyDependedOnAnotherDependency.class);
            config.bind(AnotherDependency.class, AnotherDependencyDependComponent.class);
            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () -> {
                config.getContext();
            });
            Set<Class<?>> classes = Sets.newSet(exception.getComponents());
            assertEquals(3, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));
            assertTrue(classes.contains(AnotherDependency.class));
        }
    }


    @Nested
    public class DependenciesSelection {


    }

    @Nested
    public class LifecycleManagement {


    }

}

interface Component {

    default Dependency dependency() {
        return null;
    }
}

interface Dependency {

}

interface AnotherDependency {

}


class ComponentCyclicDependencyConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentCyclicDependencyConstructor(Dependency dependency) {
        this.dependency = dependency;
    }
}

class AnotherDependencyDependComponent implements AnotherDependency {
    private Component component;

    @Inject
    public AnotherDependencyDependComponent(Component component) {
        this.component = component;
    }
}

class DependencyDependedOnAnotherDependency implements Dependency {
    private AnotherDependency anotherDependency;

    @Inject
    public DependencyDependedOnAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}
