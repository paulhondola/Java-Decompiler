from pathlib import Path

import pytest

TEMP_SENSOR_JAR = (
    Path(__file__).resolve().parents[1]
    / "java"
    / "src"
    / "test"
    / "java"
    / "tempsensor"
    / "TempSensor.jar"
)

EVENT_NOTIFIER_JAR = (
    Path(__file__).resolve().parents[1]
    / "java"
    / "src"
    / "test"
    / "java"
    / "eventnotifier"
    / "EventNotifier.jar"
)


@pytest.fixture(scope="session")
def temp_sensor_jar() -> Path:
    """Path to TempSensor.jar — a small JAR with 7 classes including the Observer interface."""
    return TEMP_SENSOR_JAR
