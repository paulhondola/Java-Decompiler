package org.paul.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DecompileConfigTest {

    @Nested
    @DisplayName("defaults()")
    class Defaults {

        private final DecompileConfig config = DecompileConfig.defaults();

        @Test
        @DisplayName("returns empty ignore pattern list")
        void defaults_returnsEmptyIgnorePatterns() {
            assertThat(config.ignorePatterns()).isEmpty();
        }

        @Test
        @DisplayName("uses simple (non-fully-qualified) class names")
        void defaults_notFullyQualified() {
            assertThat(config.fullyQualified()).isFalse();
        }

        @Test
        @DisplayName("shows method names")
        void defaults_showMethodsEnabled() {
            assertThat(config.showMethods()).isTrue();
        }

        @Test
        @DisplayName("shows field attributes")
        void defaults_showAttributesEnabled() {
            assertThat(config.showAttributes()).isTrue();
        }

        @Test
        @DisplayName("ignore pattern list is unmodifiable")
        void defaults_ignorePatternListIsUnmodifiable() {
            assertThat(config.ignorePatterns())
                    .isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("custom config")
    class CustomConfig {

        @Test
        @DisplayName("stores provided ignore patterns")
        void customConfig_storesIgnorePatterns() {
            var config = new DecompileConfig(List.of("java.lang.*", "java.util.*"), false, true, true);
            assertThat(config.ignorePatterns()).containsExactly("java.lang.*", "java.util.*");
        }

        @Test
        @DisplayName("fullyQualified flag is preserved")
        void customConfig_preservesFullyQualifiedFlag() {
            var config = new DecompileConfig(List.of(), true, true, true);
            assertThat(config.fullyQualified()).isTrue();
        }

        @Test
        @DisplayName("showMethods false disables method output")
        void customConfig_showMethodsFalse() {
            var config = new DecompileConfig(List.of(), false, false, true);
            assertThat(config.showMethods()).isFalse();
        }

        @Test
        @DisplayName("showAttributes false disables field output")
        void customConfig_showAttributesFalse() {
            var config = new DecompileConfig(List.of(), false, true, false);
            assertThat(config.showAttributes()).isFalse();
        }
    }
}
