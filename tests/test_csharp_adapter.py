from pathlib import Path

import pytest

from archlens.adapters import csharp_adapter
from archlens.adapters.csharp_adapter import _EXE, analyze_dll
from archlens.config import DecompileConfig
from tests.test_config import TEMP_SENSOR_DLL

_binary_present = pytest.mark.skipif(
    not _EXE.exists(),
    reason="CSharpAnalyzer binary not built — run 'dotnet build CSharpAnalyzer/' first",
)


@_binary_present
def test_analyze_dll_produces_plantuml(temp_sensor_dll: Path):
    result = analyze_dll(temp_sensor_dll, DecompileConfig(format="plantuml"))

    assert "@startuml" in result
    assert "@enduml" in result
    assert "interface Observer" in result
    assert "Update()" in result


@_binary_present
def test_analyze_dll_produces_yuml(temp_sensor_dll: Path):
    result = analyze_dll(temp_sensor_dll, DecompileConfig(format="yuml"))

    assert "[Observer||Update()]" in result


@_binary_present
def test_analyze_dll_with_ignore_pattern(temp_sensor_dll: Path):
    result = analyze_dll(
        temp_sensor_dll,
        DecompileConfig(format="plantuml", ignore=["Observer"]),
    )

    assert "interface Observer" not in result


def test_missing_binary_raises_file_not_found(monkeypatch):
    monkeypatch.setattr(csharp_adapter, "_EXE", Path("/nonexistent/CSharpAnalyzer"))
    with pytest.raises(FileNotFoundError, match="dotnet build"):
        analyze_dll(TEMP_SENSOR_DLL, DecompileConfig(format="plantuml"))
