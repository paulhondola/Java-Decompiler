from unittest.mock import patch

import pytest

from archlens.config import DecompileConfig
from archlens.router import route

_CFG = DecompileConfig(format="plantuml")


def test_route_jar_calls_java_adapter():
    with patch("archlens.router.analyze_jar", return_value="@startuml\n@enduml\n") as mock:
        result = route("Foo.jar", _CFG)
    mock.assert_called_once_with("Foo.jar", _CFG)
    assert result == "@startuml\n@enduml\n"


def test_route_dll_calls_csharp_adapter():
    with patch("archlens.router.analyze_dll", return_value="@startuml\n@enduml\n") as mock:
        result = route("SomeLib.dll", _CFG)
    mock.assert_called_once_with("SomeLib.dll", _CFG)
    assert result == "@startuml\n@enduml\n"


def test_route_raises_for_unknown_extension():
    with pytest.raises(ValueError, match="Unsupported file type"):
        route("Foo.txt", _CFG)


def test_route_extension_case_insensitive():
    with patch("archlens.router.analyze_jar", return_value="output") as mock:
        route("Foo.JAR", _CFG)
    mock.assert_called_once()
