package org.abbet.di;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectionTest {
    private Dependency dependency = mock(Dependency.class);

    private Context context = mock(Context.class);

    @BeforeEach
    public void beforeEach() {
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
    }

    @Nested
    public class ConstructorInjection {
        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {

            }
        }

        @Nested
        class Injection {
            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {

                DefaultConstructor instance = new InjectionProvider<>(DefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {

                ComponentWithInjectConstructor instance = new InjectionProvider<>(ComponentWithInjectConstructor.class).get(context);
                assertNotNull(instance);
                assertSame(dependency, instance.dependency());
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<ComponentWithInjectConstructor> provider = new InjectionProvider<>(ComponentWithInjectConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        @Nested
        class IllegalInjectConstructors {
            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> {
                    new InjectionProvider<>(ConstructorInjection.AbstractComponent.class);
                });
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> {
                    new InjectionProvider<>(Component.class);
                });
            }

            @Test
            public void should_throw_exception_if_more_than_one_inject_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> {
                    new InjectionProvider<>(ComponentWithMultipleInjectConstructor.class);
                });

            }

            @Test
            public void should_throw_exception_if_no_default_or_inject_constructor() {
                assertThrows(IllegalComponentException.class, () -> {
                    new InjectionProvider<>(ComponentWithoutInjectOrDefaultConstructor.class);
                });
            }
        }

        static class DefaultConstructor {

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

    @Nested
    public class FieldInjection {

        @Nested
        class Injection {
            @Test
            public void should_inject_dependency_via_field() {

                ComponentWithFieldInjection component = new InjectionProvider<>(ComponentWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);

            }

            @Test
            public void should_inject_dependency_via_superclass_inject_field() {

                ComponentWithFieldInjection component = new InjectionProvider<>(SubClassWithFieldInjection.class).get(context);
                assertSame(dependency, component.dependency);
            }


            @Test
            public void should_include_dependency_from_field_dependency() {
                InjectionProvider<ComponentWithFieldInjection> provider = new InjectionProvider<>(FieldInjection.ComponentWithFieldInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        static class ComponentWithFieldInjection {
            @Inject
            Dependency dependency;
        }

        static class SubClassWithFieldInjection extends ComponentWithFieldInjection {
        }


        static class FinalInjectField {
            @Inject
            final Dependency dependency = null;
        }

        @Nested
        class IllegalInjectField {
            @Test
            public void should_throw_exception_if_inject_field_is_final() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FieldInjection.FinalInjectField.class));
            }
        }
    }

    @Nested
    public class MethodInjection {

        @Nested
        class Injection {
            @Test
            public void should_call_inject_method_even_if_no_dependency_declare() {

                InjectMethodWithNoDependency component = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
                assertTrue(component.called);
            }

            @Test
            public void should_inject_dependency_via_inject_method() {

                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);
                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_inject_dependency_via_inject_method_from_superclass() {

                SubclassWithInjectMethod component = new InjectionProvider<>(SubclassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);

            }

            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }


            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }

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


            static class SubclassOverrideSuperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_call_only_once_if_subclass_override_superclass_inject_method_with_inject() {

                SubclassOverrideSuperClassWithInject component = new InjectionProvider<>(SubclassOverrideSuperClassWithInject.class).get(context);
                assertEquals(1, component.superCalled);
            }

            static class SubclassOverrideSuperClassWithoutInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_if_subclass_override_superclass_inject_method_without_inject() {

                SubclassOverrideSuperClassWithoutInject component = new InjectionProvider<>(SubclassOverrideSuperClassWithoutInject.class).get(context);
                assertEquals(0, component.superCalled);
            }


            @Test
            public void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }


        }

        @Nested
        class IllegalInjectMethods {

            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }
        }


    }

    static class ComponentWithInjectConstructor implements Component {
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
}
