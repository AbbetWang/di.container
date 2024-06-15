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

                DefaultConstructor instance = new ConstructorInjectionProvider<>(DefaultConstructor.class).get(context);
                assertNotNull(instance);
            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {

                ComponentWithInjectConstructor instance = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class).get(context);
                assertNotNull(instance);
                assertSame(dependency, instance.getDependency());
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                ConstructorInjectionProvider<ComponentWithInjectConstructor> provider = new ConstructorInjectionProvider<>(ComponentWithInjectConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }
        }

        static class DefaultConstructor {

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

            ComponentWithFieldInjection component = new ConstructorInjectionProvider<>(ComponentWithFieldInjection.class).get(context);
            assertSame(dependency, component.dependency);

        }

        @Test
        public void should_inject_dependency_via_superclass_inject_field() {

            ComponentWithFieldInjection component = new ConstructorInjectionProvider<>(SubClassWithFieldInjection.class).get(context);
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

            InjectMethodWithNoDependency component = new ConstructorInjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
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

            InjectMethodWithDependency component = new ConstructorInjectionProvider<>(InjectMethodWithDependency.class).get(context);
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

            SubclassWithInjectMethod component = new ConstructorInjectionProvider<>(SubclassWithInjectMethod.class).get(context);
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

            SubclassOverrideSuperClassWithInject component = new ConstructorInjectionProvider<>(SubclassOverrideSuperClassWithInject.class).get(context);
            assertEquals(1, component.superCalled);
        }

        static class SubclassOverrideSuperClassWithoutInject extends MethodInjection.SuperClassWithInjectMethod {
            void install() {
                super.install();
            }
        }

        @Test
        public void should_not_call__if_subclass_override_superclass_inject_method_without_inject() {

            SubclassOverrideSuperClassWithoutInject component = new ConstructorInjectionProvider<>(SubclassOverrideSuperClassWithoutInject.class).get(context);
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
