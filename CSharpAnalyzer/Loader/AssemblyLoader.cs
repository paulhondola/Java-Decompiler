using System.Reflection;
using System.Runtime.CompilerServices;

namespace CSharpAnalyzer.Loader;

/// <summary>
/// Opens a .dll assembly, loads every non-compiler-generated top-level type,
/// and returns them in metadata order — mirroring JarLoader.java.
/// </summary>
public static class AssemblyLoader
{
    public static IReadOnlyList<Type> Load(string assemblyPath)
    {
        var fullPath = Path.GetFullPath(assemblyPath);

        if (!File.Exists(fullPath))
            throw new InvalidOperationException($"Failed to load assembly: {assemblyPath}");

        Assembly assembly;
        try
        {
            assembly = Assembly.LoadFrom(fullPath);
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException($"Failed to load assembly: {assemblyPath}", ex);
        }

        Type[] allTypes;
        try
        {
            allTypes = assembly.GetTypes();
        }
        catch (ReflectionTypeLoadException ex)
        {
            allTypes = ex.Types.Where(t => t != null).ToArray()!;
        }

        return allTypes.Where(t => !t.IsNested && !IsCompilerGenerated(t)).ToList().AsReadOnly();
    }

    private static bool IsCompilerGenerated(Type t) =>
        t.IsDefined(typeof(CompilerGeneratedAttribute), false) || t.Name.Contains('<');
}
