package org.paul;

import org.paul.config.DecompileConfig;
import org.paul.filter.ClassFilter;
import org.paul.formatter.UmlFormatter;
import org.paul.introspection.ClassInspector;
import org.paul.loader.JarLoader;
import org.paul.model.ClassInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Main {

    static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Main <jar> --format <yuml|plantuml> [options]");
            System.exit(1);
        }
    }

    /**
     * Runs the full pipeline: load → filter → inspect → format.
     * Uses default config (no ignores, simple names, show methods + attributes).
     */
    public static String decompile(String jarPath, UmlFormatter formatter) {
        DecompileConfig config = DecompileConfig.defaults();

        List<Class<?>> rawClasses = JarLoader.load(jarPath);
        List<Class<?>> filtered = ClassFilter.filter(rawClasses, config);

        Set<String> loadedNames = new LinkedHashSet<>();
        for (Class<?> c : filtered) {
            loadedNames.add(config.fullyQualified() ? c.getName() : c.getSimpleName());
        }

        List<ClassInfo> classInfos = filtered.stream()
                .map(c -> ClassInspector.inspect(c, config, loadedNames))
                .toList();

        return formatter.format(classInfos, config);
    }
}
