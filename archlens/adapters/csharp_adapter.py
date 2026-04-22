import subprocess
from pathlib import Path

from archlens.config import DecompileConfig

_ROOT = Path(__file__).resolve().parents[2]
_EXE = _ROOT / "CSharpAnalyzer" / "bin" / "Debug" / "net10.0" / "CSharpAnalyzer"


def analyze_dll(dll_path: str | Path, config: DecompileConfig) -> str:
    """Invoke the CSharpAnalyzer binary on `dll_path` and return the diagram string.

    Raises:
        FileNotFoundError: If the binary has not been built yet.
        subprocess.CalledProcessError: If the dotnet process exits non-zero.
    """
    if not _EXE.exists():
        raise FileNotFoundError(
            f"CSharpAnalyzer binary not found at {_EXE}. "
            "Run 'dotnet build CSharpAnalyzer/CSharpAnalyzer.slnx' first."
        )

    result = subprocess.run(
        _build_command(str(dll_path), config),
        capture_output=True,
        text=True,
        check=True,
    )
    return result.stdout


def _build_command(dll_path: str, config: DecompileConfig) -> list[str]:
    cmd = [str(_EXE), dll_path, "--format", config.format.lower()]
    for pattern in config.ignore:
        cmd += ["--ignore", pattern]
    if config.output:
        cmd += ["--output", config.output]
    return cmd
