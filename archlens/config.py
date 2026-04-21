from dataclasses import dataclass, field
from typing import Literal


@dataclass
class DecompileConfig:
    format: Literal["plantuml", "yuml"]
    ignore: list[str] = field(default_factory=list)
    output: str | None = None
    yuml_mode: Literal["SIMPLE", "CLASSES"] = "SIMPLE"
