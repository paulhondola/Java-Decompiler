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
 * Unit tests for {@link PlantUmlFormatter}.
 *
 * <p>Tests use hand-crafted {@link ClassInfo} objects with controlled member ordering.
 */
class PlantUmlFormatterTest {

    private static final DecompileConfig CONFIG = DecompileConfig.defaults();
    private final PlantUmlFormatter formatter = new PlantUmlFormatter();

    // ── Envelope ──────────────────────────────────────────────────────────────

    private static ClassInfo classInfo(Class<?> clazz,
                                       List<FieldInfo> fields, List<String> methods,
                                       List<Relationship> relationships) {
        return new ClassInfo(clazz, fields, methods, relationships);
    }

    // ── Class / interface blocks ───────────────────────────────────────────────

    @Nested
    @DisplayName("document envelope")
    class Envelope {

        @Test
        @DisplayName("output starts with @startuml")
        void format_outputStartsWithStartUml() {
            assertThat(formatter.format(List.of(), CONFIG)).startsWith("@startuml");
        }

        @Test
        @DisplayName("output ends with @enduml followed by newline")
        void format_outputEndsWithEndUml() {
            assertThat(formatter.format(List.of(), CONFIG)).endsWith("@enduml\n");
        }

        @Test
        @DisplayName("empty class list produces minimal @startuml / @enduml document")
        void format_emptyList_producesMinimalDocument() {
            assertThat(formatter.format(List.of(), CONFIG))
                    .isEqualTo("@startuml\n\n@enduml\n");
        }
    }

    // ── Relationships ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("class and interface blocks")
    class Blocks {

        @Test
        @DisplayName("non-interface renders with 'class' keyword")
        void format_class_usesClassKeyword() {
            var info = classInfo(String.class, List.of(), List.of(), List.of());
            assertThat(formatter.format(List.of(info), CONFIG)).contains("class String{");
        }

        @Test
        @DisplayName("interface renders with 'interface' keyword")
        void format_interface_usesInterfaceKeyword() {
            var info = classInfo(Runnable.class, List.of(), List.of(), List.of());
            assertThat(formatter.format(List.of(info), CONFIG)).contains("interface Runnable{");
        }

        @Test
        @DisplayName("each class block is closed with '}'")
        void format_classBlock_isClosed() {
            var info = classInfo(String.class, List.of(), List.of(), List.of());
            String output = formatter.format(List.of(info), CONFIG);
            assertThat(output).contains("class String{\n}\n");
        }

        @Test
        @DisplayName("fields render as '  modifier name:type' with 2-space indent")
        void format_field_rendersWithTwoSpaceIndent() {
            var info = classInfo(Runnable.class,
                    List.of(new FieldInfo("count", "int", '-')),
                    List.of(), List.of());
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("  -count:int\n");
        }

        @Test
        @DisplayName("methods render as '  methodName()' with 2-space indent")
        void format_method_rendersWithTwoSpaceIndent() {
            var info = classInfo(Runnable.class, List.of(),
                    List.of("run()"), List.of());
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("  run()\n");
        }

        @Test
        @DisplayName("field access modifiers are attached directly to the name (no space)")
        void format_field_modifierDirectlyBeforeName() {
            var info = classInfo(Runnable.class,
                    List.of(new FieldInfo("value", "float", '#')),
                    List.of(), List.of());
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("  #value:float");
        }

        @Test
        @DisplayName("showAttributes=false produces class block with no fields")
        void format_showAttributesFalse_noFields() {
            var config = new DecompileConfig(List.of(), false, true, false);
            var info = classInfo(Runnable.class,
                    List.of(new FieldInfo("count", "int", '-')),
                    List.of("run()"), List.of());
            String output = formatter.format(List.of(info), config);
            assertThat(output).doesNotContain("-count:int");
            assertThat(output).contains("  run()");
        }

        @Test
        @DisplayName("showMethods=false produces class block with no methods")
        void format_showMethodsFalse_noMethods() {
            var config = new DecompileConfig(List.of(), false, false, true);
            var info = classInfo(Runnable.class,
                    List.of(new FieldInfo("count", "int", '-')),
                    List.of("run()"), List.of());
            String output = formatter.format(List.of(info), config);
            assertThat(output).contains("  -count:int");
            assertThat(output).doesNotContain("run()");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("relationship lines")
    class Relationships {

        @Test
        @DisplayName("Implements relationship renders as 'Interface <|--- Implementor'")
        void format_implements_rendersInheritanceArrow() {
            var info = classInfo(Runnable.class, List.of(), List.of(),
                    List.of(new Relationship.Implements("Subject")));
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("Subject <|--- Runnable");
        }

        @Test
        @DisplayName("Extends relationship renders as 'Parent <|--- Child'")
        void format_extends_rendersInheritanceArrow() {
            var info = classInfo(Runnable.class, List.of(), List.of(),
                    List.of(new Relationship.Extends("BaseClass")));
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("BaseClass <|--- Runnable");
        }

        @Test
        @DisplayName("Association relationship renders as 'Owner ---> Target'")
        void format_association_rendersDependencyArrow() {
            var info = classInfo(Runnable.class, List.of(), List.of(),
                    List.of(new Relationship.Association("Observer")));
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("Runnable ---> Observer");
        }

        @Test
        @DisplayName("Dependency relationship renders identically to association")
        void format_dependency_rendersDependencyArrow() {
            var info = classInfo(Runnable.class, List.of(), List.of(),
                    List.of(new Relationship.Dependency("Observer")));
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("Runnable ---> Observer");
        }

        @Test
        @DisplayName("each relationship line is followed by a blank line")
        void format_relationshipLine_followedByBlankLine() {
            var info = classInfo(Runnable.class, List.of(), List.of(),
                    List.of(new Relationship.Implements("Subject")));
            assertThat(formatter.format(List.of(info), CONFIG))
                    .contains("Subject <|--- Runnable\n\n");
        }

        @Test
        @DisplayName("class definitions appear before relationship section")
        void format_classBlocksBeforeRelationships() {
            var info = classInfo(Runnable.class, List.of(), List.of(),
                    List.of(new Relationship.Implements("Subject")));
            String output = formatter.format(List.of(info), CONFIG);
            int blockPos = output.indexOf("class Runnable{");
            int relPos = output.indexOf("Subject <|--- Runnable");
            assertThat(blockPos).isLessThan(relPos);
        }
    }
}
