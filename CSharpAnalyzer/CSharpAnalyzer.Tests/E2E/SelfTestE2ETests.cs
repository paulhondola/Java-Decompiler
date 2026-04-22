using CSharpAnalyzer.Config;
using CSharpAnalyzer.Formatter;
using FluentAssertions;

namespace CSharpAnalyzer.Tests.E2E;

/// <summary>
/// Runs the full pipeline against the TempSensor.dll fixture.
/// On first run, golden files are generated and committed.
/// On subsequent runs, output is compared against golden files.
/// </summary>
public class SelfTestE2ETests
{
    private static readonly string FixtureDll = Path.GetFullPath(
        Path.Combine(AppContext.BaseDirectory,
            "../../../../../fixtures/csharp/TempSensor/bin/Debug/net10.0/TempSensor.dll"));

    private static readonly string YumlGolden = Path.GetFullPath(
        Path.Combine(AppContext.BaseDirectory,
            "../../../../../fixtures/csharp/TempSensor/TempSensor.yuml"));

    private static readonly string PumlGolden = Path.GetFullPath(
        Path.Combine(AppContext.BaseDirectory,
            "../../../../../fixtures/csharp/TempSensor/TempSensor.puml"));

    [Fact]
    public void SelfTest_YumlClasses_MatchesGoldenFile()
    {
        var result = Program.Decompile(FixtureDll, new YumlFormatter(), DecompileConfig.Defaults());

        if (!File.Exists(YumlGolden))
        {
            File.WriteAllText(YumlGolden, result);
            return; // First run: generate golden file
        }

        var golden = File.ReadAllText(YumlGolden);
        result.Should().Be(golden);
    }

    [Fact]
    public void SelfTest_PlantUml_MatchesGoldenFile()
    {
        var result = Program.Decompile(FixtureDll, new PlantUmlFormatter(), DecompileConfig.Defaults());

        if (!File.Exists(PumlGolden))
        {
            File.WriteAllText(PumlGolden, result);
            return; // First run: generate golden file
        }

        var golden = File.ReadAllText(PumlGolden);
        result.Should().Be(golden);
    }
}
