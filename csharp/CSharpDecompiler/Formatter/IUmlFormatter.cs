using CSharpDecompiler.Config;
using CSharpDecompiler.Model;

namespace CSharpDecompiler.Formatter;

public interface IUmlFormatter
{
    string Format(IReadOnlyList<TypeInfo> types, DecompileConfig config);
}
