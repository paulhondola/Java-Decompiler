using CSharpAnalyzer.Config;
using CSharpAnalyzer.Model;

namespace CSharpAnalyzer.Formatter;

public interface IUmlFormatter
{
    string Format(IReadOnlyList<TypeInfo> types, DecompileConfig config);
}
