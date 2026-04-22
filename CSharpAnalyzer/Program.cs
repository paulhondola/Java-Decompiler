using CSharpAnalyzer.Config;
using CSharpAnalyzer.Filter;
using CSharpAnalyzer.Formatter;
using CSharpAnalyzer.Introspection;
using CSharpAnalyzer.Loader;

namespace CSharpAnalyzer;

internal static class Program
{
    public static int Main(string[] args)
    {
        throw new NotImplementedException();
    }

    /// <summary>Runs the full pipeline: load → filter → inspect → format.</summary>
    public static string Decompile(
        string assemblyPath,
        IUmlFormatter formatter,
        DecompileConfig config
    )
    {
        var types = TypeFilter.Filter(AssemblyLoader.Load(assemblyPath), config);

        var loadedNames = types
            .Select(t => config.FullyQualified ? t.FullName! : t.Name)
            .ToHashSet();

        return formatter.Format(
            types.Select(t => TypeInspector.Inspect(t, config, loadedNames)).ToList(),
            config
        );
    }
}
