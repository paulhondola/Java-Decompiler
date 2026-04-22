using CSharpAnalyzer.Config;
using CSharpAnalyzer.Formatter;
using CSharpAnalyzer.Model;
using FluentAssertions;
using FieldInfo = CSharpAnalyzer.Model.FieldInfo;

namespace CSharpAnalyzer.Tests.Formatter;

public class YumlFormatterTests
{
    private static readonly YumlFormatter Formatter = new();
    private static readonly DecompileConfig DefaultConfig = DecompileConfig.Defaults();
    private static readonly DecompileConfig NoFields = new([], false, true, false);
    private static readonly DecompileConfig NoMethods = new([], false, false, true);

    private static TypeInfo MakeType(
        Type type,
        IReadOnlyList<FieldInfo>? fields = null,
        IReadOnlyList<string>? methods = null,
        IReadOnlyList<Relationship>? relationships = null)
        => new(type, fields ?? [], methods ?? [], relationships ?? []);

    // ── Format ────────────────────────────────────────────────────────────────
    [Fact]
    public void Format_EmptyList_ReturnsEmptyString()
        => Formatter.Format([], DefaultConfig).Should().Be("");

    [Fact]
    public void Format_SingleType_RendersName()
    {
        var result = Formatter.Format([MakeType(typeof(IDisposable))], DefaultConfig);
        result.Should().Contain("[IDisposable||]\n");
    }

    [Fact]
    public void Format_TypeWithMembers_ShowsMembers()
    {
        var fields = new[] { new FieldInfo("count", "Int32", '-') };
        var result = Formatter.Format([MakeType(typeof(IDisposable), fields, ["Run()"])], DefaultConfig);
        result.Should().Contain("[IDisposable|- count:Int32|Run()]\n");
    }

    [Fact]
    public void Format_ShowAttributesFalse_FieldsOmitted()
    {
        var fields = new[] { new FieldInfo("count", "Int32", '-') };
        var result = Formatter.Format([MakeType(typeof(IDisposable), fields, ["Run()"])], NoFields);
        result.Should().Contain("[IDisposable||Run()]\n");
    }

    [Fact]
    public void Format_ShowMethodsFalse_MethodsOmitted()
    {
        var fields = new[] { new FieldInfo("count", "Int32", '-') };
        var result = Formatter.Format([MakeType(typeof(IDisposable), fields, ["Run()"])], NoMethods);
        result.Should().Contain("[IDisposable|- count:Int32|]\n");
    }

    [Fact]
    public void Format_MultipleFields_SemicolonSeparated()
    {
        var fields = new[] { new FieldInfo("count", "Int32", '-'), new FieldInfo("name", "String", '+') };
        var result = Formatter.Format([MakeType(typeof(IDisposable), fields)], DefaultConfig);
        result.Should().Contain("[IDisposable|- count:Int32;+ name:String|]\n");
    }

    // ── Relationships ─────────────────────────────────────────────────────────
    [Fact]
    public void Format_Implements_RendersWithDottedArrow()
    {
        var info = MakeType(typeof(IDisposable), relationships: [new Relationship.Implements("IBase")]);
        var result = Formatter.Format([info], DefaultConfig);
        result.Should().Contain("[IBase]^-.-[IDisposable]\n");
    }

    [Fact]
    public void Format_Extends_RendersWithSolidArrow()
    {
        var info = MakeType(typeof(IDisposable), relationships: [new Relationship.Extends("Base")]);
        var result = Formatter.Format([info], DefaultConfig);
        result.Should().Contain("[Base]^-[IDisposable]\n");
    }

    [Fact]
    public void Format_Association_RendersWithForwardArrow()
    {
        var info = MakeType(typeof(IDisposable), relationships: [new Relationship.Association("Widget")]);
        var result = Formatter.Format([info], DefaultConfig);
        result.Should().Contain("[IDisposable]->[Widget]\n");
    }

    [Fact]
    public void Format_Dependency_RendersLikeAssociation()
    {
        var info = MakeType(typeof(IDisposable), relationships: [new Relationship.Dependency("Service")]);
        var result = Formatter.Format([info], DefaultConfig);
        result.Should().Contain("[IDisposable]->[Service]\n");
    }

    [Fact]
    public void Format_MultipleTypes_EachOnOwnLine()
    {
        var result = Formatter.Format([MakeType(typeof(IDisposable)), MakeType(typeof(IComparable))], DefaultConfig);
        result.Should().Contain("[IDisposable||]\n");
        result.Should().Contain("[IComparable||]\n");
    }

    [Fact]
    public void Format_RelationshipAppearsAfterTypeLine()
    {
        var info = MakeType(typeof(IDisposable), relationships: [new Relationship.Extends("Base")]);
        var result = Formatter.Format([info], DefaultConfig);
        var typeIdx = result.IndexOf("[IDisposable||]", StringComparison.Ordinal);
        var relIdx = result.IndexOf("[Base]^-[IDisposable]", StringComparison.Ordinal);
        typeIdx.Should().BeLessThan(relIdx);
    }
}
