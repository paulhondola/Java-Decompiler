using CSharpAnalyzer.Config;
using FluentAssertions;

namespace CSharpAnalyzer.Tests.Config;

public class DecompileConfigTests
{
    public class Defaults
    {
        private readonly DecompileConfig _config = DecompileConfig.Defaults();

        [Fact]
        public void Defaults_ReturnsEmptyIgnorePatterns() =>
            _config.IgnorePatterns.Should().BeEmpty();

        [Fact]
        public void Defaults_NotFullyQualified() => _config.FullyQualified.Should().BeFalse();

        [Fact]
        public void Defaults_ShowMethodsEnabled() => _config.ShowMethods.Should().BeTrue();

        [Fact]
        public void Defaults_ShowAttributesEnabled() => _config.ShowAttributes.Should().BeTrue();

        [Fact]
        public void Defaults_IgnorePatternListIsReadOnly()
        {
            var act = () => ((IList<string>)_config.IgnorePatterns).Add("anything");
            act.Should().Throw<NotSupportedException>();
        }
    }

    public class CustomConfig
    {
        [Fact]
        public void CustomConfig_StoresIgnorePatterns()
        {
            var config = new DecompileConfig(["java.lang.*", "java.util.*"], false, true, true);
            config.IgnorePatterns.Should().ContainInOrder("java.lang.*", "java.util.*");
        }

        [Fact]
        public void CustomConfig_PreservesFullyQualifiedFlag()
        {
            var config = new DecompileConfig([], true, true, true);
            config.FullyQualified.Should().BeTrue();
        }

        [Fact]
        public void CustomConfig_ShowMethodsFalse()
        {
            var config = new DecompileConfig([], false, false, true);
            config.ShowMethods.Should().BeFalse();
        }

        [Fact]
        public void CustomConfig_ShowAttributesFalse()
        {
            var config = new DecompileConfig([], false, true, false);
            config.ShowAttributes.Should().BeFalse();
        }
    }
}
