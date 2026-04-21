namespace CSharpDecompiler.Loader;

/// <summary>
/// Opens a .dll assembly, loads every non-compiler-generated type it contains,
/// and returns them in metadata order.
/// </summary>
public static class AssemblyLoader
{
    public static IReadOnlyList<Type> Load(string assemblyPath)
    {
        throw new NotImplementedException();
    }
}
