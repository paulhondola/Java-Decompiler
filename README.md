# Java Reverse Engineering Tool

A Java-based reverse engineering tool that uses reflection (`java.lang.reflection`) to extract design information from compiled `.jar` files and generate UML class diagrams.

## Overview

This tool introspects compiled Java bytecode and produces structured output describing the class diagram of the analyzed code — including classes, interfaces, fields, methods, and relationships. Output can be formatted for popular UML tools like **yuml** and **plantuml**.

## Features

### Core
- Accepts `.jar` files as input via command-line arguments
- Uses `java.util.jar.JarFile` and `URLClassLoader` to load and inspect compiled classes
- Extracts:
  - Classes and interfaces
  - Fields (attributes)
  - Methods
  - Relationships: `extends`, `implements`, `association`, `dependency`
  - Parameterized type arguments

### Configuration Options
| Option | Description |
|--------|-------------|
| `--ignore <pattern>` | Exclude classes matching a pattern (e.g. `java.lang.*`) |
| `--fully-qualified` | Show class names as fully qualified names |
| `--show-methods` | Include method names in the diagram |
| `--show-attributes` | Include field names in the diagram |

### Output Formats
The tool is designed with an extensible architecture — new UML output formats can be added with minimal effort and no changes to the core logic.

Supported formats:
- **yuml** — produces output compatible with [yuml.me](https://yuml.me)
- **plantuml** — produces output compatible with [plantuml.com](https://plantuml.com)

## Usage

```bash
java -jar java-decompiler.jar <target.jar> [options]
```

**Examples:**

```bash
# Generate a yuml diagram with all members shown
java -jar java-decompiler.jar TempSensor.jar --format yuml --show-methods --show-attributes

# Generate a plantuml diagram using fully qualified class names
java -jar java-decompiler.jar TempSensor.jar --format plantuml --fully-qualified

# Generate a yuml diagram ignoring java.lang classes
java -jar java-decompiler.jar EventNotifier.jar --format yuml --ignore "java.lang.*"
```

## Notes on Relationships

| Relationship | Source |
|---|---|
| `extends` | Superclass via reflection |
| `implements` | Interfaces via reflection |
| `association` | Field types |
| `dependency` | Method parameter/return types |

> Aggregation and composition cannot be distinguished via reflection alone — both are reported as `association`.
> Cardinality of associations is not included.

## Build

Requires Java 25+ and Maven.

```bash
mvn package
```
