from pathlib import Path

import pytest

_ROOT = Path(__file__).resolve().parents[1]

TEMP_SENSOR_JAR = (
    _ROOT
    / "JavaAnalyzer"
    / "src"
    / "test"
    / "java"
    / "tempsensor"
    / "target"
    / "classes"
    / "TempSensor.jar"
)

EVENT_NOTIFIER_JAR = (
    _ROOT
    / "JavaAnalyzer"
    / "src"
    / "test"
    / "java"
    / "eventnotifier"
    / "target"
    / "classes"
    / "EventNotifier.jar"
)

TEMP_SENSOR_DLL = _ROOT / "CSharpAnalyzer" / "bin" / "Debug" / "net10.0" / "TempSensor.dll"


@pytest.fixture(scope="session")
def temp_sensor_jar() -> Path:
    """Path to TempSensor.jar — a small JAR with 7 classes including the Observer interface."""
    return TEMP_SENSOR_JAR


@pytest.fixture(scope="session")
def temp_sensor_dll() -> Path:
    """Path to TempSensor.dll — C# Observer-pattern library with 7 types."""
    return TEMP_SENSOR_DLL
