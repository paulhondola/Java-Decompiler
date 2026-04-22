using CSharpAnalyzer.Config;
using CSharpAnalyzer.Model;

namespace CSharpAnalyzer.Introspection;

/// <summary>
/// Extracts a <see cref="TypeInfo"/> from a single <see cref="Type"/> using System.Reflection.
/// </summary>
public static class TypeInspector
{
    public static TypeInfo Inspect(
        Type type,
        DecompileConfig config,
        IReadOnlySet<string> loadedTypeNames
    )
    {
        throw new NotImplementedException();
    }
}
