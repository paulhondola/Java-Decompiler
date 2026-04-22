package org.paul.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.paul.config.DecompileConfig;

import java.io.File;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClassFilterTest {

    @Nested
    @DisplayName("no ignore patterns")
    class NoPatterns {

        @Test
        @DisplayName("returns all classes unchanged")
        void filter_noPatterns_returnsAllClasses() {
            var config = DecompileConfig.defaults(); // empty ignore list
            List<Class<?>> classes = List.of(String.class, Integer.class, File.class);

            assertThat(ClassFilter.filter(classes, config))
                    .containsExactlyElementsOf(classes);
        }

        @Test
        @DisplayName("empty input returns empty list")
        void filter_noPatterns_emptyInput_returnsEmpty() {
            assertThat(ClassFilter.filter(List.of(), DecompileConfig.defaults())).isEmpty();
        }
    }

    @Nested
    @DisplayName("exact-match pattern")
    class ExactMatch {

        @Test
        @DisplayName("excludes the matched class")
        void filter_exactPattern_excludesClass() {
            var config = new DecompileConfig(List.of("java.lang.String"), false, true, true);

            assertThat(ClassFilter.filter(List.of(String.class, Integer.class), config))
                    .containsExactly(Integer.class);
        }

        @Test
        @DisplayName("keeps all classes when no name matches")
        void filter_exactPattern_noMatch_keepsAll() {
            var config = new DecompileConfig(List.of("com.example.Foo"), false, true, true);
            List<Class<?>> classes = List.of(String.class, Integer.class);

            assertThat(ClassFilter.filter(classes, config))
                    .containsExactlyElementsOf(classes);
        }
    }

    @Nested
    @DisplayName("wildcard package pattern (.*)")
    class WildcardPattern {

        @Test
        @DisplayName("excludes all classes in the package")
        void filter_wildcardPattern_excludesPackage() {
            var config = new DecompileConfig(List.of("java.lang.*"), false, true, true);

            assertThat(ClassFilter.filter(List.of(String.class, Integer.class, File.class), config))
                    .containsExactly(File.class); // File is java.io, not java.lang
        }

        @Test
        @DisplayName("does not exclude classes from a sub-package of a different name")
        void filter_wildcardPattern_doesNotMatchUnrelatedPackage() {
            var config = new DecompileConfig(List.of("java.lang.*"), false, true, true);

            assertThat(ClassFilter.filter(List.of(java.util.List.class), config))
                    .containsExactly(java.util.List.class);
        }

        @Test
        @DisplayName("multiple patterns exclude all matches")
        void filter_multiplePatterns_excludesAllMatching() {
            var config = new DecompileConfig(List.of("java.lang.*", "java.io.*"), false, true, true);

            assertThat(ClassFilter.filter(
                    List.of(String.class, File.class, java.util.ArrayList.class), config))
                    .containsExactly(java.util.ArrayList.class);
        }
    }
}
