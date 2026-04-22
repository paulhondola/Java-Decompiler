using CSharpAnalyzer.Config;
using CSharpAnalyzer.Formatter;
using CSharpAnalyzer.Model;
using FluentAssertions;
using FieldInfo = CSharpAnalyzer.Model.FieldInfo;

namespace CSharpAnalyzer.Tests.Formatter;

public class PlantUmlFormatterTests
{
    private static readonly PlantUmlFormatter Formatter = new();
    private static readonly DecompileConfig DefaultConfig = DecompileConfig.Defaults();
    private static readonly DecompileConfig NoFields = new([], false, true, false);
    private static readonly DecompileConfig NoMethods = new([], false, false, true);

    private static TypeInfo MakeType(
        Type type,
        IReadOnlyList<FieldInfo>? fields = null,
        IReadOnlyList<string>? methods = null,
        IReadOnlyList<Relationship>? relationships = null)
        => new(type, fields ?? [], methods ?? [], relationships ?? []);

    // ── Envelope ─────────────────────────────────────────────────────────────
    public class Envelope
    {
        [Fact]
        public void Format_OutputStartsWithStartUml()
            => Formatter.Format([], DefaultConfig).Should().StartWith("@startuml");

        [Fact]
        public void Format_OutputEndsWithEndUml()
            => Formatter.Format([], DefaultConfig).Should().EndWith("@enduml\n");

        [Fact]
        public void Format_EmptyList_ProducesMinimalDocument()
            => Formatter.Format([], DefaultConfig).Should().Be("@startuml\n\n@enduml\n");
    }

    // ── Blocks ────────────────────────────────────────────────────────────────
    public class Blocks
    {
        [Fact]
        public void Format_Class_UsesClassKeyword()
        {
            var result = Formatter.Format([MakeType(typeof(string))], DefaultConfig);
            result.Should().Contain("class String{");
        }

        [Fact]
        public void Format_Interface_UsesInterfaceKeyword()
        {
            var result = Formatter.Format([MakeType(typeof(IDisposable))], DefaultConfig);
            result.Should().Contain("interface IDisposable{");
        }

        [Fact]
        public void Format_ClassBlock_IsClosed()
        {
            var result = Formatter.Format([MakeType(typeof(string))], DefaultConfig);
            result.Should().Contain("class String{\n}\n");
        }

        [Fact]
        public void Format_Field_RendersWithTwoSpaceIndent()
        {
            var fields = new[] { new FieldInfo("count", "Int32", '-') };
            var result = Formatter.Format([MakeType(typeof(string), fields)], DefaultConfig);
            result.Should().Contain("  -count:Int32\n");
        }

        [Fact]
        public void Format_Method_RendersWithTwoSpaceIndent()
        {
            var result = Formatter.Format([MakeType(typeof(string), methods: ["Run()"])], DefaultConfig);
            result.Should().Contain("  Run()\n");
        }

        [Fact]
        public void Format_ShowAttributesFalse_NoFields()
        {
            var fields = new[] { new FieldInfo("count", "Int32", '-') };
            var result = Formatter.Format([MakeType(typeof(string), fields)], NoFields);
            result.Should().NotContain("-count:Int32");
        }

        [Fact]
        public void Format_ShowMethodsFalse_NoMethods()
        {
            var result = Formatter.Format([MakeType(typeof(string), methods: ["Run()"])], NoMethods);
            result.Should().NotContain("Run()");
        }
    }

    // ── RelationshipLines ─────────────────────────────────────────────────────
    public class RelationshipLines
    {
        private static TypeInfo TypeWithRel(Type type, Relationship rel)
            => new(type, [], [], [rel]);

        [Fact]
        public void Format_Implements_RendersInheritanceArrow()
        {
            var info = TypeWithRel(typeof(IDisposable), new Relationship.Implements("IRunnable"));
            var result = Formatter.Format([info], DefaultConfig);
            result.Should().Contain("IRunnable <|--- IDisposable");
        }

        [Fact]
        public void Format_Extends_RendersInheritanceArrow()
        {
            var info = TypeWithRel(typeof(string), new Relationship.Extends("Base"));
            var result = Formatter.Format([info], DefaultConfig);
            result.Should().Contain("Base <|--- String");
        }

        [Fact]
        public void Format_Association_RendersDependencyArrow()
        {
            var info = TypeWithRel(typeof(string), new Relationship.Association("Widget"));
            var result = Formatter.Format([info], DefaultConfig);
            result.Should().Contain("String ---> Widget");
        }

        [Fact]
        public void Format_Dependency_RendersDependencyArrow()
        {
            var info = TypeWithRel(typeof(string), new Relationship.Dependency("Widget"));
            var result = Formatter.Format([info], DefaultConfig);
            result.Should().Contain("String ---> Widget");
        }

        [Fact]
        public void Format_RelationshipLine_FollowedByBlankLine()
        {
            var info = TypeWithRel(typeof(string), new Relationship.Extends("Base"));
            var result = Formatter.Format([info], DefaultConfig);
            result.Should().Contain("Base <|--- String\n\n");
        }

        [Fact]
        public void Format_ClassBlocksBeforeRelationships()
        {
            var info = TypeWithRel(typeof(string), new Relationship.Extends("Base"));
            var result = Formatter.Format([info], DefaultConfig);
            var blockIdx = result.IndexOf("class String{", StringComparison.Ordinal);
            var relIdx = result.IndexOf("Base <|--- String", StringComparison.Ordinal);
            blockIdx.Should().BeLessThan(relIdx);
        }
    }
}
