from pathlib import Path

from archlens.adapters.csharp_adapter import decompile_dll
from archlens.adapters.java_adapter import decompile_jar
from archlens.config import DecompileConfig


def route(input_path: str, config: DecompileConfig) -> str:
    """Route `input_path` to the correct adapter based on its file extension.

    Raises:
        ValueError: For any extension other than .jar or .dll.
        NotImplementedError: When routing to the C# adapter (not yet implemented).
    """
    match Path(input_path).suffix.lower():
        case ".jar":
            return decompile_jar(input_path, config)
        case ".dll":
            return decompile_dll(input_path, config)
        case s:
            raise ValueError(f"Unsupported file type '{s}'. Expected .jar or .dll.")
