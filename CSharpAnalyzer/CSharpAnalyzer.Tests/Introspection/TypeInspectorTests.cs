using CSharpAnalyzer.Config;
using CSharpAnalyzer.Introspection;
using CSharpAnalyzer.Model;
using FluentAssertions;

namespace CSharpAnalyzer.Tests.Introspection;

public class TypeInspectorTests
{
    // ── Inline fixture types (plain fields, no auto-properties) ─────────────
    private interface IPrintable { void Print(); }
    private interface IDisplayable : IPrintable { void Display(); }

    private class Widget
    {
        public float height;
        protected int width;
        private string label = "";
        public string GetLabel() => label;
        protected void Resize() { }
    }

    private class Button : Widget
    {
        private Widget icon = null!;
        public void Click() { }
    }

    private class GenericHolder
    {
        private List<Widget> items = [];
        private Type meta = typeof(object);
    }

    // ── helpers ─────────────────────────────────────────────────────────────
    private static readonly DecompileConfig DefaultConfig = DecompileConfig.Defaults();

    private static IReadOnlySet<string> Loaded(params Type[] types)
        => types.Select(t => t.Name).ToHashSet();

    // ── IsInterface ──────────────────────────────────────────────────────────
    public class IsInterface
    {
        [Fact]
        public void Inspect_Interface_TypeIsInterface()
        {
            var info = TypeInspector.Inspect(typeof(IPrintable), DefaultConfig, Loaded(typeof(IPrintable)));
            info.Type.IsInterface.Should().BeTrue();
        }

        [Fact]
        public void Inspect_Class_TypeIsNotInterface()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Type.IsInterface.Should().BeFalse();
        }
    }

    // ── Fields ───────────────────────────────────────────────────────────────
    public class Fields
    {
        [Fact]
        public void Inspect_Widget_ExtractsThreeFields()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Fields.Should().HaveCount(3);
        }

        [Fact]
        public void Inspect_PrivateField_DashModifier()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Fields.Should().Contain(f => f.Name == "label" && f.AccessModifier == '-');
        }

        [Fact]
        public void Inspect_ProtectedField_HashModifier()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Fields.Should().Contain(f => f.Name == "width" && f.AccessModifier == '#');
        }

        [Fact]
        public void Inspect_PublicField_PlusModifier()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Fields.Should().Contain(f => f.Name == "height" && f.AccessModifier == '+');
        }

        [Fact]
        public void Inspect_Field_TypeNameUsesSimpleName()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Fields.Should().Contain(f => f.Name == "label" && f.TypeName == "String");
        }

        [Fact]
        public void Inspect_Interface_NoFields()
        {
            var info = TypeInspector.Inspect(typeof(IPrintable), DefaultConfig, Loaded(typeof(IPrintable)));
            info.Fields.Should().BeEmpty();
        }
    }

    // ── Methods ──────────────────────────────────────────────────────────────
    public class Methods
    {
        [Fact]
        public void Inspect_Widget_ExtractsMethods()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Methods.Should().Contain(["GetLabel()", "Resize()"]);
        }

        [Fact]
        public void Inspect_Interface_ExtractsMethods()
        {
            var info = TypeInspector.Inspect(typeof(IPrintable), DefaultConfig, Loaded(typeof(IPrintable)));
            info.Methods.Should().Contain("Print()");
        }

        [Fact]
        public void Inspect_MethodNames_AlwaysHaveParentheses()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Methods.Should().AllSatisfy(m => m.Should().EndWith("()"));
        }
    }

    // ── Relationships ─────────────────────────────────────────────────────────
    public class Relationships
    {
        [Fact]
        public void Inspect_ButtonExtendsWidget_AddsExtendsRelationship()
        {
            var loaded = Loaded(typeof(Widget), typeof(Button));
            var info = TypeInspector.Inspect(typeof(Button), DefaultConfig, loaded);
            info.Relationships.Any(r => r is Relationship.Extends { TargetName: "Widget" }).Should().BeTrue();
        }

        [Fact]
        public void Inspect_SuperclassNotInLoadedSet_NoExtendsRelationship()
        {
            var loaded = Loaded(typeof(Button)); // Widget not in set
            var info = TypeInspector.Inspect(typeof(Button), DefaultConfig, loaded);
            info.Relationships.Any(r => r is Relationship.Extends).Should().BeFalse();
        }

        [Fact]
        public void Inspect_WidgetImplementsNothing()
        {
            var info = TypeInspector.Inspect(typeof(Widget), DefaultConfig, Loaded(typeof(Widget)));
            info.Relationships.Any(r => r is Relationship.Implements).Should().BeFalse();
        }

        [Fact]
        public void Inspect_DisplayableExtendsIPrintable_AddsImplements()
        {
            var loaded = Loaded(typeof(IPrintable), typeof(IDisplayable));
            var info = TypeInspector.Inspect(typeof(IDisplayable), DefaultConfig, loaded);
            info.Relationships.Any(r => r is Relationship.Implements { TargetName: "IPrintable" }).Should().BeTrue();
        }

        [Fact]
        public void Inspect_ButtonIconField_AddsAssociationToWidget()
        {
            var loaded = Loaded(typeof(Widget), typeof(Button));
            var info = TypeInspector.Inspect(typeof(Button), DefaultConfig, loaded);
            info.Relationships.Any(r => r is Relationship.Association { TargetName: "Widget" }).Should().BeTrue();
        }

        [Fact]
        public void Inspect_FieldNotInLoadedSet_NoAssociation()
        {
            var loaded = Loaded(typeof(Button)); // Widget not in set
            var info = TypeInspector.Inspect(typeof(Button), DefaultConfig, loaded);
            info.Relationships.Any(r => r is Relationship.Association).Should().BeFalse();
        }

        [Fact]
        public void Inspect_GenericField_TypeArgIsAssociation()
        {
            // List<Widget> → Association to Widget, not List
            var loaded = Loaded(typeof(Widget), typeof(GenericHolder));
            var info = TypeInspector.Inspect(typeof(GenericHolder), DefaultConfig, loaded);
            info.Relationships.Any(r => r is Relationship.Association { TargetName: "Widget" }).Should().BeTrue();
            info.Relationships.Any(r => r is Relationship.Association { TargetName: "List" }).Should().BeFalse();
        }

        [Fact]
        public void Inspect_DuplicateAssociationsToSameTarget_Deduplicated()
        {
            // Button has one Widget field — result has at most one Association to Widget
            var loaded = Loaded(typeof(Widget), typeof(Button));
            var info = TypeInspector.Inspect(typeof(Button), DefaultConfig, loaded);
            info.Relationships.OfType<Relationship.Association>()
                .Where(a => a.TargetName == "Widget")
                .Should().HaveCount(1);
        }
    }

    // ── GetTypeName ───────────────────────────────────────────────────────────
    public class GetTypeNameTests
    {
        [Fact]
        public void GetTypeName_SimpleType_ReturnsSimpleName()
        {
            var result = TypeInspector.GetTypeName(typeof(string), DefaultConfig);
            result.Should().Be("String");
        }

        [Fact]
        public void GetTypeName_PrimitiveType_ReturnsReflectionName()
        {
            // C# reflection uses Int32, not int
            var result = TypeInspector.GetTypeName(typeof(int), DefaultConfig);
            result.Should().Be("Int32");
        }

        [Fact]
        public void GetTypeName_GenericType_RendersOfSyntax()
        {
            var result = TypeInspector.GetTypeName(typeof(List<Widget>), DefaultConfig);
            result.Should().Be("List of Widget");
        }

        [Fact]
        public void GetTypeName_NestedGeneric_RendersFirstArg()
        {
            var result = TypeInspector.GetTypeName(typeof(Dictionary<string, int>), DefaultConfig);
            result.Should().Be("Dictionary of String, Int32");
        }
    }
}
