import subprocess
import sys
from pathlib import Path

_ROOT = Path(__file__).resolve().parents[1]


def _run(cmd: list[str], cwd: Path) -> None:
    result = subprocess.run(cmd, cwd=cwd)
    sys.exit(result.returncode)


def java() -> None:
    """Build the Java decompiler fat JAR via Maven."""
    _run(["mvn", "-f", str(_ROOT / "java" / "pom.xml"), "package", "-DskipTests"], cwd=_ROOT)


def csharp() -> None:
    """Build the C# decompiler stub via dotnet."""
    _run(["dotnet", "build", str(_ROOT / "csharp" / "CSharpDecompiler")], cwd=_ROOT)


def all() -> None:
    """Build both Java and C# projects."""
    for cmd, cwd in [
        (["mvn", "-f", str(_ROOT / "java" / "pom.xml"), "package", "-DskipTests"], _ROOT),
        (["dotnet", "build", str(_ROOT / "csharp" / "CSharpDecompiler")], _ROOT),
    ]:
        result = subprocess.run(cmd, cwd=cwd)
        if result.returncode != 0:
            sys.exit(result.returncode)
