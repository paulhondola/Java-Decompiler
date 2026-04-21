namespace CSharpDecompiler.Model;

/// <summary>Represents a single field extracted from a type.</summary>
/// <param name="Name">Field name.</param>
/// <param name="TypeName">Display string for the field type (e.g. "List of Observer").</param>
/// <param name="AccessModifier">UML access char: '-' private, '#' protected, '+' public, '~' internal.</param>
public sealed record FieldInfo(string Name, string TypeName, char AccessModifier);
