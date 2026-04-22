using System.Text;
using CSharpAnalyzer.Config;
using CSharpAnalyzer.Model;

namespace CSharpAnalyzer.Formatter;

/// <summary>
/// Produces yUML class diagram output (Classes mode only — Simple mode was removed).
/// Format per type: [Name|fields|methods] followed immediately by relationship lines.
/// Mirrors YumlFormatter.java (Classes mode).
/// </summary>
public sealed class YumlFormatter : IUmlFormatter
{
    public string Format(IReadOnlyList<TypeInfo> types, DecompileConfig config)
    {
        if (types.Count == 0)
            return "";

        var sb = new StringBuilder();

        foreach (var info in types)
        {
            var name = StripArity(info.Type.Name);

            // Fields section: "- name:type" entries joined by ";"
            var fieldsSection =
                config.ShowAttributes && info.Fields.Count > 0
                    ? string.Join(
                        ";",
                        info.Fields.Select(f => $"{f.AccessModifier} {f.Name}:{f.TypeName}")
                    )
                    : "";

            // Methods section: "Method()" entries joined by ";"
            var methodsSection =
                config.ShowMethods && info.Methods.Count > 0 ? string.Join(";", info.Methods) : "";

            sb.Append($"[{name}|{fieldsSection}|{methodsSection}]\n");

            // Relationship lines appear directly after the type line
            foreach (var rel in info.Relationships)
            {
                var line = rel switch
                {
                    Relationship.Extends e => $"[{e.TargetName}]^-[{name}]",
                    Relationship.Implements i => $"[{i.TargetName}]^-.-[{name}]",
                    Relationship.Association a => $"[{name}]->[{a.TargetName}]",
                    Relationship.Dependency d => $"[{name}]->[{d.TargetName}]",
                    _ => throw new InvalidOperationException(
                        $"Unknown relationship type: {rel.GetType().Name}"
                    ),
                };
                sb.Append($"{line}\n");
            }
        }

        return sb.ToString();
    }

    private static string StripArity(string name)
    {
        int i = name.IndexOf('`');
        return i >= 0 ? name[..i] : name;
    }
}
