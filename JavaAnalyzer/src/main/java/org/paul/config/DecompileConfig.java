package org.paul.config;

import java.util.List;

/**
 * Immutable configuration for a decompile run.
 *
 * @param ignorePatterns class name patterns to exclude (e.g. "java.lang.*")
 * @param fullyQualified show fully-qualified class names
 * @param showMethods    include method names in the diagram
 * @param showAttributes include field names in the diagram
 */
public record DecompileConfig(
        List<String> ignorePatterns,
        boolean fullyQualified,
        boolean showMethods,
        boolean showAttributes
) {
    public static DecompileConfig defaults() {
        return new DecompileConfig(List.of(), false, true, true);
    }
}
