from pathlib import Path

from archlens.config import DecompileConfig

_ROOT = Path(__file__).resolve().parents[2]
_DLL = _ROOT / "CSharpAnalyzer" / "bin" / "Debug" / "net10.0" / "CSharpAnalyzer.dll"


def decompile_dll(dll_path: str, config: DecompileConfig) -> str:
    raise NotImplementedError("C# decompilation not yet implemented")
