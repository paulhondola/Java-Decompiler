package org.paul;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventNotifierTest {

    private static final String JAR = "src/test/java/eventnotifier/EventNotifier.jar";

    @Test
    void testSimpleYuml() throws IOException {
        String expected = Files.readString(Paths.get("src/test/java/eventnotifier/EventNotifier-simple-yuml.txt"));
        String actual = Main.decompile(JAR, new YumlFormatter(YumlFormatter.Mode.SIMPLE));
        assertEquals(expected, actual);
    }

    @Test
    void testClassesYuml() throws IOException {
        String expected = Files.readString(Paths.get("src/test/java/eventnotifier/EventNotifier-classes-yuml.txt"));
        String actual = Main.decompile(JAR, new YumlFormatter(YumlFormatter.Mode.CLASSES));
        assertEquals(expected, actual);
    }
}
