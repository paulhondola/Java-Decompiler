namespace CSharpDecompiler.Model;

/// <summary>Immutable snapshot of a single type extracted from an assembly.</summary>
/// <param name="Type">The reflected Type object.</param>
/// <param name="Fields">Declared fields (non-compiler-generated).</param>
/// <param name="Methods">Method signatures as "Name()" strings (non-compiler-generated).</param>
/// <param name="Relationships">Extends/implements/association edges to other loaded types.</param>
public sealed record TypeInfo(
    Type Type,
    IReadOnlyList<FieldInfo> Fields,
    IReadOnlyList<string> Methods,
    IReadOnlyList<Relationship> Relationships
);
