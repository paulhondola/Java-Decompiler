# C# Decompiler — Implementation Plan

Mirrors the Java decompiler architecture exactly. This document covers every implementation
decision, divergence from Java, and test case required before a line of code is written.

---

## 1. Java → C# Concept Map

| Java concept                 | C# equivalent                                    | Notes                                               |
| ---------------------------- | ------------------------------------------------ | --------------------------------------------------- |
| `Class<?>`                   | `System.Type`                                    | Central reflection object                           |
| `JarFile` + `URLClassLoader` | `Assembly.LoadFrom()`                            | DLL loading                                         |
| `getDeclaredFields()`        | `GetFields(BindingFlags.DeclaredOnly \| ...)`    | Need explicit binding flags                         |
| `getDeclaredMethods()`       | `GetMethods(BindingFlags.DeclaredOnly \| ...)`   | Same                                                |
| `getSuperclass()`            | `Type.BaseType`                                  | C# root is `object`, not skipped as `Object.class`  |
| `getInterfaces()`            | `Type.GetInterfaces()`                           | ⚠ Returns ALL interfaces — must diff with base type |
| `isSynthetic()`              | `IsDefined(CompilerGeneratedAttribute)`          | Different mechanism                                 |
| `isAnonymousClass()`         | `Type.Name.Contains('<')`                        | Compiler-generated names use `<>`                   |
| `name.contains("$")`         | `Type.IsNested`                                  | Skip all nested types (same scope as Java `$`)      |
| `ParameterizedType`          | `Type.IsGenericType`                             |                                                     |
| `WildcardType` (`?`)         | `Type.IsGenericParameter`                        | Open generic parameters: `T`, `TKey`                |
| `GenericArrayType`           | `Type.IsArray && GetElementType().IsGenericType` |                                                     |
| `TypeVariable`               | `Type.IsGenericParameter`                        | Named type params                                   |
| `Modifier.isPublic()`        | `FieldInfo.IsPublic`                             |                                                     |
| `Modifier.isProtected()`     | `FieldInfo.IsFamily`                             | C# calls it "family"                                |
| `Modifier.isPrivate()`       | `FieldInfo.IsPrivate`                            |                                                     |
| package-private              | `FieldInfo.IsAssembly`                           | C# internal                                         |
| `record` (Java)              | `record` (C#)                                    | Both immutable value types                          |
| `sealed interface`           | `abstract record` with `sealed record` subtypes  | Pattern match works the same                        |
| `List.copyOf()`              | `.ToList().AsReadOnly()`                         | Returns `IReadOnlyList<T>`                          |
| `JUnit 5 @Nested`            | xUnit nested class (no attribute needed)         |                                                     |
| `@DisplayName`               | `[Trait("Category", "...")]` or just method name | Method name is the display                          |
| `AssertJ assertThat`         | `FluentAssertions .Should()`                     |                                                     |
| picocli                      | `System.CommandLine`                             | Official MS library                                 |

---

## 2. Critical C# Divergences from Java

### 2.1 `GetInterfaces()` returns inherited interfaces too

Java's `getInterfaces()` returns only the interfaces **directly declared** on the type.
C#'s `GetInterfaces()` returns the full transitive closure.

**Fix**: subtract the base type's interfaces:

```csharp
var directInterfaces = type.GetInterfaces();
if (type.BaseType != null)
{
    var inherited = type.BaseType.GetInterfaces();
    directInterfaces = directInterfaces.Except(inherited).ToArray();
}
```

### 2.2 Generic type name has a backtick arity suffix

`List<Widget>.GetGenericTypeDefinition().Name` → `"List\`1"` — the `` `1 `` must be stripped.

```csharp
private static string StripArity(string name)
{
    int i = name.IndexOf('`');
    return i >= 0 ? name[..i] : name;
}
```

Apply `StripArity` whenever displaying generic type names.

### 2.3 Property backing fields pollute field list

Auto-properties like `public string Name { get; set; }` generate a field named
`<Name>k__BackingField`. Filter them:

```csharp
field.Name.StartsWith('<') // skip compiler backing fields
```

### 2.4 Property accessors pollute method list

`get_Name()` / `set_Name()` have `MethodInfo.IsSpecialName == true`. Filter them
(same as Java's `isBridge()` + `isSynthetic()` guard):

```csharp
!method.IsSpecialName
```

### 2.5 `ReflectionTypeLoadException` instead of `NoClassDefFoundError`

When an assembly has unresolved dependencies, `GetTypes()` throws `ReflectionTypeLoadException`.
The partial results are in `ex.Types` (may contain nulls):

```csharp
try { allTypes = assembly.GetTypes(); }
catch (ReflectionTypeLoadException ex)
{ allTypes = ex.Types.Where(t => t != null).ToArray()!; }
```

### 2.6 C# primitive display names differ from Java

Java reflection returns `int`, `boolean`. C# reflection returns `Int32`, `Boolean`.
Do **not** map these — expose the reflection names as-is. This is a difference in the output
format, not a bug.

### 2.7 Root class is `object` not `Object`

Java skips `Object.class` in the Extends check. C# must skip `typeof(object)` (i.e.
`System.Object`). The condition is identical:

```csharp
type.BaseType != null && type.BaseType != typeof(object)
```

### 2.8 `Type.Name` vs `Type.FullName`

- `Type.Name` for a generic like `List<Widget>` is `"List\`1"` — needs arity stripping.
- `Type.FullName` for the same is `"System.Collections.Generic.List\`1"` — also needs stripping.
- For non-generic types, `Name` is clean: `"String"`, `"Widget"`.

Apply `StripArity` everywhere a type name is rendered.

### 2.9 Access modifier on `System.Reflection.FieldInfo` (not `int` flags)

Unlike Java's `int Modifier` bitmask, C# `FieldInfo` has boolean properties:

| C# property          | UML char                   |
| -------------------- | -------------------------- |
| `IsPublic`           | `'+'`                      |
| `IsFamily`           | `'#'`                      |
| `IsFamilyOrAssembly` | `'#'` (treat as protected) |
| `IsPrivate`          | `'-'`                      |
| `IsAssembly`         | `'~'`                      |

---

## 3. Main Project — File-by-File Implementation Spec

### 3.1 `Loader/AssemblyLoader.cs`

**Purpose**: mirrors `JarLoader.java` — open a `.dll`, load non-compiler-generated
non-nested types, return in metadata order.

```
Load(string assemblyPath) → IReadOnlyList<Type>
```

**Algorithm**:

1. Resolve `assemblyPath` to absolute path. If file not found, throw `InvalidOperationException`
   with message containing `"Failed to load assembly: {assemblyPath}"` (mirrors Java's message).
2. `Assembly.LoadFrom(fullPath)` — wrap in try/catch, rethrow as `InvalidOperationException`.
3. Call `assembly.GetTypes()` inside a try/catch for `ReflectionTypeLoadException`; use
   `ex.Types.Where(t => t != null)` as fallback.
4. Filter: `!t.IsNested && !IsCompilerGenerated(t)`.
5. `IsCompilerGenerated(t)`: `t.IsDefined(typeof(CompilerGeneratedAttribute), false) || t.Name.Contains('<')`.
6. Return `.ToList().AsReadOnly()`.

**No ordering guarantee beyond metadata order** (mirrors Java's JAR-entry order).

---

### 3.2 `Filter/TypeFilter.cs`

**Purpose**: mirrors `ClassFilter.java` — exclude types by name pattern.

```
Filter(IReadOnlyList<Type> types, DecompileConfig config) → IReadOnlyList<Type>
IsIgnored(Type type, DecompileConfig config) → bool   (private)
```

**Algorithm** — identical to Java:

- Empty pattern list → return input unchanged.
- Pattern ending `.*`: strip suffix, check `fqn.StartsWith(pkg + ".")` or `fqn == pkg`.
- Exact match: `fqn == pattern`.
- `fqn = type.FullName ?? type.Name`.

---

### 3.3 `Introspection/TypeInspector.cs`

**Purpose**: mirrors `ClassInspector.java` — extract `TypeInfo` from a single `Type`.

**Public API**:

```
Inspect(Type type, DecompileConfig config, IReadOnlySet<string> loadedTypeNames) → TypeInfo
GetTypeName(Type type, DecompileConfig config) → string   (internal, tested directly)
```

**Private helpers**:

```
AccessModChar(FieldInfo field) → char
TypeDisplayName(Type type, DecompileConfig config) → string
GetTypeArgDisplayName(Type type, DecompileConfig config) → string
CollectTargets(Type type, IReadOnlySet<string> loaded, DecompileConfig config,
               LinkedHashSet<string> targets) → void
StripArity(string name) → string
```

**Field extraction**:

- `type.GetFields(BindingFlags.DeclaredOnly | BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance | BindingFlags.Static)`
- Skip: `field.Name.StartsWith('<')` (backing fields), `field.IsDefined(typeof(CompilerGeneratedAttribute), false)`.
- For each field: `AccessModChar(field)`, `GetTypeName(field.FieldType, config)`.
- Collect association targets from `field.FieldType`.

**Method extraction**:

- `type.GetMethods(BindingFlags.DeclaredOnly | BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Instance | BindingFlags.Static)`
- Skip: `method.IsSpecialName`, `method.IsDefined(typeof(CompilerGeneratedAttribute), false)`.
- Format: `"MethodName()"` — lowercase first char only if following Java convention? No — use
  C# PascalCase as-is from reflection. `method.Name + "()"`.
- Sort alphabetically (mirrors Java's `Collections.sort(methodNames)`).

**Relationships — Association** (field-based, same as Java):

- Walk `field.FieldType` via `CollectTargets`.
- `CollectTargets` for generic types: strip arity from raw type name, check if in loaded set;
  recurse into `GetGenericArguments()`.
- Use `LinkedHashSet<string>` to preserve encounter order and deduplicate.

**Relationships — Extends**:

```csharp
if (type.BaseType != null && type.BaseType != typeof(object))
{
    string targetName = TypeDisplayName(type.BaseType, config);
    if (loadedTypeNames.Contains(targetName))
        relationships.Add(new Relationship.Extends(targetName));
}
```

**Relationships — Implements** (directly declared only):

```csharp
var directIfaces = type.GetInterfaces();
if (type.BaseType != null)
    directIfaces = directIfaces.Except(type.BaseType.GetInterfaces()).ToArray();

foreach (var iface in directIfaces)
{
    string targetName = TypeDisplayName(iface, config);
    if (loadedTypeNames.Contains(targetName))
        relationships.Add(new Relationship.Implements(targetName));
}
```

**GetTypeName**:

```
type.IsGenericType  → "{StripArity(rawName)} of {args joined by ", "}"
type.IsArray        → "{GetTypeName(elementType)}[]"
else                → TypeDisplayName(type, config)
```

**GetTypeArgDisplayName**:

```
IsGenericParameter  → type.Name  (T, TKey, etc.)
IsGenericType       → StripArity(name of GetGenericTypeDefinition())
else                → TypeDisplayName(type, config)
```

---

### 3.4 `Formatter/PlantUmlFormatter.cs`

**Purpose**: mirrors `PlantUmlFormatter.java` — identical output format.

```
Format(IReadOnlyList<TypeInfo> types, DecompileConfig config) → string
```

**Algorithm** — two-pass, same as Java:

Pass 1 — type blocks:

```
@startuml\n\n
for each type:
  keyword = type.Type.IsInterface ? "interface" : "class"
  name = config.FullyQualified ? type.Type.FullName! : StripArity(type.Type.Name)
  "{keyword} {name}{\n"
  if config.ShowAttributes: each field → "  {mod}{name}:{typeName}\n"
  if config.ShowMethods:    each method → "  {method}\n"
  "}\n\n"
```

Pass 2 — relationship lines:

```
for each type:
  for each relationship:
    Extends(t)     → "{t} <|--- {name}"
    Implements(t)  → "{t} <|--- {name}"
    Association(t) → "{name} ---> {t}"
    Dependency(t)  → "{name} ---> {t}"
  append "\n\n" after each line
@enduml\n
```

---

### 3.5 `Formatter/YumlFormatter.cs`

**Purpose**: mirrors `YumlFormatter.java`.

```
Format(IReadOnlyList<TypeInfo> types, DecompileConfig config) → string
```

**Mode.Simple**:

```
for each type:
  "[{name}]\n"
  for each relationship:
    Extends(t)     → "[{t}]^-[{name}]"
    Implements(t)  → "[{t}]^-.-[{name}]"
    Association(t) → "[{name}]->[{t}]"
    Dependency(t)  → "[{name}]->[{t}]"
  "\n" after each relationship line
```

**Mode.Classes**:

```
for each type:
  fields string (if ShowAttributes):  "- name:type" joined by ";"
  methods string (if ShowMethods):    "method()" joined by ";"
  "[{name}|{fields}|{methods}]\n"
  (same relationship lines as Simple)
```

Note: Java's CLASSES mode uses a space between modifier and name: `"- count:int"`.
The field format is `"{mod} {name}:{type}"` (space after modifier).

---

### 3.6 `Program.cs`

**Purpose**: CLI entry point using `System.CommandLine`.

Options mirroring Java's picocli:

- `<assemblyPath>` — positional argument (the DLL to analyse)
- `--format` — required, enum `yuml | plantuml`
- `--output` — optional file path
- `--yuml-mode` — optional, enum `simple | classes` (default: `simple`)
- `--ignore` — repeatable or comma-separated patterns

`Decompile()` is already correct in the skeleton — no changes needed.

---

## 4. Test Project Setup

### 4.1 New project: `csharp/CSharpDecompiler.Tests/`

```
CSharpDecompiler.Tests.csproj
```

**Packages**:

- `xunit` 2.9+
- `xunit.runner.visualstudio`
- `FluentAssertions` 7+
- `Microsoft.NET.Test.Sdk`
- Project reference: `../CSharpDecompiler/CSharpDecompiler.csproj`

**Target framework**: `net10.0` (matches main project).

---

## 5. Test Project — File-by-File Spec

### 5.1 `Config/DecompileConfigTests.cs`

Mirror `DecompileConfigTest.java` exactly.

**Nested class `Defaults`**:

- `Defaults_ReturnsEmptyIgnorePatterns` — `config.IgnorePatterns.Should().BeEmpty()`
- `Defaults_NotFullyQualified` — `config.FullyQualified.Should().BeFalse()`
- `Defaults_ShowMethodsEnabled` — `config.ShowMethods.Should().BeTrue()`
- `Defaults_ShowAttributesEnabled` — `config.ShowAttributes.Should().BeTrue()`
- `Defaults_IgnorePatternListIsReadOnly` — try `Add()` on the list, expect `NotSupportedException`

**Nested class `CustomConfig`**:

- `CustomConfig_StoresIgnorePatterns`
- `CustomConfig_PreservesFullyQualifiedFlag`
- `CustomConfig_ShowMethodsFalse`
- `CustomConfig_ShowAttributesFalse`

---

### 5.2 `Loader/AssemblyLoaderTests.cs`

Mirror `JarLoaderTest.java`. Requires a fixture DLL — see §6.

**Nested class `FixtureDll`** (equivalent of TempSensor):

- `Load_LoadsExpectedTypeCount` — count matches known fixture type count
- `Load_ContainsExpectedTypeNames` — exact names (simple) match
- `Load_EntryOrderPreserved` — `ContainInOrder(...)`
- `Load_NoNullEntries`
- `Load_ReturnsReadOnlyList` — `list.Should().BeAssignableTo<IReadOnlyList<Type>>()`

**Nested class `ErrorHandling`**:

- `Load_NonExistentPath_ThrowsWithDescriptiveMessage` — `Should().Throw<InvalidOperationException>().WithMessage("*Failed to load assembly*")`

---

### 5.3 `Filter/TypeFilterTests.cs`

Mirror `ClassFilterTest.java`. Uses `typeof(string)`, `typeof(int)`, `typeof(System.IO.File)` as
stand-in types (same pattern as Java using `String.class`, `Integer.class`, `File.class`).

**Nested class `NoPatterns`**:

- `Filter_NoPatterns_ReturnsAllTypes`
- `Filter_NoPatterns_EmptyInput_ReturnsEmpty`

**Nested class `ExactMatch`**:

- `Filter_ExactPattern_ExcludesType` — filter `"System.String"`, expect only `int` remains
- `Filter_ExactPattern_NoMatch_KeepsAll`

**Nested class `WildcardPattern`**:

- `Filter_WildcardPattern_ExcludesNamespace` — `"System.*"` removes `typeof(string)`, `typeof(int)`
- `Filter_WildcardPattern_DoesNotMatchUnrelatedNamespace`
- `Filter_MultiplePatterns_ExcludesAllMatching`

---

### 5.4 `Introspection/TypeInspectorTests.cs`

Mirror `ClassInspectorTest.java`. Define inline private fixture types:

```csharp
private interface IPrintable { void Print(); }
private interface IDisplayable : IPrintable { void Display(); }

private class Widget
{
    public float Height;
    protected int Width;
    private string _label = "";
    public string GetLabel() => _label;
    protected void Resize() { }
}

private class Button : Widget
{
    private Widget _icon = null!;
    public void Click() { }
}

private class GenericHolder
{
    private List<Widget> _items = [];
    private Type _meta = typeof(object);
}
```

⚠ Use plain fields (not auto-properties) to avoid backing field noise.
⚠ Use `_` prefix on private fields — they are not `IsSpecialName` and won't be filtered, so
keep that in mind: `_label`, `_icon`, `_items`, `_meta`. The tests must account for actual
field names.

Actually — match Java exactly. Java's `Widget` has `label`, `width`, `height` (no prefix).
Use field names without underscore. C# allows this at class scope.

**Nested class `IsInterface`**:

- `Inspect_Interface_TypeIsInterface` — `info.Type.IsInterface.Should().BeTrue()`
- `Inspect_Class_TypeIsNotInterface`

**Nested class `Fields`**:

- `Inspect_Widget_ExtractsThreeFields` — `info.Fields.Should().HaveCount(3)`
- `Inspect_PrivateField_DashModifier`
- `Inspect_ProtectedField_HashModifier`
- `Inspect_PublicField_PlusModifier`
- `Inspect_Field_TypeNameUsesSimpleName` — `label` field → `"String"`
- `Inspect_Interface_NoFields`

**Nested class `Methods`**:

- `Inspect_Widget_ExtractsMethods` — `ContainInAnyOrder("GetLabel()", "Resize()")`
- `Inspect_Interface_ExtractsMethods` — `"Print()"`
- `Inspect_MethodNames_AlwaysHaveParentheses`

**Nested class `Relationships`**:

- `Inspect_ButtonExtendsWidget_AddsExtendsRelationship`
- `Inspect_SuperclassNotInLoadedSet_NoExtendsRelationship`
- `Inspect_WidgetImplementsNothing`
- `Inspect_DisplayableExtendsIPrintable_AddsImplements`
- `Inspect_ButtonIconField_AddsAssociationToWidget`
- `Inspect_FieldNotInLoadedSet_NoAssociation`
- `Inspect_GenericField_TypeArgIsAssociation` — `List<Widget>` → Association to `Widget`, not `List`
- `Inspect_DuplicateAssociationsToSameTarget_Deduplicated`

**Nested class `GetTypeName`**:

- `GetTypeName_SimpleType_ReturnsSimpleName` — `typeof(string)` → `"String"`
- `GetTypeName_PrimitiveType_ReturnsName` — `typeof(int)` → `"Int32"` (C# reflection name)
- `GetTypeName_GenericType_RendersOfSyntax` — `typeof(List<Widget>)` → `"List of Widget"`
- `GetTypeName_NestedGeneric_RendersFirstArg` — test `typeof(Dictionary<string, int>)` → `"Dictionary of String, Int32"`

---

### 5.5 `Formatter/PlantUmlFormatterTests.cs`

Mirror `PlantUmlFormatterTest.java`. Use hand-crafted `TypeInfo` objects.

Helper factory (same pattern as Java):

```csharp
private static TypeInfo MakeTypeInfo(Type type,
    IReadOnlyList<FieldInfo> fields,
    IReadOnlyList<string> methods,
    IReadOnlyList<Relationship> relationships)
    => new(type, fields, methods, relationships);
```

**Nested class `Envelope`**:

- `Format_OutputStartsWithStartUml`
- `Format_OutputEndsWithEndUml`
- `Format_EmptyList_ProducesMinimalDocument` — `"@startuml\n\n@enduml\n"`

**Nested class `Blocks`**:

- `Format_Class_UsesClassKeyword` — `"class String{"`
- `Format_Interface_UsesInterfaceKeyword` — `"interface IRunnable{"` (use an actual interface type)
- `Format_ClassBlock_IsClosed` — contains `"class String{\n}\n"`
- `Format_Field_RendersWithTwoSpaceIndent` — `"  -count:Int32\n"` (note C# type name)
- `Format_Method_RendersWithTwoSpaceIndent` — `"  Run()\n"`
- `Format_ShowAttributesFalse_NoFields`
- `Format_ShowMethodsFalse_NoMethods`

**Nested class `RelationshipLines`**:

- `Format_Implements_RendersInheritanceArrow` — `"Subject <|--- Runnable"`
- `Format_Extends_RendersInheritanceArrow`
- `Format_Association_RendersDependencyArrow`
- `Format_Dependency_RendersDependencyArrow`
- `Format_RelationshipLine_FollowedByBlankLine`
- `Format_ClassBlocksBeforeRelationships`

---

### 5.6 `Formatter/YumlFormatterTests.cs`

Mirror `YumlFormatterTest.java`. Use hand-crafted `TypeInfo`.

Use `typeof(IDisposable)` and `typeof(IComparable)` as type carriers (have simple names,
are interfaces — same role as `Runnable.class` in Java).

**Nested class `SimpleMode`**:

- `Format_EmptyList_ReturnsEmptyString`
- `Format_SingleType_RendersName` — `"[IDisposable]\n"`
- `Format_TypeWithMembers_NoMembersInOutput`
- `Format_Implements_RendersWithDottedArrow`
- `Format_Extends_RendersWithSolidArrow`
- `Format_Association_RendersWithForwardArrow`
- `Format_Dependency_RendersLikeAssociation`
- `Format_MultipleTypes_EachOnOwnLine`
- `Format_RelationshipAppearsAfterTypeLine`

**Nested class `ClassesMode`**:

- `Format_NoMembers_RendersEmptySections` — `"[IDisposable||]\n"`
- `Format_WithFields_RendersFieldSection` — `"[IDisposable|- count:Int32;+ name:String|]\n"`
- `Format_WithMethods_RendersMethodSection`
- `Format_FullType_RendersAllSections`
- `Format_ShowAttributesFalse_FieldsOmitted`
- `Format_ShowMethodsFalse_MethodsOmitted`

---

### 5.7 `E2E/SelfTestE2ETests.cs`

Mirrors `TempSensorTest.java` / `EventNotifierTest.java` but uses the fixture DLL.

```csharp
private const string FixtureDll = "fixtures/csharp/TempSensor/TempSensor.dll";
```

- `SelfTest_YumlClasses_MatchesGoldenFile`
- `SelfTest_PlantUml_MatchesGoldenFile`

Golden files: `fixtures/csharp/TempSensor/TempSensor.yuml`, `TempSensor.puml`.
Generated on first correct run, then committed and used as regression anchors.

---

## 6. Fixture DLL Strategy

### 6.1 New project: `fixtures/csharp/TempSensor/`

A minimal C# class library mirroring the Observer pattern from the Java `TempSensor.jar`.

```
TempSensor.csproj   (net10.0, no output type override needed — defaults to Dll)
Observer.cs
Subject.cs
TemperatureSensor.cs
AverageDisplay.cs
NumericDisplay.cs
TextDisplay.cs
MainDriver.cs
```

**Target**: 7 non-nested, non-compiler-generated types — same count as the Java JAR.

**Class structure**:

```csharp
// Observer.cs
public interface Observer { void Update(); }

// Subject.cs
public interface Subject { void Attach(Observer o); void Detach(Observer o); void NotifyObservers(); }

// TemperatureSensor.cs
public class TemperatureSensor : Subject {
    private List<Observer> observers = [];
    private int tempState;
    public void Attach(Observer o) { }
    public void Detach(Observer o) { }
    public void NotifyObservers() { }
    public void SetTemp(int temp) { }
    public int GetTemp() => tempState;
}

// AverageDisplay.cs
public class AverageDisplay : Observer {
    private float sum;
    private int count;
    public void Update() { }
    public void Display() { }
}

// NumericDisplay.cs
public class NumericDisplay : Observer {
    private int value;
    public void Update() { }
    public void Display() { }
}

// TextDisplay.cs
public class TextDisplay : Observer {
    private int value;
    public void Update() { }
    public void Display() { }
}

// MainDriver.cs
public class MainDriver { public void Main() { } }
```

Build produces `TempSensor.dll`. Run the decompiler on it with `defaults()` config,
capture output → commit as `TempSensor.yuml` and `TempSensor.puml` golden files.

---

## 7. Implementation Order

Dependencies flow bottom-up. Implement and test each layer before the next.

```
1. Fixture project (fixtures/csharp/TempSensor/)  — build TempSensor.dll
2. Test project scaffold (CSharpDecompiler.Tests.csproj, packages)
3. DecompileConfig  → DecompileConfigTests        (no dependencies)
4. TypeFilter       → TypeFilterTests             (depends on DecompileConfig)
5. AssemblyLoader   → AssemblyLoaderTests         (depends on fixture DLL)
6. TypeInspector    → TypeInspectorTests          (depends on DecompileConfig, Model)
7. PlantUmlFormatter→ PlantUmlFormatterTests      (depends on Model, DecompileConfig)
8. YumlFormatter    → YumlFormatterTests          (same)
9. Program (CLI)    — manual smoke test only
10. E2E tests       → SelfTestE2ETests            (depends on all of the above + golden files)
```

Each step: write test → watch it fail → implement → watch it pass.

---

## 8. Open Questions / Decisions to Make During Implementation

| #   | Question                                                                      | Default answer                                                           |
| --- | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| 1   | Include `internal` types from the DLL?                                        | Yes — same as Java's package-private                                     |
| 2   | Show `static` fields?                                                         | Yes — Java's `getDeclaredFields()` includes static                       |
| 3   | Show inherited methods?                                                       | No — `BindingFlags.DeclaredOnly` (mirrors Java's `getDeclaredMethods()`) |
| 4   | Nested types in fixture — skip or include?                                    | Skip (`IsNested` filter)                                                 |
| 5   | `object` base class — show or skip in Extends?                                | Skip (mirrors Java skipping `Object.class`)                              |
| 6   | `IDisposable`, `IComparable` auto-implemented by many types — show or filter? | Show if in loaded set                                                    |
| 7   | `protected internal` access — map to `'#'` or `'~'`?                          | `'#'` (closest semantic match)                                           |
