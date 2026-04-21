package org.paul.filter;

import org.paul.config.DecompileConfig;

import java.util.List;

/**
 * Removes classes whose fully-qualified name matches any pattern in the ignore list.
 * Patterns support a trailing {@code .*} wildcard (e.g. {@code java.lang.*}).
 */
public class ClassFilter {

    public static List<Class<?>> filter(List<Class<?>> classes, DecompileConfig config) {
        if (config.ignorePatterns().isEmpty()) return classes;

        return classes.stream()
                .filter(c -> !isIgnored(c, config))
                .toList();
    }

    private static boolean isIgnored(Class<?> clazz, DecompileConfig config) {
        String fqn = clazz.getName();
        for (String pattern : config.ignorePatterns()) {
            if (pattern.endsWith(".*")) {
                String pkg = pattern.substring(0, pattern.length() - 2);
                if (fqn.startsWith(pkg + ".") || fqn.equals(pkg)) return true;
            } else {
                if (fqn.equals(pattern)) return true;
            }
        }
        return false;
    }
}
