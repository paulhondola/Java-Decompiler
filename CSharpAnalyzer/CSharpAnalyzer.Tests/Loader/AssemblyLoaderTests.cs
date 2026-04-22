using CSharpAnalyzer.Loader;
using FluentAssertions;

namespace CSharpAnalyzer.Tests.Loader;

public class AssemblyLoaderTests
{
    // AppContext.BaseDirectory = CSharpAnalyzer/CSharpAnalyzer.Tests/bin/Debug/net10.0/
    // 5 levels up reaches the archlens root
    private static readonly string FixtureDll = Path.GetFullPath(
        Path.Combine(AppContext.BaseDirectory,
            "../../../../../fixtures/csharp/TempSensor/bin/Debug/net10.0/TempSensor.dll"));

    public class FixtureDllTests
    {
        private readonly IReadOnlyList<Type> _types = AssemblyLoader.Load(
            Path.GetFullPath(Path.Combine(AppContext.BaseDirectory,
                "../../../../../fixtures/csharp/TempSensor/bin/Debug/net10.0/TempSensor.dll")));

        [Fact]
        public void Load_LoadsExpectedTypeCount()
            => _types.Should().HaveCount(7);

        [Fact]
        public void Load_ContainsExpectedTypeNames()
        {
            var names = _types.Select(t => t.Name).ToList();
            names.Should().Contain([
                "AverageDisplay", "MainDriver", "NumericDisplay",
                "Observer", "Subject", "TemperatureSensor", "TextDisplay"
            ]);
        }

        [Fact]
        public void Load_NoNullEntries()
            => _types.Should().NotContainNulls();

        [Fact]
        public void Load_ReturnsReadOnlyList()
            => _types.Should().BeAssignableTo<IReadOnlyList<Type>>();
    }

    public class ErrorHandling
    {
        [Fact]
        public void Load_NonExistentPath_ThrowsWithDescriptiveMessage()
        {
            var act = () => AssemblyLoader.Load("/nonexistent/path/Missing.dll");
            act.Should().Throw<InvalidOperationException>()
               .WithMessage("*Failed to load assembly*");
        }
    }
}
