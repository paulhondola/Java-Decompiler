package org.paul.model;

public sealed interface Relationship permits
        Relationship.Extends,
        Relationship.Implements,
        Relationship.Association,
        Relationship.Dependency {

    record Extends(String targetName) implements Relationship {
    }

    record Implements(String targetName) implements Relationship {
    }

    record Association(String targetName) implements Relationship {
    }

    record Dependency(String targetName) implements Relationship {
    }
}
