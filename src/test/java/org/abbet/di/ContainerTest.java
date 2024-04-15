package org.abbet.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class ContainerTest {

    Context context;

    @BeforeEach
    public void beforeEach() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {

        // TODO: instance
        @Test
        public void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() {
            };
            context.bind(Component.class, instance);
            assertSame(instance, context.get(Component.class));
        }

        // TODO: abstract class
        // TODO: interface
        @Nested
        public class ConstructorInjection {

            //TODO: No args constructor
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            // TODO: with dependencies

            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, dependency);
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            // TODO: A -> B ->C
            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                context.bind(Component.class, ComponentWithInjectConstructor.class);
                context.bind(Dependency.class, DependencyWithInjectConstructor.class);
                context.bind(String.class, "indirect dependency");
                Component instance = context.get(Component.class);
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());

            }

            // TODO: multiple inject constructors throw exception
            @Test
            public void should_throw_exception_when_more_than_one_inject_constructor_provided() {
                assertThrows(IllegalInjectConstructorException.class, () -> {
                    context.bind(Component.class, ComponentWithMultipleInjectConstructor.class);
                    context.bind(String.class, "dependency");
                    Component component = context.get(Component.class);
                });

            }


            // TODO: nor default constructor or inject constructor exception
            @Test
            public void should_throw_exception_when_no_default_or_inject_constructor() {
                assertThrows(IllegalInjectConstructorException.class, () -> {
                    context.bind(Component.class, ComponentWithoutInjectOrDefaultConstructor.class);
                    Component component = context.get(Component.class);
                });

            }
            // TODO: Dependency Not Found exception


        }

        @Nested
        public class FieldInjection {


        }

        @Nested
        public class MethodInjection {

        }

    }

    @Nested
    public class DependenciesSelection {


    }

    @Nested
    public class LifecycleManagement {


    }

    interface Component {

    }

    interface Dependency {

    }

    static class ComponentWithDefaultConstructor implements Component {
        public ComponentWithDefaultConstructor() {
        }
    }

    static class ComponentWithInjectConstructor implements Component {
        private Dependency dependency;

        @Inject
        public ComponentWithInjectConstructor(Dependency dependency) {
            this.dependency = dependency;
        }

        public Dependency getDependency() {
            return dependency;
        }
    }

    static class DependencyWithInjectConstructor implements Dependency {
        String dependency;

        @Inject
        public DependencyWithInjectConstructor(String dependency) {
            this.dependency = dependency;
        }

        public String getDependency() {
            return dependency;
        }
    }

    static class ComponentWithMultipleInjectConstructor implements Component {

        String dependency;

        @Inject
        public ComponentWithMultipleInjectConstructor() {
        }

        @Inject
        public ComponentWithMultipleInjectConstructor(String dependency) {
            this.dependency = dependency;
        }
    }

    static class ComponentWithoutInjectOrDefaultConstructor implements Component {

        public ComponentWithoutInjectOrDefaultConstructor(String whatever) {
        }
    }
}
