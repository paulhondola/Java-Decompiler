package org.paul.e2e;

import org.junit.jupiter.api.Test;
import org.paul.Main;
import org.paul.config.DecompileConfig;
import org.paul.formatter.YumlFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventNotifierTest {

    private static final String JAR = "src/test/java/eventnotifier/EventNotifier.jar";

    @Test
    void testYuml() throws IOException {
        String expected = Files.readString(Paths.get("src/test/java/eventnotifier/EventNotifier.yuml"));
        String actual = Main.decompile(JAR, new YumlFormatter(), DecompileConfig.defaults());
        assertEquals(expected, actual);
    }
}
