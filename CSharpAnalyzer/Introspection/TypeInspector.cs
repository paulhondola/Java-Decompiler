using System.Reflection;
using System.Runtime.CompilerServices;
using CSharpAnalyzer.Config;
using MFI = CSharpAnalyzer.Model.FieldInfo;
using MRel = CSharpAnalyzer.Model.Relationship;
using MTI = CSharpAnalyzer.Model.TypeInfo;
using SFI = System.Reflection.FieldInfo;

namespace CSharpAnalyzer.Introspection;

/// <summary>
/// Extracts a <see cref="MTI"/> from a single <see cref="Type"/> using System.Reflection.
/// Mirrors ClassInspector.java — see docs/csharp-implementation-plan.md §3.3 for divergences.
/// </summary>
public static class TypeInspector
{
    private static readonly BindingFlags DeclaredFlags =
        BindingFlags.DeclaredOnly
        | BindingFlags.Public
        | BindingFlags.NonPublic
        | BindingFlags.Instance
        | BindingFlags.Static;

    public static MTI Inspect(
        Type type,
        DecompileConfig config,
        IReadOnlySet<string> loadedTypeNames
    )
    {
        var fields = ExtractFields(type, config);
        var methods = ExtractMethods(type, config);
        var relationships = ExtractRelationships(type, config, loadedTypeNames);
        return new MTI(type, fields, methods, relationships);
    }

    /// <summary>
    /// Returns the display name of a type for field/method type annotations.
    /// Generic types use "List of Widget" syntax; arrays use "T[]".
    /// </summary>
    public static string GetTypeName(Type type, DecompileConfig config)
    {
        if (type.IsGenericType)
        {
            var rawName = StripArity(type.GetGenericTypeDefinition().Name);
            var args = type.GetGenericArguments().Select(a => GetTypeArgDisplayName(a, config));
            return $"{rawName} of {string.Join(", ", args)}";
        }

        if (type.IsArray)
            return $"{GetTypeName(type.GetElementType()!, config)}[]";

        return TypeDisplayName(type, config);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private static char AccessModChar(SFI field)
    {
        if (field.IsPublic)
            return '+';
        if (field.IsFamily || field.IsFamilyOrAssembly)
            return '#';
        if (field.IsAssembly)
            return '~';
        return '-';
    }

    private static string TypeDisplayName(Type type, DecompileConfig config) =>
        config.FullyQualified ? StripArity(type.FullName ?? type.Name) : StripArity(type.Name);

    private static string GetTypeArgDisplayName(Type type, DecompileConfig config)
    {
        if (type.IsGenericParameter)
            return type.Name; // T, TKey, etc.
        if (type.IsGenericType)
            return StripArity(type.GetGenericTypeDefinition().Name);
        return TypeDisplayName(type, config);
    }

    private static string StripArity(string name)
    {
        int i = name.IndexOf('`');
        return i >= 0 ? name[..i] : name;
    }

    private static IReadOnlyList<MFI> ExtractFields(Type type, DecompileConfig config) =>
        type.GetFields(DeclaredFlags)
            .Where(f =>
                !f.Name.StartsWith('<') && !f.IsDefined(typeof(CompilerGeneratedAttribute), false)
            )
            .Select(f => new MFI(f.Name, GetTypeName(f.FieldType, config), AccessModChar(f)))
            .ToList()
            .AsReadOnly();

    private static IReadOnlyList<string> ExtractMethods(Type type, DecompileConfig config) =>
        type.GetMethods(DeclaredFlags)
            .Where(m => !m.IsSpecialName && !m.IsDefined(typeof(CompilerGeneratedAttribute), false))
            .Select(m => m.Name + "()")
            .OrderBy(name => name, StringComparer.Ordinal)
            .ToList()
            .AsReadOnly();

    private static IReadOnlyList<MRel> ExtractRelationships(
        Type type,
        DecompileConfig config,
        IReadOnlySet<string> loadedTypeNames
    )
    {
        var relationships = new List<MRel>();

        // Field-based associations — insertion-ordered, deduplicated (LinkedHashSet semantics)
        var seen = new HashSet<string>();
        var associationTargets = new List<string>();

        foreach (
            var field in type.GetFields(DeclaredFlags)
                .Where(f =>
                    !f.Name.StartsWith('<')
                    && !f.IsDefined(typeof(CompilerGeneratedAttribute), false)
                )
        )
        {
            CollectTargets(field.FieldType, loadedTypeNames, config, associationTargets, seen);
        }

        foreach (var target in associationTargets)
            relationships.Add(new MRel.Association(target));

        // Extends — skip object root (mirrors Java skipping Object.class)
        if (type.BaseType != null && type.BaseType != typeof(object))
        {
            var targetName = TypeDisplayName(type.BaseType, config);
            if (loadedTypeNames.Contains(targetName))
                relationships.Add(new MRel.Extends(targetName));
        }

        // Implements — directly declared interfaces only (§2.1: subtract base type's interfaces,
        // then remove any iface already implied by another iface in the remaining set)
        var directIfaces = type.GetInterfaces();
        if (type.BaseType != null)
            directIfaces = directIfaces.Except(type.BaseType.GetInterfaces()).ToArray();
        directIfaces = directIfaces
            .Where(iface =>
                !directIfaces.Any(other =>
                    other != iface && other.GetInterfaces().Contains(iface)
                )
            )
            .ToArray();

        foreach (var iface in directIfaces)
        {
            var targetName = TypeDisplayName(iface, config);
            if (loadedTypeNames.Contains(targetName))
                relationships.Add(new MRel.Implements(targetName));
        }

        return relationships.AsReadOnly();
    }

    /// <summary>
    /// Recursively walks a field type, adding any loaded user-type names to
    /// <paramref name="order"/> in encounter order (deduplication via <paramref name="seen"/>).
    /// Generic containers are skipped — only their type arguments are checked.
    /// </summary>
    private static void CollectTargets(
        Type type,
        IReadOnlySet<string> loaded,
        DecompileConfig config,
        List<string> order,
        HashSet<string> seen
    )
    {
        if (type.IsGenericType)
        {
            foreach (var arg in type.GetGenericArguments())
                CollectTargets(arg, loaded, config, order, seen);
        }
        else if (type.IsArray)
        {
            CollectTargets(type.GetElementType()!, loaded, config, order, seen);
        }
        else
        {
            var name = TypeDisplayName(type, config);
            if (loaded.Contains(name) && seen.Add(name))
                order.Add(name);
        }
    }
}
