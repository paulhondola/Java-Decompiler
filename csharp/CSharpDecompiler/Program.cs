using CSharpDecompiler.Config;
using CSharpDecompiler.Filter;
using CSharpDecompiler.Formatter;
using CSharpDecompiler.Introspection;
using CSharpDecompiler.Loader;

namespace CSharpDecompiler;

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
