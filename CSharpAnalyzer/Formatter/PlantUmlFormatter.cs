using System.Text;
using CSharpAnalyzer.Config;
using CSharpAnalyzer.Model;

namespace CSharpAnalyzer.Formatter;

/// <summary>
/// Produces PlantUML class diagram output — identical format to PlantUmlFormatter.java.
/// Two-pass: type blocks first, then relationship lines.
/// </summary>
public sealed class PlantUmlFormatter : IUmlFormatter
{
    public string Format(IReadOnlyList<TypeInfo> types, DecompileConfig config)
    {
        var sb = new StringBuilder();
        sb.Append("@startuml\n\n");

        // Pass 1: type blocks
        foreach (var info in types)
        {
            var keyword = info.Type.IsInterface ? "interface" : "class";
            var name = ResolveName(info.Type, config);

            sb.Append($"{keyword} {name}{{\n");

            if (config.ShowAttributes)
            {
                foreach (var field in info.Fields)
                    sb.Append($"  {field.AccessModifier}{field.Name}:{field.TypeName}\n");
            }

            if (config.ShowMethods)
            {
                foreach (var method in info.Methods)
                    sb.Append($"  {method}\n");
            }

            sb.Append("}\n\n");
        }

        // Pass 2: relationship lines (each followed by a blank line)
        foreach (var info in types)
        {
            var name = ResolveName(info.Type, config);

            foreach (var rel in info.Relationships)
            {
                var line = rel switch
                {
                    Relationship.Extends e => $"{e.TargetName} <|--- {name}",
                    Relationship.Implements i => $"{i.TargetName} <|--- {name}",
                    Relationship.Association a => $"{name} ---> {a.TargetName}",
                    Relationship.Dependency d => $"{name} ---> {d.TargetName}",
                    _ => throw new InvalidOperationException(
                        $"Unknown relationship type: {rel.GetType().Name}"
                    ),
                };
                sb.Append($"{line}\n\n");
            }
        }

        sb.Append("@enduml\n");
        return sb.ToString();
    }

    private static string ResolveName(Type type, DecompileConfig config)
    {
        var raw = config.FullyQualified ? (type.FullName ?? type.Name) : type.Name;
        int tick = raw.IndexOf('`');
        return tick >= 0 ? raw[..tick] : raw;
    }
}
