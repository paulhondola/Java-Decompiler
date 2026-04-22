package org.paul.formatter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.paul.config.DecompileConfig;
import org.paul.model.ClassInfo;
import org.paul.model.FieldInfo;
import org.paul.model.Relationship;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link YumlFormatter}.
 *
 * <p>Tests use hand-crafted {@link ClassInfo} objects so that field and method ordering
 * is fully deterministic, independent of JVM reflection order.
 */
class YumlFormatterTest {

    // Arbitrary Class<?> carriers — formatters only read getSimpleName()
    private static final Class<?> OBSERVER_CLASS = Runnable.class;   // simple name "Runnable" used as stand-in
    private static final Class<?> SUBJECT_CLASS = Comparable.class;

    private static final DecompileConfig CONFIG = DecompileConfig.defaults();

    private static ClassInfo classInfo(Class<?> clazz,
                                       List<FieldInfo> fields, List<String> methods,
                                       List<Relationship> relationships) {
        return new ClassInfo(clazz, fields, methods, relationships);
    }

    @Nested
    @DisplayName("CLASSES mode")
    class ClassesMode {

        private final YumlFormatter yuml = new YumlFormatter();

        @Test
        @DisplayName("class with no fields or methods renders as [Name||]")
        void format_noMembers_rendersEmptySections() {
            var info = classInfo(OBSERVER_CLASS, List.of(), List.of(), List.of());
            assertThat(yuml.format(List.of(info), CONFIG)).isEqualTo("[Runnable||]\n");
        }

        @Test
        @DisplayName("fields are rendered as 'modifier name:type' separated by semicolons")
        void format_withFields_rendersFieldSection() {
            var info = classInfo(OBSERVER_CLASS,
                    List.of(new FieldInfo("count", "int", '-'),
                            new FieldInfo("name", "String", '+')),
                    List.of(),
                    List.of());
            assertThat(yuml.format(List.of(info), CONFIG))
                    .isEqualTo("[Runnable|- count:int;+ name:String|]\n");
        }

        @Test
        @DisplayName("methods are rendered semicolon-separated in the third section")
        void format_withMethods_rendersMethodSection() {
            var info = classInfo(OBSERVER_CLASS,
                    List.of(),
                    List.of("update()", "display()"),
                    List.of());
            assertThat(yuml.format(List.of(info), CONFIG))
                    .isEqualTo("[Runnable||update();display()]\n");
        }

        @Test
        @DisplayName("full class with fields, methods, and relationship")
        void format_fullClass_rendersAllSections() {
            var info = classInfo(OBSERVER_CLASS,
                    List.of(new FieldInfo("value", "int", '-')),
                    List.of("update()"),
                    List.of(new Relationship.Implements("Subject")));
            String output = yuml.format(List.of(info), CONFIG);
            assertThat(output).contains("[Runnable|- value:int|update()]");
            assertThat(output).contains("[Subject]^-.-[Runnable]");
        }

        @Test
        @DisplayName("showAttributes=false omits field section content")
        void format_showAttributesFalse_fieldsOmitted() {
            var config = new DecompileConfig(List.of(), false, true, false);
            var info = classInfo(OBSERVER_CLASS,
                    List.of(new FieldInfo("count", "int", '-')),
                    List.of("run()"),
                    List.of());
            assertThat(yuml.format(List.of(info), config))
                    .isEqualTo("[Runnable||run()]\n");
        }

        @Test
        @DisplayName("showMethods=false omits method section content")
        void format_showMethodsFalse_methodsOmitted() {
            var config = new DecompileConfig(List.of(), false, false, true);
            var info = classInfo(OBSERVER_CLASS,
                    List.of(new FieldInfo("count", "int", '-')),
                    List.of("run()"),
                    List.of());
            assertThat(yuml.format(List.of(info), config))
                    .isEqualTo("[Runnable|- count:int|]\n");
        }
    }
}
