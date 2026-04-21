package org.paul.introspection;

import org.paul.config.DecompileConfig;
import org.paul.model.ClassInfo;
import org.paul.model.FieldInfo;
import org.paul.model.Relationship;

import java.lang.reflect.*;
import java.util.*;

/**
 * Extracts a {@link ClassInfo} from a single {@link Class} using {@code java.lang.reflect}.
 *
 * <p>Relationship discovery order (preserved in the output list):
 * <ol>
 *   <li>Associations — from declared fields whose type (or type parameter) is in the loaded set</li>
 *   <li>Extends — declared superclass (if not Object and if in the loaded set)</li>
 *   <li>Implements — declared interfaces that are in the loaded set</li>
 * </ol>
 *
 * <p>Method-level dependency detection is intentionally omitted: test fixtures show only
 * field-based associations and class-hierarchy relationships.
 */
public class ClassInspector {

    public static ClassInfo inspect(Class<?> clazz, DecompileConfig config, Set<String> loadedClassNames) {
        List<FieldInfo> fields = new ArrayList<>();
        // LinkedHashSet preserves first-seen order and deduplicates
        LinkedHashSet<String> associationTargets = new LinkedHashSet<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isSynthetic()) continue;

            char accessMod = accessModChar(field.getModifiers());
            String typeName = getTypeName(field.getGenericType(), config);
            fields.add(new FieldInfo(field.getName(), typeName, accessMod));

            collectTargets(field.getGenericType(), loadedClassNames, config, associationTargets);
        }

        List<String> methodNames = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge()) continue;
            methodNames.add(method.getName() + "()");
        }

        Collections.sort(methodNames);

        List<Relationship> relationships = new ArrayList<>();

        // 1. Associations (from fields) — order follows getDeclaredFields()
        for (String target : associationTargets) {
            relationships.add(new Relationship.Association(target));
        }

        // 2. Extends (superclass, if in loaded set and not Object)
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && !superclass.equals(Object.class)) {
            String targetName = classDisplayName(superclass, config);
            if (loadedClassNames.contains(targetName)) {
                relationships.add(new Relationship.Extends(targetName));
            }
        }

        // 3. Implements (interfaces, if in loaded set)
        for (Class<?> iface : clazz.getInterfaces()) {
            String targetName = classDisplayName(iface, config);
            if (loadedClassNames.contains(targetName)) {
                relationships.add(new Relationship.Implements(targetName));
            }
        }

        return new ClassInfo(
                clazz,
                List.copyOf(fields),
                List.copyOf(methodNames),
                List.copyOf(relationships)
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static char accessModChar(int modifiers) {
        if (Modifier.isPublic(modifiers)) return '+';
        if (Modifier.isProtected(modifiers)) return '#';
        if (Modifier.isPrivate(modifiers)) return '-';
        return '~';
    }

    /**
     * Returns the display name for a class according to the config's fullyQualified setting.
     */
    private static String classDisplayName(Class<?> c, DecompileConfig config) {
        return config.fullyQualified() ? c.getName() : c.getSimpleName();
    }

    /**
     * Builds the display string for a generic type, e.g. {@code "ArrayList of Observer"}
     * or {@code "Class of ?"}.
     */
    static String getTypeName(Type type, DecompileConfig config) {
        return switch (type) {
            case ParameterizedType pt -> {
                String rawName = classDisplayName((Class<?>) pt.getRawType(), config);
                String args = Arrays.stream(pt.getActualTypeArguments())
                        .map(t -> typeArgDisplayName(t, config))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                yield rawName + " of " + args;
            }
            case Class<?> c -> {
                if (c.isArray()) yield getTypeName(c.getComponentType(), config) + "[]";
                yield classDisplayName(c, config);
            }
            case WildcardType ignored -> "?";
            case GenericArrayType at -> getTypeName(at.getGenericComponentType(), config) + "[]";
            case TypeVariable<?> tv -> tv.getName();
            default -> type.getTypeName();
        };
    }

    private static String typeArgDisplayName(Type type, DecompileConfig config) {
        return switch (type) {
            case WildcardType ignored -> "?";
            case Class<?> c -> classDisplayName(c, config);
            case ParameterizedType pt -> classDisplayName((Class<?>) pt.getRawType(), config);
            case TypeVariable<?> tv -> tv.getName();
            default -> "?";
        };
    }

    /**
     * Walks a generic type and adds any class names that appear in {@code loadedClassNames}
     * to {@code targets} (in encounter order).
     */
    private static void collectTargets(Type type, Set<String> loadedClassNames,
                                       DecompileConfig config, LinkedHashSet<String> targets) {
        switch (type) {
            case ParameterizedType pt -> {
                // Raw type might itself be a loaded class
                String rawName = classDisplayName((Class<?>) pt.getRawType(), config);
                if (loadedClassNames.contains(rawName)) targets.add(rawName);
                // Recurse into type arguments
                for (Type arg : pt.getActualTypeArguments()) {
                    collectTargets(arg, loadedClassNames, config, targets);
                }
            }
            case Class<?> c -> {
                if (!c.isPrimitive() && !c.isArray()) {
                    String name = classDisplayName(c, config);
                    if (loadedClassNames.contains(name)) targets.add(name);
                }
            }
            case WildcardType wt -> {
                for (Type bound : wt.getUpperBounds()) {
                    collectTargets(bound, loadedClassNames, config, targets);
                }
            }
            default -> { /* TypeVariable, GenericArrayType — ignore for relationship purposes */ }
        }
    }
}
