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

    // ── SIMPLE mode ───────────────────────────────────────────────────────────

    private static ClassInfo classInfo(Class<?> clazz,
                                       List<FieldInfo> fields, List<String> methods,
                                       List<Relationship> relationships) {
        return new ClassInfo(clazz, fields, methods, relationships);
    }

    // ── CLASSES mode ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SIMPLE mode")
    class SimpleMode {

        private final YumlFormatter yuml = new YumlFormatter(YumlFormatter.Mode.SIMPLE);

        @Test
        @DisplayName("empty class list produces empty string")
        void format_emptyList_returnsEmptyString() {
            assertThat(yuml.format(List.of(), CONFIG)).isEmpty();
        }

        @Test
        @DisplayName("single class renders as [ClassName]")
        void format_singleClass_rendersName() {
            var info = classInfo(OBSERVER_CLASS, List.of(), List.of(), List.of());
            assertThat(yuml.format(List.of(info), CONFIG)).isEqualTo("[Runnable]\n");
        }

        @Test
        @DisplayName("class with fields and methods — members are NOT shown")
        void format_classWithMembers_noMembersInOutput() {
            var info = classInfo(OBSERVER_CLASS,
                    List.of(new FieldInfo("count", "int", '-')),
                    List.of("tick()"),
                    List.of());
            assertThat(yuml.format(List.of(info), CONFIG))
                    .doesNotContain("|")
                    .isEqualTo("[Runnable]\n");
        }

        @Test
        @DisplayName("Implements relationship renders as [Interface]^-.-[Implementor]")
        void format_implements_rendersWithDottedArrow() {
            var info = classInfo(OBSERVER_CLASS, List.of(), List.of(),
                    List.of(new Relationship.Implements("Subject")));
            assertThat(yuml.format(List.of(info), CONFIG))
                    .contains("[Subject]^-.-[Runnable]");
        }

        @Test
        @DisplayName("Extends relationship renders as [Parent]^-[Child]")
        void format_extends_rendersWithSolidArrow() {
            var info = classInfo(OBSERVER_CLASS, List.of(), List.of(),
                    List.of(new Relationship.Extends("BaseClass")));
            assertThat(yuml.format(List.of(info), CONFIG))
                    .contains("[BaseClass]^-[Runnable]");
        }

        @Test
        @DisplayName("Association relationship renders as [Owner]->[Target]")
        void format_association_rendersWithForwardArrow() {
            var info = classInfo(OBSERVER_CLASS, List.of(), List.of(),
                    List.of(new Relationship.Association("Subject")));
            assertThat(yuml.format(List.of(info), CONFIG))
                    .contains("[Runnable]->[Subject]");
        }

        @Test
        @DisplayName("Dependency relationship renders identically to association")
        void format_dependency_rendersLikeAssociation() {
            var info = classInfo(OBSERVER_CLASS, List.of(), List.of(),
                    List.of(new Relationship.Dependency("Subject")));
            assertThat(yuml.format(List.of(info), CONFIG))
                    .contains("[Runnable]->[Subject]");
        }

        @Test
        @DisplayName("multiple classes each appear on their own line")
        void format_multipleClasses_eachOnOwnLine() {
            var a = classInfo(OBSERVER_CLASS, List.of(), List.of(), List.of());
            var b = classInfo(SUBJECT_CLASS, List.of(), List.of(), List.of());
            assertThat(yuml.format(List.of(a, b), CONFIG))
                    .isEqualTo("[Runnable]\n[Comparable]\n");
        }

        @Test
        @DisplayName("relationships are emitted after the class node line")
        void format_relationshipAppearsAfterClassLine() {
            var info = classInfo(OBSERVER_CLASS, List.of(), List.of(),
                    List.of(new Relationship.Implements("Subject")));
            String output = yuml.format(List.of(info), CONFIG);
            int classLine = output.indexOf("[Runnable]");
            int relLine = output.indexOf("[Subject]^-.-[Runnable]");
            assertThat(classLine).isLessThan(relLine);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CLASSES mode")
    class ClassesMode {

        private final YumlFormatter yuml = new YumlFormatter(YumlFormatter.Mode.CLASSES);

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
