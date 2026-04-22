using CSharpAnalyzer.Config;
using CSharpAnalyzer.Filter;
using FluentAssertions;

namespace CSharpAnalyzer.Tests.Filter;

public class TypeFilterTests
{
    public class NoPatterns
    {
        [Fact]
        public void Filter_NoPatterns_ReturnsAllTypes()
        {
            var types = new[] { typeof(string), typeof(int), typeof(System.IO.File) };
            var result = TypeFilter.Filter(types, DecompileConfig.Defaults());
            result.Should().BeEquivalentTo(types);
        }

        [Fact]
        public void Filter_NoPatterns_EmptyInput_ReturnsEmpty()
        {
            var result = TypeFilter.Filter([], DecompileConfig.Defaults());
            result.Should().BeEmpty();
        }
    }

    public class ExactMatch
    {
        [Fact]
        public void Filter_ExactPattern_ExcludesType()
        {
            var types = new[] { typeof(string), typeof(int) };
            var config = new DecompileConfig(["System.String"], false, true, true);
            var result = TypeFilter.Filter(types, config);
            result.Should().ContainSingle().Which.Should().Be(typeof(int));
        }

        [Fact]
        public void Filter_ExactPattern_NoMatch_KeepsAll()
        {
            var types = new[] { typeof(string), typeof(int) };
            var config = new DecompileConfig(["System.Double"], false, true, true);
            var result = TypeFilter.Filter(types, config);
            result.Should().HaveCount(2);
        }
    }

    public class WildcardPattern
    {
        [Fact]
        public void Filter_WildcardPattern_ExcludesNamespace()
        {
            var types = new[] { typeof(string), typeof(int), typeof(System.IO.File) };
            var config = new DecompileConfig(["System.IO.*"], false, true, true);
            var result = TypeFilter.Filter(types, config);
            result.Should().HaveCount(2).And.NotContain(typeof(System.IO.File));
        }

        [Fact]
        public void Filter_WildcardPattern_DoesNotMatchUnrelatedNamespace()
        {
            var types = new[] { typeof(string), typeof(System.IO.File) };
            var config = new DecompileConfig(["System.Collections.*"], false, true, true);
            var result = TypeFilter.Filter(types, config);
            result.Should().HaveCount(2);
        }

        [Fact]
        public void Filter_MultiplePatterns_ExcludesAllMatching()
        {
            var types = new[] { typeof(string), typeof(int), typeof(System.IO.File) };
            var config = new DecompileConfig(["System.String", "System.IO.*"], false, true, true);
            var result = TypeFilter.Filter(types, config);
            result.Should().ContainSingle().Which.Should().Be(typeof(int));
        }
    }
}
