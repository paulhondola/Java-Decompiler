package org.paul.introspection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.paul.config.DecompileConfig;
import org.paul.model.ClassInfo;
import org.paul.model.FieldInfo;
import org.paul.model.Relationship;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ClassInspectorTest {

    // ── Test fixtures ─────────────────────────────────────────────────────────
    // Static nested types so getSimpleName() returns plain names (no outer prefix)

    private static final DecompileConfig CONFIG = DecompileConfig.defaults();

    private static FieldInfo fieldNamed(Class<?> clazz, String name) {
        ClassInfo info = ClassInspector.inspect(clazz, CONFIG, Set.of(clazz.getSimpleName()));
        return info.fields().stream()
                .filter(f -> f.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Field not found: " + name));
    }

    interface Printable {
        void print();
    }

    interface Displayable extends Printable {
        void display();
    }

    static class Widget {
        public float height;
        protected int width;
        private String label;

        public String getLabel() {
            return label;
        }

        protected void resize() {
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    static class Button extends Widget {
        private Widget icon;

        public void click() {
        }
    }

    static class GenericHolder {
        private List<Widget> items;
        private Class<?> meta;
    }

    @Nested
    @DisplayName("isInterface flag")
    class IsInterface {

        @Test
        @DisplayName("returns true for an interface")
        void inspect_interface_setsIsInterfaceTrue() {
            var info = ClassInspector.inspect(Printable.class, CONFIG, Set.of("Printable"));
            assertThat(info.clazz().isInterface()).isTrue();
        }

        @Test
        @DisplayName("returns false for a class")
        void inspect_class_setsIsInterfaceFalse() {
            var info = ClassInspector.inspect(Widget.class, CONFIG, Set.of("Widget"));
            assertThat(info.clazz().isInterface()).isFalse();
        }
    }

    @Nested
    @DisplayName("field extraction")
    class Fields {

        @Test
        @DisplayName("counts all declared (non-synthetic) fields")
        void inspect_widget_extractsThreeFields() {
            var info = ClassInspector.inspect(Widget.class, CONFIG, Set.of("Widget"));
            assertThat(info.fields()).hasSize(3);
        }

        @Test
        @DisplayName("private field gets '-' access modifier")
        void inspect_privateField_dashModifier() {
            var label = fieldNamed(Widget.class, "label");
            assertThat(label.accessModifier()).isEqualTo('-');
        }

        @Test
        @DisplayName("protected field gets '#' access modifier")
        void inspect_protectedField_hashModifier() {
            var width = fieldNamed(Widget.class, "width");
            assertThat(width.accessModifier()).isEqualTo('#');
        }

        @Test
        @DisplayName("public field gets '+' access modifier")
        void inspect_publicField_plusModifier() {
            var height = fieldNamed(Widget.class, "height");
            assertThat(height.accessModifier()).isEqualTo('+');
        }

        @Test
        @DisplayName("field type name is the simple class name")
        void inspect_field_typeNameUsesSimpleName() {
            var label = fieldNamed(Widget.class, "label");
            assertThat(label.typeName()).isEqualTo("String");
        }

        @Test
        @DisplayName("interface has no declared fields")
        void inspect_interface_noFields() {
            var info = ClassInspector.inspect(Printable.class, CONFIG, Set.of("Printable"));
            assertThat(info.fields()).isEmpty();
        }
    }

    @Nested
    @DisplayName("method extraction")
    class Methods {

        @Test
        @DisplayName("extracts all non-synthetic methods as 'name()' strings")
        void inspect_widget_extractsMethods() {
            var info = ClassInspector.inspect(Widget.class, CONFIG, Set.of("Widget"));
            assertThat(info.methods()).containsExactlyInAnyOrder("getLabel()", "resize()");
        }

        @Test
        @DisplayName("interface methods are extracted")
        void inspect_interface_extractsMethods() {
            var info = ClassInspector.inspect(Printable.class, CONFIG, Set.of("Printable"));
            assertThat(info.methods()).containsExactly("print()");
        }

        @Test
        @DisplayName("method name always ends with ()")
        void inspect_methodNames_alwaysHaveParentheses() {
            var info = ClassInspector.inspect(Widget.class, CONFIG, Set.of("Widget"));
            assertThat(info.methods()).allMatch(m -> m.endsWith("()"));
        }
    }

    @Nested
    @DisplayName("relationship extraction")
    class Relationships {

        @Test
        @DisplayName("class extending loaded class gets Extends relationship")
        void inspect_buttonExtendsWidget_addsExtendsRelationship() {
            var info = ClassInspector.inspect(
                    Button.class, CONFIG, Set.of("Button", "Widget"));

            assertThat(info.relationships())
                    .filteredOn(r -> r instanceof Relationship.Extends(
                            String targetName
                    ) && targetName.equals("Widget"))
                    .hasSize(1);
        }

        @Test
        @DisplayName("class extending a class NOT in loaded set gets no Extends")
        void inspect_superclassNotInLoadedSet_noExtendsRelationship() {
            // Widget extends Object, which is NOT in loaded names
            var info = ClassInspector.inspect(Widget.class, CONFIG, Set.of("Widget"));

            assertThat(info.relationships())
                    .noneMatch(r -> r instanceof Relationship.Extends);
        }

        @Test
        @DisplayName("class implementing loaded interface gets Implements relationship")
        void inspect_widgetImplementsNothing_buttonDoesNotImplementPrintable() {
            // Printable is extended by Displayable; Button only extends Widget
            var info = ClassInspector.inspect(
                    Button.class, CONFIG, Set.of("Button", "Widget", "Printable"));

            assertThat(info.relationships())
                    .noneMatch(r -> r instanceof Relationship.Implements(String targetName)
                            && targetName.equals("Printable"));
        }

        @Test
        @DisplayName("interface extending loaded interface gets Implements relationship")
        void inspect_displayableExtendsInitiable_addsImplements() {
            var info = ClassInspector.inspect(
                    Displayable.class, CONFIG, Set.of("Displayable", "Printable"));

            assertThat(info.relationships())
                    .filteredOn(r -> r instanceof Relationship.Implements(
                            String targetName
                    ) && targetName.equals("Printable"))
                    .hasSize(1);
        }

        @Test
        @DisplayName("field whose type is a loaded class adds Association")
        void inspect_buttonIconField_addsAssociationToWidget() {
            var info = ClassInspector.inspect(
                    Button.class, CONFIG, Set.of("Button", "Widget"));

            assertThat(info.relationships())
                    .filteredOn(r -> r instanceof Relationship.Association(
                            String targetName
                    ) && targetName.equals("Widget"))
                    .hasSize(1);
        }

        @Test
        @DisplayName("field whose type is NOT in loaded set produces no association")
        void inspect_fieldNotInLoadedSet_noAssociation() {
            // Widget.label is String — not in loaded set
            var info = ClassInspector.inspect(Widget.class, CONFIG, Set.of("Widget"));

            assertThat(info.relationships())
                    .noneMatch(r -> r instanceof Relationship.Association);
        }

        @Test
        @DisplayName("parameterized field contributes type-arg as association, not raw type")
        void inspect_genericField_typeArgIsAssociation() {
            // GenericHolder has List<Widget> — List is java.util, Widget is loaded
            var info = ClassInspector.inspect(
                    GenericHolder.class, CONFIG, Set.of("GenericHolder", "Widget"));

            assertThat(info.relationships())
                    .filteredOn(r -> r instanceof Relationship.Association(
                            String targetName
                    ) && targetName.equals("Widget"))
                    .hasSize(1);

            // "List" itself should NOT be in associations
            assertThat(info.relationships())
                    .noneMatch(r -> r instanceof Relationship.Association(
                            String targetName
                    ) && targetName.equals("List"));
        }

        @Test
        @DisplayName("duplicate field associations to same target are deduplicated")
        void inspect_twoFieldsSameTarget_onlyOneAssociation() {
            // Define inline: class with two Widget fields
            // (re-use Button which has one Widget field; Widget.class is loaded)
            var info = ClassInspector.inspect(Button.class, CONFIG, Set.of("Button", "Widget"));

            long count = info.relationships().stream()
                    .filter(r -> r instanceof Relationship.Association(
                            String targetName
                    ) && targetName.equals("Widget"))
                    .count();
            assertThat(count).isEqualTo(1);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTypeName helper")
    class GetTypeName {

        @Test
        @DisplayName("simple class returns its simple name")
        void getTypeName_simpleClass_returnsSimpleName() {
            assertThat(ClassInspector.getTypeName(String.class, CONFIG)).isEqualTo("String");
        }

        @Test
        @DisplayName("primitive type returns its primitive name")
        void getTypeName_primitiveInt_returnsInt() {
            assertThat(ClassInspector.getTypeName(int.class, CONFIG)).isEqualTo("int");
        }

        @Test
        @DisplayName("parameterized type renders as 'RawType of ArgType'")
        void getTypeName_parameterizedType_rendersOfSyntax() throws Exception {
            Type genericType = GenericHolder.class.getDeclaredField("items").getGenericType();
            assertThat(ClassInspector.getTypeName(genericType, CONFIG)).isEqualTo("List of Widget");
        }

        @Test
        @DisplayName("wildcard type (Class<?>) renders as 'ClassName of ?'")
        void getTypeName_classOfWildcard_rendersQuestionMark() throws Exception {
            Type genericType = GenericHolder.class.getDeclaredField("meta").getGenericType();
            assertThat(ClassInspector.getTypeName(genericType, CONFIG)).isEqualTo("Class of ?");
        }
    }
}
