package org.abbet.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Nested
public class InjectionTest {
    ContextConfig config;

    @BeforeEach
    public void beforeEach() {
        config = new ContextConfig();
    }

    @Nested
    public class ConstructorInjection {
        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {

            }
        }


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
        public void should_throw_exception_if_component_is_abstract() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ConstructorInjection.AbstractComponent.class);
            });
        }

        @Test
        public void should_throw_exception_if_component_is_interface() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(Component.class);
            });
        }

        @Test
        public void should_throw_exception_when_more_than_one_inject_constructor_provided() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ComponentWithMultipleInjectConstructor.class);
            });

        }


        @Test
        public void should_throw_exception_when_no_default_or_inject_constructor() {
            assertThrows(IllegalComponentException.class, () -> {
                new ConstructorInjectionProvider<>(ComponentWithoutInjectOrDefaultConstructor.class);
            });
        }

        @Test
        public void should_include_dependency_from_inject_constructor() {
            ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

    }

    @Nested
    public class FieldInjection {

        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubClassWithFieldInjection extends FieldInjection.ComponentWithFieldInjection {
        }

        @Test
        public void should_inject_dependency_via_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(FieldInjection.ComponentWithFieldInjection.class, FieldInjection.ComponentWithFieldInjection.class);
            FieldInjection.ComponentWithFieldInjection component = config.getContext().get(FieldInjection.ComponentWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);

        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(FieldInjection.SubClassWithFieldInjection.class, FieldInjection.SubClassWithFieldInjection.class);
            FieldInjection.ComponentWithFieldInjection component = config.getContext().get(FieldInjection.SubClassWithFieldInjection.class).get();
            assertSame(dependency, component.dependency);
        }


        @Test
        public void should_include_field_dependency_in_dependencies() {
            ConstructorInjectionProvider<FieldInjection.ComponentWithFieldInjection> provider = new ConstructorInjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Test
        public void should_throw_exception_if_inject_field_is_final() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(FieldInjection.FinalInjectField.class));
        }
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
            config.bind(MethodInjection.InjectMethodWithNoDependency.class, MethodInjection.InjectMethodWithNoDependency.class);
            MethodInjection.InjectMethodWithNoDependency component = config.getContext().get(MethodInjection.InjectMethodWithNoDependency.class).get();
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
            config.bind(MethodInjection.InjectMethodWithDependency.class, MethodInjection.InjectMethodWithDependency.class);
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            MethodInjection.InjectMethodWithDependency component = config.getContext().get(MethodInjection.InjectMethodWithDependency.class).get();

            assertSame(dependency, component.dependency);
        }

        static class SuperClassWithInjectMethod {
            int superCalled = 0;

            @Inject
            void install() {
                superCalled++;
            }
        }

        static class SubclassWithInjectMethod extends MethodInjection.SuperClassWithInjectMethod {
            int subCalled = 0;

            @Inject
            void installAnother() {
                subCalled = superCalled + 1;
            }
        }

        @Test
        public void should_inject_dependency_via_inject_method_from_superclass() {
            config.bind(MethodInjection.SubclassWithInjectMethod.class, MethodInjection.SubclassWithInjectMethod.class);
            MethodInjection.SubclassWithInjectMethod component = config.getContext().get(MethodInjection.SubclassWithInjectMethod.class).get();
            assertEquals(1, component.superCalled);
            assertEquals(2, component.subCalled);

        }

        static class SubclassOverrideSuperClassWithInject extends MethodInjection.SuperClassWithInjectMethod {
            @Inject
            void install() {
                super.install();
            }
        }

        @Test
        public void should_call_only_once_if_subclass_override_superclass_inject_method_with_inject() {
            config.bind(MethodInjection.SubclassOverrideSuperClassWithInject.class, MethodInjection.SubclassOverrideSuperClassWithInject.class);
            MethodInjection.SubclassOverrideSuperClassWithInject component = config.getContext().get(MethodInjection.SubclassOverrideSuperClassWithInject.class).get();
            assertEquals(1, component.superCalled);
        }

        static class SubclassOverrideSuperClassWithoutInject extends MethodInjection.SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call__if_subclass_override_superclass_inject_method_without_inject() {
            config.bind(MethodInjection.SubclassOverrideSuperClassWithoutInject.class, MethodInjection.SubclassOverrideSuperClassWithoutInject.class);
            MethodInjection.SubclassOverrideSuperClassWithoutInject component = config.getContext().get(MethodInjection.SubclassOverrideSuperClassWithoutInject.class).get();
            assertEquals(0, component.superCalled);
        }


        @Test
        public void should_include_dependencies_from_inject_method() {
            ConstructorInjectionProvider<MethodInjection.InjectMethodWithDependency> provider = new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithDependency.class);
            assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
        }

        static class InjectMethodWithTypeParameter {
            @Inject
            <T> void install() {
            }
        }

        @Test
        public void should_throw_exception_if_inject_method_has_type_parameter() {
            assertThrows(IllegalComponentException.class, () -> new ConstructorInjectionProvider<>(MethodInjection.InjectMethodWithTypeParameter.class));
        }


    }

}
