using CSharpAnalyzer.Config;

namespace CSharpAnalyzer.Filter;

/// <summary>
/// Removes types whose fully-qualified name matches any pattern in the ignore list.
/// Patterns support a trailing .* wildcard (e.g. "System.Collections.*").
/// </summary>
public static class TypeFilter
{
    public static IReadOnlyList<Type> Filter(IReadOnlyList<Type> types, DecompileConfig config)
    {
        throw new NotImplementedException();
    }
}
