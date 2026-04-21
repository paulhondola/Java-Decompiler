using CSharpDecompiler.Config;
using CSharpDecompiler.Model;

namespace CSharpDecompiler.Formatter;

public sealed class YumlFormatter(YumlFormatter.Mode mode) : IUmlFormatter
{
    public string Format(IReadOnlyList<TypeInfo> types, DecompileConfig config)
    {
        throw new NotImplementedException();
    }

    public enum Mode
    {
        Simple,
        Classes,
    }
}
