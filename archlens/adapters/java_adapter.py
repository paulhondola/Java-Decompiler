import subprocess
from pathlib import Path

from archlens.config import DecompileConfig

_ROOT = Path(__file__).resolve().parents[2]
_FAT_JAR = _ROOT / "JavaAnalyzer" / "target" / "JavaAnalyzer-1.0-SNAPSHOT.jar"


def decompile_jar(jar_path: str | Path, config: DecompileConfig) -> str:
    """Invoke the fat JAR on `jar_path` and return the diagram string.

    Raises:
        FileNotFoundError: If the fat JAR has not been built yet.
        subprocess.CalledProcessError: If the Java process exits non-zero.
    """
    if not _FAT_JAR.exists():
        raise FileNotFoundError(
            f"Fat JAR not found at {_FAT_JAR}. Run 'mvn -f JavaAnalyzer/pom.xml package' first."
        )

    result = subprocess.run(
        _build_command(str(jar_path), config),
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout


def _build_command(jar_path: str, config: DecompileConfig) -> list[str]:
    cmd = ["java", "-jar", str(_FAT_JAR), jar_path, f"--format={config.format}"]
    for pattern in config.ignore:
        cmd += ["--ignore", pattern]
    if config.output:
        cmd += ["--output", config.output]
    return cmd
