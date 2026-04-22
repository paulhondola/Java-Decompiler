using CSharpAnalyzer.Config;

namespace CSharpAnalyzer.Filter;

/// <summary>
/// Removes types whose fully-qualified name matches any pattern in the ignore list.
/// Patterns support a trailing .* wildcard (e.g. "System.Collections.*").
/// Mirrors ClassFilter.java exactly.
/// </summary>
public static class TypeFilter
{
    public static IReadOnlyList<Type> Filter(IReadOnlyList<Type> types, DecompileConfig config)
    {
        if (config.IgnorePatterns.Count == 0)
            return types;
        return types.Where(t => !IsIgnored(t, config)).ToList().AsReadOnly();
    }

    private static bool IsIgnored(Type type, DecompileConfig config)
    {
        var fqn = type.FullName ?? type.Name;
        return config.IgnorePatterns.Any(pattern =>
        {
            if (pattern.EndsWith(".*"))
            {
                var pkg = pattern[..^2]; // strip ".*"
                return fqn.StartsWith(pkg + ".") || fqn == pkg;
            }
            return fqn == pattern;
        });
    }
}
