from pathlib import Path

import pytest

from archlens.adapters import java_adapter
from archlens.adapters.java_adapter import _FAT_JAR, decompile_jar
from archlens.config import DecompileConfig
from tests.conftest import TEMP_SENSOR_JAR

_fat_jar_present = pytest.mark.skipif(
    not _FAT_JAR.exists(),
    reason="fat JAR not built — run 'mvn -f java/pom.xml package' first",
)


@_fat_jar_present
def test_decompile_jar_produces_plantuml(temp_sensor_jar: Path):
    result = decompile_jar(temp_sensor_jar, DecompileConfig(format="plantuml"))

    assert "@startuml" in result
    assert "@enduml" in result
    assert "interface Observer" in result
    assert "update()" in result


@_fat_jar_present
def test_decompile_jar_produces_yuml(temp_sensor_jar: Path):
    result = decompile_jar(temp_sensor_jar, DecompileConfig(format="yuml"))

    assert "[Observer]" in result


@_fat_jar_present
def test_decompile_jar_with_ignore_pattern(temp_sensor_jar: Path):
    result = decompile_jar(
        temp_sensor_jar,
        DecompileConfig(format="plantuml", ignore=["Observer"]),
    )

    assert "interface Observer" not in result


def test_missing_fat_jar_raises_file_not_found(monkeypatch):
    monkeypatch.setattr(java_adapter, "_FAT_JAR", Path("/nonexistent/Java-Decompiler.jar"))
    with pytest.raises(FileNotFoundError, match="mvn"):
        decompile_jar(TEMP_SENSOR_JAR, DecompileConfig(format="plantuml"))
