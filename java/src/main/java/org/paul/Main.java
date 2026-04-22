package org.paul;

import org.paul.config.DecompileConfig;
import org.paul.filter.ClassFilter;
import org.paul.formatter.UmlFormatter;
import org.paul.introspection.ClassInspector;
import org.paul.loader.JarLoader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(
        name = "java-decompiler",
        description = "Generate UML diagrams from a JAR file.",
        mixinStandardHelpOptions = true
)
public class Main implements Callable<Integer> {

    /**
     * Formatter registry built once from {@link ServiceLoader}.
     * Keys are lowercase formatter names (e.g. {@code "plantuml"}, {@code "yuml"}).
     * To register a new formatter: implement {@link UmlFormatter} and add its fully-qualified
     * class name to {@code META-INF/services/org.paul.formatter.UmlFormatter}.
     */
    static final Map<String, UmlFormatter> FORMATTERS;

    static {
        Map<String, UmlFormatter> map = new LinkedHashMap<>();
        ServiceLoader.load(UmlFormatter.class).forEach(f -> map.put(f.name(), f));
        FORMATTERS = Collections.unmodifiableMap(map);
    }

    @Option(names = "--ignore", paramLabel = "PATTERN", split = ",",
            description = "Class name patterns to exclude, comma-separated or repeated (e.g. 'java.lang.*').")
    private List<String> ignorePatterns = List.of();
    @Parameters(index = "0", paramLabel = "JAR", description = "Path to the JAR file to analyse.")
    private String jarPath;
    @Option(names = "--format", required = true,
            description = "Output format. Available: plantuml, yuml (case-insensitive).")
    private String format;
    @Option(names = "--output", paramLabel = "FILE",
            description = "Write output to FILE instead of stdout.")
    private Path outputFile;

    static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    /**
     * Runs the full pipeline: load → filter → inspect → format.
     */
    public static String decompile(String jarPath, UmlFormatter formatter, DecompileConfig config) {
        List<Class<?>> filtered = ClassFilter.filter(JarLoader.load(jarPath), config);

        Set<String> loadedNames = filtered.stream()
                .map(c -> config.fullyQualified() ? c.getName() : c.getSimpleName())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return formatter.format(
                filtered.stream()
                        .map(c -> ClassInspector.inspect(c, config, loadedNames))
                        .toList(),
                config);
    }

    @Override
    public Integer call()
     {
        UmlFormatter formatter = FORMATTERS.get(format.toLowerCase());
        if (formatter == null) {
            throw new IllegalArgumentException(
                "Unknown formatter '" + format + "'. Available: " + FORMATTERS.keySet());
        }
        DecompileConfig config = new DecompileConfig(ignorePatterns, false, true, true);
        String result = decompile(jarPath, formatter, config);

        if (outputFile != null) {
            try {
                Files.writeString(outputFile, result);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            System.out.print(result);
        }

        return 0;
    }
}
