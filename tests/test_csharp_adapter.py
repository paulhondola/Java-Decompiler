import pytest

from archlens.adapters.csharp_adapter import decompile_dll
from archlens.config import DecompileConfig


def test_decompile_dll_raises_not_implemented():
    with pytest.raises(NotImplementedError, match="C# decompilation not yet implemented"):
        decompile_dll("SomeLibrary.dll", DecompileConfig(format="plantuml"))


def test_decompile_dll_raises_regardless_of_config():
    for fmt in ("plantuml", "yuml"):
        with pytest.raises(NotImplementedError):
            decompile_dll("Any.dll", DecompileConfig(format=fmt))
