package org.paul.model;

import java.util.List;

/**
 * Immutable snapshot of a single class extracted from a JAR.
 *
 * @param clazz         the loaded Class object
 * @param fields        declared fields (non-synthetic)
 * @param methods       method signatures as "name()" strings (non-synthetic, non-bridge)
 * @param relationships extends/implements/association/dependency edges to other loaded classes
 */
public record ClassInfo(
        Class<?> clazz,
        List<FieldInfo> fields,
        List<String> methods,
        List<Relationship> relationships
) {
}
