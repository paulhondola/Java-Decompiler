package org.paul.model;

/**
 * Represents a single field extracted from a class.
 *
 * @param name           field name
 * @param typeName       display string for the field type (e.g. "ArrayList of Observer")
 * @param accessModifier UML access char: '-' private, '#' protected, '+' public, '~' package
 */
public record FieldInfo(String name, String typeName, char accessModifier) {}
