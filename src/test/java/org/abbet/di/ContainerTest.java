package org.abbet.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


public class ContainerTest {

    ContextConfig config;

    @BeforeEach
    public void beforeEach() {
        config = new ContextConfig();
    }

    @Nested
    public class ComponentConstruction {

        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            config.bind(Component.class, instance);
            assertSame(instance, config.getContext().get(Component.class).get());
        }


        @Test
        public void should_return_empty_if_component_not_defined() {
            Optional<Component> component = config.getContext().get(Component.class);
            assertTrue(component.isEmpty());

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

    }

    @Nested
    public class DependenciesSelection {


    }

    @Nested
    public class LifecycleManagement {


    }

}

interface Component {

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

    public Dependency getDependency() {
        return dependency;
    }
}

class DependencyWithInjectConstructor implements Dependency {
    String dependency;

    @Inject
    public DependencyWithInjectConstructor(String dependency) {
        this.dependency = dependency;
    }

    public String getDependency() {
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
