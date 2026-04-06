package org.paul.e2e;

import org.junit.jupiter.api.Test;
import org.paul.Main;
import org.paul.formatter.PlantUmlFormatter;
import org.paul.formatter.YumlFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TempSensorTest {

    private static final String JAR = "src/test/java/tempsensor/TempSensor.jar";

    @Test
    void testYuml() throws IOException {
        String expected = Files.readString(Paths.get("src/test/java/tempsensor/TempSensor-yuml.txt"));
        String actual = Main.decompile(JAR, new YumlFormatter(YumlFormatter.Mode.CLASSES));
        assertEquals(expected, actual);
    }

    @Test
    void testPlantuml() throws IOException {
        String expected = Files.readString(Paths.get("src/test/java/tempsensor/TempSensor-plantuml.txt"));
        String actual = Main.decompile(JAR, new PlantUmlFormatter());
        assertEquals(expected, actual);
    }
}
