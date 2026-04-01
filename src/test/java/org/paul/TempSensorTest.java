package org.paul;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TempSensorTest {

    private static final String JAR = "src/test/java/tempsensor/TempSensor.jar";

    @Test
    void testYuml() throws IOException {
        String expected = Files.readString(Paths.get("src/test/java/tempsensor/TempSensor-yuml.txt"));
        String actual = Main.decompile(JAR, new YumlFormatter(YumlFormatter.Mode.SIMPLE));
        assertEquals(expected, actual);
    }

    @Test
    void testPlantuml() throws IOException {
        String expected = Files.readString(Paths.get("src/test/java/tempsensor/TempSensor-plantuml.txt"));
        String actual = Main.decompile(JAR, new PlantUmlFormatter());
        assertEquals(expected, actual);
    }
}
