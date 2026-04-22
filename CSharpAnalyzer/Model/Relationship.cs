namespace CSharpAnalyzer.Model;

public abstract record Relationship(string TargetName)
{
    public sealed record Extends(string TargetName) : Relationship(TargetName);

    public sealed record Implements(string TargetName) : Relationship(TargetName);

    public sealed record Association(string TargetName) : Relationship(TargetName);

    public sealed record Dependency(string TargetName) : Relationship(TargetName);
}
