package org.abbet.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;


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
        public class ConstructorInjection {
            abstract class AbstractComponent implements Component {
                @Inject
                public AbstractComponent() {

                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> {
                    new ConstructorInjectionProvider<>(AbstractComponent.class);
                });
            }

            // TODO: interface
            @Test
            public void should_bind_type_to_a_class_with_default_constructor() {
                config.bind(Component.class, ComponentWithDefaultConstructor.class);
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertTrue(instance instanceof ComponentWithDefaultConstructor);

            }

            @Test
            public void should_bind_type_to_a_class_with_inject_constructor() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, dependency);
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                assertSame(dependency, ((ComponentWithInjectConstructor) instance).getDependency());
            }

            @Test
            public void should_bind_type_to_a_class_with_transitive_dependencies() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                config.bind(String.class, "indirect dependency");
                Component instance = config.getContext().get(Component.class).get();
                assertNotNull(instance);
                Dependency dependency = ((ComponentWithInjectConstructor) instance).getDependency();
                assertNotNull(dependency);
                assertEquals("indirect dependency", ((DependencyWithInjectConstructor) dependency).getDependency());

            }

            @Test
            public void should_throw_exception_when_more_than_one_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithMultipleInjectConstructor.class);
                    config.bind(String.class, "dependency");
                    config.getContext().get(Component.class).get();
                });

            }


            @Test
            public void should_throw_exception_when_no_default_or_inject_constructor() {
                assertThrows(IllegalComponentException.class, () -> {
                    config.bind(Component.class, ComponentWithoutInjectOrDefaultConstructor.class);
                });

            }

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
            public void should_throw_exception_if_transitive_dependency_not_found() {
                config.bind(Component.class, ComponentWithInjectConstructor.class);
                config.bind(Dependency.class, DependencyWithInjectConstructor.class);
                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> {
                    config.getContext();
                });
                assertEquals(String.class, exception.getDependency());
                assertEquals(Dependency.class, exception.getComponent());
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
        public class FieldInjection {

            static class ComponentWithFieldInjection {
                @Inject
                Dependency dependency;
            }

            static class SubClassWithFieldInjection extends ComponentWithFieldInjection {
            }

            @Test
            public void should_inject_dependency_via_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(ComponentWithFieldInjection.class, ComponentWithFieldInjection.class);
                ComponentWithFieldInjection component = config.getContext().get(ComponentWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);

            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(SubClassWithFieldInjection.class, SubClassWithFieldInjection.class);
                ComponentWithFieldInjection component = config.getContext().get(SubClassWithFieldInjection.class).get();
                assertSame(dependency, component.dependency);
            }


            @Test
            public void should_include_field_dependency_in_dependencies() {
                ConstructorInjectionProvider<ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
            // TODO throw exception if field is final
        }

        @Nested
        public class MethodInjection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            public void should_call_inject_method_even_if_no_dependency_declare() {
                config.bind(InjectMethodWithNoDependency.class, InjectMethodWithNoDependency.class);
                InjectMethodWithNoDependency component = config.getContext().get(InjectMethodWithNoDependency.class).get();
                assertTrue(component.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }

            }

            @Test
            public void should_inject_dependency_via_inject_method() {
                config.bind(InjectMethodWithDependency.class, InjectMethodWithDependency.class);
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                InjectMethodWithDependency component = config.getContext().get(InjectMethodWithDependency.class).get();

                assertSame(dependency, component.dependency);
            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method_from_superclass() {
                config.bind(SubclassWithInjectMethod.class, SubclassWithInjectMethod.class);
                SubclassWithInjectMethod component = config.getContext().get(SubclassWithInjectMethod.class).get();
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);

            }

            static class SubclassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_call_only_once_if_subclass_override_superclass_inject_method_with_inject() {
                config.bind(SubclassOverrideSuperClassWithInject.class, SubclassOverrideSuperClassWithInject.class);
                SubclassOverrideSuperClassWithInject component = config.getContext().get(SubclassOverrideSuperClassWithInject.class).get();
                assertEquals(1, component.superCalled);
            }

            static class SubclassOverrideSuperClassWithoutInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call__if_subclass_override_superclass_inject_method_without_inject() {
                config.bind(SubclassOverrideSuperClassWithoutInject.class, SubclassOverrideSuperClassWithoutInject.class);
                SubclassOverrideSuperClassWithoutInject component = config.getContext().get(SubclassOverrideSuperClassWithoutInject.class).get();
                assertEquals(0, component.superCalled);
            }


            @Test
            public void should_include_dependencies_from_inject_method() {
                ConstructorInjectionProvider<InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

            //TODO throw exception if type parameter defined


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

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
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
