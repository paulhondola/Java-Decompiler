# Architecture: Java Reverse Engineering Tool

## Overview

The tool loads a `.jar` file, introspects its compiled classes via `java.lang.reflect`, builds a
structured class model, then formats it as a UML diagram in the requested output format.

The pipeline is strictly layered: **loading → introspection → filtering → model building → formatting**.
Formatters never touch the filesystem or JAR; the core pipeline never produces strings.

---

## Package Structure

```
org.paul/
├── Main.java                  # Entry point — orchestrates the full pipeline
├── config/
│   └── DecompileConfig.java   # Immutable config record (flags, ignore list)
├── loader/
│   └── JarLoader.java         # Opens JAR, loads Class<?> objects via URLClassLoader
├── model/
│   ├── ClassInfo.java         # Immutable record: class + extracted relationships
│   ├── Relationship.java      # Sealed type: Extends | Implements | Association | Dependency
│   └── FieldInfo.java         # Immutable record: field name, type, access modifier
├── introspection/
│   └── ClassInspector.java    # Extracts ClassInfo from a single Class<?> + config
├── filter/
│   └── ClassFilter.java       # Applies ignore patterns to List<ClassInfo>
└── formatter/
    ├── UmlFormatter.java      # Interface: format(List<ClassInfo>, DecompileConfig) → String
    ├── YumlFormatter.java     # yuml output (SIMPLE / CLASSES modes)
    └── PlantUmlFormatter.java # plantuml output
```

---

## Components

### 1. `DecompileConfig` (config layer)

Immutable record holding all user-supplied options.

```java
public record DecompileConfig(
    List<String> ignorePatterns,   // e.g. ["java.lang.*", "java.util.*"]
    boolean fullyQualified,        // show fully-qualified class names
    boolean showMethods,           // include method names
    boolean showAttributes         // include field names
) {
    public static DecompileConfig defaults() { ... }
}
```

### 2. `JarLoader` (loader layer)

Responsibility: given a `.jar` path, return the raw `List<Class<?>>`.

- Opens the JAR with `java.util.jar.JarFile`
- Iterates entries to collect `.class` names
- Loads each via `URLClassLoader`
- Skips anonymous/synthetic classes
- Throws `JarLoadException` (unchecked) on IO or classloading failure

```
JarLoader.load(jarPath: String) → List<Class<?>>
```

### 3. Domain Model (model layer)

#### `Relationship` — sealed type hierarchy

```java
public sealed interface Relationship permits
    Relationship.Extends,
    Relationship.Implements,
    Relationship.Association,
    Relationship.Dependency {

    record Extends(String targetName) implements Relationship {}
    record Implements(String targetName) implements Relationship {}
    record Association(String targetName) implements Relationship {}
    record Dependency(String targetName) implements Relationship {}
}
```

Using a sealed interface here means switch expressions over relationship types are exhaustive —
the compiler catches missing cases when a new relationship type is added.

#### `FieldInfo`

```java
public record FieldInfo(String name, String typeName, char accessModifier)
// accessModifier: '-' private, '#' protected, '+' public, '~' package
```

#### `ClassInfo`

```java
public record ClassInfo(
    Class<?>             clazz,
    boolean              isInterface,
    List<FieldInfo>      fields,
    List<String>         methods,        // method signatures as strings
    List<Relationship>   relationships
) {}
```

### 4. `ClassInspector` (introspection layer)

Extracts a `ClassInfo` from a single `Class<?>` using `java.lang.reflect`:

| Reflection API | What it extracts |
|---|---|
| `clazz.getSuperclass()` | `Relationship.Extends` (if not `Object`) |
| `clazz.getInterfaces()` | `Relationship.Implements` |
| `clazz.getDeclaredFields()` | `FieldInfo` list + `Relationship.Association` from field types |
| `clazz.getDeclaredMethods()` | method names + `Relationship.Dependency` from param/return types |
| `clazz.isInterface()` | class vs interface flag |

**Parameterized types:** Use `field.getGenericType()` and cast to
`ParameterizedType` to extract actual type arguments (e.g., `ArrayList<Observer>` → `"ArrayList of Observer"`).

**Self-references:** A class referencing itself (e.g. `EventService` singleton field of type
`EventService`) produces a valid `Association` — kept intentionally (visible in test fixtures).

```
ClassInspector.inspect(clazz: Class<?>, config: DecompileConfig) → ClassInfo
```

### 5. `ClassFilter` (filter layer)

Applies `ignorePatterns` from config. Patterns support `*` as a wildcard suffix
(e.g., `java.lang.*`). Filtering operates on the class's fully-qualified name.

```
ClassFilter.filter(classes: List<Class<?>>, config: DecompileConfig) → List<Class<?>>
```

Filtering happens **before** introspection to avoid loading metadata for ignored classes.

### 6. `UmlFormatter` interface (formatter layer)

```java
public interface UmlFormatter {
    String format(List<ClassInfo> classes, DecompileConfig config);
}
```

> Note: the current interface signature is `format(List<Class<?>>)`. During implementation
> it will be upgraded to accept `List<ClassInfo>` so formatters don't need to re-introspect.
> Tests will be updated accordingly.

---

## Pipeline Flow

```
CLI args
   │
   ▼
DecompileConfig (parse flags)
   │
   ▼
JarLoader.load(jarPath)           → List<Class<?>>
   │
   ▼
ClassFilter.filter(...)           → List<Class<?>>  (ignore patterns applied)
   │
   ▼
ClassInspector.inspect(each)      → List<ClassInfo>
   │
   ▼
UmlFormatter.format(classInfos)   → String
   │
   ▼
stdout / file
```

---

## Output Format Specifications

### yuml SIMPLE mode

One line per class, then one line per relationship. No member details.

```
[ClassName]
[Interface]^-.-[Implementor]       ← implements
[Parent]^-[Child]                  ← extends (class)
[Owner]->[Target]                  ← association or dependency
```

### yuml CLASSES mode

Same relationships, but class nodes include fields and methods:

```
[ClassName|fields|methods]
```

Field syntax: `ACCESS name:type` (e.g., `- sum:float`)
Multiple fields: semicolon-separated `- sum:float;- count:int`
Method syntax: `methodName()`, semicolon-separated
Parameterized: `ArrayList of Observer`

Access modifier mapping:
| Java modifier | yuml symbol |
|---|---|
| `private` | `-` |
| `protected` | `#` |
| `public` | `+` |
| package-private | `~` |

### plantuml mode

```
@startuml

interface InterfaceName{
  method()
}

class ClassName{
  -fieldName:FieldType
  methodName()
}

Parent <|--- Child          ← implements or extends
Owner ---> Target           ← association or dependency

@enduml
```

---

## Relationship Deduplication

Relationships must be deduplicated before formatting:
- A class implementing multiple interfaces produces one line per interface
- Field types of the same target class produce one association line
- Method param/return types deduplicate with associations of the same target

---

## Extension Points

Adding a new output format requires:
1. Create a new class implementing `UmlFormatter`
2. Register it in `Main` (or via a `FormatterRegistry` map keyed by format name string)

No changes to the core pipeline, loader, inspector, or model are required.

---

## Implementation Roadmap

| Phase | Components | Tests |
|---|---|---|
| 1 | `DecompileConfig`, `Relationship`, `FieldInfo`, `ClassInfo` | — (data classes) |
| 2 | `JarLoader` | Unit: can load TempSensor.jar class list |
| 3 | `ClassFilter` | Unit: ignore pattern matching |
| 4 | `ClassInspector` | Unit: extract fields/methods/relationships from a known class |
| 5 | `YumlFormatter` (SIMPLE) | Integration: TempSensor + EventNotifier simple yuml fixtures |
| 6 | `YumlFormatter` (CLASSES) | Integration: EventNotifier classes yuml fixture |
| 7 | `PlantUmlFormatter` | Integration: TempSensor plantuml fixture |
| 8 | `Main` CLI wiring | Manual: run tool end-to-end |

---

## Key Design Decisions

**Why `List<ClassInfo>` instead of `List<Class<?>>`?**
Formatters should not be responsible for introspection. Centralizing reflection in
`ClassInspector` means each formatter only needs to render data — no reflection logic is
duplicated, and formatters are trivially testable with hand-crafted `ClassInfo` instances.

**Why a sealed `Relationship` type?**
The four relationship kinds are fixed by the task spec. A sealed type makes the set closed and
enables exhaustive switch-based rendering in each formatter without risk of silently missing a
new kind if one were ever added.

**Why filter before inspect?**
Introspection loads field types and method signatures, which can themselves reference classes in
the ignore list. Filtering first prevents those from appearing as relationship targets and avoids
the overhead of inspecting classes that will be discarded.
