namespace CSharpDecompiler.Config;

/// <summary>Immutable configuration for a decompile run.</summary>
/// <param name="IgnorePatterns">Type name patterns to exclude (e.g. "System.Collections.*").</param>
/// <param name="FullyQualified">Show fully-qualified type names.</param>
/// <param name="ShowMethods">Include method names in the diagram.</param>
/// <param name="ShowAttributes">Include field/property names in the diagram.</param>
public sealed record DecompileConfig(
    IReadOnlyList<string> IgnorePatterns,
    bool FullyQualified,
    bool ShowMethods,
    bool ShowAttributes
)
{
    public static DecompileConfig Defaults() => new([], false, true, true);
}
