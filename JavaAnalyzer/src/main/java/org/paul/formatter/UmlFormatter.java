package org.paul.formatter;

import org.paul.config.DecompileConfig;
import org.paul.model.ClassInfo;

import java.util.List;

public interface UmlFormatter {
    String format(List<ClassInfo> classes, DecompileConfig config);

    /**
     * The lowercase key used to select this formatter via {@code --format}.
     * Derived from the class name by default: {@code PlantUmlFormatter} → {@code "plantuml"}.
     * Override to provide a custom name (e.g. a formatter named {@code GraphVizDotFormatter}
     * could return {@code "dot"}).
     */
    default String name() {
        return getClass().getSimpleName().replace("Formatter", "").toLowerCase();
    }
}
