using CSharpAnalyzer.Config;
using CSharpAnalyzer.Filter;
using CSharpAnalyzer.Formatter;
using CSharpAnalyzer.Introspection;
using CSharpAnalyzer.Loader;

namespace CSharpAnalyzer;

public static class Program
{
    public static int Main(string[] args)
    {
        string? assemblyPath = null;
        string format = "plantuml";
        string? output = null;
        var ignore = new List<string>();

        for (int i = 0; i < args.Length; i++)
        {
            switch (args[i])
            {
                case "--format" when i + 1 < args.Length:
                    format = args[++i];
                    break;
                case "--output" when i + 1 < args.Length:
                    output = args[++i];
                    break;
                case "--ignore" when i + 1 < args.Length:
                    ignore.Add(args[++i]);
                    break;
                default:
                    if (!args[i].StartsWith("--"))
                        assemblyPath = args[i];
                    break;
            }
        }

        if (assemblyPath is null)
        {
            Console.Error.WriteLine(
                "Usage: CSharpAnalyzer <assemblyPath> --format <yuml|plantuml> [--output <path>] [--ignore <pattern>]...");
            return 1;
        }

        IUmlFormatter formatter = format.ToLowerInvariant() switch
        {
            "plantuml" => new PlantUmlFormatter(),
            "yuml" => new YumlFormatter(),
            _ => throw new ArgumentException($"Unknown format '{format}'. Use 'yuml' or 'plantuml'.")
        };

        var config = new DecompileConfig(ignore.AsReadOnly(), false, true, true);
        var result = Decompile(assemblyPath, formatter, config);

        if (output is not null)
            File.WriteAllText(output, result);
        else
            Console.Write(result);

        return 0;
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
