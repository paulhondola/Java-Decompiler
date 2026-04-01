# Assignment: MyReverseEngineeringTool

**Objective:** Use reflective properties of programming languages (Introspection) to extract design information from compiled code.

---

## Description

Using the reflective capabilities of Java (`java.lang.reflection`) or .NET (`System.Reflection`), implement a simple reverse engineering tool that extracts class diagram information from compiled binaries.

---

## Standard Requirements

The tool accepts as command-line arguments the name of a compiled file (`.jar`, `.dll`, or `.exe`) and extracts all information needed to describe the class diagram of the classes it contains:

- Classes and interfaces
- Methods
- Fields
- Relationships between classes/interfaces

**Java-specific guidance:**
- Use `java.util.jar.JarFile` to open `.jar` files
- Use `URLClassLoader` to load classes and retrieve `Class` metaobjects
- Make intensive use of introspection

---

## Bonus Requirements

### UML Output Formats

The tool must produce outputs compatible with UML drawing tools such as **yuml** and **plantuml**.

- Design the tool so that **new output formats can be added with minimal effort and no changes to the core logic**

### Configuration Options

The tool must support the following options:

- **Ignore list** — specify classes to exclude from the diagram (e.g. `java.lang.*`)
- **Fully qualified names** — show class names as fully qualified: `yes/no`
- **Show methods** — include method names in the diagram: `yes/no`
- **Show attributes** — include field names in the diagram: `yes/no`

### Relationships

Distinguish 4 types of relationships:

| Type | Description |
|------|-------------|
| `extends` | Class/interface inheritance |
| `implements` | Interface implementation |
| `association` | Field-level reference |
| `dependency` | Method parameter/return type reference |

> Note: Aggregation and composition cannot be distinguished via reflection. Both are reported as `association`.
> Cardinality of associations is ignored.

### Parameterized Types

When parameterized types are encountered, include their actual type arguments in the diagram.

---

## Examples

### TempSensor.jar

- `--format yuml` produces `TempSensor-yuml.txt`
- `--format plantuml` produces `TempSensor-plantuml.txt`

### EventNotifier.jar

- With class members shown → `EventNotifier-classes-yuml.txt`
- Without class members → `EventNotifier-simple-yuml.txt`
