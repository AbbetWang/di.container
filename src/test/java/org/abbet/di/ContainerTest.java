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
        @Test
        public void should_throw_exception_if_dependencies_not_found() {
            config.bind(Component.class, ComponentWithInjectConstructor.class);
            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                config.getContext();
            });
            assertEquals(Dependency.class, exception.getDependency());
            assertEquals(Component.class, exception.getComponent());

        }

        @Test
        public void should_throw_exception_if_cyclic_dependencies_found() {
            config.bind(Component.class, ComponentCyclicDependencyConstructor.class);
            config.bind(Dependency.class, DependencyCyclicComponentConstructor.class);
            CyclicDependencyFoundException exception = assertThrows(CyclicDependencyFoundException.class, () -> {
                config.getContext();
            });

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());
            assertEquals(2, classes.size());
            assertTrue(classes.contains(Component.class));
            assertTrue(classes.contains(Dependency.class));

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

class ComponentWithInjectConstructor implements Component {
    private Dependency dependency;

    @Inject
    public ComponentWithInjectConstructor(Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public Dependency dependency() {
        return dependency;
    }
}

class ComponentWithMultipleInjectConstructor implements Component {

    String dependency;

    @Inject
    public ComponentWithMultipleInjectConstructor() {
    }

    @Inject
    public ComponentWithMultipleInjectConstructor(String dependency) {
        this.dependency = dependency;
    }
}

class ComponentWithoutInjectOrDefaultConstructor implements Component {

    public ComponentWithoutInjectOrDefaultConstructor(String whatever) {
    }
}

class DependencyCyclicComponentConstructor implements Dependency {
    private Component component;

    @Inject
    public DependencyCyclicComponentConstructor(Component component) {
        this.component = component;
    }
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
