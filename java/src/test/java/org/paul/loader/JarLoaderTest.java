package org.paul.loader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JarLoaderTest {

    private static final String TEMP_SENSOR_JAR = "src/test/java/tempsensor/TempSensor.jar";
    private static final String EVENT_NOTIFIER_JAR = "src/test/java/eventnotifier/EventNotifier.jar";

    @Nested
    @DisplayName("TempSensor.jar")
    class TempSensor {

        @Test
        @DisplayName("loads exactly 7 classes")
        void load_tempSensorJar_loadsSevenClasses() {
            assertThat(JarLoader.load(TEMP_SENSOR_JAR)).hasSize(7);
        }

        @Test
        @DisplayName("returns all expected simple class names")
        void load_tempSensorJar_containsExpectedClassNames() {
            List<String> names = JarLoader.load(TEMP_SENSOR_JAR).stream()
                    .map(Class::getSimpleName)
                    .toList();

            assertThat(names).containsExactlyInAnyOrder(
                    "AverageDisplay", "MainDriver", "NumericDisplay",
                    "Observer", "Subject", "TemperatureSensor", "TextDisplay");
        }

        @Test
        @DisplayName("returned list preserves JAR-entry order (alphabetical for this JAR)")
        void load_tempSensorJar_entryOrderPreserved() {
            List<String> names = JarLoader.load(TEMP_SENSOR_JAR).stream()
                    .map(Class::getSimpleName)
                    .toList();

            assertThat(names).containsExactly(
                    "AverageDisplay", "MainDriver", "NumericDisplay",
                    "Observer", "Subject", "TemperatureSensor", "TextDisplay");
        }

        @Test
        @DisplayName("no null entries in the result")
        void load_tempSensorJar_noNullEntries() {
            assertThat(JarLoader.load(TEMP_SENSOR_JAR)).doesNotContainNull();
        }

        @Test
        @DisplayName("returns unmodifiable list")
        void load_tempSensorJar_returnsUnmodifiableList() {
            var classes = JarLoader.load(TEMP_SENSOR_JAR);
            assertThat(classes).isUnmodifiable();
        }
    }

    @Nested
    @DisplayName("EventNotifier.jar")
    class EventNotifier {

        @Test
        @DisplayName("loads exactly 16 classes")
        void load_eventNotifierJar_loadsSixteenClasses() {
            assertThat(JarLoader.load(EVENT_NOTIFIER_JAR)).hasSize(16);
        }

        @Test
        @DisplayName("returns all expected simple class names")
        void load_eventNotifierJar_containsExpectedClassNames() {
            List<String> names = JarLoader.load(EVENT_NOTIFIER_JAR).stream()
                    .map(Class::getSimpleName)
                    .toList();

            assertThat(names).containsExactlyInAnyOrder(
                    "Event", "EventService", "Filter", "InvalidEventTypeException",
                    "Subscriber", "Subscription", "CriticalFaultFilter", "FaultEvent",
                    "ManagementEvent", "StatusEvent", "Main", "Hub", "Router",
                    "ErrorLogger", "PagingSystem", "StatusConsole");
        }

        @Test
        @DisplayName("no synthetic or anonymous classes are included")
        void load_eventNotifierJar_noAnonymousClasses() {
            assertThat(JarLoader.load(EVENT_NOTIFIER_JAR))
                    .noneMatch(Class::isAnonymousClass)
                    .noneMatch(Class::isSynthetic);
        }
    }

    @Nested
    @DisplayName("error handling")
    class ErrorHandling {

        @Test
        @DisplayName("non-existent path throws RuntimeException with descriptive message")
        void load_nonExistentPath_throwsRuntimeException() {
            assertThatThrownBy(() -> JarLoader.load("does/not/exist.jar"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to load JAR");
        }
    }
}
